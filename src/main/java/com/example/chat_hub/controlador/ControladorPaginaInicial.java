package com.example.chat_hub.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador para a página inicial do sistema de chat.
 */
@Controller
public class ControladorPaginaInicial {

    /**
     * Mapeia a URL raiz ("/") para o template "index".
     *
     * @return O nome do template da página inicial
     */
    @GetMapping("/")
    public String mostrarPaginaInicial() {
        return "index"; // Nome do template Thymeleaf (src/main/resources/templates/index.html)
    }

    @PostMapping("/logout")
    public String logout(@RequestParam String utilizador, RedirectAttributes redirectAttributes) {
        // Lógica para alterar o estado do utilizador para offline
        redirectAttributes.addFlashAttribute("mensagem", "Adeus e até à próxima, " + utilizador + "!");
        redirectAttributes.addFlashAttribute("mensagemTipo", "sucesso");
        return "redirect:/";
    }

}
