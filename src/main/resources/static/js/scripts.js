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
    }
});

// Função para carregar os dados do usuário (salas, usuários online e offline)
function carregarDados(nomeUtilizador) {
    carregarSalas(nomeUtilizador);
    carregarUtilizadoresOnline();
    carregarUtilizadoresOffline();
}

// Função para carregar as salas do usuário
function carregarSalas(nomeUtilizador) {
    fetch('/api/chat/salas/utilizador?nomeUtilizador=' + nomeUtilizador)
        .then(response => response.json())
        .then(data => {
            atualizarListaSalas(data);
        })
        .catch(error => console.error('Erro ao carregar salas:', error));
}

// Função para atualizar a lista de salas na interface
function atualizarListaSalas(salas) {
    const listaSalas = document.getElementById('listaSalas');
    listaSalas.innerHTML = '';

    salas.forEach(sala => {
        const salaButton = document.createElement('button');
        salaButton.className = 'sala-button';
        salaButton.textContent = sala.nomeSala || sala;
        salaButton.onclick = () => {
            currentChatRoom = sala.nomeSala || sala; // Definir a sala atual
            marcarSalaComoSelecionada(salaButton); // Marcar botão como selecionado
        };
        listaSalas.appendChild(salaButton);
    });
}

// Função para marcar a sala selecionada
function selecionarSala(button) {
    const buttons = document.querySelectorAll('.sala-button');
    buttons.forEach(btn => btn.classList.remove('selected'));
    button.classList.add('selected');
}

// Função para selecionar um participante para um novo chat
function selectParticipant(participant) {
    if (!selectedParticipants.includes(participant)) {
        selectedParticipants.push(participant);
        updateParticipantsDisplay();
        document.getElementById("newChatContainer").style.display = 'block';
    }
}

// Função para atualizar a exibição dos participantes selecionados
function updateParticipantsDisplay() {
    const participantsContainer = document.getElementById("chatParticipants");
    participantsContainer.innerHTML = '';

    selectedParticipants.forEach(participant => {
        const participantButton = document.createElement("div");
        participantButton.textContent = participant;
        participantButton.className = 'participant-button';
        participantsContainer.appendChild(participantButton);
    });
}

// Função para criar um novo chat
function criarChat() {
    const chatName = document.getElementById("newChatName").value;
    if (chatName) {
        const payload = { chatName, currentUser, participants: selectedParticipants };
        fetch("/api/chat/criarChat", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        }).then(response => {
            if (response.ok) {
                document.getElementById('newChatContainer').style.display = 'none';
                selectedParticipants = [];
                updateParticipantsDisplay();
                carregarSalas(currentUser); // Atualiza a lista de salas
            } else {
                alert("Erro ao criar chat.");
                console.error("Erro ao criar chat:", response);
            }
        });
    }
}

// Função para carregar os utilizadores online
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

// Função para carregar os utilizadores offline
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
        })
        .catch(error => console.error('Erro ao carregar utilizadores offline:', error));
}

// Função para conectar ao WebSocket
function connectWebSocket(username) {
    if (ws) {
        ws.close();  // Fecha a conexão WebSocket existente corretamente.
    }
    ws = new WebSocket(`ws://${window.location.host}/ecra_dashboard?username=${username}`);
    ws.onopen = function() {
        ws.send(JSON.stringify({ type: "login", username }));
    };
    ws.onmessage = function(event) {
        const data = JSON.parse(event.data);
        handleWebSocketMessage(data);
    };
    ws.onerror = function(error) {
        console.error("Erro no WebSocket:", error);
    };
    ws.onclose = function() {
        setTimeout(() => connectWebSocket(username), 5000);
    };
}

// Função para lidar com mensagens recebidas pelo WebSocket
function handleWebSocketMessage(data) {
    switch (data.tipo) {
        case "nova_mensagem":
            if (data.sala === currentChatRoom) {
                adicionarMensagemAoChat(data.mensagem);
            }
            break;
        case "atualizar_usuarios":
            atualizarListaUsuarios();
            break;
        case "mudanca_sala":
            carregarSalas(currentUser);
            break;
        case "mudanca_participantes_chat":
            if (data.sala === currentChatRoom) {
                carregarParticipantesChat(currentChatRoom);
            }
            break;
        default:
            console.error("Tipo de mensagem desconhecido:", data.tipo);
    }
}

// Função para adicionar uma mensagem ao chat
function adicionarMensagemAoChat(mensagem) {
    const chatHistory = document.getElementById('chatHistory');
    let mensagemExiste = false;

    // Conferir se a mensagem já foi adicionada ao chatHistory
    Array.from(chatHistory.children).forEach(child => {
        const remetente = child.dataset.remetente;
        const conteudo = child.dataset.conteudo;
        const dataCriacao = child.dataset.dataCriacao;
        if (remetente === mensagem.remetente && conteudo === mensagem.conteudo && dataCriacao === mensagem.dataCriacao) {
            mensagemExiste = true;
        }
    });

    if (!mensagemExiste) {
        const isOutgoing = mensagem.remetente === currentUser;
        const messageDiv = criarElementoMensagem(mensagem.remetente, formatarDataHora(mensagem.dataCriacao), mensagem.conteudo, isOutgoing);
        chatHistory.appendChild(messageDiv);
        chatHistory.scrollTop = chatHistory.scrollHeight;
    }
}

// Função para criar o elemento HTML de uma mensagem
function criarElementoMensagem(remetente, dataCriacao, conteudo, isOutgoing) {
    const messageDiv = document.createElement('div');
    messageDiv.className = isOutgoing ? 'message outgoing' : 'message incoming';

    messageDiv.dataset.remetente = remetente;
    messageDiv.dataset.conteudo = conteudo;
    messageDiv.dataset.dataCriacao = dataCriacao;

    const messageHeader = document.createElement('div');
    messageHeader.className = 'message-header';
    messageHeader.textContent = `${remetente} - ${dataCriacao}`;

    const messageContent = document.createElement('div');
    messageContent.className = 'message-content';
    messageContent.textContent = conteudo;

    messageDiv.appendChild(messageHeader);
    messageDiv.appendChild(messageContent);
    return messageDiv;
}

// Função para enviar uma mensagem
function sendMessage() {
    const messageText = document.getElementById('message').value;

    if (!currentChatRoom || messageText.trim() === '') {
        alert("Por favor, selecione uma sala e digite uma mensagem válida para enviar.");
        return;
    }

    const dataCriacao = new Date().toISOString();  // ISO string para compatibilidade

    const message = {
        remetente: currentUser,
        conteudo: messageText,
        nomeSala: currentChatRoom,
        dataCriacao: dataCriacao
    };

    ws.send(JSON.stringify({ tipo: "nova_mensagem", mensagem: message, sala: currentChatRoom }));
    document.getElementById('message').value = '';

    fetch('/api/chat/mensagem', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(message)
    });
}

// Função para carregar as mensagens de uma sala
function carregarMensagens(nomeSala) {
    fetch(`/api/chat/mensagens?nomeSala=${nomeSala}`)
        .then(response => response.json())
        .then(data => {
            const chatHistory = document.getElementById('chatHistory');
            const existingMessages = new Map(); // Usar um Map para armazenar combinações de atributos de mensagem

            // Limpar o histórico de chat existente
            while (chatHistory.firstChild) {
                chatHistory.removeChild(chatHistory.firstChild);
            }

            data.forEach(mensagem => {
                const messageKey = mensagem.remetente + mensagem.dataCriacao + mensagem.conteudo;
                // Verifica se a mensagem já foi adicionada ao chatHistory
                if (!existingMessages.has(messageKey)) {
                    const isOutgoing = mensagem.remetente === currentUser;
                    const messageDiv = criarElementoMensagem(mensagem.remetente, formatarDataHora(mensagem.dataCriacao), mensagem.conteudo, isOutgoing);
                    chatHistory.appendChild(messageDiv);
                    existingMessages.set(messageKey, true); // Adiciona a combinação dos atributos ao Map
                }
            });
            chatHistory.scrollTop = chatHistory.scrollHeight;
        })
        .catch(error => console.error('Erro ao carregar mensagens:', error));
}

// Função para formatar a data e hora de uma mensagem
function formatarDataHora(dataHora) {
    const data = new Date(dataHora);
    const dia = String(data.getDate()).padStart(2, '0');
    const mes = String(data.getMonth() + 1).padStart(2, '0'); // Mês começa do 0
    const ano = data.getFullYear();
    const horas = String(data.getHours()).padStart(2, '0');
    const minutos = String(data.getMinutes()).padStart(2, '0');
    return `${dia}-${mes}-${ano} ${horas}:${minutos}`;
}

// Função para atualizar o status do usuário
function atualizarStatus(status) {
    fetch(`/estado_ligacao?nomeUtilizador=${currentUser}&estado=${status}`, {
        method: 'POST'
    });
}

// Função para carregar os participantes de uma sala de chat
function carregarParticipantesChat(nomeSala) {
    fetch(`/api/chat/participantes?nomeSala=${nomeSala}`)
        .then(response => response.json())
        .then(data => {
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

// Função para marcar a sala como selecionada
function marcarSalaComoSelecionada(button) {
    const buttons = document.querySelectorAll('.sala-button');
    buttons.forEach(btn => btn.classList.remove('selected'));
    button.classList.add('selected');
}

// Função para realizar logout
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

// Função para atualizar a lista de usuários (online e offline)
function atualizarListaUsuarios() {
    carregarUtilizadoresOnline();
    carregarUtilizadoresOffline();
}
