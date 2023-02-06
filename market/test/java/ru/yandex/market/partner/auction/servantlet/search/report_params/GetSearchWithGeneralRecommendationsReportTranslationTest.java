package ru.yandex.market.partner.auction.servantlet.search.report_params;

import java.io.IOException;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.recommend.BidRecommendationRequest;
import ru.yandex.market.core.ds.DatasourceService;
import ru.yandex.market.partner.auction.AuctionBulkCommon;
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
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasSearchQuery;

/**
 * Тесты для {@link SearchAuctionOffersServantlet}.
 * Проверка того, что когда при получении рекомендаций для ТП для дефолтного типа {@link SearchQueryRecommendationsType#HYBRID_CARD},
 * в репорт передаются ожидаемые feature параметры.
 */
@ExtendWith(MockitoExtension.class)
class GetSearchWithGeneralRecommendationsReportTranslationTest extends AuctionServantletMockBase {
    @InjectMocks
    private SearchAuctionOffersServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> searchAuctionOffersServantlet;
    @InjectMocks
    private BidComponentsInferenceManager bidComponentsInferenceManager;
    @InjectMocks
    private ReportRecommendationService recommendationsService;

    @BeforeEach
    void beforeEach() throws IOException {
        Mockito.reset(mockedExistenceChecker);
        Mockito.reset(auctionService);

        searchAuctionOffersServantlet.setBidComponentsInferenceManager(bidComponentsInferenceManager);
        searchAuctionOffersServantlet.setRecommendationsService(recommendationsService);
        mockBidLimits();

        mockRegionsAndTariff();

        mockRecommendationServiceEmptyCalculateResult();

        mockServRequestCrudActionREAD();
        mockServRequestIdentificationParams();

        mockSearchReportAnswers();
        mockOfferExists();
    }

    /**
     * Если регион явно задан в параметрах к сервантлету, то должен быть указан в запрсое к репорту.
     */
    @DisplayName("Регион явно указан в запросе")
    @Test
    void test_regionTranslation_when_regionSpecifiedInSearchRequest_should_sendReportRequestWithPassedRegion() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);
        mockServantletPassedArgs("" +
                "q=" + SOME_SEARCH_QUERY +
                "&regionId=" + PARAM_REGION_ID
        );

        searchAuctionOffersServantlet.process(servRequest, servResponse);

        BidRecommendationRequest parallelRecRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(parallelRecRequest, hasRegionId(PARAM_REGION_ID));

        BidRecommendationRequest hybridRecRequest = extractRecommendatorLoadPassedRequest(modelCardBidRecommendator);
        assertThat(hybridRecRequest, hasRegionId(PARAM_REGION_ID));

        BidRecommendationRequest marketRecRequest = extractRecommendatorPassedRequest(marketSearchBidRecommendator);
        assertThat(marketRecRequest, hasRegionId(PARAM_REGION_ID));
    }

    /**
     * Если регион явно НЕ задан в параметрах к сервантлету, то должен в запросе к репорту указывается(если сущесвтует)
     * {@link DatasourceService#getLocalDeliveryRegion(long)}.
     */
    @DisplayName("Регион не указан в запросе")
    @Test
    void test_regionTranslation_when_noRegionSpecifiedInRecsByGroup_should_sendReportRequestWithLocalRegion() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);
        mockServantletPassedArgs("q=" + SOME_SEARCH_QUERY);

        searchAuctionOffersServantlet.process(servRequest, servResponse);

        BidRecommendationRequest parallelRecRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(parallelRecRequest, hasRegionId(LOCAL_DELIVERY_REGION_ID));

        BidRecommendationRequest hybridRecRequest = extractRecommendatorLoadPassedRequest(modelCardBidRecommendator);
        assertThat(hybridRecRequest, hasRegionId(LOCAL_DELIVERY_REGION_ID));

        BidRecommendationRequest marketRecRequest = extractRecommendatorPassedRequest(marketSearchBidRecommendator);
        assertThat(marketRecRequest, hasRegionId(LOCAL_DELIVERY_REGION_ID));
    }

    @DisplayName("SHOP_OFFER_ID. Проверка searchQuery, переданного в репорт по умолчанию")
    @Test
    void test_useSearchQueryFromPassedParams_when_idOffer() {
        mockShopAuctionType(AuctionOfferIdType.SHOP_OFFER_ID);
        mockServantletPassedArgs("q=" + SOME_SEARCH_QUERY);

        searchAuctionOffersServantlet.process(servRequest, servResponse);

        BidRecommendationRequest parallelRecRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(parallelRecRequest, hasAuctionBulkQuery(SOME_SEARCH_QUERY));
        assertThat(parallelRecRequest, hasSearchQuery(null));
        assertThat(parallelRecRequest, hasOfferId(SHOFFER_ID_200304546_1482033));

        BidRecommendationRequest marketRecRequest = extractRecommendatorPassedRequest(marketSearchBidRecommendator);
        assertThat(marketRecRequest, hasAuctionBulkQuery(SOME_SEARCH_QUERY));
        assertThat(marketRecRequest, hasSearchQuery(SOME_SEARCH_QUERY));
        assertThat(marketRecRequest, hasOfferId(SHOFFER_ID_200304546_1482033));

        //для карточных рекомендаций поисковый запрос не нужен
        BidRecommendationRequest hybridRecRequest = extractRecommendatorLoadPassedRequest(modelCardBidRecommendator);
        assertThat(hybridRecRequest, hasAuctionBulkQuery(null));
        assertThat(hybridRecRequest, hasSearchQuery(null));
        assertThat(hybridRecRequest, hasOfferId(SHOFFER_ID_200304546_1482033));
    }

    /**
     * Для {@link AuctionOfferIdType#SHOP_OFFER_ID} в качестве поискового запроса идет title, полученный в запросе
     * к {@link MarketReportPlace#API_OFFERINFO}.
     */
    @DisplayName("SHOP_OFFER_ID. Проверка searchQuery, переданного в репорт при установке флага offer_title_as_search_query")
    @Test
    void test_useOfferTitleAsSearchQuery_when_idOffer() {
        mockShopAuctionType(AuctionOfferIdType.SHOP_OFFER_ID);
        mockServantletPassedArgs("q=" + SOME_SEARCH_QUERY + "&offer_title_as_search_query=true");

        searchAuctionOffersServantlet.process(servRequest, servResponse);

        BidRecommendationRequest parallelRecRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(parallelRecRequest, hasAuctionBulkQuery(AuctionBulkCommon.OFFER_EXISTS.getOfferName()));
        assertThat(parallelRecRequest, hasSearchQuery(null));
        assertThat(parallelRecRequest, hasOfferId(SHOFFER_ID_200304546_1482033));

        BidRecommendationRequest marketRecRequest = extractRecommendatorPassedRequest(marketSearchBidRecommendator);
        assertThat(marketRecRequest, hasAuctionBulkQuery(AuctionBulkCommon.OFFER_EXISTS.getOfferName()));
        assertThat(marketRecRequest, hasSearchQuery(AuctionBulkCommon.OFFER_EXISTS.getOfferName()));
        assertThat(marketRecRequest, hasOfferId(SHOFFER_ID_200304546_1482033));

        //для карточных рекомендаций поисковый запрос не нужен
        BidRecommendationRequest hybridRecRequest = extractRecommendatorLoadPassedRequest(modelCardBidRecommendator);
        assertThat(hybridRecRequest, hasAuctionBulkQuery(null));
        assertThat(hybridRecRequest, hasSearchQuery(null));
        assertThat(hybridRecRequest, hasOfferId(SHOFFER_ID_200304546_1482033));
    }

    @DisplayName("TITLE. Проверка searchQuery, переданный в репорт по умолчанию")
    @Test
    void test_useSearchQueryFromPassedParams_when_titleOffer() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);
        mockServantletPassedArgs("q=" + SOME_SEARCH_QUERY);

        searchAuctionOffersServantlet.process(servRequest, servResponse);

        BidRecommendationRequest parallelRecRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(parallelRecRequest, hasAuctionBulkQuery(SOME_SEARCH_QUERY));
        assertThat(parallelRecRequest, hasSearchQuery(null));
        assertThat(parallelRecRequest, hasOfferId(SOME_TITLE_OFFER_ID));

        BidRecommendationRequest marketRecRequest = extractRecommendatorPassedRequest(marketSearchBidRecommendator);
        assertThat(marketRecRequest, hasAuctionBulkQuery(SOME_SEARCH_QUERY));
        assertThat(marketRecRequest, hasSearchQuery(SOME_SEARCH_QUERY));
        assertThat(marketRecRequest, hasOfferId(SOME_TITLE_OFFER_ID));

        BidRecommendationRequest hybridRecRequest = extractRecommendatorLoadPassedRequest(modelCardBidRecommendator);
        assertThat(hybridRecRequest, hasAuctionBulkQuery(SOME_OFFER_NAME));
        assertThat(hybridRecRequest, hasSearchQuery(null));
        assertThat(hybridRecRequest, hasOfferId(SOME_TITLE_OFFER_ID));
    }

    /**
     * Если задан флаг, то запрос в репорт за рекомендациями делаем на основе имени оффера.
     */
    @DisplayName("TITLE. Проверка searchQuery, переданного в репорт при установке флага offer_title_as_search_query")
    @Test
    void test_useOfferTitleAsSearchQuery_when_titleOffer() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);
        mockServantletPassedArgs("q=" + SOME_SEARCH_QUERY +
                "&offer_title_as_search_query=true");

        searchAuctionOffersServantlet.process(servRequest, servResponse);

        BidRecommendationRequest parallelRecRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(parallelRecRequest, hasAuctionBulkQuery(SOME_OFFER_NAME));
        assertThat(parallelRecRequest, hasSearchQuery(null));
        assertThat(parallelRecRequest, hasOfferId(SOME_TITLE_OFFER_ID));

        BidRecommendationRequest marketRecRequest = extractRecommendatorPassedRequest(marketSearchBidRecommendator);
        assertThat(marketRecRequest, hasAuctionBulkQuery(SOME_OFFER_NAME));
        assertThat(marketRecRequest, hasSearchQuery(SOME_OFFER_NAME));
        assertThat(marketRecRequest, hasOfferId(SOME_TITLE_OFFER_ID));

        BidRecommendationRequest hybridRecRequest = extractRecommendatorLoadPassedRequest(modelCardBidRecommendator);
        assertThat(hybridRecRequest, hasAuctionBulkQuery(SOME_OFFER_NAME));
        assertThat(hybridRecRequest, hasSearchQuery(null));
        assertThat(hybridRecRequest, hasOfferId(SOME_TITLE_OFFER_ID));
    }
}