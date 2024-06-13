package com.example.chat_hub.config;

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
        logger.info("Mensagem recebida: " + payload);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> msgData = objectMapper.readValue(payload, HashMap.class);
            String type = msgData.get("type");
            String username = msgData.get("username");

            if ("login".equals(type) && username != null) {
                sessions.put(username, session);
                session.getAttributes().put("username", username);
                logger.info("Utilizador com login: " + username);
                logger.info("Gravado" + session.getAttributes().get("username"));
            }
        } catch (IOException e) {
            logger.error("Falha ao analisar mensagem: " + payload, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            sessions.remove(username);
            logger.info("Utilizador desconectado: " + username);
        } else {
            sessions.values().remove(session);
            logger.info("Sessão WebSocket desconectada: " + session.getId());
        }
    }

    public void notificarMudancaEstadoUtilizadores() {
        logger.info("Notificando mudança de estado dos utilizadores");
        if (sessions.isEmpty()) {
            logger.warn("Nenhuma sessão WebSocket ativa encontrada");
        }
        sessions.values().removeIf(session -> !session.isOpen()); // Remover sessões fechadas
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    logger.info("Enviando mensagem de notificação para a sessão: {}", session.getId());
                    session.sendMessage(new TextMessage("{\"tipo\":\"mudanca_estado_utilizadores\"}"));
                } catch (IOException e) {
                    logger.error("Erro ao enviar mensagem de notificação: ", e);
                }
            } else {
                logger.warn("Sessão WebSocket fechada encontrada: " + session.getId());
            }
        }
    }

    public void notificarClientes(String tipo, String sala) {
        logger.info("Notificando clientes do tipo: " + tipo + " na sala: " + sala);
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    String payload = "{\"tipo\":\"" + tipo + "\",\"sala\":\"" + sala + "\"}";
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException e) {
                    logger.error("Erro ao enviar mensagem de notificação: ", e);
                }
            }
        }
    }
}
