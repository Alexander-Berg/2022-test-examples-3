package ru.yandex.market.partner.auction.servantlet.bulk.report_params.update;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.recommend.BidRecommendationRequest;
import ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasRegionId;
import static ru.yandex.market.core.auction.model.AuctionGoalPlace.PREMIUM_FIRST_PLACE;
import static ru.yandex.market.partner.auction.HybridGoal.GoalType.HYBRID_CARD;

/**
 * Проверка трансляции параметров в {@link AuctionBulkOfferBidsServantlet} в запросах установки ставок.
 * Проверяем, что параметры переданные в карточный рекомендатор(в виде mock-ов), соответсвуют ожиданиям.
 */
@ExtendWith(MockitoExtension.class)
class BulkUpdateCardHybridReportTranslationTest extends AuctionBulkServantletlMockBase {

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
     * Установка ставок на основе рекомендаций для карточки.
     * Регион задан явно в запросе -> в рекоммендатор передается заданное значение.
     */
    @Test
    void test_updateBid_when_regionSpecifiedInCardGoalUpdate_should_sendReportRequestWithPassedRegion() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.goal.type=" + HYBRID_CARD +
                "&req1.goal.value=" + PREMIUM_FIRST_PLACE +
                "&req1.linkType=" + AuctionBidComponentsLink.CARD_NO_LINK_CBID_PRIORITY +
                "&regionId=" + PARAM_REGION_ID
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest hybridRecRequest = extractRecommendatorLoadPassedRequest(modelCardBidRecommendator);
        assertThat(hybridRecRequest, hasRegionId(PARAM_REGION_ID));
    }

    /**
     * Установка ставок на основе рекомендаций для карточки.
     * Регион не задан явно в запросе -> в рекоммендатор передается локальный регион.
     */
    @Test
    void test_updateBid_when_noRegionSpecifiedInCardGoalUpdate_should_sendReportRequestWithLocalRegion() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.goal.type=" + HYBRID_CARD +
                "&req1.goal.value=" + PREMIUM_FIRST_PLACE +
                "&req1.linkType=" + AuctionBidComponentsLink.CARD_NO_LINK_CBID_PRIORITY
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest hybridRecRequest = extractRecommendatorLoadPassedRequest(modelCardBidRecommendator);
        assertThat(hybridRecRequest, hasRegionId(LOCAL_DELIVERY_REGION_ID));
    }
}