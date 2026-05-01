package model;

public class Player {
	//Roles of player. Can be accessed using Player.Role.IMPOSTOR or Player.Role.CREWMATE
    public enum Role {
        IMPOSTOR, CREWMATE
    }

    private String name;
    private Role role;
    private boolean eliminated; //Variable to check if eliminated or not
    private String submittedWord; //Variable for word submitted
    
    //Setting up player
    public Player(String name) {
        this.name = name;
        this.role = Role.CREWMATE;
        this.eliminated = false;
        this.submittedWord = null;
    }
    
    
    //GETTERS
    public String getName() {
    	return name;
    }
    public Role getRole() {
    	return role;
    }
    public boolean isEliminated() {
    	return eliminated;
    }
    public String getSubmittedWord() {
    	return submittedWord;
    }
    
    //SETTERS
    public void setRole(Role role) {
    	this.role = role;
    }
    public void setEliminated(boolean eliminated) {
    	this.eliminated = eliminated;
    }
    public void setSubmittedWord(String word) {
    	this.submittedWord = word;
    }
}