package com.example.chat_hub.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EleicaoLider {

    private static final Logger logger = LoggerFactory.getLogger(EleicaoLider.class);
    private static final String ELECTION_NAMESPACE = "/eleicao";
    private ZooKeeper zooKeeper;
    private String currentZNodeName;

    public EleicaoLider(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
        criarElectionNodeSeNecessario();
    }

    private void criarElectionNodeSeNecessario() {
        try {
            if (zooKeeper.exists(ELECTION_NAMESPACE, false) == null) {
                zooKeeper.create(ELECTION_NAMESPACE, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                logger.info("Nó de eleição criado: {}", ELECTION_NAMESPACE);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Erro ao criar nó de eleição", e);
        }
    }

    public void voluntariarParaLideranca() throws KeeperException, InterruptedException {
        String zNodePrefix = ELECTION_NAMESPACE + "/candidato_";
        String zNodeFullPath = zooKeeper.create(zNodePrefix, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);
        this.currentZNodeName = zNodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
        logger.info("Candidato voluntariado para liderança: {}", this.currentZNodeName);
    }

    public boolean verificarLideranca() throws KeeperException, InterruptedException {
        return verificarLiderancaRecursivamente(currentZNodeName);
    }

    private boolean verificarLiderancaRecursivamente(String candidateZNode)
            throws KeeperException, InterruptedException {
        String predecessorZNodeName = getPredecessorZNodeName(candidateZNode);
        if (predecessorZNodeName == null) {
            logger.info("Nó {} é o líder", candidateZNode);
            return true;
        }

        Stat predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZNodeName, watchedEvent -> {
            if (watchedEvent.getType() == Watcher.Event.EventType.NodeDeleted) {
                logger.info("Nó predecessor deletado: {}", predecessorZNodeName);
                try {
                    verificarLideranca();
                } catch (KeeperException | InterruptedException e) {
                    logger.error("Erro ao verificar liderança", e);
                }
            }
        });

        boolean isLeader = predecessorStat == null && verificarLiderancaRecursivamente(predecessorZNodeName);
        if (isLeader) {
            logger.info("Nó {} tornou-se o líder após verificação recursiva", candidateZNode);
        }
        return isLeader;
    }

    private String getPredecessorZNodeName(String candidateZNode) throws KeeperException, InterruptedException {
        List<String> zNodes = zooKeeper.getChildren(ELECTION_NAMESPACE, false);
        zNodes.sort(String::compareTo);

        String predecessorZNodeName = null;
        for (String zNode : zNodes) {
            if (zNode.equals(candidateZNode)) {
                break;
            }
            predecessorZNodeName = zNode;
        }

        logger.info("Predecessor do nó {} é {}", candidateZNode, predecessorZNodeName);
        return predecessorZNodeName;
    }
}
