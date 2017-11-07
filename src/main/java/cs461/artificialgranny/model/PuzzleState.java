package cs461.artificialgranny.model;

import static cs461.artificialgranny.PuzzleManager.PUZZLE_SIZE;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PuzzleState {

  private String date;
  private int[][] geometry;
  private char[][] letters;
  private Map<Integer, String> clues;
  private Multimap<Integer, Integer> related;

  private Map<Integer, int[]> wordPositions;

  private String[] candidates;
  private int chosenCandidateIndex;
  private int[] chosenPos;

  private List<Integer> questions;
  private int index;

  public PuzzleState(String date, int[][] geometry, char[][] letters,
      Map<Integer, String> clues, Multimap<Integer, Integer> related) {
    this.date = date;
    this.geometry = geometry;
    this.letters = letters;
    this.clues = clues;
    this.related = related;
    loadWordPositions();
  }

  public PuzzleState(PuzzleState puzzleState) {
    this(puzzleState.date, puzzleState.geometry, puzzleState.letters, puzzleState.clues,
        puzzleState.related);
  }

  public void step() {
    if (questions == null) {
      questions = new ArrayList<>(clues.keySet());
      index = 0;
    }
    if (index == questions.size()) {
      index = 0;
    }

    solve();
    index++;
  }

  public boolean isFull() {
    if (questions != null && index != questions.size()) {
      return false;
    }
    for (int i = 0; i < PUZZLE_SIZE; i++) {
      for (int j = 0; j < PUZZLE_SIZE; j++) {
        if (letters[i][j] == '?') {
          return false;
        }
      }
    }
    return true;
  }

  public String getDate() {
    return date;
  }

  public int[][] getGeometry() {
    return geometry;
  }

  public char[][] getLetters() {
    return letters;
  }

  public Map<Integer, String> getClues() {
    return clues;
  }

  public Multimap<Integer, Integer> getRelated() {
    return related;
  }

  public String[] getCandidates() {
    return candidates;
  }

  public int getChosenCandidateIndex() {
    return chosenCandidateIndex;
  }

  public int[] getChosenPos() {
    return chosenPos;
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

    if (related != null) {
      sb.append("\nRelated:\n");
      sb.append(related);
    }

    return sb.toString();
  }

  public String lettersToString() {
    StringBuilder sb = new StringBuilder();
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

  private void loadWordPositions() {
    wordPositions = new HashMap<>();
    for (int num : clues.keySet()) {
      int[] pos = new int[3];

      for (int i = 0; i < PUZZLE_SIZE; i++) {
        for (int j = 0; j < PUZZLE_SIZE; j++) {
          if (num > 0 ? geometry[i][j] == num : geometry[i][j] == -num) {
            pos[0] = i; // x coordinate
            pos[1] = j; // y coordinate
            i = PUZZLE_SIZE;
            j = PUZZLE_SIZE;
          }
        }
      }
      if (num < 0) {
        for (int i = pos[0]; i < PUZZLE_SIZE; i++) {
          if (geometry[i][pos[1]] < 0) {
            pos[2] = i - pos[0];
            i = PUZZLE_SIZE;
          } else if (i == PUZZLE_SIZE - 1) {
            pos[2] = i - pos[0] + 1;
          }
        }
      } else {
        for (int j = pos[1]; j < PUZZLE_SIZE; j++) {
          if (geometry[pos[0]][j] < 0) {
            pos[2] = j - pos[1];
            j = PUZZLE_SIZE;
          } else if (j == PUZZLE_SIZE - 1) {
            pos[2] = j - pos[1] + 1;
          }
        }
      }
      wordPositions.put(num, pos);
    }
  }

  private void solve() {
    chosenPos = wordPositions.get(questions.get(index));
    String clue = clues.get(questions.get(index));
    List<Integer> relatedNums = null;
    if (related != null && related.containsKey(questions.get(index))) {
      relatedNums = new ArrayList<>(related.get(questions.get(index)));
    }

    char[] word = new char[chosenPos[2]];

    for (int i = 0; i < chosenPos[2]; i++) {
      if (questions.get(index) < 0) {
        word[i] = letters[chosenPos[0] + i][chosenPos[1]];
      } else {
        word[i] = letters[chosenPos[0]][chosenPos[1] + i];
      }
    }
    //TODO implement solver
    // bogo solver
    String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    Random random = new Random();

    for (int i = 0; i < word.length; i++) {
      if (word[i] == '?') {
        word[i] = alphabet.charAt(random.nextInt(alphabet.length()));
      }
    }

    for (int i = 0; i < chosenPos[2]; i++) {
      if (questions.get(index) < 0) {
        letters[chosenPos[0] + i][chosenPos[1]] = word[i];
      } else {
        letters[chosenPos[0]][chosenPos[1] + i] = word[i];
      }
    }
  }
}
