package com.example.chat_hub.zookeeper;

import com.example.chat_hub.config.ConfiguracaoWebSocket;
import com.example.chat_hub.modelo.Chat;
import com.example.chat_hub.modelo.Mensagem;
import com.example.chat_hub.modelo.Utilizador;
import com.example.chat_hub.repositorio.RepositorioChat;
import com.example.chat_hub.repositorio.RepositorioMensagem;
import com.example.chat_hub.repositorio.RepositorioUtilizador;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class GerenciadorZooKeeper implements Watcher {

    private ZooKeeper zooKeeper;
    private String zookeeperServer;
    private int sessionTimeout;
    private ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(GerenciadorZooKeeper.class);

    @Autowired
    private RepositorioUtilizador repositorioUtilizador;

    @Autowired
    private RepositorioChat repositorioChat;

    @Autowired
    private RepositorioMensagem repositorioMensagem;

    @Autowired
    private ConfiguracaoWebSocket configuracaoWebSocket;

    @Autowired
    public GerenciadorZooKeeper(@Value("${enderecoZooKeeper}") String zookeeperServer,
            @Value("${sessionTimeout}") int sessionTimeout) {
        this.zookeeperServer = zookeeperServer;
        this.sessionTimeout = sessionTimeout;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // Registrando módulo para suporte a Java Time
    }

    @PostConstruct
    public void init() {
        conectar();
        resetarBasesDeDadosNoZooKeeper();
        resetarBasesDeDadosMongoDB();
    }

    // Conecta ao servidor ZooKeeper
    private void conectar() {
        try {
            this.zooKeeper = new ZooKeeper(zookeeperServer, sessionTimeout, this);
        } catch (IOException e) {
            logger.error("Erro de IO ao conectar ao servidor ZooKeeper", e);
        }
    }

    // Reconecta ao servidor ZooKeeper em caso de falha
    private void reconectar() {
        try {
            this.zooKeeper.close();
            conectar();
        } catch (InterruptedException e) {
            logger.error("Erro ao reconectar ao ZooKeeper", e);
        }
    }

    // Lida com eventos do ZooKeeper
    @Override
    public void process(WatchedEvent event) {
        if (event.getState() == Event.KeeperState.Expired) {
            reconectar();
        } else if (event.getState() == Event.KeeperState.SyncConnected) {
            rearmarWatchers();
        }
    }

    // Rearma os watchers após reconexão
    private void rearmarWatchers() {
        monitorarUtilizadores();
        List<String> salas = listarSalas();
        for (String sala : salas) {
            monitorarSala(sala);
            monitorarMensagens(sala);
        }
        List<String> utilizadores = listarUtilizadores();
        for (String utilizador : utilizadores) {
            monitorarEstadoUtilizador(utilizador);
            List<String> salasPorUtilizador = listarSalasPorParticipante(utilizador);
            for (String sala : salasPorUtilizador) {
                monitorarSala(sala);
                monitorarMensagens(sala);
            }
        }
    }

    // Cria as bases de dados no ZooKeeper
    private void criarBasesDeDadosNoZooKeeper() {
        try {
            if (zooKeeper.exists("/utilizadores", false) == null) {
                zooKeeper.create("/utilizadores", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            if (zooKeeper.exists("/chats", false) == null) {
                zooKeeper.create("/chats", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao criar bases de dados no ZooKeeper", e);
        }
    }

    // Limpa as bases de dados no ZooKeeper
    private void limparBasesDeDadosNoZooKeeper() {
        try {
            if (zooKeeper.exists("/utilizadores", false) != null) {
                List<String> utilizadores = zooKeeper.getChildren("/utilizadores", false);
                for (String utilizador : utilizadores) {
                    zooKeeper.delete("/utilizadores/" + utilizador + "/estado", -1);
                    zooKeeper.delete("/utilizadores/" + utilizador, -1);
                }
                zooKeeper.delete("/utilizadores", -1);
            }
            if (zooKeeper.exists("/chats", false) != null) {
                List<String> chats = zooKeeper.getChildren("/chats", false);
                for (String chat : chats) {
                    List<String> mensagens = zooKeeper.getChildren("/chats/" + chat + "/mensagens", false);
                    for (String mensagem : mensagens) {
                        zooKeeper.delete("/chats/" + chat + "/mensagens/" + mensagem, -1);
                    }
                    List<String> participantes = zooKeeper.getChildren("/chats/" + chat + "/participantes", false);
                    for (String participante : participantes) {
                        zooKeeper.delete("/chats/" + chat + "/participantes/" + participante, -1);
                    }
                    zooKeeper.delete("/chats/" + chat + "/mensagens", -1);
                    zooKeeper.delete("/chats/" + chat + "/participantes", -1);
                    zooKeeper.delete("/chats/" + chat, -1);
                }
                zooKeeper.delete("/chats", -1);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao limpar bases de dados no ZooKeeper", e);
        }
    }

    // Reseta as bases de dados no ZooKeeper
    private void resetarBasesDeDadosNoZooKeeper() {
        limparBasesDeDadosNoZooKeeper();
        criarBasesDeDadosNoZooKeeper();
    }

    // Limpa as bases de dados no MongoDB
    private void limparBasesDeDadosMongoDB() {
        repositorioUtilizador.deleteAll();
        repositorioChat.deleteAll();
        repositorioMensagem.deleteAll();
    }

    // Reseta as bases de dados no MongoDB
    private void resetarBasesDeDadosMongoDB() {
        limparBasesDeDadosMongoDB();
    }

    // -------------------------- MONGODB ---------------------------------//
    // Funções para operações de escrita no MongoDB

    public void registrarUsuario(Utilizador utilizador) {
        repositorioUtilizador.save(utilizador);
    }

    public void atualizarUsuario(Utilizador utilizador) {
        repositorioUtilizador.save(utilizador);
    }

    public void apagarUsuario(Utilizador utilizador) {
        repositorioUtilizador.delete(utilizador);
    }

    public void alterarEstadoUsuario(String nomeUtilizador, String estado) {
        List<Utilizador> utilizadores = repositorioUtilizador.findByNomeUtilizador(nomeUtilizador);
        if (!utilizadores.isEmpty()) {
            Utilizador utilizador = utilizadores.get(0);
            utilizador.setStatus(estado);
            repositorioUtilizador.save(utilizador);
        }
    }

    public void criarChat(Chat chat) {
        repositorioChat.save(chat);
    }

    public void enviarMensagem(Mensagem mensagem) {
        repositorioMensagem.save(mensagem);
    }

    public void guardarMensagem(Mensagem mensagem) {
        repositorioMensagem.save(mensagem);
    }

    // -------------------------- Leitura ---------------------------------//
    // Funções para operações de leitura no MongoDB

    public boolean validarUsuario(String nomeUtilizador, String senha) {
        List<Utilizador> utilizadores = repositorioUtilizador.findByNomeUtilizador(nomeUtilizador);
        if (!utilizadores.isEmpty()) {
            Utilizador utilizador = utilizadores.get(0);
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            return passwordEncoder.matches(senha, utilizador.getSenha());
        }
        return false;
    }

    public List<Chat> listarChatsPorCriador(String criador) {
        return repositorioChat.findByCriador(criador);
    }

    public List<Chat> listarChatsPorParticipantes(String participante1, String participante2) {
        return repositorioChat.findByParticipantesIn(List.of(participante1, participante2));
    }

    public List<Chat> listarChatsPorParticipante(String participante) {
        return repositorioChat.findByParticipantesContaining(participante);
    }

    public List<Mensagem> listarMensagensPorNomeSala(String nomeSala) {
        return repositorioMensagem.findByNomeSala(nomeSala);
    }

    public List<Utilizador> listarUtilizadoresPorStatus(String estado) {
        return repositorioUtilizador.findByStatus(estado);
    }

    public List<Utilizador> listarUtilizadoresPorNome(String nomeUtilizador) {
        return repositorioUtilizador.findByNomeUtilizador(nomeUtilizador);
    }

    public Utilizador buscarUsuario(String nomeUtilizador) {
        List<Utilizador> utilizadores = repositorioUtilizador.findByNomeUtilizador(nomeUtilizador);
        return utilizadores.isEmpty() ? null : utilizadores.get(0);
    }

    public boolean existeUsuario(String nomeUtilizador) {
        List<Utilizador> utilizadores = repositorioUtilizador.findByNomeUtilizador(nomeUtilizador);
        return !utilizadores.isEmpty();
    }

    public List<Chat> listarChats() {
        return repositorioChat.findAll();
    }

    public List<Mensagem> listarMensagens(String nomeSala) {
        return repositorioMensagem.findByNomeSala(nomeSala);
    }

    // ------------------------ ZOOKEEPER -------------------------------//
    // Funções para operações de escrita no ZooKeeper

    // Registra um novo usuário no ZooKeeper
    public void registrarNovoUsuario(String nomeUtilizador) {
        String userPath = "/utilizadores/" + nomeUtilizador;
        String statePath = userPath + "/estado";
        try {
            zooKeeper.create(userPath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zooKeeper.create(statePath, "offline".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            configuracaoWebSocket.notificarMudancaEstadoUtilizadores(); // Notifica todos os clientes
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao registrar novo utilizador " + nomeUtilizador, e);
        }
    }

    // Atualiza o estado do usuário no ZooKeeper
    public void atualizarEstadoUtilizador(String nomeUtilizador, String estado) {
        String statePath = "/utilizadores/" + nomeUtilizador + "/estado";
        try {
            if (zooKeeper.exists(statePath, false) != null) {
                zooKeeper.setData(statePath, estado.getBytes(), -1);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao atualizar estado do utilizador no ZooKeeper", e);
        }
    }

    // Apaga um usuário do ZooKeeper
    public void apagarUtilizador(String nomeUtilizador) {
        String path = "/utilizadores/" + nomeUtilizador;
        try {
            if (zooKeeper.exists(path, false) != null) {
                zooKeeper.delete(path + "/estado", -1);
                zooKeeper.delete(path, -1);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao apagar utilizador no ZooKeeper", e);
        }
    }

    // Cria uma nova sala de chat no ZooKeeper
    public void criarSalaDeChat(String nomeSala, List<String> participantes) {
        String path = "/chats/" + nomeSala;
        try {
            if (zooKeeper.exists(path, false) == null) {
                zooKeeper.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                zooKeeper.create(path + "/mensagens", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                zooKeeper.create(path + "/participantes", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
                for (String participante : participantes) {
                    String participantePath = path + "/participantes/" + participante;
                    zooKeeper.create(participantePath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao criar sala de chat no ZooKeeper", e);
        }
    }

    // Faz um usuário entrar em uma sala no ZooKeeper
    public void entrarSala(String sala, String utilizador) {
        String path = String.format("/chats/%s/participantes/%s", sala, utilizador);
        try {
            zooKeeper.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            notificarParticipantesDaSala(sala); // Notificar participantes da entrada na sala
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao entrar na sala no ZooKeeper", e);
        }
    }

    // Faz um usuário sair de uma sala no ZooKeeper
    public void sairSala(String sala, String utilizador) {
        String path = String.format("/chats/%s/participantes/%s", sala, utilizador);
        try {
            if (zooKeeper.exists(path, false) != null) {
                zooKeeper.delete(path, -1);
                notificarParticipantesDaSala(sala); // Notificar participantes da saída da sala
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao sair da sala no ZooKeeper", e);
        }
    }

    // Adiciona uma mensagem a uma sala no ZooKeeper
    public void adicionarMensagem(String sala, String conteudoMensagem, String remetente, String dataCriacao) {
        String path = String.format("/chats/%s/mensagens/mensagem_", sala);
        try {
            Mensagem mensagem = new Mensagem();
            mensagem.setConteudo(conteudoMensagem);
            mensagem.setNomeSala(sala);
            mensagem.setRemetente(remetente);
            mensagem.setDataCriacao(dataCriacao);

            String mensagemJson = objectMapper.writeValueAsString(mensagem);

            String createdPath = zooKeeper.create(path, mensagemJson.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT_SEQUENTIAL);

            // Remover a linha abaixo para evitar duplicação de notificações
            // configuracaoWebSocket.enviarNotificacaoNovaMensagem(sala, mensagem);
        } catch (KeeperException | InterruptedException | IOException e) {
            logger.error("Erro ao adicionar mensagem no ZooKeeper", e);
        }
    }

    // -------------------------- Leitura -------------------------------//
    // Funções para operações de leitura no ZooKeeper

    public List<String> listarSalasPorParticipante(String nomeUtilizador) {
        String path = "/chats";
        List<String> salas = new ArrayList<>();
        try {
            Stat stat = zooKeeper.exists(path, false);
            if (stat != null) {
                List<String> todasSalas = zooKeeper.getChildren(path, false);
                for (String sala : todasSalas) {
                    String participantesPath = path + "/" + sala + "/participantes";
                    List<String> participantes = zooKeeper.getChildren(participantesPath, false);
                    if (participantes.contains(nomeUtilizador)) {
                        salas.add(sala);
                    }
                }
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao listar salas por participante no ZooKeeper", e);
        }
        return salas;
    }

    public List<String> listarUtilizadoresPorStatusZook(String estado) {
        String path = "/utilizadores";
        List<String> utilizadores = new ArrayList<>();
        try {
            List<String> nomes = zooKeeper.getChildren(path, false);
            for (String nome : nomes) {
                byte[] data = zooKeeper.getData(path + "/" + nome + "/estado", false, null);
                String estadoUtilizador = new String(data);
                if (estadoUtilizador.equals(estado)) {
                    utilizadores.add(nome);
                }
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao listar utilizadores por status no ZooKeeper", e);
        }
        return utilizadores;
    }

    public List<Mensagem> listarMensagensPorSala(String sala) {
        String path = String.format("/chats/%s/mensagens", sala);
        List<Mensagem> mensagens = new ArrayList<>();
        try {
            List<String> children = zooKeeper.getChildren(path, false);
            children.sort(String::compareTo);
            for (String child : children) {
                byte[] data = zooKeeper.getData(path + "/" + child, false, null);
                String conteudo = new String(data);
                Mensagem mensagem = objectMapper.readValue(conteudo, Mensagem.class);
                mensagem.setId(child);
                mensagem.setNomeSala(sala);
                mensagens.add(mensagem);
            }
        } catch (KeeperException | InterruptedException | IOException e) {
            logger.error("Erro ao listar mensagens por sala no ZooKeeper", e);
        }
        return mensagens;
    }

    // -------------------------- Notificações -------------------------------//
    // Notifica os participantes da sala sobre mudanças

    private void notificarParticipantesDaSala(String sala) {
        String path = String.format("/chats/%s/participantes", sala);
        try {
            List<String> participantes = zooKeeper.getChildren(path, false);
            for (String participante : participantes) {
                configuracaoWebSocket.notificarClientes("mudanca_participantes_chat", sala, null);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao notificar participantes da sala no ZooKeeper", e);
        }
    }

    // Notifica sobre novas mensagens na sala
    private void notificarMensagensDaSala(String sala, Mensagem mensagem) {
        String path = String.format("/chats/%s/participantes", sala);
        try {
            List<String> participantes = zooKeeper.getChildren(path, false);
            for (String participante : participantes) {
                configuracaoWebSocket.notificarClientes("nova_mensagem", sala, mensagem);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao notificar sobre novas mensagens no ZooKeeper", e);
        }
    }

    public List<String> listarSalas() {
        String path = "/chats";
        List<String> salas = new ArrayList<>();
        try {
            Stat stat = zooKeeper.exists(path, false);
            if (stat != null) {
                salas = zooKeeper.getChildren(path, false);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao listar salas no ZooKeeper", e);
        }
        return salas;
    }

    // Monitoramento de utilizadores no ZooKeeper
    public void monitorarUtilizadores() {
        String path = "/utilizadores";
        try {
            Stat stat = zooKeeper.exists(path, true); // Adicionando watcher no path
            if (stat != null) {
                List<String> utilizadores = zooKeeper.getChildren(path, event -> {
                    if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                        configuracaoWebSocket.notificarMudancaEstadoUtilizadores();
                        monitorarUtilizadores(); // Rearmar o watcher após um evento
                    }
                });
                utilizadores.forEach(this::monitorarEstadoUtilizador);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Falha ao monitorar utilizadores", e);
        }
    }

    // Monitoramento do estado dos utilizadores
    public void monitorarEstadoUtilizador(String utilizador) {
        String path = "/utilizadores/" + utilizador + "/estado";
        try {
            // Adiciona um watcher permanente ao estado do utilizador
            Stat stat = zooKeeper.exists(path, true);
            if (stat != null) {
                zooKeeper.getData(path, event -> {
                    if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {
                        configuracaoWebSocket.notificarMudancaEstadoUtilizadores();
                        monitorarEstadoUtilizador(utilizador); // Rearma o watcher
                    }
                }, null);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao monitorar estado do utilizador " + utilizador, e);
        }
    }

    // Monitoramento de uma sala no ZooKeeper
    public void monitorarSala(String sala) {
        String path = "/chats/" + sala + "/participantes";
        try {
            Stat stat = zooKeeper.exists(path, false);
            if (stat != null) {
                zooKeeper.getChildren(path, event -> {
                    if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                        configuracaoWebSocket.notificarClientes("mudanca_participantes_chat", sala);
                        monitorarSala(sala);
                    }
                });
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao monitorar sala: " + sala, e);
        }
    }

    // Monitoramento de mensagens em uma sala no ZooKeeper
    public void monitorarMensagens(String sala) {
        String path = "/chats/" + sala + "/mensagens";
        try {
            Stat stat = zooKeeper.exists(path, true); // Garante que o watcher é persistente
            if (stat != null) {
                zooKeeper.getChildren(path, new Watcher() {
                    public void process(WatchedEvent event) {
                        if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                            // Tentativa de recuperar a última mensagem adicionada e notificar
                            try {
                                List<String> children = zooKeeper.getChildren(path, false);
                                if (!children.isEmpty()) {
                                    String lastChild = children.get(children.size() - 1);
                                    byte[] data = zooKeeper.getData(path + "/" + lastChild, false, null);
                                    Mensagem mensagem = objectMapper.readValue(new String(data), Mensagem.class);
                                    configuracaoWebSocket.enviarNotificacaoNovaMensagem(sala, mensagem);
                                }
                            } catch (KeeperException | InterruptedException | IOException e) {
                                logger.error("Erro ao notificar sobre nova mensagem na sala: " + sala, e);
                            }
                            monitorarMensagens(sala); // Rearma o watcher
                        }
                    }
                });
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao monitorar mensagens na sala: " + sala, e);
        }
    }

    // Lista todos os utilizadores no ZooKeeper
    public List<String> listarUtilizadores() {
        String path = "/utilizadores";
        List<String> utilizadores = new ArrayList<>();
        try {
            Stat stat = zooKeeper.exists(path, false);
            if (stat != null) {
                utilizadores = zooKeeper.getChildren(path, false);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao listar utilizadores no ZooKeeper", e);
        }
        return utilizadores;
    }

}
