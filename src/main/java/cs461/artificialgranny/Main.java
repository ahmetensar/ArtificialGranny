package cs461.artificialgranny;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

  private Stage primaryStage;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    this.primaryStage = primaryStage;
    this.primaryStage.setTitle("Artificial Granny");
    this.primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("project_icon.png")));
    initRootLayout();
  }

  /**
   * Initializes the root layout.
   */
  private void initRootLayout() {
    try {
      // Load root layout from fxml file.
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(getClass().getResource("PuzzleLayout.fxml"));
      BorderPane rootLayout = loader.load();
      PuzzleLayoutController puzzleLayoutController = loader.getController();
      puzzleLayoutController.setStage(primaryStage);


      // Show the scene containing the root layout.
      Scene scene = new Scene(rootLayout);
      scene.getStylesheets().add(getClass().getResource("Stylesheet.css").toExternalForm());
      primaryStage.setScene(scene);
      primaryStage.show();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}