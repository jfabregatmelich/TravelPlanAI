package com.travelplan.travelplanai.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplan.travelplanai.model.DailyPlan;
import com.travelplan.travelplanai.model.TravelPlan;
import com.travelplan.travelplanai.model.TravelRequest;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Funcion principal que genera el plan de viaje llamando a la API de Gemini
    public TravelPlan generateTravelPlan(TravelRequest request) throws Exception {
        String prompt = buildPrompt(request);
        System.out.println("DEBUG - Prompt enviado a Gemini:\n" + prompt);
        
        String geminiResponse = callGeminiApi(prompt);
        System.out.println("DEBUG - Respuesta de Gemini:\n" + geminiResponse);
        
        return parseGeminiResponse(geminiResponse, request);
    }

        // Construye el prompt para enviar a Gemini con los datos del usuario
    private String buildPrompt(TravelRequest request) {
        String countriesStr = String.join(", ", request.getCountries());
        
        // Calcular presupuesto por día
        int budgetPerDay = request.getBudget() / request.getDays();
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres un experto planificador de viajes por Europa especializado en itinerarios económicos y realistas.\n\n");
        
        prompt.append("DATOS DEL VIAJE:\n");
        prompt.append("- Paises a visitar: ").append(countriesStr).append("\n");
        prompt.append("- Ciudad principal: ").append(request.getCity()).append("\n");
        prompt.append("- Número de días: ").append(request.getDays()).append("\n");
        prompt.append("- Presupuesto total: ").append(request.getBudget()).append(" EUR\n");
        prompt.append("- Presupuesto por día: ").append(budgetPerDay).append(" EUR/día\n\n");
        
        prompt.append("INSTRUCCIONES IMPORTANTES:\n");
        prompt.append("Genera un plan de viaje detallado y REALISTA para ").append(request.getDays())
              .append(" días en ").append(request.getCity()).append(".\n\n");
        
        prompt.append("Para CADA día debes incluir:\n");
        prompt.append("1. MAÑANA: Un lugar turístico principal o actividad cultural\n");
        prompt.append("2. COMIDA: Un restaurante recomendado, mercado local o zona de tapeo\n");
        prompt.append("3. TARDE: Otro lugar turístico, museo, parque o actividad interesante\n");
        prompt.append("4. PRECIO MAÑANA: Costo estimado en EUR (entrada, transporte, etc.)\n");
        prompt.append("5. PRECIO COMIDA: Costo estimado de la comida en EUR\n");
        prompt.append("6. PRECIO TARDE: Costo estimado de la actividad de tarde en EUR\n\n");
        
        prompt.append("FORMATO DE DESCRIPCIÓN CON HORARIO:\n");
        prompt.append("Para cada actividad (MAÑANA, COMIDA, TARDE), DEBES incluir el horario de apertura del lugar.\n");
        prompt.append("El horario debe ir al FINAL de la descripción, con el formato EXACTO: \"Abierto de Xh a Yh\"\n");
        prompt.append("Ejemplos:\n");
        prompt.append("- \"Explorar el Barrio Gotico: Paseo por sus callejones medievales, visita a la Catedral de Barcelona. Abierto de 9h a 20h.\"\n");
        prompt.append("- \"Mercado de La Boqueria: Disfruta de tapas y zumos naturales. Abierto de 9h a 20h.\"\n");
        prompt.append("- \"Paseo por Las Ramblas y el Mirador de Colom. Abierto de 9h a 22h.\"\n\n");
        
        prompt.append("CONSIDERACIONES DE PRESUPUESTO (").append(budgetPerDay).append(" EUR/día):\n");
        if (budgetPerDay < 50) {
            prompt.append("- Presupuesto ajustado: prioriza actividades gratuitas (0-10 EUR por actividad)\n");
            prompt.append("- Comidas económicas: 5-15 EUR\n");
        } else if (budgetPerDay < 100) {
            prompt.append("- Presupuesto moderado: actividades de 5-20 EUR\n");
            prompt.append("- Comidas: 10-25 EUR\n");
        } else {
            prompt.append("- Presupuesto cómodo: actividades de 10-40 EUR\n");
            prompt.append("- Comidas: 15-40 EUR\n");
        }
        
        prompt.append("\nFORMATO DE RESPUESTA - RESPONDE ÚNICAMENTE CON ESTE JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"days\": [\n");
        
        for (int i = 1; i <= request.getDays(); i++) {
            prompt.append("    {\n");
            prompt.append("      \"day\": ").append(i).append(",\n");
            prompt.append("      \"morning\": \"descripción de la actividad CON horario al final (Abierto de Xh a Yh)\",\n");
            prompt.append("      \"morningPrice\": 0,\n");
            prompt.append("      \"lunch\": \"nombre del restaurante o tipo de comida CON horario al final\",\n");
            prompt.append("      \"lunchPrice\": 0,\n");
            prompt.append("      \"afternoon\": \"descripción de la actividad CON horario al final\",\n");
            prompt.append("      \"afternoonPrice\": 0\n");
            prompt.append("    }");
            if (i < request.getDays()) prompt.append(",");
            prompt.append("\n");
        }
        
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        
        prompt.append("REGLAS:\n");
        prompt.append("- Los precios deben ser realistas para ").append(request.getCity()).append("\n");
        prompt.append("- La suma de precios por día debe aproximarse al presupuesto de ").append(budgetPerDay).append(" EUR\n");
        prompt.append("- Si una actividad es gratuita, pon 0\n");
        prompt.append("- RESPUESTA VÁLIDA EN JSON\n");
        prompt.append("- IMPORTANTE: Cada descripción DEBE terminar con el horario en el formato exacto 'Abierto de Xh a Yh'\n");
        
        return prompt.toString();
    }

    // Envia la peticion HTTP a la API de Google Gemini
    private String callGeminiApi(String prompt) throws Exception {
        String url = apiUrl + "?key=" + apiKey;
        
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            ),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "topK", 40,
                "topP", 0.95,
                "maxOutputTokens", 4096
            )
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class
        );
        
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new Exception("Error en API Gemini: " + response.getStatusCode());
        }
        
        return extractTextFromGeminiResponse(response.getBody());
    }

    // Extrae solo el texto de la respuesta de Gemini
    private String extractTextFromGeminiResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        
        if (root.has("error")) {
            String errorMsg = root.path("error").path("message").asText();
            throw new Exception("Gemini API error: " + errorMsg);
        }
        
        String text = root.path("candidates")
                          .path(0)
                          .path("content")
                          .path("parts")
                          .path(0)
                          .path("text")
                          .asText();
        
        if (text == null || text.isEmpty()) {
            throw new Exception("Respuesta vacia de Gemini");
        }
        
        text = text.replace("```json", "")
                   .replace("```JSON", "")
                   .replace("```", "")
                   .trim();
        
        return text;
    }

    // Parsea la respuesta JSON de Gemini con precios
    private TravelPlan parseGeminiResponse(String jsonResponse, TravelRequest request) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode daysArray = root.path("days");
            
            if (daysArray.isMissingNode() || daysArray.size() == 0) {
                daysArray = root.path("itinerary");
                if (daysArray.isMissingNode() || daysArray.size() == 0) {
                    throw new Exception("No se encontró el array 'days'");
                }
            }
            
            List<DailyPlan> itinerary = new ArrayList<>();
            
            for (JsonNode dayNode : daysArray) {
                int day = dayNode.path("day").asInt();
                String morning = dayNode.path("morning").asText();
                String lunch = dayNode.path("lunch").asText();
                String afternoon = dayNode.path("afternoon").asText();
                
                // Obtener precios
                Integer morningPrice = dayNode.has("morningPrice") ? dayNode.path("morningPrice").asInt() : null;
                Integer lunchPrice = dayNode.has("lunchPrice") ? dayNode.path("lunchPrice").asInt() : null;
                Integer afternoonPrice = dayNode.has("afternoonPrice") ? dayNode.path("afternoonPrice").asInt() : null;
                
                // Valores por defecto
                if (morning == null || morning.isEmpty()) morning = "Explorar el centro histórico";
                if (lunch == null || lunch.isEmpty()) lunch = "Descubrir la gastronomía local";
                if (afternoon == null || afternoon.isEmpty()) afternoon = "Visitar lugares emblemáticos";
                
                DailyPlan dailyPlan = new DailyPlan(day, morning, lunch, afternoon, 
                                                     morningPrice, lunchPrice, afternoonPrice);
                itinerary.add(dailyPlan);
            }
            
            TravelPlan plan = new TravelPlan();
            plan.setDestination(request.getCity());
            plan.setTotalDays(request.getDays());
            plan.setTotalBudget(request.getBudget());
            plan.setBudgetPerDay(request.getBudget() / request.getDays());
            plan.setItinerary(itinerary);
            
            return plan;
            
        } catch (Exception e) {
            System.err.println("Error parseando JSON: " + e.getMessage());
            return generateMockPlan(request);
        }
    }
    
    // Plan mock con precios
    public TravelPlan generateMockPlan(TravelRequest request) {
        System.out.println("Generando plan mock para " + request.getCity());
        
        List<DailyPlan> itinerary = new ArrayList<>();
        int budgetPerDay = request.getBudget() / request.getDays();
        Random random = new Random();
        
        String[] morningActivities = {
            "Visitar el casco histórico y plaza principal",
            "Recorrer los museos más importantes",
            "Explorar mercados y zonas comerciales",
            "Descubrir la arquitectura local",
            "Paseo por parques y jardines emblemáticos"
        };
        
        String[] lunchOptions = {
            "Mercado de San Miguel - comida tradicional",
            "Restaurante El Brillante - comida típica",
            "Zona de tapas en el centro histórico",
            "Picnic en el parque con productos locales",
            "Food truck park - variedad internacional"
        };
        
        String[] afternoonActivities = {
            "Visitar monumentos y miradores",
            "Recorrer barrios con encanto",
            "Museo de arte contemporáneo",
            "Excursión a lugares cercanos",
            "Tiempo libre para compras y relax"
        };
        
        for (int i = 1; i <= request.getDays(); i++) {
            // Generar precios realistas según presupuesto
            int morningPrice = budgetPerDay >= 80 ? random.nextInt(25) + 5 : random.nextInt(15);
            int lunchPrice = budgetPerDay >= 80 ? random.nextInt(30) + 10 : random.nextInt(20) + 5;
            int afternoonPrice = budgetPerDay >= 80 ? random.nextInt(25) + 5 : random.nextInt(15);
            
            // Ajustar para no exceder presupuesto
            int total = morningPrice + lunchPrice + afternoonPrice;
            if (total > budgetPerDay) {
                float factor = (float) budgetPerDay / total;
                morningPrice = Math.max(0, (int)(morningPrice * factor));
                lunchPrice = Math.max(0, (int)(lunchPrice * factor));
                afternoonPrice = Math.max(0, (int)(afternoonPrice * factor));
            }
            
            String morning = morningActivities[(i - 1) % morningActivities.length];
            String lunch = lunchOptions[(i - 1) % lunchOptions.length];
            String afternoon = afternoonActivities[(i - 1) % afternoonActivities.length];
            
            itinerary.add(new DailyPlan(i, morning, lunch, afternoon, 
                                        morningPrice, lunchPrice, afternoonPrice));
        }
        
        TravelPlan plan = new TravelPlan();
        plan.setDestination(request.getCity());
        plan.setTotalDays(request.getDays());
        plan.setTotalBudget(request.getBudget());
        plan.setBudgetPerDay(budgetPerDay);
        plan.setItinerary(itinerary);
        
        return plan;
    }
}