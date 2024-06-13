package com.example.chat_hub.repositorio;

import com.example.chat_hub.modelo.Utilizador;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RepositorioUtilizador extends MongoRepository<Utilizador, String> {
    List<Utilizador> findByNomeUtilizador(String nomeUtilizador);

    List<Utilizador> findByStatus(String status);
}
