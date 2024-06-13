package com.example.chat_hub.controlador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.chat_hub.modelo.Mensagem;
import com.example.chat_hub.servico.ServicoChat;
import com.example.chat_hub.servico.ServicoUtilizador;
import com.example.chat_hub.zookeeper.GerenciadorZooKeeper;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ControladorChat {

    @Autowired
    private ServicoChat servicoChat;

    @Autowired
    private ServicoUtilizador servicoUtilizador;

    @Autowired
    private GerenciadorZooKeeper gerenciadorZooKeeper;

    private static final Logger logger = LoggerFactory.getLogger(ControladorChat.class);

    @PostMapping("/criarChat")
    public void criarSala(@RequestBody Map<String, Object> payload) {
        logger.info("Recebido pedido para criar chat com payload: {}", payload);

        try {
            String chatName = (String) payload.get("chatName");
            String currentUser = (String) payload.get("currentUser");
            List<String> participants = (List<String>) payload.get("participants");

            logger.info("Dados recebidos - chatName: {}, currentUser: {}, participants: {}", chatName, currentUser,
                    participants);

            if (chatName == null || chatName.isEmpty() || participants == null || participants.isEmpty()) {
                logger.error(
                        "Nome do chat e participantes são obrigatórios. Dados inválidos - chatName: {}, participants: {}",
                        chatName, participants);
                throw new IllegalArgumentException("Nome do chat e participantes são obrigatórios.");
            }

            // Criar o chat
            logger.info("Chamando servicoChat.criarSalaDeChat() com chatName: {}, currentUser: {}, participants: {}",
                    chatName, currentUser, participants);
            servicoChat.criarSalaDeChat(chatName, currentUser, participants);

            logger.info("Chat criado com sucesso - chatName: {}, currentUser: {}, participants: {}", chatName,
                    currentUser, participants);
        } catch (Exception e) {
            logger.error("Erro ao criar chat", e);
            throw new RuntimeException("Erro ao criar chat", e);
        }
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
