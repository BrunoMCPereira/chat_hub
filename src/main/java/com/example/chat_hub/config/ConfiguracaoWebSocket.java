package com.example.chat_hub.config;

import com.example.chat_hub.modelo.Mensagem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSocket
public class ConfiguracaoWebSocket extends TextWebSocketHandler implements WebSocketConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(ConfiguracaoWebSocket.class);
    private Map<String, WebSocketSession> sessions = new HashMap<>();
    private ObjectMapper objectMapper;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(this, "/ecra_dashboard").setAllowedOrigins("*");
        logger.info("WebSocket handler registado em /ecra_dashboard");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if ("username".equals(pair[0]) && pair.length > 1) {
                    String username = pair[1];
                    sessions.put(session.getId(), session);
                    logger.info("Nova conexão WebSocket estabelecida, sessionId: " + session.getId() + ", username: "
                            + username);
                }
            }
        } else {
            sessions.put(session.getId(), session);
            logger.warn("Username não fornecido na conexão WebSocket, sessionId: " + session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.info("WebS - Mensagem recebida: " + payload);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> msgData = objectMapper.readValue(payload, HashMap.class);
            String type = msgData.get("type");
            String username = msgData.get("username");

            if ("login".equals(type) && username != null) {
                sessions.put(username, session);
                session.getAttributes().put("username", username);
                logger.info("WebS - Utilizador com login: " + username);
                logger.info("WebS - Gravado" + session.getAttributes().get("username"));
            }
        } catch (IOException e) {
            logger.error("WebS - Falha ao analisar mensagem: " + payload, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            sessions.remove(username);
            logger.info("WebS - Utilizador desconectado: " + username);
        } else {
            sessions.values().remove(session);
            logger.info("WebS - Sessão WebSocket desconectada: " + session.getId());
        }
    }

    public void notificarMudancaEstadoUtilizadores() {
        logger.info("WebS - Notificando todos os clientes para atualizar estados dos usuários.");
        String payload = "{\"tipo\":\"atualizar_usuarios\"}";
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException e) {
                    logger.error("WebS - Erro ao enviar mensagem de atualização de usuários", e);
                }
            }
        }
    }

    public void notificarClientes(String tipo, String sala, Mensagem mensagem) {
        logger.info("WebS - Notificando clientes do tipo: " + tipo + (sala != null ? " na sala: " + sala : ""));
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    String payload;
                    if (mensagem != null) {
                        payload = "{\"tipo\":\"" + tipo + "\",\"sala\":\"" + sala + "\",\"mensagem\":"
                                + objectMapper.writeValueAsString(mensagem) + "}";
                    } else if (sala != null) {
                        payload = "{\"tipo\":\"" + tipo + "\",\"sala\":\"" + sala + "\"}";
                    } else {
                        payload = "{\"tipo\":\"" + tipo + "\"}";
                    }
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException e) {
                    logger.error("WebS - Erro ao enviar mensagem de notificação: ", e);
                }
            }
        }
    }

    public void notificarClientes(String tipo, String sala) {
        notificarClientes(tipo, sala, null);
    }

    // Ajuste da função de notificação de mudança de sala
    public void notificarClientesSobreMudancaDeSala(String sala) {
        logger.info("WebS - Notificando clientes sobre mudança na sala: " + sala);
        notificarClientes("mudanca_sala", sala, null);
    }

    public void enviarNotificacaoNovaMensagem(String sala, Mensagem mensagem) {
        String payload = String.format(
                "{\"tipo\":\"nova_mensagem\",\"sala\":\"%s\",\"mensagem\":{\"remetente\":\"%s\",\"conteudo\":\"%s\",\"dataCriacao\":\"%s\"}}",
                sala, mensagem.getRemetente(), mensagem.getConteudo(), mensagem.getDataCriacao());

        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                    logger.info("WebS - Mensagem de nova mensagem enviada para sala: " + sala);
                } catch (IOException e) {
                    logger.error("Erro ao enviar mensagem de notificação", e);
                }
            }
        }
    }

}
