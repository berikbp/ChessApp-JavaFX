package com.example.projectchess;

import java.io.*;
import java.net.*;
import javafx.application.Platform;

public class ChessClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private GameBoard gameBoard; // Reference to UI for updating moves

    public ChessClient(String serverAddress, int port, GameBoard gameBoard) throws IOException {
        this.socket = new Socket(serverAddress, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.gameBoard = gameBoard;
        startListening();
    }

    // Sends a move message in the format: "MOVE sRow,sCol eRow,eCol"
    public void sendMove(String move) {
        out.println(move);
    }

    // Listens for incoming messages and updates the UI accordingly.
    private void startListening() {
        Thread listener = new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received move: " + message);
                    String finalMessage = message;
                    Platform.runLater(() -> gameBoard.processIncomingMove(finalMessage));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listener.setDaemon(true);
        listener.start();
    }
}
