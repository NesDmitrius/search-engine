package skillbox.SearchEngine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import skillbox.SearchEngine.config.Page;
import skillbox.SearchEngine.config.Site;
import skillbox.SearchEngine.config.SitesList;
import skillbox.SearchEngine.dto.CustomResponse;
import skillbox.SearchEngine.dto.ErrorMessage;
import skillbox.SearchEngine.dto.ErrorResponse;
import skillbox.SearchEngine.dto.SuccessfulResponse;
import skillbox.SearchEngine.model.PageEntity;
import skillbox.SearchEngine.model.SiteEntity;
import skillbox.SearchEngine.model.Status;
import skillbox.SearchEngine.repositories.PageRepository;
import skillbox.SearchEngine.repositories.SiteRepository;
import skillbox.SearchEngine.utility.IndexingSite;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final List<Site> siteList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private boolean result;
    private volatile boolean isIndexing;

    private ErrorResponse errorResponse;

    private static final int COUNT_THREADS = Runtime.getRuntime().availableProcessors() * 2;
    private final ExecutorService executorService;

    @Autowired
    public IndexingServiceImpl(SitesList sites, SiteRepository siteRepository, PageRepository pageRepository) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        siteList = sites.getSites();
        result = false;
        isIndexing = false;
        executorService = Executors.newFixedThreadPool(COUNT_THREADS);
    }

    public CustomResponse getSuccessfulResponse() {
        SuccessfulResponse successfulResponse = new SuccessfulResponse();
        successfulResponse.setResult(true);
        return successfulResponse;
    }

    @Override
    public CustomResponse getResponseStartIndexing() {
        if (isIndexing) {
            errorResponse = new ErrorResponse();
            errorResponse.setResult(false);
            errorResponse.setError(ErrorMessage.START_INDEXING_ERROR.getMessage());
            return errorResponse;
        }
        if (!executorService.isShutdown()) {
            startIndexing();
            isIndexing = true;
        }
        return getSuccessfulResponse();
    }

    public CustomResponse getResponseStopIndexing() {
        if (!isIndexing) {
            errorResponse = new ErrorResponse();
            errorResponse.setResult(false);
            errorResponse.setError(ErrorMessage.STOP_INDEXING_ERROR.getMessage());
            return errorResponse;
        }
        if (!executorService.isShutdown()) {
            stopIndexing();
        }
        return getSuccessfulResponse();
    }

    public void startIndexing() {
        for (Site site : siteList) {
            executorService.execute(() -> {
                deleteDataFromDB(site);
                SiteEntity siteEntity = new SiteEntity();
                createNewIndexingSite(site, siteEntity);
                IndexingSite indexingSite = new IndexingSite(site.getUrl());
                List<Page> pageList = indexingSite.indexing();
                createNewPageIndexingSite(pageList, siteEntity);
                siteEntity.setStatusTime(updateStatusTime());
                siteEntity.setStatus(Status.INDEXED);
                siteRepository.save(siteEntity);
                System.out.println("Indexing completed");
                executorService.shutdown();
            });
        }
        isIndexing = false;
    }

    public synchronized void deleteDataFromDB(Site site) {
        String siteUrl = site.getUrl();
        List<SiteEntity> listSiteEntity = siteRepository.findByUrl(siteUrl);
        if (!listSiteEntity.isEmpty()) {
            List<PageEntity> pageEntityList = pageRepository.findBySite_UrlLike(site.getUrl());
            pageRepository.deleteAll(pageEntityList);
        }
        siteRepository.deleteByUrl(siteUrl);
    }

    public void createNewIndexingSite(Site site, SiteEntity siteEntity) {
        siteEntity.setUrl(site.getUrl());
        siteEntity.setName(site.getName());
        siteEntity.setStatus(Status.INDEXING);
        siteEntity.setStatusTime(updateStatusTime());
        siteRepository.save(siteEntity);
    }

    public void createNewPageIndexingSite(List<Page> pageList, SiteEntity siteEntity) {
        List<PageEntity> pageEntities = new ArrayList<>(pageList.size());
        for (Page page : pageList) {
            PageEntity pageEntity = new PageEntity();
            pageEntity.setPath(page.getPath());
            pageEntity.setCode(page.getCode());
            pageEntity.setContent(page.getContent());
            pageEntity.setSite(siteEntity);
            pageEntities.add(pageEntity);
        }
        pageRepository.saveAll(pageEntities);
    }

    public LocalDateTime updateStatusTime() {
        return LocalDateTime.now();
    }

    public void stopIndexing() {
        isIndexing = false;
        executorService.shutdown();
        try {
            if (executorService.awaitTermination(2, TimeUnit.MINUTES)) {
                System.out.println("Все задания выполнены!");
            } else {
                List<Runnable> notExecuted = executorService.shutdownNow();
                System.out.printf("Так и не запустилось %d заданий.%n", notExecuted.size());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<SiteEntity> listSiteEntity = siteRepository.findAll();
        for (SiteEntity siteEntity : listSiteEntity) {
            if (siteEntity.getStatus().equals(Status.INDEXING)) {
                siteEntity.setStatusTime(updateStatusTime());
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setLastError(ErrorMessage.CANCEL_INDEXING_ERROR.getMessage());
                siteRepository.save(siteEntity);
            }
        }
    }

}
