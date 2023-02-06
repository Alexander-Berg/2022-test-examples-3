package ru.yandex.market.partner.auction.servantlet.bulk.report_params.read;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;

import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.recommend.BidRecommendationRequest;
import ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet;
import ru.yandex.market.partner.auction.BidComponentsInferenceManager;
import ru.yandex.market.partner.auction.BulkReadQueryType;
import ru.yandex.market.partner.auction.ReportRecommendationService;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

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
class GetParallelSearchRecommendationsByGroupIdReportTranslationTest extends AuctionBulkServantletlMockBase {
    @InjectMocks
    private AuctionBulkOfferBidsServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> auctionBulkOfferBidsServantlet;
    @InjectMocks
    private BidComponentsInferenceManager bidComponentsInferenceManager;
    @InjectMocks
    private ReportRecommendationService recommendationsService;

    @BeforeEach
    void beforeEach() {
        auctionBulkOfferBidsServantlet.configure();
        mockBidLimits();

        auctionBulkOfferBidsServantlet.setBidComponentsInferenceManager(bidComponentsInferenceManager);
        auctionBulkOfferBidsServantlet.setRecommendationsService(recommendationsService);
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
        mockServantletPassedArgs("" +
                "type=" + BulkReadQueryType.PARALLEL_SEARCH_GROUP
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest recRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(recRequest, hasRegionId(LOCAL_DELIVERY_REGION_ID));
        assertThat(recRequest, hasOfferId(SOME_TITLE_OFFER_ID));
        assertThat(recRequest, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(recRequest, hasAuctionBulkQuery(SOME_OFFER_NAME));
    }

    /**
     * В дополнение к общему кейсу:
     * Явное указание региона в запросе транслируется корректно.
     */
    @Test
    void test_getRecommendationsByGroupId_when_explicitRegionIdInRequest() {
        mockServantletPassedArgs("" +
                "type=" + BulkReadQueryType.PARALLEL_SEARCH_GROUP +
                "&regionId=" + PARAM_REGION_ID
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest recRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(recRequest, hasRegionId(PARAM_REGION_ID));
        assertThat(recRequest, hasOfferId(SOME_TITLE_OFFER_ID));
        assertThat(recRequest, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(recRequest, hasAuctionBulkQuery(SOME_OFFER_NAME));
    }
}