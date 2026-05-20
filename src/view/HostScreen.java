package view;

import client.GameClient;
import controller.GameEventListener;
import controller.ScreenController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import utils.MessageType;
import utils.Player;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;

public class HostScreen implements GameEventListener {

    private static final double DESIGN_WIDTH = 1920;
    private static final double DESIGN_HEIGHT = 1080;

    private final ScreenController screenController;
    private final String playerName;

    private int playerCount = 0;

    private final Text playerCountText = new Text("1");
    private final Text ipText = new Text("localhost");

    private Button startGameButton;

    public HostScreen(ScreenController screenController, String playerName) {
        this.screenController = screenController;
        this.playerName = playerName;

        screenController.setActiveScreen(this);
    }

    public Parent createContent() {

        StackPane root = new StackPane();
        StackPane gameRoot = new StackPane();
        gameRoot.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);

        Group scaledGroup = new Group(gameRoot);

        // TEXTS
        Text ipLabel = new Text("Your IP Address");
        ipLabel.setFill(Color.WHITE);
        ipLabel.setFont(Font.font("Arial", FontWeight.BOLD, 40));

        ipText.setText(getLanIp());
        ipText.setFill(Color.YELLOW);
        ipText.setFont(Font.font("Courier New", FontWeight.BOLD, 70));

        Text joinedPlayersLabel = new Text("Joined Players");
        joinedPlayersLabel.setFill(Color.WHITE);
        joinedPlayersLabel.setFont(Font.font("Arial", FontWeight.BOLD, 40));

        playerCountText.setFill(Color.YELLOW);
        playerCountText.setFont(Font.font("Courier New", FontWeight.BOLD, 70));

        // BUTTONS
        startGameButton = new Button();

        ImageView startBtnImage = new ImageView(
                new Image(getClass().getResource("/assets/startGameBtn.png").toExternalForm())
        );

        startBtnImage.setFitWidth(320);
        startBtnImage.setPreserveRatio(true);

        startGameButton.setGraphic(startBtnImage);
        startGameButton.setStyle("-fx-background-color: transparent;");

        // DISABLE START BUTTON INITIALLY
        updateStartButtonState();

        Button backButton = new Button();

        ImageView backBtnImage = new ImageView(
                new Image(getClass().getResource("/assets/backBtn.png").toExternalForm())
        );

        backBtnImage.setFitWidth(220);
        backBtnImage.setPreserveRatio(true);

        backButton.setGraphic(backBtnImage);
        backButton.setStyle("-fx-background-color: transparent;");

        // LAYOUT
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);

        VBox.setMargin(ipText, new Insets(0, 0, 60, 0));
        VBox.setMargin(playerCountText, new Insets(0, 0, 60, 0));

        contentBox.getChildren().addAll(
                ipLabel,
                ipText,
                joinedPlayersLabel,
                playerCountText,
                startGameButton,
                backButton
        );

        gameRoot.getChildren().add(contentBox);
        root.getChildren().add(scaledGroup);

        root.sceneProperty().addListener((obs, oldScene, newScene) -> {

            if (newScene != null) {

                scaleUI(newScene.getWidth(), newScene.getHeight(), scaledGroup);

                newScene.widthProperty().addListener((o, ov, nv) ->
                        scaleUI(newScene.getWidth(), newScene.getHeight(), scaledGroup));

                newScene.heightProperty().addListener((o, ov, nv) ->
                        scaleUI(newScene.getWidth(), newScene.getHeight(), scaledGroup));
            }
        });

        // BUTTON EVENTS
        backButton.setOnAction(e -> {
            if (screenController.getServer() != null) {
                screenController.getServer().stop();
            }
            screenController.showTitleScreen();
        });

        startGameButton.setOnAction(e -> {

            if (playerCount < 3) {
                return;
            }

            GameClient client = screenController.getClient();

            if (client != null) {
                screenController.showWaitingScreen();
                client.startGame();
            }
        });

        return root;
    }

    // PLAYER COUNT UPDATE
    @Override
    public void onPlayersUpdated(List<Player> players) {
        playerCount = players.size();
        playerCountText.setText(String.valueOf(playerCount));

        updateStartButtonState();
    }

    private void updateStartButtonState() {
        if (startGameButton != null) {
            startGameButton.setDisable(playerCount < 3);
        }
    }

    // SCALE
    private void scaleUI(double width, double height, Group group) {

        double scale = Math.min(
                width / DESIGN_WIDTH,
                height / DESIGN_HEIGHT
        );

        group.setScaleX(scale);
        group.setScaleY(scale);
    }

    // IP
    private String getLanIp() {

        try {
            Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {

                NetworkInterface ni = interfaces.nextElement();

                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();

                while (addresses.hasMoreElements()) {

                    InetAddress addr = addresses.nextElement();
                    String ip = addr.getHostAddress();

                    if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")
                            && !ip.startsWith("127.")) {
                        return ip;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "127.0.0.1";
    }

    @Override public void onStartGame() {}
    @Override public void onMessage(String message) {}
    @Override public void onPhaseChange(String phase) {}
    @Override public void onSecretWord(String word) {}
    @Override public void onImpostorMessage() {}
    @Override public void onGameOver(String message) {}
    @Override public void onInputEnabled(MessageType inputType) {}
    @Override public void onInputDisabled() {}
    @Override public void onNameAccepted() {}
}