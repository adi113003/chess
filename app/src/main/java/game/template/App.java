package game.template;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Random;

public class App extends Application {
    private static final int SIZE = 8;
    private static final int SQUARE_SIZE = 70;
    private VBox root;
    private StackPane[][] grid = new StackPane[SIZE][SIZE];

    private int selectedRow = -1;
    private int selectedCol = -1;
    private Player currentPlayer = Player.WHITE;
    private boolean playAgainstAI = false;
    private List<String> moveHistory = new ArrayList<>();
    private TextArea moveHistoryArea;
    private Stack<Move> moveStack = new Stack<>();

    private Timeline whiteTimer;
    private Timeline blackTimer;
    private int whiteTimeSeconds;
    private int blackTimeSeconds;
    private int timeIncrement = 0; // Increment per move
    private Label whiteTimeLabel;
    private Label blackTimeLabel;

    @Override
    public void start(Stage primaryStage) throws Exception {
        root = new VBox();

        root.getChildren().add(createMenuBar());

        HBox timerBox = new HBox();
        whiteTimeLabel = new Label();
        blackTimeLabel = new Label();
        timerBox.getChildren().addAll(new Label("White: "), whiteTimeLabel, new Label("  |  Black: "), blackTimeLabel);
        root.getChildren().add(timerBox);

        GridPane gridPane = new GridPane();
        gridPane.setPrefSize(SQUARE_SIZE * 8, SQUARE_SIZE * 8);

        root.getChildren().add(gridPane);

        moveHistoryArea = new TextArea();
        moveHistoryArea.setEditable(false);
        moveHistoryArea.setPrefHeight(100);
        root.getChildren().add(moveHistoryArea);

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                StackPane cell = createCell(row, col);
                grid[row][col] = cell;
                gridPane.add(cell, col, row);
            }
        }

        HBox controls = new HBox();
        Button undoButton = new Button("Undo");
        undoButton.setOnAction(event -> undoMove());
        controls.getChildren().add(undoButton);
        root.getChildren().add(controls);

        Scene scene = new Scene(root);

        URL styleURL = getClass().getResource("/style.css");
        String stylesheet = styleURL.toExternalForm();
        scene.getStylesheets().add(stylesheet);

        primaryStage.setTitle("Chess Game");
        primaryStage.setScene(scene);
        primaryStage.show();

        showTimeControlDialog(primaryStage);
    }

    private StackPane createCell(int row, int col) {
        Rectangle rect = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);
        if ((row + col) % 2 == 0) {
            rect.getStyleClass().add("white-square");
        } else {
            rect.getStyleClass().add("black-square");
        }

        StackPane cell = new StackPane(rect);
        cell.setId(row + "-" + col);
        cell.setOnMouseClicked(event -> handleMouseClick(event, row, col));

        return cell;
    }

    private void clearBoard() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                grid[row][col].getChildren().removeIf(child -> child instanceof ImageView);
            }
        }
    }

    private void drawInitialBoard() {
        clearBoard();

        for (int col = 0; col < SIZE; col++) {
            placePiece(Player.WHITE, ChessPiece.PAWN, 6, col);
            placePiece(Player.BLACK, ChessPiece.PAWN, 1, col);
        }

        placePiece(Player.WHITE, ChessPiece.ROOK, 7, 0);
        placePiece(Player.WHITE, ChessPiece.ROOK, 7, 7);
        placePiece(Player.BLACK, ChessPiece.ROOK, 0, 0);
        placePiece(Player.BLACK, ChessPiece.ROOK, 0, 7);

        placePiece(Player.WHITE, ChessPiece.KNIGHT, 7, 1);
        placePiece(Player.WHITE, ChessPiece.KNIGHT, 7, 6);
        placePiece(Player.BLACK, ChessPiece.KNIGHT, 0, 1);
        placePiece(Player.BLACK, ChessPiece.KNIGHT, 0, 6);

        placePiece(Player.WHITE, ChessPiece.BISHOP, 7, 2);
        placePiece(Player.WHITE, ChessPiece.BISHOP, 7, 5);
        placePiece(Player.BLACK, ChessPiece.BISHOP, 0, 2);
        placePiece(Player.BLACK, ChessPiece.BISHOP, 0, 5);

        placePiece(Player.WHITE, ChessPiece.QUEEN, 7, 3);
        placePiece(Player.BLACK, ChessPiece.QUEEN, 0, 3);

        placePiece(Player.WHITE, ChessPiece.KING, 7, 4);
        placePiece(Player.BLACK, ChessPiece.KING, 0, 4);

        moveHistory.clear();
        moveStack.clear();
        updateMoveHistoryDisplay();
    }

    private void handleMouseClick(MouseEvent event, int row, int col) {
        if (currentPlayer == Player.BLACK && playAgainstAI) {
            return;
        }

        if (selectedRow == -1 && selectedCol == -1) {
            if (grid[row][col].getChildren().stream().anyMatch(child -> child instanceof ImageView)) {
                ImageView piece = (ImageView) grid[row][col].getChildren().stream()
                        .filter(child -> child instanceof ImageView).findFirst().orElse(null);
                if (piece != null) {
                    String url = piece.getImage().getUrl();
                    if ((currentPlayer == Player.WHITE && url.contains("/w")) || (currentPlayer == Player.BLACK && url.contains("/b"))) {
                        selectedRow = row;
                        selectedCol = col;
                        highlightPossibleMoves(row, col);
                    } else {
                        showAlert("Not Your Turn", "It's not your turn to move this piece.");
                    }
                }
            }
        } else {
            if (isValidMove(selectedRow, selectedCol, row, col)) {
                movePiece(selectedRow, selectedCol, row, col);
                switchTurn();
            } else {
                showAlert("Invalid Move", "The move is not valid according to chess rules.");
            }
            clearHighlights();
            selectedRow = -1;
            selectedCol = -1;
        }
    }

    private void highlightPossibleMoves(int row, int col) {
        clearHighlights();

        ImageView piece = (ImageView) grid[row][col].getChildren().stream()
                .filter(child -> child instanceof ImageView).findFirst().orElse(null);

        if (piece == null) {
            return;
        }

        String url = piece.getImage().getUrl();
        String pieceType = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'));

        for (int toRow = 0; toRow < SIZE; toRow++) {
            for (int toCol = 0; toCol < SIZE; toCol++) {
                if (isValidMove(row, col, toRow, toCol)) {
                    Rectangle highlight = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);
                    highlight.getStyleClass().add("highlight");
                    grid[toRow][toCol].getChildren().add(highlight);
                }
            }
        }
    }

    private void clearHighlights() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                grid[row][col].getChildren().removeIf(child -> child.getStyleClass().contains("highlight"));
            }
        }
    }

    private void switchTurn() {
        currentPlayer = (currentPlayer == Player.WHITE) ? Player.BLACK : Player.WHITE;

        if (currentPlayer == Player.BLACK && playAgainstAI) {
            aiMove();
            currentPlayer = Player.WHITE;
        }

        if (currentPlayer == Player.WHITE) {
            startWhiteTimer();
        } else {
            startBlackTimer();
        }
    }

    private void aiMove() {
        List<int[]> validMoves = new ArrayList<>();

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (grid[row][col].getChildren().stream().anyMatch(child -> child instanceof ImageView)) {
                    ImageView piece = (ImageView) grid[row][col].getChildren().stream()
                            .filter(child -> child instanceof ImageView).findFirst().orElse(null);
                    if (piece != null && piece.getImage().getUrl().contains("/b")) {
                        for (int toRow = 0; toRow < SIZE; toRow++) {
                            for (int toCol = 0; toCol < SIZE; toCol++) {
                                if (isValidMove(row, col, toRow, toCol)) {
                                    validMoves.add(new int[]{row, col, toRow, toCol});
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!validMoves.isEmpty()) {
            Random rand = new Random();
            int[] move = validMoves.get(rand.nextInt(validMoves.size()));
            movePiece(move[0], move[1], move[2], move[3]);
        }
    }

    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow == toRow && fromCol == toCol) {
            return false;
        }

        ImageView piece = (ImageView) grid[fromRow][fromCol].getChildren().stream()
                .filter(child -> child instanceof ImageView).findFirst().orElse(null);

        if (piece == null) {
            return false;
        }

        String url = piece.getImage().getUrl();
        String pieceType = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'));

        switch (pieceType) {
            case "wpawn":
                return validatePawnMove(fromRow, fromCol, toRow, toCol, Player.WHITE);
            case "bpawn":
                return validatePawnMove(fromRow, fromCol, toRow, toCol, Player.BLACK);
            case "wrook":
            case "brook":
                return validateRookMove(fromRow, fromCol, toRow, toCol);
            case "wknight":
            case "bknight":
                return validateKnightMove(fromRow, fromCol, toRow, toCol);
            case "wbishop":
            case "bbishop":
                return validateBishopMove(fromRow, fromCol, toRow, toCol);
            case "wqueen":
            case "bqueen":
                return validateQueenMove(fromRow, fromCol, toRow, toCol);
            case "wking":
            case "bking":
                return validateKingMove(fromRow, fromCol, toRow, toCol);
            default:
                return false;
        }
    }

    private boolean validatePawnMove(int fromRow, int fromCol, int toRow, int toCol, Player player) {
        int direction = (player == Player.WHITE) ? -1 : 1;
        int startRow = (player == Player.WHITE) ? 6 : 1;

        if (fromCol == toCol) {
            if (toRow == fromRow + direction) {
                return isEmpty(toRow, toCol);
            }
            if (fromRow == startRow && toRow == fromRow + 2 * direction) {
                return isEmpty(fromRow + direction, fromCol) && isEmpty(toRow, toCol);
            }
        } else if (Math.abs(fromCol - toCol) == 1 && toRow == fromRow + direction) {
            return !isEmpty(toRow, toCol) && isEnemyPiece(toRow, toCol, player);
        }

        return false;
    }

    private boolean validateRookMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow != toRow && fromCol != toCol) {
            return false;
        }

        int stepRow = Integer.compare(toRow, fromRow);
        int stepCol = Integer.compare(toCol, fromCol);

        int currentRow = fromRow + stepRow;
        int currentCol = fromCol + stepCol;

        while (currentRow != toRow || currentCol != toCol) {
            if (!isEmpty(currentRow, currentCol)) {
                return false;
            }
            currentRow += stepRow;
            currentCol += stepCol;
        }

        return true;
    }

    private boolean validateKnightMove(int fromRow, int fromCol, int toRow, int toCol) {
        int rowDiff = Math.abs(fromRow - toRow);
        int colDiff = Math.abs(fromCol - toCol);

        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }

    private boolean validateBishopMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (Math.abs(fromRow - toRow) != Math.abs(fromCol - toCol)) {
            return false;
        }

        int stepRow = Integer.compare(toRow, fromRow);
        int stepCol = Integer.compare(toCol, fromCol);

        int currentRow = fromRow + stepRow;
        int currentCol = fromCol + stepCol;

        while (currentRow != toRow || currentCol != toCol) {
            if (!isEmpty(currentRow, currentCol)) {
                return false;
            }
            currentRow += stepRow;
            currentCol += stepCol;
        }

        return true;
    }

    private boolean validateQueenMove(int fromRow, int fromCol, int toRow, int toCol) {
        return validateRookMove(fromRow, fromCol, toRow, toCol) || validateBishopMove(fromRow, fromCol, toRow, toCol);
    }

    private boolean validateKingMove(int fromRow, int fromCol, int toRow, int toCol) {
        return Math.abs(fromRow - toRow) <= 1 && Math.abs(fromCol - toCol) <= 1;
    }

    private boolean isEmpty(int row, int col) {
        return grid[row][col].getChildren().stream().noneMatch(child -> child instanceof ImageView);
    }

    private boolean isEnemyPiece(int row, int col, Player player) {
        ImageView piece = (ImageView) grid[row][col].getChildren().stream()
                .filter(child -> child instanceof ImageView).findFirst().orElse(null);

        if (piece == null) {
            return false;
        }

        String url = piece.getImage().getUrl();
        return (player == Player.WHITE && url.contains("/b")) || (player == Player.BLACK && url.contains("/w"));
    }

    private void movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        ImageView piece = (ImageView) grid[fromRow][fromCol].getChildren().stream()
                .filter(child -> child instanceof ImageView).findFirst().orElse(null);

        if (piece != null) {
            // Capture logic: remove the piece at the destination if it belongs to the opponent
            if (grid[toRow][toCol].getChildren().stream().anyMatch(child -> child instanceof ImageView)) {
                ImageView capturedPiece = (ImageView) grid[toRow][toCol].getChildren().stream()
                        .filter(child -> child instanceof ImageView).findFirst().orElse(null);
                if (capturedPiece != null && isEnemyPiece(toRow, toCol, currentPlayer)) {
                    grid[toRow][toCol].getChildren().remove(capturedPiece);
                }
            }

            grid[toRow][toCol].getChildren().add(piece);
            grid[fromRow][fromCol].getChildren().remove(piece);
            moveStack.push(new Move(fromRow, fromCol, toRow, toCol, piece)); // Save the move to the stack
            logMove(fromRow, fromCol, toRow, toCol, piece);
        }
    }

    private void undoMove() {
        if (!moveStack.isEmpty()) {
            Move lastMove = moveStack.pop();
            ImageView piece = (ImageView) grid[lastMove.toRow][lastMove.toCol].getChildren().stream()
                    .filter(child -> child instanceof ImageView).findFirst().orElse(null);

            if (piece != null) {
                grid[lastMove.fromRow][lastMove.fromCol].getChildren().add(piece);
                grid[lastMove.toRow][lastMove.toCol].getChildren().remove(piece);
                moveHistory.remove(moveHistory.size() - 1); // Remove the last move from the history
                updateMoveHistoryDisplay();
                switchTurn(); // Switch turn back
            }
        }
    }

    private void logMove(int fromRow, int fromCol, int toRow, int toCol, ImageView piece) {
        String pieceType = piece.getImage().getUrl().substring(piece.getImage().getUrl().lastIndexOf('/') + 1, piece.getImage().getUrl().lastIndexOf('.'));
        String move = String.format("%s: %s (%d, %d) -> (%d, %d)", currentPlayer, pieceType, fromRow, fromCol, toRow, toCol);
        moveHistory.add(move);
        updateMoveHistoryDisplay();
    }

    private void updateMoveHistoryDisplay() {
        moveHistoryArea.setText(String.join("\n", moveHistory));
    }

    private void placePiece(Player player, ChessPiece piece, int row, int col) {
        String imageName = (player == Player.WHITE ? "w" : "b") + piece.toString().toLowerCase() + ".png";
        ImageView image = loadImage(imageName);
        grid[row][col].getChildren().add(image);
        image.setFitWidth(SQUARE_SIZE);
        image.setFitHeight(SQUARE_SIZE);
    }

    private ImageView loadImage(String name) {
        return new ImageView(getClass().getResource("/assets/" + name).toExternalForm());
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("menubar");

        Menu fileMenu = new Menu("File");

        addMenuItem(fileMenu, "Load from file", () -> {
            System.out.println("Load from file");
        });

        addMenuItem(fileMenu, "Initial Board", () -> {
            drawInitialBoard();
        });

        Menu UndoMenu = new Menu("Undo");
        addMenuItem(UndoMenu, "Undo", () -> {
            undoMove();
        });

        Menu gameModeMenu = new Menu("Game Mode");
        addMenuItem(gameModeMenu, "Play Against Human", () -> {
            playAgainstAI = false;
            drawInitialBoard();
        });
        addMenuItem(gameModeMenu, "Play Against AI", () -> {
            playAgainstAI = true;
            drawInitialBoard();
        });

        menuBar.getMenus().addAll(fileMenu, gameModeMenu, UndoMenu);

        return menuBar;
    }

    private void addMenuItem(Menu menu, String name, Runnable action) {
        MenuItem menuItem = new MenuItem(name);
        menuItem.setOnAction(event -> action.run());
        menu.getItems().add(menuItem);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void startWhiteTimer() {
        if (blackTimer != null) {
            blackTimer.stop();
        }
        if (whiteTimer == null) {
            whiteTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                whiteTimeSeconds--;
                whiteTimeLabel.setText(formatTime(whiteTimeSeconds));
                if (whiteTimeSeconds <= 0) {
                    showAlert("Time's Up!", "Black wins by timeout!");
                }
            }));
            whiteTimer.setCycleCount(Timeline.INDEFINITE);
        }
        whiteTimer.play();
    }

    private void startBlackTimer() {
        if (whiteTimer != null) {
            whiteTimer.stop();
        }
        if (blackTimer == null) {
            blackTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                blackTimeSeconds--;
                blackTimeLabel.setText(formatTime(blackTimeSeconds));
                if (blackTimeSeconds <= 0) {
                    showAlert("Time's Up!", "White wins by timeout!");
                }
            }));
            blackTimer.setCycleCount(Timeline.INDEFINITE);
        }
        blackTimer.play();
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void showTimeControlDialog(Stage primaryStage) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(primaryStage);
        dialog.setTitle("Time Control Settings");

        Label timeLabel = new Label("Initial Time (minutes): ");
        TextField timeField = new TextField("10");
        Label incrementLabel = new Label("Increment (seconds): ");
        TextField incrementField = new TextField("0");

        GridPane grid = new GridPane();
        grid.add(timeLabel, 0, 0);
        grid.add(timeField, 1, 0);
        grid.add(incrementLabel, 0, 1);
        grid.add(incrementField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int initialTime = Integer.parseInt(timeField.getText()) * 60;
                int increment = Integer.parseInt(incrementField.getText());
                whiteTimeSeconds = initialTime;
                blackTimeSeconds = initialTime;
                timeIncrement = increment;
                whiteTimeLabel.setText(formatTime(whiteTimeSeconds));
                blackTimeLabel.setText(formatTime(blackTimeSeconds));
                drawInitialBoard();
                startWhiteTimer();
            } else {
                primaryStage.close();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class Move {
    int fromRow, fromCol, toRow, toCol;
    ImageView piece;

    public Move(int fromRow, int fromCol, int toRow, int toCol, ImageView piece) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.piece = piece;
    }
}


