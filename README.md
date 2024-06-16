Chat Hub
Descrição

O Chat Hub é uma aplicação de mensagens em tempo real que utiliza Apache ZooKeeper para gerenciar estados de usuários, salas de chat e mensagens. Este projeto foi desenvolvido para demonstrar o uso de ZooKeeper tanto em modo standalone quanto em um ambiente de cluster, fornecendo exemplos de configuração e lógica para ambos os casos.
Branches
Branch Master

    Descrição: Esta branch foi programada para funcionar em um nó znode em standalone mode. Toda a lógica e operações foram configuradas para um ambiente de nó único.
    Uso: Ideal para testes e desenvolvimento local onde a simplicidade e a configuração de um único nó são suficientes.

Branch Cluster

    Descrição: Esta branch foi desenvolvida para funcionar em um ambiente com múltiplos nós, formando um cluster. Nessa configuração, a eleição de líder é parametrizada para gerenciar as operações de escrita. Para as operações de leitura, foi implementado um sistema de round robin, que percorre os elementos constituintes do cluster para distribuir as requisições de leitura de forma equilibrada.
    Uso: Recomendado para ambientes de produção onde alta disponibilidade e balanceamento de carga são necessários.

Funcionalidades

    Gerenciamento de Usuários: Registro, atualização e remoção de usuários.
    Salas de Chat: Criação, listagem e gerenciamento de participantes em salas de chat.
    Mensagens em Tempo Real: Envio e recepção de mensagens em tempo real utilizando WebSockets.
    Monitoramento: Utilização de watchers do ZooKeeper para monitorar mudanças nos estados dos nós e notificações em tempo real.

Configuração e Execução
Pré-requisitos

    Java 11+
    Apache ZooKeeper
    MongoDB
    Gradle

Configuração
Clone o repositório:
git clone https://github.com/seu-usuario/chat-hub.git
cd chat-hub

Escolha a branch desejada:
git checkout master  # Para modo standalone
# ou
git checkout cluster  # Para ambiente de cluster
Configure o arquivo application.properties conforme necessário.

Execução
Compile o projeto:
gradlew clean build

Inicie a aplicação:
gradlew bootRun

Estrutura do Projeto

    config: Configurações de WebSocket e ZooKeeper.
    modelo: Modelos de dados para usuários, chats e mensagens.
    repositorio: Interfaces de repositório para MongoDB.
    servico: Implementações dos serviços principais da aplicação.
    zookeeper: Classes para gerenciamento do ZooKeeper, incluindo eleição de líder e monitoramento de nós.
