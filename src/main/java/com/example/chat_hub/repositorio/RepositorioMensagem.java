package com.example.chat_hub.repositorio;

import com.example.chat_hub.modelo.Mensagem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositorioMensagem extends MongoRepository<Mensagem, String> {

    List<Mensagem> findByRemetente(String remetente);

    List<Mensagem> findByNomeSala(String sala);
}
