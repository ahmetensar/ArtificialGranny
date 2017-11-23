package cs461.artificialgranny;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

class Puzzle implements Serializable {

  private static final int SIZE = 5;
  private static final String URL_PUZZLE = "https://nytimes.com/crosswords/game/mini";
  private static final Pattern DATE_REGEX = Pattern.compile("<date>(.+?)</date>");
  private static final Pattern CLUE_REGEX = Pattern.compile("<clue>(.+?)</clue>");
  private static final Pattern SQUARE_REGEX = Pattern.compile("<square>(.+?)</square>");
  private static final Pattern RELATED_REGEX = Pattern.compile("<related>(.+?)</related>");
  private static WebDriver driver;
  private String date;
  private char[][] letters;
  private int[][] geometry;
  private Map<Integer, String> clues;
  private Map<Integer, int[]> wordPositions;
  private Multimap<Integer, Integer> related;

  void savePuzzleToFile(File file) throws IOException {
    StringBuilder content = new StringBuilder();

    content.append("<date>").append(date).append("</date>\n");
    clues.forEach((key, value) -> content.append("<clue>").append(key)
        .append(",").append(value).append("</clue>\n")
    );

    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 5; j++) {
        content.append("<square>").append(geometry[i][j]).append(",")
            .append(letters[i][j]).append("</square>\n");
      }
    }

    if (related != null) {
      for (int i : related.keySet()) {
        content.append("<related>").append(i);
        for (int j : related.get(i)) {
          content.append(",").append(j);
        }
        content.append("</related>\n");
      }

    }

    // save to file
    FileWriter fileWriter = new FileWriter(file);
    fileWriter.write(content.toString());
    fileWriter.close();
  }

  void loadPuzzleFromFile(File file) throws IOException {
    String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));

    date = "";
    final Matcher dateMatcher = DATE_REGEX.matcher(content);
    if (dateMatcher.find()) {
      date = dateMatcher.group(1);
    }

    int count = 0;
    geometry = new int[SIZE][SIZE];
    letters = new char[SIZE][SIZE];
    final Matcher squareMatcher = SQUARE_REGEX.matcher(content);
    while (squareMatcher.find()) {
      String[] entry = squareMatcher.group(1).split(",");
      geometry[count / 5][count % 5] = Integer.parseInt(entry[0]);
      letters[count / 5][count % 5] = entry[1].charAt(0);
      count++;
    }

    clues = new LinkedHashMap<>();
    final Matcher acrossMatcher = CLUE_REGEX.matcher(content);
    while (acrossMatcher.find()) {
      String entry = acrossMatcher.group(1);
      int split = entry.indexOf(',');
      clues.put(Integer.parseInt(entry.substring(0, split)), entry.substring(split + 1));
    }

    final Matcher relatedMatcher = RELATED_REGEX.matcher(content);
    related = null;
    while (relatedMatcher.find()) {
      if (related == null) {
        related = HashMultimap.create();
      }
      String entry[] = relatedMatcher.group(1).split(",");
      for (int i = 1; i < entry.length; i++) {
        related.put(Integer.parseInt(entry[0]), Integer.parseInt(entry[i]));
      }
    }
    loadWordPositions();
  }

  void emptyPuzzle() {
    date = null;
    geometry = null;
    letters = null;
    clues = null;
    related = null;
    wordPositions = null;
  }

  void loadWordPositions() {
    wordPositions = new HashMap<>();
    for (int num : clues.keySet()) {
      int[] pos = new int[3];

      for (int i = 0; i < SIZE; i++) {
        for (int j = 0; j < SIZE; j++) {
          if (num > 0 ? geometry[i][j] == num : geometry[i][j] == -num) {
            pos[0] = i; // x coordinate
            pos[1] = j; // y coordinate
            i = SIZE;
            j = SIZE;
          }
        }
      }
      if (num < 0) {
        for (int i = pos[0]; i < SIZE; i++) {
          if (geometry[i][pos[1]] < 0) {
            pos[2] = i - pos[0];
            i = SIZE;
          } else if (i == SIZE - 1) {
            pos[2] = i - pos[0] + 1;
          }
        }
      } else {
        for (int j = pos[1]; j < SIZE; j++) {
          if (geometry[pos[0]][j] < 0) {
            pos[2] = j - pos[1];
            j = SIZE;
          } else if (j == SIZE - 1) {
            pos[2] = j - pos[1] + 1;
          }
        }
      }
      wordPositions.put(num, pos);
    }
  }

  static boolean isDriverNull() {
    return driver == null;
  }

  static void setupDriver() {
    ChromeDriverManager.getInstance().setup();
  }

  static void openDriver() {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--disable-remote-fonts");
    options.addArguments("--mute-audio");
    options.addArguments("--headless");
    options.addArguments("--disable-gpu");

    driver = new ChromeDriver(options);
  }

  static void openPage() {
    driver.get(URL_PUZZLE);
  }

  static void clickButtons() {
    // wait until OK button is ready
    (new WebDriverWait(driver, 10))
        .until(ExpectedConditions
            .presenceOfElementLocated(
                By.cssSelector("button[class^='ModalBody-button--']>div>span")));

    // click buttons
    driver.findElement(By.cssSelector("button[class^='ModalBody-button--']>div>span")).click();
    driver.findElement(By.cssSelector("div[class^='Toolbar-expandedMenu--']> li:nth-child(2) > "
        + "button")).click();
    driver.findElement(By.cssSelector("div[class^='Toolbar-expandedMenu--']> li:nth-child(2) > "
        + "button + ul>li:nth-child(3)>a")).click();
    driver.findElement(By.cssSelector("div[class^='ModalBody-buttonContainer--']>:nth-child(2)"))
        .click();
    driver.findElement(By.cssSelector("div[class^='ModalBody-closeX--']>a")).click();
  }

  void loadDateFromDriver() {
    WebElement element = driver.findElement(By.cssSelector("div[class^='PuzzleDetails-date--']"));
    SimpleDateFormat sdf = new SimpleDateFormat("MMMMM dd, yyyy", Locale.US);
    date = "";
    try {
      Date d = sdf.parse(element.getText().split(" ", 2)[1]);
      sdf.applyPattern("dd_MM_YYYY");
      date = sdf.format(d);
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

  void loadSquaresFromDriver() {
    List<WebElement> isEmptyList = driver.findElement(By.cssSelector("g[data-group='cells']"))
        .findElements(By.cssSelector("rect"));

    if (isEmptyList.size() != SIZE * SIZE) {
      driver.quit();
      throw new IllegalStateException();
    }

    geometry = new int[SIZE][SIZE];
    letters = new char[SIZE][SIZE];
    for (int i = 0; i < SIZE; i++) {
      for (int j = 0; j < SIZE; j++) {
        if (isEmptyList.get(i * SIZE + j).getAttribute("fill").equals("black")) {
          geometry[i][j] = -1;
          letters[i][j] = '-';
        }
      }
    }

    List<WebElement> squareList = driver.findElement(By.cssSelector("g[data-group='cells']"))
        .findElements(By.tagName("text"));

    int squareListIndex = 0;
    for (int i = 0; i < SIZE; i++) {
      for (int j = 0; j < SIZE; j++) {
        if (geometry[i][j] != -1) {
          if (squareList.get(squareListIndex).getAttribute("text-anchor").equals("start")) {
            geometry[i][j] = Integer.parseInt(squareList.get(squareListIndex).getText());
            squareListIndex++;
          }
          letters[i][j] = squareList.get(squareListIndex).getText().charAt(0);
          squareListIndex++;
        }
      }
    }
  }

  void loadCluesFromDriver() {
    clues = new LinkedHashMap<>();
    for (int i = 1; i <= 2; i++) {
      for (int j = 1; j <= 5; j++) {
        WebElement element = driver.findElement(
            By.cssSelector("section[class^='Layout-clueLists--']> div:nth-child(" + i
                + ")> ol> li:nth-child(" + j + ")"
            )
        );
        int num = Integer.parseInt(
            element.findElement(By.cssSelector("span[class^='Clue-label--']")).getText()
        );
        if (i == 2) {
          num = -num; // down clues
        }

        clues.put(num,
            element.findElement(By.cssSelector("span[class^='Clue-text--']")).getText());

        element.click();

        if (driver.findElements(By.cssSelector("li[class*='Clue-related']")).size() > 0) {
          if (related == null) {
            related = HashMultimap.create();
          }
          for (int k = 1; k <= 2; k++) {
            List<WebElement> relatedClues = driver.findElements(
                By.cssSelector("section[class^='Layout-clueLists--']> div:nth-child(" + k
                    + ")> ol>li[class*='Clue-related']"
                )
            );
            for (WebElement relatedClue : relatedClues) {
              int relatedNum = Integer.parseInt(
                  relatedClue.findElement(By.cssSelector("span[class^='Clue-label--']")).getText()
              );
              if (k == 2) {
                relatedNum = -relatedNum;
              }
              related.put(num, relatedNum);
            }
          }
        }
      }
    }
  }

  static void closeDriver() {
    driver.quit();
  }

  void changeToSolvable() {
    letters = new char[SIZE][SIZE];
    for (int i = 0; i < SIZE; i++) {
      for (int j = 0; j < SIZE; j++) {
        if (geometry[i][j] == -1) {
          letters[i][j] = '-';
        } else {
          letters[i][j] = ' ';
        }
      }
    }
  }

  @Override
  public String toString() {
    if (date == null) {
      return "";
    }

    StringBuilder sb = new StringBuilder();

    sb.append(date).append("\n");

    clues.forEach((key, value) -> {
      if (key > 0) {
        if (key == 1) {
          sb.append("\nAcross:\n");
        }
        sb.append(key).append(": ").append(value).append("\n");
      } else {
        if (key == -1) {
          sb.append("\nDown:\n");
        }
        sb.append(-key).append(": ").append(value).append("\n");
      }
    });

    sb.append("\nSquares:\n");
    for (int i = 0; i < SIZE; i++) {
      for (int j = 0; j < SIZE; j++) {
        if (geometry[i][j] > 0) {
          sb.append("(").append(geometry[i][j]).append(")");
        } else {
          sb.append("   ");
        }
        sb.append(letters[i][j]).append("     ");
      }
      sb.append("\n");
    }

    if (related != null) {
      sb.append("\nRelated:\n");
      sb.append(related);
    }

    return sb.toString();
  }

  String getDate() {
    return date;
  }

  String getDateFormatted() {
    try {
      DateFormat fromFormat = new SimpleDateFormat("dd_MM_yyyy");
      fromFormat.setLenient(false);
      DateFormat toFormat = new SimpleDateFormat("dd/MM/yyyy");
      toFormat.setLenient(false);
      Date dateObject = fromFormat.parse(date);
      return toFormat.format(dateObject);
    } catch (ParseException e) {
      e.printStackTrace();
      return "";
    }
  }

  int[][] getGeometry() {
    return geometry;
  }

  char[][] getLetters() {
    return letters;
  }

  Map<Integer, String> getClues() {
    return clues;
  }

  Multimap<Integer, Integer> getRelated() {
    return related;
  }

  Map<Integer, int[]> getWordPositions() {
    return wordPositions;
  }

  void setPuzzleSquare(int x, int y, char letter) {
    letters[x][y] = letter;
  }
}
