package ru.yandex.market.api.partner.controllers.auction.model.recommender;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.hamcrest.MockitoHamcrest;
import org.xml.sax.SAXException;

import ru.yandex.market.api.partner.controllers.auction.model.OfferBid;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiReportOfferExistenceValidator.AmbiguousReportAnswerException;
import ru.yandex.market.common.parser.LiteInputStreamParser;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.MarketReportOverloadedException;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.MarketSearchRequest;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.model.BidPlace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.partner.controllers.auction.matchers.OfferBidMatchers.hasError;
import static ru.yandex.market.api.partner.controllers.auction.matchers.OfferBidMatchers.hasNoError;
import static ru.yandex.market.api.partner.controllers.auction.matchers.OfferBidMatchers.hasNoSearchQuery;
import static ru.yandex.market.api.partner.controllers.auction.matchers.OfferBidMatchers.hasOfferId;
import static ru.yandex.market.api.partner.controllers.auction.matchers.OfferBidMatchers.hasSearchQuery;
import static ru.yandex.market.api.partner.controllers.auction.matchers.OfferBidMatchers.hasTitleOfferId;
import static ru.yandex.market.core.auction.matchers.MarketSearchRequestMatchers.hasPlace;

/**
 * Тесты для {@link ApiReportOfferExistenceValidator}.
 */
@ExtendWith(MockitoExtension.class)
class ApiReportOfferExistenceValidatorTest {
    private static final String NO_SEARCH_QUERY = null;
    private static final int URL_MAX_LENGTH_HUGE = Integer.MAX_VALUE;
    private static final long SHOP_ID_IRRELEVANT = 774L;
    private static final String API_OFFERINFO_OK = "api_offerinfo_ok.xml";
    private static final String CHECK_OFFERS_OK = "check_offers_response.json";
    private static final String API_OFFERINFO_NOT_FOUND = "api_offerinfo_not_found.xml";
    private static final AuctionOfferId SHOP_FEED_OFFER_ID_475690 = new AuctionOfferId(475690L, "510689.YNDX-000SB");
    private static final AuctionOfferId SHOP_FEED_OFFER_ID_MISSING = new AuctionOfferId(1234L, "nonTitleId");
    @Mock
    private static AsyncMarketReportService marketReportService;
    private ApiReportOfferExistenceValidator checker;
    @Mock
    private CheckOffersParamsService checkOffersParamsService;

    private static OfferBid buildSomeBid(AuctionOfferId id, String searchQuery) {
        AuctionBidValues someRequestedValues = new AuctionBidValues(ImmutableMap.of(
                BidPlace.CARD, BigInteger.valueOf(123L),
                BidPlace.MARKET_PLACE, BigInteger.valueOf(123L),
                BidPlace.SEARCH, BigInteger.valueOf(123L)
        ));

        OfferBid offerBid = new OfferBid();

        offerBid.setBidValues(someRequestedValues);
        offerBid.setOfferId(id);
        offerBid.setSearchQuery(searchQuery);

        return offerBid;
    }

    private static AsyncMarketReportService buildServiceThrowing(Throwable t) {

        return new AsyncMarketReportService() {
            @Override
            public <R> CompletableFuture<R> async(MarketSearchRequest searchRequest, LiteInputStreamParser<R> parser) {
                return CompletableFuture.supplyAsync(() -> {
                    throw (RuntimeException) t;
                });
            }
        };
    }

    @BeforeEach
    void before() {
        checker = new ApiReportOfferExistenceValidator(
                marketReportService,
                new CheckOffersService(marketReportService),
                checkOffersParamsService
        );

        when(checkOffersParamsService.isCheckOffersEnabled())
                .thenReturn(true);
        when(checkOffersParamsService.getMaxUriLength())
                .thenReturn(URL_MAX_LENGTH_HUGE);
    }

    @DisplayName("TITLE. Для найденного оффера не изменяется поисковый запрос если уже задан")
    @Test
    void test_checkOffersExistenceInReport_should_notOverwriteSearchQuery_when_alreadySet()
            throws IOException, SAXException {
        mockApiOfferInfo(API_OFFERINFO_OK);

        OfferBid testBid = buildSomeBid(
                new AuctionOfferId("часы CaSiO aq-s810w-2a"),
                "must_not_be_changed"
        );

        checker.checkExistentAndMarkAbsent(SHOP_ID_IRRELEVANT, ImmutableList.of(testBid), AuctionOfferIdType.TITLE);
        assertThat(testBid, hasSearchQuery("must_not_be_changed"));
        assertThat(testBid, hasNoError());
    }

    @DisplayName("TITLE. Для НЕ найденного оффера выставляется ошибка и не изменяется поисковый запрос")
    @Test
    void test_checkOffersExistenceInReport_should_setError_when_notFound() throws IOException, SAXException {
        mockApiOfferInfo(API_OFFERINFO_NOT_FOUND);
        OfferBid testBid = buildSomeBid(
                new AuctionOfferId("irrelevant"),
                "irrelevant"
        );

        checker.checkExistentAndMarkAbsent(SHOP_ID_IRRELEVANT, ImmutableList.of(testBid), AuctionOfferIdType.TITLE);

        assertThat(testBid, hasTitleOfferId("irrelevant"));
        assertThat(testBid, hasError("OFFER_NOT_FOUND"));
        assertThat(testBid, hasSearchQuery("irrelevant"));
    }

    @DisplayName("TITLE. Для найденного оффера выставляется поисковый запрос если отсуствует и редактируется тайтл")
    @Test
    void test_checkOffersExistenceInReport_should_useAnswerNameAnSetQueryForTitleBid_when_passedWithMixedCaseAndEmptyQuery()
            throws IOException, SAXException {
        mockApiOfferInfo(API_OFFERINFO_OK);

        OfferBid testBid = buildSomeBid(
                new AuctionOfferId("часы CaSiO aq-s810w-2a"),
                NO_SEARCH_QUERY
        );

        checker.checkExistentAndMarkAbsent(SHOP_ID_IRRELEVANT, ImmutableList.of(testBid), AuctionOfferIdType.TITLE);

        assertThat(testBid, hasTitleOfferId("Часы Casio AQ-S810W-2A"));
        assertThat(testBid, hasNoError());
        assertThat(testBid, hasSearchQuery("Часы Casio AQ-S810W-2A"));
    }

    @DisplayName("SHOP_OFFER_ID. Для НЕ найденного оффера выставляется ошибка и не изменяется поисковый запрос")
    @Test
    void test_checkOffersExistenceInReport_ifMissingOffer_then_markError() throws IOException {
        mockCheckOffers(CHECK_OFFERS_OK);
        OfferBid testBid = buildSomeBid(SHOP_FEED_OFFER_ID_MISSING, NO_SEARCH_QUERY);

        checker.checkExistentAndMarkAbsent(
                SHOP_ID_IRRELEVANT,
                ImmutableList.of(testBid),
                AuctionOfferIdType.SHOP_OFFER_ID
        );

        assertThat(testBid, hasOfferId(SHOP_FEED_OFFER_ID_MISSING));
        assertThat(testBid, hasError("OFFER_NOT_FOUND"));
        assertThat(testBid, hasNoSearchQuery());
    }

    @DisplayName("SHOP_OFFER_ID. Для найденного оффера выставляется поисковый запрос если отсутствует")
    @Test
    void test_checkOffersExistenceInReport_setSearchQueryIfMissing() throws IOException {
        mockCheckOffers(CHECK_OFFERS_OK);
        OfferBid testBid = buildSomeBid(SHOP_FEED_OFFER_ID_475690, NO_SEARCH_QUERY);

        checker.checkExistentAndMarkAbsent(
                SHOP_ID_IRRELEVANT,
                ImmutableList.of(testBid),
                AuctionOfferIdType.SHOP_OFFER_ID
        );

        assertThat(testBid, hasOfferId(SHOP_FEED_OFFER_ID_475690));
        assertThat(testBid, hasNoError());
        assertThat(testBid, hasSearchQuery("unknown"));
    }

    @DisplayName("SHOP_OFFER_ID. Для найденного оффера не изменяется поисковый запрос если уже задан")
    @Test
    void
    test_checkOffersExistenceInReport_dontChangeSearchQuery_when_found() throws IOException {
        mockCheckOffers(CHECK_OFFERS_OK);
        OfferBid testBid = buildSomeBid(SHOP_FEED_OFFER_ID_475690, "must_not_be_changed");

        checker.checkExistentAndMarkAbsent(
                SHOP_ID_IRRELEVANT,
                ImmutableList.of(testBid),
                AuctionOfferIdType.SHOP_OFFER_ID
        );

        assertThat(testBid, hasOfferId(SHOP_FEED_OFFER_ID_475690));
        assertThat(testBid, hasNoError());
        assertThat(testBid, hasSearchQuery("must_not_be_changed"));
    }

    @DisplayName("Exceptions handling test")
    @Test
    void test_checkOffersExistenceInReport_should_returnNotFound_when_nestedExceptionIsFiltered() {
        List<Throwable> nestedExceptions = ImmutableList.of(
                new MarketReportOverloadedException("some overload"),
                new AmbiguousReportAnswerException("some ambiguous")//,
        );

        for (Throwable ex : nestedExceptions) {

            AsyncMarketReportService marketReportServiceThatThrows = buildServiceThrowing(ex);

            ApiReportOfferExistenceValidator checker =
                    new ApiReportOfferExistenceValidator(
                            marketReportServiceThatThrows,
                            null,
                            checkOffersParamsService
                    );

            OfferBid testBid = buildSomeBid(
                    new AuctionOfferId("irrelevant"),
                    null
            );

            checker.checkExistentAndMarkAbsent(SHOP_ID_IRRELEVANT, ImmutableList.of(testBid), AuctionOfferIdType.TITLE);
            assertThat(
                    "must not throw when nested exception is:" + ex,
                    testBid,
                    hasError("OFFER_NOT_FOUND")
            );
        }
    }

    private void mockApiOfferInfo(String filename) throws IOException, SAXException {
        try (InputStream in = getClass().getResourceAsStream(filename)) {
            final ApiReportOfferExistenceValidator.ReportOfferExistenceParser parser
                    = new ApiReportOfferExistenceValidator.ReportOfferExistenceParser();

            parser.parseXmlStream(in);

            doReturn(CompletableFuture.completedFuture(parser))
                    .when(marketReportService)
                    .async(
                            MockitoHamcrest.argThat(hasPlace(MarketReportPlace.API_OFFERINFO)),
                            any()
                    );
        }
    }

    private void mockCheckOffers(String fileName) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(fileName)) {
            final CheckOffersResponseParser parser = new CheckOffersResponseParser();
            parser.parse(in);

            doReturn(CompletableFuture.completedFuture(parser.getResult()))
                    .when(marketReportService)
                    .async(
                            MockitoHamcrest.argThat(hasPlace(MarketReportPlace.CHECK_OFFERS)),
                            any()
                    );
        }
    }
}