package com.example.chat_hub.servico;

import com.example.chat_hub.modelo.Mensagem;
import com.example.chat_hub.zookeeper.GerenciadorZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicoMensagem {

    @Autowired
    private GerenciadorZooKeeper gerenciadorZooKeeper;

    /**
     * Salva uma mensagem no MongoDB através do ZooKeeper
     * @param mensagem O objeto Mensagem a ser salvo
     */
    public void guardarMensagem(Mensagem mensagem) {
        gerenciadorZooKeeper.guardarMensagem(mensagem);
    }

    /**
     * Carrega todas as mensagens de uma sala específica
     * @param nomeSala O nome da sala de onde as mensagens serão carregadas
     * @return A lista de mensagens na sala
     */
    public List<Mensagem> carregarMensagens(String nomeSala) {
        return gerenciadorZooKeeper.listarMensagens(nomeSala);
    }
}
