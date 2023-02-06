package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.IdentityHashMap;
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
import ru.yandex.direct.core.entity.auction.container.bs.KeywordBidBsAuctionData;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordBsAuctionService;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
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
import static ru.yandex.direct.core.entity.keyword.service.KeywordAutoPricesCalculator.AUTOBROKER_MULTIPLIER;
import static ru.yandex.direct.core.entity.keyword.service.KeywordTestUtils.getCampaignMap;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.BS_TRAFARET_AUCTION_CLIENT_WEB;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerAddKeywordPricesTest extends TextAdGroupControllerTestBase {
    @Autowired
    @Qualifier(BS_TRAFARET_AUCTION_CLIENT_WEB)
    private BsTrafaretClient bsTrafaretClientWeb;

    @Autowired
    private KeywordBsAuctionService keywordBsAuctionService;

    @Before
    public void before() {
        super.before();
        when(bsTrafaretClientWeb.getAuctionResultsWithPositionCtrCorrection(anyList())).thenAnswer(
                invocation -> {
                    List<BsRequest<BsRequestPhrase>> requests = invocation.getArgument(0);
                    return generateDefaultBsAuctionResponse(requests);
                }
        );
    }

    @Test
    public void autoCommonPriceTest() {
        double commonPrice = 12.0;
        WebTextAdGroup requestAdGroup = makeAdGroup();
        requestAdGroup.withGeneralPrice(commonPrice);
        requestAdGroup.getKeywords().get(0).withPrice(null).withPriceContext(null);

        addAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> addedKeywords = findKeywords();
        assertThat("должна быть добавлена одна фраза", addedKeywords, hasSize(1));
        assertThat("цена фразы на поиске не совпадает с ожидаемой",
                moneyOf(addedKeywords.get(0).getPrice()), equalTo(moneyOf(commonPrice)));
        assertThat("цена фразы в сети не совпадает с ожидаемой",
                moneyOf(addedKeywords.get(0).getPriceContext()), equalTo(moneyOf(commonPrice)));
    }

    @Test
    public void autoAuctionPriceTest() {
        WebTextAdGroup requestAdGroup = makeAdGroup();
        requestAdGroup.withGeneralPrice(null);
        requestAdGroup.getKeywords().get(0).withPrice(null).withPriceContext(null);

        addAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> addedKeywords = findKeywords();
        Money expectedPrice = getFirstGuaranteePrice(addedKeywords.get(0))
                .multiply(AUTOBROKER_MULTIPLIER).roundToAuctionStepUp();
        assertThat("должна быть добавлена одна фраза", addedKeywords, hasSize(1));
        assertThat("цена фразы на поиске не совпадает с ожидаемой",
                moneyOf(addedKeywords.get(0).getPrice()), equalTo(expectedPrice));
    }

    @Test
    public void autoContextPriceTest() {
        WebTextAdGroup requestAdGroup = makeAdGroup();
        requestAdGroup.withGeneralPrice(null);
        requestAdGroup.getKeywords().get(0).withPrice(null).withPriceContext(null);

        addAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> addedKeywords = findKeywords();
        assertThat("должна быть добавлена одна группа", addedKeywords, hasSize(1));
        assertThat("данные добавленной группы не совпадает с ожидаемыми",
                Money.valueOf(addedKeywords.get(0).getPriceContext(), clientCurrency.getCode()),
                equalTo(Money.valueOf(clientCurrency.getDefaultPrice(), clientCurrency.getCode())));
    }

    private WebTextAdGroup makeAdGroup() {
        return TestWebAdGroupBuilder.someWebAdGroup(campaignInfo.getCampaignId())
                .withSomeBanner()
                .withSomeKeyword()
                .build();
    }

    private void addAndCheckResult(List<WebTextAdGroup> requestAdGroups) {
        WebResponse response = controller.saveTextAdGroup(requestAdGroups, campaignInfo.getCampaignId(),
                true, false, false, null, null);
        checkResponse(response);
    }

    private Money getFirstGuaranteePrice(Keyword kw) {
        Currency currency = clientService.getWorkCurrency(clientId);
        IdentityHashMap<Keyword, KeywordBidBsAuctionData> auctionResult =
                keywordBsAuctionService.getBsAuctionData(clientId, singletonList(kw), currency,
                        getCampaignMap(campaignRepository, kw));
        return auctionResult.get(kw).getGuarantee().first().getBidPrice();
    }
}

