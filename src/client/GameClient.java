package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

import utils.Message;
import utils.MessageType;

public class GameClient {
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private String playerName;
    private boolean connected;
    private boolean nameAccepted;
    private Scanner consoleScanner;
    private boolean isMyTurn;
    private boolean waitingForInput;
    private String currentPrompt;
    private boolean gameStarted; // Track if game has started

    public GameClient(String serverAddress, int port) {
        this.connected = false;
        this.nameAccepted = false;
        this.isMyTurn = false;
        this.waitingForInput = false;
        this.currentPrompt = null;
        this.gameStarted = false;
        this.consoleScanner = new Scanner(System.in);

        try {
            System.out.println("Connecting to " + serverAddress + ":" + port + "...");
            socket = new Socket(serverAddress, port);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
            connected = true;

            System.out.println("[OK] Connected to game server!");

            // Start listening for messages in background
            startListening();

            // Request name (this will block until name is accepted)
            requestName();

            // After name is accepted, start the command loop
            commandLoop();

        } catch (IOException e) {
            System.err.println("[FAILED] Failed to connect: " + e.getMessage());
        }
    }

    private void requestName() {
        while (!nameAccepted && connected) {
            System.out.print("\nEnter your name: ");
            String name = consoleScanner.nextLine().trim();

            if (name.isEmpty()) {
                System.out.println("Name cannot be empty!");
                continue;
            }

            try {
                Message joinMsg = new Message(MessageType.JOIN, name, "");
                outputStream.writeObject(joinMsg);
                outputStream.flush();
                this.playerName = name;
                System.out.println("Attempting to join as: " + name);

                // Wait a bit for server response
                try { Thread.sleep(500); } catch (InterruptedException e) {}

            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private void commandLoop() {
        System.out.println("\n=========================================");
        System.out.println("Game ready! " + playerName);
        System.out.println("Type 'start' if you are the HOST to begin the game");
        System.out.println("Type 'quit' to exit");
        System.out.println("=========================================\n");

        while (connected && nameAccepted) {
            String input = consoleScanner.nextLine().trim();

            if (input.equalsIgnoreCase("quit")) {
                System.out.println("Exiting...");
                System.exit(0);
            } else if (input.equalsIgnoreCase("start") && !gameStarted) {
                startGame();
            } else if (waitingForInput) {
                // We are waiting for game input (word, vote, or statement)
                processGameInput(input);
            } else if (gameStarted && !waitingForInput) {
                System.out.println("Waiting for your turn... Please wait for the prompt.");
            } else if (input.equalsIgnoreCase("start") && gameStarted) {
                System.out.println("Game already started! Please wait for your turn.");
            } else {
                System.out.println("Unknown command. Type 'start' if you are the host, or wait for your turn.");
            }
        }
    }

    private void processGameInput(String input) {
        if (input.isEmpty()) {
            if (waitingForInput) {
                System.out.print("Input cannot be empty. Please enter again: ");
            }
            return;
        }

        switch (currentPrompt) {
            case "WORD":
                // Send the word to server
                sendMessage(new Message(MessageType.SEND_WORD, playerName, input));
                System.out.println("Submitting word...");
                // IMPORTANT: Do NOT clear waitingForInput here
                // Wait for server response (accept or reject)
                break;
            case "VOTE":
                System.out.println("[VOTE] Casting vote for: " + input);
                sendMessage(new Message(MessageType.VOTE, playerName, input));
                System.out.println("Waiting for server to validate vote...");
                break;
            case "STATEMENT":
                sendMessage(new Message(MessageType.SEND_STATEMENT, playerName, input));
                System.out.println("[OK] Statement submitted! Waiting for other players...\n");
                waitingForInput = false;
                currentPrompt = null;
                isMyTurn = false;
                break;
            default:
                System.out.println("Unknown prompt type: " + currentPrompt);
                waitingForInput = false;
                currentPrompt = null;
                break;
        }
    }

    private void startListening() {
        new Thread(() -> {
            try {
                while (connected) {
                    Message msg = (Message) inputStream.readObject();
                    handleMessage(msg);
                }
            } catch (IOException | ClassNotFoundException e) {
                if (connected) {
                    System.err.println("\n[!] Connection lost");
                    connected = false;
                }
            }
        }).start();
    }

    private void handleMessage(Message message) {
        switch (message.getType()) {
            case NAME_ACCEPTED:
                System.out.println("\n[OK] " + message.getContent());
                nameAccepted = true;
                break;

            case NAME_TAKEN:
                System.err.println("\n[FAILED] " + message.getContent());
                nameAccepted = false;
                playerName = null;
                requestName();
                break;

            case PLAYER_LIST:
                String players = message.getContent();
                if (players != null && !players.isEmpty()) {
                    String[] playerArray = players.split(",");
                    System.out.println("\n--- Current Players (" + playerArray.length + "/4) ---");
                    for (String player : playerArray) {
                        if (player != null && !player.trim().isEmpty()) {
                            System.out.println("  - " + player.trim());
                        }
                    }
                    System.out.println("----------------------------------------");
                }
                break;

            case JOIN:
                System.out.println("\n[NEW] " + message.getSender() + " " + message.getContent());
                break;

            case START_GAME:
                System.out.println("\n[START] GAME STARTING!");
                System.out.println("=========================================\n");
                gameStarted = true;
                break;

            case SEND_SECRET:
                System.out.println("\n+---------------------------------------+");
                System.out.println("|           YOUR SECRET WORD            |");
                System.out.println("|                                       |");
                System.out.println("|           " + message.getContent() + "                       |");
                System.out.println("|                                       |");
                System.out.println("|    DO NOT SAY THE SECRET WORD!        |");
                System.out.println("+---------------------------------------+\n");
                break;

            case TURN:
                if (message.getSender() != null && message.getContent() != null) {
                    // It's someone else's turn
                    System.out.println("\n[TURN] " + message.getSender() + " " + message.getContent());
                    isMyTurn = false;
                } else if (message.getContent() != null && message.getContent().contains("YOUR TURN")) {
                    // It's my turn - only prompt if not already waiting for input
                    if (!waitingForInput) {
                        isMyTurn = true;
                        waitingForInput = true;
                        currentPrompt = "WORD";
                        System.out.println("\n[YOUR TURN] " + message.getContent());
                        System.out.print(">> ");
                    }
                }
                break;

            case SEND_WORD:
                if (message.getSender() != null) {
                    System.out.println("[WORD] " + message.getSender() + ": \"" + message.getContent() + "\"");
                } else {
                    System.out.println(message.getContent());
                }
                break;

            case VOTE:
                if (message.getContent() != null && message.getContent().contains("Your turn to vote")) {
                    if (!waitingForInput) {
                        waitingForInput = true;
                        currentPrompt = "VOTE";
                        System.out.println("\n=========================================");
                        System.out.println("VOTING PHASE");
                        System.out.println("=========================================");

                        // Extract and show valid options
                        if (message.getContent().contains("Options: ")) {
                            String options = message.getContent().substring(message.getContent().indexOf("Options: ") + 9);
                            System.out.println("\nYou MUST vote for one of these EXACT names:");
                            String[] optionList = options.split(", ");
                            for (String option : optionList) {
                                System.out.println("  -> " + option);
                            }
                            System.out.println("\nEnter the EXACT name as shown above:");
                            System.out.print(">> ");
                        } else {
                            System.out.print(">> ");
                        }
                    }
                } else if (message.getContent() != null) {
                    // This is a vote notification from other players
                    if (!message.getContent().contains("Your turn to vote")) {
                        System.out.println("[VOTE] " + message.getContent());
                    }
                }
                break;

            case SEND_STATEMENT:
                if (message.getSender() != null && message.getContent() != null) {
                    System.out.println("[STATEMENT] " + message.getSender() + ": \"" + message.getContent() + "\"");
                } else if (message.getContent() != null && message.getContent().contains("Defend yourself")) {
                    if (!waitingForInput) {
                        waitingForInput = true;
                        currentPrompt = "STATEMENT";
                        System.out.println("\n[STATEMENT] " + message.getContent());
                        System.out.print(">> ");
                    }
                } else if (message.getContent() != null) {
                    System.out.println(message.getContent());
                }
                break;

            case PLAYER_ELIMINATED:
                System.out.println("\n=========================================");
                System.out.println("[ELIMINATED] " + message.getContent() + " has been eliminated!");
                System.out.println("=========================================\n");
                break;

            case PHASE_CHANGE:
                String phase = message.getContent();
                System.out.println("\n+-----------------------------------------+");
                System.out.println("|         PHASE: " + phase + "         |");
                System.out.println("+-----------------------------------------+\n");
                break;

            case CREWMATES_WIN:
                System.out.println("\n=========================================");
                System.out.println("     CREWMATES WIN!");
                System.out.println("     The impostor was: " + message.getContent());
                System.out.println("=========================================\n");
                connected = false;
                System.exit(0);
                break;

            case IMPOSTOR_WINS:
                System.out.println("\n=========================================");
                System.out.println("     IMPOSTOR WINS!");
                System.out.println("     The impostor was: " + message.getContent());
                System.out.println("=========================================\n");
                connected = false;
                System.exit(0);
                break;

            case VOTE_ACCEPTED:
                System.out.println("[OK] " + message.getContent());
                waitingForInput = false;
                currentPrompt = null;
                break;

            case WORD_ACCEPTED:
                System.out.println("[OK] " + message.getContent());
                System.out.println("Waiting for other players...\n");
                waitingForInput = false;
                currentPrompt = null;
                isMyTurn = false;
                break;

            case ERROR:
                System.err.println("\n[ERROR] " + message.getContent());
                // If the error is about invalid vote, set waitingForInput to true to allow retry
                if (message.getContent().contains("Invalid vote")) {
                    waitingForInput = true;
                    currentPrompt = "VOTE";
                    System.out.print(">> ");
                }
                break;

            default:
                System.out.println("[DEBUG] " + message.getType() + ": " + message.getContent());
                break;
        }
    }

    public void sendMessage(Message message) {
        try {
            if (outputStream != null && connected) {
                outputStream.writeObject(message);
                outputStream.flush();
            }
        } catch (IOException e) {
            System.err.println("Error sending: " + e.getMessage());
        }
    }

    public void startGame() {
        if (nameAccepted && !gameStarted) {
            sendMessage(new Message(MessageType.START_GAME, playerName, ""));
            System.out.println("Starting game...");
        } else if (gameStarted) {
            System.out.println("Game already started!");
        } else {
            System.out.println("Cannot start - name not accepted yet!");
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=========================================");
        System.out.println("     GUESS THE IMPOSTOR - CLIENT");
        System.out.println("=========================================");

        System.out.print("Server address (localhost): ");
        String address = scanner.nextLine();
        if (address.isEmpty()) address = "localhost";

        System.out.print("Port (12345): ");
        String portStr = scanner.nextLine();
        int port = portStr.isEmpty() ? 12345 : Integer.parseInt(portStr);

        new GameClient(address, port);
    }
}