package com.example.chat_hub.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "utilizador")
public class Utilizador {

    @Id
    private String id;
    private String nomeUtilizador;
    private String senha;
    private String status;

    // Construtor padrão
    public Utilizador() {
    }

    // Getters e setters
    public String getId() {
        return id;
    }

    public String getNomeUtilizador() {
        return nomeUtilizador;
    }

    public void setNomeUtilizador(String nomeUtilizador) {
        this.nomeUtilizador = nomeUtilizador;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}


