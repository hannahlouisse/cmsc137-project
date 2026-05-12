package controller;

import utils.Message;
import utils.MessageType;
import utils.Player;
import java.util.List;

public class Test1 {

    public static void main(String[] args) {

        String[] names = {"Alice", "Bob", "Charlie", "Diana"};
        GameController game = new GameController(names);

        System.out.println("=< GAME START >=");
        List<Message> startMessages = game.start();

        String victim1 = "";
        String victim2 = "";

        for (Message m : startMessages) {
            System.out.println(m);
            if (m.getType() == MessageType.SEND_SECRET && !m.getContent().equals("???")) {
                if (victim1.isEmpty()) {
                    victim1 = m.getSender();
                } else if (victim2.isEmpty()) {
                    victim2 = m.getSender();
                }
            }
        }

        System.out.println("=< WORD SUBMISSION ROUND 1 >=");
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Alice", "Apple")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Bob", "Apple")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Charlie", "Apple")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Diana", "Apple")));

        System.out.println("=< VOTING ROUND 1 >=");
        print(game.processMessage(new Message(MessageType.VOTE, "Alice", victim1)));
        print(game.processMessage(new Message(MessageType.VOTE, "Bob", victim1)));
        print(game.processMessage(new Message(MessageType.VOTE, "Charlie", victim1)));
        print(game.processMessage(new Message(MessageType.VOTE, "Diana", victim1)));

        System.out.println("=< WORD SUBMISSION ROUND 2 >=");
        List<Player> activeR2 = game.getPlayers();
        print(game.processMessage(new Message(MessageType.SEND_WORD, activeR2.get(0).getName(), "Banana")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, activeR2.get(1).getName(), "Banana")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, activeR2.get(2).getName(), "Banana")));

        System.out.println("=< VOTING ROUND 2 >=");
        print(game.processMessage(new Message(MessageType.VOTE, activeR2.get(0).getName(), victim2)));
        print(game.processMessage(new Message(MessageType.VOTE, activeR2.get(1).getName(), victim2)));
        print(game.processMessage(new Message(MessageType.VOTE, activeR2.get(2).getName(), victim2)));

        System.out.println("=< TEST COMPLETE >=");
    }

    private static void print(List<Message> messages) {
        for (Message m : messages) System.out.println(m);
    }
}