package skillbox.SearchEngine.utility;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LemmasFromText {

    private final static String REGEX_RUS = "[^А-Яа-я]+";
    private final static String REGEX_ENG = "[^A-Za-z]+";
    private final static String[] FUNC_PARTS_OF_SPEECH = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "МС", "ВВОДН", "ЧАСТ",
            "CONJ", "PART", "PN_ADJ", "ARTICLE", "PREP", "PN"};
    private final LuceneMorphology russianLuceneMorphology;
    private final LuceneMorphology englishLuceneMorphology;

    public LemmasFromText() throws IOException {
        russianLuceneMorphology = new RussianLuceneMorphology();
        englishLuceneMorphology = new EnglishLuceneMorphology();
    }

    public Map<String, Integer> getLemmasFromText(String text) throws IOException {
        Map<String, Integer> lemmas = new HashMap<>();
        String [] russianWords = text.split(REGEX_RUS);
        String [] englishWords = text.split(REGEX_ENG);
        if (russianWords.length != 0) {
            lemmas.putAll(getRusLemmas(russianWords));
        }
        if (englishWords.length != 0) {
            lemmas.putAll(getEngLemmas(englishWords));
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

    private Map<String, Integer> getRusLemmas(String[] words) {
        Map<String, Integer> rusLemmas = new HashMap<>();
        for (String word : words) {
            if (word.isBlank() || word.length() < 3) {
                continue;
            }
            List<String> wordInfo = russianLuceneMorphology.getMorphInfo(word.toLowerCase());
            if (wordHasPartsOfSpeech(wordInfo)) {
                continue;
            }
            List<String> wordBaseForm = russianLuceneMorphology.getNormalForms(word.toLowerCase());
            if (wordBaseForm.isEmpty()) {
                continue;
            }
            String normalWord = wordBaseForm.get(0);
            rusLemmas.merge(normalWord, 1, Integer::sum);
        }
        return rusLemmas;
    }

    private Map<String, Integer> getEngLemmas(String[] words)  {
        Map<String, Integer> engLemmas = new HashMap<>();
        for (String word : words) {
            if (word.isBlank() || word.length() < 3) {
                continue;
            }
            List<String> wordInfo = englishLuceneMorphology.getMorphInfo(word.toLowerCase());
            if (wordHasPartsOfSpeech(wordInfo)) {
                continue;
            }
            List<String> wordBaseForm = englishLuceneMorphology.getNormalForms(word.toLowerCase());
            if (wordBaseForm.isEmpty()) {
                continue;
            }
            String normalWord = wordBaseForm.get(0);
            engLemmas.merge(normalWord, 1, Integer::sum);
        }
        return engLemmas;
    }

    public String getNormalForm(String word) {
        String [] russianWords = word.split(REGEX_RUS);
        String [] englishWords = word.split(REGEX_ENG);
        if (russianWords.length != 0) {
            if (russianWords[0].length() < 3) {
                return word;
            }
            List<String> wordInfo = russianLuceneMorphology.getMorphInfo(russianWords[0].toLowerCase());
            if (wordHasPartsOfSpeech(wordInfo)) {
                return word;
            }
            List<String> wordBaseForm = russianLuceneMorphology.getNormalForms(russianWords[0].toLowerCase());
            if (!wordBaseForm.isEmpty()) {
                return wordBaseForm.get(0);
            }
        }
        if (englishWords.length != 0) {
            if (englishWords[0].length() < 3) {
                return word;
            }
            List<String> wordInfo = englishLuceneMorphology.getMorphInfo(englishWords[0].toLowerCase());
            if (wordHasPartsOfSpeech(wordInfo)) {
                return word;
            }
            List<String> wordBaseForm = englishLuceneMorphology.getNormalForms(englishWords[0].toLowerCase());
            if (!wordBaseForm.isEmpty()) {
                return wordBaseForm.get(0);
            }
        }
        return word;
    }

}
