package view;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import controller.ScreenController;

import client.GameClient;
import server.GameServer;

public class TitleScreen {

    private static final double DESIGN_WIDTH = 1920;
    private static final double DESIGN_HEIGHT = 1080;

    private final ScreenController screenController;

    public TitleScreen(ScreenController screenController) {

        this.screenController = screenController;
    }

    public Parent createContent() {

        StackPane root = new StackPane();

        StackPane gameRoot = new StackPane();

        gameRoot.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);

        Group scaledGroup = new Group(gameRoot);

        // TITLE IMAGE
        ImageView titleImage = new ImageView(
                new Image(getClass().getResource("/assets/titleText.png").toExternalForm())
        );

        titleImage.setFitWidth(1100);

        titleImage.setPreserveRatio(true);

        // NAME FIELD
        TextField nameField = new TextField();

        nameField.setPromptText("Enter your name");

        nameField.setMaxWidth(400);

        nameField.setStyle("""
                -fx-font-size: 40px;
                -fx-padding: 15px;
                -fx-background-radius: 15px;
                -fx-border-radius: 15px;
                """);

        // CREATE BUTTON
        Button createButton = new Button();

        ImageView createBtnImage = new ImageView(
                new Image(getClass().getResource("/assets/createGameBtn.png").toExternalForm())
        );

        createBtnImage.setFitWidth(400);

        createBtnImage.setPreserveRatio(true);

        createButton.setGraphic(createBtnImage);

        createButton.setStyle("""
                -fx-background-color: transparent;
                -fx-padding: 0;
                """);

        // JOIN BUTTON
        Button joinButton = new Button();

        ImageView joinBtnImage = new ImageView(
                new Image(getClass().getResource("/assets/joinGameBtn.png").toExternalForm())
        );

        joinBtnImage.setFitWidth(400);

        joinBtnImage.setPreserveRatio(true);

        joinButton.setGraphic(joinBtnImage);

        joinButton.setStyle("""
                -fx-background-color: transparent;
                -fx-padding: 0;
                """);

        // BUTTON BOX
        HBox buttonBox = new HBox(40);

        buttonBox.setAlignment(Pos.CENTER);

        buttonBox.getChildren().addAll(
                createButton,
                joinButton
        );

        // CONTENT BOX
        VBox contentBox = new VBox(150);

        contentBox.setAlignment(Pos.CENTER);

        contentBox.getChildren().addAll(
                titleImage,
                nameField,
                buttonBox
        );

        gameRoot.getChildren().add(contentBox);

        root.getChildren().add(scaledGroup);

        // SCALING
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

        // BUTTON ACTIONS
        createButton.setOnAction(e -> {

            String playerName = nameField.getText().trim();

            if (!playerName.isEmpty()) {

                // START SERVER
                GameServer server = new GameServer(12345);

                server.start();

                // CREATE HOST CLIENT
                GameClient client =
                        new GameClient(
                                getLanIp(),
                                12345,
                                playerName
                        );

                screenController.setServer(server);

                screenController.setClient(client);

                // OPEN HOST SCREEN
                screenController.setHosting(true);
                screenController.showHostScreen(playerName);
            }
        });

        joinButton.setOnAction(e -> {

            String playerName = nameField.getText().trim();

            if (!playerName.isEmpty()) {
                screenController.showPlayerScreen(playerName);
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
    
    private String getLanIp() {

        try {

            java.util.Enumeration<java.net.NetworkInterface> interfaces =
                    java.net.NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {

                java.net.NetworkInterface ni = interfaces.nextElement();

                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }

                java.util.Enumeration<java.net.InetAddress> addresses =
                        ni.getInetAddresses();

                while (addresses.hasMoreElements()) {

                    java.net.InetAddress addr = addresses.nextElement();

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
}
