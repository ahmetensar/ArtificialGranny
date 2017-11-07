package cs461.artificialgranny;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import cs461.artificialgranny.model.PuzzleState;
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

public class PuzzleManager {

  public static final int PUZZLE_SIZE = 5;
  private static final String URL_PUZZLE = "https://nytimes.com/crosswords/game/mini";
  private static final Pattern DATE_REGEX = Pattern.compile("<date>(.+?)</date>");
  private static final Pattern CLUE_REGEX = Pattern.compile("<clue>(.+?)</clue>");
  private static final Pattern SQUARE_REGEX = Pattern.compile("<square>(.+?)</square>");
  private static final Pattern RELATED_REGEX = Pattern.compile("<related>(.+?)</related>");

  private PuzzleState puzzleState;

  PuzzleManager() {
    loadPuzzleFromWeb();
  }

  PuzzleManager(File file) {
    loadPuzzleFromFile(file);
  }

  PuzzleState getPuzzleState() {
    return puzzleState;
  }

  PuzzleState getSolvableState() {
    char[][] letters = new char[PUZZLE_SIZE][PUZZLE_SIZE];
    for (int i = 0; i < PUZZLE_SIZE; i++) {
      for (int j = 0; j < PUZZLE_SIZE; j++) {
        if (puzzleState.getGeometry()[i][j] == -1) {
          letters[i][j] = '-';
        } else {
          letters[i][j] = '?';
        }
      }
    }
    return new PuzzleState(puzzleState.getDate(), puzzleState.getGeometry(),
        letters, puzzleState.getClues(), puzzleState.getRelated());
  }

  void savePuzzleToFile(String path) {
    StringBuilder content = new StringBuilder();
    content.append("<date>").append(puzzleState.getDate()).append("</date>\n");
    puzzleState.getClues().forEach((key, value) -> content.append("<clue>").append(key)
        .append(",").append(value).append("</clue>\n")
    );

    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 5; j++) {
        content.append("<square>").append(puzzleState.getGeometry()[i][j]).append(",")
            .append(puzzleState.getLetters()[i][j]).append("</square>\n");
      }
    }

    if (puzzleState.getRelated() != null) {
      for (int i : puzzleState.getRelated().keySet()) {
        content.append("<related>").append(i);
        for (int j : puzzleState.getRelated().get(i)) {
          content.append(",").append(j);
        }
        content.append("</related>\n");
      }

    }

    // save to file
    try {
      Files.write(Paths.get(path),
          content.toString().getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println("Puzzle is saved to file");
    System.out.println();
  }

  private void loadPuzzleFromFile(File file) {
    try {
      String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));

      String date = "";
      final Matcher dateMatcher = DATE_REGEX.matcher(content);
      if (dateMatcher.find()) {
        date = dateMatcher.group(1);
      }

      int count = 0;
      int[][] geometry = new int[PUZZLE_SIZE][PUZZLE_SIZE];
      char[][] letters = new char[PUZZLE_SIZE][PUZZLE_SIZE];
      final Matcher squareMatcher = SQUARE_REGEX.matcher(content);
      while (squareMatcher.find()) {
        String[] entry = squareMatcher.group(1).split(",");
        geometry[count / 5][count % 5] = Integer.parseInt(entry[0]);
        letters[count / 5][count % 5] = entry[1].charAt(0);
        count++;
      }

      Map<Integer, String> clues = new LinkedHashMap<>();
      final Matcher acrossMatcher = CLUE_REGEX.matcher(content);
      while (acrossMatcher.find()) {
        String entry = acrossMatcher.group(1);
        int split = entry.indexOf(',');
        clues.put(Integer.parseInt(entry.substring(0, split)), entry.substring(split + 1));
      }

      final Matcher relatedMatcher = RELATED_REGEX.matcher(content);
      Multimap<Integer, Integer> related = null;
      while (relatedMatcher.find()) {
        if (related == null) {
          related = HashMultimap.create();
        }
        String entry[] = relatedMatcher.group(1).split(",");
        for (int i = 1; i < entry.length; i++) {
          System.out.println(entry[0]);
          System.out.println(entry[i]);
          related.put(Integer.parseInt(entry[0]), Integer.parseInt(entry[i]));
        }
      }

      puzzleState = new PuzzleState(date, geometry, letters, clues, related);

      System.out.println("Puzzle is loaded from file");
      System.out.println();

    } catch (IOException e) {
      System.out.println("Puzzle is not found!");
      System.out.println();
    }
  }

  private void loadPuzzleFromWeb() {
    ChromeDriverManager.getInstance().setup();
    System.out.println();

    ChromeOptions options = new ChromeOptions();
    options.addArguments("--disable-remote-fonts");
    options.addArguments("--mute-audio");
    options.addArguments("--headless");
    options.addArguments("--disable-gpu");

    System.out.println();

    Instant start = Instant.now();
    WebDriver driver = new ChromeDriver(options);

    System.out.println();
    System.out.println("Driver is opened");
    System.out.println();
    System.out.println("Page is loading...");

    driver.get(URL_PUZZLE);

    System.out.println("Page is loaded");
    System.out.println();

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

    // date
    WebElement element = driver.findElement(By.cssSelector("div[class^='PuzzleDetails-date--']"));
    SimpleDateFormat sdf = new SimpleDateFormat("MMMMM dd, yyyy", Locale.US);
    String date = "";
    try {
      Date d = sdf.parse(element.getText().split(" ", 2)[1]);
      sdf.applyPattern("dd_MM_YYYY");
      date = sdf.format(d);
    } catch (ParseException e) {
      e.printStackTrace();
    }

    // geometry and letters
    List<WebElement> isEmptyList = driver.findElement(By.cssSelector("g[data-group='cells']"))
        .findElements(By.cssSelector("rect"));

    if (isEmptyList.size() != PUZZLE_SIZE * PUZZLE_SIZE) {
      driver.quit();
      System.out.println("Wrong puzzle: puzzle size is " + (int) Math.sqrt(isEmptyList.size()));
      throw new IllegalStateException();
    }

    int[][] geometry = new int[PUZZLE_SIZE][PUZZLE_SIZE];
    char[][] letters = new char[PUZZLE_SIZE][PUZZLE_SIZE];
    for (int i = 0; i < PUZZLE_SIZE; i++) {
      for (int j = 0; j < PUZZLE_SIZE; j++) {
        if (isEmptyList.get(i * PUZZLE_SIZE + j).getAttribute("fill").equals("black")) {
          geometry[i][j] = -1;
          letters[i][j] = '-';
        }
      }
    }

    List<WebElement> squareList = driver.findElement(By.cssSelector("g[data-group='cells']"))
        .findElements(By.tagName("text"));

    int squareListIndex = 0;
    for (int i = 0; i < PUZZLE_SIZE; i++) {
      for (int j = 0; j < PUZZLE_SIZE; j++) {
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

    // clues and related
    Map<Integer, String> clues = new LinkedHashMap<>();
    Multimap<Integer, Integer> related = null;
    for (int i = 1; i <= 2; i++) {
      for (int j = 1; j <= 5; j++) {
        element = driver.findElement(
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

    puzzleState = new PuzzleState(date, geometry, letters, clues, related);

    driver.quit();
    Instant end = Instant.now();

    System.out.println("Driver is closed");
    System.out.format("Driver time: %ds\n\n", Duration.between(start, end).getSeconds());
  }

}