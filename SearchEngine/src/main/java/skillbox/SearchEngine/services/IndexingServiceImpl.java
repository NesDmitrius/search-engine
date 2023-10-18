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
import skillbox.SearchEngine.utility.LemmasFromText;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Service
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final List<Site> siteList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private volatile boolean isIndexing;
    private ErrorResponse errorResponse;
    private static final int COUNT_THREADS = Runtime.getRuntime().availableProcessors() * 2;
    private ExecutorService executorService;

    @Autowired
    public IndexingServiceImpl(SitesList sites, SiteRepository siteRepository, PageRepository pageRepository) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        siteList = sites.getSites();
        isIndexing = false;
    }

    public CustomResponse getSuccessfulResponse() {
        SuccessfulResponse successfulResponse = new SuccessfulResponse();
        successfulResponse.setResult(true);
        return successfulResponse;
    }

    @Override
    public CustomResponse getResponseStartIndexing() {
        if (isIndexing && !executorService.isTerminated()) {
            errorResponse = new ErrorResponse();
            errorResponse.setResult(false);
            errorResponse.setError(ErrorMessage.START_INDEXING_ERROR.getMessage());
            return errorResponse;
        }
        isIndexing = true;
        executorService = Executors.newFixedThreadPool(COUNT_THREADS);
        startIndexing();
        return getSuccessfulResponse();
    }

    public CustomResponse getResponseStopIndexing() {
        if (!isIndexing || executorService.isTerminated()) {
            errorResponse = new ErrorResponse();
            errorResponse.setResult(false);
            errorResponse.setError(ErrorMessage.STOP_INDEXING_ERROR.getMessage());
            return errorResponse;
        }
        awaitTerminationAfterShutdown(executorService);
        stopIndexing();
        return getSuccessfulResponse();
    }

    public CustomResponse getResponseIndexPage(String url) {
        if (url.isEmpty() || url.isBlank() || !urlHasSiteList(url)) {
            errorResponse = new ErrorResponse();
            errorResponse.setResult(false);
            errorResponse.setError(ErrorMessage.INDEX_PAGE_ERROR.getMessage());
            return errorResponse;
        }
        getLemmasFromPage(url);
        return getSuccessfulResponse();
    }

    public void startIndexing() {
        for (Site site : siteList) {
            executorService.submit(() -> {
                deleteDataFromDB(site);
                SiteEntity siteEntity = new SiteEntity();
                createNewIndexingSite(site, siteEntity);
                IndexingSite indexingSite = new IndexingSite(site.getUrl());
                List<Page> pageList = indexingSite.indexing();
                createNewPageIndexingSite(pageList, siteEntity);
                updateSiteIndexed(siteEntity);
                awaitTerminationAfterShutdown(executorService);
                System.out.println("Indexing completed");
            });
        }
    }

    public synchronized void deleteDataFromDB(Site site) {
        String siteUrl = site.getUrl();
        List<SiteEntity> listSiteEntity = siteRepository.findByUrl(siteUrl);
        if (!listSiteEntity.isEmpty()) {
            List<PageEntity> pageEntityList = pageRepository.findBySite_UrlLike(siteUrl);
            pageRepository.deleteAll(pageEntityList);
        }
        siteRepository.deleteByUrl(siteUrl);
    }

    public synchronized void createNewIndexingSite(Site site, SiteEntity siteEntity) {
        siteEntity.setUrl(site.getUrl());
        siteEntity.setName(site.getName());
        siteEntity.setStatus(Status.INDEXING);
        siteEntity.setStatusTime(updateStatusTime());
        siteRepository.save(siteEntity);
    }

    public synchronized void createNewPageIndexingSite(List<Page> pageList, SiteEntity siteEntity) {
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

    public synchronized void updateSiteIndexed(SiteEntity siteEntity) {
        siteEntity.setStatusTime(updateStatusTime());
        siteEntity.setStatus(Status.INDEXED);
        siteRepository.save(siteEntity);
    }

    public synchronized void stopIndexing() {
        awaitTerminationAfterShutdown(executorService);
        isIndexing = false;
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

    public void awaitTerminationAfterShutdown(ExecutorService threadPool) {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.MILLISECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public boolean urlHasSiteList(String url) {
        for (Site site : siteList) {
            if (site.getUrl().contains(url.strip())) {
                return true;
            }
        }
        return false;
    }

    public void getLemmasFromPage(String url) {
        try {
            Logger.getLogger(IndexingServiceImpl.class.getSimpleName()).info(url);
            LemmasFromText lemmasFromText = new LemmasFromText();
            String textWithoutHtml = lemmasFromText.textFromPage(url);
            Map<String, Integer> lemmasTextHtml = lemmasFromText.getLemmasFromText(textWithoutHtml);
            for (Map.Entry<String, Integer> entry : lemmasTextHtml.entrySet()) {
                System.out.println(entry.getKey() + " - " + entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
