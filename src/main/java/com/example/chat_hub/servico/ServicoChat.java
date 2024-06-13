package com.example.chat_hub.servico;

import com.example.chat_hub.modelo.Chat;
import com.example.chat_hub.modelo.Mensagem;
import com.example.chat_hub.zookeeper.GerenciadorZooKeeper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ServicoChat {

    @Autowired
    private GerenciadorZooKeeper gerenciadorZooKeeper;

    public void criarSalaDeChat(List<String> data) {
        String nomeSala = data.get(0);
        String nomeUtilizador = data.get(1);
        List<String> participantes = data.subList(2, data.size());

        // Gera o nome do chat se não for fornecido
        if (nomeSala == null || nomeSala.isEmpty() || "null".equals(nomeSala)) {
            List<Chat> chatsExistentes = gerenciadorZooKeeper.listarChatsPorCriador(nomeUtilizador);
            nomeSala = gerarNomeChat(nomeUtilizador, chatsExistentes);
        }

        // Cria o objeto Chat
        Chat novoChat = new Chat();
        novoChat.setNomeSala(nomeSala);
        novoChat.setCriador(nomeUtilizador);
        novoChat.setParticipantes(participantes);

        // Cria a sala de chat no ZooKeeper
        gerenciadorZooKeeper.criarSalaDeChat(nomeSala, participantes);

        // Salva o chat no MongoDB
        gerenciadorZooKeeper.criarChat(novoChat);
    }

    public String gerarNomeChat(String nomeUtilizador, List<Chat> chatsExistentes) {
        String prefixo = "O Chat do " + nomeUtilizador;
        int count = 0;

        for (Chat chat : chatsExistentes) {
            if (chat.getNomeSala().contains(prefixo)) {
                count++;
            }
        }

        if (count == 0) {
            return prefixo;
        } else {
            return prefixo + " n.º " + (count + 1);
        }
    }

    public List<String> carregarSalasAtivas(String nomeUtilizador) {
        // Lista de chats ativos do usuário no ZooKeeper
        return gerenciadorZooKeeper.listarSalasPorParticipante(nomeUtilizador);
    }

    public List<Mensagem> carregarMensagens(String nomeSala) {
        return gerenciadorZooKeeper.listarMensagensPorSala(nomeSala);
    }

    public void adicionarMensagem(String sala, String conteudoMensagem, String remetente) {
        // Adicionar a mensagem ao ZooKeeper
        gerenciadorZooKeeper.adicionarMensagem(sala, conteudoMensagem, remetente);

        // Criar o objeto mensagem
        Mensagem mensagem = new Mensagem();
        mensagem.setConteudo(conteudoMensagem);
        mensagem.setNomeSala(sala);
        mensagem.setRemetente(remetente);
        mensagem.setTimestamp(LocalDateTime.now());

        // Tentar enviar a mensagem e capturar possíveis exceções
        try {
            gerenciadorZooKeeper.enviarMensagem(mensagem);
            System.out.println("Mensagem adicionada na sala '" + sala + "'.");
        } catch (Exception e) { // Use Exception para capturar todas as exceções possíveis se você não sabe
                                // exatamente quais exceções são lançadas
            e.printStackTrace();
        }
    }
}