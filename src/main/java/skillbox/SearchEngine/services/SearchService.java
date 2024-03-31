package skillbox.SearchEngine.services;

import skillbox.SearchEngine.dto.CustomResponse;

public interface SearchService {
    CustomResponse getSearchFromSite(String query, String site, int offset, int limit);
    CustomResponse getSearch(String query, int offset, int limit);
}
