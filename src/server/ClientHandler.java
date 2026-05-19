package server;

import controller.GameController;
import utils.Message;
import utils.MessageType;
import utils.Player;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final GameServer server;
    private Player player;
    private boolean connected = true;
    private boolean disconnecting = false;

    public ClientHandler(Socket socket, GameServer server) {

        this.socket = socket;
        this.server = server;

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            connected = false;
        }
    }

    @Override
    public void run() {

        try {
            while (connected) {

                Object obj = in.readObject();

                if (!(obj instanceof Message msg)) continue;

                route(msg);
            }

        } catch (Exception e) {
            disconnect();
        }
    }

    private void route(Message msg) {

        GameController gc = server.getGameController();

        switch (msg.getType()) {

            case JOIN -> handleJoin(msg);

            case START_GAME -> startGame();

            case SEND_WORD -> {
                if (gc != null) gc.receiveWord(msg.getSender(), msg.getContent());
            }

            case VOTE -> {
                if (gc != null) gc.receiveVote(msg.getSender(), msg.getContent());
            }

            case SEND_STATEMENT -> {
                System.out.println("[SERVER] Statement: " + msg.getContent());
                gc.receiveStatement(msg.getSender(), msg.getContent());
            }

            case PLAYER_LIST -> server.broadcastPlayerList();
            
            case SYSTEM -> {
                server.broadcast(msg);
            }

            case ERROR -> {
                server.sendTo(msg.getSender(), msg);
            }

            default -> System.out.println("[SERVER] Unhandled: " + msg.getType());
        }
    }

    private void handleJoin(Message msg) {

        String name = msg.getSender();

        if (name == null || name.isBlank()) {
            sendMessage(new Message(MessageType.ERROR, "Invalid name"));
            return;
        }

        player = new Player(name);

        sendMessage(new Message(MessageType.SYSTEM, "Welcome " + name));

        server.broadcastExcept(
                new Message(MessageType.SYSTEM, name + " joined"),
                this
        );

        server.broadcastPlayerList();
    }

    private void startGame() {

        if (player == null) return;

        if (server.isGameStarted()) {
            sendMessage(new Message(MessageType.ERROR, "Already started"));
            return;
        }

        String[] names = server.getClients().stream()
                .map(ClientHandler::getPlayerName)
                .filter(n -> n != null)
                .toArray(String[]::new);

        if (names.length < 3 || names.length > 4) {
            sendMessage(new Message(MessageType.ERROR, "Need 3-4 players"));
            return;
        }

        GameController gc = new GameController(names, server);
        server.setGameController(gc);

        server.broadcast(new Message(MessageType.START_GAME, "start"));

        new Thread(gc::startGame).start();
    }

    public void sendMessage(Message msg) {

        try {
            if (out != null && connected) {
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) {
            disconnect();
        }
    }

    public void disconnect() {

        if (disconnecting) return;
        disconnecting = true;

        connected = false;

        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}

        server.removeClient(this);
    }

    public String getPlayerName() {
        return player != null ? player.getName() : null;
    }
}