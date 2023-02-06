package ru.yandex.direct.core.entity.keyword.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.advq.AdvqClient;
import ru.yandex.direct.advq.SearchKeywordResult;
import ru.yandex.direct.advq.SearchRequest;
import ru.yandex.direct.advq.search.SearchItem;
import ru.yandex.direct.advq.search.Statistics;
import ru.yandex.direct.core.entity.adgroup.service.MinusKeywordPreparingTool;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.KeywordWithMinuses;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.keyword.service.KeywordTestUtils.getCampaignMap;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class KeywordShowsForecastServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private AdvqClient client;

    @Autowired
    private MinusKeywordPreparingTool minusKeywordPreparingTool;

    @Autowired
    private KeywordShowsForecastService serviceUnderTest;

    @Autowired
    private CampaignRepository campaignRepository;

    private KeywordInfo keywordInfo;
    private ClientId clientId;
    private ClientInfo clientInfo;

    @Before
    public void setUp() throws Exception {
        keywordInfo = steps.keywordSteps().createDefaultKeywordWithText("фраза");
        clientInfo = keywordInfo.getAdGroupInfo().getClientInfo();
        clientId = keywordInfo.getAdGroupInfo().getClientId();
    }

    @Test
    public void getPhrasesShows_CorrectResultOnAdvqClientSuccess() {
        SearchKeywordResult successResult =
                SearchKeywordResult.success(new SearchItem().withStat(new Statistics().withTotalCount(1000L)));

        when(client.search(anyCollection(), any(), any())).thenAnswer(invocation -> {
            Collection<SearchRequest> arg = invocation.getArgument(0);
            return StreamEx.of(arg)
                    .mapToEntry(SearchRequest::getKeywords)
                    .mapValues(keywords ->
                            StreamEx.of(keywords)
                                    .mapToEntry(kw -> successResult)
                                    .toMap())
                    .toCustomMap(IdentityHashMap::new);
        });

        Keyword keyword = keywordInfo.getKeyword();
        Map<Keyword, SearchKeywordResult> psr =
                serviceUnderTest.getPhrasesShows(singletonList(keyword),
                        KeywordShowsForecastService.DEFAULT_ADVQ_CALL_TIMEOUT, clientId,
                        getCampaignMap(campaignRepository, keyword));

        assertThat(psr.get(keyword).getResult().getTotalCount(), equalTo(1000L));
    }

    @Test
    public void getPhrasesShows_CorrectResultOnAdvqClientError() {
        SearchKeywordResult failResult = SearchKeywordResult.failure(singletonList(new RuntimeException("ex1")));

        when(client.search(anyCollection(), any(), any())).thenAnswer(invocation -> {
            Collection<SearchRequest> arg = invocation.getArgument(0);
            return StreamEx.of(arg)
                    .mapToEntry(SearchRequest::getKeywords)
                    .mapValues(keywords ->
                            StreamEx.of(keywords)
                                    .mapToEntry(kw -> failResult)
                                    .toMap())
                    .toCustomMap(IdentityHashMap::new);
        });

        Keyword keyword = keywordInfo.getKeyword();
        Map<Keyword, SearchKeywordResult> psr =
                serviceUnderTest.getPhrasesShows(singletonList(keyword),
                        KeywordShowsForecastService.DEFAULT_ADVQ_CALL_TIMEOUT, clientId,
                        getCampaignMap(campaignRepository, keyword));

        assertThat(psr.get(keyword).hasErrors(), equalTo(true));
        //noinspection ConstantConditions
        assertThat(psr.get(keyword).getErrors().get(0).getMessage(), equalTo("ex1"));
    }

    @Test
    public void getPhrasesShows_LibraryMinusKeywordsMerged() {
        MinusKeywordsPackInfo libraryMinusKeywordsPack =
                steps.minusKeywordsPackSteps().createMinusKeywordsPack(libraryMinusKeywordsPack()
                        .withMinusKeywords(asList("abracadabra", "kkkkk", "word 2")), clientInfo);
        MinusKeywordsPackInfo privateMinusKeywordsPack =
                steps.minusKeywordsPackSteps().createMinusKeywordsPack(libraryMinusKeywordsPack()
                        .withMinusKeywords(asList("bararara", "cdcdcd", "word 1")), clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup()
                        .withMinusKeywordsId(privateMinusKeywordsPack.getMinusKeywordPackId())
                        .withMinusKeywords(privateMinusKeywordsPack.getMinusKeywordsPack().getMinusKeywords())
                        .withLibraryMinusKeywordsIds(singletonList(libraryMinusKeywordsPack.getMinusKeywordPackId())),
                clientInfo);
        Keyword keyword = steps.keywordSteps().createKeywordWithText("фраза", adGroupInfo).getKeyword();

        serviceUnderTest.getPhrasesShows(singletonList(keyword), KeywordShowsForecastService.DEFAULT_ADVQ_CALL_TIMEOUT,
                clientId, getCampaignMap(campaignRepository, keyword));

        ArgumentCaptor<Collection<SearchRequest>> clientRequestCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(client, atLeastOnce()).search(clientRequestCaptor.capture(), any(), any());

        String expected = getExpectedKeywordWithMinuses(keyword, privateMinusKeywordsPack, libraryMinusKeywordsPack);
        List<SearchRequest> resultList = new ArrayList<>(clientRequestCaptor.getValue());
        assertThat(resultList.get(0).getKeywords().get(0).getPhrase(), is(expected));
    }

    private String getExpectedKeywordWithMinuses(Keyword keyword, MinusKeywordsPackInfo privatePack,
                                                 MinusKeywordsPackInfo libraryPack) {
        List<String> allMinusKeywords = minusKeywordPreparingTool
                .mergePrivateAndLibrary(privatePack.getMinusKeywordsPack().getMinusKeywords(),
                        singletonList(libraryPack.getMinusKeywordsPack().getMinusKeywords()));
        KeywordWithMinuses keywordWithMinuses = KeywordWithMinuses.fromPhrase(keyword.getPhrase());
        keywordWithMinuses.addMinusKeywords(allMinusKeywords);
        return serviceUnderTest.keywordToAdvqFormat(keywordWithMinuses);
    }
}
