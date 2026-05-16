package controller;

import model.GameState;
import model.WordDictionary;
import utils.Player;
import utils.Message;
import utils.MessageType;
import server.GameServer;

import java.util.*;
import java.util.stream.Collectors;

public class GameController {
	//Current value of a given game state
	private GameState state;
	private GameServer server;
	private Map<String, String> votes;
	private Map<String, String> tieStatements;
	private String impostorName;
	private boolean turnCompleted;
	private int submittedCount;
	private int voteCount;
	private int statementCount;
	private int tieVoteCount;
	private boolean votingComplete;
	private Object voteLock = new Object();

	public GameController(String[] names, GameServer server) {
		this.server = server;
		this.votes = new HashMap<>();
		this.tieStatements = new HashMap<>();

		List<Player> players = new ArrayList<>();
		for (String name : names) {
			players.add(new Player(name));
		}

		String secretWord = WordDictionary.getRandomWord();

		Random rand = new Random();
		int impostorIndex = rand.nextInt(players.size());
		players.get(impostorIndex).setRole(Player.Role.IMPOSTOR);
		this.impostorName = players.get(impostorIndex).getName();

		this.state = new GameState(players, secretWord);

		System.out.println("\n=========================================");
		System.out.println("GAME INITIALIZED");
		System.out.println("Secret Word: " + secretWord);
		System.out.println("Impostor: " + impostorName);
		System.out.println("Players: " + String.join(", ", names));
		System.out.println("=========================================\n");
	}

	public void startNetworkGame() {
		while (state.getPhase() != GameState.GamePhase.GAME_OVER) {
			System.out.println("\n========== ROUND " + state.getCurrentRound() + " ==========");

			state.setPhase(GameState.GamePhase.WORD_SUBMITTING);
			broadcastPhaseChange("WORD_SUBMITTING");
			wordSubmissionPhase();

			state.setPhase(GameState.GamePhase.VOTING);
			broadcastPhaseChange("VOTING");
			votingPhase();

			if (checkGameOver()) {
				break;
			}

			state.nextRound();
		}
	}

	private void wordSubmissionPhase() {
		List<Player> activePlayers = state.getActivePlayers();
		state.resetTurns();
		submittedCount = 0;

		for (Player player : activePlayers) {
			if (player.getRole() == Player.Role.CREWMATE) {
				sendToPlayer(player.getName(), new Message(MessageType.SEND_SECRET, state.getSecretWord()));
			} else {
				sendToPlayer(player.getName(), new Message(MessageType.SEND_SECRET, "??? (You are the IMPOSTOR!)"));
			}
		}

		server.broadcast(new Message(MessageType.SEND_WORD, "\n========== WORD SUBMISSION PHASE =========="));

		while (!state.isAllTurnsComplete()) {
			Player currentPlayer = state.getCurrentPlayer();
			if (currentPlayer == null) break;

			turnCompleted = false;

			System.out.println("[GAME] " + currentPlayer.getName() + "'s turn");
			server.broadcast(new Message(MessageType.TURN, currentPlayer.getName(), "is now describing the word..."));
			sendToPlayer(currentPlayer.getName(),
					new Message(MessageType.TURN, "YOUR TURN! Enter a word related to the secret word (cannot be the secret word itself):"));

			while (!turnCompleted) {
				try { Thread.sleep(100); } catch (InterruptedException e) { return; }
			}

			state.nextTurn();
		}

		System.out.println("[GAME] All words submitted!");
		server.broadcast(new Message(MessageType.SEND_WORD, "\n========== ALL WORDS SUBMITTED =========="));
		showSubmittedWordsSummary();
	}

	private void votingPhase() {
		List<Player> activePlayers = state.getActivePlayers();
		int totalActivePlayers = activePlayers.size();
		voteCount = 0;
		votes.clear();
		votingComplete = false;

		System.out.println("[GAME] Starting voting phase with " + totalActivePlayers + " players");
		server.broadcast(new Message(MessageType.SEND_WORD, "\n========== VOTING PHASE =========="));

		List<String> votableNames = activePlayers.stream().map(Player::getName).collect(Collectors.toList());
		String votableList = String.join(", ", votableNames);

		server.broadcast(new Message(MessageType.SEND_WORD, "Vote for who you think is the impostor!"));
		server.broadcast(new Message(MessageType.SEND_WORD, "Options: " + votableList));

		// Send voting prompts to all players
		for (Player voter : activePlayers) {
			sendToPlayer(voter.getName(), new Message(MessageType.VOTE, "Your turn to vote. Options: " + votableList));
		}

		System.out.println("[GAME] Waiting for " + totalActivePlayers + " votes...");

		// Wait for all votes
		synchronized (voteLock) {
			while (voteCount < totalActivePlayers) {
				try {
					voteLock.wait(1000);
					System.out.println("[GAME] Votes received: " + voteCount + "/" + totalActivePlayers);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}

		System.out.println("[GAME] All votes received! Tallying results...");

		// Tally votes
		Map<String, Integer> voteTally = new HashMap<>();
		for (Player player : activePlayers) {
			voteTally.put(player.getName(), 0);
		}

		for (Map.Entry<String, String> entry : votes.entrySet()) {
			String votedFor = entry.getValue();
			if (voteTally.containsKey(votedFor)) {
				voteTally.put(votedFor, voteTally.get(votedFor) + 1);
			}
		}

		// Broadcast vote results
		server.broadcast(new Message(MessageType.SEND_WORD, "\n========== VOTE RESULTS =========="));
		for (Map.Entry<String, Integer> entry : voteTally.entrySet()) {
			String result = entry.getKey() + ": " + entry.getValue() + " votes";
			server.broadcast(new Message(MessageType.SEND_WORD, result));
			System.out.println(result);
		}

		// Find max votes
		int maxVotes = Collections.max(voteTally.values());
		List<String> tiedPlayers = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : voteTally.entrySet()) {
			if (entry.getValue() == maxVotes) {
				tiedPlayers.add(entry.getKey());
			}
		}

		String eliminatedPlayer;

		if (tiedPlayers.size() > 1) {
			System.out.println("[GAME] Tie detected between: " + String.join(", ", tiedPlayers));
			server.broadcast(new Message(MessageType.SEND_WORD, "\n[TIE] Tie detected between: " + String.join(", ", tiedPlayers)));
			eliminatedPlayer = tiebreakerPhase(tiedPlayers);
		} else {
			eliminatedPlayer = tiedPlayers.get(0);
			System.out.println("[GAME] Player eliminated: " + eliminatedPlayer);
			server.broadcast(new Message(MessageType.SEND_WORD, "\n[ELIMINATED] " + eliminatedPlayer));
		}

		// Eliminate player
		for (Player player : state.getActivePlayers()) {
			if (player.getName().equals(eliminatedPlayer)) {
				player.setEliminated(true);
				break;
			}
		}

		server.broadcast(new Message(MessageType.PLAYER_ELIMINATED, eliminatedPlayer));
		try { Thread.sleep(2000); } catch (InterruptedException e) {}
	}

	private String tiebreakerPhase(List<String> tiedPlayers) {
		state.setPhase(GameState.GamePhase.TIEBREAKER);
		broadcastPhaseChange("TIEBREAKER");

		System.out.println("[GAME] TIEBREAKER ROUND");
		server.broadcast(new Message(MessageType.SEND_STATEMENT, "\n========== TIEBREAKER ROUND =========="));
		server.broadcast(new Message(MessageType.SEND_STATEMENT, "Tied players: " + String.join(", ", tiedPlayers)));

		// Reset for tiebreaker
		tieStatements.clear();
		statementCount = 0;
		votes.clear(); // Clear previous votes
		tieVoteCount = 0;

		// Get defense statements from tied players
		for (String playerName : tiedPlayers) {
			server.broadcast(new Message(MessageType.SEND_STATEMENT, playerName + " is now defending..."));
			sendToPlayer(playerName,
					new Message(MessageType.SEND_STATEMENT, "Defend yourself! Why should you not be eliminated? Enter your statement:"));
		}

		// Wait for statements (30 seconds max)
		int maxWait = 30;
		while (statementCount < tiedPlayers.size() && maxWait > 0) {
			try {
				Thread.sleep(1000);
				maxWait--;
				System.out.println("[GAME] Waiting for statements... " + statementCount + "/" + tiedPlayers.size());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		// If any player didn't submit, add default statement
		for (String playerName : tiedPlayers) {
			if (!tieStatements.containsKey(playerName)) {
				tieStatements.put(playerName, "[No statement provided]");
				System.out.println("[GAME] " + playerName + " did not submit a statement");
			}
		}

		// Broadcast all defense statements
		server.broadcast(new Message(MessageType.SEND_STATEMENT, "\n--- DEFENSE STATEMENTS ---"));
		for (Map.Entry<String, String> entry : tieStatements.entrySet()) {
			server.broadcast(new Message(MessageType.SEND_STATEMENT, entry.getKey() + ": \"" + entry.getValue() + "\""));
		}

		// Tiebreaker voting - ALL active players vote
		List<Player> activePlayers = state.getActivePlayers();
		List<String> voters = activePlayers.stream().map(Player::getName).collect(Collectors.toList());
		int totalVoters = voters.size();

		String votableList = String.join(", ", tiedPlayers);

		System.out.println("[GAME] Tiebreaker - Total voters: " + totalVoters);
		System.out.println("[GAME] Tiebreaker - Voters list: " + voters);
		System.out.println("[GAME] Tiebreaker - Candidates (valid votes): " + tiedPlayers);

		server.broadcast(new Message(MessageType.SEND_STATEMENT, "\n--- TIEBREAKER VOTE ---"));
		server.broadcast(new Message(MessageType.SEND_STATEMENT, "All players must now vote for the impostor."));
		server.broadcast(new Message(MessageType.SEND_STATEMENT, "Valid options: " + votableList));
		server.broadcast(new Message(MessageType.SEND_STATEMENT, "You must type the EXACT name from the options above!"));

		// Send vote prompts to EACH voter individually
		for (String voter : voters) {
			String voteMessage = "Your turn to vote. Valid options: " + votableList;
			sendToPlayer(voter, new Message(MessageType.VOTE, voteMessage));
			System.out.println("[GAME] Sent vote prompt to: " + voter);
		}

		// Wait for all tiebreaker votes
		System.out.println("[GAME] Waiting for " + totalVoters + " tiebreaker votes...");

		int waitSeconds = 60;
		while (tieVoteCount < totalVoters && waitSeconds > 0) {
			try {
				Thread.sleep(1000);
				waitSeconds--;
				System.out.println("[GAME] Tiebreaker votes received: " + tieVoteCount + "/" + totalVoters);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		System.out.println("[GAME] All tiebreaker votes received or timeout!");

		// Print all votes received for debugging
		System.out.println("[GAME] All votes in tiebreaker:");
		for (Map.Entry<String, String> entry : votes.entrySet()) {
			System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
		}

		// Count votes for tied players only (ignore invalid votes)
		Map<String, Integer> tieVotes = new HashMap<>();
		for (String player : tiedPlayers) {
			tieVotes.put(player, 0);
		}

		int invalidVotes = 0;
		for (Map.Entry<String, String> entry : votes.entrySet()) {
			String votedFor = entry.getValue();
			if (tiedPlayers.contains(votedFor)) {
				tieVotes.put(votedFor, tieVotes.get(votedFor) + 1);
				System.out.println("[GAME] Valid vote: " + entry.getKey() + " -> " + votedFor);
			} else {
				invalidVotes++;
				System.out.println("[GAME] INVALID vote ignored: " + entry.getKey() + " -> " + votedFor + " (not in options)");
				// Notify the player who voted invalidly
				sendToPlayer(entry.getKey(), new Message(MessageType.ERROR, "Invalid vote! You must vote for one of: " + votableList));
			}
		}

		if (invalidVotes > 0) {
			System.out.println("[GAME] " + invalidVotes + " invalid votes were ignored");
		}

		// Broadcast results
		server.broadcast(new Message(MessageType.SEND_STATEMENT, "\n--- TIEBREAKER RESULTS ---"));
		for (Map.Entry<String, Integer> entry : tieVotes.entrySet()) {
			String result = entry.getKey() + ": " + entry.getValue() + " votes";
			server.broadcast(new Message(MessageType.SEND_STATEMENT, result));
			System.out.println(result);
		}

		if (invalidVotes > 0) {
			server.broadcast(new Message(MessageType.SEND_STATEMENT, "\n[WARNING] " + invalidVotes + " invalid votes were ignored!"));
		}

		// Find eliminated player (player with most valid votes)
		int maxVotes = Collections.max(tieVotes.values());
		List<String> eliminatedCandidates = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : tieVotes.entrySet()) {
			if (entry.getValue() == maxVotes) {
				eliminatedCandidates.add(entry.getKey());
			}
		}

		String eliminated;
		if (eliminatedCandidates.size() > 1) {
			// Still tied? Randomly eliminate one
			Random rand = new Random();
			eliminated = eliminatedCandidates.get(rand.nextInt(eliminatedCandidates.size()));
			System.out.println("[GAME] Still tied after tiebreaker! Randomly eliminating: " + eliminated);
			server.broadcast(new Message(MessageType.SEND_STATEMENT, "\n[WARNING] Still tied! Randomly eliminating: " + eliminated));
		} else {
			eliminated = eliminatedCandidates.get(0);
		}

		server.broadcast(new Message(MessageType.SEND_STATEMENT, "\n[RESULT] " + eliminated + " has been eliminated!"));

		// Clear votes after tiebreaker
		votes.clear();

		return eliminated;
	}

	private void showSubmittedWordsSummary() {
		StringBuilder words = new StringBuilder();
		words.append("\n========== SUBMITTED WORDS ==========\n");
		for (Player player : state.getActivePlayers()) {
			words.append(player.getName()).append(": \"").append(player.getSubmittedWord()).append("\"\n");
		}
		server.broadcast(new Message(MessageType.SEND_WORD, words.toString()));
	}

	private boolean checkGameOver() {
		List<Player> activePlayers = state.getActivePlayers();
		boolean impostorAlive = activePlayers.stream().anyMatch(p -> p.getRole() == Player.Role.IMPOSTOR);

		if (!impostorAlive) {
			server.broadcast(new Message(MessageType.CREWMATES_WIN, impostorName));
			state.setPhase(GameState.GamePhase.GAME_OVER);
			broadcastPhaseChange("GAME_OVER");
			return true;
		}

		if (activePlayers.size() == 2 && impostorAlive) {
			server.broadcast(new Message(MessageType.IMPOSTOR_WINS, impostorName));
			state.setPhase(GameState.GamePhase.GAME_OVER);
			broadcastPhaseChange("GAME_OVER");
			return true;
		}

		return false;
	}

	private void broadcastPhaseChange(String phase) {
		server.broadcast(new Message(MessageType.PHASE_CHANGE, phase));
	}

	private void sendToPlayer(String playerName, Message message) {
		for (server.ClientHandler client : server.getClients()) {
			if (playerName.equals(client.getPlayerName())) {
				client.sendMessage(message);
				break;
			}
		}
	}

	public void receiveWord(String playerName, String word) {
		for (Player player : state.getActivePlayers()) {
			if (player.getName().equals(playerName)) {
				if (player.getRole() == Player.Role.CREWMATE && word.equalsIgnoreCase(state.getSecretWord())) {
					sendToPlayer(playerName, new Message(MessageType.ERROR, "You cannot submit the secret word!"));
					return;
				}

				player.setSubmittedWord(word);
				turnCompleted = true;
				submittedCount++;
				System.out.println("[GAME] " + playerName + " submitted: " + word);
				server.broadcast(new Message(MessageType.SEND_WORD, playerName, word));
				break;
			}
		}
	}

	public void receiveVote(String voterName, String votedName) {
		System.out.println("[DEBUG] receiveVote - Voter: " + voterName + ", Voted: " + votedName);
		System.out.println("[DEBUG] Current phase: " + state.getPhase());

		synchronized (voteLock) {
			// Check if this player already voted
			if (votes.containsKey(voterName)) {
				System.out.println("[DEBUG] Player " + voterName + " already voted! Ignoring duplicate.");
				sendToPlayer(voterName, new Message(MessageType.ERROR, "You already voted!"));
				return;
			}

			boolean isValidVote = false;
			List<String> validOptions = new ArrayList<>();

			if (state.getPhase() == GameState.GamePhase.VOTING) {
				// Get all active players as valid options
				validOptions = state.getActivePlayers().stream()
						.map(Player::getName)
						.collect(Collectors.toList());
				isValidVote = validOptions.contains(votedName);

			} else if (state.getPhase() == GameState.GamePhase.TIEBREAKER) {
				// Get tied players as valid options
				// Need to get tied players from current tiebreaker
				// For now, get all active players
				validOptions = state.getActivePlayers().stream()
						.map(Player::getName)
						.collect(Collectors.toList());
				isValidVote = validOptions.contains(votedName);
			}

			if (!isValidVote) {
				System.out.println("[GAME] Invalid vote from " + voterName + " for '" + votedName + "'");
				System.out.println("[GAME] Valid options: " + validOptions);

				// Send error and ask for vote again
				String errorMsg = "Invalid vote! '" + votedName + "' is not a valid option.\nValid options: " + String.join(", ", validOptions) + "\nPlease vote again:";
				sendToPlayer(voterName, new Message(MessageType.ERROR, errorMsg));

				// Send the vote prompt again
				String votableList = String.join(", ", validOptions);
				sendToPlayer(voterName, new Message(MessageType.VOTE, "Your turn to vote. Options: " + votableList));
				return; // Don't count this vote, wait for retry
			}

			sendToPlayer(voterName, new Message(MessageType.VOTE_ACCEPTED, "Your vote for '" + votedName + "' has been recorded!"));
			// Valid vote - count it
			votes.put(voterName, votedName);

			if (state.getPhase() == GameState.GamePhase.VOTING) {
				voteCount++;
				System.out.println("[GAME] Vote " + voteCount + ": " + voterName + " -> " + votedName);
				server.broadcast(new Message(MessageType.SEND_WORD, "[VOTE] " + voterName + " voted for " + votedName));
			} else if (state.getPhase() == GameState.GamePhase.TIEBREAKER) {
				tieVoteCount++;
				System.out.println("[GAME] Tiebreaker vote " + tieVoteCount + ": " + voterName + " -> " + votedName);
				server.broadcast(new Message(MessageType.SEND_STATEMENT, "[VOTE] " + voterName + " voted for " + votedName));
			}

			voteLock.notifyAll();
		}
	}

	public void receiveStatement(String playerName, String statement) {
		synchronized (voteLock) {
			if (!tieStatements.containsKey(playerName)) {
				tieStatements.put(playerName, statement);
				statementCount++;
				System.out.println("[GAME] Statement from " + playerName + ": " + statement);
				server.broadcast(new Message(MessageType.SEND_STATEMENT, playerName + " said: \"" + statement + "\""));
				voteLock.notifyAll();
			}
		}
	}
}