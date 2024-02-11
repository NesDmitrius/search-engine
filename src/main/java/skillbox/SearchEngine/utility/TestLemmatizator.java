package skillbox.SearchEngine.utility;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
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

        System.out.println("Английский:");
        LuceneMorphology luceneMorphologyEng = new EnglishLuceneMorphology();
        List<String> wordBaseFormEng = luceneMorphologyEng.getNormalForms("texts");
        List<String> wordInfoEng = luceneMorphologyEng.getMorphInfo("and");
        wordBaseFormEng.forEach(System.out::println);
        wordInfoEng.forEach(System.out::println);

        String text = "Повторное появление леопарда в Осетии позволяет предположить, " +
                "что леопард постоянно обитает в некоторых районах Северного Кавказа." +
                "English texts for beginners to practice reading and comprehension online and for free. " +
                "Practicing your comprehension of written English will both improve your vocabulary and understanding " +
                "of grammar and word order. The texts below are designed to help you develop while giving you " +
                "an instant evaluation of your progress.";

        LemmasFromText lemmasFromText = new LemmasFromText();
        Map<String, Integer> lemmas = lemmasFromText.getLemmasFromText(text);
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }
}
