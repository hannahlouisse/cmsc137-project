package controller;

import model.GameState;
import model.Player;
import utils.WordDictionary;

import java.util.*;

public class GameController {
	//Current value of a given game state
	private GameState value;
	
	//Set up the game, assign roles, and words
	public GameController(String[] name) { //holds all players 
		
	}
	
	//Game Loop
	public void start (Scanner scanner) {
		//Loop the game until not game over
		while (value.getPhase() != GameState.GamePhase.GAME_OVER) {
			
		}
	}
	
	//Game Functions
	private void wordGivingPhase(Scanner scanner) {
		
	}
	
	private void votingPhase(Scanner scanner) {
		
	}
	
	private void showSubmittedWords() {
		
	}
	
	private void resetSubmittedWords() {
		
	}
	
	private void gameOverPhase() {
		
	}
}
