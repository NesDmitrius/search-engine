package skillbox.SearchEngine.config;

import lombok.Setter;

@Setter
public class Page {

    private String path;
    private int code;
    private String content;

    public Page(String path, int code, String content) {
        this.path = path;
        this.code = code;
        this.content = content;
    }

    public String getPath() {
        return path;
    }

    public int getCode() {
        return code;
    }

    public String getContent() {
        return content;
    }
}
