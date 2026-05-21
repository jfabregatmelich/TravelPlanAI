package com.travelplan.travelplanai.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        String geminiResponse = callGeminiApi(prompt);
        return parseGeminiResponse(geminiResponse, request);
    }

    // Construye el prompt para enviar a Gemini con los datos del usuario
    private String buildPrompt(TravelRequest request) {
        String countriesStr = String.join(", ", request.getCountries());
        
        return """
            Eres un experto planificador de viajes por Europa.
            
            DATOS DEL VIAJE:
            - Paises a visitar: %s
            - Ciudad principal: %s
            - Numero de dias: %d
            - Presupuesto total: %d EUR
            
            INSTRUCCIONES:
            Genera un plan de viaje detallado para %d dias en %s.
            Para CADA dia debes incluir:
            1. Actividad para la MAÑANA (un lugar turistico o actividad)
            2. Lugar recomendado para COMER al mediodia (nombre de restaurante o tipo de comida)
            3. Actividad para la TARDE (otro lugar turistico o actividad)
            
            RESPONDE UNICAMENTE con un JSON en este formato exacto:
            {
                "days": [
                    {
                        "day": 1,
                        "morning": "descripcion de actividad de manana",
                        "lunch": "nombre o descripcion del lugar para comer",
                        "afternoon": "descripcion de actividad de tarde"
                    }
                ]
            }
            
            IMPORTANTE: 
            - Adapta las recomendaciones al presupuesto de %d EUR
            - Las actividades deben ser realistas y famosas en %s
            - Responde SOLO el JSON, sin texto adicional
            """.formatted(countriesStr, request.getCity(), request.getDays(), request.getBudget(),
                          request.getDays(), request.getCity(), request.getBudget(), request.getCity());
    }

    // Envia la peticion HTTP a la API de Google Gemini
    private String callGeminiApi(String prompt) throws Exception {
        String url = apiUrl + "?key=" + apiKey;
        
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
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

    // Extrae solo el texto de la respuesta de Gemini (elimina el JSON envolvente)
    private String extractTextFromGeminiResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
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
        
        // Limpiar posibles marcadores de codigo markdown
        text = text.replace("```json", "").replace("```", "").trim();
        return text;
    }

    // Parsea la respuesta JSON de Gemini y la convierte en un objeto TravelPlan
    private TravelPlan parseGeminiResponse(String jsonResponse, TravelRequest request) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode daysArray = root.path("days");
        
        List<DailyPlan> itinerary = new ArrayList<>();
        
        for (JsonNode dayNode : daysArray) {
            int day = dayNode.path("day").asInt();
            String morning = dayNode.path("morning").asText();
            String lunch = dayNode.path("lunch").asText();
            String afternoon = dayNode.path("afternoon").asText();
            
            itinerary.add(new DailyPlan(day, morning, lunch, afternoon));
        }
        
        TravelPlan plan = new TravelPlan();
        plan.setDestination(request.getCity());
        plan.setTotalDays(request.getDays());
        plan.setTotalBudget(request.getBudget());
        plan.setItinerary(itinerary);
        
        return plan;
    }
}