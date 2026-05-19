package view;

import client.GameClient;
import controller.GameEventListener;
import controller.ScreenController;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import model.GameState;
import utils.Message;
import utils.MessageType;
import utils.Player;

import java.util.List;
import java.util.function.Supplier;

public class GameScreen implements GameEventListener {

    private static final double DESIGN_WIDTH = 1920;
    private static final double DESIGN_HEIGHT = 1080;

    private final StackPane root;

    private final VBox playerListBox;
    private final ChatPanel chatPanel;
    private final StackPane centerInfoBox;

    private final Supplier<GameClient> clientSupplier;
    private final GameClient client;

    private MessageType currentInputType = MessageType.SEND_WORD;

    private boolean eliminated = false;
    private boolean myTurn = false;

    public GameScreen(Supplier<GameClient> clientSupplier,
                      ScreenController screenController) {

        this.clientSupplier = clientSupplier;
        this.client = clientSupplier.get();

        screenController.setActiveScreen(this);

        root = new StackPane();

        StackPane gameRoot = new StackPane();
        gameRoot.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);

        Group scaledGroup = new Group(gameRoot);

        root.getChildren().add(scaledGroup);

        // BACKGROUND
        ImageView background = new ImageView(
                new Image(
                        getClass()
                                .getResource("/assets/gameBg.png")
                                .toExternalForm()
                )
        );

        background.fitWidthProperty().bind(gameRoot.widthProperty());
        background.fitHeightProperty().bind(gameRoot.heightProperty());
        background.setPreserveRatio(false);

        // PLAYER LIST
        playerListBox = new VBox(20);
        playerListBox.setPadding(new Insets(40));
        playerListBox.setPrefWidth(380);

        // CHAT PANEL
        chatPanel = new ChatPanel();
        chatPanel.setPrefWidth(520);
        chatPanel.setInputDisabled(true);

        // CENTER INFO
        centerInfoBox = new StackPane();
        centerInfoBox.setPrefSize(700, 220);

        BorderPane.setAlignment(centerInfoBox, Pos.TOP_CENTER);
        BorderPane.setMargin(centerInfoBox, new Insets(80, 0, 0, 0));

        // LAYOUT
        BorderPane content = new BorderPane();

        content.setLeft(playerListBox);
        content.setRight(chatPanel);
        content.setTop(centerInfoBox);

        gameRoot.getChildren().addAll(background, content);

        // SCALING
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {

            if (newScene != null) {

                scaleUI(
                        newScene.getWidth(),
                        newScene.getHeight(),
                        scaledGroup
                );

                newScene.widthProperty().addListener((o, ov, nv) ->
                        scaleUI(
                                newScene.getWidth(),
                                newScene.getHeight(),
                                scaledGroup
                        )
                );

                newScene.heightProperty().addListener((o, ov, nv) ->
                        scaleUI(
                                newScene.getWidth(),
                                newScene.getHeight(),
                                scaledGroup
                        )
                );
            }
        });

        // REQUEST PLAYER LIST
        client.sendMessage(
                new Message(
                        MessageType.PLAYER_LIST,
                        client.getPlayerName(),
                        ""
                )
        );

        // SEND BUTTON
        chatPanel.getSendButton().setOnAction(e -> {

            if (eliminated) return;

            String text = chatPanel.getInputText().trim();

            if (text.isEmpty()) return;

            client.sendMessage(
                    new Message(
                            currentInputType,
                            client.getPlayerName(),
                            text
                    )
            );

            chatPanel.clearInput();

            if (currentInputType == MessageType.SEND_WORD) {
                myTurn = false;
                refreshInputState();
            }
        });

        refreshInputState();
    }

    public Parent createContent() {
        return root;
    }

    private void scaleUI(double width,
                         double height,
                         Group group) {

        double scale = Math.min(
                width / DESIGN_WIDTH,
                height / DESIGN_HEIGHT
        );

        group.setScaleX(scale);
        group.setScaleY(scale);
    }

    private void refreshInputState() {

        if (eliminated) {
            chatPanel.setInputDisabled(true);
            return;
        }

        if (currentInputType == MessageType.SEND_WORD) {
            chatPanel.setInputDisabled(!myTurn);
            return;
        }

        chatPanel.setInputDisabled(false);
    }

    // EVENTS
    @Override
    public void onMessage(String message) {

        Platform.runLater(() ->
                chatPanel.appendMessage(message)
        );
    }

    @Override
    public void onPhaseChange(String phase) {

        Platform.runLater(() -> {

            // chatPanel.appendMessage("[PHASE] " + phase);

            switch (GameState.GamePhase.valueOf(phase)) {

                case WORD_SUBMITTING -> {
                    currentInputType = MessageType.SEND_WORD;
                    myTurn = false;
                }

                case VOTING -> {
                    currentInputType = MessageType.VOTE;
                }

                case TIEBREAKER -> {
                    currentInputType = MessageType.SEND_STATEMENT;
                }

                case GAME_OVER -> {
                    chatPanel.setInputDisabled(true);
                }
            }

            refreshInputState();
        });
    }

    @Override
    public void onPlayersUpdated(List<Player> players) {

        Platform.runLater(() -> {

            playerListBox.getChildren().clear();

            for (int i = 0; i < players.size(); i++) {

                Player p = players.get(i);

                if (p.getName().equals(client.getPlayerName())
                        && p.isEliminated()) {

                    eliminated = true;
                }

                playerListBox.getChildren().add(
                		new PlayerCard(
                		        p,
                		        i,
                		        client.getPlayerName()
                		)
                );
            }

            refreshInputState();
        });
    }

    @Override
    public void onSecretWord(String word) {

        Platform.runLater(() -> {

            centerInfoBox.getChildren().clear();

            Rectangle bg = new Rectangle(600, 140);
            bg.setFill(Color.WHITE);
            bg.setArcWidth(25);
            bg.setArcHeight(25);

            Text text = new Text(word);
            text.setFill(Color.BLACK);
            text.setFont(Font.font("Arial", 42));

            centerInfoBox.getChildren().addAll(bg, text);
        });
    }

    @Override
    public void onImpostorMessage() {

        Platform.runLater(() -> {

            centerInfoBox.getChildren().clear();

            VBox box = new VBox(20);
            box.setAlignment(Pos.CENTER);

            Label top = new Label("You are the");
            top.setTextFill(Color.BLACK);
            top.setFont(Font.font("Arial", 42));

            ImageView impostor = new ImageView(
                    new Image(
                            getClass()
                                    .getResource("/assets/impostorText.png")
                                    .toExternalForm()
                    )
            );

            impostor.setFitWidth(420);
            impostor.setPreserveRatio(true);

            Label bottom = new Label("Blend in with the crewmates");
            bottom.setTextFill(Color.BLACK);
            bottom.setFont(Font.font("Arial", 42));

            box.getChildren().addAll(top, impostor, bottom);

            centerInfoBox.getChildren().add(box);
        });
    }

    @Override
    public void onGameOver(String message) {

        Platform.runLater(() -> {

            chatPanel.appendMessage(message);
            chatPanel.setInputDisabled(true);
        });
    }

    @Override
    public void onStartGame() {}

    @Override
    public void onInputEnabled(MessageType inputType) {

        Platform.runLater(() -> {

            currentInputType = inputType;

            if (inputType == MessageType.SEND_WORD) {
                myTurn = true;
            }

            refreshInputState();
        });
    }

    @Override
    public void onInputDisabled() {

        Platform.runLater(() -> {

            if (currentInputType == MessageType.SEND_WORD) {
                myTurn = false;
            }

            chatPanel.setInputDisabled(true);
        });
    }
}