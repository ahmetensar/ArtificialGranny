package cs461.artificialgranny;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.SerializationUtils;

/**
 * This class controls everything about GUI
 *
 * @author ahmetensar
 * @version 0.3
 */

public class PuzzleLayoutController implements Initializable {

  /*
   * These observableLists stores the bindings for updating GUI simultaneously
   */

  private final ObservableList<StringProperty> puzzleGridLetters = FXCollections
      .observableArrayList();
  private final ObservableList<StringProperty> puzzleGridNumbers = FXCollections
      .observableArrayList();
  private final ObservableList<StringProperty> puzzleGridColors = FXCollections
      .observableArrayList();
  private final ObservableList<StringProperty> solutionGridLetters = FXCollections
      .observableArrayList();
  private final ObservableList<StringProperty> solutionGridNumbers = FXCollections
      .observableArrayList();
  private final ObservableList<StringProperty> solutionGridColors = FXCollections
      .observableArrayList();
  private final ObservableList<StringProperty> clues = FXCollections.observableArrayList();
  private final ListProperty<String> candidates = new SimpleListProperty<>();

  // the chosen word in puzzle which is marked in blue
  private final IntegerProperty chosenIndex = new SimpleIntegerProperty();


  /*
   * FXML objects
   */

  @FXML // main pane
  public BorderPane borderPane;

  @FXML // save in MenuBar
  private MenuItem saveMenuItem;

  @FXML // reset in MenuBar
  private MenuItem resetMenuItem;

  @FXML // stores the previous and next buttons above puzzle
  private HBox buttonBox;

  @FXML // stores puzzleGrid
  private StackPane puzzlePane;

  @FXML // the puzzle that the program tries to solve
  private GridPane puzzleGrid;

  @FXML // stores clues
  private TreeView<String> treeView;

  @FXML // stores listView and flowPane
  private SplitPane horizontalSplitPane;

  @FXML // stores candidates
  private ListView<String> listView;

  @FXML // stores solutionGrid
  private FlowPane flowPane;

  @FXML // the solution puzzle
  private GridPane solutionGrid;

  // cells of puzzle, used to update cells with keyboard
  private List<GridPane> cellGrids;

  // the file path of the program
  private String path;

  // this thread used when program starts, to open Chrome WebDriver
  private Thread openingThread;

  // list of puzzles, used when previous button is called
  private List<PuzzleState> steps;

  // current puzzle state to be solved
  private PuzzleState puzzleState;

  // geometry of the puzzle
  private int[][] geometry;

  // previous clicked cell grid,
  // if previous and current are same, gui changes the direction (across-down)
  private GridPane prevCellGrid;

  // selected cell index
  private int cellIndex;

  private Stage stage;
  private Puzzle solution;
  private boolean isAcross;
  private boolean isPuzzleLoaded;
  private boolean isSpaceLeft;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    setupDriver();
    setupListeners();
    bindCells();
    bindClues();
    bindCandidates();
    setupResize();
    setupPath();
  }

  private void setupDriver() {
    Task<Boolean> task = new Task<Boolean>() {
      @Override
      protected Boolean call() throws Exception {
        try {
          Puzzle.setupDriver();
          return true;
        } catch (Exception e) {
          e.printStackTrace();
          return false;
        }
      }
    };
    task.setOnSucceeded(Event -> {
      if (!task.getValue()) {
        final Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error");
        alert.setContentText("Could not load driver");
        alert.show();
      }
    });
    openingThread = new Thread(task);
    openingThread.start();
  }

  private void setupListeners() {
    BooleanProperty flag = new SimpleBooleanProperty(false);
    chosenIndex.addListener((observable, oldValue, newValue) -> {
      if (!flag.get()) {
        flag.set(true);
        cellIndex = -1;
        List<Integer> relatedList = new LinkedList<>();
        relatedList.add(newValue.intValue());
        if (solution.getRelated() != null) {
          relatedList.addAll(solution.getRelated().get(newValue.intValue()));
        }
        treeView.getSelectionModel().clearSelection();
        if (relatedList.size() > 1) {
          treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        } else {
          treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        }

        if (newValue.equals(0)) {
          treeView.getSelectionModel().select(treeView.getRoot().getChildren().get(0));
        } else {
          for (int relatedNum : relatedList) {
            for (int i = 1; i <= 2; i++) {
              TreeItem<String> subRoot = treeView.getRoot().getChildren().get(i);
              for (TreeItem<String> item : subRoot.getChildren()) {
                if (item != null) {
                  char c = item.getValue().charAt(0);
                  if (Character.isDigit(c)) {
                    int num = Character.getNumericValue(c);
                    if (subRoot.getValue().equals("Down")) {
                      num = -num;
                    }
                    if (relatedNum == num) {
                      treeView.getSelectionModel().select(item);
                    }
                  }
                }
              }
            }
          }
        }
        chosenIndex.setValue(newValue);
        flag.set(false);
      }
    });

    treeView.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> {
          if (geometry != null && newValue != null && !newValue.getParent()
              .equals(treeView.getRoot()) && newValue.getValue().length() > 0) {
            char c = newValue.getValue().charAt(0);
            if (Character.isDigit(c)) {
              int num = Character.getNumericValue(c);
              if (newValue.getParent().getValue().equals("Down")) {
                num = -num;
              }
              chosenIndex.set(num);
              showColors();
            }
          }
        }
    );

    borderPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (cellIndex >= 0) {
        int x = cellIndex / 5;
        int y = cellIndex % 5;
        if (event.getCode() == KeyCode.BACK_SPACE) {
          Label letter = (Label) cellGrids.get(cellIndex).getChildren().get(0);
          if (letter.getText().equals(" ")) {
            selectCellGrid(searchPrevCell(cellIndex));
          } else {
            String str = " ";
            letter.setText(str);
            puzzleState.getPuzzle().setPuzzleSquare(x, y, str.charAt(0));
            steps.get(steps.size() - 1).getPuzzle().setPuzzleSquare(x, y, str.charAt(0));
          }
        } else if (event.getCode() == KeyCode.LEFT) {
          if (y != 0 && geometry[x][y - 1] != -1) {
            if (!isAcross) {
              isAcross = true;
              selectCellGrid(cellIndex);
            } else
              selectCellGrid(cellIndex - 1);
          }
        } else if(event.getCode() == KeyCode.RIGHT) {
          if (y != 4 && geometry[x][y + 1] != -1) {
            if (!isAcross) {
              isAcross = true;
              selectCellGrid(cellIndex);
            } else
              selectCellGrid(cellIndex + 1);
          }
        } else if (event.getCode() == KeyCode.UP) {
          if (x != 0 && geometry[x - 1][y] != -1) {
            if (isAcross) {
              isAcross = false;
              selectCellGrid(cellIndex);
            } else
              selectCellGrid(cellIndex - 5);
          }
        } else if(event.getCode() == KeyCode.DOWN) {
          if (x != 4 && geometry[x + 1][y] != -1) {
            if (isAcross) {
              isAcross = false;
              selectCellGrid(cellIndex);
            } else
              selectCellGrid(cellIndex + 5);
          }
        } else if (event.getCode().isLetterKey() || event.getCode().isDigitKey()){
          Label letter  = (Label) cellGrids.get(cellIndex).getChildren().get(0);
          String str = event.getText().toUpperCase();
          letter.setText(str);
          puzzleState.getPuzzle().setPuzzleSquare(x, y, str.charAt(0));
          steps.get(steps.size() - 1).getPuzzle().setPuzzleSquare(x, y, str.charAt(0));
          selectCellGrid(searchNextCell(cellIndex));
        }
        event.consume();
      }
    });
  }

  private void bindCells() {
    GridPane gridPane;
    ObservableList<StringProperty> letters;
    ObservableList<StringProperty> numbers;
    ObservableList<StringProperty> colors;
    cellGrids = new ArrayList<>();

    try {
      for (int gridNum = 0; gridNum <= 1; gridNum++) {
        if (gridNum == 0) {
          gridPane = solutionGrid;
          letters = solutionGridLetters;
          numbers = solutionGridNumbers;
          colors = solutionGridColors;
        } else {
          gridPane = puzzleGrid;
          letters = puzzleGridLetters;
          numbers = puzzleGridNumbers;
          colors = puzzleGridColors;
        }

        for (int i = 0; i < 5; i++) {
          for (int j = 0; j < 5; j++) {
            final int x = i;
            final int y = j;

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Main.class.getResource("CellGrid.fxml"));
            GridPane cellGrid = loader.load();

            StringProperty letter = new SimpleStringProperty();
            ((Label) cellGrid.getChildren().get(0)).textProperty().bindBidirectional(letter);
            StringProperty number = new SimpleStringProperty();
            ((Label) cellGrid.getChildren().get(1)).textProperty().bind(number);

            StringProperty color = new SimpleStringProperty();
            color.addListener((observable, oldValue, newValue) -> {
              switch (newValue) {
                case "BLACK":
                  cellGrid.setBackground(new Background(
                      new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, new Insets(1, 1, 1, 1))));
                  break;
                case "CHOSEN_LINE":
                  cellGrid.setBackground(new Background(
                      new BackgroundFill(Color.rgb(167, 216, 255), CornerRadii.EMPTY,
                          new Insets(1, 1, 1, 1))));
                  break;
                case "CHOSEN_SQUARE":
                  cellGrid.setBackground(new Background(
                      new BackgroundFill(Color.rgb(255, 218, 0), CornerRadii.EMPTY,
                          new Insets(1, 1, 1, 1))));
                  break;
                default:
                  cellGrid.setBackground(new Background(
                      new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, new Insets(1, 1, 1, 1))));
                  break;
              }
            });
            color.set("");

            if (gridNum == 1)
              cellGrids.add(cellGrid);

            gridPane.add(cellGrid, j, i);
            letters.add(letter);
            numbers.add(number);
            colors.add(color);

            isPuzzleLoaded = false;
            cellGrid.setOnMouseClicked(event -> {
              if (isPuzzleLoaded)
                selectCellGrid(x * 5 + y);
            });
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void bindClues() {
    TreeItem<String> root = new TreeItem<>();
    root.setExpanded(true);

    TreeItem<String> date = new TreeItem<>("Date");

    TreeItem<String> acrossRoot = new TreeItem<>("Across");
    acrossRoot.setExpanded(true);

    TreeItem<String> downRoot = new TreeItem<>("Down");
    downRoot.setExpanded(true);

    for (int i = 0; i < 10; i++) {
      StringProperty clue = new SimpleStringProperty();
      TreeItem<String> item = new TreeItem<>("");
      item.valueProperty().bind(clue);
      if (i < 5) {
        acrossRoot.getChildren().add(item);
      } else {
        downRoot.getChildren().add(item);
      }
      clues.add(clue);
    }
    root.getChildren().add(date);
    root.getChildren().add(acrossRoot);
    root.getChildren().add(downRoot);

    treeView.setRoot(root);
    treeView.setShowRoot(false);
  }

  private void bindCandidates() {
    listView.itemsProperty().bind(candidates);
  }

  private void setupResize() {
    SplitPane.setResizableWithParent(flowPane, false);
    SplitPane.setResizableWithParent(listView, false);
    listView.minWidthProperty().bind(horizontalSplitPane.widthProperty().multiply(0.5));
    listView.prefWidthProperty().bind(horizontalSplitPane.widthProperty().multiply(0.5));
    puzzleGrid.maxHeightProperty().bind(puzzlePane.widthProperty());
    puzzleGrid.maxWidthProperty().bind(puzzleGrid.heightProperty());
    solutionGrid.prefWidthProperty().bind(
        horizontalSplitPane.widthProperty().subtract(listView.widthProperty()).subtract(10));
    solutionGrid.prefHeightProperty().bind(
        horizontalSplitPane.widthProperty().subtract(listView.widthProperty()).subtract(10));
  }

  private void setupPath() {
    try {
      path = getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
      int index = path.lastIndexOf("/");
      if(index > -1)
        path = path.substring(0, index + 1);
      else
        path = System.getProperty("user.home");
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  private int searchNextCell(int index) {
    int x = index / 5;
    int y = index % 5;

    int current = isAcross ? y : x;
    for (int num = (current == 4 ? 0 : current + 1); num != current; num = (num >= 4 ? 0 : num + 1)) {
      int i = isAcross ? x : num;
      int j = isAcross ? num : y;
      if (geometry[i][j] != -1 && puzzleGridLetters.get(i * 5 + j).get().equals(" ")) {
        isSpaceLeft = true;
        return i * 5 + j;
      }
    }
    if (current == 4)
      return -1;
    if (isSpaceLeft) {
      isSpaceLeft = false;
      return -1;
    }
    return isAcross ? x * 5 + y + 1 : (x + 1) * 5 + y;

  }

  private int searchPrevCell(int index) {
    int x = isAcross ? index / 5 : index / 5 - 1;
    int y = isAcross ? index % 5 - 1 : index % 5;

    if (x == -1 || y == -1 || geometry[x][y] == -1)
      return -1;
    return x * 5 + y;
  }

  private void selectCellGrid(int index) {
    if (index == -1 || puzzleGridNumbers.get(index).get().equals("-1"))
      return;

    GridPane cellGrid = cellGrids.get(index);
    int x = index / 5;
    int y = index % 5;

    if (cellGrid == prevCellGrid ^ chosenIndex.get() >= 0) {
      isAcross = true;
      for (int newY = 0; newY <= y; newY++) {
        String str = puzzleGridNumbers.get(x * 5 + newY).get();
        if (!str.equals("")) {
          int num = Integer.parseInt(str);
          chosenIndex.set(num);
          break;
        }
      }
    } else {
      isAcross = false;
      for (int newX = 0; newX <= x; newX++) {
        String str = puzzleGridNumbers.get(newX * 5 + y).get();
        if (!str.equals("")) {
          int num = Integer.parseInt(str);
          chosenIndex.set(-num);
          break;
        }
      }
    }
    showColors();
    solutionGridColors.get(index).set("CHOSEN_SQUARE");
    puzzleGridColors.get(index).set("CHOSEN_SQUARE");

    prevCellGrid = cellGrid;

    cellIndex = index;
  }

  private void saveFile() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setInitialDirectory(new File(path));
    fileChooser.setInitialFileName(solution.getDate());

    //Extension filter
    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
        "puzzle files (*.puzzle)",
        "*.puzzle");
    fileChooser.getExtensionFilters().add(extFilter);

    //Show save file dialog
    File file = fileChooser.showSaveDialog(stage);

    if (file != null) {
      try {
        solution.savePuzzleToFile(file);
      } catch (Exception e) {
        final Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Puzzle could not be saved to file");
        alert.show();
      }
    }
  }

  private void exit() {
    final Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle("");
    alert.setHeaderText("Are you sure you want to exit?");
    alert.setContentText("");

    alert.getButtonTypes().clear();
    alert.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);

    //Deactivate Default behavior for yes-Button:
    Button yesButton = (Button) alert.getDialogPane().lookupButton(ButtonType.YES);
    yesButton.setDefaultButton(false);

    //Activate Default behavior for no-Button:
    Button noButton = (Button) alert.getDialogPane().lookupButton(ButtonType.NO);
    noButton.setDefaultButton(true);
    final Optional<ButtonType> result = alert.showAndWait();

    if (result.isPresent() && result.get() == ButtonType.YES) {
      if(!Puzzle.isDriverNull())
        Puzzle.closeDriver();
      System.exit(0);
    }
  }

  private void showStep(int stepNum) {
    showCells(steps.get(stepNum).getPuzzle(), 1);
    geometry = steps.get(stepNum).getPuzzle().getGeometry();
    chosenIndex.set(steps.get(stepNum).getChosenIndex());
    showColors();
    candidates.set(FXCollections.observableArrayList(steps.get(stepNum).getCandidates()));
  }

  private void showInitialPuzzle() {
    showCells(null, 0);
    showCells(null, 1);
    showCells(solution, 0);
    showCells(solution, 1, false);
    geometry = solution.getGeometry();
    chosenIndex.set(0);
    showColors();
    showClues();
    addStepButtons();
    saveMenuItem.setDisable(false);
    resetMenuItem.setDisable(false);
    candidates.set(null);
    treeView.getRoot().getChildren().get(0).setValue(solution.getDateFormatted());
    isPuzzleLoaded = true;
  }

  private void showCells(Puzzle puzzle, int gridNum) {
    showCells(puzzle, gridNum, true);
  }

  private void showCells(Puzzle puzzle, int gridNum, boolean showLetters) {
    ObservableList<StringProperty> letters;
    ObservableList<StringProperty> numbers;
    if (gridNum == 0) {
      letters = solutionGridLetters;
      numbers = solutionGridNumbers;
    } else if (gridNum == 1) {
      letters = puzzleGridLetters;
      numbers = puzzleGridNumbers;
    } else {
      return;
    }
    if (puzzle == null) {
      letters.forEach(stringProperty -> stringProperty.setValue(""));
      numbers.forEach(stringProperty -> stringProperty.setValue(""));
    } else {
      for (int i = 0; i < 5; i++) {
        for (int j = 0; j < 5; j++) {
          char letter = puzzle.getLetters()[i][j];
          int number = puzzle.getGeometry()[i][j];
          if (number != -1) {
            if (showLetters)
              letters.get(i * 5 + j).set(letter + "");
            else
              letters.get(i * 5 + j).set(" ");
          }
          if (number > 0) {
            numbers.get(i * 5 + j).set(number + "");
          }
        }
      }
    }
  }

  private void showClues() {
    List<Integer> keys = new ArrayList<>(solution.getClues().keySet());
    for (int i = 0; i < keys.size(); i++) {
      int key = keys.get(i);
      if (key > 0) {
        clues.get(i).set(key + ": " + solution.getClues().get(key));
      } else {
        clues.get(i).set(-key + ": " + solution.getClues().get(key));
      }
    }
  }

  private void showColors() {
    boolean[][] isChosen = new boolean[5][5];
    List<Integer> relatedList = new LinkedList<>();
    relatedList.add(chosenIndex.get());
    if (solution.getRelated() != null) {
      relatedList.addAll(solution.getRelated().get(chosenIndex.get()));
    }
    for (int relatedNum : relatedList) {
      for (int i = 0; i < 5; i++) {
        for (int j = 0; j < 5; j++) {
          int number = geometry[i][j];

          if (relatedNum < 0 && relatedNum == -number) {
            for (int k = i; k < 5; k++) {
              isChosen[k][j] = true;
            }
          } else if (relatedNum > 0 && relatedNum == number) {
            for (int k = j; k < 5; k++) {
              isChosen[i][k] = true;
            }
          }
        }
      }
    }
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 5; j++) {
        int number = geometry[i][j];

        if (number == -1) {
          puzzleGridColors.get(i * 5 + j).set("BLACK");
          solutionGridColors.get(i * 5 + j).set("BLACK");
        } else if (isChosen[i][j]) {
          puzzleGridColors.get(i * 5 + j).set("CHOSEN_LINE");
          solutionGridColors.get(i * 5 + j).set("CHOSEN_LINE");
        } else {
          puzzleGridColors.get(i * 5 + j).set("");
          solutionGridColors.get(i * 5 + j).set("");
        }
      }
    }
  }

  private void addStepButtons() {
    if(buttonBox.getChildren().size() > 2)
      buttonBox.getChildren().remove(1, 4);

    Puzzle solvablePuzzle = SerializationUtils.clone(solution);
    solvablePuzzle.changeToSolvable();

    puzzleState = new PuzzleState(solvablePuzzle);
    steps = new ArrayList<>();
    steps.add(new PuzzleState(puzzleState));
    showStep(0);

    Button previous = new Button("    <    ");
    Button next = new Button("    >    ");

    Label pageNumber = new Label("         1        ");

    previous.setDisable(true);

    previous.setOnAction(event -> {
      next.setDisable(false);

      steps.remove(steps.size() - 1);
      puzzleState = new PuzzleState(steps.get(steps.size() - 1));
      if (steps.size() == 1)
        previous.setDisable(true);

      pageNumber.textProperty().set("         " + steps.size() + "          ");
      showStep(steps.size() - 1);
    });

    next.setOnAction(event -> {
      previous.setDisable(false);

      if (puzzleState.next())
        steps.add(new PuzzleState(puzzleState));
      else
        next.setDisable(true);

      pageNumber.textProperty().set("         " + steps.size() + "          ");
      showStep(steps.size() - 1);
    });

    buttonBox.getChildren().add(1, previous);
    buttonBox.getChildren().add(2, pageNumber);
    buttonBox.getChildren().add(3, next);
  }

  @FXML
  public void importPuzzleFromWebAction() {
    BooleanProperty isCancelled = new SimpleBooleanProperty(false);

    solution = new Puzzle();

    Task<Integer> task = new Task<Integer>() {
      @Override
      protected Integer call() throws Exception {
        try {
          updateMessage("Loading driver");
          if (openingThread.isAlive())
            openingThread.join(); if(Thread.interrupted()) return -1;
          else if (Puzzle.isDriverNull()) {
            setupDriver();
            openingThread.join(); if(Thread.interrupted()) return -1;
          }
          try {
            updateMessage("Opening driver");
            Puzzle.openDriver();
          } catch (Exception e) {
            e.printStackTrace();
            updateMessage("Could not open driver, make sure Google Chrome is installed");
            return 1;
          }
          updateMessage("Opening page...");
          Puzzle.openPage(); if(Thread.interrupted()) return -1;
          updateMessage("Clicking buttons...");
          Puzzle.clickButtons(); if(Thread.interrupted()) return -1;

          solution.emptyPuzzle(); if(Thread.interrupted()) return -1;
          updateMessage("Loading date...");
          solution.loadDateFromDriver(); if(Thread.interrupted()) return -1;
          updateMessage("Loading squares...");
          solution.loadSquaresFromDriver(); if(Thread.interrupted()) return -1;
          updateMessage("Loading clues...");
          solution.loadCluesFromDriver(); if(Thread.interrupted()) return -1;
          updateMessage("Loading word positions...");
          solution.loadWordPositions(); if (Thread.interrupted()) return -1;
          return 0;
        } catch (Exception e) {
          e.printStackTrace();
          updateMessage("Puzzle could not be loaded");
          return 1;
        }
      }
    };

    final Alert alert = new Alert(AlertType.INFORMATION);
    alert.setTitle("Loading Puzzle");
    alert.setHeaderText("Loading Puzzle");
    alert.getButtonTypes().clear();
    alert.getButtonTypes().add(ButtonType.CANCEL);
    final Button cancel = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
    cancel.addEventFilter(ActionEvent.ACTION, event -> isCancelled.set(true));
    task.messageProperty().addListener((observable, oldValue, newValue) -> {
      alert.setContentText(newValue);
      alert.show();
      System.out.println(newValue);
    });

    task.setOnSucceeded(Event -> {
      if (task.getValue() == 0) {
        alert.close();
        showInitialPuzzle();
        saveFile();
        System.out.println("Puzzle is loaded");
      } else if(task.getValue() == 1){
        alert.setAlertType(AlertType.ERROR);
        alert.getButtonTypes().clear();
        alert.getButtonTypes().add(ButtonType.OK);
        alert.setTitle("Error");
        alert.setHeaderText("Error");
        alert.setContentText(task.getMessage());
        alert.show();
        System.out.println(task.getMessage());
      } else {
        alert.close();
      }
      Puzzle.closeDriver();
    });

    Thread thread = new Thread(task);
    isCancelled.addListener(observable -> thread.interrupt());
    thread.start();
  }

  @FXML
  public void openAction() {
    FileChooser fileChooser = new FileChooser();

    //Extension filter
    FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(
        "puzzle files (*.puzzle)", "*.puzzle");
    fileChooser.getExtensionFilters().add(extensionFilter);

    fileChooser.setInitialDirectory(new File(path));

    File file = fileChooser.showOpenDialog(stage);
    if (file != null) {
      solution = new Puzzle();
      try {
        solution.loadPuzzleFromFile(file);
        showInitialPuzzle();
      } catch (Exception e) {
        final Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error");
        alert.setContentText("Puzzle could not be loaded from file");
        alert.show();
      }
    } else {
      System.out.println("Invalid file");
    }
  }

  @FXML
  public void saveAction() {
    saveFile();
  }

  @FXML
  public void exitAction() {
    exit();
  }

  @FXML
  public void aboutAction() {
    final Alert alert = new Alert(AlertType.INFORMATION);
    alert.setTitle("About");
    alert.setHeaderText("Artificial Granny");
    alert.setContentText("CS461 Project\n\n"
        + "Ahmet Ensar\n"
        + "Alper Önder\n"
        + "Mert Kara\n"
        + "Tuğberk Topallar\n"
        + "Uğur Muluk");
    final ImageView imageView = new ImageView(getClass().getResource("project_icon.png").toString());
    final Circle clip = new Circle(32, 32, 32);
    imageView.setClip(clip);
    alert.setGraphic(imageView);
    alert.show();

  }

  @FXML
  public void resetAction() {
    showInitialPuzzle();
  }

  void setStage(Stage stage) {
    this.stage = stage;
    stage.setOnCloseRequest(e -> {
      e.consume();
      exit();
    });
  }
}