package main;

import java.util.Scanner;
import controller.GameController;

public class Main {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		
		System.out.print("Enter number of players: ");
		int n = Integer.parseInt(scanner.nextLine().trim());
		String[] names = new String[n];
				
		for (int i = 0; i < n; i++) {
			System.out.print("Enter name for player " + (i + 1) + ": ");
			names[i] = scanner.nextLine().trim();
		}
		
		GameController game = new GameController(names);
		game.start(scanner);
		
		scanner.close();
	}

}
