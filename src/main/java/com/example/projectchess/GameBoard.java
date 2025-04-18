package com.example.projectchess;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;
import com.example.projectchess.backend.ChessBoard;
import com.example.projectchess.backend.ChessPiece;
import com.example.projectchess.backend.Color;

public class GameBoard extends Application {

    private ChessBoard board;
    private Color currentTurn = Color.WHITE;
    private Color myColor;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private Button[][] squares = new Button[8][8];
    private Label statusLabel;
    private Button localGameButton;
    private boolean localGame;

    // Networking client.
    private ChessClient chessClient;
    // Controls for connection.
    private TextField serverAddressField;
    private TextField colorField;

    @Override
    public void start(Stage primaryStage) {
        board = new ChessBoard(); // Initialize board and pieces.

        BorderPane root = new BorderPane();

        // Status label displays the current turn.
        statusLabel = new Label("Current turn: " + currentTurn);
        statusLabel.setFont(new Font("SansSerif", 24));
        root.setTop(statusLabel);
        BorderPane.setMargin(statusLabel, new Insets(10));

        // Build the chess board grid.
        GridPane grid = new GridPane();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Button square = new Button();
                square.setPrefSize(80, 80);
                updateSquare(square, row, col);
                final int r = row, c = col;
                square.setOnAction(e -> handleSquareClick(r, c));
                squares[row][col] = square;
                grid.add(square, col, row);
            }
        }
        root.setCenter(grid);

        // Controls for networking: connect, server address, local color, and restart.
        Button connectButton = new Button("Connect");
        connectButton.setFont(new Font("SansSerif", 18));
        connectButton.setOnAction(e -> connectToServer());

        serverAddressField = new TextField("localhost");
        serverAddressField.setPrefWidth(120);

        colorField = new TextField("WHITE"); // Enter "WHITE" or "BLACK".
        colorField.setPrefWidth(80);

        Button restartButton = new Button("Restart Game");
        restartButton.setFont(new Font("SansSerif", 18));
        restartButton.setOnAction(e -> restartGame());

        localGameButton = new Button("Local Game");
        localGameButton.setFont(new Font("SansSerif", 18));
        localGameButton.setOnAction(e -> {
            localGame = true;
            statusLabel.setText("Local 2-Player mode. Current turn: " + currentTurn);
        });

        HBox controls = new HBox(10, connectButton, localGameButton, serverAddressField,
                new Label("My Color:"), colorField, restartButton);
        controls.setPadding(new Insets(10));
        root.setBottom(controls);

        Scene scene = new Scene(root, 600, 650);
        primaryStage.setTitle("CHESS");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // ------------------- Connection Methods -------------------

    /**
     * Attempts to connect to the ChessServer
     */
    private void connectToServer() {
        String serverAddress = serverAddressField.getText().trim();
        try {
            try {
                myColor = Color.valueOf(colorField.getText().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                myColor = Color.WHITE;
                colorField.setText("WHITE");
            }
            chessClient = new ChessClient(serverAddress, 5000, this);
            statusLabel.setText("Connected! Current turn: " + currentTurn + " | My Color: " + myColor);
        } catch (IOException ex) {
            statusLabel.setText("Connection failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Board Update
    /**
     * Updates the given square with the piece image
     */
    private void updateSquare(Button square, int row, int col) {
        ChessPiece piece = board.getPiece(row, col);
        if (piece != null) {
            square.setGraphic(getPieceImage(piece));
        } else {
            square.setGraphic(null);
        }
        if ((row + col) % 2 == 0) {
            square.setStyle("-fx-background-color: #f0d9b5;");
        } else {
            square.setStyle("-fx-background-color: #b58863;");
        }
    }

    //returns ImageView
    private ImageView getPieceImage(ChessPiece piece) {
        String filename;
        if (piece.getColor() == Color.WHITE) {
            switch (piece.getType()) {
                case ROOK   -> filename = "whiteRook.png";
                case QUEEN  -> filename = "whiteQueen.png";
                case PAWN   -> filename = "whitePawn.png";
                case KNIGHT -> filename = "whiteKnight.png";
                case BISHOP -> filename = "whiteBishop.png";
                case KING   -> filename = "whiteKing.png";
                default     -> filename = "";
            }
        } else {
            switch (piece.getType()) {
                case ROOK   -> filename = "blackRook.png";
                case QUEEN  -> filename = "blackQueen.png";
                case PAWN   -> filename = "blackPawn.png";
                case KNIGHT -> filename = "blackKnight.png";
                case BISHOP -> filename = "blackBishop.png";
                case KING   -> filename = "blackKing.png";
                default     -> filename = "";
            }
        }

        // Загружаем изображение из папки resources/images/
        Image image = new Image(getClass().getResource("/images/" + filename).toExternalForm());
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(55);
        imageView.setFitHeight(55);
        imageView.setPreserveRatio(true);
        return imageView;
    }


    // Moves
    // Handles a click on a board square.
    private void handleSquareClick(int row, int col) {
        // In network mode, only allow move if it's our turn.
        if (!localGame && currentTurn != myColor) {
            return;
        }
        // If no piece is selected, try to select one.
        if (selectedRow == -1 && selectedCol == -1) {
            ChessPiece piece = board.getPiece(row, col);
            if (piece != null && piece.getColor() == currentTurn) {
                selectedRow = row;
                selectedCol = col;
                highlightSquare(row, col);
                highlightLegalMoves(row, col);
            }
        } else {
            // Attempt to move the selected piece.
            List<int[]> legalMoves = board.getLegalMoves(selectedRow, selectedCol);
            boolean validMove = legalMoves.stream().anyMatch(move -> move[0] == row && move[1] == col);
            if (validMove) {
                boolean moved = board.movePiece(selectedRow, selectedCol, row, col, currentTurn);
                if (moved) {
                    // In network mode, send the move.
                    if (!localGame && chessClient != null) {
                        String moveMessage = "MOVE " + selectedRow + "," + selectedCol + " " + row + "," + col;
                        chessClient.sendMove(moveMessage);
                    }
                    currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
                    checkGameState();
                }
            }
            // Clear selection and refresh board.
            selectedRow = -1;
            selectedCol = -1;
            clearHighlights();
            updateBoard();
        }
    }

    /**
     * Checks if the specified side has any legal moves.
     */
    private boolean hasLegalMoves(Color turn) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                ChessPiece piece = board.getPiece(i, j);
                if (piece != null && piece.getColor() == turn && !board.getLegalMoves(i, j).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkGameState() {
        if (!hasLegalMoves(currentTurn)) {
            if (board.isKingInCheck(currentTurn)) {
                statusLabel.setText("Checkmate! " + (currentTurn == Color.WHITE ? "Black wins!" : "White wins!"));
                if (chessClient != null) {
                    // Send game over message to opponent.
                    chessClient.sendMove((currentTurn == Color.WHITE) ? "GAMEOVER BLACK" : "GAMEOVER WHITE");
                }
            } else {
                statusLabel.setText("Stalemate! It's a draw!");
                if (chessClient != null) {
                    chessClient.sendMove("GAMEOVER DRAW");
                }
            }
            updateBoard();
            disableBoard();
        } else if (board.isKingInCheck(currentTurn)) {
            statusLabel.setText("Check! Current turn: " + currentTurn);
        } else {
            statusLabel.setText("Current turn: " + currentTurn);
        }
    }


    private void highlightSquare(int row, int col) {
        Button square = squares[row][col];
        square.setStyle(square.getStyle() + " -fx-border-color: #2f2ffb; -fx-border-width: 3px;");

    }

    private void highlightLegalMoves(int row, int col) {
        List<int[]> moves = board.getLegalMoves(row, col);
        moves.forEach(move -> {
            int r = move[0], c = move[1];
            Button square = squares[r][c];
            square.setStyle(square.getStyle() + " -fx-border-color: #68e368; -fx-border-width: 3px;");
        });
    }

    private void clearHighlights() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                updateSquare(squares[row][col], row, col);
            }
        }
    }

    private void updateBoard() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                updateSquare(squares[row][col], row, col);
            }
        }
    }

    private void disableBoard() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[row][col].setDisable(true);
            }
        }
    }

    private void restartGame() {
        board = new ChessBoard();
        currentTurn = Color.WHITE;
        selectedRow = -1;
        selectedCol = -1;
        localGame = false;
        statusLabel.setText("Current turn: " + currentTurn);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[row][col].setDisable(false);
            }
        }
        updateBoard();
    }

    public void processIncomingMove(String message) {
        if (message.startsWith("MOVE")) {
            // Parse and apply the move.
            String[] parts = message.split(" ");
            if (parts.length == 3) {
                try {
                    String[] src = parts[1].split(",");
                    String[] dst = parts[2].split(",");
                    int sRow = Integer.parseInt(src[0]);
                    int sCol = Integer.parseInt(src[1]);
                    int eRow = Integer.parseInt(dst[0]);
                    int eCol = Integer.parseInt(dst[1]);
                    // Opponent's move: apply with the opposite color of myColor.
                    Color opponentColor = (myColor == Color.WHITE) ? Color.BLACK : Color.WHITE;
                    boolean moved = board.movePiece(sRow, sCol, eRow, eCol, opponentColor);
                    if (moved) {
                        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
                        updateBoard();
                        checkGameState();
                    }
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid MOVE format: " + message);
                }
            }
        } else if (message.startsWith("GAMEOVER")) {
            String[] parts = message.split(" ");
            if (parts.length == 2) {
                String result = parts[1]; // "WHITE", "BLACK", or "DRAW"
                switch (result) {
                    case "WHITE":
                        statusLabel.setText("Checkmate! White wins!");
                        break;
                    case "BLACK":
                        statusLabel.setText("Checkmate! Black wins!");
                        break;
                    case "DRAW":
                        statusLabel.setText("Stalemate! It's a draw!");
                        break;
                }
                updateBoard();
                disableBoard();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
