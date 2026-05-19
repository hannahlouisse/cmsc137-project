package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ChatPanel extends VBox {

    private final TextArea chatArea;

    private final TextField inputField;

    private final Button sendButton;

    public ChatPanel() {

        setSpacing(15);

        setPadding(new Insets(20));

        setPrefWidth(820);

        setAlignment(Pos.CENTER);

        // CHATBOX
        chatArea = new TextArea();

        chatArea.setEditable(false);

        chatArea.setWrapText(true);

        chatArea.setStyle("""
                -fx-font-size: 24px;
                -fx-background-radius: 15px;
                """);

        VBox.setVgrow(chatArea, Priority.ALWAYS);

        // INPUT FIELD
        inputField = new TextField();

        inputField.setPromptText("Enter message");

        inputField.setStyle("""
                -fx-font-size: 24px;
                -fx-padding: 10px;
                """);

        // SEND BUTTON
        sendButton = new Button("Send");

        sendButton.setStyle("""
                -fx-font-size: 24px;
                -fx-font-weight: bold;
                """);

        // CONTAINER FOR INPUT FIELD + SEND BUTTON
        HBox inputBox = new HBox(10);

        inputBox.getChildren().addAll(
                inputField,
                sendButton
        );

        HBox.setHgrow(inputField, Priority.ALWAYS);

        getChildren().addAll(
                chatArea,
                inputBox
        );
    }

    public void appendMessage(String message) {

        chatArea.appendText(message + "\n");
    }

    public String getInputText() {

        return inputField.getText();
    }

    public void clearInput() {

        inputField.clear();
    }

    public Button getSendButton() {

        return sendButton;
    }
    
    public void setInputDisabled(boolean disabled) {

        inputField.setDisable(disabled);

        sendButton.setDisable(disabled);
    }
}
