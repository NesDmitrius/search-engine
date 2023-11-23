package skillbox.SearchEngine.utility;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import skillbox.SearchEngine.config.Page;

import java.io.IOException;

public class ParserSinglePage {

    private Page page;
    private String textPage;

    public void parsePage(String pageUrl, String siteUrl) throws IOException {
        Document documentPage = Jsoup.connect(pageUrl).timeout(1000).ignoreHttpErrors(true).get();
        String path = pageUrl.replace(siteUrl, "");
        if (path.isEmpty()) {
            path = "/";
        }
        String content = documentPage.html();
        int statusCode = getStatusCodePage(pageUrl);
        page = new Page(path, statusCode, content);
        textPage = documentPage.text();
    }

    private int getStatusCodePage(String pageUrl) throws IOException {
        int statusCode;
        Connection.Response response = Jsoup.connect(pageUrl)
                .timeout(1000).ignoreHttpErrors(true)
                .execute();
        statusCode = response.statusCode();
        return statusCode;
    }

    public Page getPage() {
        return page;
    }

    public String getTextPage() {
        return textPage;
    }

    public String getTextPageFromContent(String content) {
        return Jsoup.parse(content).text();
    }
}
