package controller;

import utils.Message;
import utils.MessageType;
import java.util.List;

public class Test {

    public static void main(String[] args) {

        String[] players = {"Alice", "Bob", "Charlie", "Diana"};

        GameController game = new GameController(players);

        System.out.println("=< GAME START >=");

        List<Message> startMessages = game.start();

        String impostor = "";

        for (Message m : startMessages) {
            System.out.println(m);

            if (m.getType() == MessageType.SEND_SECRET &&
                m.getContent().equals("???")) {

                impostor = m.getSender();
            }
        }

        System.out.println("=< IMPOSTOR: " + impostor + " >=");

        System.out.println("=< WORD SUBMISSION >=");

        print(game.processMessage(new Message(MessageType.SEND_WORD, "Alice", "code")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Bob", "java")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Charlie", "keyboard")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Diana", "computer")));

        System.out.println("=< VOTING >=");

        print(game.processMessage(new Message(MessageType.VOTE, "Alice", impostor)));
        print(game.processMessage(new Message(MessageType.VOTE, "Bob", impostor)));
        print(game.processMessage(new Message(MessageType.VOTE, "Charlie", impostor)));
        print(game.processMessage(new Message(MessageType.VOTE, "Diana", impostor)));

        System.out.println("=< TEST COMPLETE >=");
    }

    private static void print(List<Message> messages) {
        for (Message m : messages) System.out.println(m);
    }
}