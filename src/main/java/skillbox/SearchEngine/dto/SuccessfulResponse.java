package skillbox.SearchEngine.dto;

import lombok.Data;

@Data
public class SuccessfulResponse implements CustomResponse {
    private boolean result;
}
