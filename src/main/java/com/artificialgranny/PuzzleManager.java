package com.artificialgranny;

import io.github.bonigarcia.wdm.ChromeDriverManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

class PuzzleManager {

  private static final String URL_PUZZLE = "https://nytimes.com/crosswords/game/mini";
  private static final int PUZZLE_SIZE = 5;
  private static final Pattern DATE_REGEX = Pattern.compile("<date>(.+?)</date>");
  private static final Pattern ACROSS_REGEX = Pattern.compile("<across>(.+?)</across>");
  private static final Pattern DOWN_REGEX = Pattern.compile("<down>(.+?)</down>");
  private static final Pattern SQUARE_REGEX = Pattern.compile("<square>(.+?)</square>");

  private WebDriver driver;
  private Map<Integer, String> acrossClues;
  private Map<Integer, String> downClues;
  private int[][] geometry = new int[PUZZLE_SIZE][PUZZLE_SIZE];
  private char[][] letters = new char[PUZZLE_SIZE][PUZZLE_SIZE];
  private String date;

  PuzzleManager() {
  }

  String getDate() {
    return date;
  }

  int[][] getGeometry() {
    return geometry;
  }

  boolean checkSquare(int x, int y, char c) {
    return letters[x][y] == Character.toUpperCase(c);
  }

  boolean checkWord(int lineNumber, boolean isAcross, char[] word) {
    if (word.length != PUZZLE_SIZE) {
      return false;
    }
    if (isAcross) {
      for (int i = 0; i < PUZZLE_SIZE; i++) {
        if (!checkSquare(lineNumber, i, word[i])) {
          return false;
        }
      }
    } else {
      for (int i = 0; i < PUZZLE_SIZE; i++) {
        if (!checkSquare(i, lineNumber, word[i])) {
          return false;
        }
      }
    }
    return true;
  }

  boolean checkPuzzle(char[][] puzzle) {
    if (puzzle.length != PUZZLE_SIZE) {
      return false;
    }
    for (int i = 0; i < PUZZLE_SIZE; i++) {
      if (!checkWord(i, true, puzzle[i])) {
        return false;
      }
    }
    return true;
  }

  char revealSquare(int x, int y) {
    return letters[x][y];
  }

  char[] revealWord(int lineNumber, boolean isAcross) {
    char[] word = new char[PUZZLE_SIZE];
    if (isAcross) {
      for (int i = 0; i < PUZZLE_SIZE; i++) {
        word[i] = revealSquare(lineNumber, i);
      }
    } else {
      for (int i = 0; i < PUZZLE_SIZE; i++) {
        word[i] = revealSquare(i, lineNumber);
      }
    }
    return word;
  }

  char[][] revealPuzzle() {
    char[][] puzzle = new char[PUZZLE_SIZE][];
    for (int i = 0; i < PUZZLE_SIZE; i++) {
      puzzle[i] = revealWord(i, true);
    }
    return puzzle;
  }

  @Override
  public String toString() {
    if (date == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();

    sb.append(date).append("\n");

    sb.append("\nAcross:\n");
    acrossClues.forEach((key, value) -> sb.append(key).append(": ").
        append(value).append("\n"));

    sb.append("\nDown:\n");
    downClues.forEach((key, value) -> sb.append(key).append(": ").
        append(value).append("\n"));

    sb.append("\nSquares:\n");
    for (int i = 0; i < PUZZLE_SIZE; i++) {
      for (int j = 0; j < PUZZLE_SIZE; j++) {
        if (geometry[i][j] > 0) {
          sb.append("(").append(geometry[i][j]).append(")");
        } else {
          sb.append("   ");
        }
        sb.append(letters[i][j]).append("     ");
      }
      sb.append("\n");
    }

    return sb.toString();
  }

  void loadPuzzleFromWeb() {

    ChromeDriverManager.getInstance().setup();

    ChromeOptions options = new ChromeOptions();
    options.addArguments("--disable-remote-fonts");
    options.addArguments("--mute-audio");
    options.addArguments("--headless");

    System.out.println("Starting driver");

    Instant start = Instant.now();
    driver = new ChromeDriver(options);

    System.out.println("Driver is opened");
    System.out.println();
    System.out.println("Page is loading...");

    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    driver.get(URL_PUZZLE);

    System.out.println("Page is loaded");
    System.out.println();

    (new WebDriverWait(driver, 10))
        .until(ExpectedConditions
            .presenceOfElementLocated(
                By.cssSelector("button[class^='ModalBody-button--']>div>span")));
    driver.findElement(By.cssSelector("button[class^='ModalBody-button--']>div>span")).click();

    loadSquares();
    loadDate();
    loadClues(1, acrossClues = new HashMap<>());
    loadClues(2, downClues = new HashMap<>());

    driver.close();
    Instant end = Instant.now();

    System.out.println("Driver is closed");
    System.out.format("Driver time: %ds\n\n", Duration.between(start, end).getSeconds());

    savePuzzleToFile();
  }

  void loadPuzzleFromFile(File file) {
    try {
      String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));

      final Matcher dateMatcher = DATE_REGEX.matcher(content);
      if (dateMatcher.find()) {
        date = dateMatcher.group(1);
      }

      acrossClues = new HashMap<>();
      final Matcher acrossMatcher = ACROSS_REGEX.matcher(content);
      while (acrossMatcher.find()) {
        String[] entry = acrossMatcher.group(1).split("<split>");
        acrossClues.put(Integer.parseInt(entry[0]), entry[1]);
      }

      downClues = new HashMap<>();
      final Matcher downMatcher = DOWN_REGEX.matcher(content);
      while (downMatcher.find()) {
        String[] entry = downMatcher.group(1).split("<split>");
        downClues.put(Integer.parseInt(entry[0]), entry[1]);
      }
      int count = 0;
      final Matcher squareMatcher = SQUARE_REGEX.matcher(content);
      while (squareMatcher.find()) {
        String[] entry = squareMatcher.group(1).split("<split>");
        geometry[count / 5][count % 5] = Integer.parseInt(entry[0]);
        letters[count / 5][count % 5] = entry[1].charAt(0);
        count++;
      }

      System.out.println("Puzzle is loaded from file");
      System.out.println();

    } catch (IOException e) {
      System.out.println("Puzzle is not found!");
      System.out.println();
    }
  }

  private void savePuzzleToFile() {
    StringBuilder content = new StringBuilder();
    content.append("<date>").append(date).append("</date>\n");
    acrossClues.forEach((key, value) -> content.append("<across>").append(key)
        .append("<split>").append(value).append("</across>\n"));
    downClues.forEach((key, value) -> content.append("<down>").append(key)
        .append("<split>").append(value).append("</down>\n"));
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 5; j++) {
        content.append("<square>").append(geometry[i][j]).append("<split>")
            .append(letters[i][j]).append("</square>\n");
      }
    }

    // save to file
    try {
      Files.write(Paths.get("puzzles" + File.separator + date + ".puzzle"),
          content.toString().getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println("Puzzle is saved to file");
    System.out.println();
  }

  private void loadClues(int listNum, Map<Integer, String> map) {
    for (int i = 0; i < 5; i++) {
      String str = "section[class^='Layout-clueLists--']> div:nth-child(" + listNum
          + ")> ol> li:nth-child(" + (i + 1) + ")>";
      map.put(
          Integer.parseInt(
              driver.findElement(By.cssSelector(str + "span[class^='Clue-label--']")).getText()),
          driver.findElement(By.cssSelector(str + "span[class^='Clue-text--']")).getText()
      );
    }
  }

  private void loadSquares() {
    driver.findElement(By.cssSelector("div[class^='Toolbar-expandedMenu--']> li:nth-child(2) > "
        + "button")).click();
    driver.findElement(By.cssSelector("div[class^='Toolbar-expandedMenu--']> li:nth-child(2) > "
        + "button + ul>li:nth-child(3)>a")).click();
    driver.findElement(By.cssSelector("div[class^='ModalBody-buttonContainer--']>:nth-child(2)"))
        .click();
    driver.findElement(By.cssSelector("div[class^='ModalBody-closeX--']>a")).click();
    List<WebElement> isEmptyList = driver.findElement(By.cssSelector("g[data-group='cells']"))
        .findElements(By.cssSelector("rect"));
    if (isEmptyList.size() != PUZZLE_SIZE * PUZZLE_SIZE) {
      driver.close();
      System.out.println("Wrong puzzle: puzzle size is " + (int) Math.sqrt(isEmptyList.size()));
      throw new IllegalStateException();
    }
    for (int i = 0; i < PUZZLE_SIZE; i++) {
      for (int j = 0; j < PUZZLE_SIZE; j++) {
        if (isEmptyList.get(i * PUZZLE_SIZE + j).getAttribute("fill").equals("black")) {
          geometry[i][j] = -1;
          letters[i][j] = '-';
        } else {
          geometry[i][j] = 0;
        }
      }
    }

    List<WebElement> elements = driver.findElement(By.cssSelector("g[data-group='cells']"))
        .findElements(By.tagName("text"));

    int elementsIndex = 0;
    for (int i = 0; i < PUZZLE_SIZE; i++) {
      for (int j = 0; j < PUZZLE_SIZE; j++) {
        if (geometry[i][j] != -1) {
          if (elements.get(elementsIndex).getAttribute("text-anchor").equals("start")) {
            geometry[i][j] = Integer.parseInt(elements.get(elementsIndex).getText());
            elementsIndex++;
          }
          letters[i][j] = elements.get(elementsIndex).getText().charAt(0);
          elementsIndex++;
        }
      }
    }
  }


  private void loadDate() {
    WebElement element = driver.findElement(By.cssSelector("div[class^='PuzzleDetails-date--']"));
    SimpleDateFormat sdf = new SimpleDateFormat("MMMMM dd, yyyy", Locale.US);
    try {
      Date date = sdf.parse(element.getText().split(" ", 2)[1]);
      sdf.applyPattern("dd_MM_YYYY");
      this.date = sdf.format(date);
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }
}