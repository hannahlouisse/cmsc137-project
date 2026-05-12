package controller;

import model.GameState;
import model.WordDictionary;
import utils.Player;
import utils.Message;
import utils.MessageType;

import java.util.*;

public class GameController {
	//Current value of a given game state
	private GameState state;
	
	//Hash maps for tallying and storing
	private Map<String, Integer> voteTally = new HashMap<>(); //Voting proper
	private Set<String> playersWhoVoted = new HashSet<>();
	
	//Tiebreaker voting storage
	//private Map<Player, Integer> tiebreakerTally = new HashMap<>();
	//private Map<Player, Integer> statements = new HashMap<>();
	//List<Player> tiebreakerVoters = new ArrayList<>();
	
	//Set up the game, assign roles, and words
	public GameController(String[] names) { //holds all players 
		List<Player> players = new ArrayList<>(); //Array list for dynamic changing. Stores non-eliminated players
		for (String name : names ) {
			players.add(new Player(name));
		}
		
		//Assign secret word
		String secretWord = WordDictionary.getRandomWord();
		
		//Assign impostor
		Random rand = new Random();
		int impostorIndex = rand.nextInt(players.size());
		players.get(impostorIndex).setRole(Player.Role.IMPOSTOR);
		
		this.state = new GameState(players, secretWord);
	}
	
	//Called by GameServer
	//Returns messages to be sent to server then to clients
	public List<Message> start () {
		List<Message> messages = new ArrayList<>();
		
		for (Player player : state.getPlayers()) {
			if (player.getRole() == Player.Role.CREWMATE) {
				messages.add(new Message(MessageType.SEND_SECRET, player.getName(), state.getSecretWord()));
			} else { //If impostor, don't send secret word
				messages.add(new Message(MessageType.SEND_SECRET, player.getName(), "???"));
			}
		}
		
		//After roles and secret words are given
		messages.add(new Message(MessageType.PHASE_CHANGE, "WORD_SUBMITTING"));
		
		//Return the messages that will be sent to the clients
		return messages;
	}
	
	//GAME FUNCTIONS
	//Processes the messages received from server
	public List<Message> processMessage(Message message) {
		switch(message.getType()) {
			case SEND_WORD -> {
				return wordGivingPhase(message);
			}
			case VOTE -> {
				return votingPhase(message);
			}
			case SEND_STATEMENT -> {
				return new ArrayList<>(); //PLACEHOLDER 
			}
			default -> {
				return new ArrayList<>();
			}
		}
	}
	
	//Assigns words to players
	private List<Message> wordGivingPhase(Message message) {
		List<Message> messages = new ArrayList<>();
		//Deconstruct message
		String playerName = message.getSender();
		String submitted = message.getContent();
		
		//Loop the active players and get their submitted word
		for (Player player : state.getActivePlayers()) {
			if (player.getName().equals(playerName)) {
				if (submitted.isEmpty()) {
					messages.add(new Message(MessageType.ERROR, "Submitted word cannot be empty. Try again"));
					return messages;
				}
				
				if (player.getRole() == Player.Role.CREWMATE && submitted.equalsIgnoreCase(state.getSecretWord())){
					messages.add(new Message(MessageType.ERROR, "You cannot submit the secret word. Try again"));
					return messages;
				}
				player.setSubmittedWord(submitted);
				break;	
			}
		}
		
		//Check if all players submitted
		boolean allSubmitted = state.getActivePlayers().stream().allMatch(p -> p.getSubmittedWord() != null);
		
		if (!allSubmitted) {
			return messages;
		}
		
		//Voting phase, make sure to clear beforehand
		voteTally.clear();	
		
		messages.add(new Message(MessageType.PHASE_CHANGE, GameState.GamePhase.VOTING.name()));
		return messages;
	}
	
	//Handles voting proper. If tiebreaker, use the tiebreaker() method
	private List<Message> votingPhase(Message message) {
	    List<Message> messages = new ArrayList<>();
	    String voter = message.getSender();
	    String vote = message.getContent();
	    
	    //Error if voter sends a SEND_VOTE again
	    if (playersWhoVoted.contains(voter)) {
	        messages.add(new Message(MessageType.ERROR, "You already voted"));
	        return messages;
	    }
	    
	    playersWhoVoted.add(voter);
	    
	    //Put voted target and + 1 to their votes.
	    voteTally.put(vote, voteTally.getOrDefault(vote, 0) + 1);
	    
	    //Check if all voted
	    int totalVotes = voteTally.values().stream().mapToInt(Integer::intValue).sum();
	    if (totalVotes < state.getActivePlayers().size()) {
	        ///Return empty list while waiting
	        return messages;
	    }
	    
	    //Check max votes. Use tiedPlayers to add the max voters/any potential tied players
	    List <String> tiedPlayers = new ArrayList<>();
	    int maxVotes = Collections.max(voteTally.values());
	    
	    //iterates voteTally and if it is equal to max vote, add name to tiedPlayers
	    //tiedPlayers can have 1 or more. If only 1, eliminate player meaning it has the max votes
	    for (Map.Entry<String, Integer> entry : voteTally.entrySet()) {
	        if (entry.getValue() == maxVotes) {
	            tiedPlayers.add(entry.getKey());
	        }
	    }
	    
	    voteTally.clear();
	    playersWhoVoted.clear();
	    
	    //If more than one players tied, start tiebreaker round
	    if (tiedPlayers.size() > 1) {
	        /*statements.clear();
	        tiebreakerTally.clear();
	        
	        //Create a hash map of the tied players
	        /*for (Player player : tiedPlayers) {
	        	tiebreakerTally.put(player, 0);
	        
	        
	        tiebreakerVoters.clear();
	        for (Player player : state.getActivePlayers()) {
	            if (!tiedPlayers.contains(player)) {
	                tiebreakerVoters.add(player);
	            }
	        }*/
	        
	        //Shift to tiebreaker if tie exists
	    	messages.add(new Message(MessageType.PHASE_CHANGE, GameState.GamePhase.TIEBREAKER.name()));
	        return messages;
	    }
	    
	    String eliminatedName = tiedPlayers.get(0);
	    Player eliminated = state.getActivePlayers().stream().filter(p -> p.getName().equals(eliminatedName)).findFirst().orElse(null);

	    if (eliminated != null) {
	        messages.addAll(gameOverPhase(eliminated));
	    }
	    
	    return messages;
	}
	
	//Tiebreaker round
	/*private List<Message> tieBreaker(Scanner scanner, List<Player> tiedPlayers) {
	    System.out.println("\nTIEBREAKER ROUND");
	    System.out.println("Tied players can now defend themselves.");

	    //Defending statements
	    Map<Player, String> statements = new HashMap<>();

	    for (Player players : tiedPlayers) {
	        System.out.println("\n" + players.getName() + ", you have 30 seconds to defend yourself.");
	        System.out.print("Enter your statement: ");
	        String statement = scanner.nextLine().trim();
	        statements.put(players, statement);
	    }

	    //Display statements
	    System.out.println("\nDefending Statements");
	    for (Player players : tiedPlayers) {
	        System.out.println(players.getName() + ": " + statements.get(players));
	    }

	    //Initialization if tied player voting map
	    Map<Player, Integer> votes = new HashMap<>();
	    for (Player p : tiedPlayers) {
	        votes.put(p, 0);
	    }
	    
	    //Voting of tied players
	    List<Player> voters = new ArrayList<>();
	    for (Player players : state.getActivePlayers()) {
	    	if (!tiedPlayers.contains(players)) {
	    		voters.add(players);
	    	}
	    }
	    
	    //Voting only for non-tied players
	    for (Player voter : voters) {
	        System.out.println("\n" + voter.getName() + ", vote among tied players:");

	        for (int i = 0; i < tiedPlayers.size(); i++) {
	            System.out.println((i + 1) + ". " + tiedPlayers.get(i).getName());
	        }

	        int choice;
	        while (true) {
	            System.out.print("Enter number: ");
	            try {
	                choice = Integer.parseInt(scanner.nextLine().trim()) - 1;
	                if (choice >= 0 && choice < tiedPlayers.size()) {
	                	break;
	                }
	                System.out.println("Invalid choice. Try again.");
	            } catch (NumberFormatException e) {
					System.out.println("Input a number.");
				}
	        }

	        Player voted = tiedPlayers.get(choice);
	        votes.put(voted, votes.get(voted) + 1);
	    }

	    //Get eliminated player
	    Player eliminated = votes.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
	    return eliminated;
	}*/
	
	
	//Resets all submitted words per round
	private void resetSubmittedWords() {
		for (Player player : state.getPlayers()) {
			player.setSubmittedWord(null);
		}
	}
	
	//Checks for end game conditions
	private List<Message> gameOverPhase(Player eliminated) {
		List<Message> messages = new ArrayList<>();
		//Set player as eliminated
		eliminated.setEliminated(true);
		messages.add(new Message(MessageType.PLAYER_ELIMINATED, eliminated.getName()));
		
		//Get current active players
		List<Player> activePlayers = state.getActivePlayers();
		
		//Checks if impostor is still alive by if Impostor is still there
		boolean impostorAlive = activePlayers.stream().anyMatch(p -> p.getRole() == Player.Role.IMPOSTOR);
		
		String impostorName = state.getPlayers().stream().filter(p -> p.getRole() == Player.Role.IMPOSTOR).findFirst().get().getName();
		//Crewmate Win
		if (!impostorAlive) {
			messages.add(new Message(MessageType.CREWMATES_WIN, impostorName));
			state.setPhase(GameState.GamePhase.GAME_OVER);
			return messages;
		}
		
		if (activePlayers.size() <= 2 && impostorAlive) {
			messages.add(new Message(MessageType.IMPOSTOR_WINS, impostorName));
			state.setPhase(GameState.GamePhase.GAME_OVER);
			return messages;
		}
		
		state.nextRound();
		resetSubmittedWords();
		messages.add(new Message(MessageType.PHASE_CHANGE, GameState.GamePhase.WORD_SUBMITTING.name()));
		
		return messages;
	}
	
	//ADDITIONAL GETTS FOR SERVER to check current players
	public List<Player> getPlayers(){
		return state.getActivePlayers();
	}
}
