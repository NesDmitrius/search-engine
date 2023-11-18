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
import skillbox.SearchEngine.model.*;
import skillbox.SearchEngine.repositories.IndexRepository;
import skillbox.SearchEngine.repositories.LemmaRepository;
import skillbox.SearchEngine.repositories.PageRepository;
import skillbox.SearchEngine.repositories.SiteRepository;
import skillbox.SearchEngine.utility.IndexingSite;
import skillbox.SearchEngine.utility.LemmasFromText;
import skillbox.SearchEngine.utility.ParserSinglePage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final List<Site> siteList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private volatile boolean isIndexing;
    private ErrorResponse errorResponse;
    private static final int COUNT_THREADS = Runtime.getRuntime().availableProcessors() * 2;
    private ExecutorService executorService;

    @Autowired
    public IndexingServiceImpl(SitesList sites, SiteRepository siteRepository, PageRepository pageRepository,
                               LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
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
        indexingSinglePage(url);
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
            if (url.strip().startsWith(site.getUrl())) {
                return true;
            }
        }
        return false;
    }

    public Map<String, Integer> getLemmasFromPage(String textPage) {
        Map<String, Integer> lemmasMap = new HashMap<>();
        try {
            LemmasFromText lemmasFromText = new LemmasFromText();
            lemmasMap = lemmasFromText.getLemmasFromText(textPage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lemmasMap;
    }

    public void indexingSinglePage(String url) {
        String siteUrl;
        Site siteSinglePage = new Site();
        SiteEntity siteEntity = new SiteEntity();
        List<SiteEntity> listSiteEntity = new ArrayList<>();
        for (Site site : siteList) {
            siteUrl = site.getUrl();
            if (url.strip().startsWith(siteUrl)) {
                listSiteEntity = siteRepository.findByUrl(siteUrl);
                siteSinglePage = site;
            }
        }
        if (listSiteEntity.isEmpty()) {
            createSiteForSinglePage(siteSinglePage, siteEntity);
        } else {
            siteEntity = listSiteEntity.get(0);
        }
        ParserSinglePage parserSinglePage = new ParserSinglePage();
        try {
            parserSinglePage.parsePage(url, siteEntity.getUrl());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Page page = parserSinglePage.getPage();
        String textPage = parserSinglePage.getTextPage();
        String pathPage = page.getPath();
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        deleteInfoSinglePage(pathPage, siteEntity);
        PageEntity pageEntity = createSinglePage(page, siteEntity);
        createLemmaAndIndexFromPage(textPage, pageEntity, siteEntity);
    }

    public void deleteInfoSinglePage(String pathPage, SiteEntity siteEntity) {
        Optional<PageEntity> pageEntityOptional = pageRepository.findByPathAndSiteId(pathPage, siteEntity.getId());
        if (pageEntityOptional.isPresent()) {
            PageEntity pageEntity = pageEntityOptional.get();
            List<IndexEntity> indexToDelete = indexRepository.findIndexEntitiesByPageId(pageEntity.getId());
            List<LemmaEntity> lemmasToDelete = new ArrayList<>();
            List<LemmaEntity> lemmasFromDB = lemmaRepository.findLemmaEntitiesBySiteId(siteEntity.getId());
            if (!lemmasFromDB.isEmpty()) {
                System.out.println(lemmasFromDB.size());
                lemmasFromDB = getLemmasForPage(lemmasFromDB, indexToDelete);
                System.out.println(lemmasFromDB.size());
                List<LemmaEntity> lemmasForUpgrade = new ArrayList<>();
                for (LemmaEntity lemmaEntity : lemmasFromDB) {
                    if (lemmaEntity.getFrequency() == 1) {
                        lemmasToDelete.add(lemmaEntity);
                        continue;
                    }
                    lemmasForUpgrade.add(updateLemmaEntityForDelete(lemmaEntity));
                }
                indexRepository.deleteAll(indexToDelete);
                lemmaRepository.saveAll(lemmasForUpgrade);
                lemmaRepository.deleteAll(lemmasToDelete);
            }
            pageRepository.delete(pageEntity);
        }
    }

    public List<LemmaEntity> getLemmasForPage(List<LemmaEntity> lemmasFromDB, List<IndexEntity> indexEntities) {
        Set<Integer> lemmasId = new HashSet<>();
        if (!indexEntities.isEmpty()) {
            indexEntities.forEach(indexEntity -> lemmasId.add(indexEntity.getLemma().getId()));
        }
        return lemmasFromDB.stream()
                .filter(lemmaEntity -> lemmasId.contains(lemmaEntity.getId()))
                .collect(Collectors.toList());
    }

    public void createSiteForSinglePage(Site site, SiteEntity siteEntity) {
        siteEntity.setUrl(site.getUrl());
        siteEntity.setName(site.getName());
        siteEntity.setStatus(Status.INDEXED);
        siteEntity.setStatusTime(updateStatusTime());
        siteRepository.save(siteEntity);
    }

    public PageEntity createSinglePage(Page page, SiteEntity siteEntity) {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setPath(page.getPath());
        pageEntity.setCode(page.getCode());
        pageEntity.setContent(page.getContent());
        pageEntity.setSite(siteEntity);
        pageRepository.save(pageEntity);
        return pageEntity;
    }

    public void createLemmaAndIndexFromPage(String textPage, PageEntity pageEntity, SiteEntity siteEntity) {
        List<LemmaEntity> listLemmaEntity = lemmaRepository.findLemmaEntitiesBySiteId(siteEntity.getId());
        Map<String, Integer> lemmasMap = getLemmasFromPage(textPage);
        Set<String> lemmasSet = lemmasMap.keySet();
        List<LemmaEntity> lemmaEntities = new ArrayList<>(lemmasSet.size());
        if (listLemmaEntity.isEmpty()) {
            lemmaEntities = createNewLemmas(lemmasSet, siteEntity);
        } else {
            for (LemmaEntity lemmaEntity : listLemmaEntity) {
                String lemma = lemmaEntity.getLemma();
                if (lemmasSet.contains(lemma)) {
                    lemmaEntities.add(updateLemmaEntity(lemmaEntity));
                    lemmasSet.remove(lemma);
                }
            }
            lemmaEntities.addAll(createNewLemmas(lemmasSet, siteEntity));
        }
        lemmaRepository.saveAll(lemmaEntities);
        listLemmaEntity.clear();
        listLemmaEntity = lemmaRepository.findLemmaEntitiesBySiteId(siteEntity.getId());
//        listLemmaEntity.forEach(lemmaEntity -> {
//            System.out.println(lemmaEntity.getLemma() + " " + lemmaEntity.getId() + " " + lemmaEntity.getFrequency());
//            System.out.println(lemmaEntity.getIndexes().size());
//        });
        List<IndexEntity> indexEntities = createNewIndexes(pageEntity, listLemmaEntity, lemmasMap);
        indexRepository.saveAll(indexEntities);
    }

    public List<LemmaEntity> createNewLemmas(Set<String> lemmasSet, SiteEntity siteEntity) {
        List<LemmaEntity> lemmaEntities = new ArrayList<>(lemmasSet.size());
        if (lemmasSet.isEmpty()) {
            return lemmaEntities;
        }
        for (String lemma : lemmasSet) {
            LemmaEntity lemmaEntity = new LemmaEntity();
            lemmaEntity.setSite(siteEntity);
            lemmaEntity.setLemma(lemma);
            lemmaEntity.setFrequency(1);
            lemmaEntities.add(lemmaEntity);
        }
        return lemmaEntities;
    }

    public LemmaEntity updateLemmaEntity(LemmaEntity lemmaEntity) {
        int frequency = lemmaEntity.getFrequency() + 1;
        lemmaEntity.setFrequency(frequency);
        return lemmaEntity;
    }

    public LemmaEntity updateLemmaEntityForDelete(LemmaEntity lemmaEntity) {
        int frequency = lemmaEntity.getFrequency() - 1;
        lemmaEntity.setFrequency(frequency);
        return lemmaEntity;
    }

    public List<IndexEntity> createNewIndexes(PageEntity pageEntity,
                                              List<LemmaEntity> lemmaEntities,
                                              Map<String, Integer> lemmasMap) {

        List<IndexEntity> indexEntities = new ArrayList<>(lemmaEntities.size());

        System.out.println(lemmasMap.size());
        System.out.println();
        System.out.println(lemmaEntities.size());

        for (LemmaEntity lemmaEntity : lemmaEntities) {
            String lemma = lemmaEntity.getLemma();
            if (lemmasMap.containsKey(lemma)) {
                float rank = lemmasMap.get(lemma).floatValue();
                IndexEntity indexEntity = new IndexEntity();
                indexEntity.setPage(pageEntity);
                indexEntity.setLemma(lemmaEntity);
                indexEntity.setRank(rank);
                indexEntities.add(indexEntity);
            }
        }

        System.out.println("Indexes " + indexEntities.size());

        return indexEntities;
    }


}
