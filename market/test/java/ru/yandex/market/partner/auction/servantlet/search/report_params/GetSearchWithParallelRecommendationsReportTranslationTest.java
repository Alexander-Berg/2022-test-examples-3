package ru.yandex.market.partner.auction.servantlet.search.report_params;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.recommend.BidRecommendationRequest;
import ru.yandex.market.core.ds.DatasourceService;
import ru.yandex.market.partner.auction.BidComponentsInferenceManager;
import ru.yandex.market.partner.auction.ReportRecommendationService;
import ru.yandex.market.partner.auction.SearchAuctionOffersServantlet;
import ru.yandex.market.partner.auction.SearchQueryRecommendationsType;
import ru.yandex.market.partner.auction.servantlet.AuctionServantletMockBase;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasAuctionBulkQuery;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasOfferId;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasRegionId;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasShopId;

/**
 * Тесты для {@link SearchAuctionOffersServantlet}.
 * Проверка того, что когда при получении рекендаций для ТП на поисковой выдаче(параллельные рекомендации), в репорт
 * передаются ожидаемые feature параметры.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GetSearchWithParallelRecommendationsReportTranslationTest extends AuctionServantletMockBase {
    @InjectMocks
    private SearchAuctionOffersServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> searchAuctionOffersServantlet;
    @InjectMocks
    private BidComponentsInferenceManager bidComponentsInferenceManager;
    @InjectMocks
    private ReportRecommendationService recommendationsService;

    @Before
    public void before() throws IOException {
        searchAuctionOffersServantlet.setBidComponentsInferenceManager(bidComponentsInferenceManager);
        searchAuctionOffersServantlet.setRecommendationsService(recommendationsService);
        mockRegionsAndTariff();

        mockRecommendationServiceEmptyCalculateResult();

        mockServRequestCrudActionREAD();
        mockServRequestIdentificationParams();
        mockShopAuctionType(AuctionOfferIdType.TITLE);

        mockSearchReportAnswers();
        mockOfferExists();
    }

    /**
     * Если регион явно НЕ задан в параметрах к сервантлету, то должен в запросе к репорту указывается(если сущесвтует)
     * {@link DatasourceService#getLocalDeliveryRegion(long)}.
     */
    @Test
    public void test_regionTranslation_when_noRegionSpecifiedInRecsByGroup_should_sendReportRequestWithLocalRegion() {
        mockServantletPassedArgs("" +
                "q=" + SOME_SEARCH_QUERY +
                "&type=" + SearchQueryRecommendationsType.PARALLEL_SEARCH
        );

        searchAuctionOffersServantlet.process(servRequest, servResponse);

        BidRecommendationRequest hybridRecRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(hybridRecRequest, hasRegionId(LOCAL_DELIVERY_REGION_ID));
        assertThat(hybridRecRequest, hasOfferId(SOME_TITLE_OFFER_ID));
        assertThat(hybridRecRequest, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(hybridRecRequest, hasAuctionBulkQuery(SOME_SEARCH_QUERY));
    }

    /**
     * Если регион явно задан в параметрах к сервантлету, то должен быть указан в запрсое к репорту.
     */
    @Test
    public void test_regionTranslation_when_regionSpecifiedInSearchRequest_should_sendReportRequestWithPassedRegion() {
        mockServantletPassedArgs("" +
                "q=" + SOME_SEARCH_QUERY +
                "&type=" + SearchQueryRecommendationsType.PARALLEL_SEARCH +
                "&regionId=" + PARAM_REGION_ID
        );

        searchAuctionOffersServantlet.process(servRequest, servResponse);

        BidRecommendationRequest hybridRecRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);

        assertThat(hybridRecRequest, hasRegionId(PARAM_REGION_ID));
        assertThat(hybridRecRequest, hasOfferId(SOME_TITLE_OFFER_ID));
        assertThat(hybridRecRequest, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(hybridRecRequest, hasAuctionBulkQuery(SOME_SEARCH_QUERY));
    }

}