package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.bsauction.BsRequest;
import ru.yandex.direct.bsauction.BsRequestPhrase;
import ru.yandex.direct.bsauction.BsTrafaretClient;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.keyword.service.KeywordBsAuctionService;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.data.TestWebAdGroupBuilder;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.BS_TRAFARET_AUCTION_CLIENT;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerAddRelMatchPricesTest extends TextAdGroupControllerTestBase {

    private static final double FIRST_PHRASE_PRICE = 10.0;
    private static final double FIRST_PHRASE_PRICE_CONTEXT = 20.0;
    private static final double SECOND_PHRASE_PRICE = 30.0;
    private static final double SECOND_PHRASE_PRICE_CONTEXT = 40.0;

    // 30-й перцентиль поисковых ставок ключевых фраз
    private static final double RM_PRICE = 16.0;
    // 0.8 от средней ставки по фразам
    private static final double RM_PRICE_CONTEXT = (FIRST_PHRASE_PRICE_CONTEXT + SECOND_PHRASE_PRICE_CONTEXT)/2;

    private static final double ADGROUP_FIXED_PRICE = 123.0;

    @Autowired
    @Qualifier(BS_TRAFARET_AUCTION_CLIENT)
    private BsTrafaretClient bsTrafaretClient;

    @Autowired
    private KeywordBsAuctionService keywordBsAuctionService;

    @Before
    public void before() {
        super.before();
        when(bsTrafaretClient.getAuctionResultsWithPositionCtrCorrection(anyList())).thenAnswer(
                invocation -> {
                    List<BsRequest<BsRequestPhrase>> requests = invocation.getArgument(0);
                    return generateDefaultBsAuctionResponse(requests);
                }
        );
    }

    @Test
    public void autoPriceTest() {
        WebTextAdGroup requestAdGroup = makeAdGroup();

        addAndCheckResult(singletonList(requestAdGroup));

        List<AdGroup> addedAgGroups = findAdGroups();
        List<RelevanceMatch> addedRelMatches = findRelevanceMatches(addedAgGroups.get(0).getId());
        assertThat("должен быть добавлен бесфразный таргетинг", addedRelMatches, hasSize(1));
        assertThat("цена бесфразного таргетинга на поиске не совпадает с ожидаемой",
                moneyOf(addedRelMatches.get(0).getPrice()), equalTo(moneyOf(RM_PRICE)));
        assertThat("цена фразы в сети не совпадает с ожидаемой",
                moneyOf(addedRelMatches.get(0).getPriceContext()), equalTo(moneyOf(RM_PRICE_CONTEXT)));
    }

    @Test
    public void fixedPriceTest() {
        WebTextAdGroup requestAdGroup = makeAdGroup();
        requestAdGroup.withGeneralPrice(ADGROUP_FIXED_PRICE);

        addAndCheckResult(singletonList(requestAdGroup));

        List<AdGroup> addedAgGroups = findAdGroups();
        List<RelevanceMatch> addedRelMatches = findRelevanceMatches(addedAgGroups.get(0).getId());
        assertThat("должен быть добавлен бесфразный таргетинг", addedRelMatches, hasSize(1));
        assertThat("цена бесфразного таргетинга на поиске не совпадает с ожидаемой",
                moneyOf(addedRelMatches.get(0).getPrice()), equalTo(moneyOf(ADGROUP_FIXED_PRICE)));
        assertThat("цена фразы в сети не совпадает с ожидаемой",
                moneyOf(addedRelMatches.get(0).getPriceContext()), equalTo(moneyOf(ADGROUP_FIXED_PRICE)));
    }

    private WebTextAdGroup makeAdGroup() {
        return TestWebAdGroupBuilder.someWebAdGroup(campaignInfo.getCampaignId())
                .withSomeBanner()
                .withSomeKeyword(kw -> kw.withPrice(FIRST_PHRASE_PRICE).withPriceContext(FIRST_PHRASE_PRICE_CONTEXT))
                .withSomeKeyword(kw -> kw.withPrice(SECOND_PHRASE_PRICE).withPriceContext(SECOND_PHRASE_PRICE_CONTEXT))
                .withSomeRelMatch()
                .build();
    }

    private void addAndCheckResult(List<WebTextAdGroup> requestAdGroups) {
        WebResponse response = controller.saveTextAdGroup(requestAdGroups, campaignInfo.getCampaignId(),
                true, false, false, null, null);
        checkResponse(response);
    }
}

