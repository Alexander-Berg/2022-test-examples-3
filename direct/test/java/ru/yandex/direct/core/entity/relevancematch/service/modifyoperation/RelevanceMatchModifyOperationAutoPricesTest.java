package ru.yandex.direct.core.entity.relevancematch.service.modifyoperation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.KeywordRecentStatistics;
import ru.yandex.direct.core.entity.keyword.service.KeywordRecentStatisticsProvider;
import ru.yandex.direct.core.entity.relevancematch.container.RelevanceMatchModification;
import ru.yandex.direct.core.entity.relevancematch.container.RelevanceMatchModificationResult;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchModificationBaseTest;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionAutoPriceParams;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionFixedAutoPrices;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.result.Result;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchModifyOperationAutoPricesTest extends RelevanceMatchModificationBaseTest {

    private static final BigDecimal FIXED_AUTO_PRICE = BigDecimal.valueOf(123).setScale(2, RoundingMode.UNNECESSARY);

    @Autowired
    private Steps steps;
    @Autowired
    private RelevanceMatchRepository relevanceMatchRepository;

    private ClientInfo clientInfo;
    private Currency clientCurrency = CurrencyRub.getInstance();

    private Map<Long, KeywordRecentStatistics> hereThereStatistics = new HashMap<>();

    /**
     * При модификации автотаргетингов с добавлением и указанием фиксированной
     * ставки, у добавленного автотаргетинга выставляется фиксированная ставка.
     * Тут проверяется, что контейнер с параметрами автоставок корректно
     * передается в операцию добавления.
     */
    @Test
    public void apply_addWithFixedAutoPrice_setFixedPrice() {
        clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        RelevanceMatch relevanceMatch = makeRelevanceMatch(adGroupInfo);

        RelevanceMatchModification rmm = new RelevanceMatchModification()
                .withRelevanceMatchAdd(Collections.singletonList(relevanceMatch))
                .withRelevanceMatchUpdate(Collections.emptyList())
                .withRelevanceMatchIdsDelete(Collections.emptyList());

        Result<RelevanceMatchModificationResult> result = apply(rmm);
        Long rmId = result.getResult().getRelevanceMatchAddResult().get(0);
        Map<Long, RelevanceMatch>
                relevanceMatchMap = relevanceMatchRepository
                .getRelevanceMatchesByIds(clientInfo.getShard(), clientInfo.getClientId(), Collections.singleton(rmId));
        RelevanceMatch actualRelevanceMatch = relevanceMatchMap.get(rmId);
        assertThat(actualRelevanceMatch.getPrice(), is(FIXED_AUTO_PRICE));
    }

    private KeywordInfo createKeyword(AdGroupInfo adGroupInfo, Double searchPrice, Double contextPrice) {
        return steps.keywordSteps().createModifiedKeyword(
                adGroupInfo,
                kw -> kw.withPrice(bd(searchPrice)).withPriceContext(bd(contextPrice))
        );
    }

    private BigDecimal bd(Double price) {
        if (price == null) {
            return null;
        }
        return Money.valueOf(price, clientCurrency.getCode()).roundToAuctionStepUp().bigDecimalValue()
                .setScale(2, RoundingMode.UNNECESSARY);
    }

    private Result<RelevanceMatchModificationResult> apply(RelevanceMatchModification rmm) {
        ShowConditionAutoPriceParams autoPriceParams = new ShowConditionAutoPriceParams(
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE),
                new MockKeywordRecentStatisticsProvider()
        );
        return relevanceMatchService.createFullModifyOperation(
                clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(),
                rmm, true, autoPriceParams
        ).prepareAndApply();
    }

    private RelevanceMatch makeRelevanceMatch(AdGroupInfo adGroupInfo) {
        return new RelevanceMatch()
                .withCampaignId(adGroupInfo.getCampaignId())
                .withAdGroupId(adGroupInfo.getAdGroupId());
    }

    private class MockKeywordRecentStatisticsProvider implements KeywordRecentStatisticsProvider {
        @Override
        public Map<Long, KeywordRecentStatistics> getKeywordRecentStatistics(Collection<Keyword> keywordRequests) {
            return StreamEx.of(keywordRequests)
                    .filter(kw -> hereThereStatistics.containsKey(kw.getId()))
                    .mapToEntry(Keyword::getId, kw -> hereThereStatistics.get(kw.getId()))
                    .toMap();
        }
    }

}
