// Seleciona elementos do DOM
const connectScreen = document.getElementById('connect-screen');
const chatScreen = document.getElementById('chat-screen');
const usernameInput = document.getElementById('username');
const connectBtn = document.getElementById('connect-btn');
const userList = document.getElementById('user-list');
const chatRoom = document.getElementById('chat-room');
const messageInput = document.getElementById('message-input');
const sendBtn = document.getElementById('send-btn');
const messagesDiv = document.getElementById('messages');
const publicChatBtn = document.getElementById('public-chat-btn');
const userDisplay = document.getElementById('user');

let websocket;
let currentUser;

// Conecta ao WebSocket com o nome de usuário
connectBtn.addEventListener('click', () => {
    const username = usernameInput.value.trim();
    if (username) {
        currentUser = username;
        connectToWebSocket(username);
    }
});

function connectToWebSocket(username) {
    websocket = new WebSocket(`ws://localhost:8080/chat?username=${username}`);

    websocket.onopen = function() {
        console.log("Conectado ao WebSocket como " + username);
        userDisplay.innerText = username;
        connectScreen.style.display = 'none';
        chatScreen.style.display = 'block';
    };

    websocket.onmessage = function(event) {
        const data = JSON.parse(event.data);
        if (data.type === 'users') {
            // Atualiza a lista de usuários conectados
            updateUserList(data.users);
        } else {
            // Exibe a mensagem recebida
            displayMessage(data.sender, data.content);
        }
    };

    websocket.onclose = function() {
        console.log("Desconectado do WebSocket");
    };
}

// Atualiza a lista de usuários conectados
function updateUserList(users) {
    userList.innerHTML = '';
    users.forEach(user => {
        if (user !== currentUser) {
            const userElement = document.createElement('li');
            userElement.innerText = user;
            userElement.addEventListener('click', () => openPrivateChat(user));
            userList.appendChild(userElement);
        }
    });
}

// Abre o chat privado com um usuário
function openPrivateChat(user) {
    chatRoom.style.display = 'block';
    document.getElementById('chat-room-title').innerText = `Chat com ${user}`;

    // Enviar uma mensagem privada
    sendBtn.onclick = function() {
        const messageContent = messageInput.value.trim();
        if (messageContent) {
            const message = {
                type: 'queue',
                content: messageContent,
                sender: currentUser,
                receiver: user
            };
            websocket.send(JSON.stringify(message));
            messageInput.value = '';
            displayMessage(currentUser, messageContent);
        }
    };
}

// Abre o chat público
publicChatBtn.addEventListener('click', () => {
    chatRoom.style.display = 'block';
    document.getElementById('chat-room-title').innerText = 'Chat Público';

    sendBtn.onclick = function() {
        const messageContent = messageInput.value.trim();
        if (messageContent) {
            const message = {
                type: 'topic',
                content: messageContent,
                sender: currentUser,
                receiver: null
            };
            websocket.send(JSON.stringify(message));
            messageInput.value = '';
            displayMessage(currentUser, messageContent);
        }
    };
});

// Exibe a mensagem recebida ou enviada
function displayMessage(sender, content) {
    const messageElement = document.createElement('div');
    messageElement.innerText = `${sender}: ${content}`;
    messagesDiv.appendChild(messageElement);
    messagesDiv.scrollTop = messagesDiv.scrollHeight; // Rolagem automática para a última mensagem
}
