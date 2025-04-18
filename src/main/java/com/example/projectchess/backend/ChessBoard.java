package com.example.projectchess.backend;


import com.example.projectchess.backend.ChessPiece;
import com.example.projectchess.backend.Color;
import com.example.projectchess.backend.PieceType;

import java.util.ArrayList;
import java.util.List;

public class ChessBoard {
    // Board uses 0-indexed rows and columns.
    // row 0 is the top (Black's back rank) and row 7 is the bottom (White's back rank)
    private ChessPiece[][] board;

    // Castling rights flags
    private boolean whiteKingMoved, whiteKingRookMoved, whiteQueenRookMoved;
    private boolean blackKingMoved, blackKingRookMoved, blackQueenRookMoved;

    // En passant target square (if any); if none, set to -1.
    // When a pawn makes a two-square move, the square it “skipped” becomes available for en passant.
    private int enPassantTargetRow = -1, enPassantTargetCol = -1;

    public ChessBoard() {
        board = new ChessPiece[8][8];
        setupBoard();
    }

    public void setupBoard() {
        // Clear board
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = null;
            }
        }

        // Place pawns
        for (int col = 0; col < 8; col++) {
            board[1][col] = new ChessPiece(PieceType.PAWN, Color.BLACK);
            board[6][col] = new ChessPiece(PieceType.PAWN, Color.WHITE);
        }
        // Place Black pieces (row 0)
        board[0][0] = new ChessPiece(PieceType.ROOK, Color.BLACK);
        board[0][1] = new ChessPiece(PieceType.KNIGHT, Color.BLACK);
        board[0][2] = new ChessPiece(PieceType.BISHOP, Color.BLACK);
        board[0][3] = new ChessPiece(PieceType.QUEEN, Color.BLACK);
        board[0][4] = new ChessPiece(PieceType.KING, Color.BLACK);
        board[0][5] = new ChessPiece(PieceType.BISHOP, Color.BLACK);
        board[0][6] = new ChessPiece(PieceType.KNIGHT, Color.BLACK);
        board[0][7] = new ChessPiece(PieceType.ROOK, Color.BLACK);

        // Place White pieces (row 7)
        board[7][0] = new ChessPiece(PieceType.ROOK, Color.WHITE);
        board[7][1] = new ChessPiece(PieceType.KNIGHT, Color.WHITE);
        board[7][2] = new ChessPiece(PieceType.BISHOP, Color.WHITE);
        board[7][3] = new ChessPiece(PieceType.QUEEN, Color.WHITE);
        board[7][4] = new ChessPiece(PieceType.KING, Color.WHITE);
        board[7][5] = new ChessPiece(PieceType.BISHOP, Color.WHITE);
        board[7][6] = new ChessPiece(PieceType.KNIGHT, Color.WHITE);
        board[7][7] = new ChessPiece(PieceType.ROOK, Color.WHITE);

        // Reset castling rights and en passant
        whiteKingMoved = whiteKingRookMoved = whiteQueenRookMoved = false;
        blackKingMoved = blackKingRookMoved = blackQueenRookMoved = false;
        enPassantTargetRow = enPassantTargetCol = -1;
    }

    public ChessPiece getPiece(int row, int col) {
        if (isValidCoordinate(row, col)) {
            return board[row][col];
        }
        return null;
    }

    private boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    // Returns a list of legal moves (each as int[]{endRow, endCol}) for a piece at (row, col)
    public List<int[]> getLegalMoves(int row, int col) {
        List<int[]> moves = new ArrayList<>();
        ChessPiece piece = getPiece(row, col);
        if (piece == null) return moves;

        Color color = piece.getColor();
        PieceType type = piece.getType();

        // Direction for pawn moves: White moves "up" (decreasing row), Black moves "down" (increasing row)
        int pawnDir = (color == Color.WHITE) ? -1 : 1;

        switch(type) {
            case PAWN:
                // One square forward
                if (isValidCoordinate(row + pawnDir, col) && getPiece(row + pawnDir, col) == null) {
                    moves.add(new int[]{row + pawnDir, col});
                    // Two squares forward from starting position
                    int startRow = (color == Color.WHITE) ? 6 : 1;
                    if (row == startRow && getPiece(row + pawnDir, col) == null && getPiece(row + 2*pawnDir, col) == null) {
                        moves.add(new int[]{row + 2*pawnDir, col});
                    }
                }
                // Captures (diagonally)
                for (int dcol = -1; dcol <= 1; dcol += 2) {
                    int nrow = row + pawnDir;
                    int ncol = col + dcol;
                    if (isValidCoordinate(nrow, ncol)) {
                        ChessPiece target = getPiece(nrow, ncol);
                        if (target != null && target.getColor() != color) {
                            moves.add(new int[]{nrow, ncol});
                        }
                        // En passant capture: if target square is empty but matches en passant target.
                        if (target == null && nrow == enPassantTargetRow && ncol == enPassantTargetCol) {
                            moves.add(new int[]{nrow, ncol});
                        }
                    }
                }
                break;

            case KNIGHT:
                int[][] knightMoves = {
                        {row-2, col-1}, {row-2, col+1},
                        {row-1, col-2}, {row-1, col+2},
                        {row+1, col-2}, {row+1, col+2},
                        {row+2, col-1}, {row+2, col+1}
                };
                for (int[] move : knightMoves) {
                    int r = move[0], c = move[1];
                    if (isValidCoordinate(r, c) && (getPiece(r, c) == null || getPiece(r, c).getColor() != color))
                        moves.add(new int[]{r, c});
                }
                break;

            case BISHOP:
                moves.addAll(getSlidingMoves(row, col, color, new int[][]{{-1,-1}, {-1,1}, {1,-1}, {1,1}}));
                break;

            case ROOK:
                moves.addAll(getSlidingMoves(row, col, color, new int[][]{{-1,0}, {1,0}, {0,-1}, {0,1}}));
                break;

            case QUEEN:
                moves.addAll(getSlidingMoves(row, col, color, new int[][]{
                        {-1,-1}, {-1,1}, {1,-1}, {1,1},
                        {-1,0}, {1,0}, {0,-1}, {0,1}
                }));
                break;

            case KING:
                // Normal king moves (one square in any direction)
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int r = row + dr, c = col + dc;
                        if (isValidCoordinate(r, c) && (getPiece(r, c) == null || getPiece(r, c).getColor() != color)) {
                            // For king moves, also check that the destination is not attacked.
                            if (!isSquareAttacked(r, c, (color == Color.WHITE ? Color.BLACK : Color.WHITE))) {
                                moves.add(new int[]{r, c});
                            }
                        }
                    }
                }
                // Castling moves (if king is not in check and squares between are not attacked)
                if (!isKingInCheck(color)) {
                    // Kingside castling:
                    if (color == Color.WHITE && !whiteKingMoved && !whiteKingRookMoved) {
                        if (getPiece(7,5) == null && getPiece(7,6) == null &&
                                !isSquareAttacked(7,5, Color.BLACK) && !isSquareAttacked(7,6, Color.BLACK)) {
                            moves.add(new int[]{7,6}); // move king two squares right
                        }
                    } else if (color == Color.BLACK && !blackKingMoved && !blackKingRookMoved) {
                        if (getPiece(0,5) == null && getPiece(0,6) == null &&
                                !isSquareAttacked(0,5, Color.WHITE) && !isSquareAttacked(0,6, Color.WHITE)) {
                            moves.add(new int[]{0,6});
                        }
                    }
                    // Queenside castling:
                    if (color == Color.WHITE && !whiteKingMoved && !whiteQueenRookMoved) {
                        if (getPiece(7,1) == null && getPiece(7,2) == null && getPiece(7,3) == null &&
                                !isSquareAttacked(7,2, Color.BLACK) && !isSquareAttacked(7,3, Color.BLACK)) {
                            moves.add(new int[]{7,2}); // king moves two squares left
                        }
                    } else if (color == Color.BLACK && !blackKingMoved && !blackQueenRookMoved) {
                        if (getPiece(0,1) == null && getPiece(0,2) == null && getPiece(0,3) == null &&
                                !isSquareAttacked(0,2, Color.WHITE) && !isSquareAttacked(0,3, Color.WHITE)) {
                            moves.add(new int[]{0,2});
                        }
                    }
                }
                break;
        }
        // Now filter out moves that would leave the king in check.
        List<int[]> legalMoves = new ArrayList<>();
        for (int[] move : moves) {
            if (isMoveSafe(row, col, move[0], move[1], color)) {
                legalMoves.add(move);
            }
        }
        return legalMoves;
    }

    // Helper for sliding pieces (bishop, rook, queen)
    private List<int[]> getSlidingMoves(int row, int col, Color color, int[][] directions) {
        List<int[]> moves = new ArrayList<>();
        for (int[] d : directions) {
            int r = row + d[0], c = col + d[1];
            while (isValidCoordinate(r, c)) {
                ChessPiece target = getPiece(r, c);
                if (target == null) {
                    moves.add(new int[]{r, c});
                } else {
                    if (target.getColor() != color) {
                        moves.add(new int[]{r, c});
                    }
                    break;
                }
                r += d[0];
                c += d[1];
            }
        }
        return moves;
    }

    // Determines whether moving a piece from (sRow, sCol) to (eRow, eCol) leaves the king safe.
    private boolean isMoveSafe(int sRow, int sCol, int eRow, int eCol, Color turn) {
        // Save board state and flags.
        ChessPiece[][] backup = copyBoard();
        boolean bWhiteKingMoved = whiteKingMoved;
        boolean bWhiteKRookMoved = whiteKingRookMoved;
        boolean bWhiteQRookMoved = whiteQueenRookMoved;
        boolean bBlackKingMoved = blackKingMoved;
        boolean bBlackKRookMoved = blackKingRookMoved;
        boolean bBlackQRookMoved = blackQueenRookMoved;
        int backupEPTRow = enPassantTargetRow;
        int backupEPTCol = enPassantTargetCol;

        // Make the move
        makeMove(sRow, sCol, eRow, eCol, turn, true);
        boolean safe = !isKingInCheck(turn);

        // Restore state.
        board = backup;
        whiteKingMoved = bWhiteKingMoved;
        whiteKingRookMoved = bWhiteKRookMoved;
        whiteQueenRookMoved = bWhiteQRookMoved;
        blackKingMoved = bBlackKingMoved;
        blackKingRookMoved = bBlackKRookMoved;
        blackQueenRookMoved = bBlackQRookMoved;
        enPassantTargetRow = backupEPTRow;
        enPassantTargetCol = backupEPTCol;

        return safe;
    }

    // Copy board for simulation.
    private ChessPiece[][] copyBoard() {
        ChessPiece[][] newBoard = new ChessPiece[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                newBoard[i][j] = board[i][j];
            }
        }
        return newBoard;
    }

    // Checks whether the king of the given color is in check.
    public boolean isKingInCheck(Color color) {
        int kingRow = -1, kingCol = -1;
        // Find the king.
        for (int i = 0; i < 8 && kingRow == -1; i++) {
            for (int j = 0; j < 8 && kingCol == -1; j++) {
                ChessPiece p = board[i][j];
                if (p != null && p.getType() == PieceType.KING && p.getColor() == color) {
                    kingRow = i;
                    kingCol = j;
                }
            }
        }
        if (kingRow == -1) return true; // Should not happen.
        Color opponent = (color == Color.WHITE ? Color.BLACK : Color.WHITE);
        return isSquareAttacked(kingRow, kingCol, opponent);
    }

    // Checks if a square is attacked by any piece of the given attackerColor.
    public boolean isSquareAttacked(int row, int col, Color attackerColor) {
        // For every piece of attackerColor, if it can move (pseudo‑legal) to (row,col), then the square is attacked.
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                ChessPiece piece = board[i][j];
                if (piece != null && piece.getColor() == attackerColor) {
                    if (canAttackSquare(i, j, row, col)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Returns whether the piece at (sRow,sCol) can attack the target square (ignoring pins and king safety).
    private boolean canAttackSquare(int sRow, int sCol, int tRow, int tCol) {
        ChessPiece piece = getPiece(sRow, sCol);
        if (piece == null || (sRow == tRow && sCol == tCol) ) return false;
        Color color = piece.getColor();
        int pawnDir = (color == Color.WHITE) ? -1 : 1;
        switch(piece.getType()){
            case PAWN:
                // Pawns attack diagonally.
                if (tRow == sRow + pawnDir && (tCol == sCol - 1 || tCol == sCol + 1)) {
                    return true;
                }
                break;
            case KNIGHT:
                int[][] knightMoves = {
                        {sRow-2, sCol-1}, {sRow-2, sCol+1},
                        {sRow-1, sCol-2}, {sRow-1, sCol+2},
                        {sRow+1, sCol-2}, {sRow+1, sCol+2},
                        {sRow+2, sCol-1}, {sRow+2, sCol+1}
                };
                for (int[] move : knightMoves) {
                    if (move[0] == tRow && move[1] == tCol) return true;
                }
                break;
            case BISHOP:
                if (Math.abs(tRow - sRow) == Math.abs(tCol - sCol)) {
                    int dr = (tRow - sRow) > 0 ? 1 : -1;
                    int dc = (tCol - sCol) > 0 ? 1 : -1;
                    int r = sRow + dr, c = sCol + dc;
                    while (r != tRow && c != tCol) {
                        if (getPiece(r, c) != null) return false;
                        r += dr; c += dc;
                    }
                    return true;
                }
                break;
            case ROOK:
                if (sRow == tRow || sCol == tCol) {
                    int dr = (tRow - sRow) == 0 ? 0 : (tRow - sRow) / Math.abs(tRow - sRow);
                    int dc = (tCol - sCol) == 0 ? 0 : (tCol - sCol) / Math.abs(tCol - sCol);
                    int r = sRow + dr, c = sCol + dc;
                    while (r != tRow || c != tCol) {
                        if (getPiece(r, c) != null) return false;
                        r += dr; c += dc;
                    }
                    return true;
                }
                break;
            case QUEEN:
                // Combine rook and bishop logic.
                if (sRow == tRow || sCol == tCol) {
                    int dr = (tRow - sRow) == 0 ? 0 : (tRow - sRow) / Math.abs(tRow - sRow);
                    int dc = (tCol - sCol) == 0 ? 0 : (tCol - sCol) / Math.abs(tCol - sCol);
                    int r = sRow + dr, c = sCol + dc;
                    while (r != tRow || c != tCol) {
                        if (getPiece(r, c) != null) return false;
                        r += dr; c += dc;
                    }
                    return true;
                } else if (Math.abs(tRow - sRow) == Math.abs(tCol - sCol)) {
                    int dr = (tRow - sRow) > 0 ? 1 : -1;
                    int dc = (tCol - sCol) > 0 ? 1 : -1;
                    int r = sRow + dr, c = sCol + dc;
                    while (r != tRow && c != tCol) {
                        if (getPiece(r, c) != null) return false;
                        r += dr; c += dc;
                    }
                    return true;
                }
                break;
            case KING:
                if (Math.abs(sRow - tRow) <= 1 && Math.abs(sCol - tCol) <= 1) return true;
                break;
        }
        return false;
    }

    /**
     * Attempts to move a piece from (sRow, sCol) to (eRow, eCol) for the given turn.
     * Returns true if the move is legal and executed.
     */
    public boolean movePiece(int sRow, int sCol, int eRow, int eCol, Color turn) {
        ChessPiece piece = getPiece(sRow, sCol);
        if (piece == null || piece.getColor() != turn) {
            System.out.println("No valid piece at the starting square.");
            return false;
        }
        List<int[]> legalMoves = getLegalMoves(sRow, sCol);
        boolean found = false;
        for (int[] move : legalMoves) {
            if (move[0] == eRow && move[1] == eCol) {
                found = true;
                break;
            }
        }
        if (!found) {
            System.out.println("Illegal move!");
            return false;
        }
        // Execute the move.
        makeMove(sRow, sCol, eRow, eCol, turn, false);
        return true;
    }

    /**
     * Makes a move from (sRow,sCol) to (eRow,eCol) without validating king safety.
     * If simulate is true, the state will not be permanently updated.
     */
    private void makeMove(int sRow, int sCol, int eRow, int eCol, Color turn, boolean simulate) {
        ChessPiece piece = getPiece(sRow, sCol);
        // Clear any previous en passant target.
        enPassantTargetRow = enPassantTargetCol = -1;

        // Handle castling if the king moves two squares.
        if (piece.getType() == PieceType.KING && Math.abs(eCol - sCol) == 2) {
            // Kingside castling
            if (eCol > sCol) {
                // Move the rook.
                board[eRow][eCol - 1] = board[eRow][7];
                board[eRow][7] = null;
                if (turn == Color.WHITE) whiteKingRookMoved = true;
                else blackKingRookMoved = true;
            } else { // Queenside castling
                board[eRow][eCol + 1] = board[eRow][0];
                board[eRow][0] = null;
                if (turn == Color.WHITE) whiteQueenRookMoved = true;
                else blackQueenRookMoved = true;
            }
        }

        // Handle en passant capture.
        if (piece.getType() == PieceType.PAWN && sCol != eCol && getPiece(eRow, eCol) == null) {
            // Capturing en passant: remove the pawn behind the target square.
            board[sRow][eCol] = null;
        }

        // Move the piece.
        board[eRow][eCol] = piece;
        board[sRow][sCol] = null;

        // Pawn promotion (auto promote to queen)
        if (piece.getType() == PieceType.PAWN) {
            if ((piece.getColor() == Color.WHITE && eRow == 0) ||
                    (piece.getColor() == Color.BLACK && eRow == 7)) {
                board[eRow][eCol] = new ChessPiece(PieceType.QUEEN, piece.getColor());
            }
            // Set en passant target if pawn moved two squares.
            if (Math.abs(eRow - sRow) == 2) {
                // The square the pawn passed over.
                enPassantTargetRow = (sRow + eRow) / 2;
                enPassantTargetCol = sCol;
            }
        }

        // Update castling rights if king or rook moved.
        if (piece.getType() == PieceType.KING) {
            if (piece.getColor() == Color.WHITE) whiteKingMoved = true;
            else blackKingMoved = true;
        }
        if (piece.getType() == PieceType.ROOK) {
            if (sRow == 7 && sCol == 0) whiteQueenRookMoved = true;
            if (sRow == 7 && sCol == 7) whiteKingRookMoved = true;
            if (sRow == 0 && sCol == 0) blackQueenRookMoved = true;
            if (sRow == 0 && sCol == 7) blackKingRookMoved = true;
        }

        // (If not simulating, the new state persists.)
    }
}






