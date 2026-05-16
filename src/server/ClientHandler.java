package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import utils.Message;
import utils.MessageType;
import utils.Player;

public class ClientHandler implements Runnable {
    //Manages the connected client. One instance is created for each player
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private GameServer server;
    private Player player;
    private boolean connected;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
        this.connected = true;

        try {
            this.outputStream = new ObjectOutputStream(socket.getOutputStream());
            this.inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.err.println("[ClientHandler] Error creating streams: " + e.getMessage());
            connected = false;
        }
    }

    @Override
    public void run() {
        try {
            while (connected) {
                Message message = (Message) inputStream.readObject();

                if (message == null) continue;

                System.out.println("[SERVER] Received: " + message.getType() + " from " + message.getSender());

                switch (message.getType()) {
                    case JOIN:
                        handleJoin(message);
                        break;
                    case START_GAME:
                        handleStartGame();
                        break;
                    case SEND_WORD:
                        handleSendWord(message);
                        break;
                    case VOTE:
                        handleVote(message);
                        break;
                    case SEND_STATEMENT:
                        handleStatement(message);
                        break;
                    default:
                        System.out.println("[SERVER] Unhandled message type: " + message.getType());
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (connected) {
                System.err.println("[ClientHandler] Error for " + getPlayerName() + ": " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    private void handleJoin(Message message) {
        String requestedName = message.getSender();

        // Check if name is valid
        if (requestedName == null || requestedName.trim().isEmpty()) {
            sendMessage(new Message(MessageType.ERROR, "Name cannot be empty!"));
            return;
        }

        // Check if name is already taken
        if (server.isNameTaken(requestedName)) {
            System.out.println("[SERVER] Name '" + requestedName + "' rejected - already taken");
            sendMessage(new Message(MessageType.NAME_TAKEN, "Name '" + requestedName + "' is already taken. Please choose another name."));
            return;
        }

        // Check if game already started
        if (server.isGameStarted()) {
            sendMessage(new Message(MessageType.ERROR, "Game already in progress. Cannot join."));
            return;
        }

        // Name is unique, accept it
        this.player = new Player(requestedName);
        System.out.println("[SERVER] Player '" + requestedName + "' joined the game!");

        // Send acceptance message
        sendMessage(new Message(MessageType.NAME_ACCEPTED, "Welcome " + requestedName + "!"));

        // Notify all other clients about the new player
        Message joinMessage = new Message(MessageType.JOIN, requestedName, "joined the game");
        server.broadcastExcept(joinMessage, this);

        // Send updated player list to all clients
        server.broadcastPlayerList();
    }

    private void handleStartGame() {
        if (player == null) {
            sendMessage(new Message(MessageType.ERROR, "You must join first!"));
            return;
        }

        if (!server.isGameStarted()) {
            // Get all player names from connected clients
            String[] playerNames = server.getClients().stream()
                    .map(ClientHandler::getPlayerName)
                    .filter(name -> name != null)
                    .toArray(String[]::new);

            int playerCount = playerNames.length;

            if (playerCount >= 3 && playerCount <= 4) {
                System.out.println("[SERVER] Starting game with players: " + String.join(", ", playerNames));

                // Notify all clients that game is starting
                server.broadcast(new Message(MessageType.START_GAME, "Game is starting!"));

                // Create GameController
                controller.GameController gc = new controller.GameController(playerNames, server);
                server.setGameController(gc);

                // Start the game in a separate thread
                new Thread(() -> gc.startNetworkGame()).start();
            } else {
                String errorMsg = "Need 3-4 players to start! Current: " + playerCount;
                System.out.println("[SERVER] " + errorMsg);
                sendMessage(new Message(MessageType.ERROR, errorMsg));
            }
        } else {
            sendMessage(new Message(MessageType.ERROR, "Game already started!"));
        }
    }

    private void handleSendWord(Message message) {
        if (server.getGameController() != null) {
            server.getGameController().receiveWord(message.getSender(), message.getContent());
        }
    }

    private void handleVote(Message message) {
        if (server.getGameController() != null) {
            server.getGameController().receiveVote(message.getSender(), message.getContent());
        }
    }

    private void handleStatement(Message message) {
        if (server.getGameController() != null) {
            server.getGameController().receiveStatement(message.getSender(), message.getContent());
        }
    }

    public void sendMessage(Message message) {
        try {
            if (outputStream != null && connected) {
                outputStream.writeObject(message);
                outputStream.flush();
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Error sending message to " + getPlayerName() + ": " + e.getMessage());
        }
    }

    public void disconnect() {
        if (connected) {
            connected = false;

            // Notify other players that this player disconnected
            if (player != null && !server.isGameStarted()) {
                Message disconnectMsg = new Message(MessageType.JOIN, player.getName(), "has left the game");
                server.broadcast(disconnectMsg);
                server.broadcastPlayerList();
                System.out.println("[SERVER] Player '" + player.getName() + "' disconnected");
            }

            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("[SERVER] Error disconnecting client: " + e.getMessage());
            }
            server.removeClient(this);
        }
    }

    public String getPlayerName() {
        return player != null ? player.getName() : null;
    }

    public Player getPlayer() {
        return player;
    }
}