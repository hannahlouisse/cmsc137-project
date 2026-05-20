package view;

import client.GameClient;
import controller.GameEventListener;
import controller.ScreenController;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import utils.MessageType;
import utils.Player;

import java.util.List;

public class WaitingScreen implements GameEventListener {

    private static final double DESIGN_WIDTH = 1920;
    private static final double DESIGN_HEIGHT = 1080;

    private final ScreenController controller;
    private final GameClient client;

    private final StackPane root = new StackPane();

    public WaitingScreen(ScreenController controller, GameClient client) {

        this.controller = controller;
        this.client = client;

        controller.setActiveScreen(this);
    }

    public Parent createContent() {

        StackPane gameRoot = new StackPane();

        gameRoot.setPrefSize(
                DESIGN_WIDTH,
                DESIGN_HEIGHT
        );

        ImageView waitingText = new ImageView(
                new Image(
                        getClass()
                                .getResource("/assets/waitingText.png")
                                .toExternalForm()
                )
        );

        waitingText.setFitWidth(700);
        waitingText.setPreserveRatio(true);

        StackPane.setAlignment(waitingText, Pos.CENTER);

        gameRoot.getChildren().add(waitingText);

        Group scaledGroup = new Group(gameRoot);

        root.getChildren().add(scaledGroup);

        root.sceneProperty().addListener((obs, oldScene, newScene) -> {

            if (newScene != null) {

                scaleUI(newScene.getWidth(), newScene.getHeight(), scaledGroup);

                newScene.widthProperty().addListener((o, ov, nv) -> {
                    scaleUI(newScene.getWidth(), newScene.getHeight(), scaledGroup);
                });

                newScene.heightProperty().addListener((o, ov, nv) -> {
                    scaleUI(newScene.getWidth(), newScene.getHeight(), scaledGroup);
                });
            }
        });

        return root;
    }

    private void scaleUI(double width,
                         double height,
                         Group group) {

        double scaleX = width / DESIGN_WIDTH;

        double scaleY = height / DESIGN_HEIGHT;

        double scale = Math.min(scaleX, scaleY);

        group.setScaleX(scale);

        group.setScaleY(scale);
    }

    @Override
    public void onStartGame() {
        Platform.runLater(() ->
                controller.showGameScreen()
        );
    }

    @Override
    public void onMessage(String message) {}

    @Override
    public void onPhaseChange(String phase) {}

    @Override
    public void onPlayersUpdated(List<Player> players) {}

    @Override
    public void onSecretWord(String word) {}

    @Override
    public void onImpostorMessage() {}

    @Override
    public void onGameOver(String message) {}

    @Override
    public void onInputEnabled(MessageType inputType) {}

    @Override
    public void onInputDisabled() {}
    
    @Override
    public void onNameAccepted() {}
}