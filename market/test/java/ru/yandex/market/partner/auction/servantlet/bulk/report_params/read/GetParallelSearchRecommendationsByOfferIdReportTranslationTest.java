package ru.yandex.market.partner.auction.servantlet.bulk.report_params.read;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.recommend.BidRecommendationRequest;
import ru.yandex.market.partner.auction.BulkReadQueryType;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasAuctionBulkQuery;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasOfferId;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasRegionId;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasShopId;

/**
 * Проверяем, что связный запрос в репорт при получении параллельных рекомендациях по иденификатору группы
 * содержит ожидаемый набор аттрибутов для ТП.
 * ТП берется из мока ставок для группы в {@link AuctionService}.
 */
@ExtendWith(MockitoExtension.class)
class GetParallelSearchRecommendationsByOfferIdReportTranslationTest extends AuctionBulkServantletlMockBase {

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
    void test_getRecommendationsByOfferTitle() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);

        mockServantletPassedArgs("" +
                "type=" + BulkReadQueryType.PARALLEL_SEARCH_REC_OFFER +
                "&req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.searchQuery=" + SOME_SEARCH_QUERY
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest recRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(recRequest, hasRegionId(LOCAL_DELIVERY_REGION_ID));
        assertThat(recRequest, hasOfferId(SOME_TITLE_OFFER_ID));
        assertThat(recRequest, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(recRequest, hasAuctionBulkQuery(SOME_SEARCH_QUERY));
    }

    /**
     * В дополнение к общему кейсу:
     * Явное указание региона в запросе транслируется корректно.
     */
    @Test
    void test_getRecommendationsByOffer_when_explicitRegionIdInRequest() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);

        mockServantletPassedArgs("" +
                "type=" + BulkReadQueryType.PARALLEL_SEARCH_REC_OFFER +
                "&req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.searchQuery=" + SOME_SEARCH_QUERY +
                "&regionId=" + PARAM_REGION_ID
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest recRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(recRequest, hasRegionId(PARAM_REGION_ID));
        assertThat(recRequest, hasOfferId(SOME_TITLE_OFFER_ID));
        assertThat(recRequest, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(recRequest, hasAuctionBulkQuery(SOME_SEARCH_QUERY));
    }

    /**
     * В дополнение к общему кейсу:
     * Поисковый запрос используемый при походе в репорт - поофферный.
     */
    @Test
    void test_getRecommendationsByOffer_when_passedGeneralQuery_then_hasNoEffect() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);

        mockServantletPassedArgs("" +
                "type=" + BulkReadQueryType.PARALLEL_SEARCH_REC_OFFER +
                "&req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.searchQuery=" + SOME_SEARCH_QUERY +
                "&searchQuery=thisQueryWillBeIgnored"
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest recRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(recRequest, hasRegionId(LOCAL_DELIVERY_REGION_ID));
        assertThat(recRequest, hasOfferId(SOME_TITLE_OFFER_ID));
        assertThat(recRequest, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(recRequest, hasAuctionBulkQuery(SOME_SEARCH_QUERY));
    }

    /**
     * В дополнение к общему кейсу:
     * проверяем для случая идентификации {@link AuctionOfferIdType#SHOP_OFFER_ID}.
     */
    @Test
    void test_getRecommendationsByOfferId() {
        mockShopAuctionType(AuctionOfferIdType.SHOP_OFFER_ID);

        mockServantletPassedArgs("" +
                "type=" + BulkReadQueryType.PARALLEL_SEARCH_REC_OFFER +
                "&req.size=1" +
                "&req1.offerId=" + SOME_FEED_OFFER_ID.getId() +
                "&req1.feedId=" + SOME_FEED_OFFER_ID.getFeedId() +
                "&req1.searchQuery=" + SOME_SEARCH_QUERY
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest recRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(recRequest, hasRegionId(LOCAL_DELIVERY_REGION_ID));
        assertThat(recRequest, hasOfferId(SOME_FEED_OFFER_ID));
        assertThat(recRequest, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(recRequest, hasAuctionBulkQuery(SOME_SEARCH_QUERY));
    }
}