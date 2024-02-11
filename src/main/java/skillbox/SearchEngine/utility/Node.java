package skillbox.SearchEngine.utility;

import java.util.concurrent.CopyOnWriteArrayList;

public class Node {

    private final String site;
    private volatile CopyOnWriteArrayList<Node> children;

    public Node(String site) {
        this.site = site;
        children = new CopyOnWriteArrayList<>();
    }

    public String getSite() {
        return site;
    }

    public void addChild(Node node) {
        children.add(node);
    }

    public CopyOnWriteArrayList<Node> getChildren() {
        return children;
    }

    @Override
    public String toString() {

        StringBuilder result = new StringBuilder(getSite());
        getChildren().forEach(child -> result.append("\n").append(child));
        return result.toString();
    }


}
