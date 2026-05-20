package view;

import controller.GameEventListener;
import controller.ScreenController;
import client.GameClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.List;
import utils.Player;
import utils.MessageType;

public class PlayerScreen implements GameEventListener {

    private static final double DESIGN_WIDTH = 1920;
    private static final double DESIGN_HEIGHT = 1080;

    private final ScreenController screenController;
    private final String playerName;

    private final Text errorText = new Text();

    public PlayerScreen(ScreenController screenController,
                        String playerName) {

        this.screenController = screenController;
        this.playerName = playerName;
        screenController.setActiveScreen(this);
    }

    public Parent createContent() {

        StackPane root = new StackPane();
        StackPane gameRoot = new StackPane();

        gameRoot.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);

        Group scaledGroup = new Group(gameRoot);

        Text titleText = new Text("Enter Host's IP Address");
        titleText.setFill(Color.WHITE);
        titleText.setFont(Font.font("Arial", FontWeight.BOLD, 40));

        errorText.setFill(Color.RED);
        errorText.setFont(Font.font("Arial", FontWeight.BOLD, 30));

        TextField ipField = new TextField();
        ipField.setPromptText("XXX.XXX.XX.X");
        ipField.setMaxWidth(500);
        ipField.setStyle("""
                -fx-font-size: 28px;
                -fx-padding: 15px;
                -fx-background-radius: 15px;
                -fx-border-radius: 15px;
                """);

        Button joinButton = new Button();
        ImageView joinBtnImage = new ImageView(
                new Image(getClass().getResource("/assets/joinGameBtn.png").toExternalForm())
        );
        joinBtnImage.setFitWidth(300);
        joinBtnImage.setPreserveRatio(true);
        joinButton.setGraphic(joinBtnImage);

        joinButton.setStyle("""
                -fx-background-color: transparent;
                -fx-padding: 0;
                """);

        Button backButton = new Button();
        ImageView backBtnImage = new ImageView(
                new Image(getClass().getResource("/assets/backBtn.png").toExternalForm())
        );
        backBtnImage.setFitWidth(220);
        backBtnImage.setPreserveRatio(true);
        backButton.setGraphic(backBtnImage);

        backButton.setStyle("""
                -fx-background-color: transparent;
                -fx-padding: 0;
                """);

        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);

        VBox.setMargin(titleText, new Insets(0, 0, 10, 0));
        VBox.setMargin(ipField, new Insets(0, 0, 30, 0));
        VBox.setMargin(errorText, new Insets(0, 0, 40, 0));
        VBox.setMargin(joinButton, new Insets(0, 0, 20, 0));

        contentBox.getChildren().addAll(
                titleText,
                ipField,
                errorText,
                joinButton,
                backButton
        );

        gameRoot.getChildren().add(contentBox);
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

        backButton.setOnAction(e -> screenController.showTitleScreen());

        joinButton.setOnAction(e -> {

            String ip = ipField.getText().trim();

            if (ip.isEmpty()) {
                errorText.setText("IP cannot be empty");
                return;
            }

            GameClient client = new GameClient(
                    ip,
                    12345,
                    playerName
            );

            screenController.setClient(client);
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
    public void onMessage(String message) {
        Platform.runLater(() -> errorText.setText(message));
    }

    @Override public void onPhaseChange(String phase) {}
    @Override public void onPlayersUpdated(List<Player> players) {}
    @Override public void onSecretWord(String word) {}
    @Override public void onImpostorMessage() {}
    @Override public void onGameOver(String message) {}
    @Override public void onStartGame() {}
    @Override public void onInputEnabled(MessageType inputType) {}
    @Override public void onInputDisabled() {}
    @Override public void onNameAccepted() {}
}