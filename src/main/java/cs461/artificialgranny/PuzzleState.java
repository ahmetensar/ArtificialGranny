package cs461.artificialgranny;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.SerializationUtils;

class PuzzleState {

  private final Puzzle puzzle;

  private final List<Integer> questions;
  private int index;

  private List<String> candidates;
  private boolean isSpaceLeft;

  PuzzleState(Puzzle puzzle) {
    this.puzzle = SerializationUtils.clone(puzzle);
    questions = new ArrayList<>(puzzle.getClues().keySet());
    candidates = new ArrayList<>();
    index = -1;
    isSpaceLeft = true;
  }

  PuzzleState(PuzzleState puzzleState) {
    this.puzzle = SerializationUtils.clone(puzzleState.puzzle);
    this.questions = puzzleState.questions;
    this.index = puzzleState.index;
    this.candidates = new ArrayList<>(puzzleState.candidates);
    this.isSpaceLeft = puzzleState.isSpaceLeft;
  }

  boolean next() {
    if (isLast()) {
      return false;
    }
    solve();
    return true;
  }

  private boolean isLast() {
    if(questions != null && (index == questions.size() / 2 - 1 || index == questions.size() - 1)) {
      if (isSpaceLeft) {
        isSpaceLeft = false;
        return false;
      }
      return true;
    }
    return false;
  }

  private void solve() {
    index++;
    if (index == questions.size()) {
      index = 0;
    }
    int[] chosenPos = puzzle.getWordPositions().get(questions.get(index));

//    String clue = puzzle.clues.get(questions.get(index));
//    List<Integer> relatedNumbers;
//    if (puzzle.getRelated() != null && puzzle.getRelated().containsKey(questions.get(index))) {
//      relatedNumbers = new ArrayList<>(puzzle.getRelated().get(questions.get(index)));
//    }

    char[] oldWord = new char[chosenPos[2]];

    for (int i = 0; i < chosenPos[2]; i++) {
      if (questions.get(index) < 0) {
        oldWord[i] = puzzle.getLetters()[chosenPos[0] + i][chosenPos[1]];
      } else {
        oldWord[i] = puzzle.getLetters()[chosenPos[0]][chosenPos[1] + i];
      }
    }

    for (char c : oldWord) {
      if (c == ' ') {
        isSpaceLeft = true;
      }
    }

    //TODO implement solver
    // bogo solver
    String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    Random random = new Random();

    candidates = new ArrayList<>();
    candidates.add(new String(oldWord));
    for (int i = 0; i < 10; i++) {
      char[] newWord = Arrays.copyOf(oldWord, oldWord.length);
      for (int j = 0; j < newWord.length; j++) {
        if (newWord[j] == ' ') {
          newWord[j] = alphabet.charAt(random.nextInt(alphabet.length()));
        }
      }
      if (!Arrays.equals(oldWord, newWord)) {
        candidates.add(new String(newWord));
      }
    }

    Collections.shuffle(candidates);
    String chosenWord = candidates.get(0);

    if (!chosenWord.equals(new String(oldWord))) {
      for (int i = 0; i < chosenPos[2]; i++) {
        if (questions.get(index) < 0) {
          puzzle.getLetters()[chosenPos[0] + i][chosenPos[1]] = chosenWord.charAt(i);
        } else {
          puzzle.getLetters()[chosenPos[0]][chosenPos[1] + i] = chosenWord.charAt(i);
        }
      }
    }
  }

  Puzzle getPuzzle() {
    return puzzle;
  }

  List<String> getCandidates() {
    return candidates;
  }

  int getChosenIndex() {
    return index > -1 ? questions.get(index) : 0;
  }


}
