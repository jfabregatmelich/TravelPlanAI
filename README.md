# TravelPlan AI desarrollado por Joel Fabregat Melich

Aplicación web full-stack que genera itinerarios de viaje personalizados para destinos europeos mediante inteligencia artificial. El usuario introduce su destino, número de días y presupuesto, y la aplicación devuelve un plan detallado día a día —con actividades de mañana, comida y tarde, horarios de apertura y costes estimados— exportable en PDF.

---

## Tecnologías

**Backend**
- Java 17
- Spring Boot 3.2 (Spring Web, DevTools)
- Maven
- iText 5 — generación programática de PDFs con diseño personalizado
- Jackson — serialización y deserialización JSON

**Frontend**
- HTML5, CSS3 y JavaScript vanilla
- Font Awesome 6 — iconografía
- Diseño responsive adaptado a móvil

**Inteligencia Artificial**
- Google Gemini 2.5 Flash Lite API — modelo LLM para generación de itinerarios
- Integración vía HTTP REST con `RestTemplate` y prompt engineering estructurado

---

## Arquitectura y flujo de datos

```
Frontend (HTML/JS)
    → POST /api/plan/generate
        → PlanController
            → GeminiService (prompt + llamada HTTP a Gemini API)
                ← JSON estructurado del modelo
            ← TravelPlan (objeto Java)
        ← ResponseEntity<TravelPlan>
    → POST /api/pdf/generate
        → PdfController (iText 5, renderizado A4)
        ← PDF binario con cabecera Content-Disposition
```

---

## Ficheros clave

**`GeminiService.java`** — núcleo de la integración con IA. Construye un prompt detallado en castellano con las restricciones del usuario (presupuesto por día, ciudad, número de días), llama a la API de Gemini mediante `RestTemplate`, extrae el texto de la respuesta, elimina los bloques de código Markdown y parsea el JSON resultante a objetos `DailyPlan`. Si el modelo devuelve una respuesta inválida, activa un plan de respaldo (`generateMockPlan`) para garantizar que el usuario siempre recibe una respuesta.

**`PlanController.java`** — controlador REST que expone `POST /api/plan/generate`. Valida los campos de entrada (ciudad, países, días entre 1 y 30, presupuesto mayor que cero), normaliza el presupuesto total a partir del presupuesto por día si se proporciona, y gestiona el fallback automático al plan mock en caso de error de la API externa.

**`PdfController.java`** — genera el documento PDF directamente en memoria con iText 5, sin escribir ficheros temporales en disco. Implementa un motor de layout propio sobre páginas A4 (595 × 842 pt) con paleta de colores coherente con el frontend (verde oscuro `#2c5a3b`, acento dorado `#cb7b3c`, fondo crema `#fffcf5`). Devuelve el binario con cabecera `Content-Disposition: attachment`.

**`TravelRequest.java` / `TravelPlan.java`** — modelos POJO que representan la petición del usuario y el plan generado. `TravelRequest` admite tanto presupuesto total como presupuesto por día; `TravelPlan` agrega la lista de `DailyPlan` con las tres franjas horarias y sus precios.

---

## Configuración y ejecución

Clona el repositorio y añade tu clave de Gemini en `src/main/resources/application.properties`:

```properties
gemini.api.key=TU_API_KEY_AQUI
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent
```

Obtén una API key gratuita en [Google AI Studio](https://aistudio.google.com/). Luego:

```bash
./mvnw spring-boot:run
```

La aplicación arranca en `http://localhost:8080/frontend/index.html`.

---

## Ejemplo de salida

El directorio incluye `travel_plan_Valencia.pdf` como muestra de un itinerario generado para Valencia con presupuesto moderado. El PDF contiene el desglose por día con actividades, horarios y costes, maquetado con el sistema de layout propio del `PdfController`.

---

## Posibles mejoras

- Autenticación de usuarios y guardado de itinerarios en base de datos (Spring Data JPA + PostgreSQL)
- Soporte multiidioma del prompt y la interfaz
- Integración con APIs de precios reales (Booking, Google Places)
- Despliegue en contenedor Docker con variable de entorno para la API key