package controller;

import utils.Message;
import utils.MessageType;
import utils.Player;
import java.util.List;

public class Test3 {

    public static void main(String[] args) {

        String[] names = {"Alice", "Bob", "Charlie", "Diana", "Edward"};
        GameController game = new GameController(names);

        System.out.println("=< GAME START >=");
        List<Message> startMessages = game.start();

        String impostor = "";
        String v1 = "";
        String v2 = "";

        for (Message m : startMessages) {
            System.out.println(m);
            if (m.getType() == MessageType.SEND_SECRET) {
                if (m.getContent().equals("???")) {
                    impostor = m.getSender();
                } else if (v1.isEmpty()) {
                    v1 = m.getSender();
                } else if (v2.isEmpty()) {
                    v2 = m.getSender();
                }
            }
        }

        System.out.println("=< ROUND 1: ELIMINATE " + v1 + " >=");
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Alice", "apple")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Bob", "apple")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Charlie", "apple")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Diana", "apple")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, "Edward", "apple")));

        print(game.processMessage(new Message(MessageType.VOTE, "Alice", v1)));
        print(game.processMessage(new Message(MessageType.VOTE, "Bob", v1)));
        print(game.processMessage(new Message(MessageType.VOTE, "Charlie", v1)));
        print(game.processMessage(new Message(MessageType.VOTE, "Diana", v1)));
        print(game.processMessage(new Message(MessageType.VOTE, "Edward", v1)));

        System.out.println("=< ROUND 2: ELIMINATE " + v2 + " >=");
        List<Player> activeR2 = game.getPlayers();
        print(game.processMessage(new Message(MessageType.SEND_WORD, activeR2.get(0).getName(), "berry")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, activeR2.get(1).getName(), "berry")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, activeR2.get(2).getName(), "berry")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, activeR2.get(3).getName(), "berry")));

        print(game.processMessage(new Message(MessageType.VOTE, activeR2.get(0).getName(), v2)));
        print(game.processMessage(new Message(MessageType.VOTE, activeR2.get(1).getName(), v2)));
        print(game.processMessage(new Message(MessageType.VOTE, activeR2.get(2).getName(), v2)));
        print(game.processMessage(new Message(MessageType.VOTE, activeR2.get(3).getName(), v2)));

        System.out.println("=< ROUND 3: ELIMINATE IMPOSTOR " + impostor + " >=");
        List<Player> activeR3 = game.getPlayers();
        print(game.processMessage(new Message(MessageType.SEND_WORD, activeR3.get(0).getName(), "cherry")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, activeR3.get(1).getName(), "cherry")));
        print(game.processMessage(new Message(MessageType.SEND_WORD, activeR3.get(2).getName(), "cherry")));

        print(game.processMessage(new Message(MessageType.VOTE, activeR3.get(0).getName(), impostor)));
        print(game.processMessage(new Message(MessageType.VOTE, activeR3.get(1).getName(), impostor)));
        print(game.processMessage(new Message(MessageType.VOTE, activeR3.get(2).getName(), impostor)));

        System.out.println("=< TEST COMPLETE >=");
    }

    private static void print(List<Message> messages) {
        for (Message m : messages) System.out.println(m);
    }
}