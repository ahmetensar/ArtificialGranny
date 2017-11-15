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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.SerializationUtils;

public class PuzzleLayoutController implements Initializable {

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
  private final IntegerProperty chosenIndex = new SimpleIntegerProperty();

  @FXML
  public BorderPane borderPane;
  @FXML
  private Button solveButton;
  @FXML
  private TreeView<String> treeView;
  @FXML
  private SplitPane horizontalSplitPane;
  @FXML
  private ListView<String> listView;
  @FXML
  private StackPane puzzlePane;
  @FXML
  private GridPane puzzleGrid;
  @FXML
  private GridPane solutionGrid;
  @FXML
  private FlowPane flowPane;
  @FXML
  private VBox vBox;
  @FXML
  private MenuItem saveMenuItem;
  private Stage stage;
  private ButtonBar solveBox;

  private String path;
  private Thread openingThread;
  private Puzzle solution;
  private List<PuzzleState> steps;
  private int[][] geometry;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    setupDriver();
    bindCells(0);
    bindCells(1);
    bindClues();
    bindCandidates();
    setResize();
    try {
      path = getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
      int index = path.lastIndexOf(File.separator);
      if(index > -1)
        path = path.substring(0, index + 1);
      else
        path = System.getProperty("user.home");
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
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

  private void bindCells(int gridNum) {
    GridPane gridPane;
    ObservableList<StringProperty> letters;
    ObservableList<StringProperty> numbers;
    ObservableList<StringProperty> colors;
    if (gridNum == 0) {
      gridPane = solutionGrid;
      letters = solutionGridLetters;
      numbers = solutionGridNumbers;
      colors = solutionGridColors;
    } else if (gridNum == 1) {
      gridPane = puzzleGrid;
      letters = puzzleGridLetters;
      numbers = puzzleGridNumbers;
      colors = puzzleGridColors;
    } else {
      return;
    }
    try {
      for (int i = 0; i < 5; i++) {
        for (int j = 0; j < 5; j++) {
          FXMLLoader loader = new FXMLLoader();
          loader.setLocation(Main.class.getResource("CellGrid.fxml"));
          GridPane cellGrid = loader.load();

          StringProperty letter = new SimpleStringProperty();
          ((Label) cellGrid.getChildren().get(0)).textProperty().bind(letter);
          StringProperty number = new SimpleStringProperty();
          ((Label) cellGrid.getChildren().get(1)).textProperty().bind(number);

          StringProperty color = new SimpleStringProperty();
          color.addListener((observable, oldValue, newValue) -> {
            switch (newValue) {
              case "BLACK":
                cellGrid.setBackground(new Background(
                    new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, new Insets(1, 1, 1, 1))));
                break;
              case "CHOSEN":
                cellGrid.setBackground(new Background(
                    new BackgroundFill(Color.rgb(167, 216, 255), CornerRadii.EMPTY,
                        new Insets(1, 1, 1, 1))));
                break;
              default:
                cellGrid.setBackground(new Background(
                    new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, new Insets(1, 1, 1, 1))));
                break;
            }
          });
          color.set("");
          gridPane.add(cellGrid, j, i);
          letters.add(letter);
          numbers.add(number);
          colors.add(color);
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
    BooleanProperty flag = new SimpleBooleanProperty(false);
    chosenIndex.addListener((observable, oldValue, newValue) -> {
      if (!flag.get()) {
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
                      flag.set(true);
                      treeView.getSelectionModel().select(item);
                      flag.set(false);
                    }
                  }
                }
              }
            }
          }
        }
        showColors();
      }
    });

    treeView.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (geometry != null && newValue != null && !newValue.getParent()
              .equals(treeView.getRoot()) && newValue.getValue().length() > 0) {
            char c = newValue.getValue().charAt(0);
            if (Character.isDigit(c)) {
              int num = Character.getNumericValue(c);
              if (newValue.getParent().getValue().equals("Down")) {
                num = -num;
              }
              chosenIndex.set(num);
            }
          }
        });
  }

  private void bindCandidates() {
    listView.itemsProperty().bind(candidates);
  }

  private void setResize() {
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

  @FXML
  public void solveAction() {
    BooleanProperty isCancelled = new SimpleBooleanProperty(false);

    Task<Boolean> task = new Task<Boolean>() {
      @Override
      protected Boolean call() throws Exception {
        try {
          Puzzle solvablePuzzle = SerializationUtils.clone(solution);
          solvablePuzzle.changeToSolvable();

          PuzzleState puzzleState = new PuzzleState(solvablePuzzle);
          steps = new ArrayList<>();
          steps.add(new PuzzleState(puzzleState));
          while (puzzleState.next()) {
            if (isCancelled.get()) {
              return false;
            }
            steps.add(new PuzzleState(puzzleState));
          }
          return true;
        } catch (Exception e) {
          e.printStackTrace();
          return false;
        }
      }
    };

    final Alert alert = new Alert(AlertType.INFORMATION);
    alert.getButtonTypes().clear();
    alert.getButtonTypes().add(ButtonType.CANCEL);
    alert.setTitle("Solving Puzzle");
    alert.setHeaderText("Solving Puzzle");
    final Button cancel = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
    cancel.addEventFilter(ActionEvent.ACTION, event -> isCancelled.set(true));
    alert.show();

    task.setOnSucceeded(Event -> {
      alert.getButtonTypes().clear();
      alert.getButtonTypes().add(ButtonType.OK);
      if (task.getValue()) {
        alert.close();
        try {
          FXMLLoader loader = new FXMLLoader();
          loader.setLocation(Main.class.getResource("ButtonBar.fxml"));
          ButtonBar buttonBar = loader.load();
          ButtonBarController buttonBarController = loader.getController();

          solveBox = (ButtonBar) vBox.getChildren().remove(0);
          vBox.getChildren().add(0, buttonBar);

          buttonBarController.setLast(steps.size());
          buttonBarController.getPageNumber().textProperty().addListener(
              (observable, oldValue, newValue) -> showStep(Integer.parseInt(newValue) - 1));
          buttonBarController.getPageNumber().textProperty().setValue("1");
          System.out.println("Puzzle is solved");
        } catch (IOException e) {
          e.printStackTrace();
          alert.setAlertType(AlertType.ERROR);
          alert.setTitle("Error");
          alert.setHeaderText("Error");
          alert.setContentText("GUI component could not be loaded");
          alert.show();
          System.out.println("ButtonBar.fxml could not be loaded");
        }
      } else {
        alert.setAlertType(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error");
        alert.setContentText("Puzzle could not be solved");
        alert.show();
        System.out.println("Puzzle could not be solved");
      }
    });

    Thread thread = new Thread(task);
    thread.start();
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
          updateMessage("Closing driver...");
          Puzzle.closeDriver(); if(Thread.interrupted()) return -1;
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
        saveAction();
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

  @FXML
  public void exitAction() {
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
      System.exit(0);
    }
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

  private void showStep(int stepNum) {
    showCells(steps.get(stepNum).getPuzzle(), 1);
    showColors(steps.get(stepNum));
    candidates.set(FXCollections.observableArrayList(steps.get(stepNum).getCandidates()));
  }

  private void showInitialPuzzle() {
    if (solveBox != null) {
      vBox.getChildren().remove(0);
      vBox.getChildren().add(0, solveBox);
      solveBox = null;
    }
    showCells(null, 0);
    showCells(null, 1);
    showCells(solution, 0);
    showCells(solution, 1, false);
    showColors(solution);
    showClues();
    saveMenuItem.setDisable(false);
    solveButton.setDisable(false);
    candidates.set(null);
    treeView.getRoot().getChildren().get(0).setValue(solution.getDateFormatted());
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
          if (letter != '-' && showLetters) {
            letters.get(i * 5 + j).set(letter + "");
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

  private void showColors(PuzzleState puzzleState) {
    geometry = puzzleState.getPuzzle().getGeometry();
    chosenIndex.set(puzzleState.getChosenIndex());
  }

  private void showColors(Puzzle puzzle) {
    geometry = puzzle.getGeometry();
    chosenIndex.set(0);
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
          puzzleGridColors.get(i * 5 + j).set("CHOSEN");
          solutionGridColors.get(i * 5 + j).set("CHOSEN");
        } else {
          puzzleGridColors.get(i * 5 + j).set("");
          solutionGridColors.get(i * 5 + j).set("");
        }
      }
    }
  }

  void setStage(Stage stage) {
    this.stage = stage;
    stage.setOnCloseRequest(e -> {
      e.consume();
      exitAction();
    });
  }
}