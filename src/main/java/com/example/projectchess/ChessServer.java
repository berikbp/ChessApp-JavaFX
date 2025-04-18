package com.example.projectchess;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChessServer {
    public static void main(String[] args) {
        int port = 5000;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ChessServer started on port " + port + ". Waiting for players...");
            Socket player1 = serverSocket.accept();
            System.out.println("Player 1 connected from " + player1.getInetAddress());
            Socket player2 = serverSocket.accept();
            System.out.println("Player 2 connected from " + player2.getInetAddress());

            ExecutorService executor = Executors.newFixedThreadPool(2);
            executor.execute(new ClientHandler(player1, player2));
            executor.execute(new ClientHandler(player2, player1));
            executor.shutdown();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private Socket opponent;

    public ClientHandler(Socket socket, Socket opponent) {
        this.socket = socket;
        this.opponent = opponent;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter outOpponent = new PrintWriter(opponent.getOutputStream(), true);
        ) {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received from " + socket.getInetAddress() + ": " + message);
                // Forward the message to the opponent.
                outOpponent.println(message);
            }
        } catch (IOException ex) {
            System.out.println("Connection closed for " + socket.getInetAddress());
        }
    }
}
