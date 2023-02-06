package ru.yandex.direct.web.entity.keyword.stat.service;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.ListUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.advq.AdvqClient;
import ru.yandex.direct.advq.AdvqSearchOptions;
import ru.yandex.direct.advq.SearchKeywordResult;
import ru.yandex.direct.advq.SearchRequest;
import ru.yandex.direct.advq.search.AdvqRequestKeyword;
import ru.yandex.direct.advq.search.SearchItem;
import ru.yandex.direct.advq.search.Statistics;
import ru.yandex.direct.advq.search.StatisticsPhraseItem;
import ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.keyword.stat.model.KeywordStatShowsBulkEntry;
import ru.yandex.direct.web.entity.keyword.stat.model.KeywordStatShowsBulkRequest;
import ru.yandex.direct.web.entity.keyword.stat.model.KeywordStatShowsBulkResponse;
import ru.yandex.direct.web.entity.keyword.stat.model.KeywordStatShowsItem;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordStatShowsServiceConvertedTest {

    private static final String DEFAULT_GEO = String.valueOf(Region.RUSSIA_REGION_ID);
    private static final String PHRASE = "some phrase";
    private static final String MINUS_PHRASE = "minus";
    private static final String SEARCH_PHRASE = PHRASE + " -" + MINUS_PHRASE;
    private static final GdAdGroupType GD_AD_GROUP_TYPE = GdAdGroupType.TEXT;
    private static final long SHOWS_VALUE = 1L;

    @Autowired
    private AdvqClient advqClient;
    @Autowired
    private KeywordStatShowsService keywordStatShowsService;

    private static ImmutableMap<AdvqRequestKeyword, SearchKeywordResult> getSearchValue(List<String> phrases) {
        List<StatisticsPhraseItem> includingPhrases = mapList(phrases,
                p -> {
                    StatisticsPhraseItem pi = new StatisticsPhraseItem();
                    pi.setCnt(SHOWS_VALUE);
                    pi.setPhrase(p);
                    return pi;
                });
        Statistics stat = new Statistics();
        stat.setIncludingPhrases(includingPhrases);
        SearchItem searchItem = new SearchItem()
                .withStat(stat);
        SearchKeywordResult keywordResult = SearchKeywordResult.success(searchItem);
        return ImmutableMap.of(new AdvqRequestKeyword(SEARCH_PHRASE), keywordResult);
    }

    private static KeywordStatShowsBulkRequest getRequest() {
        return new KeywordStatShowsBulkRequest()
                .withPhrases(singletonList(PHRASE))
                .withCommonMinusPhrases(singletonList(MINUS_PHRASE))
                .withGeo(DEFAULT_GEO)
                .withAdGroupType(GD_AD_GROUP_TYPE)
                .withNeedSearchedWith(false);
    }

    private static KeywordStatShowsBulkResponse getExpectedResponse(List<String> phrases) {
        return new KeywordStatShowsBulkResponse(
                singletonList(
                        new KeywordStatShowsBulkEntry()
                                .withSearchedAlso(emptyList())
                                .withShows(0L)
                                .withSearchedWith(
                                        mapList(phrases,
                                                p -> new KeywordStatShowsItem()
                                                        .withPhrase(p)
                                                        .withShows(SHOWS_VALUE)))));
    }

    @Test
    public void getKeywordStatShowsBulk_withPhrases_success() {
        KeywordStatShowsBulkRequest bulkRequest = getRequest();
        List<String> phrases = List.of("test1 test2 test3 test4 test5 test6",
                "test1 test2 test3 test4 test5 test6 test7 ");
        ImmutableMap<AdvqRequestKeyword, SearchKeywordResult> searchValue = getSearchValue(phrases);
        mockAdvqClientSearchValue(searchValue);

        KeywordStatShowsBulkResponse expectedResponse = getExpectedResponse(phrases);

        WebResponse actualResponse = keywordStatShowsService.getKeywordStatShowsBulk(bulkRequest);

        assertThat(actualResponse)
                .is(matchedBy(beanDiffer(expectedResponse).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void getKeywordStatShowsBulk_whenAdvqClientPhrasesIsTooLong_success() {
        KeywordStatShowsBulkRequest bulkRequest = getRequest();
        List<String> shortPhrases = List.of("test1 test2 test3 test4 test5 test6",
                "test1 test2 test3 test4 test5 test6 test7 ");
        List<String> longPhrases = List.of("test1 test2 test3 test4 test5 test6 test7 test8",
                "test1 test2 test3 test4 test5 test6 test7 test8 test9");
        List<String> phrases = ListUtils.union(shortPhrases, longPhrases);
        ImmutableMap<AdvqRequestKeyword, SearchKeywordResult> searchValue = getSearchValue(phrases);
        mockAdvqClientSearchValue(searchValue);

        KeywordStatShowsBulkResponse expectedResponse = getExpectedResponse(shortPhrases);

        WebResponse actualResponse = keywordStatShowsService.getKeywordStatShowsBulk(bulkRequest);

        assertThat(actualResponse)
                .is(matchedBy(beanDiffer(expectedResponse).useCompareStrategy(onlyExpectedFields())));
    }

    private void mockAdvqClientSearchValue(ImmutableMap<AdvqRequestKeyword, SearchKeywordResult> searchValue) {
        when(advqClient.search(anyCollection(), any(AdvqSearchOptions.class)))
                .thenAnswer(args -> {
                    Map<SearchRequest, Map<AdvqRequestKeyword, SearchKeywordResult>> advqResults = new IdentityHashMap<>();
                    Collection<SearchRequest> requests = args.getArgument(0);
                    requests.forEach(request -> advqResults.put(request, searchValue));
                    return advqResults;
                });
    }
}
