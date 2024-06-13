package com.example.chat_hub.controlador;

import com.example.chat_hub.servico.ServicoUtilizador;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

@Controller
public class ControladorLogin {

    @Autowired
    private ServicoUtilizador servicoUtilizador;

    @GetMapping("/login")
    public String mostrarPaginaDeLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String nomeUtilizador,
            @RequestParam String senha,
            RedirectAttributes redirectAttributes) {
        boolean isValidUser = servicoUtilizador.validarUsuario(nomeUtilizador, senha);
        if (isValidUser) {
            servicoUtilizador.alterarEstado(nomeUtilizador, "online");
            redirectAttributes.addFlashAttribute("nomeUtilizador", nomeUtilizador);
            return "redirect:/dashboard?nomeUtilizador=" + nomeUtilizador;
        }
        redirectAttributes.addFlashAttribute("mensagem", "Credenciais inválidas. Por favor, tente novamente.");
        redirectAttributes.addFlashAttribute("mensagemTipo", "erro");
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String mostrarDashboard(@RequestParam String nomeUtilizador, Model model) {
        model.addAttribute("nomeUtilizador", nomeUtilizador);
        return "dashboard";
    }
}
