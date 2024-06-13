let ws;
let currentUser;
let currentChatRoom;
let selectedParticipants = [];

document.addEventListener("DOMContentLoaded", function() {
    const urlParams = new URLSearchParams(window.location.search);
    currentUser = urlParams.get('nomeUtilizador');
    const nomeUtilizadorElement = document.getElementById("nomeUtilizador");
    if (nomeUtilizadorElement && currentUser) {
        nomeUtilizadorElement.textContent = currentUser;
        carregarDados(currentUser);
        connectWebSocket(currentUser);
        atualizarStatus("online");
        console.log("Página carregada e WebSocket conectado. Utilizador:", currentUser);
    }
});

function carregarDados(nomeUtilizador) {
    carregarSalas(nomeUtilizador);
    carregarUtilizadoresOnline();
    carregarUtilizadoresOffline();
    console.log("Dados carregados para o utilizador:", nomeUtilizador);
}

function carregarSalas(nomeUtilizador) {
    fetch('/api/chat/salas/utilizador?nomeUtilizador=' + nomeUtilizador)
        .then(response => response.json())
        .then(data => {
            atualizarListaSalas(data);
            console.log("Salas carregadas para o utilizador:", nomeUtilizador, data);
        })
        .catch(error => console.error('Erro ao carregar salas:', error));
}

function atualizarListaSalas(salas) {
    const listaSalas = document.getElementById('listaSalas');
    listaSalas.innerHTML = '';

    salas.forEach(sala => {
        const salaDiv = document.createElement('div');
        salaDiv.className = 'sala';
        salaDiv.textContent = sala;
        salaDiv.onclick = () => {
            carregarMensagens(sala);
            carregarParticipantesChat(sala);
        };
        listaSalas.appendChild(salaDiv);
    });
    console.log("Lista de salas atualizada:", salas);
}

function selectParticipant(participant) {
    if (!selectedParticipants.includes(participant)) {
        selectedParticipants.push(participant);
        updateParticipantsDisplay();
        document.getElementById("newChatContainer").style.display = 'block';
    }
    console.log("Participante selecionado:", participant);
}

function updateParticipantsDisplay() {
    const participantsContainer = document.getElementById("chatParticipants");
    participantsContainer.innerHTML = '';

    const currentUserButton = document.createElement("div");
    currentUserButton.textContent = currentUser;
    currentUserButton.className = 'participant-button';
    participantsContainer.appendChild(currentUserButton);

    selectedParticipants.forEach(participant => {
        const participantButton = document.createElement("div");
        participantButton.textContent = participant;
        participantButton.className = 'participant-button';
        participantsContainer.appendChild(participantButton);
    });
    console.log("Participantes exibidos:", selectedParticipants);
}

function criarChat() {
    const chatName = document.getElementById("newChatName").value;
    if (chatName) {
        const payload = { chatName, currentUser, participants: selectedParticipants };
        console.log("Iniciando criação de chat com payload:", payload);

        fetch("/api/chat/criarChat", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        }).then(response => {
            if (response.ok) {
                console.log("Chat criado com sucesso:", chatName);
                // Carregar dados novamente para atualizar a lista de salas
                carregarSalas(currentUser);
            } else {
                alert("Erro ao criar chat.");
                console.error("Erro ao criar chat:", response);
            }
        }).catch(error => {
            console.error("Erro na requisição de criação de chat:", error);
        });
    } else {
        console.warn("Nome do chat não fornecido.");
    }
}


function carregarUtilizadoresOnline() {
    fetch('/api/chat/utilizadores/online')
        .then(response => {
            if (!response.ok) {
                throw new Error(`Erro na rede ao tentar carregar utilizadores online: ${response.statusText}`);
            }
            return response.json();
        })
        .then(utilizadores => {
            const utilizadoresOnlineContainer = document.getElementById("utilizadoresOnline");
            utilizadoresOnlineContainer.innerHTML = ''; // Limpar container antes de adicionar novos elementos
            utilizadores.forEach(utilizador => {
                if (utilizador !== currentUser) {
                    const button = document.createElement("button");
                    button.textContent = utilizador;
                    button.className = "user-button online";
                    button.onclick = () => selectParticipant(utilizador);
                    utilizadoresOnlineContainer.appendChild(button);
                }
            });
        })
        .catch(error => console.error('Erro ao carregar utilizadores online:', error));
}

function carregarUtilizadoresOffline() {
    fetch('/api/chat/utilizadores/offline')
        .then(response => {
            if (!response.ok) {
                throw new Error(`Erro na rede ao tentar carregar utilizadores offline: ${response.statusText}`);
            }
            return response.json();
        })
        .then(utilizadores => {
            const utilizadoresOfflineContainer = document.getElementById("utilizadoresOffline");
            utilizadoresOfflineContainer.innerHTML = ''; // Limpar container antes de adicionar novos elementos
            utilizadores.forEach(utilizador => {
                if (utilizador !== currentUser) {
                    const button = document.createElement("button");
                    button.textContent = utilizador;
                    button.className = "user-button offline";
                    button.onclick = () => selectParticipant(utilizador);
                    utilizadoresOfflineContainer.appendChild(button);
                }
            });
            console.log("Utilizadores offline carregados:", utilizadores);
        })
        .catch(error => console.error('Erro ao carregar utilizadores offline:', error));
}

function connectWebSocket(username) {
    if (ws) {
        ws.close();
    }
    ws = new WebSocket("ws://" + window.location.host + "/ecra_dashboard?username=" + username);
    ws.onopen = function() {
        console.log("Conectado ao WebSocket");
        ws.send(JSON.stringify({ type: "login", username: username }));
    };
    ws.onmessage = function(event) {
        const data = JSON.parse(event.data);
        handleWebSocketMessage(data);
    };
    ws.onerror = function(error) {
        console.error("Erro no WebSocket:", error);
    };
    ws.onclose = function() {
        console.log("WebSocket fechado, tentando reconectar em 5 segundos...");
        setTimeout(() => connectWebSocket(username), 5000);
    };
}

function handleWebSocketMessage(data) {
    const tipo = data.tipo;
    const mensagem = data.mensagem;
    const sala = data.sala;

    console.log("Mensagem recebida do WebSocket:", data); // Log para depuração

    switch (tipo) {
        case "mudanca_estado_utilizadores":
            carregarUtilizadoresOnline();
            carregarUtilizadoresOffline();
            break;
        case "mudanca_participantes_chat":
            if (sala === currentChatRoom) {
                carregarParticipantesChat(currentChatRoom);
            }
            break;
        case "nova_mensagem_chat":
            if (sala === currentChatRoom) {
                const chatHistory = document.getElementById('chatHistory');
                const messageDiv = document.createElement('div');
                messageDiv.className = mensagem.remetente === currentUser ? 'message outgoing' : 'message incoming';
                messageDiv.textContent = `${mensagem.remetente}: ${mensagem.conteudo}`;
                chatHistory.appendChild(messageDiv);
                chatHistory.scrollTop = chatHistory.scrollHeight;
            }
            break;
        case "mudanca_sala":
            carregarSalas(currentUser);
            break;
        default:
            console.error("Tipo de mensagem desconhecido:", tipo);
    }
}

function sendMessage() {
    const messageText = document.getElementById('message').value;
    const chatHistory = document.getElementById('chatHistory');
    const outgoingMessage = document.createElement('div');
    outgoingMessage.className = 'message outgoing';
    outgoingMessage.textContent = messageText;
    chatHistory.appendChild(outgoingMessage);
    chatHistory.scrollTop = chatHistory.scrollHeight;

    const message = { remetente: currentUser, conteudo: messageText, nomeSala: currentChatRoom };
    ws.send(JSON.stringify({ tipo: "nova_mensagem_chat", mensagem: message, sala: currentChatRoom }));
    document.getElementById('message').value = '';

    fetch('/api/chat/mensagem', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(message)
    });
}

function carregarMensagens(nomeSala) {
    currentChatRoom = nomeSala;
    fetch(`/api/chat/mensagens?nomeSala=${nomeSala}`)
        .then(response => response.json())
        .then(data => {
            const chatHistory = document.getElementById('chatHistory');
            chatHistory.innerHTML = '';
            data.forEach(mensagem => {
                const messageDiv = document.createElement('div');
                messageDiv.className = mensagem.remetente === currentUser ? 'message outgoing' : 'message incoming';
                messageDiv.textContent = `${mensagem.remetente}: ${mensagem.conteudo}`;
                chatHistory.appendChild(messageDiv);
            });
            chatHistory.scrollTop = chatHistory.scrollHeight;
            console.log("Mensagens carregadas para a sala:", nomeSala, data);
        })
        .catch(error => console.error('Erro ao carregar mensagens:', error));
}

function atualizarStatus(status) {
    fetch(`/estado_ligacao?nomeUtilizador=${currentUser}&estado=${status}`, {
        method: 'POST'
    });
}

function carregarParticipantesChat(nomeSala) {
    fetch(`/api/chat/participantes?nomeSala=${nomeSala}`)
        .then(response => response.json())
        .then(data => {
            // Atualize a lista de participantes na interface do usuário
            const participantsContainer = document.getElementById('chatParticipants');
            participantsContainer.innerHTML = '';
            data.forEach(participante => {
                const participantDiv = document.createElement('div');
                participantDiv.className = 'participant';
                participantDiv.textContent = participante;
                participantsContainer.appendChild(participantDiv);
            });
        })
        .catch(error => console.error('Erro ao carregar participantes da sala:', error));
}

function logout() {
    fetch(`/estado_ligacao?nomeUtilizador=${currentUser}&estado=offline`, {
        method: 'POST'
    })
    .then(response => {
        window.location.href = '/';
    })
    .catch(error => {
        window.location.href = '/';
    });
}
