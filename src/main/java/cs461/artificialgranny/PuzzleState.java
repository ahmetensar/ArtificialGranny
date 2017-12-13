package cs461.artificialgranny;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.SerializationUtils;
// Imports the Google Cloud client library
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;

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
        if (questions != null && (index == questions.size() / 2 - 1 || index == questions.size() - 1)) {
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

        String clue = puzzle.clues.get(questions.get(index));
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
        StringTokenizer clueTok = new StringTokenizer(clue);
        String query = "=" + clueTok.nextToken();
        while (clueTok.hasMoreTokens()) {
            query = query.concat("+" + clueTok.nextToken());
        }
        query = query.concat("&sp=");

        for (char c : oldWord) {
            if (c == ' ') {
                query = query.concat("?");
            } else {
                query = query.concat(Character.toString(c));
            }
        }
        try {
            URL url = new URL("https://api.datamuse.com/words?ml" + query);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.connect();
            //int responsecode = con.getResponseCode();
            String s = null;
            Scanner sc = new Scanner(url.openStream());
            while (sc.hasNext()) {
                s += sc.next();
            }
            sc.close();
            String[] cands = s.split("word\":\"");
            for(int i = 1; i<cands.length; i++){
                String c = cands[i];
                int indexof = c.indexOf("\"");
                candidates.add( c.substring(0, indexof) );
            }
            
        } catch (MalformedURLException ex) {
            Logger.getLogger(PuzzleState.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PuzzleState.class.getName()).log(Level.SEVERE, null, ex);
        }
        String chosenWord;
        if(candidates.size() >= 2)
            chosenWord = candidates.get(1);
        else
            chosenWord = candidates.get(0);
        
        System.out.println(chosenWord);
        
        if (!chosenWord.equals(new String(oldWord))) {
            for (int i = 0; i < chosenPos[2]; i++) {
                if(!(i < chosenWord.length()))
                    break;
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
