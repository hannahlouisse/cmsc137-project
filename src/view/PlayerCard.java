package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import utils.Player;

public class PlayerCard extends StackPane {

    public PlayerCard(Player player,
                      int index,
                      String localPlayerName) {

        // CARD BACKGROUND
        Rectangle background = new Rectangle(
                490,
                100
        );

        background.setArcWidth(20);
        background.setArcHeight(20);

        background.setFill(Color.WHITE);

        // ICONS
        String[] icons = {
                "/assets/redCrewmate.png",
                "/assets/yellowCrewmate.png",
                "/assets/cyanCrewmate.png",
                "/assets/purpleCrewmate.png"
        };

        String iconPath =
                icons[index % icons.length];

        ImageView icon = new ImageView(
                new Image(
                        getClass()
                                .getResource(iconPath)
                                .toExternalForm()
                )
        );

        icon.setFitWidth(90);
        icon.setFitHeight(90);

        // NAME
        Text nameText = new Text(
                player.getName()
        );

        nameText.setFont(
                Font.font(
                        "Arial",
                        44
                )
        );

        // OWN NAME IS BLUE
        if (player.getName().equals(localPlayerName)) {

            nameText.setFill(
                    Color.rgb(40, 140, 255)
            );

        } else {

            nameText.setFill(Color.BLACK);
        }

        // CONTENT
        HBox content = new HBox(20);

        content.setAlignment(Pos.CENTER_LEFT);

        VBox iconBox = new VBox(icon);

        iconBox.setAlignment(Pos.BOTTOM_CENTER);
        iconBox.setPrefHeight(90);

        HBox.setMargin(
                iconBox,
                new Insets(0, 0, 0, 15)
        );

        content.getChildren().addAll(
                iconBox,
                nameText
        );

        getChildren().addAll(
                background,
                content
        );

        setAlignment(Pos.CENTER_LEFT);

        // ELIMINATED STYLE
        if (player.isEliminated()) {

            setOpacity(0.4);
        }
    }
}