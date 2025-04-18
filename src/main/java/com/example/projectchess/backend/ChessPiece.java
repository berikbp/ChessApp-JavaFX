package com.example.projectchess.backend;

public class ChessPiece {
    private PieceType type;
    private Color color;

    public ChessPiece(PieceType type, Color color) {
        this.type = type;
        this.color = color;
    }

    public PieceType getType() {
        return type;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        // Uppercase for White, lowercase for Black.
        return switch (type) {
            case KING -> color != Color.WHITE ? "♚" : "♔";
            case QUEEN -> color != Color.WHITE ? "♛" : "♕";
            case ROOK -> color != Color.WHITE ? "♜" : "♖";
            case BISHOP -> color != Color.WHITE ? "♝" : "♗";
            case KNIGHT -> color != Color.WHITE ? "♞" : "♘";
            case PAWN -> color != Color.WHITE ? "♟" : "♙";
            default -> "?";
        };
    }
}
