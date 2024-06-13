package com.example.chat_hub.controlador;

import com.example.chat_hub.modelo.Mensagem;
import com.example.chat_hub.servico.ServicoChat;
import com.example.chat_hub.servico.ServicoUtilizador;
import com.example.chat_hub.zookeeper.GerenciadorZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ControladorChat {

    @Autowired
    private ServicoChat servicoChat;

    @Autowired
    private ServicoUtilizador servicoUtilizador;

    @Autowired
    private GerenciadorZooKeeper gerenciadorZooKeeper;

    @PostMapping("/criarChat")
    public String criarSala(@RequestBody String payload, RedirectAttributes redirectAttributes) {
        List<String> data = List.of(payload.split(","));
        servicoChat.criarSalaDeChat(data);

        redirectAttributes.addFlashAttribute("mensagem", "Criou o chat com sucesso.");
        redirectAttributes.addFlashAttribute("mensagemTipo", "sucesso");

        return "redirect:/chat"; // Redireciona para a página de chat
    }

    @PostMapping("/mensagem")
    public String adicionarMensagem(@RequestParam String sala, @RequestParam String mensagem,
            @RequestParam String nomeUtilizador, RedirectAttributes redirectAttributes) {
        gerenciadorZooKeeper.adicionarMensagem(sala, mensagem, nomeUtilizador);

        redirectAttributes.addFlashAttribute("mensagem", "Mensagem enviada para a sala '" + sala + "'.");
        redirectAttributes.addFlashAttribute("mensagemTipo", "sucesso");

        return "redirect:/chat"; // Redireciona para a página de chat
    }

    @PostMapping("/entrar")
    public String entrarSala(@RequestParam String sala, @RequestParam String utilizador,
            RedirectAttributes redirectAttributes) {
        gerenciadorZooKeeper.entrarSala(sala, utilizador);

        redirectAttributes.addFlashAttribute("mensagem",
                "Utilizador '" + utilizador + "' entrou na sala '" + sala + "'.");
        redirectAttributes.addFlashAttribute("mensagemTipo", "sucesso");

        return "redirect:/chat"; // Redireciona para a página de chat
    }

    @PostMapping("/sair")
    public String sairSala(@RequestParam String sala, @RequestParam String utilizador,
            RedirectAttributes redirectAttributes) {
        gerenciadorZooKeeper.sairSala(sala, utilizador);

        redirectAttributes.addFlashAttribute("mensagem",
                "Utilizador '" + utilizador + "' saiu da sala '" + sala + "'.");
        redirectAttributes.addFlashAttribute("mensagemTipo", "erro");

        return "redirect:/chat"; // Redireciona para a página de chat
    }

    @PostMapping("/login")
    public String login(@RequestParam String utilizador, RedirectAttributes redirectAttributes) {
        servicoUtilizador.alterarEstado(utilizador, "online");

        redirectAttributes.addFlashAttribute("mensagem", "Utilizador '" + utilizador + "' está agora online.");
        redirectAttributes.addFlashAttribute("mensagemTipo", "sucesso");

        return "redirect:/chat"; // Redireciona para a página de chat
    }

    @PostMapping("/logout")
    public String logout(@RequestParam String utilizador, RedirectAttributes redirectAttributes) {
        servicoUtilizador.alterarEstado(utilizador, "offline");

        redirectAttributes.addFlashAttribute("mensagem", "Até à próxima, " + utilizador + "!");
        redirectAttributes.addFlashAttribute("mensagemTipo", "sucesso");

        return "redirect:/login"; // Redireciona para a página de login
    }

    @GetMapping("/utilizadores/online")
    public List<String> listarUtilizadoresOnline() {
        return servicoUtilizador.listarNomesUtilizadoresPorStatus("online");
    }

    @GetMapping("/utilizadores/offline")
    public List<String> listarUtilizadoresOffline() {
        return servicoUtilizador.listarNomesUtilizadoresPorStatus("offline");
    }

    @GetMapping("/salas/utilizador")
    public List<String> listarSalasAtiva(String nomeUtilizador) {
        return servicoChat.carregarSalasAtivas(nomeUtilizador);
    }

    @GetMapping("/api/chat/mensagens")
    public List<Mensagem> listarMensagens(@RequestParam String nomeSala) {
        return servicoChat.carregarMensagens(nomeSala);

    }

}
