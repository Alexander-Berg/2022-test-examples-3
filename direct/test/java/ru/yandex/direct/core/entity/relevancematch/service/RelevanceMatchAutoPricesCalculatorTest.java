package ru.yandex.direct.core.entity.relevancematch.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.KeywordRecentStatistics;
import ru.yandex.direct.core.entity.keyword.service.KeywordRecentStatisticsProvider;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchAutoPricesCalculatorTest {

    @Autowired
    private Steps steps;
    @Autowired
    private KeywordService keywordService;
    @Autowired
    private CampaignService campaignService;

    private ClientInfo clientInfo;
    private ClientId clientId;
    private Currency clientCurrency;
    private double defaultPrice;
    private double minPrice;

    private Map<Long, KeywordRecentStatistics> hereThereStatistics = new HashMap<>();

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        clientCurrency = clientInfo.getClient().getWorkCurrency().getCurrency();
        defaultPrice = clientCurrency.getDefaultPrice().doubleValue();
        minPrice = clientCurrency.getMinPrice().doubleValue();
    }

    /**
     * Если в группе нет ключевых фраз, выставляется ставка по умолчанию.
     */
    @Test
    public void calcPrice_noKeywords_defaultPrices() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        RelevanceMatch relevanceMatch = makeRelevanceMatch(adGroupInfo);

        RelevanceMatchAutoPricesCalculator calculator = makeCalculator();
        calculator.calcAutoPricesInAdd(singleton(relevanceMatch), mapCampaigns(campaignInfo));

        assertRmPrices(relevanceMatch, defaultPrice, defaultPrice);
    }

     /**
     * Если у кампании автоматическая стратегия, ставки не считаются.
     */
    @Test
    public void calcPrice_noKeywordsAutoStrategy_noPrices() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaignAutoStrategy(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        RelevanceMatch relevanceMatch = makeRelevanceMatch(adGroupInfo);

        RelevanceMatchAutoPricesCalculator calculator = makeCalculator();
        calculator.calcAutoPricesInAdd(singleton(relevanceMatch), mapCampaigns(campaignInfo));
        assertNoPrices(relevanceMatch);
    }

    /**
     * Если нужно посчитать ставки для автотаргетингов из разных групп,
     * ставки считаются на основе ставок фраз в соответствующих группах.
     */
    @Test
    public void calcPrice_twoKeywords_calcPricesFromKeywords() {
        CampaignInfo campaignInfo1 = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroupInfo adGroupInfo1 = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo1);
        createKeyword(adGroupInfo1, 123.0, 456.0);
        createKeyword(adGroupInfo1, 321.0, 654.0);
        CampaignInfo campaignInfo2 = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroupInfo adGroupInfo2 = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo2);
        createKeyword(adGroupInfo2, 234.0, 567.0);
        createKeyword(adGroupInfo2, 432.0, 765.0);
        RelevanceMatch relevanceMatch1 = makeRelevanceMatch(adGroupInfo1);
        RelevanceMatch relevanceMatch2 = makeRelevanceMatch(adGroupInfo2);

        double expectedSearchPrice1 = 123 + (321 - 123) * 0.3;
        double expectedContextPrice1 = (456 + 654) / 2.0;
        double expectedSearchPrice2 = 234 + (432 - 234) * 0.3;
        double expectedContextPrice2 = (567 + 765) / 2.0;

        RelevanceMatchAutoPricesCalculator calculator = makeCalculator();
        calculator.calcAutoPricesInAdd(asList(relevanceMatch1, relevanceMatch2),
                mapCampaigns(campaignInfo1, campaignInfo2));
        assertRmPrices(relevanceMatch1, expectedSearchPrice1, expectedContextPrice1);
        assertRmPrices(relevanceMatch2, expectedSearchPrice2, expectedContextPrice2);
    }

    /**
     * Автоматическая ставка в сети считается с учетом статистики кликов
     * по фразе в сети.
     */
    @Test
    public void calcPrices_keywordsWithStats_calcPriceContextFromKeywordsWithStats() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        KeywordInfo kw1 = createKeyword(adGroupInfo, 123.0, 456.0);
        KeywordInfo kw2 = createKeyword(adGroupInfo, 321.0, 654.0);
        hereThereStatistics.put(kw1.getId(), new FakeStats(120L, 130L, 140L, 150L, 160L, 170L));
        hereThereStatistics.put(kw2.getId(), new FakeStats(220L, 230L, 240L, 250L, 260L, 270L));

        RelevanceMatch relevanceMatch = makeRelevanceMatch(adGroupInfo);
        double expectedSearchPrice = 123 + (321 - 123) * 0.3;
        double expectedContextPrice = (456 * 161 + 654 * 261) / (161.0 + 261.0);

        RelevanceMatchAutoPricesCalculator calculator = makeCalculator();
        calculator.calcAutoPricesInAdd(singletonList(relevanceMatch), mapCampaigns(campaignInfo));
        assertRmPrices(relevanceMatch, expectedSearchPrice, expectedContextPrice);
    }

    /**
     * Если у фраз в группе нет ставок в сети, в расчеты берутся ставки
     * фраз на поиске и статистика по кликам по этим фразам на поиске.
     */
    @Test
    public void calcContextPrice_keywordsWithoutPriceContext_useKeywordsSearchPricesWithStats() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        KeywordInfo kw1 = createKeyword(adGroupInfo, 123.0, null);
        KeywordInfo kw2 = createKeyword(adGroupInfo, 321.0, null);
        steps.keywordSteps().resetKeywordsContextPrices(clientInfo.getShard(), asList(kw1.getId(), kw2.getId()));
        hereThereStatistics.put(kw1.getId(), new FakeStats(120L, 130L, 140L, 150L, 160L, 170L));
        hereThereStatistics.put(kw2.getId(), new FakeStats(220L, 230L, 240L, 250L, 260L, 270L));

        RelevanceMatch relevanceMatch = makeRelevanceMatch(adGroupInfo);
        double expectedSearchPrice = 123 + (321 - 123) * 0.3;
        double expectedContextPrice = (123 * 131 + 321 * 231) / (131.0 + 231.0);

        RelevanceMatchAutoPricesCalculator calculator = makeCalculator();
        calculator.calcAutoPricesInAdd(singletonList(relevanceMatch), mapCampaigns(campaignInfo));
        assertRmPrices(relevanceMatch, expectedSearchPrice, expectedContextPrice);
    }

    /**
     * Если у всех фраз в группе нет никаких ставок, выставляются минимальные ставки.
     */
    @Test
    public void calcPrices_keywordsWithoutPrices_defaultPrices() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        KeywordInfo kw1 = createKeyword(adGroupInfo, null, null);
        KeywordInfo kw2 = createKeyword(adGroupInfo, null, null);
        steps.keywordSteps().resetKeywordsContextPrices(clientInfo.getShard(), asList(kw1.getId(), kw2.getId()));
        steps.keywordSteps().resetKeywordsSearchPrices(clientInfo.getShard(), asList(kw1.getId(), kw2.getId()));
        hereThereStatistics.put(kw1.getId(), new FakeStats(120L, 130L, 140L, 150L, 160L, 170L));
        hereThereStatistics.put(kw2.getId(), new FakeStats(220L, 230L, 240L, 250L, 260L, 270L));

        RelevanceMatch relevanceMatch = makeRelevanceMatch(adGroupInfo);
        double expectedSearchPrice = minPrice;
        double expectedContextPrice = minPrice;

        RelevanceMatchAutoPricesCalculator calculator = makeCalculator();
        calculator.calcAutoPricesInAdd(singletonList(relevanceMatch), mapCampaigns(campaignInfo));
        assertRmPrices(relevanceMatch, expectedSearchPrice, expectedContextPrice);
    }

    /**
     * Если в группе есть одна фраза, у которой минимальные ставки,
     * автоматические ставки тоже должны быть минимальными
     * (по крайней мере, не меньше)
     */
    @Test
    public void calcPrice_keywordWithMinPrice_minPrices() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        double minPrice = clientCurrency.getMinPrice().doubleValue();
        createKeyword(adGroupInfo, minPrice, minPrice);
        RelevanceMatch relevanceMatch = makeRelevanceMatch(adGroupInfo);

        RelevanceMatchAutoPricesCalculator calculator = makeCalculator();
        calculator.calcAutoPricesInAdd(Collections.singletonList(relevanceMatch), mapCampaigns(campaignInfo));
        assertRmPrices(relevanceMatch, minPrice, minPrice);
    }

    @Test
    public void calcContextPrice_withRoundingError() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        double price = 3;
        createKeyword(adGroupInfo, price, price);
        createKeyword(adGroupInfo, price, price);
        createKeyword(adGroupInfo, price, price);
        RelevanceMatch relevanceMatch = makeRelevanceMatch(adGroupInfo);

        RelevanceMatchAutoPricesCalculator calculator = makeCalculator();
        calculator.calcAutoPricesInAdd(Collections.singletonList(relevanceMatch), mapCampaigns(campaignInfo));
        assertRmPrices(relevanceMatch, price, price);
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
        return Money.valueOf(price, clientCurrency.getCode()).roundToAuctionStepUp().bigDecimalValue();
    }

    private RelevanceMatchAutoPricesCalculator makeCalculator() {
        KeywordRecentStatisticsProvider statisticsProvider = new MockKeywordRecentStatisticsProvider();

        return new RelevanceMatchAutoPricesCalculator(
                statisticsProvider, keywordService, clientCurrency, clientId
        );
    }

    private RelevanceMatch makeRelevanceMatch(AdGroupInfo adGroupInfo) {
        return new RelevanceMatch()
                .withCampaignId(adGroupInfo.getCampaignId())
                .withAdGroupId(adGroupInfo.getAdGroupId());
    }

    private Map<Long, Campaign> mapCampaigns(CampaignInfo... campaigns) {
        List<Long> campaignIds = mapList(asList(campaigns), CampaignInfo::getCampaignId);
        List<Campaign> campaignList = campaignService.getCampaignsWithStrategies(clientId, campaignIds);
        return StreamEx.of(campaignList)
                .toMap(Campaign::getId, identity());
    }

    private void assertRmPrices(RelevanceMatch relevanceMatch, double searchPrice, Double contextPrice) {
        assertThat(relevanceMatch.getPrice(), notNullValue());
        assertThat(bd(relevanceMatch.getPrice().doubleValue()), is(bd(searchPrice)));
        if (contextPrice == null) {
            assertThat(relevanceMatch.getPriceContext(), nullValue());
        } else {
            assertThat(relevanceMatch.getPriceContext(), notNullValue());
            assertThat(bd(relevanceMatch.getPriceContext().doubleValue()), is(bd(contextPrice)));
        }

    }

    private void assertNoPrices(RelevanceMatch relevanceMatch) {
        assertThat(relevanceMatch.getPrice(), nullValue());
        assertThat(relevanceMatch.getPriceContext(), nullValue());
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

    private class FakeStats implements KeywordRecentStatistics {
        private Long searchShows;
        private Long searchClicks;
        private Long searchEshows;
        private Long networkShows;
        private Long networkClicks;
        private Long networkEshows;

        public FakeStats(Long searchShows, Long searchClicks, Long searchEshows, Long networkShows,
                         Long networkClicks, Long networkEshows) {
            this.searchShows = searchShows;
            this.searchClicks = searchClicks;
            this.searchEshows = searchEshows;
            this.networkShows = networkShows;
            this.networkClicks = networkClicks;
            this.networkEshows = networkEshows;
        }

        @Override
        public Long getSearchShows() {
            return searchShows;
        }

        @Override
        public Long getSearchClicks() {
            return searchClicks;
        }

        @Override
        public Double getSearchEshows() {
            return searchEshows.doubleValue();
        }

        @Override
        public Long getNetworkShows() {
            return networkShows;
        }

        @Override
        public Long getNetworkClicks() {
            return networkClicks;
        }

        @Override
        public Double getNetworkEshows() {
            return networkEshows.doubleValue();
        }
    }
}
