package controller;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import client.GameClient;
import server.GameServer;
import utils.MessageType;
import utils.Player;
import utils.Message;

import view.*;

import java.util.List;

public class ScreenController implements GameEventListener {

    private final Stage stage;
    private final Scene scene;
    private final StackPane backgroundLayer;
    private final StackPane screenLayer;

    private MediaPlayer mediaPlayer;
    private GameClient client;
    private GameServer server;

    private GameEventListener activeScreen;

    public ScreenController(Stage stage,
                            Scene scene,
                            StackPane backgroundLayer,
                            StackPane screenLayer) {

        this.stage = stage;
        this.scene = scene;
        this.backgroundLayer = backgroundLayer;
        this.screenLayer = screenLayer;
    }

    public void initialize() {
        setupBackground();
        showTitleScreen();
    }

    public void setClient(GameClient client) {
        this.client = client;
        this.client.setListener(this);
    }

    public GameClient getClient() {
        return client;
    }

    public void setServer(GameServer server) {
        this.server = server;
    }

    public GameServer getServer() {
        return server;
    }

    private void setupBackground() {

        try {
            String path = getClass()
                    .getResource("/assets/titleScreenBg.mp4")
                    .toExternalForm();

            mediaPlayer = new MediaPlayer(new Media(path));
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setAutoPlay(true);

            MediaView view = new MediaView(mediaPlayer);

            view.fitWidthProperty().bind(scene.widthProperty());
            view.fitHeightProperty().bind(scene.heightProperty());
            view.setPreserveRatio(false);

            backgroundLayer.getChildren().add(view);

        } catch (Exception e) {
            System.err.println("[SCREEN] Background failed");
        }
    }

    public void showTitleScreen() {
        setActiveScreen(null);
        screenLayer.getChildren().setAll(new TitleScreen(this).createContent());
    }

    public void showHostScreen(String name) {
        setActiveScreen(null);
        screenLayer.getChildren().setAll(new HostScreen(this, name).createContent());
    }

    public void showPlayerScreen(String name) {
        setActiveScreen(null);
        screenLayer.getChildren().setAll(new PlayerScreen(this, name).createContent());
    }

    public void showWaitingScreen() {
        setActiveScreen(null);

        if (client == null) return;

        screenLayer.getChildren().setAll(
                new WaitingScreen(this, client).createContent()
        );
    }

    public void showGameScreen() {

        Platform.runLater(() -> {

            GameScreen screen = new GameScreen(() -> client, this);

            setActiveScreen(screen);

            screenLayer.getChildren().setAll(screen.createContent());

            mediaPlayer.stop();
        });
    }

    public void setActiveScreen(GameEventListener listener) {
        this.activeScreen = listener;
    }

    private void route(GameEventListenerAction action) {
        if (activeScreen == null) return;
        Platform.runLater(action::run);
    }

    private interface GameEventListenerAction {
        void run();
    }

    @Override
    public void onPlayersUpdated(List<Player> players) {
        route(() -> activeScreen.onPlayersUpdated(players));
    }

    @Override
    public void onMessage(String message) {
        route(() -> activeScreen.onMessage(message));
    }

    @Override
    public void onStartGame() {
        showGameScreen();
    }

    @Override
    public void onPhaseChange(String phase) {
        route(() -> activeScreen.onPhaseChange(phase));
    }

    @Override
    public void onSecretWord(String word) {
        route(() -> activeScreen.onSecretWord(word));
    }

    @Override
    public void onImpostorMessage() {
        route(() -> activeScreen.onImpostorMessage());
    }

    @Override
    public void onGameOver(String message) {
        route(() -> activeScreen.onGameOver(message));
    }

    @Override
    public void onInputEnabled(MessageType type) {
        route(() -> activeScreen.onInputEnabled(type));
    }

    @Override
    public void onInputDisabled() {
        route(() -> activeScreen.onInputDisabled());
    }
}