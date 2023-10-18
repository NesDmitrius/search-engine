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

        String textHtml = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" +
                "        \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>Сайт для теста</title>\n" +
                "</head>\n" +
                "<script>\n" +
                "    // some interesting script functions\n" +
                "</script>\n" +
                "<body>\n" +
                "    <p>\n" +
                "        Текст для тестирования программы и метода<br/>\n" +
                "        1. <a\n" +
                "            id=\"link\"\n" +
                "            href=\"http://maven.apache.org/\">\n" +
                "            Maven\n" +
                "            </a> is not installed.<br/>\n" +
                "        2. Текст простой (<1G) дисковое пространство.<br/>\n" +
                "        3. Леммы из текста (<64MB) память.<br/>\n" +
                "    </p>\n" +
                "</body>\n" +
                "</html>";

        String textWithoutHtml = lemmasFromText.cleanTextHtml(textHtml);
        System.out.println(textWithoutHtml);
        Map<String, Integer> lemmasTextHtml = lemmasFromText.getLemmasFromText(textWithoutHtml);
        for (Map.Entry<String, Integer> entry : lemmasTextHtml.entrySet()) {
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }

    }
}
