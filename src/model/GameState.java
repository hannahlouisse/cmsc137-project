package model;

import java.util.List;

import utils.Player;

public class GameState {

    //Game Phases
    public enum GamePhase {
        WORD_SUBMITTING, VOTING, GAME_OVER
    }
    
    private List<Player> players; //List of current players
    private String secretWord;
    private int currentRound;
    private GamePhase phase;

    //Setting up the game
    public GameState(List<Player> players, String secretWord) {
        this.players = players;
        this.secretWord = secretWord;
        this.currentRound = 1;
        this.phase = GamePhase.WORD_SUBMITTING;
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
    }

    public List<Player> getActivePlayers() {
        return players.stream().filter(p -> !p.isEliminated()).toList();
    }
}