package skillbox.SearchEngine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import skillbox.SearchEngine.dto.statistics.DetailedStatisticsItem;
import skillbox.SearchEngine.dto.statistics.StatisticsData;
import skillbox.SearchEngine.dto.statistics.StatisticsResponse;
import skillbox.SearchEngine.dto.statistics.TotalStatistics;
import skillbox.SearchEngine.model.SiteEntity;
import skillbox.SearchEngine.repositories.LemmaRepository;
import skillbox.SearchEngine.repositories.PageRepository;
import skillbox.SearchEngine.repositories.SiteRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Autowired
    public StatisticsServiceImpl(SiteRepository siteRepository, PageRepository pageRepository,
                                 LemmaRepository lemmaRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
    }

    @Override
    public StatisticsResponse getStatistics() {

        List<SiteEntity> siteEntities = siteRepository.findAll();

        TotalStatistics total = new TotalStatistics();
        total.setSites(siteEntities.size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = getDetailedStatisticsItem(siteEntities, total);

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private List<DetailedStatisticsItem> getDetailedStatisticsItem (List<SiteEntity> siteEntities,
                                                                    TotalStatistics total) {
        String error = "";
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (SiteEntity siteEntity : siteEntities) {
            if (siteEntity.getLastError() != null) {
                error = siteEntity.getLastError();
            }
            int siteId = siteEntity.getId();
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteEntity.getName());
            item.setUrl(siteEntity.getUrl());
            int pages = pageRepository.countPageEntitiesBySiteId(siteId);
            int lemmas = lemmaRepository.countLemmaEntitiesBySiteId(siteId);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(siteEntity.getStatus().toString());
            item.setError(error);
            item.setStatusTime(siteEntity.getStatusTime());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }
        return detailed;
    }
}
