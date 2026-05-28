package com.travelplan.travelplanai.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.travelplan.travelplanai.model.DailyPlan;
import com.travelplan.travelplanai.model.TravelPlan;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "http://localhost:8080")
public class PdfController {

    private static final float PW = 595f;  // A4 width  (points)
    private static final float PH = 842f;  // A4 height (points)
    private static final float M  = 50f;   // margin

    // Colores basados en styles.css
    // Verde principal: #2c5a3b / #1e3c2c
    private static final float PRIMARY_R = 0.17f;  // #2c5a3b -> 44/255, 90/255, 59/255
    private static final float PRIMARY_G = 0.35f;
    private static final float PRIMARY_B = 0.23f;
    // Acento dorado/terracota: #cb7b3c / #f5c542
    private static final float ACCENT_R = 0.80f;   // #cb7b3c -> 203/255, 123/255, 60/255
    private static final float ACCENT_G = 0.48f;
    private static final float ACCENT_B = 0.24f;
    // Fondo claro: #fffcf5
    private static final float BG_R = 1.0f;
    private static final float BG_G = 0.99f;
    private static final float BG_B = 0.96f;
    // Texto oscuro: #2a3a30
    private static final float TEXT_R = 0.16f;
    private static final float TEXT_G = 0.23f;
    private static final float TEXT_B = 0.19f;
    // Texto secundario: #5f7169
    private static final float SEC_R = 0.37f;
    private static final float SEC_G = 0.44f;
    private static final float SEC_B = 0.41f;

    // =========================================================================
    // Endpoint
    // =========================================================================

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generatePdf(@RequestBody TravelPlan plan) {
        try {
            System.out.println("Generando PDF para: " + plan.getDestination());
            byte[] pdf = buildPdf(plan);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                ContentDisposition.attachment()
                    .filename("travel_plan_" + sanitize(plan.getDestination()) + ".pdf")
                    .build()
            );
            headers.setContentLength(pdf.length);
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error generando PDF: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================================
    // Build the full PDF
    // =========================================================================

    private byte[] buildPdf(TravelPlan plan) throws IOException {
        List<byte[]> pages = new ArrayList<>();
        Page page = new Page();
        float y = PH - M;

        // ── Header ────────────────────────────────────────────────────────────
        // TravelPlan AI en negrita y color primario
        page.textBold(M, y, "TravelPlan AI", 18, PRIMARY_R, PRIMARY_G, PRIMARY_B); y -= 22;
        page.text(M, y, "Plan de viaje inteligente", 11, SEC_R, SEC_G, SEC_B); y -= 14;
        page.hline(y, 0.8f, PRIMARY_R, PRIMARY_G, PRIMARY_B); y -= 18;

        // ── Trip summary ──────────────────────────────────────────────────────
        // Destino en negrita y color texto
        page.textBold(M, y, "Destino: " + safe(plan.getDestination()), 13, TEXT_R, TEXT_G, TEXT_B); y -= 18;
        int bpd = plan.getBudgetPerDay() != null ? plan.getBudgetPerDay()
                  : (plan.getTotalDays() > 0 ? plan.getTotalBudget() / plan.getTotalDays() : 0);
        page.text(M, y,
            "Duracion: " + plan.getTotalDays() + " dias   |   " +
            "Presupuesto: " + plan.getTotalBudget() + " EUR   |   " +
            "Por dia: " + bpd + " EUR",
            11, SEC_R, SEC_G, SEC_B); y -= 14;
        page.hline(y, 0.5f, 0.8f, 0.8f, 0.8f); y -= 22;

        // ── Section heading ───────────────────────────────────────────────────
        page.textBold(M, y, "ITINERARIO", 14, ACCENT_R, ACCENT_G, ACCENT_B); y -= 24;

        // ── Days ──────────────────────────────────────────────────────────────
        if (plan.getItinerary() != null) {
            for (DailyPlan day : plan.getItinerary()) {

                if (y < M + 90) {
                    pages.add(page.finish());
                    page = new Page();
                    y = PH - M;
                }

                // Day header badge con color acento
                page.rect(M, y, PW - M * 2, 18, ACCENT_R, ACCENT_G, ACCENT_B);
                // DIA X en negrita y subrayado
                page.textBoldUnderline(M + 6, y - 6, "DIA " + day.getDay(), 12, 1f, 1f, 1f);
                y -= 28;

                SlotResult res;
                res = writeSlot(page, pages, "Manana",  day.getMorning(),   day.getMorningPrice(),   y);
                page = res.page; y = res.y;
                res = writeSlot(page, pages, "Comida",  day.getLunch(),     day.getLunchPrice(),     y);
                page = res.page; y = res.y;
                res = writeSlot(page, pages, "Tarde",   day.getAfternoon(), day.getAfternoonPrice(), y);
                page = res.page; y = res.y;

                int total = day.getDayTotal();
                if (total > 0) {
                    page.textBold(PW - M - 130, y, "Subtotal dia: " + total + " EUR", 10, ACCENT_R, ACCENT_G, ACCENT_B);
                    y -= 16;
                }
                page.hline(y, 0.3f, 0.85f, 0.85f, 0.85f); y -= 14;
            }
        }

        // ── Footer ────────────────────────────────────────────────────────────
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        page.hline(M + 10, 0.5f, 0.75f, 0.75f, 0.75f);
        page.text(M, M - 8,
            "Generado con TravelPlan AI · Joel Fabregat Melich  -  " + date,
            8, SEC_R, SEC_G, SEC_B);

        pages.add(page.finish());
        return assemble(pages);
    }

        /**
     * Renders one time slot (morning/lunch/afternoon).
     * Returns a SlotResult containing the (possibly new) Page and the updated Y coordinate.
     */
    private SlotResult writeSlot(Page page, List<byte[]> pages,
                                 String label, String raw, Integer price, float y) throws IOException {

        if (y < M + 70) {
            pages.add(page.finish());
            page = new Page();
            y = PH - M;
        }

        // Label en negrita y color acento
        page.textBold(M + 10, y, label + ":", 10, ACCENT_R, ACCENT_G, ACCENT_B);
        y -= 14;

        String body = (raw != null && !raw.isEmpty()) ? raw : "Actividad no especificada";
        
        // Separar el texto del horario si existe
        String scheduleText = null;
        String descriptionText = body;
        
        // Buscar patrón de horario "Abierto de Xh a Yh"
        int scheduleIndex = body.lastIndexOf("Abierto de");
        if (scheduleIndex >= 0) {
            // Encontrar el punto que sigue al horario
            int endOfSchedule = body.indexOf(".", scheduleIndex);
            if (endOfSchedule < 0) {
                endOfSchedule = body.length();
            } else {
                endOfSchedule = endOfSchedule + 1; // incluir el punto
            }
            scheduleText = body.substring(scheduleIndex, endOfSchedule);
            descriptionText = body.substring(0, scheduleIndex).trim();
            // Limpiar espacios y puntuación redundante
            if (descriptionText.endsWith(".") && scheduleText.startsWith("Abierto")) {
                descriptionText = descriptionText.substring(0, descriptionText.length() - 1);
            }
        }
        
        // Escribir la descripción (puede ser multi-línea)
        for (String line : wrap(descriptionText, 72)) {
            if (y < M + 40) {
                pages.add(page.finish());
                page = new Page();
                y = PH - M;
            }
            page.text(M + 20, y, line, 10, TEXT_R, TEXT_G, TEXT_B);
            y -= 13;
        }
        
        // Escribir el horario en negrita si existe
        if (scheduleText != null && !scheduleText.isEmpty()) {
            if (y < M + 40) {
                pages.add(page.finish());
                page = new Page();
                y = PH - M;
            }
            // Horario en negrita color negro (0,0,0)
            page.textBold(M + 20, y, scheduleText, 10, PRIMARY_R, PRIMARY_G, PRIMARY_B);
            y -= 13;
        }
        
        // Escribir el precio
        if (price != null && price > 0) {
            page.textBold(M + 20, y, "Costo: " + price + " EUR", 9, PRIMARY_R, PRIMARY_G, PRIMARY_B);
            y -= 13;
        }
        return new SlotResult(page, y - 3);
    }

    // =========================================================================
    // Assemble multi-page PDF binary
    // =========================================================================

    private byte[] assemble(List<byte[]> streams) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        int n = streams.size();
        int fontObj = 3 + n * 2;

        w(out, "%PDF-1.4\n");

        // obj 1: Catalog
        offsets.add(out.size());
        w(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        // obj 2: Pages
        StringBuilder kids = new StringBuilder("[");
        for (int i = 0; i < n; i++) { if (i > 0) kids.append(" "); kids.append((3 + i * 2) + " 0 R"); }
        kids.append("]");
        offsets.add(out.size());
        w(out, "2 0 obj\n<< /Type /Pages /Kids " + kids + " /Count " + n + " >>\nendobj\n");

        // page dict + content stream pairs
        for (int i = 0; i < n; i++) {
            int po = 3 + i * 2, co = po + 1;
            byte[] s = streams.get(i);

            offsets.add(out.size());
            w(out, po + " 0 obj\n<< /Type /Page /Parent 2 0 R "
                + "/MediaBox [0 0 595 842] /Contents " + co + " 0 R "
                + "/Resources << /Font << /F1 " + fontObj + " 0 R /F2 " + (fontObj + 1) + " 0 R >> >> >>\nendobj\n");

            offsets.add(out.size());
            w(out, co + " 0 obj\n<< /Length " + s.length + " >>\nstream\n");
            out.write(s);
            w(out, "\nendstream\nendobj\n");
        }

        // font objects: F1 = normal, F2 = bold
        offsets.add(out.size());
        w(out, fontObj + " 0 obj\n<< /Type /Font /Subtype /Type1 "
            + "/BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");
        
        offsets.add(out.size());
        w(out, (fontObj + 1) + " 0 obj\n<< /Type /Font /Subtype /Type1 "
            + "/BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");

        // cross-reference table
        int total = offsets.size();
        int xref  = out.size();
        w(out, "xref\n0 " + (total + 1) + "\n");
        w(out, "0000000000 65535 f \n");
        for (int off : offsets) w(out, String.format(Locale.US, "%010d 00000 n \n", off));
        w(out, "trailer\n<< /Size " + (total + 1) + " /Root 1 0 R >>\n");
        w(out, "startxref\n" + xref + "\n%%EOF\n");

        return out.toByteArray();
    }

    // =========================================================================
    // Inner class: builds one PDF content stream
    // =========================================================================

    private static class Page {
        private final StringBuilder sb = new StringBuilder("q\n");

        // Texto normal
        void text(float x, float y, String t, int size, float r, float g, float b) {
            sb.append("BT ")
              .append("/F1 ").append(size).append(" Tf ")
              .append(f(r)).append(" ").append(f(g)).append(" ").append(f(b)).append(" rg ")
              .append("1 0 0 1 ").append(f(x)).append(" ").append(f(y)).append(" Tm ")
              .append("(").append(esc(t)).append(") Tj ")
              .append("ET\n");
        }

        // Texto en negrita
        void textBold(float x, float y, String t, int size, float r, float g, float b) {
            sb.append("BT ")
              .append("/F2 ").append(size).append(" Tf ")
              .append(f(r)).append(" ").append(f(g)).append(" ").append(f(b)).append(" rg ")
              .append("1 0 0 1 ").append(f(x)).append(" ").append(f(y)).append(" Tm ")
              .append("(").append(esc(t)).append(") Tj ")
              .append("ET\n");
        }

        // Texto en negrita y subrayado
        void textBoldUnderline(float x, float y, String t, int size, float r, float g, float b) {
            // Calculamos el ancho aproximado del texto para la línea
            float textWidth = t.length() * (size * 0.45f);
            sb.append("BT ")
              .append("/F2 ").append(size).append(" Tf ")
              .append(f(r)).append(" ").append(f(g)).append(" ").append(f(b)).append(" rg ")
              .append("1 0 0 1 ").append(f(x)).append(" ").append(f(y)).append(" Tm ")
              .append("(").append(esc(t)).append(") Tj ")
              .append("ET\n");
            // Subrayado
            sb.append("q ")
              .append(f(r)).append(" ").append(f(g)).append(" ").append(f(b)).append(" RG ")
              .append("0.8 w ")
              .append(f(x)).append(" ").append(f(y - 2)).append(" m ")
              .append(f(x + textWidth)).append(" ").append(f(y - 2)).append(" l S Q\n");
        }

        void hline(float y, float lw, float r, float g, float b) {
            sb.append("q ")
              .append(f(r)).append(" ").append(f(g)).append(" ").append(f(b)).append(" RG ")
              .append(f(lw)).append(" w ")
              .append(f(M)).append(" ").append(f(y)).append(" m ")
              .append(f(PW - M)).append(" ").append(f(y)).append(" l S Q\n");
        }

        void rect(float x, float y, float w, float h, float r, float g, float b) {
            sb.append("q ")
              .append(f(r)).append(" ").append(f(g)).append(" ").append(f(b)).append(" rg ")
              .append(f(x)).append(" ").append(f(y - h + 5)).append(" ")
              .append(f(w)).append(" ").append(f(h)).append(" re f Q\n");
        }

        byte[] finish() {
            sb.append("Q\n");
            return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
        }

        private static String f(float v) { return String.format(Locale.US, "%.2f", v); }

        private static String esc(String s) {
            if (s == null) return "";
            s = s.replace("\u00e1","a").replace("\u00e9","e").replace("\u00ed","i")
                 .replace("\u00f3","o").replace("\u00fa","u").replace("\u00fc","u")
                 .replace("\u00f1","n").replace("\u00e0","a").replace("\u00e8","e")
                 .replace("\u00ec","i").replace("\u00f2","o").replace("\u00f9","u")
                 .replace("\u00c1","A").replace("\u00c9","E").replace("\u00cd","I")
                 .replace("\u00d3","O").replace("\u00da","U").replace("\u00d1","N")
                 .replace("\u00c0","A").replace("\u00c8","E").replace("\u00cc","I")
                 .replace("\u00d2","O").replace("\u00d9","U")
                 .replace("\u00bf","?").replace("\u00a1","!")
                 .replace("\u20ac","EUR").replace("\u2019","'")
                 .replace("\u2018","'").replace("\u201c","\"").replace("\u201d","\"");
            return s.replace("\\","\\\\").replace("(","\\(").replace(")","\\)");
        }
    }

    // =========================================================================
    // Helper class to return both Page and Y coordinate from writeSlot
    // =========================================================================
    private static class SlotResult {
        Page page;
        float y;
        SlotResult(Page page, float y) { this.page = page; this.y = y; }
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    private List<String> wrap(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) { lines.add(""); return lines; }
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            int extra = cur.length() > 0 ? 1 : 0;
            if (cur.length() + extra + w.length() <= maxChars) {
                if (cur.length() > 0) cur.append(" ");
                cur.append(w);
            } else {
                if (cur.length() > 0) lines.add(cur.toString());
                cur = new StringBuilder(w);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    private String safe(String s) { return s != null ? s : ""; }

    private String sanitize(String s) {
        if (s == null) return "viaje";
        return s.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
    }

    private void w(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    // =========================================================================
    // Endpoint de prueba
    // =========================================================================

    @GetMapping("/test-generate")
    public ResponseEntity<byte[]> testGeneratePdf() {
        try {
            System.out.println("Generando PDF de prueba sin IA...");

            TravelPlan testPlan = createMockTravelPlan();

            byte[] pdf = buildPdf(testPlan);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                ContentDisposition.attachment()
                    .filename("test_travel_plan.pdf")
                    .build()
            );
            headers.setContentLength(pdf.length);
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private TravelPlan createMockTravelPlan() {
        TravelPlan plan = new TravelPlan();
        plan.setDestination("Berlín, Alemania");
        plan.setTotalDays(3);
        plan.setTotalBudget(240);
        plan.setBudgetPerDay(80);

        List<DailyPlan> itinerary = new ArrayList<>();

        DailyPlan day1 = new DailyPlan();
        day1.setDay(1);
        day1.setMorning("Visita al Reichstag y su cúpula de cristal (reserva previa gratuita es obligatoria). Ofrece vistas panorámicas de la ciudad.");
        day1.setMorningPrice(0);
        day1.setLunch("Currywurst en Curry 36 (una institución berlinesa para probar este plato típico).");
        day1.setLunchPrice(8);
        day1.setAfternoon("Paseo por la Puerta de Brandeburgo, el Monumento a los Judíos Asesinados de Europa y la Potsdamer Platz para admirar la arquitectura moderna.");
        day1.setAfternoonPrice(0);
        itinerary.add(day1);

        DailyPlan day2 = new DailyPlan();
        day2.setDay(2);
        day2.setMorning("Exploración de la Isla de los Museos: visita el Pergamonmuseum (si está abierto y te interesa la arqueología) o el Neues Museum para ver el busto de Nefertiti.");
        day2.setMorningPrice(12);
        day2.setLunch("Mercado Markthalle Neun (especialmente los jueves para el Street Food Thursday, pero siempre hay opciones interesantes).");
        day2.setLunchPrice(15);
        day2.setAfternoon("Visita al East Side Gallery, un tramo del Muro de Berlín convertido en galería de arte al aire libre.");
        day2.setAfternoonPrice(0);
        itinerary.add(day2);

        DailyPlan day3 = new DailyPlan();
        day3.setDay(3);
        day3.setMorning("Visita al Memorial del Muro de Berlín y el Centro de Documentación. Es un lugar conmovedor para entender la historia de la división de la ciudad.");
        day3.setMorningPrice(5);
        day3.setLunch("Comida en un 'Imbiss' (pequeño puesto de comida rápida) cerca de tu alojamiento, probando un Döner Kebab, muy popular y económico en Berlín.");
        day3.setLunchPrice(7);
        day3.setAfternoon("");
        day3.setAfternoonPrice(0);
        itinerary.add(day3);

        plan.setItinerary(itinerary);
        return plan;
    }
}