package server;

import controller.GameController;
import utils.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class GameServer {

    private ServerSocket serverSocket;

    private final List<ClientHandler> clients =
            Collections.synchronizedList(new ArrayList<>());

    private final ExecutorService pool = Executors.newCachedThreadPool();

    private GameController gameController;
    private boolean running = false;
    private final int port;

    public GameServer(int port) {
        this.port = port;
    }

    public void start() {

        try {
            serverSocket = new ServerSocket(port);
            running = true;

            System.out.println("[SERVER] Running on port " + port);

            new Thread(this::acceptLoop).start();

        } catch (IOException e) {
            throw new RuntimeException("Failed to start server", e);
        }
    }

    private void acceptLoop() {

        while (running) {

            try {
                Socket socket = serverSocket.accept();

                ClientHandler client = new ClientHandler(socket, this);
                clients.add(client);

                pool.execute(client);

                System.out.println("[SERVER] Client connected. Total: " + clients.size());

                broadcastPlayerList();

            } catch (IOException e) {
                if (running) {
                    System.err.println("[SERVER] Accept error: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {

        running = false;

        try {

            for (ClientHandler c : new ArrayList<>(clients)) {
                c.disconnect();
            }

            if (serverSocket != null) {
                serverSocket.close();
            }

            pool.shutdown();

            System.out.println("[SERVER] Stopped");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setGameController(GameController gc) {
        this.gameController = gc;
    }

    public GameController getGameController() {
        return gameController;
    }

    public boolean isGameStarted() {
        return gameController != null;
    }

    public List<ClientHandler> getClients() {
        synchronized (clients) {
            return new ArrayList<>(clients);
        }
    }

    public int getPlayerCount() {
        return (int) clients.stream()
                .filter(c -> c.getPlayerName() != null)
                .count();
    }

    public void broadcast(Message msg) {
        synchronized (clients) {
            for (ClientHandler c : clients) {
                c.sendMessage(msg);
            }
        }
    }

    public void sendTo(String playerName, Message msg) {
        for (ClientHandler c : clients) {
            if (playerName.equals(c.getPlayerName())) {
                c.sendMessage(msg);
                return;
            }
        }
    }

    public void broadcastExcept(Message msg, ClientHandler exclude) {
        synchronized (clients) {
            for (ClientHandler c : clients) {
                if (c != exclude) {
                    c.sendMessage(msg);
                }
            }
        }
    }

    public void broadcastPlayerList() {

        List<String> names;

        synchronized (clients) {
            names = clients.stream()
                    .map(ClientHandler::getPlayerName)
                    .filter(n -> n != null)
                    .collect(Collectors.toList());
        }

        broadcast(new utils.Message(
                utils.MessageType.PLAYER_LIST,
                String.join(",", names)
        ));

        System.out.println("[SERVER] Players: " + names);
    }
    
    public void broadcastSystem(String text) {

        broadcast(
                new Message(
                        utils.MessageType.SYSTEM,
                        text
                )
        );
    }
    
    public void broadcastPhase(String phase) {

        broadcast(
                new Message(
                        utils.MessageType.PHASE_CHANGE,
                        phase
                )
        );
    }

    public void removeClient(ClientHandler client) {

        clients.remove(client);

        System.out.println("[SERVER] Client removed. Remaining: " + clients.size());

        broadcastPlayerList();
    }
}