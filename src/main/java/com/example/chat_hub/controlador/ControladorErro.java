package com.example.chat_hub.controlador;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador para página de erro.
 */
@Controller
public class ControladorErro implements ErrorController {

    /**
     * Mapeia a URL /error para o template "error".
     *
     * @return O nome do template da página de erro
     */
    @RequestMapping("/error")
    public String handleError() {
        // Retorna o nome do template Thymeleaf (src/main/resources/templates/error.html)
        return "error";
    }
}
