package skillbox.SearchEngine.services;

import skillbox.SearchEngine.dto.CustomResponse;

public interface SearchService {
    CustomResponse getSearchFromSite(String query, String site);
    CustomResponse getSearch(String query);
}
