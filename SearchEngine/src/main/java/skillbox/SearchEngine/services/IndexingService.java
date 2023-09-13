package skillbox.SearchEngine.services;


import skillbox.SearchEngine.dto.CustomResponse;

public interface IndexingService {

    CustomResponse getResponseStartIndexing();
    CustomResponse getResponseStopIndexing();
}
