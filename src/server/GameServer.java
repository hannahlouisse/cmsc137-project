package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import controller.GameController;
import utils.Message;

public class GameServer {
	//Accepts all socket connections and hosts the game by creaeting the ClientHandler instance
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private ExecutorService threadPool;
    private GameController gameController;
    private boolean isRunning;
    private int port;

    public GameServer(int port){
        this.port = port;
        this.clients = new ArrayList<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.isRunning = false;
    }

    public void start(){
        try{
            serverSocket = new ServerSocket(port);
            isRunning = true;
            System.out.println("Game Server started on port " + port);

            // Accept client connections in a separate thread
            new Thread(() ->{
                while (isRunning){
                    try{
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("New client connected " + clientSocket.getInetAddress());
                        ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                        clients.add(clientHandler);
                        threadPool.execute(clientHandler);
                    } catch (IOException e){
                        if (isRunning){
                            System.err.println("Error accepting client connection: "+e.getMessage());
                        }

                    }
                }
            }).start();
        } catch (IOException e){
            System.err.println("Failed to start server: "+ e.getMessage());
        }
    }

    public void stop(){
        isRunning = false;
        try {
            // Disconnect all clients
            for (ClientHandler client : clients) {
                client.disconnect();
            }

            if (serverSocket != null && !serverSocket.isClosed()){
                serverSocket.close();
            }

            threadPool.shutdown();
            System.out.println("Game Server stopped");
        } catch(IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }
    public void broadcast(Message message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void broadcastExcept(Message message, ClientHandler exclude) {
        for (ClientHandler client : clients) {
            if (client != exclude) {
                client.sendMessage(message);
            }
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client disconnected. Total clients: " + clients.size());
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }

    public GameController getGameController() {
        return gameController;
    }

    public boolean isGameStarted() {
        return gameController != null;
    }

    public static void main(String[] args) {
        GameServer server = new GameServer(12345);
        server.start();
    }
}
