package skillbox.SearchEngine.dto;

import lombok.Data;

@Data
public class ErrorResponse implements CustomResponse {
    private boolean result;
    private String error;
}
