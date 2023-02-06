package ru.yandex.direct.web.entity.keyword.stat.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.advq.SearchKeywordResult;
import ru.yandex.direct.advq.exception.AdvqClientException;
import ru.yandex.direct.advq.search.AdvqRequestKeyword;
import ru.yandex.direct.advq.search.SearchItem;
import ru.yandex.direct.advq.search.Statistics;
import ru.yandex.direct.advq.search.StatisticsPhraseItem;
import ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebErrorResponse;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.keyword.stat.model.KeywordStatShowsBulkEntry;
import ru.yandex.direct.web.entity.keyword.stat.model.KeywordStatShowsBulkResponse;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.web.entity.keyword.stat.service.KeywordStatShowsConverter.toKeywordStatShowsItemsList;

@DirectWebTest
@RunWith(Parameterized.class)
public class KeywordStatShowsResponseConvertTest {

    private static final String PHRASE = "купить слона";
    private static final WebResponse ERROR_RESPONSE =
            new WebErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "search error");
    private static final GdAdGroupType AD_GROUP_TYPE = GdAdGroupType.TEXT;
    private static final  Predicate<String> TRUE_PREDICATE = (t) -> true;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();


    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private KeywordStatShowsConverter keywordStatShowsConverter;

    @Parameterized.Parameter
    public String testDescription;

    @Parameterized.Parameter(1)
    public SearchKeywordResult searchKeywordResult;

    @Parameterized.Parameter(2)
    public WebResponse expectedResponse;

    @Parameterized.Parameters(name = "{0}")
    public static Collection params() {
        SearchItem searchItem = generateSearchItem();
        return asList(new Object[][]{
                {"get error response for null result", null, ERROR_RESPONSE},
                {"get error response for empty result", SearchKeywordResult.empty(), ERROR_RESPONSE},
                {"get error response for failure result",
                        SearchKeywordResult.failure(singletonList(new RuntimeException())), ERROR_RESPONSE},
                {"get error response for result with null searchItem",
                        SearchKeywordResult.success(null), ERROR_RESPONSE},
                {"get error response for result with null stat",
                        SearchKeywordResult.success(new SearchItem()), ERROR_RESPONSE},
                {"get success response",
                        SearchKeywordResult.success(searchItem), searchItemToResponse(searchItem)},
        });
    }

    private static SearchItem generateSearchItem() {
        Statistics statistics = new Statistics();
        StatisticsPhraseItem includingPhraseItem = new StatisticsPhraseItem();
        includingPhraseItem.setPhrase(RandomStringUtils.randomAlphanumeric(11));
        includingPhraseItem.setCnt(RandomNumberUtils.nextPositiveLong());
        statistics.setIncludingPhrases(singletonList(includingPhraseItem));

        StatisticsPhraseItem associationsPhraseItem = new StatisticsPhraseItem();
        associationsPhraseItem.setPhrase(RandomStringUtils.randomAlphanumeric(11));
        associationsPhraseItem.setCnt(RandomNumberUtils.nextPositiveLong());
        statistics.setAssociations(singletonList(associationsPhraseItem));

        statistics.setErrors(singletonList("some non critical error"));
        statistics.setTotalCount(RandomNumberUtils.nextPositiveLong());

        return new SearchItem()
                .withStat(statistics);
    }

    private static WebResponse searchItemToResponse(SearchItem item) {

        KeywordStatShowsBulkEntry entry = new KeywordStatShowsBulkEntry()
                .withShows(item.getTotalCount())
                .withAdvqErrors(item.getStat().getErrors())
                .withSearchedWith(
                        toKeywordStatShowsItemsList(item.getStat().getIncludingPhrases(), AD_GROUP_TYPE, TRUE_PREDICATE))
                .withSearchedAlso(
                        toKeywordStatShowsItemsList(item.getStat().getAssociations(), AD_GROUP_TYPE, TRUE_PREDICATE));
        return new KeywordStatShowsBulkResponse(singletonList(entry));
    }

    @Test
    public void convertToResponse() {
        Map<AdvqRequestKeyword, SearchKeywordResult> resultMap = new HashMap<>();
        resultMap.put(new AdvqRequestKeyword(PHRASE), searchKeywordResult);
        WebResponse response = getResponse(resultMap);
        assertThat(response).is(matchedBy(beanDiffer(expectedResponse)));
    }

    private WebResponse getResponse(Map<AdvqRequestKeyword, SearchKeywordResult> resultMap) {
        try {
            return keywordStatShowsConverter
                    .convertToResponse(singletonList(new AdvqRequestKeyword(PHRASE)), resultMap, AD_GROUP_TYPE, TRUE_PREDICATE);
        } catch (AdvqClientException e) {
            return ERROR_RESPONSE;
        }
    }

}
