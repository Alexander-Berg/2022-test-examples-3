package ru.yandex.market.partner.auction.servantlet.search.actions;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.xml.sax.SAXException;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionBidStatus;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.partner.auction.BidComponentsInferenceManager;
import ru.yandex.market.partner.auction.LightOfferExistenceChecker;
import ru.yandex.market.partner.auction.OfferSummary;
import ru.yandex.market.partner.auction.ReportRecommendationService;
import ru.yandex.market.partner.auction.SearchAuctionOffersServantlet;
import ru.yandex.market.partner.auction.SearchQueryRecommendationsType;
import ru.yandex.market.partner.auction.servantlet.AuctionServantletMockBase;
import ru.yandex.market.partner.auction.servantlet.bulk.PartiallyRecommendatorsFactory;
import ru.yandex.market.partner.auction.view.SerializationGate;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.params.provider.Arguments.of;
import static ru.yandex.market.core.auction.AuctionService.DEFAULT_GROUP_ID;
import static ru.yandex.market.partner.auction.SearchQueryRecommendationsType.HYBRID_CARD;
import static ru.yandex.market.partner.auction.SearchQueryRecommendationsType.MARKET_SEARCH;
import static ru.yandex.market.partner.auction.SearchQueryRecommendationsType.PARALLEL_SEARCH;

/**
 * Тесты на простановку маркера {@link AuctionBidStatus#ERROR_OFFER_NOT_FOUND} в зависимости от ответа {@link LightOfferExistenceChecker}.
 * Проверяется связка биддинга, ответа репорта по рекомендациям и ответа репорта на предмет существования оффеа в индексе.
 */
class MissingMarkerTest extends AuctionServantletMockBase {

    @InjectMocks
    private SearchAuctionOffersServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> searchAuctionOffersServantlet;
    @InjectMocks
    private BidComponentsInferenceManager bidComponentsInferenceManager;
    private ReportRecommendationService recommendationsService;

    private static Stream<Arguments> testCasesForNotFound() {
        return Stream.of(
                of(HYBRID_CARD),
                of(PARALLEL_SEARCH),
                of(MARKET_SEARCH),
                /*не указан явно тип в запросе*/
                of((SearchQueryRecommendationsType) null)
        );
    }


    @BeforeEach
    void before() throws IOException {
        MockitoAnnotations.initMocks(this);

        searchAuctionOffersServantlet.setBidComponentsInferenceManager(bidComponentsInferenceManager);
        mockBidLimits();

        usefullServResponse = new MockServResponse();

        mockRegionsAndTariff();

        mockRecommendationServiceEmptyCalculateResult();

        mockServRequestCrudActionREAD();
        mockServRequestIdentificationParams();
        mockShopAuctionType(AuctionOfferIdType.TITLE);

        mockSearchReportAnswers();
    }

    /**
     * Если оффер отсутствует в индексе и существует ставка в биддинге то в информацию о статусе публикуется статус
     * {@link AuctionBidStatus#ERROR_OFFER_NOT_FOUND}, перегружая статус ставки в биддинге.
     */
    @DisplayName("Offers exists in bidding but not in index")
    @ParameterizedTest(name = "queryType={0}")
    @MethodSource("testCasesForNotFound")
    void test_getSearchOffer_when_noOfferInIndex(
            SearchQueryRecommendationsType searchQueryRecommendationsType
    ) throws Exception {

        mockAuctionExistingBid(SOME_TITLE_OFFER_ID, DEFAULT_GROUP_ID, PARAM_DATASOURCE_ID,
                AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY);

        mockReportWithExistingRecs();
        mockOfferMissing();
        mockServantletPassedArgs("q=" + SOME_SEARCH_QUERY);

        if (searchQueryRecommendationsType != null) {
            mockServantletPassedArgs("type=" + searchQueryRecommendationsType);
        }

        searchAuctionOffersServantlet.process(servRequest, usefullServResponse);

        List<SerializationGate.AuctionOffer> auctionBidResults = extractBidsFromServResponse(usefullServResponse);
        assertThat(auctionBidResults, hasSize(1));
        OfferSummary offerSummary = auctionBidResults.get(0).getOfferSummary();

        assertThat(offerSummary.getBid().getStatus(), is(AuctionBidStatus.ERROR_OFFER_NOT_FOUND));
        assertThat(offerSummary.getOffer().getGroupId(), is(DEFAULT_GROUP_ID));
        assertThat(offerSummary.getOffer().getId(), is(SOME_OFFER_NAME));
    }

    /**
     * Если оффер существует в индексе и существует ставка в биддинге то в информацию о статусе публикуется статус из биддинга.
     */
    @DisplayName("Offer exists in bidding and index")
    @ParameterizedTest(name = "queryType={0}")
    @MethodSource("testCasesForNotFound")
    void test_getSearchOffer_when_offerInIndex(
            SearchQueryRecommendationsType searchQueryRecommendationsType
    ) throws Exception {

        mockAuctionExistingBid(SOME_TITLE_OFFER_ID, DEFAULT_GROUP_ID, PARAM_DATASOURCE_ID,
                AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY);

        mockReportWithExistingRecs();
        mockOfferExists();
        mockServantletPassedArgs("q=" + SOME_SEARCH_QUERY);

        if (searchQueryRecommendationsType != null) {
            mockServantletPassedArgs("type=" + searchQueryRecommendationsType);
        }

        searchAuctionOffersServantlet.process(servRequest, usefullServResponse);

        List<SerializationGate.AuctionOffer> auctionBidResults = extractBidsFromServResponse(usefullServResponse);
        assertThat(auctionBidResults, hasSize(1));
        OfferSummary offerSummary = auctionBidResults.get(0).getOfferSummary();

        assertThat(offerSummary.getBid().getStatus(), is(AuctionBidStatus.PUBLISHED));
        assertThat(offerSummary.getOffer().getGroupId(), is(DEFAULT_GROUP_ID));
        assertThat(offerSummary.getOffer().getId(), is(SOME_OFFER_NAME));
    }

    /**
     * Если оффер существует в индексе и существует ставка в биддинге то в информацию о статусе публикуется статус из биддинга.
     * Случай, когда все рекомендаторы возвращают пустую выдачу.
     */
    @DisplayName("Offer exists in bidding and index and no recommendations")
    @ParameterizedTest(name = "queryType={0}")
    @MethodSource("testCasesForNotFound")
    void test_getSearchOffer_when_noRecommendations(
            SearchQueryRecommendationsType searchQueryRecommendationsType
    ) throws Exception {

        mockAuctionExistingBid(SOME_TITLE_OFFER_ID, DEFAULT_GROUP_ID, PARAM_DATASOURCE_ID,
                AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY);

        mockReportWithAbsentRecs();
        mockOfferExists();
        mockServantletPassedArgs("q=" + SOME_SEARCH_QUERY);

        if (searchQueryRecommendationsType != null) {
            mockServantletPassedArgs("type=" + searchQueryRecommendationsType);
        }

        searchAuctionOffersServantlet.process(servRequest, usefullServResponse);

        List<SerializationGate.AuctionOffer> auctionBidResults = extractBidsFromServResponse(usefullServResponse);
        assertThat(auctionBidResults, hasSize(1));
        OfferSummary offerSummary = auctionBidResults.get(0).getOfferSummary();

        assertThat(offerSummary.getBid().getStatus(), is(AuctionBidStatus.PUBLISHED));
        assertThat(offerSummary.getOffer().getGroupId(), is(DEFAULT_GROUP_ID));
        assertThat(offerSummary.getOffer().getId(), is(SOME_OFFER_NAME));
    }

    /**
     * Если оффер существует в индексе, и нет существующей ставки в биддинге, то нет информации о статусе применения ставки.
     */
    @DisplayName("Offers exists in index but not in bidding")
    @ParameterizedTest(name = "queryType={0}")
    @MethodSource("testCasesForNotFound")
    void test_getSearchOffer_when_noBidInBidding(
            SearchQueryRecommendationsType searchQueryRecommendationsType
    ) throws Exception {
        mockReportWithExistingRecs();
        mockOfferExists();
        mockAuctionHasNoBids();
        mockServantletPassedArgs("q=" + SOME_SEARCH_QUERY);

        if (searchQueryRecommendationsType != null) {
            mockServantletPassedArgs("type=" + searchQueryRecommendationsType);
        }

        searchAuctionOffersServantlet.process(servRequest, usefullServResponse);

        List<SerializationGate.AuctionOffer> auctionBidResults = extractBidsFromServResponse(usefullServResponse);
        assertThat(auctionBidResults, hasSize(1));
        OfferSummary offerSummary = auctionBidResults.get(0).getOfferSummary();

        assertThat(offerSummary.getBid().getStatus(), is(nullValue()));
    }

    private void mockReportWithExistingRecs() throws IOException, SAXException {
        recommendationsService = new ReportRecommendationService(
                PartiallyRecommendatorsFactory.buildParallelSearchRecommendator(
                        this.getClass().getResourceAsStream("./resources/parallel_search_ok.xml")
                ),
                PartiallyRecommendatorsFactory.buildMarketSearchRecommendator(
                        this.getClass().getResourceAsStream("./resources/market_search_ok.xml")
                ),
                PartiallyRecommendatorsFactory.buildCardRecommendator(
                        this.getClass().getResourceAsStream("./resources/hybrid_card_ok.xml")
                ),
                mockedExistenceChecker
        );

        searchAuctionOffersServantlet.setRecommendationsService(recommendationsService);

    }

    private void mockReportWithAbsentRecs() throws IOException, SAXException {
        recommendationsService = new ReportRecommendationService(
                PartiallyRecommendatorsFactory.buildParallelSearchRecommendator(
                        this.getClass().getResourceAsStream("./resources/empty_search.xml")
                ),
                PartiallyRecommendatorsFactory.buildMarketSearchRecommendator(
                        this.getClass().getResourceAsStream("./resources/empty_search.xml")
                ),
                PartiallyRecommendatorsFactory.buildCardRecommendator(
                        this.getClass().getResourceAsStream("./resources/empty_search.xml")
                ),
                mockedExistenceChecker
        );

        searchAuctionOffersServantlet.setRecommendationsService(recommendationsService);

    }

}
