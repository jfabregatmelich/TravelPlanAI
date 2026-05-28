package com.travelplan.travelplanai.controller;

import java.util.List;
import com.travelplan.travelplanai.model.TravelPlan;
import com.travelplan.travelplanai.model.TravelRequest;
import com.travelplan.travelplanai.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plan")
@CrossOrigin(origins = "http://localhost:8080")
public class PlanController {

    @Autowired
    private GeminiService geminiService;

    // Endpoint que recibe la peticion del frontend y devuelve el plan generado por IA
    @PostMapping("/generate")
    public ResponseEntity<?> generatePlan(@RequestBody TravelRequest request) {
        try {
            System.out.println("=== NUEVA PETICION RECIBIDA ===");
            System.out.println("Ciudad: " + request.getCity());
            System.out.println("Países: " + request.getCountries());
            System.out.println("Días: " + request.getDays());
            System.out.println("Presupuesto total: " + request.getBudget() + " EUR");
            System.out.println("Presupuesto por día: " + request.getBudgetPerDay() + " EUR/día");
            
            // Validar que los datos sean correctos
            if (request.getCity() == null || request.getCity().isEmpty()) {
                return ResponseEntity.badRequest().body("Error: No se ha especificado una ciudad");
            }
            
            if (request.getCountries() == null || request.getCountries().isEmpty()) {
                return ResponseEntity.badRequest().body("Error: No se ha especificado al menos un país");
            }
            
            if (request.getDays() <= 0 || request.getDays() > 30) {
                return ResponseEntity.badRequest().body("Error: El número de días debe estar entre 1 y 30");
            }
            
            // Si el request tiene budgetPerDay, recalcular el total para asegurar consistencia
            if (request.getBudgetPerDay() != null && request.getBudgetPerDay() > 0) {
                int calculatedTotal = request.getBudgetPerDay() * request.getDays();
                System.out.println("Recalculando presupuesto total: " + calculatedTotal + " EUR (basado en " + request.getBudgetPerDay() + " EUR/día)");
                request.setBudget(calculatedTotal);
            }
            
            // Validar presupuesto
            if (request.getBudget() <= 0) {
                return ResponseEntity.badRequest().body("Error: El presupuesto debe ser mayor que 0");
            }
            
            System.out.println("Enviando petición a GeminiService...");
            
            // Intentar generar el plan con Gemini
            TravelPlan plan = geminiService.generateTravelPlan(request);
            
            // Verificar que el plan tenga datos válidos
            if (plan == null) {
                throw new Exception("El servicio Gemini devolvió un plan nulo");
            }
            
            if (plan.getItinerary() == null || plan.getItinerary().isEmpty()) {
                System.out.println("WARNING: El itinerario está vacío, usando plan de respaldo");
                plan = geminiService.generateMockPlan(request);
            }
            
            // Asegurar que el presupuesto por día esté presente
            if (plan.getBudgetPerDay() == null && request.getBudgetPerDay() != null) {
                plan.setBudgetPerDay(request.getBudgetPerDay());
            } else if (plan.getBudgetPerDay() == null && request.getDays() > 0) {
                plan.setBudgetPerDay(request.getBudget() / request.getDays());
            }
            
            System.out.println("Plan generado exitosamente para " + plan.getDestination());
            System.out.println("Días: " + plan.getTotalDays());
            System.out.println("Presupuesto total: " + plan.getTotalBudget() + " EUR");
            System.out.println("Presupuesto por día: " + plan.getBudgetPerDay() + " EUR/día");
            System.out.println("Itinerario: " + plan.getItinerary().size() + " días generados");
            
            return ResponseEntity.ok(plan);
            
        } catch (Exception e) {
            System.err.println("ERROR generando el plan de viaje: " + e.getMessage());
            e.printStackTrace();
            
            // Intentar devolver un plan mock en caso de error para no dejar al usuario sin respuesta
            try {
                System.out.println("Generando plan de respaldo debido al error...");
                TravelPlan fallbackPlan = geminiService.generateMockPlan(request);
                return ResponseEntity.ok(fallbackPlan);
            } catch (Exception fallbackError) {
                System.err.println("Error también en el plan de respaldo: " + fallbackError.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error generando el plan de viaje: " + e.getMessage());
            }
        }
    }
    
    // Endpoint adicional para probar la conexión con Gemini
    @GetMapping("/test")
    public ResponseEntity<?> testConnection() {
        try {
            TravelRequest testRequest = new TravelRequest();
            testRequest.setCountries(List.of("España"));
            testRequest.setCity("Madrid");
            testRequest.setDays(3);
            testRequest.setBudget(240);
            testRequest.setBudgetPerDay(80);
            
            TravelPlan testPlan = geminiService.generateTravelPlan(testRequest);
            return ResponseEntity.ok("Conexión exitosa! Plan generado para " + testPlan.getDestination());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error de conexión: " + e.getMessage());
        }
    }
}