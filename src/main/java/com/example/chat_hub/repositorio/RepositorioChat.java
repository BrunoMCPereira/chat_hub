package com.example.chat_hub.repositorio;

import com.example.chat_hub.modelo.Chat;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RepositorioChat extends MongoRepository<Chat, String> {

    List<Chat> findByNomeSala(String nomeSala);

    List<Chat> findByParticipantesContaining(String nomeParticipante);

    List<Chat> findByParticipantesIn(List<String> participantes);

    List<Chat> findByCriador(String criador);
}
