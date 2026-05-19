package client;

import controller.GameEventListener;
import javafx.application.Platform;
import utils.Message;
import utils.MessageType;
import utils.Player;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameClient {

    private Socket socket;

    private ObjectOutputStream outputStream;

    private ObjectInputStream inputStream;

    private boolean connected;

    private String playerName;

    private GameEventListener listener;

    public GameClient(String serverAddress, int port, String playerName) {

        this.playerName = playerName;

        try {
            socket = new Socket(serverAddress, port);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
            connected = true;

            startListening();

            sendMessage(
                    new Message(
                            MessageType.JOIN,
                            playerName,
                            ""
                    )
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setListener(GameEventListener listener) {
        this.listener = listener;
    }

    private void startListening() {

        new Thread(() -> {
            try {
                while (connected) {
                    Message message = (Message) inputStream.readObject();
                    handleMessage(message);
                }
            } catch (IOException | ClassNotFoundException e) {
                connected = false;
                e.printStackTrace();
            }
        }).start();
    }

    private void handleMessage(Message message) {

        if (listener == null) {
            return;
        }

        Platform.runLater(() -> {

            switch (message.getType()) {

                case START_GAME -> {
                    listener.onStartGame();
                }

                case PLAYER_LIST -> {
                    List<Player> players = new ArrayList<>();

                    String[] names = message.getContent().split(",");

                    for (String name : names) {
                        if (!name.isBlank()) {
                            players.add(new Player(name.trim()));
                        }
                    }

                    listener.onPlayersUpdated(players);
                }

                case SYSTEM -> {
                    listener.onMessage(message.getContent());
                }

                case SEND_SECRET -> {
                    listener.onSecretWord(message.getContent());
                }

                case IMPOSTOR -> {
                    listener.onImpostorMessage();
                }

                case SEND_WORD -> {
                    String sender = message.getSender();

                    if (sender != null) {
                        listener.onMessage(sender + ": " + message.getContent());
                    }
                }
                
                case PHASE_CHANGE -> {
                    listener.onPhaseChange(message.getContent());
                }

                case CREWMATES_WIN -> {
                    listener.onGameOver("\nCREWMATES WIN!\nThe impostor was " + message.getContent() + ".");
                }

                case IMPOSTOR_WINS -> {

                    listener.onGameOver("\nIMPOSTOR WINS!\nThe impostor was " + message.getContent() + ".");
                }
                
                case ENABLE_INPUT -> {
                    listener.onInputEnabled(MessageType.valueOf(message.getContent()));
                }

                case DISABLE_INPUT -> {
                    listener.onInputDisabled();
                }

                case ERROR -> {
                    listener.onMessage("[ERROR] " + message.getContent());
                }
            }
        });
    }

    public void sendMessage(Message message) {
        try {
            outputStream.writeObject(message);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startGame() {
        sendMessage(
                new Message(
                        MessageType.START_GAME,
                        playerName,
                        ""
                )
        );
    }

    public String getPlayerName() {
        return playerName;
    }
}