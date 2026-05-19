package controller;

import utils.MessageType;
import utils.Player;

import java.util.List;

public interface GameEventListener {

    void onMessage(String message);
    
    void onPhaseChange(String phase);

    void onPlayersUpdated(List<Player> players);

    void onSecretWord(String word);

    void onImpostorMessage();

    void onGameOver(String message);

    void onStartGame();
    
    void onInputEnabled(MessageType inputType);
    
    void onInputDisabled();
}