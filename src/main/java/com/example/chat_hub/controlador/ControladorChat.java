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
        logger.info("Controlador - Recebido pedido para criar chat com payload: {}", payload);

        try {
            String chatName = (String) payload.get("chatName");
            String currentUser = (String) payload.get("currentUser");
            List<String> participants = (List<String>) payload.get("participants");

            logger.info("Controlador - Dados recebidos - chatName: {}, currentUser: {}, participants: {}", chatName,
                    currentUser,
                    participants);

            if (chatName == null || chatName.isEmpty() || participants == null || participants.isEmpty()) {
                logger.error(
                        "Controlador - Nome do chat e participantes são obrigatórios. Dados inválidos - chatName: {}, participants: {}",
                        chatName, participants);
                throw new IllegalArgumentException("Controlador - Nome do chat e participantes são obrigatórios.");
            }

            // Criar o chat
            logger.info(
                    "Controlador - Chamando servicoChat.criarSalaDeChat() com chatName: {}, currentUser: {}, participants: {}",
                    chatName, currentUser, participants);
            servicoChat.criarSalaDeChat(chatName, currentUser, participants);

            logger.info("Controlador - Chat criado com sucesso - chatName: {}, currentUser: {}, participants: {}",
                    chatName,
                    currentUser, participants);
        } catch (Exception e) {
            logger.error("Controlador - Erro ao criar chat", e);
            throw new RuntimeException("Controlador - Erro ao criar chat", e);
        }
    }

    @PostMapping("/mensagem")
    public ResponseEntity<String> adicionarMensagem(@RequestBody Map<String, Object> payload) {
        String sala = (String) payload.get("nomeSala");
        String mensagem = (String) payload.get("conteudo");
        String nomeUtilizador = (String) payload.get("remetente");
        String dataCriacao = (String) payload.get("dataCriacao");

        if (sala == null || mensagem == null || nomeUtilizador == null || dataCriacao == null) {
            return ResponseEntity.badRequest().body("Parâmetros ausentes.");
        }

        try {
            servicoChat.adicionarMensagem(sala, mensagem, nomeUtilizador, dataCriacao);
            return ResponseEntity.ok("Controlador - Mensagem enviada com sucesso.");
        } catch (Exception e) {
            logger.error("Controlador - Erro ao adicionar mensagem", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Controlador - Erro ao adicionar mensagem");
        }
    }

    @PostMapping("/entrar")
    public String entrarSala(@RequestParam String sala, @RequestParam String utilizador,
            RedirectAttributes redirectAttributes) {
        gerenciadorZooKeeper.entrarSala(sala, utilizador);

        redirectAttributes.addFlashAttribute("mensagem",
                "Controlador - Utilizador '" + utilizador + "' entrou na sala '" + sala + "'.");
        redirectAttributes.addFlashAttribute("mensagemTipo", "sucesso");

        return "redirect:/chat"; // Redireciona para a página de chat
    }

    @PostMapping("/sair")
    public String sairSala(@RequestParam String sala, @RequestParam String utilizador,
            RedirectAttributes redirectAttributes) {
        gerenciadorZooKeeper.sairSala(sala, utilizador);

        redirectAttributes.addFlashAttribute("mensagem",
                "Controlador - Utilizador '" + utilizador + "' saiu da sala '" + sala + "'.");
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
    public ResponseEntity<List<String>> getUtilizadoresOnline() {
        List<String> utilizadoresOnline = servicoUtilizador.listarNomesUtilizadoresPorStatus("online");
        logger.info("Utilizadores online: " + utilizadoresOnline);
        return ResponseEntity.ok(utilizadoresOnline);
    }

    @GetMapping("/utilizadores/offline")
    public ResponseEntity<List<String>> getUtilizadoresOffline() {
        List<String> utilizadoresOffline = servicoUtilizador.listarNomesUtilizadoresPorStatus("offline");
        logger.info("Utilizadores offline: " + utilizadoresOffline);
        return ResponseEntity.ok(utilizadoresOffline);
    }

    @GetMapping("/salas/utilizador")
    public List<String> listarSalasAtiva(String nomeUtilizador) {
        return servicoChat.carregarSalasAtivas(nomeUtilizador);
    }

    @GetMapping("/mensagens")
    public List<Mensagem> listarMensagens(@RequestParam String nomeSala) {
        return servicoChat.carregarMensagens(nomeSala);

    }

}
