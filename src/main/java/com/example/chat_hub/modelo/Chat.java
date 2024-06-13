package com.example.chat_hub.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "chats")
public class Chat {
    @Id
    private String id;
    private String nomeSala;
    private String criador;
    private String estado;
    private List<String> participantes = new ArrayList<>();
    private List<Mensagem> mensagens = new ArrayList<>();

    public Chat() {
    }

    // Getters e Setters

    public String getId() {
        return id;
    }

    public List<String> getParticipantes() {
        return participantes;
    }

    public void setParticipantes(List<String> participantes) {
        this.participantes = participantes;
    }

    public String getNomeSala() {
        return nomeSala;
    }

    public void setCriador(String criador) {
        this.criador = criador;
    }

    public String getCriador() {
        return criador;
    }

    public void setNomeSala(String nomeSala) {
        this.nomeSala = nomeSala;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<Mensagem> getMensagens() {
        return mensagens;
    }

    public void setMensagens(List<Mensagem> mensagens) {
        this.mensagens = mensagens;
    }

}
