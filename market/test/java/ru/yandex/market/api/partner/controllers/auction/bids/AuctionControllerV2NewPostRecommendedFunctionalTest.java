package ru.yandex.market.api.partner.controllers.auction.bids;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
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

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.controllers.auction.model.RecommendationTarget;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.reset;

/**
 * Тесты на получение рекомендаций в контексте объединенной ставки.
 */
@DbUnitDataSet(before = "db/CheckOffersParams.csv")
class AuctionControllerV2NewPostRecommendedFunctionalTest extends AuctionControllerFunctionalCommon {

    static Stream<Arguments> args() {

        List<List<Object>> testCasesCore = ImmutableList.of(
                Lists.newArrayList(
                        "card",
                        "bids_recommender_card.xml",
                        "post_auction_recommendations_bids/bids_recommended_api_request",
                        "post_auction_recommendations_bids/bids_recommended_api_response_card",
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        POSITIONS_UNSPECIFIED,
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "card with limited pos",
                        "bids_recommender_card.xml",
                        "post_auction_recommendations_bids/bids_recommended_api_request",
                        "post_auction_recommendations_bids/bids_recommended_api_response_card_pos123",
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        POSITIONS_1_2_3,
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),

                Lists.newArrayList(
                        "card",
                        "bids_recommender_card.xml",
                        "post_auction_recommendations_bids/bids_recommended_api_request",
                        "post_auction_recommendations_bids/bids_recommended_api_response_card",
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        POSITIONS_UNSPECIFIED,
                        AuctionOfferIdType.TITLE
                ),


                //market search
                Lists.newArrayList(
                        "market-search",
                        "bids_recommender_market_search.xml",
                        "post_auction_recommendations_bids/bids_recommended_api_request_with_query",
                        "post_auction_recommendations_bids/bids_recommended_api_response_market_search",
                        ImmutableList.of(RecommendationTarget.MARKET_SEARCH),
                        POSITIONS_UNSPECIFIED,
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "market-search with limited pos",
                        "bids_recommender_market_search.xml",
                        "post_auction_recommendations_bids/bids_recommended_api_request_with_query",
                        "post_auction_recommendations_bids/bids_recommended_api_response_market_search_pos123",
                        ImmutableList.of(RecommendationTarget.MARKET_SEARCH),
                        POSITIONS_1_2_3,
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),

                //parallel search
                Lists.newArrayList(
                        "parallel-search",
                        "bids_recommender_parallel_search.xml",
                        "post_auction_recommendations_bids/bids_recommended_api_request_with_query",
                        "post_auction_recommendations_bids/bids_recommended_api_response_parallel_search",
                        ImmutableList.of(RecommendationTarget.SEARCH),
                        POSITIONS_UNSPECIFIED,
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "parallel-search with limited pos",
                        "bids_recommender_parallel_search.xml",
                        "post_auction_recommendations_bids/bids_recommended_api_request_with_query",
                        "post_auction_recommendations_bids/bids_recommended_api_response_parallel_search_pos123",
                        ImmutableList.of(RecommendationTarget.SEARCH),
                        POSITIONS_1_2_3,
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),

                //errors
                Lists.newArrayList(
                        "report answer is empty",
                        "bids_recommender_error_empty.xml",
                        "post_auction_recommendations_bids/bids_recommended_api_request",
                        "post_auction_recommendations_bids/bids_recommended_api_response_card_empty_response",
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        POSITIONS_UNSPECIFIED,
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "report no recommendations tag",
                        "bids_recommender_error_no_recs_tag.xml",
                        "post_auction_recommendations_bids/bids_recommended_api_request",
                        "post_auction_recommendations_bids/bids_recommended_api_response_card_no_recs_no_min_but_cur",
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        POSITIONS_UNSPECIFIED,
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "report empty recommendations/card",
                        "bids_recommender_error_empty_card_recs.xml",
                        "post_auction_recommendations_bids/bids_recommended_api_request",
                        "post_auction_recommendations_bids/bids_recommended_api_response_card_no_recs_but_meta",
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        POSITIONS_UNSPECIFIED,
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "report no recommendations/card tag",
                        "bids_recommender_error_no_target_recs_tag.xml",
                        "post_auction_recommendations_bids/bids_recommended_api_request",
                        "post_auction_recommendations_bids/bids_recommended_api_response_card_no_recs",
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        POSITIONS_UNSPECIFIED,
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "report explicit error=unavailable",
                        "bids_recommender_all_unavailable.xml",
                        "post_auction_recommendations_bids/bids_recommended_api_request",
                        "post_auction_recommendations_bids/bids_recommended_api_response_card_unavailable",
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        POSITIONS_UNSPECIFIED,
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Lists.newArrayList(
                        "offer not matched",
                        "bids_recommender_card_not_matched.xml",
                        "post_auction_recommendations_bids/bids_recommended_api_request",
                        "post_auction_recommendations_bids/bids_recommended_api_response_card_not_matched",
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        POSITIONS_UNSPECIFIED,
                        AuctionOfferIdType.SHOP_OFFER_ID
                )
        );

        return buildArgsOverFormats(testCasesCore);
    }

    static Stream<Arguments> badRequestArgs() {

        return Stream.of(
                Arguments.of(
                        "illegal characters in offerId",
                        "post_auction_recommendations_bids/bids_recommended_api_request_with_illegal_characters_by_id",
                        "OfferId must match [0-9a-z",
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Arguments.of(
                        "too long offerId",
                        "post_auction_recommendations_bids/bids_recommended_api_request_with_too_long_ident_by_id",
                        "OfferId must not be longer than 120",
                        AuctionOfferIdType.SHOP_OFFER_ID
                ),
                Arguments.of(
                        "too long offerName",
                        "post_auction_recommendations_bids/bids_recommended_api_request_with_too_long_ident_by_title",
                        "Offer name should be 512 characters at most",
                        AuctionOfferIdType.TITLE
                )
        );
    }

    @BeforeEach
    void before() {
        reset(auctionService);
        reset(apiMarketReportService);
        mockCanManageAuction(SHOP_ID_774);
    }

    @DisplayName("badRequests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("badRequestArgs")
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_badRequests(
            String description,
            String apiRequest,
            String expectedErrorSubstring,
            AuctionOfferIdType offerIdType
    ) {

        if (AuctionOfferIdType.TITLE == offerIdType) {
            mockTitleIdShop(SHOP_ID_774);
        } else {
            mockOfferIdShop(SHOP_ID_774);
        }


        final HttpClientErrorException ex = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getBidsRecommendationsAndVerify(
                        apiRequest,
                        null,
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        Format.JSON,
                        POSITIONS_UNSPECIFIED
                )
        );

        final String errorBlock = String.format("{\"error\":{\"code\":400,\"message\":\"%s", expectedErrorSubstring);
        assertThat(ex.getResponseBodyAsString(), StringContains.containsString(errorBlock));
    }

    @DisplayName("POST /v2/auction/recommendations/bids")
    @ParameterizedTest(name = "{0} (target={4} pos={5} type={6} {7})")
    @MethodSource("args")
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_postRecommendations(
            String desc,
            String reportResponse,
            String apiRequest,
            String apiResponse,
            List<RecommendationTarget> targets,
            Set<Integer> positions,
            AuctionOfferIdType offerIdType,
            Format apiInteractionFormat
    ) throws Exception {
        mockReportRecommendatorAnswer(reportResponse, targets);

        if (AuctionOfferIdType.TITLE == offerIdType) {
            mockTitleIdShop(SHOP_ID_774);
            apiRequest += "_by_title";
            apiResponse += "_by_title";
        } else {
            mockOfferIdShop(SHOP_ID_774);
            apiRequest += "_by_id";
            apiResponse += "_by_id";
        }

        getBidsRecommendationsAndVerify(
                apiRequest,
                apiResponse,
                targets,
                apiInteractionFormat,
                positions
        );

    }

    @DisplayName("SHOP_OFFER_ID. маркировать позиции с ошибкой и bid=0 как unknown error")
    @Test
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_treatZeroRecAsUnknownError() throws Exception {
        mockReportRecommendatorAnswer(
                "bids_recommender_card_with_error_bid_values.xml",
                ImmutableList.of(RecommendationTarget.MODEL_CARD)
        );

        mockOfferIdShop(SHOP_ID_774);

        getBidsRecommendationsAndVerify(
                "post_auction_recommendations_bids/bids_recommended_api_request_by_id",
                "post_auction_recommendations_bids/bids_recommended_api_response_card_error_bid_values_by_id",
                ImmutableList.of(RecommendationTarget.MODEL_CARD),
                Format.XML,
                POSITIONS_UNSPECIFIED
        );
    }

    @DisplayName("SHOP_OFFER_ID. показываем cbid/minCbid для карточного оффера")
    @Test
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_useCbidComponentsIfCard() throws JSONException, URISyntaxException, IOException {
        mockReportRecommendatorAnswer(
                "bids_recommender_card.xml",
                ImmutableList.of(RecommendationTarget.MODEL_CARD)
        );
        mockOfferIdShop(SHOP_ID_774);

        getBidsRecommendationsAndVerify(
                "post_auction_recommendations_bids/bids_recommended_api_request_by_id",
                "post_auction_recommendations_bids/bids_recommended_api_response_card_when_card_offer",
                ImmutableList.of(RecommendationTarget.MODEL_CARD),
                Format.XML,
                POSITIONS_1
        );
    }

    @DisplayName("SHOP_OFFER_ID. Если фид явно не задан в запросе и всего один то подставляем(для похода в репорт)")
    @Test
    @DbUnitDataSet(before = "db/ExistingFeed.csv")
    void test_noExplicitFeed() throws JSONException, URISyntaxException, IOException {
        mockReportRecommendatorAnswer(
                "bids_recommender_card.xml",
                ImmutableList.of(RecommendationTarget.MODEL_CARD)
        );

        mockOfferIdShop(SHOP_ID_774);

        getBidsRecommendationsAndVerify(
                "post_auction_recommendations_bids/bids_recommended_api_request_no_explicit_feed_by_id",
                "post_auction_recommendations_bids/bids_recommended_api_response_card_by_id",
                ImmutableList.of(RecommendationTarget.MODEL_CARD),
                Format.JSON,
                POSITIONS_UNSPECIFIED
        );
    }

    @DisplayName("SHOP_OFFER_ID. BadRequest. Если feed_id явно не задан в запросе и у магазина он не один")
    @Test
    @DbUnitDataSet(before = "db/ExistingFeedWithMultipleFeeds.csv")
    void test_badRequeset_noExplicitFeedAndManyExist() throws JSONException, IOException {
        mockReportRecommendatorAnswer(
                "bids_recommender_card.xml",
                ImmutableList.of(RecommendationTarget.MODEL_CARD)
        );

        mockOfferIdShop(SHOP_ID_774);

        final HttpClientErrorException ex = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getBidsRecommendationsAndVerify(
                        "post_auction_recommendations_bids/bids_recommended_api_request_no_explicit_feed_by_id",
                        null,
                        ImmutableList.of(RecommendationTarget.MODEL_CARD),
                        Format.JSON,
                        POSITIONS_UNSPECIFIED
                )
        );

        assertThat(
                ex.getResponseBodyAsString(),
                StringContains.containsString("{\"error\":{\"code\":400,\"message\":\"Feed-id should be specified")
        );
    }

    private void getBidsRecommendationsAndVerify(
            String apiRequestContentFile,
            String apiResponseContentFile,
            List<RecommendationTarget> targets,
            Format format,
            Set<Integer> positions
    ) throws JSONException, URISyntaxException {

        //запрос а api
        String query = loadAsString(apiRequestContentFile + "." + format.formatName());
        ResponseEntity<String> response = sendPost(
                urlV2PostRecommendedNew(format, targets, positions),
                query,
                format.getContentType()
        );

        //проверяем ответ
        assertResponseVsExpectedFile(apiResponseContentFile + "." + format.formatName(), response, format);
    }

}
