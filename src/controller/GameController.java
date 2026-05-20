package controller;

import model.GameState;
import model.WordDictionary;
import server.GameServer;
import utils.Message;
import utils.MessageType;
import utils.Player;

import java.util.*;

public class GameController {

    private final GameServer server;
    private final GameState state;
    private int roundNumber = 0;

    // voting
    private final Map<String, String> votes = new LinkedHashMap<>();

    // tiebreaker
    private final Map<String, String> tieStatements = new LinkedHashMap<>();
    private List<String> tiedPlayers = new ArrayList<>();

    // turn-based word submission
    private int currentTurnIndex = 0;

    private final String impostorName;

    public GameController(String[] names, GameServer server) {

        this.server = server;

        List<Player> players = Arrays.stream(names)
                .map(Player::new)
                .toList();

        String secretWord = WordDictionary.getRandomWord();

        Random rand = new Random();

        int impIndex = rand.nextInt(players.size());

        players.get(impIndex).setRole(Player.Role.IMPOSTOR);

        impostorName = players.get(impIndex).getName();

        this.state = new GameState(players, secretWord);

        System.out.println(
                "[GAME] Secret=" + secretWord +
                " | Impostor=" + impostorName
        );
    }

    public void startGame() {
    	
        server.broadcastSystem("Game starts now!");
        startWordPhase();
    }

    private void startWordPhase() {
    	
        state.setPhase(GameState.GamePhase.WORD_SUBMITTING);
        
        roundNumber++;
        server.broadcastSystem("\n==========================");
        server.broadcastSystem("ROUND " + roundNumber);
        server.broadcastSystem("==========================");

        server.broadcastPhase("WORD_SUBMITTING");

        votes.clear();

        currentTurnIndex = 0;

        for (Player p : state.getPlayers()) {
            p.setSubmittedWord(null);
        }

        sendSecretInfo();

        beginCurrentPlayerTurn();
    }

    private void sendSecretInfo() {
    	
        for (Player p : state.getPlayers()) {
            if (p.getRole() == Player.Role.IMPOSTOR) {
                sendTo(
                        p,
                        MessageType.IMPOSTOR,
                        ""
                );
            } else {
                sendTo(
                        p,
                        MessageType.SEND_SECRET,
                        state.getSecretWord()
                );
            }
        }
    }

    private void beginCurrentPlayerTurn() {
    	
        List<Player> active = state.getActivePlayers();

        if (currentTurnIndex >= active.size()) {
            finishWordPhase();
            return;
        }

        Player current = active.get(currentTurnIndex);

        // disable everyone first
        for (Player p : state.getPlayers()) {
            sendTo(
                    p,
                    MessageType.DISABLE_INPUT,
                    ""
            );
        }

        // enable current player only
        sendTo(
                current,
                MessageType.ENABLE_INPUT,
                MessageType.SEND_WORD.name()
        );

        server.broadcastSystem("\n" + current.getName() + "'s turn to submit a word.");
    }

    public void receiveWord(String playerName, String word) {
    	
        Player current = state.getActivePlayers().get(currentTurnIndex);

        // not player's turn
        if (!current.getName().equals(playerName)) {
            sendError(
                    playerName,
                    "It is not your turn."
            );

            return;
        }

        Player player = find(playerName);

        if (player == null || player.isEliminated()) {
            return;
        }

        // crewmates cannot submit secret word
        if (player.getRole() == Player.Role.CREWMATE && word.equalsIgnoreCase(state.getSecretWord())) {
            sendError(
                    playerName,
                    "You cannot use the secret word."
            );
            
            sendTo(
                    player,
                    MessageType.ENABLE_INPUT,
                    MessageType.SEND_WORD.name()
            );

            return;
        }

        player.setSubmittedWord(word);

        sendTo(
                player,
                MessageType.DISABLE_INPUT,
                ""
        );

        server.broadcastSystem(playerName + " : " + word);

        currentTurnIndex++;

        beginCurrentPlayerTurn();
    }

    private void finishWordPhase() {
    	
        server.broadcastSystem("\n----- Submitted Words -----");

        // summary
        for (Player p : state.getActivePlayers()) {
            String text = p.getName() + " : " + p.getSubmittedWord();

            server.broadcast(
                    new Message(
                            MessageType.SYSTEM,
                            text
                    )
            );
        }

        startVotingPhase();
    }

    private void startVotingPhase() {
    	
        state.setPhase(GameState.GamePhase.VOTING);

        server.broadcastPhase("VOTING");

        votes.clear();

        for (Player p : state.getPlayers()) {
            if (p.isEliminated()) {
                sendTo(
                        p,
                        MessageType.DISABLE_INPUT,
                        ""
                );

                continue;
            }

            sendTo(
                    p,
                    MessageType.ENABLE_INPUT,
                    MessageType.VOTE.name()
            );
        }

        List<String> options = state.getActivePlayers()
                .stream()
                .map(Player::getName)
                .toList();

        server.broadcastSystem(
                "\nVote for the impostor. Type EXACT name.\n--- Options ---\n" +
                String.join("\n", options) + "\n"
        );
    }

    public void receiveVote(String voter, String target) {

        Player voterPlayer = find(voter);

        if (voterPlayer == null || voterPlayer.isEliminated()) {
            return;
        }

        // already voted
        if (votes.containsKey(voter)) {
            sendError(voter, "You already voted.");
            return;
        }

        // validate vote input
        Player targetPlayer = find(target);

        if (targetPlayer == null) {
            sendError(voter, "Invalid vote.");
            return;
        }

        if (targetPlayer.isEliminated()) {
            sendError(voter, "You cannot vote eliminated players.");
            return;
        }
        
        if (state.getPhase() == GameState.GamePhase.TIEBREAKER) {
            if (tiedPlayers.contains(target)) {
                sendError(voter, "You can only vote among tied players.");
                return;
            }
        }

        // valid vote
        votes.put(voter, target);

        sendTo(voterPlayer, MessageType.DISABLE_INPUT, "");

        sendSystem(voterPlayer, "(You voted " + target + ")");

        server.broadcastSystem(voter + " has voted.");

        // end voting once all active players have voted successfully
        if (votes.size() == state.getActivePlayers().size()) {
            resolveVotes();
        }
    }

    private void resolveVotes() {

        Map<String, Integer> tally = new LinkedHashMap<>();

        for (Player p : state.getActivePlayers()) {
            tally.put(
                    p.getName(),
                    0
            );
        }

        for (String vote : votes.values()) {
            tally.put(
                    vote,
                    tally.get(vote) + 1
            );
        }

        server.broadcastSystem("\n----- Voting Results -----");

        for (Map.Entry<String, Integer> e : tally.entrySet()) {
            server.broadcastSystem(e.getKey() + " : " + e.getValue() + " votes");
        }

        int max = Collections.max(tally.values());

        tiedPlayers = tally.entrySet()
                .stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .toList();

        // tie
        if (tiedPlayers.size() > 1) {
            startTieBreaker();
            return;
        }

        eliminate(tiedPlayers.get(0));

        checkGameOver();
    }

    private void startTieBreaker() {

        state.setPhase(GameState.GamePhase.TIEBREAKER);

        server.broadcastPhase("TIEBREAKER");

        tieStatements.clear();

        server.broadcastSystem("\nTie detected!");

        server.broadcastSystem(
                "Tied players : " +
                String.join(", ", tiedPlayers)
        );

        server.broadcastSystem("Tiebreaker starts now!\n");

        // disable everyone
        for (Player p : state.getPlayers()) {
            sendTo(p, MessageType.DISABLE_INPUT, "");
        }

        // enable only tied players
        for (String name : tiedPlayers) {
            Player p = find(name);

            if (p != null && !p.isEliminated()) {
                sendTo(
                        p,
                        MessageType.ENABLE_INPUT,
                        MessageType.SEND_STATEMENT.name()
                );

                sendSystem(
                        p,
                        "Send your defense statement."
                );
            }
        }
    }
    
    private void startTieBreakerVoting() {

        state.setPhase(GameState.GamePhase.VOTING);

        votes.clear();

        server.broadcastSystem("\nVote for the impostor. Type EXACT name.");

        List<Player> active = state.getActivePlayers();

        for (Player p : active) {
            sendTo(
                    p,
                    MessageType.ENABLE_INPUT,
                    MessageType.VOTE.name()
            );
        }
    }

    public void receiveStatement(String player, String statement) {

        if (!tiedPlayers.contains(player)) {
            return;
        }

        if (tieStatements.containsKey(player)) {
            sendError(
                    player,
                    "Already submitted."
            );

            return;
        }

        tieStatements.put(
                player,
                statement
        );

        sendTo(
                find(player),
                MessageType.DISABLE_INPUT,
                ""
        );

        server.broadcast(
                new Message(
                        MessageType.SYSTEM,
                        player + " : " + statement
                )
        );

        if (tieStatements.size() == tiedPlayers.size()) {
            finishTieBreaker();
        }
    }

    private void finishTieBreaker() {

        server.broadcastSystem("\n----- Defense Statements -----");

        for (Map.Entry<String, String> e : tieStatements.entrySet()) {
            server.broadcastSystem(e.getKey() + " : " + e.getValue());
        }

        startTieBreakerVoting();
    }

    private void eliminate(String playerName) {

        Player p = find(playerName);

        if (p == null) return;

        p.setEliminated(true);

        sendTo(
                p,
                MessageType.DISABLE_INPUT,
                ""
        );

        server.broadcast(
                new Message(
                        MessageType.PLAYER_ELIMINATED,
                        playerName
                )
        );

        server.broadcastSystem("\n" + playerName + " was eliminated.");
    }

    private void checkGameOver() {

        boolean impostorAlive =
                state.getActivePlayers()
                        .stream()
                        .anyMatch(
                                p -> p.getRole() ==
                                        Player.Role.IMPOSTOR
                        );

        if (!impostorAlive) {
            state.setPhase(GameState.GamePhase.GAME_OVER);

            server.broadcast(
                    new Message(
                            MessageType.CREWMATES_WIN,
                            impostorName
                    )
            );

            return;
        }

        if (state.getActivePlayers().size() <= 2) {
            state.setPhase(GameState.GamePhase.GAME_OVER);

            server.broadcast(
                    new Message(
                            MessageType.IMPOSTOR_WINS,
                            impostorName
                    )
            );

            return;
        }

        startWordPhase();
    }

    private Player find(String name) {
        return state.getPlayers()
                .stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private void sendTo(Player p, MessageType type, String content) {
        server.sendTo(
                p.getName(),
                new Message(type, content)
        );
    }

    private void sendSystem(Player p, String msg) {
        server.sendTo(
                p.getName(),
                new Message(
                        MessageType.SYSTEM,
                        msg
                )
        );
    }

    private void sendError(String player, String msg) {
        server.sendTo(
                player,
                new Message(
                        MessageType.ERROR,
                        msg
                )
        );
    }
}