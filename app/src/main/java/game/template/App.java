package game.template;

import java.net.URL;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class App extends Application
{
    private static final int SIZE = 8;
    private static final int SQUARE_SIZE = 70;
    private VBox root;
    private StackPane[][] grid = new StackPane[SIZE][SIZE];

    private int selectedRow = -1;
    private int selectedCol = -1;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        root = new VBox();

        root.getChildren().add(createMenuBar());

        GridPane gridPane = new GridPane();
        gridPane.setPrefSize(SQUARE_SIZE * 8, SQUARE_SIZE * 8);
        
        root.getChildren().add(gridPane);

        for (int row = 0; row < SIZE; row++)
        {
            for (int col = 0; col < SIZE; col++)
            {
                StackPane cell = createCell(row, col);
                grid[row][col] = cell;
                gridPane.add(cell, col, row);
            }
        }

        Scene scene = new Scene(root);

        URL styleURL = getClass().getResource("/style.css");
        String stylesheet = styleURL.toExternalForm();
        scene.getStylesheets().add(stylesheet);

        primaryStage.setTitle("Chess Game");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            System.out.println("oncloserequest");
        });

        drawInitialBoard();
    }

    private StackPane createCell(int row, int col)
    {
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

    private void clearBoard()
    {
        for (int row = 0; row < SIZE; row++)
        {
            for (int col = 0; col < SIZE; col++)
            {
                grid[row][col].getChildren().removeIf(child -> child instanceof ImageView);
            }
        }
    }

    private void drawInitialBoard()
    {
        clearBoard();
        
        // Place pawns
        for (int col = 0; col < SIZE; col++) {
            placePiece(Player.WHITE, ChessPiece.PAWN, 6, col);
            placePiece(Player.BLACK, ChessPiece.PAWN, 1, col);
        }
        
        // Place rooks
        placePiece(Player.WHITE, ChessPiece.ROOK, 7, 0);
        placePiece(Player.WHITE, ChessPiece.ROOK, 7, 7);
        placePiece(Player.BLACK, ChessPiece.ROOK, 0, 0);
        placePiece(Player.BLACK, ChessPiece.ROOK, 0, 7);
        
        // Place knights
        placePiece(Player.WHITE, ChessPiece.KNIGHT, 7, 1);
        placePiece(Player.WHITE, ChessPiece.KNIGHT, 7, 6);
        placePiece(Player.BLACK, ChessPiece.KNIGHT, 0, 1);
        placePiece(Player.BLACK, ChessPiece.KNIGHT, 0, 6);
        
        // Place bishops
        placePiece(Player.WHITE, ChessPiece.BISHOP, 7, 2);
        placePiece(Player.WHITE, ChessPiece.BISHOP, 7, 5);
        placePiece(Player.BLACK, ChessPiece.BISHOP, 0, 2);
        placePiece(Player.BLACK, ChessPiece.BISHOP, 0, 5);
        
        // Place queens
        placePiece(Player.WHITE, ChessPiece.QUEEN, 7, 3);
        placePiece(Player.BLACK, ChessPiece.QUEEN, 0, 3);
        
        // Place kings
        placePiece(Player.WHITE, ChessPiece.KING, 7, 4);
        placePiece(Player.BLACK, ChessPiece.KING, 0, 4);
    }

    private void handleMouseClick(MouseEvent event, int row, int col)
    {
        if (selectedRow == -1 && selectedCol == -1) {
            // First click: select piece
            if (grid[row][col].getChildren().stream().anyMatch(child -> child instanceof ImageView)) {
                selectedRow = row;
                selectedCol = col;
            }
        } else {
            // Second click: move piece
            if (isValidMove(selectedRow, selectedCol, row, col)) {
                movePiece(selectedRow, selectedCol, row, col);
            } else {
                showAlert("Invalid Move", "The move is not valid according to chess rules.");
            }
            selectedRow = -1;
            selectedCol = -1;
        }
    }

    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol)
    {
        if (fromRow == toRow && fromCol == toCol) {
            return false; // Can't move to the same square
        }

        ImageView piece = (ImageView) grid[fromRow][fromCol].getChildren().stream()
                .filter(child -> child instanceof ImageView).findFirst().orElse(null);

        if (piece == null) {
            return false;
        }

        String url = piece.getImage().getUrl();
        String pieceType = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'));

        // Add basic movement rules for each piece type
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

    private boolean validatePawnMove(int fromRow, int fromCol, int toRow, int toCol, Player player)
    {
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

    private boolean validateRookMove(int fromRow, int fromCol, int toRow, int toCol)
    {
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

    private boolean validateKnightMove(int fromRow, int fromCol, int toRow, int toCol)
    {
        int rowDiff = Math.abs(fromRow - toRow);
        int colDiff = Math.abs(fromCol - toCol);

        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }

    private boolean validateBishopMove(int fromRow, int fromCol, int toRow, int toCol)
    {
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

    private boolean validateQueenMove(int fromRow, int fromCol, int toRow, int toCol)
    {
        return validateRookMove(fromRow, fromCol, toRow, toCol) || validateBishopMove(fromRow, fromCol, toRow, toCol);
    }

    private boolean validateKingMove(int fromRow, int fromCol, int toRow, int toCol)
    {
        return Math.abs(fromRow - toRow) <= 1 && Math.abs(fromCol - toCol) <= 1;
    }

    private boolean isEmpty(int row, int col)
    {
        return grid[row][col].getChildren().stream().noneMatch(child -> child instanceof ImageView);
    }

    private boolean isEnemyPiece(int row, int col, Player player)
    {
        ImageView piece = (ImageView) grid[row][col].getChildren().stream()
                .filter(child -> child instanceof ImageView).findFirst().orElse(null);

        if (piece == null) {
            return false;
        }

        String url = piece.getImage().getUrl();
        return (player == Player.WHITE && url.contains("/b")) || (player == Player.BLACK && url.contains("/w"));
    }

    private void movePiece(int fromRow, int fromCol, int toRow, int toCol)
    {
        ImageView piece = (ImageView) grid[fromRow][fromCol].getChildren().stream()
                .filter(child -> child instanceof ImageView).findFirst().orElse(null);

        if (piece != null) {
            grid[toRow][toCol].getChildren().add(piece);
            grid[fromRow][fromCol].getChildren().remove(piece);
        }
    }

    private void placePiece(Player player, ChessPiece piece, int row, int col)
    {
        String imageName = (player == Player.WHITE ? "w" : "b") + piece.toString().toLowerCase() + ".png";
        ImageView image = loadImage(imageName);
        grid[row][col].getChildren().add(image);
        image.setFitWidth(SQUARE_SIZE);
        image.setFitHeight(SQUARE_SIZE);
    }

    private ImageView loadImage(String name)
    {
        return new ImageView(getClass().getResource("/assets/" + name).toExternalForm());
    }

    private MenuBar createMenuBar()
    {
        MenuBar menuBar = new MenuBar();
    	menuBar.getStyleClass().add("menubar");

    	Menu fileMenu = new Menu("File");

        addMenuItem(fileMenu, "Load from file", () -> {
            System.out.println("Load from file");
        });

        addMenuItem(fileMenu, "Initial Board", () -> {
            drawInitialBoard();
        });

        menuBar.getMenus().add(fileMenu);

        return menuBar;
    }

    private void addMenuItem(Menu menu, String name, Runnable action)
    {
        MenuItem menuItem = new MenuItem(name);
        menuItem.setOnAction(event -> action.run());
        menu.getItems().add(menuItem);
    }

    private void showAlert(String title, String message)
    {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) 
    {
        launch(args);
    }
}
