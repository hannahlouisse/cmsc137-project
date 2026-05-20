# Guess The Impostor

## How to Run
1. Install Java26 and any Java IDE. For this project, Eclipse IDE for Java Developers was installed and jdk version 26.0.1 was used. 
2. Install JavaFX 26
3. Right click the main project folder `cmsc137-project` and click Properties
4. Click Java Build Path and go to the Libraries tab.
5. Select the `Modulepath` folder and add javafx 26 jar files that is located on your javafx 26 installed folder under \lib
6. Click Apply once done
7. Right click `Main.java` under src\main and click Run As. Under Run As, click Run Configurations
8. In Main Tab, make sure main.Main is written as the Main class
9. In Arguments Tab, add the following commands

--module-path <actual_path_to_javafx_lib_folder> --add-modules javafx.controls,javafx.fxml,javafx.media

--enable-native-access=javafx.graphics

--enable-native-access=javafx.media

## Game Mechanics
A multiplayer game inspired by Among Us. At the start of the game, all players will receive a secret word except one impostor who gets nothing. Players will take turns describing the word without saying it directly. After everyone has submitted a word, players will vote on who they think the impostor is. If a tie happens, a tiebreaker round will happen where tied players will be asked to give their statement to defend their case. Afterwards, a vote will happen again and if another tiebreaker were to happen, a random person from the tied players will be evicted. The impostor wins if they avoid detection by being one of the last two standing, while crewmates win if they correctly identify the impostor.

###  Roles
- `Impostor`
- `Crewmates`

## Project Structure
`main`

- `Main.java`: 

`assets`

- 

`client`

- 

`model` 
    
- `WordDictionary.java`: Stores the possible list of words the game can utilize. Selects the word using Random.
    
- `GameState.java`: Game State structure that holds the game's data, such as the current game phase, round number, secret word, and non-eliminated players.

`view`

`controller`

- `GameController.java`: Contains the code that has the game logic. Assigns the player roles and secret word, and manages the game phases such as word submission, voting, tiebreaker and end game detection.

- `GameEventListener.java`:

- `ScreenController.java`:

`server`

- `ClientHandler.java`:

- `GameServer.java`: 

`utils`

- `Message.java`: Constructs the message that is passed between client-server-logic.

- `MessageType.java`: Contains all message type important for determining the action needed for a message.

- `Player.java`: Sets up the player's name, role, isEliminated status, and submittedWord for the round.

`view`

- `ChatPanel.java`: 