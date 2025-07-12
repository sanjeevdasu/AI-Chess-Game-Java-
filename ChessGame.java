import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.*;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ChessGame {
    private static final int BOARD_SIZE = 8;
    private Piece[][] board;
    private boolean whiteTurn;
    private JFrame frame;
    private JButton[][] squares;
    private Position selectedPiece;
    private List<Position> possibleMoves;
    private boolean gameOver;
    private Position whiteKingPosition;
    private Position blackKingPosition;
    private volatile boolean aiThinking;
    private Position enPassantTarget;
    private boolean[] castlingRights = {true, true, true, true};



    
    
    private static final int AI_DEPTH = 3;

    public ChessGame() {
        initializeBoard();
        whiteTurn = true;
        gameOver = false;
        createGUI();
        if (whiteTurn) aiMove();
    }


     private void initializeBoard() {
        board = new Piece[BOARD_SIZE][BOARD_SIZE];
        enPassantTarget = null;
        
        //  pawns
        for (int col = 0; col < BOARD_SIZE; col++) {
            board[1][col] = new Piece(PieceType.PAWN, false);
            board[6][col] = new Piece(PieceType.PAWN, true);
        }
        
        // rooks
        board[0][0] = new Piece(PieceType.ROOK, false);
        board[0][7] = new Piece(PieceType.ROOK, false);
        board[7][0] = new Piece(PieceType.ROOK, true);
        board[7][7] = new Piece(PieceType.ROOK, true);
        
        // knights
        board[0][1] = new Piece(PieceType.KNIGHT, false);
        board[0][6] = new Piece(PieceType.KNIGHT, false);
        board[7][1] = new Piece(PieceType.KNIGHT, true);
        board[7][6] = new Piece(PieceType.KNIGHT, true);
        
        // bishops
        board[0][2] = new Piece(PieceType.BISHOP, false);
        board[0][5] = new Piece(PieceType.BISHOP, false);
        board[7][2] = new Piece(PieceType.BISHOP, true);
        board[7][5] = new Piece(PieceType.BISHOP, true);
        
        // queens
        board[0][3] = new Piece(PieceType.QUEEN, false);
        board[7][3] = new Piece(PieceType.QUEEN, true);
        
        // kings
        board[0][4] = new Piece(PieceType.KING, false);
        blackKingPosition = new Position(0, 4);
        board[7][4] = new Piece(PieceType.KING, true);
        whiteKingPosition = new Position(7, 4);
    }

      private void createGUI() {
        frame = new JFrame("Chess Game - AI (White) vs Human (Black)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(BOARD_SIZE, BOARD_SIZE));
        
        squares = new JButton[BOARD_SIZE][BOARD_SIZE];
        possibleMoves = new ArrayList<>();
        
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                final int r = row;
                final int c = col;
                
                squares[row][col] = new JButton();
                squares[row][col].setPreferredSize(new Dimension(80, 80));
                squares[row][col].setOpaque(true);
                squares[row][col].setBorderPainted(false);
                
                if ((row + col) % 2 == 0) {
                    squares[row][col].setBackground(new Color(240, 217, 181));
                } else {
                    squares[row][col].setBackground(new Color(181, 136, 99));
                }
                
                squares[row][col].addActionListener(e -> {
                    if (!aiThinking && !gameOver) {
                        handleSquareClick(r, c);
                    }
                });
                
                frame.add(squares[row][col]);
            }
        }
        
        updateBoard();
        frame.pack();
        frame.setVisible(true);
    }

    private void aiMove() {
        aiThinking = true;
        frame.setTitle("Chess Game - AI thinking...");
        
        new Thread(() -> {
            try {
                Move bestMove = findBestMove(AI_DEPTH);
                
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (!gameOver) {
                            Piece capturedPiece = makeMove(bestMove.from, bestMove.to);
                            
                            
                            if (capturedPiece != null && capturedPiece.getType() == PieceType.KING) {
                                gameOver = true;
                                JOptionPane.showMessageDialog(frame, "King captured! AI (White) wins!");
                                frame.setTitle("Chess Game - AI (White) wins!");
                                return;
                            }
                            
                            boolean isCheck = isInCheck(false);
                            boolean isCheckmate = isCheck && isCheckmate(false);
                            
                            if (isCheckmate) {
                                gameOver = true;
                                JOptionPane.showMessageDialog(frame, "Checkmate! AI (White) wins!");
                                frame.setTitle("Chess Game - AI (White) wins!");
                            } else if (isCheck) {
                                JOptionPane.showMessageDialog(frame, "Human (Black) is in check!");
                            }
                            
                            whiteTurn = !whiteTurn;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(frame, "Error during AI move: " + e.getMessage());
                    } finally {
                        aiThinking = false;
                        if (!gameOver) {
                            frame.setTitle("Chess Game - Human (Black)'s turn");
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame, "AI encountered an error: " + e.getMessage());
                    aiThinking = false;
                    if (!gameOver) {
                        frame.setTitle("Chess Game - Human (Black)'s turn");
                    }
                });
            }
        }).start();
    }

      private void handleSquareClick(int row, int col) {
        if (aiThinking || gameOver) return;
        
        try {
            Position clickedPos = new Position(row, col);
            
            if (selectedPiece == null) {
                Piece piece = board[row][col];
                if (piece != null && piece.isWhite() == whiteTurn) {
                    selectedPiece = clickedPos;
                    possibleMoves = getValidMoves(selectedPiece);
                    
                    
                    if (isInCheck(whiteTurn)) {
                        List<Position> legalMoves = new ArrayList<>();
                        for (Position move : possibleMoves) {
                            Piece[][] tempBoard = copyBoard();
                            makeMoveOnBoard(selectedPiece, move, tempBoard);
                            Position kingPos = whiteTurn ? whiteKingPosition : blackKingPosition;
                            if (!isSquareUnderAttack(kingPos, !whiteTurn, tempBoard)) {
                                legalMoves.add(move);
                            }
                        }
                        possibleMoves = legalMoves;
                    }
                    
                    highlightPossibleMoves();
                }
            } else {
                if (clickedPos.equals(selectedPiece)) {
                    clearSelection();
                } else if (isPossibleMove(clickedPos)) {
                    Piece capturedPiece = makeMove(selectedPiece, clickedPos);
                    clearSelection();
                    
                    
                    if (capturedPiece != null && capturedPiece.getType() == PieceType.KING) {
                        gameOver = true;
                        String winner = whiteTurn ? "Human (Black)" : "AI (White)";
                        JOptionPane.showMessageDialog(frame, "King captured! " + winner + " wins!");
                        frame.setTitle("Chess Game - " + winner + " wins!");
                        return;
                    }
                    
                  
                    boolean isCheck = isInCheck(!whiteTurn);
                    boolean isCheckmate = isCheck && isCheckmate(!whiteTurn);
                    
                    if (isCheckmate) {
                        gameOver = true;
                        String winner = whiteTurn ? "AI (White)" : "Human (Black)";
                        JOptionPane.showMessageDialog(frame, "Checkmate! " + winner + " wins!");
                        frame.setTitle("Chess Game - " + winner + " wins!");
                    } else if (isCheck) {
                        JOptionPane.showMessageDialog(frame, 
                            (whiteTurn ? "Human (Black)" : "AI (White)") + " is in check!");
                    }
                    
                    whiteTurn = !whiteTurn;
                    
                    if (whiteTurn && !gameOver) {
                        aiMove();
                    }
                } else {
                    Piece piece = board[row][col];
                    if (piece != null && piece.isWhite() == whiteTurn) {
                        selectedPiece = clickedPos;
                        possibleMoves = getValidMoves(selectedPiece);
                        highlightPossibleMoves();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error during move: " + e.getMessage());
            clearSelection();
        }
    }

    private Move findBestMove(int depth) {
        List<Move> allMoves = generateAllMoves(true); 
        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;
        
        for (Move move : allMoves) {
            
            Piece captured = makeMoveOnBoard(move.from, move.to, board);
            
            
            int moveValue = minimax(depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            
           
            undoMoveOnBoard(move.from, move.to, captured, board);
            
            if (moveValue > bestValue) {
                bestValue = moveValue;
                bestMove = move;
            }
        }
        
        return bestMove != null ? bestMove : allMoves.get(0); 
    }

    private int minimax(int depth, int alpha, int beta, boolean maximizingPlayer) {
        if (depth == 0) {
            return evaluateBoard();
        }
        
        List<Move> moves = generateAllMoves(maximizingPlayer);
        
        if (maximizingPlayer) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : moves) {
                Piece captured = makeMoveOnBoard(move.from, move.to, board);
                int eval = minimax(depth - 1, alpha, beta, false);
                undoMoveOnBoard(move.from, move.to, captured, board);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : moves) {
                Piece captured = makeMoveOnBoard(move.from, move.to, board);
                int eval = minimax(depth - 1, alpha, beta, true);
                undoMoveOnBoard(move.from, move.to, captured, board);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

   private List<Position> getValidMoves(Position position) {
        List<Position> possibleMoves = getPossibleMoves(position);
        List<Position> validMoves = new ArrayList<>();
        
        Piece piece = board[position.row][position.col];
        if (piece == null) return validMoves;
        
        boolean isKingInCheck = isInCheck(piece.isWhite());
        
        for (Position move : possibleMoves) {
            Piece[][] tempBoard = copyBoard();
            makeMoveOnBoard(position, move, tempBoard);
            
            
            Position kingPos;
            if (piece.getType() == PieceType.KING) {
                kingPos = move;
            } else {
                kingPos = piece.isWhite() ? whiteKingPosition : blackKingPosition;
            }
            
            if (!isSquareUnderAttack(kingPos, !piece.isWhite(), tempBoard)) {
           
                if (piece.getType() == PieceType.KING && Math.abs(position.col - move.col) == 2) {
                    if (isValidCastle(piece.isWhite(), move.col == 6)) {
                        validMoves.add(move);
                    }
                } else {
                    validMoves.add(move);
                }
            }
        }
        
        return validMoves;
    }

    private boolean isValidCastle(boolean isWhite, boolean kingside) {
        if (!canCastle(isWhite, kingside)) return false;
        
        int row = isWhite ? 7 : 0;
        int kingCol = 4;
        int rookCol = kingside ? 7 : 0;
        int step = kingside ? 1 : -1;
        
       
        for (int col = kingCol + step; col != rookCol; col += step) {
            if (board[row][col] != null) return false;
        }
        
      
        for (int col = kingCol; col != (kingside ? 6 : 2); col += step) {
            if (isSquareUnderAttack(new Position(row, col), !isWhite, board)) {
                return false;
            }
        }
        
        return true;
    }


    private boolean isInCheck(boolean forWhite) {
        Position kingPos = forWhite ? whiteKingPosition : blackKingPosition;
        return isSquareUnderAttack(kingPos, !forWhite, board);
    }

   private boolean isCheckmate(boolean forWhite) {
        if (!isInCheck(forWhite)) return false;
        
        
        for (int fromRow = 0; fromRow < BOARD_SIZE; fromRow++) {
            for (int fromCol = 0; fromCol < BOARD_SIZE; fromCol++) {
                Position from = new Position(fromRow, fromCol);
                Piece piece = board[fromRow][fromCol];
                
                if (piece != null && piece.isWhite() == forWhite) {
                    List<Position> validMoves = getValidMoves(from);
                    if (!validMoves.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }

    private boolean isSquareUnderAttack(Position square, boolean byWhite, Piece[][] board) {
      
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.isWhite() == byWhite) {
                    List<Position> moves = getPossibleMoves(new Position(row, col), board);
                    for (Position move : moves) {
                        if (move.equals(square)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Piece[][] copyBoard() {
        Piece[][] copy = new Piece[BOARD_SIZE][BOARD_SIZE];
        for (int row = 0; row < BOARD_SIZE; row++) {
            System.arraycopy(board[row], 0, copy[row], 0, BOARD_SIZE);
        }
        return copy;
    }

    private List<Move> generateAllMoves(boolean forWhite) {
        List<Move> moves = new ArrayList<>();
        
        for (int fromRow = 0; fromRow < BOARD_SIZE; fromRow++) {
            for (int fromCol = 0; fromCol < BOARD_SIZE; fromCol++) {
                Position from = new Position(fromRow, fromCol);
                Piece piece = board[fromRow][fromCol];
                
                if (piece != null && piece.isWhite() == forWhite) {
                    List<Position> destinations = getValidMoves(from);
                    for (Position to : destinations) {
                        moves.add(new Move(from, to));
                    }
                }
            }
        }
        
        return moves;
    }

    private List<Position> getPossibleMoves(Position position) {
        return getPossibleMoves(position, board);
    }

    private List<Position> getPossibleMoves(Position position, Piece[][] board) {
        Piece piece = board[position.row][position.col];
        if (piece == null) return new ArrayList<>();
        
        List<Position> moves = new ArrayList<>();
        
        switch (piece.getType()) {
            case PAWN:
                getPawnMoves(position, piece.isWhite(), moves, board);
                break;
            case ROOK:
                getRookMoves(position, piece.isWhite(), moves, board);
                break;
            case KNIGHT:
                getKnightMoves(position, piece.isWhite(), moves, board);
                break;
            case BISHOP:
                getBishopMoves(position, piece.isWhite(), moves, board);
                break;
            case QUEEN:
                getRookMoves(position, piece.isWhite(), moves, board);
                getBishopMoves(position, piece.isWhite(), moves, board);
                break;
            case KING:
                getKingMoves(position, piece.isWhite(), moves, board);
                break;
        }
        
        return moves;
    }

    private void getPawnMoves(Position position, boolean isWhite, List<Position> moves, Piece[][] board) {
        int direction = isWhite ? -1 : 1;
        int startRow = isWhite ? 6 : 1;
        
       
        int newRow = position.row + direction;
        if (isValidPosition(newRow, position.col) && board[newRow][position.col] == null) {
            moves.add(new Position(newRow, position.col));
            
            
            if (position.row == startRow) {
                newRow = position.row + 2 * direction;
                if (isValidPosition(newRow, position.col) && board[newRow][position.col] == null && 
                    board[position.row + direction][position.col] == null) {
                    moves.add(new Position(newRow, position.col));
                }
            }
        }
        
       
        int[] captureCols = {position.col - 1, position.col + 1};
        for (int col : captureCols) {
            newRow = position.row + direction;
            if (isValidPosition(newRow, col)) {
                Piece target = board[newRow][col];
                if (target != null && target.isWhite() != isWhite) {
                    moves.add(new Position(newRow, col));
                }
               
                else if (enPassantTarget != null && enPassantTarget.row == newRow && enPassantTarget.col == col) {
                    moves.add(new Position(newRow, col));
                }
            }
        }
    }

    private void getRookMoves(Position position, boolean isWhite, List<Position> moves, Piece[][] board) {
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        getSlidingMoves(position, isWhite, moves, directions, board);
    }

    private void getBishopMoves(Position position, boolean isWhite, List<Position> moves, Piece[][] board) {
        int[][] directions = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        getSlidingMoves(position, isWhite, moves, directions, board);
    }

    private void getSlidingMoves(Position position, boolean isWhite, List<Position> moves, int[][] directions, Piece[][] board) {
        for (int[] dir : directions) {
            int newRow = position.row + dir[0];
            int newCol = position.col + dir[1];
            
            while (isValidPosition(newRow, newCol)) {
                Piece target = board[newRow][newCol];
                
                if (target == null) {
                    moves.add(new Position(newRow, newCol));
                } else {
                    if (target.isWhite() != isWhite) {
                        moves.add(new Position(newRow, newCol));
                    }
                    break;
                }
                
                newRow += dir[0];
                newCol += dir[1];
            }
        }
    }

    private void getKnightMoves(Position position, boolean isWhite, List<Position> moves, Piece[][] board) {
        int[][] knightMoves = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };
        
        for (int[] move : knightMoves) {
            int newRow = position.row + move[0];
            int newCol = position.col + move[1];
            
            if (isValidPosition(newRow, newCol)) {
                Piece target = board[newRow][newCol];
                if (target == null || target.isWhite() != isWhite) {
                    moves.add(new Position(newRow, newCol));
                }
            }
        }
    }

    private void getKingMoves(Position position, boolean isWhite, List<Position> moves, Piece[][] board) {
       
        for (int row = -1; row <= 1; row++) {
            for (int col = -1; col <= 1; col++) {
                if (row == 0 && col == 0) continue;
                
                int newRow = position.row + row;
                int newCol = position.col + col;
                
                if (isValidPosition(newRow, newCol)) {
                    Piece target = board[newRow][newCol];
                    if (target == null || target.isWhite() != isWhite) {
                        moves.add(new Position(newRow, newCol));
                    }
                }
            }
        }
        
        
        if ((isWhite && position.equals(whiteKingPosition)) || (!isWhite && position.equals(blackKingPosition))) {
            
            if (canCastle(isWhite, true)) {
                int row = isWhite ? 7 : 0;
                if (board[row][5] == null && board[row][6] == null && 
                    !isSquareUnderAttack(new Position(row, 4), !isWhite, board) &&
                    !isSquareUnderAttack(new Position(row, 5), !isWhite, board) &&
                    !isSquareUnderAttack(new Position(row, 6), !isWhite, board)) {
                    moves.add(new Position(row, 6));
                }
            }
            
            
            if (canCastle(isWhite, false)) {
                int row = isWhite ? 7 : 0;
                if (board[row][3] == null && board[row][2] == null && board[row][1] == null &&
                    !isSquareUnderAttack(new Position(row, 4), !isWhite, board) &&
                    !isSquareUnderAttack(new Position(row, 3), !isWhite, board) &&
                    !isSquareUnderAttack(new Position(row, 2), !isWhite, board)) {
                    moves.add(new Position(row, 2));
                }
            }
        }
    }

    private boolean canCastle(boolean isWhite, boolean kingside) {
        if (isWhite) {
            return kingside ? castlingRights[0] : castlingRights[1];
        } else {
            return kingside ? castlingRights[2] : castlingRights[3];
        }
    }

     private Piece makeMove(Position from, Position to) {
        Piece movingPiece = board[from.row][from.col];
        Piece capturedPiece = board[to.row][to.col];
        
        
        if (movingPiece.getType() == PieceType.KING && Math.abs(from.col - to.col) == 2) {
            performCastle(from, to);
            return null;
        }
       
        else if (movingPiece.getType() == PieceType.PAWN && to.equals(enPassantTarget)) {
            return performEnPassant(from, to);
        }
        
        
        updateCastlingRights(from, movingPiece);
        
       
        if (movingPiece.getType() == PieceType.PAWN && Math.abs(from.row - to.row) == 2) {
            enPassantTarget = new Position((from.row + to.row) / 2, from.col);
        } else {
            enPassantTarget = null;
        }
        
       
        if (movingPiece.getType() == PieceType.KING) {
            if (movingPiece.isWhite()) {
                whiteKingPosition = to;
            } else {
                blackKingPosition = to;
            }
        }
        
       
        board[to.row][to.col] = movingPiece;
        board[from.row][from.col] = null;
        
       
        if (board[to.row][to.col] != null && board[to.row][to.col].getType() == PieceType.PAWN) {
            if (to.row == 0 || to.row == 7) {
                promotePawn(to);
            }
        }
        
        updateBoard();
        return capturedPiece;
    }

       private void performCastle(Position from, Position to) {
        boolean isWhite = board[from.row][from.col].isWhite();
        int row = isWhite ? 7 : 0;
        
        if (to.col == 6) { 
            board[row][6] = board[row][4];
            board[row][5] = board[row][7];
            board[row][4] = null;
            board[row][7] = null;
        } else if (to.col == 2) { 
            board[row][2] = board[row][4];
            board[row][3] = board[row][0];
            board[row][4] = null;
            board[row][0] = null;
        }
        
        if (isWhite) {
            whiteKingPosition = new Position(row, to.col);
            castlingRights[0] = false;
            castlingRights[1] = false;
        } else {
            blackKingPosition = new Position(row, to.col);
            castlingRights[2] = false;
            castlingRights[3] = false;
        }
        
        updateBoard();
    }

    private Piece performEnPassant(Position from, Position to) {
        Piece movingPiece = board[from.row][from.col];
        int capturedPawnRow = movingPiece.isWhite() ? to.row + 1 : to.row - 1;
        Piece capturedPiece = board[capturedPawnRow][to.col];
        
        board[to.row][to.col] = movingPiece;
        board[from.row][from.col] = null;
        board[capturedPawnRow][to.col] = null;
        
        updateBoard();
        return capturedPiece;
    }

   private void promotePawn(Position position) {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(frame, "Choose promotion:", "Pawn Promotion",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);
        
        PieceType promotion;
        switch (choice) {
            case 0: promotion = PieceType.QUEEN; break;
            case 1: promotion = PieceType.ROOK; break;
            case 2: promotion = PieceType.BISHOP; break;
            case 3: promotion = PieceType.KNIGHT; break;
            default: promotion = PieceType.QUEEN;
        }
        
        board[position.row][position.col] = new Piece(promotion, board[position.row][position.col].isWhite());
        updateBoard();
    }

    private void updateCastlingRights(Position from, Piece piece) {
        if (piece.getType() == PieceType.KING) {
            if (piece.isWhite()) {
                castlingRights[0] = false;
                castlingRights[1] = false;
            } else {
                castlingRights[2] = false;
                castlingRights[3] = false;
            }
        } else if (piece.getType() == PieceType.ROOK) {
            if (piece.isWhite()) {
                if (from.equals(new Position(7, 0))) castlingRights[1] = false;
                if (from.equals(new Position(7, 7))) castlingRights[0] = false;
            } else {
                if (from.equals(new Position(0, 0))) castlingRights[3] = false;
                if (from.equals(new Position(0, 7))) castlingRights[2] = false;
            }
        }
    }

    private Piece makeMoveOnBoard(Position from, Position to, Piece[][] board) {
        Piece movingPiece = board[from.row][from.col];
        Piece captured = board[to.row][to.col];
        
        board[to.row][to.col] = movingPiece;
        board[from.row][from.col] = null;
        
        return captured;
    }

    private void undoMoveOnBoard(Position from, Position to, Piece captured, Piece[][] board) {
        Piece movingPiece = board[to.row][to.col];
        board[from.row][from.col] = movingPiece;
        board[to.row][to.col] = captured;
    }

    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE;
    }

    private boolean isPossibleMove(Position position) {
        for (Position move : possibleMoves) {
            if (move.equals(position)) {
                return true;
            }
        }
        return false;
    }

    private void highlightPossibleMoves() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (selectedPiece != null && selectedPiece.row == row && selectedPiece.col == col) {
                    squares[row][col].setBackground(Color.YELLOW);
                } else if (isPossibleMove(new Position(row, col))) {
                    squares[row][col].setBackground(Color.GREEN);
                } else {
                    if ((row + col) % 2 == 0) {
                        squares[row][col].setBackground(new Color(240, 217, 181));
                    } else {
                        squares[row][col].setBackground(new Color(181, 136, 99));
                    }
                }
            }
        }
        updateBoard();
    }

    private void clearSelection() {
        selectedPiece = null;
        possibleMoves.clear();
        
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if ((row + col) % 2 == 0) {
                    squares[row][col].setBackground(new Color(240, 217, 181));
                } else {
                    squares[row][col].setBackground(new Color(181, 136, 99));
                }
            }
        }
        updateBoard();
    }

    private void updateBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                squares[row][col].setIcon(null);
                
                Piece piece = board[row][col];
                if (piece != null) {
                    String iconPath = getIconPath(piece);
                    squares[row][col].setIcon(new ImageIcon(iconPath));
                }
            }
        }
    }

    private String getIconPath(Piece piece) {
        String color = piece.isWhite() ? "w" : "b";
        String type = "";
        
        switch (piece.getType()) {
            case PAWN: type = "p"; break;
            case ROOK: type = "r"; break;
            case KNIGHT: type = "n"; break;
            case BISHOP: type = "b"; break;
            case QUEEN: type = "q"; break;
            case KING: type = "k"; break;
        }
        
        return "chess_pieces/" + color + type + ".png"; 
    }

    private int evaluateBoard() {
        int score = 0;
        
       
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Piece piece = board[row][col];
                if (piece != null) {
                    int value = getPieceValue(piece.getType());
                    score += piece.isWhite() ? value : -value;
                }
            }
        }
        
        
        if (isInCheck(true)) score -= 50;  
        if (isInCheck(false)) score += 50; 
        
        return score;
    }

    private int getPieceValue(PieceType type) {
        switch (type) {
            case PAWN: return 10;
            case KNIGHT: return 30;
            case BISHOP: return 30;
            case ROOK: return 50;
            case QUEEN: return 90;
            case KING: return 900;
            default: return 0;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChessGame());
    }

   
    private static class Piece {
        private PieceType type;
        private boolean white;
        
        public Piece(PieceType type, boolean white) {
            this.type = type;
            this.white = white;
        }
        
        public PieceType getType() { return type; }
        public boolean isWhite() { return white; }
    }

    private enum PieceType {
        PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING
    }

    private static class Position {
        int row, col;
        
        public Position(int row, int col) {
            this.row = row;
            this.col = col;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Position position = (Position) obj;
            return row == position.row && col == position.col;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(row, col);
        }
    }

    private static class Move {
        Position from, to;
        
        public Move(Position from, Position to) {
            this.from = from;
            this.to = to;
        }
    }
}