package cs461.artificialgranny;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class CellGridController {

  private final DoubleProperty letterSize = new SimpleDoubleProperty(15);
  private final DoubleProperty numberSize = new SimpleDoubleProperty(10);
  @FXML
  private GridPane cellGrid;
  @FXML
  private Label letter;
  @FXML
  private Label number;

  @FXML
  public void initialize() {
    letterSize.bind(cellGrid.widthProperty().divide(2));
    letter.styleProperty().bind(Bindings.concat("-fx-font-size: ", letterSize.asString(), ";"));
    numberSize.bind(cellGrid.widthProperty().divide(4));
    number.styleProperty().bind(Bindings.concat("-fx-font-size: ", numberSize.asString(), ";"));
  }
}