package main;

import controller.ScreenController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        StackPane root = new StackPane();

        StackPane backgroundLayer = new StackPane();

        StackPane screenLayer = new StackPane();

        root.getChildren().addAll(
                backgroundLayer,
                screenLayer
        );

        Scene scene = new Scene(root, 1280, 720);

        ScreenController screenController = new ScreenController(
                stage,
                scene,
                backgroundLayer,
                screenLayer
        );

        screenController.initialize();

        stage.setTitle("Guess The Impostor");

        stage.setScene(scene);

        stage.setMinWidth(640);
        stage.setMinHeight(360);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
