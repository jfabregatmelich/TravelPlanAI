package com.travelplan.travelplanai.controller;

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
            System.out.println("Recibida peticion para: " + request.getCity());
            TravelPlan plan = geminiService.generateTravelPlan(request);
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generando el plan: " + e.getMessage());
        }
    }
}