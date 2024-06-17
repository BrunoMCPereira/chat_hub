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
        console.log("scripts - Página carregada e WebSocket conectado. Utilizador:", currentUser);
    }
});


function carregarDados(nomeUtilizador) {
    carregarSalas(nomeUtilizador);
    carregarUtilizadoresOnline();
    carregarUtilizadoresOffline();
    console.log("scripts - Dados carregados para o utilizador:", nomeUtilizador);
}

function carregarSalas(nomeUtilizador) {
    console.log("scripts - Carregando salas para o utilizador:", nomeUtilizador);
    fetch('/api/chat/salas/utilizador?nomeUtilizador=' + nomeUtilizador)
        .then(response => response.json())
        .then(data => {
            atualizarListaSalas(data);
            console.log("scripts - Salas carregadas para o utilizador:", nomeUtilizador, data);
        })
        .catch(error => console.error('scripts - Erro ao carregar salas:', error));
}

function atualizarListaSalas(salas) {
    const listaSalas = document.getElementById('listaSalas');
    listaSalas.innerHTML = '';

    salas.forEach(sala => {
        const salaButton = document.createElement('button');
        salaButton.className = 'sala-button';
        salaButton.textContent = sala.nomeSala || sala;
        salaButton.onclick = () => {
            console.log(`scripts - Clicou na sala: ${sala.nomeSala || sala}`);
            currentChatRoom = sala.nomeSala || sala; // Definir a sala atual
            marcarSalaComoSelecionada(salaButton); // Marcar botão como selecionado
        };
        listaSalas.appendChild(salaButton);
    });
    console.log("scripts - Lista de salas atualizada:", salas);
}

function selecionarSala(button) {
    // Remove a classe 'selected' de todos os botões
    const buttons = document.querySelectorAll('.sala-button');
    buttons.forEach(btn => btn.classList.remove('selected'));

    // Adiciona a classe 'selected' ao botão clicado
    button.classList.add('selected');
}

function selectParticipant(participant) {
    if (!selectedParticipants.includes(participant)) {
        selectedParticipants.push(participant);
        updateParticipantsDisplay();
        document.getElementById("newChatContainer").style.display = 'block';
    }
    console.log("scripts - Participante selecionado:", participant);
}

function updateParticipantsDisplay() {
    console.log("scripts - Atualizando exibição dos participantes...");
    const participantsContainer = document.getElementById("chatParticipants");
    participantsContainer.innerHTML = '';

    selectedParticipants.forEach(participant => {
        const participantButton = document.createElement("div");
        participantButton.textContent = participant;
        participantButton.className = 'participant-button';
        participantsContainer.appendChild(participantButton);
        console.log("scripts - Participante adicionado à exibição:", participant);
    });

    console.log("scripts - Exibição dos participantes atualizada:", selectedParticipants);
}

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
                // Esconde o container de criar chat e limpa os participantes selecionados
                document.getElementById('newChatContainer').style.display = 'none';
                selectedParticipants = [];
                updateParticipantsDisplay();
                carregarSalas(currentUser); // Atualiza a lista de salas
                console.log("scripts - Chat criado com sucesso:", chatName);
            } else {
                alert("scripts - Erro ao criar chat.");
                console.error("scripts - Erro ao criar chat:", response);
            }
        });
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
            console.log("Utilizadores online API response:", utilizadores);
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
        .catch(error => console.error('scripts - Erro ao carregar utilizadores online:', error));
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
            console.log("Utilizadores offline API response:", utilizadores);
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
        .catch(error => console.error('scripts - Erro ao carregar utilizadores offline:', error));
}

function connectWebSocket(username) {
    if (ws) {
        ws.close();  // Fecha a conexão WebSocket existente corretamente.
    }
    ws = new WebSocket(`ws://${window.location.host}/ecra_dashboard?username=${username}`);
    ws.onopen = function() {
        console.log("scripts - Conectado ao WebSocket");
        ws.send(JSON.stringify({ type: "login", username }));
    };
    ws.onmessage = function(event) {
        const data = JSON.parse(event.data);
        handleWebSocketMessage(data);
    };
    ws.onerror = function(error) {
        console.error("scripts - Erro no WebSocket:", error);
    };
    ws.onclose = function() {
        console.log("scripts - WebSocket fechado, tentando reconectar em 5 segundos...");
        setTimeout(() => connectWebSocket(username), 5000);
    };
}


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
            console.error("scripts - Tipo de mensagem desconhecido:", data.tipo);
    }
}

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
            console.log("scripts - Mensagem já existe no chatHistory:", mensagem);
        }
    });

    if (!mensagemExiste) {
        const isOutgoing = mensagem.remetente === currentUser;
        const messageDiv = criarElementoMensagem(mensagem.remetente, formatarDataHora(mensagem.dataCriacao), mensagem.conteudo, isOutgoing);
        chatHistory.appendChild(messageDiv);
        chatHistory.scrollTop = chatHistory.scrollHeight;
        console.log("scripts - Nova mensagem adicionada ao chatHistory:", mensagem);
    }
}


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


function sendMessage() {
    const messageText = document.getElementById('message').value;

    if (!currentChatRoom || messageText.trim() === '') {
        alert("scripts - Por favor, selecione uma sala e digite uma mensagem válida para enviar.");
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
            console.log("scripts - Mensagens carregadas para a sala:", nomeSala, data);
        })
        .catch(error => console.error('scripts - Erro ao carregar mensagens:', error));
}



function formatarDataHora(dataHora) {
    const data = new Date(dataHora);
    const dia = String(data.getDate()).padStart(2, '0');
    const mes = String(data.getMonth() + 1).padStart(2, '0'); // Mês começa do 0
    const ano = data.getFullYear();
    const horas = String(data.getHours()).padStart(2, '0');
    const minutos = String(data.getMinutes()).padStart(2, '0');
    return `${dia}-${mes}-${ano} ${horas}:${minutos}`;
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
        .catch(error => console.error('scripts - Erro ao carregar participantes da sala:', error));
}

function marcarSalaComoSelecionada(button) {
    // Remover classe 'selected' de todos os botões
    const buttons = document.querySelectorAll('.sala-button');
    buttons.forEach(btn => btn.classList.remove('selected'));

    // Adicionar classe 'selected' ao botão clicado
    button.classList.add('selected');
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


function atualizarListaUsuarios() {
    // Implementação de atualizações para listas de usuários online e offline
    carregarUtilizadoresOnline();
    carregarUtilizadoresOffline();
}