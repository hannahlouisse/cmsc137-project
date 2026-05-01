package controller;

import model.GameState;
import model.Player;
import utils.WordDictionary;
import java.util.*;

public class GameController {
	//Current value of a given game state
	private GameState state;
	
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
	
	//Game Loop
	public void start (Scanner scanner) {
		//Loop the game until not game over
		while (state.getPhase() != GameState.GamePhase.GAME_OVER) {
			System.out.println("\nRound #" + state.getCurrentRound());
			
			state.setPhase(GameState.GamePhase.WORD_SUBMITTING);
			wordGivingPhase(scanner);
			showSubmittedWords();
			state.setPhase(GameState.GamePhase.VOTING);
			votingPhase(scanner);
			gameOverPhase();
			
			//Check if current phase is not game over. If not, increment round and go back to word submission phase then reset all submitted words
			if (state.getPhase() != GameState.GamePhase.GAME_OVER) {
				state.nextRound();
				state.setPhase(GameState.GamePhase.WORD_SUBMITTING);
				resetSubmittedWords();
			}
			
		}
	}
	
	//GAME FUNCTIONS
	
	//Assigns words to players
	private void wordGivingPhase(Scanner scanner) {
		for (Player player : state.getActivePlayers()) {
			System.out.println("\n" + player.getName() + "'s turn.");
			
			//Prints the word depending on the role. If impostor, don't give the secret word
			if (player.getRole() == Player.Role.CREWMATE) {
				System.out.println(state.getSecretWord());
			} else {
				System.out.println("You are the impostor. Blend in with the crewmates.");
			}
			
			String submitted;
			while (true) {
				System.out.print("Enter your word: ");
				submitted = scanner.nextLine().trim();
				
				if (submitted.isEmpty()) {
					System.out.println("Submitted word cannot be empty. Try again");
					continue;
				}
				
				if (player.getRole() == Player.Role.CREWMATE && submitted.equalsIgnoreCase(state.getSecretWord())){
					System.out.println("You cannot submit the secret word!");
					continue;
				}
				
				break;
			}
			player.setSubmittedWord(submitted);
		}
	}
	
	private void votingPhase(Scanner scanner) {
		state.setPhase(GameState.GamePhase.VOTING); //set phase to voting
		System.out.println("VOTING ROUND: Vote who you think is the impostor");
		
		List<Player> activePlayers = state.getActivePlayers();
		Map<Player, Integer> voteTally = new HashMap<>();
		
		//Initialize current hash map
		for (Player players : activePlayers) {
			voteTally.put(players, 0);
		}
		
		//Iterates all active players for voting proper
		for (Player voter : activePlayers) {
			System.out.println(voter.getName() + "'s turn to vote for the impostor.");
			
			//For temporary list of players that can be voted
			List<Player> options = new ArrayList<>();
			//To print number properly for display purposes
			int number = 1;

			//Makes sure voter doesn't see their own name as options
			for (Player players : activePlayers) {
				//Skip if the current player is the voter
			    if (!players.equals(voter)) {
			        System.out.println(number + ". " + players.getName());
			        options.add(players);
			        number++;
			    }
			}
			
			//Checks if choice is valid. Asks again if not
			int choice;
			while (true) {
				System.out.print("Enter number to vote: ");
				String input = scanner.nextLine().trim();
				try {
					choice = Integer.parseInt(input) - 1;
					if (choice >= 0 && choice < options.size()) {
						break;
					}
					System.out.println("Invalid choice. Try again.");
					
				} catch (NumberFormatException e) {
					System.out.println("Input a number.");
				}
			}
			
			//Gets the name of who is voted and adds 1 to the hash map
			Player voted = options.get(choice);
			voteTally.put(voted, voteTally.get(voted) + 1);
		}
		
		//Get the max votes in the hashmap
		int maxVotes = Collections.max(voteTally.values());

		//Lists all possible tied players. If it matches number of maxVotes, add to tiedPlayers
		List<Player> tiedPlayers = new ArrayList<>();
		for (Player players : activePlayers) {
		    if (voteTally.get(players) == maxVotes) {
		        tiedPlayers.add(players);
		    }
		}
	   
		//If more than one players tied, start tiebreaker round
		if (tiedPlayers.size() > 1) {
		    System.out.println("\nTie detected! Starting tiebreaker round...");
		    Player eliminatedPlayer = tieBreaker(scanner, tiedPlayers);
		    
		    System.out.println("\n" + eliminatedPlayer.getName() + " has now been eliminated.");
		    eliminatedPlayer.setEliminated(true);
		    return;
		}
		
		//If no tie, simply get the one with highest max votes
		Player eliminatedPlayer = tiedPlayers.get(0);
		System.out.println("\n" + eliminatedPlayer.getName() + " has now been eliminated.");
		eliminatedPlayer.setEliminated(true);
	}
	
	//Tiebreaker round
	private Player tieBreaker(Scanner scanner, List<Player> tiedPlayers) {
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
	}
	
	//Prints all submitted words
	private void showSubmittedWords() {
		System.out.println("\n--- Submitted Words ---");
		
		//Get active players and print their submitted word
        for (Player player : state.getActivePlayers()) {
            System.out.println(player.getName() + ": " + player.getSubmittedWord());
        }
	}
	
	//Resets all submitted words per round
	private void resetSubmittedWords() {
		for (Player player : state.getPlayers()) {
			player.setSubmittedWord(null);
		}
	}
	
	//Checks for end game conditions
	private void gameOverPhase() {
		if (state.getPhase() == GameState.GamePhase.GAME_OVER) return;
		
		//Get current active players
		List<Player> activePlayers = state.getActivePlayers();
		
		//Checks if impostor is still alive by if Impostor is still there
		boolean impostorAlive = activePlayers.stream().anyMatch(p -> p.getRole() == Player.Role.IMPOSTOR);
		
		if (!impostorAlive) {
			System.out.println("\nYou all got the impostor. Crewmate wins!");
			state.setPhase(GameState.GamePhase.GAME_OVER);
			return;
		}
		
		//Checks if active players is now two and impostor is alive
        if (activePlayers.size() == 2 && impostorAlive) {
            System.out.println("\nImpostor is among the last two. Impostor wins!");
            state.setPhase(GameState.GamePhase.GAME_OVER);
            return;
        }
	}
}
