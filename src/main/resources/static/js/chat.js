var stompClient = null;

function connect() {
    var socket = new SockJS('/chat-websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/broadcast', function (message) {
            showBroadcastMessage(JSON.parse(message.body).input);
        });

        // // Assinando a fila privada para mensagens recebidas
        // stompClient.subscribe("/user/queue/private-messages", function (message) {
        //     showPrivateMessage(JSON.parse(message.body));
        // });
    });
}

function sendBroadcastMessage() {
    var user = document.getElementById('username').value;
    var messageContent = document.getElementById('broadcastMessage').value;

    var chatInput = {
        user: user,
        message: messageContent
    };

    stompClient.send("/app/sendBroadcastMessage", {}, JSON.stringify(chatInput));
}

function showBroadcastMessage(message) {
    var messageArea = document.getElementById('broadcastMessageArea');
    var messageElement = document.createElement('li');
    messageElement.textContent = message;
    messageArea.appendChild(messageElement);
}

// function sendPrivateMessage() {
//     var toUser = document.getElementById('privateUser').value;
//     var messageContent = document.getElementById('privateMessage').value;
//
//     stompClient.send("/app/sendPrivateMessage/" + toUser, {}, JSON.stringify(messageContent));
// }
//
// function showPrivateMessage(message) {
//     var messageArea = document.getElementById('privateMessageArea');
//     var messageElement = document.createElement('li');
//     messageElement.textContent = message;
//     messageArea.appendChild(messageElement);
// }

// Chame a função connect() ao carregar a página
window.onload = connect;
