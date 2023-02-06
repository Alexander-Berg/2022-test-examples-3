package ru.yandex.market.api.partner.controllers.auction.bids;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.unitils.reflectionassert.ReflectionAssert;
import org.xml.sax.SAXException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.controllers.auction.model.OfferBid;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiReportOfferExistenceValidator;
import ru.yandex.market.api.partner.report.ApiMarketReportService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.auction.model.AuctionBidStatus;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.model.BidPlace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.reset;

/**
 * Тесты на ручки получения/установки ставок по значениям после объединения в единую bid компоненту.
 */
@DbUnitDataSet(before = "db/CheckOffersParams.csv")
class AuctionControllerV2NewValuesFunctionalTest extends AuctionControllerFunctionalCommon {
    /**
     * Формат сообщения если кидается {@link ru.yandex.market.api.partner.apisupport.ApiInvalidRequestException}.
     */
    private static final String MODERN_STYLE_BAD_REQ_MESSAGE_TMPL = "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"BAD_REQUEST\",\"message\":\"%s";

    /**
     * Формат сообщения для старого формата ошибок.
     */
    private static final String OLD_STYLE_BAD_REQ_MESSAGE_TMPL = "{\"error\":{\"code\":400,\"message\":\"%s";

    /**
     * В тестах как вырожденный случай значения для карточной({@link BidPlace#CARD})
     * и поисковой({@link BidPlace#SEARCH}) компоненты заданы различными, но при синхронизации они всегда одинаковые.
     * В дальнейшем(когда выпилят в репорте и индексаторе) будет оставлена только одна {@link BidPlace#SEARCH},
     * потому в тестах проверяется, что корректно подтягивается всегда {@link BidPlace#SEARCH}
     */
    private static Stream<Arguments> getBidsArgs() {
        List<List<Object>> testCasesCore = ImmutableList.of(
                Lists.newArrayList(
                        "bids and explicit reset flag",
                        "post_auction_bids/post_bids_api_request",
                        "post_auction_bids/post_bids_api_response",
                        ImmutableList.of(TITLE_ID_501, TITLE_ID_502),
                        prepareBidsForIds(
                                ImmutableList.of(TITLE_ID_501, TITLE_ID_502),
                                BiddingRequestBuilder.builder()
                                        .setBid(BidPlace.CARD, BID_VALUE_11)
                                        .setBid(BidPlace.SEARCH, BID_VALUE_10)
                                        .build()
                        ),
                        AuctionOfferIdType.TITLE
                ),
                Lists.newArrayList(
                        "bids and explicit reset flag",
                        "post_auction_bids/post_bids_api_request",
                        "post_auction_bids/post_bids_api_response",
                        ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                        prepareBidsForIds(
                                ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                                BiddingRequestBuilder.builder()
                                        .setBid(BidPlace.CARD, BID_VALUE_11)
                                        .setBid(BidPlace.SEARCH, BID_VALUE_10)
                                        .setBid(BidPlace.FLAG_DONT_PULL_BIDS, BID_VALUE_0)
                                        .build()
                        ),
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "bids and flag not specified",
                        "post_auction_bids/post_bids_api_request",
                        "post_auction_bids/post_bids_api_response",
                        ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                        prepareBidsForIds(
                                ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                                BiddingRequestBuilder.builder()
                                        .setBid(BidPlace.CARD, BID_VALUE_11)
                                        .setBid(BidPlace.SEARCH, BID_VALUE_10)
                                        .build()
                        ),
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "bids and flag set",
                        "post_auction_bids/post_bids_api_request",
                        "post_auction_bids/post_bids_api_response_flag_set",
                        ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                        prepareBidsForIds(
                                ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                                BiddingRequestBuilder.builder()
                                        .setBid(BidPlace.CARD, BID_VALUE_11)
                                        .setBid(BidPlace.SEARCH, BID_VALUE_10)
                                        .setBid(BidPlace.FLAG_DONT_PULL_BIDS, BID_VALUE_1)
                                        .build()
                        ),
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "existing cbid with absent bid leads to error",
                        "post_auction_bids/post_bids_api_request",
                        "post_auction_bids/post_bids_api_response_unknown_no_meta",
                        ImmutableList.of(TITLE_ID_501, TITLE_ID_502),
                        prepareBidsForIds(
                                ImmutableList.of(TITLE_ID_501, TITLE_ID_502),
                                BiddingRequestBuilder.builder()
                                        .setBid(BidPlace.CARD, BID_VALUE_11)
                                        .build()
                        ),
                        AuctionOfferIdType.TITLE
                ),
                Lists.newArrayList(
                        "only bid component is ok. card not required",
                        "post_auction_bids/post_bids_api_request",
                        "post_auction_bids/post_bids_api_response",
                        ImmutableList.of(TITLE_ID_501, TITLE_ID_502),
                        prepareBidsForIds(
                                ImmutableList.of(TITLE_ID_501, TITLE_ID_502),
                                BiddingRequestBuilder.builder()
                                        .setBid(BidPlace.SEARCH, BID_VALUE_10)
                                        .build()
                        ),
                        AuctionOfferIdType.TITLE
                ),
                Lists.newArrayList(
                        "flag only offer",
                        "post_auction_bids/post_bids_api_request",
                        "post_auction_bids/post_bids_api_response_flag_only_set",
                        ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                        prepareBidsForIds(
                                ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                                BiddingRequestBuilder.builder()
                                        .setBid(BidPlace.FLAG_DONT_PULL_BIDS, BID_VALUE_1)
                                        .build(),
                                BID_MOD_TIME,
                                AuctionBidStatus.PUBLISHED
                        ),
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),

                //statuses
                Lists.newArrayList(
                        "status=published",
                        "post_auction_bids/post_bids_api_request",
                        "post_auction_bids/post_bids_api_response_published",
                        ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                        prepareBidsForIds(
                                ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                                BiddingRequestBuilder.builder()
                                        .setBid(BidPlace.CARD, BID_VALUE_11)
                                        .setBid(BidPlace.SEARCH, BID_VALUE_10)
                                        .setBid(BidPlace.FLAG_DONT_PULL_BIDS, BID_VALUE_1)
                                        .build(),
                                BID_MOD_TIME,
                                AuctionBidStatus.PUBLISHED
                        ),
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "status=indexing",
                        "post_auction_bids/post_bids_api_request",
                        "post_auction_bids/post_bids_api_response_indexing",
                        ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                        prepareBidsForIds(
                                ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                                BiddingRequestBuilder.builder()
                                        .setBid(BidPlace.CARD, BID_VALUE_11)
                                        .setBid(BidPlace.SEARCH, BID_VALUE_10)
                                        .setBid(BidPlace.FLAG_DONT_PULL_BIDS, BID_VALUE_1)
                                        .build(),
                                BID_MOD_TIME,
                                AuctionBidStatus.INDEXING
                        ),
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "status=offer_not_found",
                        "post_auction_bids/post_bids_api_request",
                        "post_auction_bids/post_bids_api_response_not_found",
                        ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                        prepareBidsForIds(
                                ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                                BiddingRequestBuilder.builder()
                                        .setBid(BidPlace.CARD, BID_VALUE_11)
                                        .setBid(BidPlace.SEARCH, BID_VALUE_10)
                                        .setBid(BidPlace.FLAG_DONT_PULL_BIDS, BID_VALUE_1)
                                        .build(),
                                BID_MOD_TIME,
                                AuctionBidStatus.ERROR_OFFER_NOT_FOUND
                        ),
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "status=offer_not_found when no bid nor cbid nor flag",
                        "post_auction_bids/post_bids_api_request",
                        "post_auction_bids/post_bids_api_response_not_found_no_meta",
                        ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                        prepareBidsForIds(
                                ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                                BiddingRequestBuilder.builder().build(),
                                BID_MOD_TIME,
                                AuctionBidStatus.ERROR_OFFER_NOT_FOUND
                        ),
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "status=unknown",
                        "post_auction_bids/post_bids_api_request",
                        "post_auction_bids/post_bids_api_response_error_unknown",
                        ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                        prepareBidsForIds(
                                ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                                BiddingRequestBuilder.builder()
                                        .setBid(BidPlace.CARD, BID_VALUE_11)
                                        .setBid(BidPlace.SEARCH, BID_VALUE_10)
                                        .setBid(BidPlace.FLAG_DONT_PULL_BIDS, BID_VALUE_1)
                                        .build(),
                                BID_MOD_TIME,
                                AuctionBidStatus.ERROR_UNKNOWN
                        ),
                        AuctionOfferIdType.SHOP_OFFER_ID
                )
        );

        return buildArgsOverFormats(testCasesCore);
    }

    private static Stream<Arguments> getBidsBadRequestArgs() {

        return Stream.of(
                Arguments.of(
                        "illegal characters in offerId",
                        "post_auction_bids/post_bids_api_request_with_illegal_characters_by_id.json",
                        "OfferId must match [0-9a-z",
                        AuctionOfferIdType.SHOP_OFFER_ID,
                        OLD_STYLE_BAD_REQ_MESSAGE_TMPL
                ),
                Arguments.of(
                        "too long offerId",
                        "post_auction_bids/post_bids_api_request_with_too_long_ident_by_id.json",
                        "OfferId must not be longer than 120",
                        AuctionOfferIdType.SHOP_OFFER_ID,
                        OLD_STYLE_BAD_REQ_MESSAGE_TMPL
                ),
                Arguments.of(
                        "too long offerName",
                        "post_auction_bids/post_bids_api_request_with_too_long_ident_by_title.json",
                        "Offer name should be 512 characters at most",
                        AuctionOfferIdType.TITLE,
                        OLD_STYLE_BAD_REQ_MESSAGE_TMPL
                ),
                Arguments.of(
                        "wrong input format",
                        "post_auction_bids/post_bids_api_request_empty_wrong.json",
                        "No offers specified",
                        AuctionOfferIdType.TITLE,
                        MODERN_STYLE_BAD_REQ_MESSAGE_TMPL
                ),
                Arguments.of(
                        "no offers specified",
                        "post_auction_bids/post_bids_api_request_empty_no_offers.json",
                        "No offers specified",
                        AuctionOfferIdType.TITLE,
                        MODERN_STYLE_BAD_REQ_MESSAGE_TMPL
                )
        );
    }

    private static Stream<Arguments> setBidsArgs() {

        List<List<Object>> testCasesCore = ImmutableList.of(
                Lists.newArrayList(
                        "only bid set",
                        "put_auction_bids/put_bids_api_request_bid_set",
                        "put_auction_bids/put_bids_api_response_bid_set",
                        prepareBidsPassedToAuctionService(
                                ImmutableList.of(TITLE_CORRECTED_ID),
                                BiddingRequestBuilder.builder(AuctionBidValues.KEEP_OLD_BID_VALUE)
                                        .setBid(BidPlace.CARD, BID_VALUE_10)
                                        .setBid(BidPlace.SEARCH, BID_VALUE_10)
                                        .build()
                        ),
                        AuctionOfferIdType.TITLE
                ),
                Lists.newArrayList(
                        "only bid set",
                        "put_auction_bids/put_bids_api_request_bid_set",
                        "put_auction_bids/put_bids_api_response_bid_set",
                        prepareBidsPassedToAuctionService(
                                ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                                BiddingRequestBuilder.builder(AuctionBidValues.KEEP_OLD_BID_VALUE)
                                        .setBid(BidPlace.CARD, BID_VALUE_10)
                                        .setBid(BidPlace.SEARCH, BID_VALUE_10)
                                        .build(),
                                "unknown"
                        ),
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "bid and flag set",
                        "put_auction_bids/put_bids_api_request_bid_set_and_flag_set",
                        "put_auction_bids/put_bids_api_response_bid_set_and_flag_set",
                        prepareBidsPassedToAuctionService(
                                ImmutableList.of(FEED_OID_100_1001),
                                BiddingRequestBuilder.builder(AuctionBidValues.KEEP_OLD_BID_VALUE)
                                        .setBid(BidPlace.CARD, BID_VALUE_10)
                                        .setBid(BidPlace.SEARCH, BID_VALUE_10)
                                        .setBid(BidPlace.FLAG_DONT_PULL_BIDS, BID_VALUE_1)
                                        .build(),
                                "unknown"
                        ),
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "bid set and flag reset",
                        "put_auction_bids/put_bids_api_request_bid_set_and_flag_reset",
                        "put_auction_bids/put_bids_api_response_bid_set_and_flag_reset",
                        prepareBidsPassedToAuctionService(
                                ImmutableList.of(FEED_OID_100_1001),
                                BiddingRequestBuilder.builder(AuctionBidValues.KEEP_OLD_BID_VALUE)
                                        .setBid(BidPlace.CARD, BID_VALUE_10)
                                        .setBid(BidPlace.SEARCH, BID_VALUE_10)
                                        .setBid(BidPlace.FLAG_DONT_PULL_BIDS, BID_VALUE_0)
                                        .build(),
                                "unknown"
                        ),
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "flag only set",
                        "put_auction_bids/put_bids_api_request_flag_only_set",
                        "put_auction_bids/put_bids_api_response_flag_only_set",
                        prepareBidsPassedToAuctionService(
                                ImmutableList.of(FEED_OID_100_1001),
                                BiddingRequestBuilder.builder(AuctionBidValues.KEEP_OLD_BID_VALUE)
                                        .setBid(BidPlace.FLAG_DONT_PULL_BIDS, BID_VALUE_1)
                                        .build(),
                                "unknown"
                        ),
                        AuctionOfferIdType.SHOP_OFFER_ID
                )
        );

        return buildArgsOverFormats(testCasesCore);
    }

    private static Stream<Arguments> setBidsBadRequestArgs() {

        return Stream.of(
                Arguments.of(
                        "illegal characters in offerId",
                        "put_auction_bids/put_bids_api_request_with_illegal_characters_by_id.json",
                        "OfferId must match [0-9a-z",
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Arguments.of(
                        "too long offerId",
                        "put_auction_bids/put_bids_api_request_with_too_long_ident_by_id.json",
                        "OfferId must not be longer than 120",
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Arguments.of(
                        "too long offerName",
                        "put_auction_bids/put_bids_api_request_with_too_long_ident_by_title.json",
                        "Offer name should be 512 characters at most",
                        AuctionOfferIdType.TITLE
                )
        );
    }

    @BeforeEach
    void before() {
        reset(auctionService);
        reset(apiMarketReportService);
    }

    @DisplayName("set bids with extended offerId characters")
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    @Test
    void test_setBidsWithExtendedOfferId() throws JSONException, URISyntaxException, IOException, SAXException {
        mockApiOfferInfoReportAnswer(apiMarketReportService, "report/api_offerinfo_for_casio.xml");
        mockCheckOffersReportAnswer(apiMarketReportService, "report/check_offers.json");

        mockOfferIdShop(SHOP_ID_774);

        setBidsAndVerify(
                "put_auction_bids/put_bids_api_request_extended_characters_by_id.json",
                "put_auction_bids/put_bids_api_response_extended_characters_by_id.json",
                prepareBidsPassedToAuctionService(
                        ImmutableList.of(FEED_OID_100_EXTENDED),
                        BiddingRequestBuilder.builder(AuctionBidValues.KEEP_OLD_BID_VALUE)
                                .setBid(BidPlace.SEARCH, BID_VALUE_10)
                                .setBid(BidPlace.CARD, BID_VALUE_10)
                                .build(),
                        "unknown"
                ),
                Format.JSON
        );
    }

    @DisplayName("Ставки можно сбрасывать, используя явное значение \"0\"")
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    @Test
    void test_setBidsWithExtendedResetValueOfferId() throws JSONException, URISyntaxException, IOException {
        mockCheckOffersReportAnswer(apiMarketReportService, "report/check_offers.json");

        mockOfferIdShop(SHOP_ID_774);

        setBidsAndVerify(
                "put_auction_bids/put_bids_api_request_explicit_reset_bid_by_id.json",
                "put_auction_bids/put_bids_api_response_explicit_reset_bid_by_id.json",
                prepareBidsPassedToAuctionService(
                        ImmutableList.of(FEED_OID_100_1001),
                        BiddingRequestBuilder.builder(AuctionBidValues.KEEP_OLD_BID_VALUE)
                                .setBid(BidPlace.SEARCH, OfferBid.CLEAR_BID_VALUE.toBigInteger())
                                .setBid(BidPlace.CARD, OfferBid.CLEAR_BID_VALUE.toBigInteger())
                                .build(),
                        "unknown"
                ),
                Format.JSON
        );
    }

    @DisplayName("POST /v2/auction/bids")
    @ParameterizedTest(name = "{0} (type={5} format={6})")
    @MethodSource("getBidsArgs")
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_getBids(
            String description,
            String apiRequestContentFile,
            String apiResponseContentFile,
            List<AuctionOfferId> biddingPassedParamBids,
            List<AuctionOfferBid> biddingMockedAnswerBids,
            AuctionOfferIdType offerIdType,
            Format format
    ) throws JSONException, URISyntaxException {

        if (AuctionOfferIdType.TITLE == offerIdType) {
            mockTitleIdShop(SHOP_ID_774);
            apiRequestContentFile += "_by_title." + format.formatName();
            apiResponseContentFile += "_by_title." + format.formatName();
        } else {
            mockOfferIdShop(SHOP_ID_774);
            apiRequestContentFile += "_by_id." + format.formatName();
            apiResponseContentFile += "_by_id." + format.formatName();
        }

        getBidsAndVerify(
                apiRequestContentFile,
                apiResponseContentFile,
                biddingPassedParamBids,
                biddingMockedAnswerBids,
                format
        );
    }


    @DisplayName("PUT /v2/auction/bids badRequests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("setBidsBadRequestArgs")
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_setBidsBadRequests(
            String description,
            String apiRequest,
            String expectedErrorSubstring,
            AuctionOfferIdType offerIdType
    ) throws IOException, SAXException {
        mockApiOfferInfoReportAnswer(apiMarketReportService, "report/api_offerinfo_for_casio.xml");
        mockCheckOffersReportAnswer(apiMarketReportService, "report/check_offers.json");


        if (AuctionOfferIdType.TITLE == offerIdType) {
            mockTitleIdShop(SHOP_ID_774);
        } else {
            mockOfferIdShop(SHOP_ID_774);
        }

        final HttpClientErrorException ex = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> setBidsAndVerify(
                        apiRequest,
                        null,
                        Collections.emptyList(),
                        Format.JSON
                )
        );

        final String errorBlock = String.format("{\"error\":{\"code\":400,\"message\":\"%s", expectedErrorSubstring);
        assertThat(ex.getResponseBodyAsString(), StringContains.containsString(errorBlock));
    }

    @DisplayName("POST /v2/auction/bids badRequests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getBidsBadRequestArgs")
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_getBidsBadRequests(
            String description,
            String apiRequest,
            String expectedErrorSubstring,
            AuctionOfferIdType offerIdType,
            String errorBadRequestTmplStyle
    ) {

        if (AuctionOfferIdType.TITLE == offerIdType) {
            mockTitleIdShop(SHOP_ID_774);
        } else {
            mockOfferIdShop(SHOP_ID_774);
        }

        final HttpClientErrorException ex = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getBidsAndVerify(
                        apiRequest,
                        null,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Format.JSON
                )
        );

        final String errorBlock = String.format(errorBadRequestTmplStyle, expectedErrorSubstring);
        assertThat(ex.getResponseBodyAsString(), StringContains.containsString(errorBlock));
    }

    @DisplayName("PUT /v2/auction/bids")
    @ParameterizedTest(name = "{0} (type={4} format={5})")
    @MethodSource("setBidsArgs")
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_setBids(
            String description,
            String apiRequestContentFile,
            String apiResponseContentFile,
            List<AuctionOfferBid> biddingPassedParamBids,
            AuctionOfferIdType offerIdType,
            Format format
    ) throws Exception {
        mockApiOfferInfoReportAnswer(apiMarketReportService, "report/api_offerinfo_for_casio.xml");
        mockCheckOffersReportAnswer(apiMarketReportService, "report/check_offers.json");

        if (AuctionOfferIdType.TITLE == offerIdType) {
            mockTitleIdShop(SHOP_ID_774);
            apiRequestContentFile += "_by_title." + format.formatName();
            apiResponseContentFile += "_by_title." + format.formatName();
        } else {
            mockOfferIdShop(SHOP_ID_774);
            apiRequestContentFile += "_by_id." + format.formatName();
            apiResponseContentFile += "_by_id." + format.formatName();
        }

        setBidsAndVerify(
                apiRequestContentFile,
                apiResponseContentFile,
                biddingPassedParamBids,
                format
        );
    }

    @DisplayName("take bid when offer not matched to card")
    @Test
    void test_getBids_when_nonCardOffer() throws JSONException, URISyntaxException {

        test_getBids(
                null,
                "post_auction_bids/post_bids_api_request",
                "post_auction_bids/post_bids_api_response_when_non_card_offer",
                ImmutableList.of(TITLE_ID_501, TITLE_ID_502),
                prepareBidsForIds(
                        ImmutableList.of(TITLE_ID_501, TITLE_ID_502),
                        BiddingRequestBuilder.builder()
                                .setBid(BidPlace.SEARCH, BID_VALUE_12)
                                .setBid(BidPlace.CARD, BID_VALUE_15)
                                .build()
                ),
                AuctionOfferIdType.TITLE,
                Format.XML
        );
    }

    @DisplayName("Если фид явно не задан в запросе и всего один то подставляем(для похода в api_offerinfo)")
    @Test
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_getBids_when_nonExplicitFeed() throws JSONException, URISyntaxException {

        test_getBids(
                null,
                "post_auction_bids/post_bids_api_request_no_explicit_feed",
                "post_auction_bids/post_bids_api_response",
                ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                prepareBidsForIds(
                        ImmutableList.of(FEED_OID_100_1001, FEED_OID_100_1002),
                        BiddingRequestBuilder.builder()
                                .setBid(BidPlace.SEARCH, BID_VALUE_10)
                                .build()
                ),
                AuctionOfferIdType.SHOP_OFFER_ID,
                Format.JSON
        );
    }

    @DisplayName("mark unknown_error when has no bid")
    @Test
    void test_getBids_when_hasNoBid() throws JSONException, URISyntaxException {

        test_getBids(
                null,
                "post_auction_bids/post_bids_api_request",
                "post_auction_bids/post_bids_api_response_unknown_no_meta",
                ImmutableList.of(TITLE_ID_501, TITLE_ID_502),
                prepareBidsForIds(
                        ImmutableList.of(TITLE_ID_501, TITLE_ID_502),
                        BiddingRequestBuilder.builder()
                                .setBid(BidPlace.CARD, BID_VALUE_15)
                                .build()
                ),
                AuctionOfferIdType.TITLE,
                Format.XML
        );

    }

    /**
     * smoke test на то, что при отсутствии идентификатора оффера в ответе репорта:
     * - ответ ПАПИ содержит маркер ошибки
     * - не происходит вызова сервиса аукциона на установку ставок.
     * Для примера взят формат взаимодействия - JSON.
     * См {@link ApiReportOfferExistenceValidator#checkOffersService}.
     */
    @DisplayName(".SHOP_OFFER_ID. error(offer_not_found) when check_offers has no requested id answer")
    @Test
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_error_when_setUnknownOffer() throws Exception {
        mockOfferIdShop(SHOP_ID_774);
        mockCheckOffersReportAnswer(apiMarketReportService, "report/check_offers_empty_response.json");

        setBidsAndVerifyFailed(
                "put_auction_bids/put_bids_api_request_bid_set_by_id.json",
                "put_auction_bids/put_bids_api_response_error_on_missing_offer_by_id.json",
                v2BidsNew(Format.JSON),
                Format.JSON
        );
    }

    /**
     * smoke test на то, что при пустом ответе репорта:
     * - ответ ПАПИ содержит маркер ошибки
     * - не происходит вызова сервиса аукциона на установку ставок.
     * Для примера взят формат взаимодействия - JSON.
     * См {@link ApiReportOfferExistenceValidator.ReportOfferExistenceParser}.
     */
    @DisplayName("TITLE. error(offer_not_found) when api_offerinfo has empty answer")
    @Test
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_error_when_setUnknownOfferAndTitle() throws Exception {
        mockTitleIdShop(SHOP_ID_774);
        mockApiOfferInfoReportAnswer(apiMarketReportService, "report/api_offerinfo_empty.xml");

        setBidsAndVerifyFailed(
                "put_auction_bids/put_bids_api_request_bid_set_by_title.json",
                "put_auction_bids/put_bids_api_response_error_on_missing_offer_by_title.json",
                v2BidsNew(Format.JSON),
                Format.JSON
        );
    }

    /**
     * Тут явная 500ка, так как явно что-то не так. Приемлемым будет даже пустой ответ, но не явная ошибка -
     * не хочется маскировать такие ситуации.
     */
    @DisplayName("error(500 - internal error) when check_offers returns explicit error")
    @Test
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_error500_when_explicitErrorFromReport() throws Exception {
        mockOfferIdShop(SHOP_ID_774);

        HttpServerErrorException ex = Assertions.assertThrows(
                HttpServerErrorException.class,
                //тут не суть важно какие файлы дял ответа так как ожидаем 500ку
                () -> setBidsAndVerifyFailed(
                        "put_auction_bids/put_bids_api_request_bid_set_by_id.json",
                        "put_auction_bids/put_bids_api_response_error_on_missing_offer_by_id.json",
                        v2BidsNew(Format.JSON),
                        Format.JSON
                )
        );

        assertThat(ex.getStatusCode(), Matchers.is(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @DisplayName("error(invalid_bid_value) when too big")
    @Test
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_error_when_setInvalidBid() throws Exception {
        mockOfferIdShop(SHOP_ID_774);
        mockCheckOffersReportAnswer(apiMarketReportService, "report/check_offers.json");

        setBidsAndVerifyFailed(
                "put_auction_bids/put_bids_api_request_invalid_bid_set_by_id.json",
                "put_auction_bids/put_bids_api_response_error_invalid_bid_set_by_id.json",
                v2BidsNew(Format.JSON),
                Format.JSON
        );
    }

    /**
     * Отправляем запрос на получение ставок с заданным payload содержимым и типом.
     * Проверяем получаемый в api ответ и сериализованную структуру в соответствии с форматом.
     *
     * @param apiRequestContentFile   - путь к файлу с фактическим содержимым запроса в АПИ
     * @param apiResponseContentFile  - путь к файлу с ожидаемым содержимым ответа в АПИ
     * @param biddingPassedParamBids  - ожидаемые параметры, переданные в auctionService
     * @param biddingMockedAnswerBids - мок ответа для auctionService'а эмулирующий существование запрошенных ставок
     * @param format                  - формат запроса и ответа в апи
     */
    private void getBidsAndVerify(
            String apiRequestContentFile,
            String apiResponseContentFile,
            List<AuctionOfferId> biddingPassedParamBids,
            List<AuctionOfferBid> biddingMockedAnswerBids,
            Format format
    ) throws JSONException, URISyntaxException {
        //моки
        mockExistingBidsForGet(
                SHOP_ID_774,
                biddingPassedParamBids,
                biddingMockedAnswerBids
        );

        //запрос а api
        String query = loadAsString(apiRequestContentFile);
        ResponseEntity<String> response = sendPost(v2BidsNew(format), query, format.getContentType());

        if (apiResponseContentFile != null) {
            //проверяем ответ
            assertResponseVsExpectedFile(apiResponseContentFile, response, format);
        }
    }

    /**
     * Отправляем запрос на установку ставок с заданным payload содержимым и типом.
     * Проверяем какие параметры ушли в сервис биддинга.
     * Проверяем получаемый в api ответ и сериализованную структуру в соответствии с форматом.
     *
     * @param apiRequestContentFile  - путь к файлу с фактическим содержимым запроса в АПИ
     * @param apiResponseContentFile - путь к файлу с ожидаемым содержимым ответа в АПИ
     * @param biddingPassedParamBids - ожидаемые параметры, переданные в auctionService
     * @param format                 - формат запроса и ответа в апи
     */
    private void setBidsAndVerify(
            String apiRequestContentFile,
            String apiResponseContentFile,
            List<AuctionOfferBid> biddingPassedParamBids,
            Format format
    ) throws JSONException, URISyntaxException {
        //моки
        mockCanManageAuction(SHOP_ID_774);
        mockCanMakeMbid(SHOP_ID_774);

        //запрос а api
        String query = loadAsString(apiRequestContentFile);
        ResponseEntity<String> response = sendPut(v2BidsNew(format), query, format.getContentType());

        //проверяем какие значения были переданы в auctionService при установке
        List<AuctionOfferBid> actualBiddingPassedBids = extractAuctionSetOffersBids(SHOP_ID_774);
        ReflectionAssert.assertLenientEquals(biddingPassedParamBids, actualBiddingPassedBids);

        //проверяем содержимое ответа в api
        assertResponseVsExpectedFile(apiResponseContentFile, response, format);
    }

}
