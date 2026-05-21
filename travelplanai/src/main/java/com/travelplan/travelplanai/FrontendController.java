package com.travelplan.travelplanai;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    // Funcion que redirige la raiz al archivo index.html del frontend
    @GetMapping("/")
    public String serveFrontend() {
        return "forward:/frontend/index.html";
    }
}
