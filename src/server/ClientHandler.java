package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import utils.Message;
import utils.MessageType;
import utils.Player;

public class ClientHandler implements Runnable {
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
            System.err.println("Error creating streams for client: " + e.getMessage());
            connected = false;
        }
    }

    @Override
    public void run() {
        try {
            while (connected) {
                Message message = (Message) inputStream.readObject();

                if (message == null) continue;

                System.out.println("Received: " + message.getType() + " from " + message.getSender());

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
                        System.out.println("Unhandled message type: " + message.getType());
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void handleJoin(Message message) {
        String playerName = message.getSender();
        this.player = new Player(playerName);

        System.out.println("Player joined: " + playerName);

        // Notify all clients about the new player
        Message joinMessage = new Message(MessageType.JOIN, playerName, "");
        server.broadcast(joinMessage);
    }

    private void handleStartGame() {
        if (!server.isGameStarted()) {
            // Get all player names from connected clients
            String[] playerNames = server.getClients().stream()
                    .map(client -> client.getPlayerName())
                    .filter(name -> name != null)
                    .toArray(String[]::new);

            if (playerNames.length >= 3 && playerNames.length <= 4) {
                System.out.println("Starting game with players: " + String.join(", ", playerNames));

                // Create GameController
                controller.GameController gc = new controller.GameController(playerNames, server);
                server.setGameController(gc);

                // Start the game in a separate thread
                new Thread(() -> gc.startNetworkGame()).start();
            } else {
                sendMessage(new Message(MessageType.ERROR, "Need 3-4 players to start! Current: " + playerNames.length));
            }
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
            System.err.println("Error sending message to " + getPlayerName() + ": " + e.getMessage());
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error disconnecting client: " + e.getMessage());
        }
        server.removeClient(this);
    }

    public String getPlayerName() {
        return player != null ? player.getName() : null;
    }

    public Player getPlayer() {
        return player;
    }
}