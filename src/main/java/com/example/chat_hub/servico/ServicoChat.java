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

    public void criarSalaDeChat(String chatName, String currentUser, List<String> participants) {
        // Adicionar o currentUser aos participantes
        if (!participants.contains(currentUser)) {
            participants.add(currentUser);
        }

        // Gera o nome do chat se não for fornecido
        if (chatName == null || chatName.isEmpty() || "null".equals(chatName)) {
            List<Chat> chatsExistentes = gerenciadorZooKeeper.listarChatsPorCriador(currentUser);
            chatName = gerarNomeChat(currentUser, chatsExistentes);
        }

        // Cria o objeto Chat
        Chat novoChat = new Chat();
        novoChat.setNomeSala(chatName);
        novoChat.setCriador(currentUser);
        novoChat.setParticipantes(participants);

        // Cria a sala de chat no ZooKeeper
        gerenciadorZooKeeper.criarSalaDeChat(chatName, participants);

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

    public void adicionarMensagem(String sala, String conteudoMensagem, String remetente, String dataCriacao) {
        // Adicionar a mensagem ao ZooKeeper
        gerenciadorZooKeeper.adicionarMensagem(sala, conteudoMensagem, remetente, dataCriacao);

        // Criar o objeto mensagem
        Mensagem mensagem = new Mensagem();
        mensagem.setConteudo(conteudoMensagem);
        mensagem.setNomeSala(sala);
        mensagem.setRemetente(remetente);
        mensagem.setDataCriacao(dataCriacao);

        // Tentar enviar a mensagem e capturar possíveis exceções
        try {
            gerenciadorZooKeeper.enviarMensagem(mensagem);
            System.out.println("ServicoChat - Mensagem adicionada na sala '" + sala + "'.");
        } catch (Exception e) { // Use Exception para capturar todas as exceções possíveis se você não sabe
                                // exatamente quais exceções são lançadas
            e.printStackTrace();
        }
    }

}
