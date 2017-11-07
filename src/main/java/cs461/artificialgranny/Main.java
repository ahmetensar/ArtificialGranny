package cs461.artificialgranny;

import cs461.artificialgranny.model.PuzzleState;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

public class Main {

  public static void main(String[] args) {
    File puzzleDir = new File("puzzles");
    if (!puzzleDir.exists()) {
      if (puzzleDir.mkdir()) {
        System.out.println("Puzzles folder is created");
      } else {
        System.out.println("Puzzles folder could not be created");
      }
      System.out.println();
    }

    Scanner scan = new Scanner(System.in);
    PuzzleManager pm = null;
    int choice;
    do {
      System.out.println("1: Load puzzle from web");
      System.out.println("2: Load puzzle from file");
      if (pm != null) {
        System.out.println("3: Solve");
      }
      System.out.println("0: Exit");
      System.out.println();
      System.out.println("Enter an integer:");
      choice = scan.nextInt();
      scan.nextLine();

      if (choice == 1) {
        pm = new PuzzleManager();
        pm.savePuzzleToFile("puzzles" + File.separator
            + pm.getPuzzleState().getDate() + ".puzzle");
        System.out.println(pm.getPuzzleState());
      }

      if (choice == 2) {
        File f = new File("puzzles");
        File[] fileArray = f.listFiles();
        if (fileArray == null) {
          System.out.println("There is no puzzle");
        } else {
          ArrayList<File> files = new ArrayList<>(Arrays.asList(fileArray));
          Collections.sort(files);
          for (int i = 0; i < files.size(); i++) {
            if (files.get(i).getName().endsWith(".puzzle")) {
              System.out.println((i + 1) + ": " + files.get(i).toString()
                  .substring(8, files.get(i).toString().length()));
            } else {
              files.remove(i);
              i--;
            }
          }
          System.out.println();
          System.out.println("Choose:");
          int puzzleNum = scan.nextInt();
          if (puzzleNum < 1 || puzzleNum > files.size()) {
            System.out.println("Invalid!");
          } else {
            pm = new PuzzleManager(files.get(puzzleNum - 1));
            System.out.println(pm.getPuzzleState());
          }
        }
      }

      if (choice == 3 && pm != null) {
        PuzzleState solvableState = pm.getSolvableState();
        while (!solvableState.isFull()) {
          solvableState.step();
          System.out.println(solvableState.lettersToString());
        }
      }

      System.out.println();
      System.out.println();
      System.out.println();
    } while (choice != 0);
  }
}
