package skillbox.SearchEngine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import skillbox.SearchEngine.dto.CustomResponse;
import skillbox.SearchEngine.dto.ErrorMessage;
import skillbox.SearchEngine.dto.ErrorResponse;
import skillbox.SearchEngine.dto.searches.SearchData;
import skillbox.SearchEngine.dto.searches.SearchResponse;
import skillbox.SearchEngine.model.IndexEntity;
import skillbox.SearchEngine.model.LemmaEntity;
import skillbox.SearchEngine.model.PageEntity;
import skillbox.SearchEngine.model.SiteEntity;
import skillbox.SearchEngine.repositories.IndexRepository;
import skillbox.SearchEngine.repositories.LemmaRepository;
import skillbox.SearchEngine.repositories.PageRepository;
import skillbox.SearchEngine.repositories.SiteRepository;
import skillbox.SearchEngine.utility.LemmasFromText;
import skillbox.SearchEngine.utility.ParserSinglePage;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private static final int OPTIMAL_FREQUENCY_PERCENT = 60;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Autowired
    public SearchServiceImpl(SiteRepository siteRepository, PageRepository pageRepository,
                             LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Override
    public CustomResponse getSearch(String query, int offset, int limit) {
        if (query.isBlank()) {
            return getErrorResponse();
        }

        List<SiteEntity> siteEntities = siteRepository.findAllByStatus();
        if (siteEntities.isEmpty()) {
            return getNotIndexedErrorResponse();
        }

        List<SearchData>  data = new ArrayList<>();
        for (SiteEntity siteEntity : siteEntities) {
            List<LemmaEntity> lemmaEntities = getRareSortedLemmasFromSite(getLemmasText(query), siteEntity);
            List<PageEntity> pageEntities = getPageByLemmas(lemmaEntities);
            data.addAll(getSearchDataList(query, siteEntity, pageEntities, lemmaEntities));
        }

        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResult(true);
        searchResponse.setCount(data.size());
        searchResponse.setData(getSortListSearchData(data).stream().skip(offset).limit(limit).toList());
        return searchResponse;
    }

    @Override
    public CustomResponse getSearchFromSite(String query, String site, int offset, int limit) {
        if (query.isBlank()) {
            return getErrorResponse();
        }

        Optional<SiteEntity> optionalSiteEntity = siteRepository.findByUrlAndStatus(site);
        if (optionalSiteEntity.isEmpty()) {
            return getNotIndexedErrorResponse();
        }
        SiteEntity siteEntity = optionalSiteEntity.get();
        List<LemmaEntity> lemmaEntities = getRareSortedLemmasFromSite(getLemmasText(query), siteEntity);
        List<PageEntity> pageEntities = getPageByLemmas(lemmaEntities);

        List<SearchData> data = getSearchDataList(query, siteEntity, pageEntities, lemmaEntities);

        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResult(true);
        searchResponse.setCount(data.size());
        searchResponse.setData(getSortListSearchData(data).stream().skip(offset).limit(limit).toList());
        return searchResponse;
    }

    private CustomResponse getErrorResponse() {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setResult(false);
        errorResponse.setError(ErrorMessage.SEARCH_ERROR.getMessage());
        return errorResponse;
    }

    private CustomResponse getNotIndexedErrorResponse() {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setResult(false);
        errorResponse.setError(ErrorMessage.DEFAULT_ERROR.getMessage());
        return errorResponse;
    }

    private List<SearchData> getSearchDataList(String query, SiteEntity siteEntity, List<PageEntity> pageEntities,
                                               List<LemmaEntity> lemmaEntities) {
        List<SearchData>  data = new ArrayList<>();
        if (pageEntities.isEmpty()) {
            return data;
        }
        List<Float> relevanceAbsList = new ArrayList<>();
        pageEntities.forEach(pageEntity -> relevanceAbsList.add(getRelevance(pageEntity, lemmaEntities)));
        float maxRelevanceAbs = relevanceAbsList.stream().max(Float::compare).orElse(1f);
        ParserSinglePage parserSinglePage = new ParserSinglePage();
        for (PageEntity pageEntity : pageEntities) {
            String contentPage = pageEntity.getContent();
            SearchData item = new SearchData();
            item.setSite(siteEntity.getUrl());
            item.setSiteName(siteEntity.getName());
            item.setUri(pageEntity.getPath());
            item.setTitle(parserSinglePage.getTitlePageFromContent(contentPage));
            item.setSnippet(getSnippet(query, parserSinglePage.getTextPageFromContent(contentPage)));
            item.setRelevance(getRelevance(pageEntity, lemmaEntities) / maxRelevanceAbs);
            data.add(item);
        }
        return data;
    }

    private List<SearchData> getSortListSearchData(List<SearchData> data) {
        return data.stream()
                .sorted(Comparator.comparing(SearchData::getRelevance).reversed())
                .collect(Collectors.toList());
    }

    private Set<String> getLemmasText(String text) {
        Map<String, Integer> lemmasMap = new HashMap<>();
        try {
            LemmasFromText lemmasFromText = new LemmasFromText();
            lemmasMap = lemmasFromText.getLemmasFromText(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashSet<>(lemmasMap.keySet());
    }

    private List<LemmaEntity> getRareSortedLemmasFromSite(Set<String> lemmasSet, SiteEntity siteEntity) {
        int siteId = siteEntity.getId();
        int countPage = pageRepository.countPageEntitiesBySiteId(siteId);
        List<LemmaEntity> lemmaEntities = lemmaRepository.findLemmaEntitiesBySiteId(siteId);

        int minCountLemmaOnPage = OPTIMAL_FREQUENCY_PERCENT * countPage / 100;
        return lemmaEntities.stream()
                .filter(lemmaEntity -> lemmasSet.contains(lemmaEntity.getLemma()))
                .filter(lemmaEntity -> lemmaEntity.getFrequency() <= minCountLemmaOnPage)
                .sorted(Comparator.comparing(LemmaEntity::getFrequency))
                .collect(Collectors.toList());
    }

    private List<PageEntity> getPageByLemmas(List<LemmaEntity> lemmaEntities) {
        List<PageEntity> pageEntities = new ArrayList<>(lemmaEntities.size());
        if (lemmaEntities.isEmpty()) {
            return pageEntities;
        }
        List<IndexEntity> indexEntities = indexRepository.findIndexEntitiesByLemmaId(lemmaEntities.get(0).getId());
        pageEntities = pageRepository.findAllById(indexEntities.stream().map(indexEntity ->
                indexEntity.getPage().getId()).collect(Collectors.toList()));
        System.out.println("Поиск первого слова - " + pageEntities.size());
        if (lemmaEntities.size() == 1) {
            System.out.println("Поиск одного слова - " + pageEntities.size());
            return pageEntities;
        }
        System.out.println("Первый поиск - " + pageEntities.size());
        lemmaEntities = lemmaEntities.stream().skip(1).collect(Collectors.toList());
        for (LemmaEntity lemmaEntity : lemmaEntities) {
            indexEntities = indexRepository.findIndexEntitiesByLemmaId(lemmaEntity.getId());
            Set<Integer> idPages = indexEntities.stream()
                    .map(indexEntity -> indexEntity.getPage().getId()).collect(Collectors.toSet());
            pageEntities = pageEntities.stream()
                    .filter(pageEntity -> idPages.contains(pageEntity.getId())).collect(Collectors.toList());
            System.out.println("Последующий поиск - " + pageEntities.size());
        }
        System.out.println("Итоговый поиск - " + pageEntities.size());
        return pageEntities;
    }

    private float getRelevance(PageEntity pageEntity, List<LemmaEntity> lemmaEntities) {
        float relevanceAbs = 0f;
        for (LemmaEntity lemmaEntity : lemmaEntities) {
            Optional<IndexEntity> optionalIndexEntity = indexRepository.findIndexEntityByPageIdAndLemmaId(pageEntity.getId(), lemmaEntity.getId());
            if (optionalIndexEntity.isEmpty()) {
                return 0f;
            }
            IndexEntity indexEntity = optionalIndexEntity.get();
            relevanceAbs += indexEntity.getRank();
        }
        return relevanceAbs;
    }

    private String getNormalFormWord(String word) {
        try {
            LemmasFromText lemmasFromText = new LemmasFromText();
            String normalFormWord = lemmasFromText.getNormalForm(word);
            System.out.println("Нормальная форма: " + normalFormWord);
            return normalFormWord;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getSnippet(String query, String textPage) {
        StringBuilder snippet = new StringBuilder();

//        String[] wordsTextPage = textPage.split("\\s+");
        Set<String> lemmasQuerySet = getLemmasText(query);
        Set<String> wordsQueryFromContent = new HashSet<>();

        Pattern pattern;
        Matcher matcher;
        for (String word : lemmasQuerySet) {
            String basisWord;
            if (word.length() <= 3) {
                basisWord = getNormalFormWord(word);
            } else {
                basisWord = getNormalFormWord(word);
                basisWord = basisWord.substring(0, basisWord.length() - 1);
            }
            pattern = Pattern.compile(basisWord);
            matcher = pattern.matcher(textPage.toLowerCase());
            while (matcher.find()) {
                int start = matcher.start();
                int end = textPage.indexOf(" ", start);
                wordsQueryFromContent.add(textPage.substring(start, end).strip());
            }
        }

        List<String> sentencesFromContent = getSentencesFromContent(textPage);
        sentencesFromContent.forEach(sentence ->
                snippet.append(getSentenceWithWordQuery(sentence, wordsQueryFromContent)));
        if (snippet.length() >= 300) {
            int start = 0;
            int end = 300;
            return snippet.substring(start, end).concat(" ...");
        }
        return snippet.toString();
    }

    private List<String> getSentencesFromContent(String textPage) {
        List<String> sentencesFromContent = new ArrayList<>();
        String regexSentence = "[A-ZА-Я][^.!?)]+[.!?]+\\s?";
        Pattern pattern = Pattern.compile(regexSentence);
        Matcher matcher = pattern.matcher(textPage);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            sentencesFromContent.add(textPage.substring(start, end));
        }
        return sentencesFromContent;
    }

    private String getSentenceWithWordQuery(String sentence, Set<String> wordsQueryFromContent) {
        String result = "";
        List<String> wordsSentence = Arrays.stream(sentence.split("\\s+")).toList();
        for (String word : wordsSentence) {
            if (!wordsQueryFromContent.contains(word)) {
                continue;
            }
            String wordsQuery = " <b>".concat(word).concat("</b> ");
            result = sentence.replaceAll(word, wordsQuery);
            sentence = result;
        }
        return result;
    }

}
