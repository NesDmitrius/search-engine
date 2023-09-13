package skillbox.SearchEngine.utility;

import skillbox.SearchEngine.config.Page;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class IndexingSite {

    private String site;

    public IndexingSite(String site) {
        this.site = site;
    }

    public List<Page> indexing() {

        System.out.println("Start");
        Node root = new Node(site);
        ParserLinks parserLinks = new ParserLinks(root);
        System.out.println("Pool");
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(parserLinks);
        System.out.println("Print");
        System.out.println("End");
        pool.shutdown();
        return parserLinks.getPageList();


    }
}
