package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import controller.GameController;
import utils.Message;
import utils.MessageType;

public class GameServer {
    //Accepts all socket connections and hosts the game by creating the ClientHandler instance
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private ExecutorService threadPool;
    private GameController gameController;
    private boolean isRunning;
    private int port;

    public GameServer(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.isRunning = false;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            System.out.println("=================================");
            System.out.println("Guess the Impostor - Game Server");
            System.out.println("Server started on port " + port);
            System.out.println("Waiting for players to connect...");
            System.out.println("=================================");

            // Accept client connections in a separate thread
            new Thread(() -> {
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("[SERVER] New client connected: " + clientSocket.getInetAddress());

                        ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                        clients.add(clientHandler);
                        threadPool.execute(clientHandler);

                    } catch (IOException e) {
                        if (isRunning) {
                            System.err.println("[SERVER] Error accepting client connection: " + e.getMessage());
                        }
                    }
                }
            }).start();

        } catch (IOException e) {
            System.err.println("[SERVER] Failed to start server: " + e.getMessage());
        }
    }

    public void stop() {
        isRunning = false;
        try {
            // Disconnect all clients
            for (ClientHandler client : clients) {
                client.disconnect();
            }

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            threadPool.shutdown();
            System.out.println("[SERVER] Game Server stopped");

        } catch (IOException e) {
            System.err.println("[SERVER] Error stopping server: " + e.getMessage());
        }
    }

    // Check if a name is already taken
    public boolean isNameTaken(String name) {
        return clients.stream()
                .filter(client -> client.getPlayerName() != null)
                .anyMatch(client -> client.getPlayerName().equalsIgnoreCase(name));
    }

    // Broadcast current player list to all clients
    public void broadcastPlayerList() {
        List<String> playerNames = clients.stream()
                .map(ClientHandler::getPlayerName)
                .filter(name -> name != null)
                .collect(Collectors.toList());

        String playerList = String.join(",", playerNames);
        Message playerListMsg = new Message(MessageType.PLAYER_LIST, "PLAYER_LIST", playerList);
        broadcast(playerListMsg);

        System.out.println("[SERVER] Current players (" + playerNames.size() + "): " + String.join(", ", playerNames));
    }

    public void broadcast(Message message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void broadcastExcept(Message message, ClientHandler exclude) {
        for (ClientHandler client : clients) {
            if (client != exclude) {
                client.sendMessage(message);
            }
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("[SERVER] Client disconnected. Total clients: " + clients.size());
        broadcastPlayerList();
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }

    public GameController getGameController() {
        return gameController;
    }

    public boolean isGameStarted() {
        return gameController != null;
    }

    public int getPlayerCount() {
        return (int) clients.stream().filter(c -> c.getPlayerName() != null).count();
    }

    public static void main(String[] args) {
        int port = 12345;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Using default 12345");
            }
        }
        GameServer server = new GameServer(port);
        server.start();
    }
}