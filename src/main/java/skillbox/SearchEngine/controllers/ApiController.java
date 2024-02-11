package skillbox.SearchEngine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skillbox.SearchEngine.dto.CustomResponse;
import skillbox.SearchEngine.dto.statistics.StatisticsResponse;
import skillbox.SearchEngine.services.IndexingService;
import skillbox.SearchEngine.services.SearchService;
import skillbox.SearchEngine.services.StatisticsService;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService,
                         SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<CustomResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.getResponseStartIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<CustomResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.getResponseStopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<CustomResponse> indexPage(@RequestParam String url) {
        return ResponseEntity.ok(indexingService.getResponseIndexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<CustomResponse> search(@RequestParam String query, @RequestParam Optional<String> site) {
        return site.map(s -> ResponseEntity.ok(searchService.getSearchFromSite(query, s)))
                .orElseGet(() -> ResponseEntity.ok(searchService.getSearch(query)));
    }
}
