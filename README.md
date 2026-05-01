# Guess The Impostor


## How to Run

## Game Mechanics

## Project Structure
`main` 

`model` 
    
    - `Player.java`: Player structure that holds players their values, such as name, role, eliminated status, and word submitted for that round. Roles are divided into IMPOSTOR and CREWMATE
    
    - `GameState.java`: Game State structure that holds the game's data, such as the current game phase, round number, secret word, and non-eliminated players.

`view`

`controller`
    - `GameController.java`: Contains the code that has the game logic. Assigns the player roles and secret word, and manages the game phases.

`utils`
   
    - `WordDictionary.java`: Stores the possible list of words the game can utilize. Selects the word using Random
   
    - `Constants.java`