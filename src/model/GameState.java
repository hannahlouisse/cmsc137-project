package model;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import utils.Player;

public class GameState {

    //Game Phases
    public enum GamePhase {
        WORD_SUBMITTING, VOTING, TIEBREAKER, GAME_OVER
    }

    private List<Player> players; //List of current players
    private String secretWord;
    private int currentRound;
    private GamePhase phase;
    private int currentTurnIndex; // Track whose turn it is

    //Setting up the game
    public GameState(List<Player> players, String secretWord) {
        this.players = new ArrayList<>(players);
        this.secretWord = secretWord;
        this.currentRound = 1;
        this.phase = GamePhase.WORD_SUBMITTING;
        this.currentTurnIndex = 0;
    }

    //GETTERS
    public List<Player> getPlayers() {
        return players;
    }
    public String getSecretWord() {
        return secretWord;
    }
    public int getCurrentRound() {
        return currentRound;
    }
    public GamePhase getPhase() {
        return phase;
    }

    //SETTERS
    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    //Increments if next round
    public void nextRound() {
        currentRound++;
        currentTurnIndex = 0; // Reset turn index for new round
    }

    public List<Player> getActivePlayers() {
        return players.stream().filter(p -> !p.isEliminated()).collect(Collectors.toList());
    }

    // Get current player for sequential turns
    public Player getCurrentPlayer() {
        List<Player> active = getActivePlayers();
        if (currentTurnIndex < active.size()) {
            return active.get(currentTurnIndex);
        }
        return null;
    }

    // Move to next player's turn
    public void nextTurn() {
        currentTurnIndex++;
    }

    // Check if all players have taken their turn
    public boolean isAllTurnsComplete() {
        return currentTurnIndex >= getActivePlayers().size();
    }

    // Reset turns for new round
    public void resetTurns() {
        currentTurnIndex = 0;
    }
}