package skillbox.SearchEngine.utility;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TestLemmatizator {

    public static void main(String[] args) throws IOException {

        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
        List<String> wordBaseForm = luceneMorphology.getNormalForms("леса");
        List<String> wordInfo = luceneMorphology.getMorphInfo("некоторый");
        wordBaseForm.forEach(System.out::println);
        wordInfo.forEach(System.out::println);

        String text = "Повторное появление леопарда в Осетии позволяет предположить, " +
                "что леопард постоянно обитает в некоторых районах Северного Кавказа.";

        LemmasFromText lemmasFromText = new LemmasFromText();
        Map<String, Integer> lemmas = lemmasFromText.getLemmasFromText(text);
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }
}
