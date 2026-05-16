package controller;

//import client.GameClient;

public class ScreenController {
    //private GameClient gameClient;
    private String currentScreen;

    public ScreenController() {
        //this.gameClient = gameClient;
        this.currentScreen = "TITLE";
    }

    public void showSecretWord(String word) {
        System.out.println("[SCREEN] Showing secret word: " + word);
        // In GUI implementation, update the display
    }

    public void showTurn(String message) {
        System.out.println("[SCREEN] Turn notification: " + message);
        // In GUI implementation, show input field
    }

    public void showVotingOptions(String options) {
        System.out.println("[SCREEN] Voting options: " + options);
        // In GUI implementation, show voting buttons
    }

    public void showTiebreaker(String message) {
        System.out.println("[SCREEN] Tiebreaker: " + message);
        // In GUI implementation, show statement input
    }

    public void showSubmittedWords(String words) {
        System.out.println("[SCREEN] Submitted words:\n" + words);
        // In GUI implementation, show word list
    }

    public void showElimination(String playerName) {
        System.out.println("[SCREEN] Player eliminated: " + playerName);
        // In GUI implementation, update player list
    }

    public void changePhase(String phase) {
        currentScreen = phase;
        System.out.println("[SCREEN] Phase changed to: " + phase);
        // In GUI implementation, switch screens
    }

    public void showGameOver(String winner, String impostorName) {
        System.out.println("[SCREEN] GAME OVER - Winner: " + winner + ", Impostor: " + impostorName);
        // In GUI implementation, show game over screen
    }

    public String getCurrentScreen() {
        return currentScreen;
    }
}