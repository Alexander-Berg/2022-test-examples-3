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
import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionBidStatus;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.partner.auction.BidComponentsInferenceManager;
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
 * Тесты на то, что в результатах поисковой выдачи для {@link SearchAuctionOffersServantlet} фигурируют данные,
 * подтянутые из сервиса биддинга.
 * <p>
 * Существующее ТП - мок ставок для группы в {@link AuctionService}.
 * Ответы репорта замоканы в файлах. Необходимо, так как существующая логика подразумевает маркировку статуса ставки
 * как {@link AuctionBidStatus#ERROR_OFFER_NOT_FOUND}, если не удалось получить ответа по рекомендациям (см ).
 * И это перетрет статус ставик из биддинга - а для проверки этого не хочется.
 */
class SearchOffersByQueryTest extends AuctionServantletMockBase {

    @InjectMocks
    private SearchAuctionOffersServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> searchAuctionOffersServantlet;
    @InjectMocks
    private BidComponentsInferenceManager bidComponentsInferenceManager;

    private static Stream<Arguments> testCases() {
        return Stream.of(
                of("&type=" + HYBRID_CARD, HYBRID_CARD.toString()),
                of("&type=" + PARALLEL_SEARCH, PARALLEL_SEARCH.toString()),
                of("&type=" + MARKET_SEARCH, MARKET_SEARCH.toString()),
                /*не указан явно тип в запросе*/
                of("", "type not specified")
        );
    }

    private static Stream<Arguments> testCasesForReset() {
        return Stream.of(
                of(HYBRID_CARD),
                of(PARALLEL_SEARCH),
                of(MARKET_SEARCH),
                /*не указан явно тип в запросе*/
                of((SearchQueryRecommendationsType) null)
        );
    }

    @BeforeEach
    void before() throws IOException, SAXException {
        MockitoAnnotations.initMocks(this);

        ReportRecommendationService recommendationsService = new ReportRecommendationService(
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
        mockOfferExists();

        searchAuctionOffersServantlet.setBidComponentsInferenceManager(bidComponentsInferenceManager);
        searchAuctionOffersServantlet.setRecommendationsService(recommendationsService);
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
     * Проверяем, что в результат для поисковой выдачи подтягиваеются бэдж и статус из биддинга.
     */
    @DisplayName("Search answer with merged bids info when bid exists")
    @ParameterizedTest(name = "queryType={1}")
    @MethodSource("testCases")
    void test_getSearchOffer_when_thereIsOfferInBidding(
            String searchTypeArg,
            String description
    ) {
        mockAuctionExistingBid(SOME_TITLE_OFFER_ID, DEFAULT_GROUP_ID, PARAM_DATASOURCE_ID, AuctionBidComponentsLink.CARD_LINK_CBID_VARIABLE);

        mockServantletPassedArgs("q=" + SOME_SEARCH_QUERY +
                searchTypeArg
        );

        searchAuctionOffersServantlet.process(servRequest, usefullServResponse);

        List<SerializationGate.AuctionOffer> auctionBidResults = extractBidsFromServResponse(usefullServResponse);
        assertThat(auctionBidResults, hasSize(1));

        OfferSummary offerSummary = auctionBidResults.get(0).getOfferSummary();

        assertThat(offerSummary.getBid().getStatus(), is(AuctionBidStatus.PUBLISHED));
        assertThat(offerSummary.getOffer().getGroupId(), is(DEFAULT_GROUP_ID));
        assertThat(offerSummary.getOffer().getOfferId(), is(SOME_OFFER_NAME));
    }

    /**
     * Проверяем, что в результате для поисковой выдачи нет информации о статусе публикации и группе если ставка в
     * биддинге сброшена.
     */
    @DisplayName("Search answer without merged bids info when bid exists but reset")
    @ParameterizedTest(name = "queryType={0}")
    @MethodSource("testCasesForReset")
    void test_getSearchOffer_when_thereIsResetOfferInBidding(
            SearchQueryRecommendationsType searchQueryRecommendationsType
    ) {
        //ставка в биддинге существует, но сброшена.
        AuctionOfferBid existingBid = new AuctionOfferBid(
                PARAM_DATASOURCE_ID,
                SOME_TITLE_OFFER_ID,
                SOME_TITLE_OFFER_ID.getId(),
                DEFAULT_GROUP_ID,
                AuctionBidValues.RESET
        );
        //не имеет значения, но логически после сброса и до подтверждения индекстаором это такой статус
        existingBid.setStatus(AuctionBidStatus.INDEXING);
        existingBid.setLinkType(AuctionBidComponentsLink.DEFAULT_LINK_TYPE);

        mockAuctionExistingBid(existingBid, SOME_TITLE_OFFER_ID, DEFAULT_GROUP_ID, PARAM_DATASOURCE_ID);

        mockServantletPassedArgs("q=" + SOME_SEARCH_QUERY);

        if (searchQueryRecommendationsType != null) {
            mockServantletPassedArgs("" +
                    "type=" + searchQueryRecommendationsType
            );
        }

        searchAuctionOffersServantlet.process(servRequest, usefullServResponse);

        List<SerializationGate.AuctionOffer> auctionBidResults = extractBidsFromServResponse(usefullServResponse);
        assertThat(auctionBidResults, hasSize(1));
        OfferSummary offerSummary = auctionBidResults.get(0).getOfferSummary();

        assertThat(offerSummary.getBid().getStatus(), is(nullValue()));
        assertThat(offerSummary.getOffer().getOfferId(), is(SOME_OFFER_NAME));
    }

}
