package com.example.chat_hub.config;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoZooKeeper {

    @Bean
    public ZooKeeper zooKeeper(@Value("${enderecoZooKeeper}") String enderecoZooKeeper,
            @Value("${sessionTimeout}") int sessionTimeout) {
        ZooKeeper zooKeeper = null;
        try {
            zooKeeper = new ZooKeeper(enderecoZooKeeper, sessionTimeout, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    System.out.println("Conectado ao ZooKeeper em " + enderecoZooKeeper);
                }
            });
        } catch (Exception e) {
            System.err.println("Falha ao conectar ao ZooKeeper");
            e.printStackTrace();
        }
        return zooKeeper;
    }
}
