package skillbox.SearchEngine.utility;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import skillbox.SearchEngine.config.Page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveTask;

public class ParserLinks extends RecursiveTask<String> {

    private static final String USER_AGENT = "DmitriusSearchBot";
    private static final String REFERRER = "https://www.google.com";

    private final Node node;
    private String site;
    private static CopyOnWriteArrayList<String> allSetLinks = new CopyOnWriteArrayList<>();
    private List<Page> pageList = new ArrayList<>();

    private int statusCodePage;

    public ParserLinks(Node node) {
        this.node = node;
    }

    @Override
    protected String compute() {

        List<ParserLinks> subTask = new LinkedList<>();
        site = node.getSite();
        String link = "";

        try {
            Thread.sleep(500);
            getLinks(site);

        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Node linkPage : node.getChildren()) {
            ParserLinks task = new ParserLinks(linkPage);
            task.fork();
            subTask.add(task);
        }
        for (ParserLinks task : subTask) {
            link = task.join();
        }
        return link;

    }

    private void getLinks(String url) throws Exception {
        Document page = Jsoup.connect(url).userAgent(USER_AGENT)
                .referrer(REFERRER).timeout(100000).ignoreHttpErrors(true).get();
        Elements links = page.select("a");
        for(Element element : links) {
            String link = element.attr("abs:href");
            if (isCheckedUrl(link)) {
                allSetLinks.add(link);
                node.addChild(new Node(link));
                String path = link.replace(site, "");
                statusCodePage = getStatusCodePage(link);
                String content = page.html();
                pageList.add(new Page(path, statusCodePage, content));
                getLinks(link);
            }
        }
    }

    private boolean isCheckedUrl(String url) {
        return (!url.isEmpty() && url.startsWith(site.concat("/"))
                && !allSetLinks.contains(url) && !url.contains("#") && !url.contains("?")
                && !url.matches("([^\\s]+(\\.(?i)(jpg|jpeg|png|gif|bmp|pdf|doc|docx|xls|xlsx|mp3))$)"));
    }

    private int getStatusCodePage(String url) {
        int statusCode = 0;
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(USER_AGENT).referrer(REFERRER)
                    .timeout(10000).ignoreHttpErrors(true)
                    .execute();
            statusCode = response.statusCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return statusCode;
    }

    public List<Page> getPageList() {
        allSetLinks.clear();
        return pageList;
    }
}
