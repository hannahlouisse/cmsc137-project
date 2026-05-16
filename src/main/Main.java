package main;

import java.util.Scanner;
import server.GameServer;
import client.GameClient;

public class Main {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);

		System.out.println("=========================================");
		System.out.println("     GUESS THE IMPOSTOR");
		System.out.println("=========================================");
		System.out.println("1. Start Server");
		System.out.println("2. Start Client");
		System.out.print("Choose (1/2): ");

		String choice = scanner.nextLine().trim();

		if (choice.equals("1")) {
			// Start as server
			System.out.print("Enter port (12345): ");
			String portInput = scanner.nextLine();
			int port = portInput.isEmpty() ? 12345 : Integer.parseInt(portInput);

			GameServer server = new GameServer(port);
			server.start();

			System.out.println("Server running. Press Enter to stop...");
			scanner.nextLine();
			server.stop();

		} else if (choice.equals("2")) {
			// Start as client
			System.out.print("Enter server address (localhost): ");
			String address = scanner.nextLine();
			if (address.isEmpty()) address = "localhost";

			System.out.print("Enter port (12345): ");
			String portInput = scanner.nextLine();
			int port = portInput.isEmpty() ? 12345 : Integer.parseInt(portInput);

			GameClient client = new GameClient(address, port);

			// Wait for client to finish
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		scanner.close();
	}
}