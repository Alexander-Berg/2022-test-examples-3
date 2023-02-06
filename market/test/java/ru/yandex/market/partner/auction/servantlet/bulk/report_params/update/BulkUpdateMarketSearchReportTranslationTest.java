package ru.yandex.market.partner.auction.servantlet.bulk.report_params.update;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ru.yandex.market.core.auction.recommend.BidRecommendationRequest;
import ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasRegionId;
import static ru.yandex.market.core.auction.model.AuctionGoalPlace.PREMIUM_FIRST_PLACE;
import static ru.yandex.market.partner.auction.HybridGoal.GoalType.MARKET_SEARCH;

/**
 * Проверка трансляции параметров в {@link AuctionBulkOfferBidsServantlet} в запросах установки ставок.
 * Проверяем, что параметры переданные в рекомендатор для поиска на макрете(в виде mock-ов), соответсвуют ожиданиям.
 */
@ExtendWith(MockitoExtension.class)
class BulkUpdateMarketSearchReportTranslationTest extends AuctionBulkServantletlMockBase {

    @BeforeEach
    void beforeEach() {
        auctionBulkOfferBidsServantlet.configure();
        generalBulkServantletInit(recommendationsService);

        mockBidLimits();
        mockRegionsAndTariff();

        mockAuctionExistingBid(SOME_TITLE_OFFER_ID, PARAM_DATASOURCE_ID);
        mockCheckHomeRegionInIndex();
        mockRecommendationServiceEmptyCalculateResult();
        mockOfferExists();

        mockServRequestCrudActionUPDATE();
        mockServRequestIdentificationParams();
    }

    /**
     * Установка ставок на основе рекомендаций для маркетного поиска.
     * Регион задан явно в запросе -> в рекоммендатор передается заданное значение.
     */
    @Test
    void test_updateBid_when_regionSpecifiedInMarketSearchGoalUpdate_should_sendReportRequestWithPassedRegion() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.goal.type=" + MARKET_SEARCH +
                "&req1.goal.value=" + PREMIUM_FIRST_PLACE +
                "&regionId=" + PARAM_REGION_ID
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest parallelRecRequest = extractRecommendatorPassedRequest(marketSearchBidRecommendator);
        assertThat(parallelRecRequest, hasRegionId(PARAM_REGION_ID));
    }

    /**
     * Установка ставок на основе рекомендаций для маркетного поиска.
     * Регион не задан явно в запросе -> в рекоммендатор передается локальный регион.
     */
    @Test
    void test_updateBid_when_noRegionSpecifiedInMarketSearchGoalUpdate_should_sendReportRequestWithLocalRegion() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.goal.type=" + MARKET_SEARCH +
                "&req1.goal.value=" + PREMIUM_FIRST_PLACE
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest parallelRecRequest = extractRecommendatorPassedRequest(marketSearchBidRecommendator);
        assertThat(parallelRecRequest, hasRegionId(LOCAL_DELIVERY_REGION_ID));
    }
}