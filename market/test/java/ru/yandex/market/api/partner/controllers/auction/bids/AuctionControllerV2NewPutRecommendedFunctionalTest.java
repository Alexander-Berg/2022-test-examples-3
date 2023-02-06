package ru.yandex.market.api.partner.controllers.auction.bids;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.hamcrest.core.StringContains;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.controllers.auction.model.RecommendationTarget;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.BidPlace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.reset;

/**
 * Тесты на установку рекомендаций по ставкам с использование рекомендованных значений в контексте объединенной ставки.
 */
class AuctionControllerV2NewPutRecommendedFunctionalTest extends AuctionControllerFunctionalCommon {

    private static final Map<BidPlace, BigInteger> SEARCH_1ST_PLACE =
            BiddingRequestBuilder.builder(AuctionBidValues.KEEP_OLD_BID_VALUE)
                    .setBid(BidPlace.CARD, BID_VALUE_27)
                    .setBid(BidPlace.SEARCH, BID_VALUE_27)
                    .build();

    private static final Map<BidPlace, BigInteger> SEARCH_3RD_PLACE =
            BiddingRequestBuilder.builder(AuctionBidValues.KEEP_OLD_BID_VALUE)
                    .setBid(BidPlace.CARD, BID_VALUE_15)
                    .setBid(BidPlace.SEARCH, BID_VALUE_15)
                    .build();

    private static final Map<BidPlace, BigInteger> CARD_1ST_PLACE =
            BiddingRequestBuilder.builder(AuctionBidValues.KEEP_OLD_BID_VALUE)
                    .setBid(BidPlace.CARD, BID_VALUE_7069)
                    .setBid(BidPlace.SEARCH, BID_VALUE_7069)
                    .build();

    private static final Map<BidPlace, BigInteger> CARD_3RD_PLACE =
            BiddingRequestBuilder.builder(AuctionBidValues.KEEP_OLD_BID_VALUE)
                    .setBid(BidPlace.CARD, BID_VALUE_5158)
                    .setBid(BidPlace.SEARCH, BID_VALUE_5158)
                    .build();

    private static Stream<Arguments> args() {

        List<List<Object>> testCasesCore = ImmutableList.of(
                Lists.newArrayList(
                        "card 1st pos",
                        "bids_recommender_card.xml",
                        "put_auction_recommendations_bids/bids_recommended_api_request",
                        "put_auction_recommendations_bids/bids_recommended_api_response_card_pos1",
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        POSITION_1,
                        prepareBidsPassedToAuctionService(
                                ImmutableList.of(FEED_OID_100_1001),
                                CARD_1ST_PLACE,
                                REPORT_TITLE
                        )
                ),
                Lists.newArrayList(
                        "card 3rd pos",
                        "bids_recommender_card.xml",
                        "put_auction_recommendations_bids/bids_recommended_api_request",
                        "put_auction_recommendations_bids/bids_recommended_api_response_card_pos3",
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        POSITION_3,
                        prepareBidsPassedToAuctionService(
                                ImmutableList.of(FEED_OID_100_1001),
                                CARD_3RD_PLACE,
                                REPORT_TITLE
                        )
                ),
                Lists.newArrayList(
                        "search 1st pos",
                        "bids_recommender_parallel_search.xml",
                        "put_auction_recommendations_bids/bids_recommended_api_request_with_query",
                        "put_auction_recommendations_bids/bids_recommended_api_response_search_pos1",
                        ImmutableList.of(RecommendationTarget.SEARCH),
                        POSITION_1,
                        prepareBidsPassedToAuctionService(
                                ImmutableList.of(FEED_OID_100_1001),
                                SEARCH_1ST_PLACE,
                                REPORT_TITLE
                        )
                ),
                Lists.newArrayList(
                        "search 3rd pos",
                        "bids_recommender_parallel_search.xml",
                        "put_auction_recommendations_bids/bids_recommended_api_request_with_query",
                        "put_auction_recommendations_bids/bids_recommended_api_response_search_pos3",
                        ImmutableList.of(RecommendationTarget.SEARCH),
                        POSITION_3,
                        prepareBidsPassedToAuctionService(
                                ImmutableList.of(FEED_OID_100_1001),
                                SEARCH_3RD_PLACE,
                                REPORT_TITLE
                        )
                )

        );

        return buildArgsOverFormats(testCasesCore);
    }

    @BeforeEach
    void before() {
        reset(auctionService);
        mockCanManageAuction(SHOP_ID_774);
    }

    @DisplayName("PUT /v2/auction/recommendations/bids")
    @ParameterizedTest(name = "{0} (target={4} pos={5} {7})")
    @MethodSource("args")
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_putRecommended(
            String desc,
            String reportResponse,
            String apiRequest,
            String apiResponse,
            List<RecommendationTarget> targets,
            Integer position,
            List<AuctionOfferBid> biddingPassedParamBids,
            Format apiInteractionFormat
    ) throws Exception {
        mockReportRecommendatorAnswer(reportResponse, targets);

        mockOfferIdShop(SHOP_ID_774);
        apiRequest += "_by_id";
        apiResponse += "_by_id";

        putBidsRecommendedAndVerify(
                apiRequest,
                apiResponse,
                apiInteractionFormat,
                biddingPassedParamBids,
                urlV2PutRecommendedNew(apiInteractionFormat, targets, position)
        );
    }

    @DisplayName("bid with limited adjustment")
    @Test
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_putRecommended_explicitAdjustment() throws Exception {

        final Map<BidPlace, BigInteger> expectedActualBidSet = BiddingRequestBuilder.builder(
                AuctionBidValues.KEEP_OLD_BID_VALUE)
                .setBid(BidPlace.CARD, BID_VALUE_7997)
                .setBid(BidPlace.SEARCH, BID_VALUE_7997)
                .build();

        mockReportRecommendatorAnswer("bids_recommender_card.xml", ImmutableList.of(RecommendationTarget.MODEL_CARD));

        mockOfferIdShop(SHOP_ID_774);

        putBidsRecommendedAndVerify(
                "put_auction_recommendations_bids/bids_recommended_api_request_by_id",
                "put_auction_recommendations_bids/bids_recommended_api_response_card_pos1_adj_by_id",
                Format.XML,
                prepareBidsPassedToAuctionService(
                        ImmutableList.of(FEED_OID_100_1001),
                        expectedActualBidSet,
                        REPORT_TITLE
                ),
                urlV2PutRecommendedNew(
                        Format.XML,
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        POSITION_1,
                        BigDecimal.valueOf(100.0),
                        BigDecimal.valueOf(79.97)
                )
        );
    }

    /**
     * Пользовательское ограничение на значение ставки подтягивается к минимальному значению, полученному от репорта.
     */
    @DisplayName("bid with limited adjustment with min_bid pullup")
    @Test
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_putRecommended_explicitAdjustmentAndMinBid() throws Exception {

        final Map<BidPlace, BigInteger> expectedActualBidSet = BiddingRequestBuilder.builder(
                AuctionBidValues.KEEP_OLD_BID_VALUE)
                .setBid(BidPlace.CARD, BID_VALUE_12)
                .setBid(BidPlace.SEARCH, BID_VALUE_12)
                .build();

        mockReportRecommendatorAnswer("bids_recommender_card.xml", ImmutableList.of(RecommendationTarget.MODEL_CARD));

        mockOfferIdShop(SHOP_ID_774);

        putBidsRecommendedAndVerify(
                "put_auction_recommendations_bids/bids_recommended_api_request_by_id",
                "put_auction_recommendations_bids/bids_recommended_api_response_card_pos1_adj_and_minbid_by_id",
                Format.XML,
                prepareBidsPassedToAuctionService(
                        ImmutableList.of(FEED_OID_100_1001),
                        expectedActualBidSet,
                        REPORT_TITLE
                ),
                urlV2PutRecommendedNew(
                        Format.XML,
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        POSITION_1,
                        BigDecimal.valueOf(100.0),
                        BigDecimal.valueOf(0.02)//ограничено сверху но меньше min-bid от репорта, будет подтянуто
                )
        );
    }

    @DisplayName("bid adjustment with default max")
    @Test
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_putRecommended_explicitAdjustmentWithDefaultMax() throws Exception {

        final Map<BidPlace, BigInteger> expectedActualBidSet = BiddingRequestBuilder.builder(
                AuctionBidValues.KEEP_OLD_BID_VALUE)
                .setBid(BidPlace.CARD, BID_VALUE_8400)
                .setBid(BidPlace.SEARCH, BID_VALUE_8400)
                .build();

        mockReportRecommendatorAnswer("bids_recommender_card.xml", ImmutableList.of(RecommendationTarget.MODEL_CARD));

        mockOfferIdShop(SHOP_ID_774);

        putBidsRecommendedAndVerify(
                "put_auction_recommendations_bids/bids_recommended_api_request_by_id",
                "put_auction_recommendations_bids/bids_recommended_api_response_card_pos1_adj_default_max_by_id",
                Format.XML,
                prepareBidsPassedToAuctionService(
                        ImmutableList.of(FEED_OID_100_1001),
                        expectedActualBidSet,
                        REPORT_TITLE
                ),
                urlV2PutRecommendedNew(
                        Format.XML,
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        POSITION_1,
                        BigDecimal.valueOf(100.0),
                        null
                )
        );
    }

    @DisplayName("error(bad request) when no query in search request")
    @Test
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_error_when_missingQuery() {
        HttpClientErrorException ex = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> test_putRecommended(
                        null,
                        "bids_recommender_parallel_search.xml",
                        "put_auction_recommendations_bids/bids_recommended_api_request",
                        "put_auction_recommendations_bids/bids_recommended_api_response_card_pos1",
                        ImmutableList.of(RecommendationTarget.SEARCH),
                        POSITION_1,
                        Collections.emptyList(),
                        Format.JSON
                )
        );

        assertThat(ex.getResponseBodyAsString(), StringContains.containsString("Query should be specified"));
    }

    private void putBidsRecommendedAndVerify(
            String apiRequestContentFile,
            String apiResponseContentFile,
            Format format,
            List<AuctionOfferBid> biddingPassedParamBids,
            String url
    ) throws JSONException {

        //запрос а api
        ResponseEntity<String> response = sendPut(
                url,
                loadAsString(apiRequestContentFile + "." + format.formatName()),
                format.getContentType()
        );

        //проверяем какие значения были переданы в auctionService при установке
        if (biddingPassedParamBids != null) {
            List<AuctionOfferBid> actualBiddingPassedBids = extractAuctionSetOffersBids(SHOP_ID_774);
            ReflectionAssert.assertLenientEquals(biddingPassedParamBids, actualBiddingPassedBids);
        }

        //проверяем ответ
        assertResponseVsExpectedFile(apiResponseContentFile + "." + format.formatName(), response, format);
    }

}
