package cs461.artificialgranny;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ButtonBarController {

  @FXML
  private Label pageNumber;

  private int last;

  void setLast(int last) {
    this.last = last;
  }

  Label getPageNumber() {
    return pageNumber;
  }

  public void firstButtonAction() {
    pageNumber.setText("1");
  }

  public void previousButtonAction() {
    int value = Integer.parseInt(pageNumber.textProperty().getValue());
    if (value > 1) {
      pageNumber.setText((value - 1) + "");
    }
  }

  public void nextButtonAction() {
    int value = Integer.parseInt(pageNumber.textProperty().getValue());
    if (value < last) {
      pageNumber.setText((value + 1) + "");
    }
  }

  public void lastButtonAction() {
    pageNumber.setText(last + "");
  }
}
