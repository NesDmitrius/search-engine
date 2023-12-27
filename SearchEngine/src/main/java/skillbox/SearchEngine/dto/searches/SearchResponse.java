package skillbox.SearchEngine.dto.searches;

import lombok.Data;
import skillbox.SearchEngine.dto.CustomResponse;

import java.util.List;

@Data
public class SearchResponse implements CustomResponse {
    private boolean result;
    private int count;
    private List<SearchData> data;
}
