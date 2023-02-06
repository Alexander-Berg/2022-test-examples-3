package ru.yandex.market.partner.auction.servantlet.bulk.report_params.read;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.recommend.BidRecommendationRequest;
import ru.yandex.market.partner.auction.BulkReadQueryType;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasAuctionBulkQuery;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasOfferId;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasRegionId;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasSearchQuery;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasShopId;

/**
 * Проверяем, что связный запрос в репорт при получении карточных рекомендациях по идентификатору группы
 * содержит ожидаемый набор аттрибутов для ТП.
 * ТП берется из мока ставок для группы в {@link AuctionService}.
 */
@ExtendWith(MockitoExtension.class)
class GetCardRecommendationsByGroupIdReportTranslationTest extends AuctionBulkServantletlMockBase {

    @BeforeEach
    void beforeEach() {
        auctionBulkOfferBidsServantlet.configure();
        generalBulkServantletInit(recommendationsService);

        mockBidLimits();
        mockRegionsAndTariff();

        mockAuctionServicePartialBidsForDefaultGroup(true);
        mockCheckHomeRegionInIndex();
        mockRecommendationServiceEmptyCalculateResult();

        mockServRequestCrudActionREAD();
        mockServRequestIdentificationParams();
        mockOfferExists();
    }

    /**
     * Общий кейс.
     */
    @Test
    void test_getRecommendationsByGroupId() {
        mockServantletPassedArgs(
                "type=" + BulkReadQueryType.HYBRID_REC_GROUP
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest parallelRecRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(parallelRecRequest, hasRegionId(LOCAL_DELIVERY_REGION_ID));
        assertThat(parallelRecRequest, hasOfferId(SOME_TITLE_OFFER_ID));
        assertThat(parallelRecRequest, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(parallelRecRequest, hasSearchQuery(null));
        assertThat(parallelRecRequest, hasAuctionBulkQuery(SOME_OFFER_NAME));//в моке поисковый запрос сделан идентифчным id

        BidRecommendationRequest hybridRecRequest = extractRecommendatorLoadPassedRequest(modelCardBidRecommendator);
        assertThat(hybridRecRequest, hasRegionId(LOCAL_DELIVERY_REGION_ID));
        assertThat(hybridRecRequest, hasOfferId(SOME_TITLE_OFFER_ID));
        assertThat(hybridRecRequest, hasShopId(PARAM_DATASOURCE_ID));
    }

    /**
     * В дополнение к общему кейсу:
     * Явное КК в запросе транслируется корректно.
     */
    @Test
    void test_getRecommendationsByGroupId_when_explicitRegionIdInRequest() {
        mockServantletPassedArgs("" +
                "type=" + BulkReadQueryType.HYBRID_REC_GROUP +
                "&regionId=" + PARAM_REGION_ID
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest parallelRecRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(parallelRecRequest, hasRegionId(PARAM_REGION_ID));
        assertThat(parallelRecRequest, hasOfferId(SOME_TITLE_OFFER_ID));
        assertThat(parallelRecRequest, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(parallelRecRequest, hasSearchQuery(null));
        assertThat(parallelRecRequest, hasAuctionBulkQuery(SOME_OFFER_NAME));//в моке поисковый запрос сделан идентифчным id

        BidRecommendationRequest hybridRecRequest = extractRecommendatorLoadPassedRequest(modelCardBidRecommendator);
        assertThat(hybridRecRequest, hasRegionId(PARAM_REGION_ID));
        assertThat(hybridRecRequest, hasOfferId(SOME_TITLE_OFFER_ID));
        assertThat(hybridRecRequest, hasShopId(PARAM_DATASOURCE_ID));

    }

}