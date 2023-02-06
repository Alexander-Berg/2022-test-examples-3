package ru.yandex.market.partner.auction.servantlet.bulk.report_params.update;

import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.recommend.BidRecommendationRequest;
import ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasAuctionBulkQuery;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasOfferId;
import static ru.yandex.market.core.auction.matchers.BidRecommendationRequestFeatureMatchers.hasShopId;
import static ru.yandex.market.core.auction.model.AuctionGoalPlace.FIRST_PLACE;
import static ru.yandex.market.core.auction.model.AuctionGoalPlace.PREMIUM_FIRST_PLACE;
import static ru.yandex.market.partner.auction.HybridGoal.GoalType.MARKET_SEARCH;
import static ru.yandex.market.partner.auction.HybridGoal.GoalType.PARALLEL_SEARCH;

/**
 * Проверка трансляции параметра поискового запроса для поисховых рекомендаций в {@link AuctionBulkOfferBidsServantlet}
 * в запросах создания/изменения ставок с использованием целей.
 * Проверяем, что параметры переданные в рекомендатор, соответствуют ожиданиям.
 */
@ExtendWith(MockitoExtension.class)
class BulkUpdateQueryOverloadReportTranslationTest extends AuctionBulkServantletlMockBase {

    private static Stream<Arguments> testCasesForCreate() {
        return Stream.of(
                Arguments.of(
                        "Указанный общий поисковый запрос корректно используется при создании",
                        "&req1.goal.value=" + PREMIUM_FIRST_PLACE +
                                "&searchQuery=newGeneralQuery",
                        "newGeneralQuery",
                        AuctionOfferIdType.TITLE
                ),
                Arguments.of(
                        "Флаг offer_title_as_search_query имеет больший приоритет приориет, чем общий посковый переданный явно в аргументах",
                        "&req1.goal.value=" + PREMIUM_FIRST_PLACE +
                                "&searchQuery=newGeneralQuery" +
                                "&offer_title_as_search_query=true",
                        "someOfferName",
                        AuctionOfferIdType.TITLE
                )
        );
    }

    private static Stream<Arguments> testCasesForUpdate() {
        return Stream.of(
                Arguments.of(
                        "По умолчаниию поисковый запрос берется из существующего для ставки",
                        "&req1.goal.value=" + FIRST_PLACE,
                        SOME_OFFER_NAME,
                        AuctionOfferIdType.TITLE
                ),
                Arguments.of(
                        "Указанный явно поисковый запрос имеет больший приоритет, чем привязанный к ставке",
                        "&req1.goal.value=" + FIRST_PLACE +
                                "&req1.searchQuery=newOfferSearchQuery",
                        "newOfferSearchQuery",
                        AuctionOfferIdType.TITLE
                ),
                Arguments.of(
                        "Указанный общий поисковый запрос имеет меньший приоритет, чем поофферный",
                        "&req1.goal.value=" + PREMIUM_FIRST_PLACE +
                                "&req1.searchQuery=newOfferSearchQuery" +
                                "&searchQuery=newGeneralQuery",
                        "newOfferSearchQuery",
                        AuctionOfferIdType.TITLE
                ),
                Arguments.of(
                        "Указанный общий поисковый запрос имеет приоритет над уже существующим для ставки",
                        "&req1.goal.value=" + PREMIUM_FIRST_PLACE +
                                "&searchQuery=newGeneralQuery",
                        "newGeneralQuery",
                        AuctionOfferIdType.TITLE
                ),
                Arguments.of(
                        "Флаг offer_title_as_search_query имеет приориет над поисковым запросом, переданными явно в аргументах",
                        "&req1.goal.value=" + PREMIUM_FIRST_PLACE +
                                "&searchQuery=newGeneralQuery" +
                                "&offer_title_as_search_query=true",
                        "someOfferName",
                        AuctionOfferIdType.TITLE
                )
        );
    }

    @BeforeEach
    void beforeEach() {
        auctionBulkOfferBidsServantlet.configure();
        generalBulkServantletInit(recommendationsService);

        mockBidLimits();
        mockRegionsAndTariff();

        mockCheckHomeRegionInIndex();
        mockRecommendationServiceEmptyCalculateResult();
        mockOfferExists();

        mockServRequestCrudActionUPDATE();
        mockServRequestIdentificationParams();
        mockShopAuctionType(AuctionOfferIdType.TITLE);
    }

    @MethodSource("testCasesForUpdate")
    @DisplayName("Запрос в репорте параллельных рекомендаций при изменении ставки")
    @ParameterizedTest(name = "{0}")
    void test_updateBidParallelSearch(
            String desc,
            String servantletQueryMixin,
            String expectedSetQuery,
            AuctionOfferIdType auctionOfferIdType
    ) {
        mockShopAuctionType(auctionOfferIdType);
        mockAuctionExistingBid(SOME_TITLE_OFFER_ID, PARAM_DATASOURCE_ID);

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                servantletQueryMixin +
                "&req1.goal.type=" + PARALLEL_SEARCH
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest recRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(recRequest, hasOfferId(SOME_TITLE_OFFER_ID));
        assertThat(recRequest, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(recRequest, hasAuctionBulkQuery(expectedSetQuery));
    }

    @MethodSource("testCasesForCreate")
    @DisplayName("Запрос в репорте параллельных рекомендаций при создании ставки")
    @ParameterizedTest(name = "{0}")
    void test_createBidParallelSearch(
            String desc,
            String servantletQueryMixin,
            String expectedSetQuery,
            AuctionOfferIdType auctionOfferIdType
    ) {
        mockShopAuctionType(auctionOfferIdType);

        mockAuctionHasNoBids();

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                servantletQueryMixin +
                "&req1.goal.type=" + PARALLEL_SEARCH
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest recRequest = extractRecommendatorPassedRequest(parallelSearchBidRecommendator);
        assertThat(recRequest, hasOfferId(SOME_TITLE_OFFER_ID));
        assertThat(recRequest, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(recRequest, hasAuctionBulkQuery(expectedSetQuery));
    }

    @MethodSource("testCasesForUpdate")
    @DisplayName("Запрос в репорте маркетных рекомендаций при изменении ставки")
    @ParameterizedTest(name = "{0}")
    void test_updateBidMarketSearch(
            String desc,
            String servantletQueryMixin,
            String expectedSetQuery
    ) {
        mockAuctionExistingBid(SOME_TITLE_OFFER_ID, PARAM_DATASOURCE_ID);

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                servantletQueryMixin +
                "&req1.goal.type=" + MARKET_SEARCH
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest recRequest = extractRecommendatorPassedRequest(marketSearchBidRecommendator);
        assertThat(recRequest, hasOfferId(SOME_TITLE_OFFER_ID));
        assertThat(recRequest, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(recRequest, hasAuctionBulkQuery(expectedSetQuery));
    }

    @MethodSource("testCasesForCreate")
    @DisplayName("Запрос в репорте маркетных рекомендаций при создании ставки")
    @ParameterizedTest(name = "{0}")
    void test_createBidMarketSearch(
            String desc,
            String servantletQueryMixin,
            String expectedSetQuery
    ) {
        mockAuctionHasNoBids();

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                servantletQueryMixin +
                "&req1.goal.type=" + MARKET_SEARCH
        );

        auctionBulkOfferBidsServantlet.process(servRequest, servResponse);

        BidRecommendationRequest recRequest = extractRecommendatorPassedRequest(marketSearchBidRecommendator);
        assertThat(recRequest, hasOfferId(SOME_TITLE_OFFER_ID));
        assertThat(recRequest, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(recRequest, hasAuctionBulkQuery(expectedSetQuery));
    }

}