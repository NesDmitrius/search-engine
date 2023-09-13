package skillbox.SearchEngine.dto;

public enum ErrorMessage {

    START_INDEXING_ERROR("Индексация уже запущена"),
    STOP_INDEXING_ERROR("Индексация не запущена"),
    INDEX_PAGE_ERROR("Данная страница находится за пределами сайтов, указанных в конфигурационном файле"),
    SEARCH_ERROR("Задан пустой запрос"),
    DEFAULT_ERROR("Указанная страница не найдена"),
    CANCEL_INDEXING_ERROR("Индексация остановлена пользователем");

    private final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
