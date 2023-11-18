package skillbox.SearchEngine.utility;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LemmasFromText {

    private final static String REGEX = "[^А-Яа-я]+";
    private final static String[] FUNC_PARTS_OF_SPEECH = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "МС"};

    public Map<String, Integer> getLemmasFromText(String text) throws IOException {
        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
        Map<String, Integer> lemmas = new HashMap<>();
        String [] words = text.split(REGEX);
        for (String word : words) {
            if (word.isBlank() || word.length() < 3) {
                continue;
            }

            List<String> wordInfo = luceneMorphology.getMorphInfo(word.toLowerCase());
            if (wordHasPartsOfSpeech(wordInfo)) {
                continue;
            }

            List<String> wordBaseForm = luceneMorphology.getNormalForms(word.toLowerCase());
            if (wordBaseForm.isEmpty()) {
                continue;
            }
            String normalWord = wordBaseForm.get(0);
            lemmas.merge(normalWord, 1, Integer::sum);
        }
        return lemmas;

    }

    private boolean wordHasPartsOfSpeech(List<String> words) {
        return words.stream().anyMatch(this::isFunctionalPartsOfSpeech);
    }

    private boolean isFunctionalPartsOfSpeech(String word) {
        for (String partsOfSpeech : FUNC_PARTS_OF_SPEECH) {
            if (word.contains(partsOfSpeech)) {
                return true;
            }
        }
        return false;
    }

}
