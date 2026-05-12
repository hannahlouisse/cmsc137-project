package controller;

import utils.Message;
import utils.MessageType;
import utils.Player;
import java.util.List;

public class Test2 {

    public static void main(String[] args) {

        String[] names = {"Alice", "Bob", "Charlie", "Diana"};
        GameController game = new GameController(names);

        System.out.println("=< GAME START >=");
        List<Message> startMessages = game.start();

        String impostor = "";
        String victim1 = "";

        for (Message m : startMessages) {
            System.out.println(m);
            if (m.getType() == MessageType.SEND_SECRET) {
                if (m.getContent().equals("???")) {
                    impostor = m.getSender();
                } else if (victim1.isEmpty()) {
                    victim1 = m.getSender();
                }
            }
        }

        System.out.println("=< IMPOSTOR: " + impostor + " >=");

        System.out.println("=< WORD SUBMISSION ROUND 1 >=");
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Alice", "code")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Bob", "java")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Charlie", "keyboard")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Diana", "computer")));

        System.out.println("=< VOTING ROUND 1 (ELIMINATE CREWMATE) >=");
        print(game.processMessage(new Message(MessageType.VOTE, "Alice", victim1)));
        print(game.processMessage(new Message(MessageType.VOTE, "Bob", victim1)));
        print(game.processMessage(new Message(MessageType.VOTE, "Charlie", victim1)));
        print(game.processMessage(new Message(MessageType.VOTE, "Diana", victim1)));

        System.out.println("=< WORD SUBMISSION ROUND 2 >=");
        List<Player> activeR2 = game.getPlayers();
        print(game.processMessage(new Message(MessageType.SEND_WORD, activeR2.get(0).getName(), "script")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, activeR2.get(1).getName(), "syntax")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, activeR2.get(2).getName(), "logic")));

        System.out.println("=< VOTING ROUND 2 (ELIMINATE IMPOSTOR) >=");
        print(game.processMessage(new Message(MessageType.VOTE, activeR2.get(0).getName(), impostor)));
        print(game.processMessage(new Message(MessageType.VOTE, activeR2.get(1).getName(), impostor)));
        print(game.processMessage(new Message(MessageType.VOTE, activeR2.get(2).getName(), impostor)));

        System.out.println("=< TEST COMPLETE >=");
    }

    private static void print(List<Message> messages) {
        for (Message m : messages) System.out.println(m);
    }
}