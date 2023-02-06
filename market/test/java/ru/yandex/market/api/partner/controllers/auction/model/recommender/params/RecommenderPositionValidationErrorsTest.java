package ru.yandex.market.api.partner.controllers.auction.model.recommender.params;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.api.partner.controllers.auction.model.ApiRecommendationsRequest;
import ru.yandex.market.api.partner.controllers.auction.model.AuctionOffer;
import ru.yandex.market.api.partner.controllers.auction.model.RecommendationTarget;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiAuctionAbstractRecommender;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiAuctionMarketSearchRecommender;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiAuctionModelCardRecommender;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiAuctionParallelSearchRecommender;
import ru.yandex.market.api.partner.request.InvalidRequestException;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.auction.recommend.BidRecommendator;
import ru.yandex.market.core.geobase.model.Region;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Проверка границ и количества и значений позиций для 4х рекомендаторов:
 * search,market-search,model-card,hybrid
 */
class RecommenderPositionValidationErrorsTest {

    private static final Long SHOP_ID = 123L;
    private static final Set<Integer> POSITIONS_HUGE_NUMBER = ImmutableSet.of(1000);
    private static final Set<Integer> POSITIONS_INVALID_NUMBER = ImmutableSet.of(-1, 0);
    private static final Set<Integer> POSITIONS_LONG_LIST = IntStream.range(1, 20).boxed().collect(Collectors.toSet());
    /**
     * ТП котоыре нужны только для того чтобы отрабатывал функционал иетрирующий оп офферам.
     * На содержимое этих ТП полагаться нельзя.
     */
    private static final AuctionOffer IRRELEVANT_PARTIAL_OFFER = new AuctionOffer();
    private static final Set<AuctionOffer> IRRELEVANT_PARTIAL_OFFERS = ImmutableSet.of(IRRELEVANT_PARTIAL_OFFER);
    private static final Long SOME_REGION_ID = 2377L;

    static {
        IRRELEVANT_PARTIAL_OFFER.setOfferId(new AuctionOfferId("SomeOfferId"));
    }

    private static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(
                        "Идентификатор позиции больше максимального",
                        new ApiAuctionMarketSearchRecommender(),
                        RecommendationTarget.MARKET_SEARCH,
                        POSITIONS_HUGE_NUMBER,
                        "Position number should be positive and not greater than"
                ),
                Arguments.of(
                        "Идентификатор позиции больше максимального",
                        new ApiAuctionParallelSearchRecommender(Mockito.mock(BidRecommendator.class)),
                        RecommendationTarget.SEARCH,
                        POSITIONS_HUGE_NUMBER,
                        "Position number should be positive and not greater than"
                ),
                Arguments.of(
                        "Идентификатор позиции больше максимального",
                        new ApiAuctionModelCardRecommender(),
                        RecommendationTarget.MODEL_CARD,
                        POSITIONS_HUGE_NUMBER,
                        "Position number should be positive and not greater than"
                ),
                Arguments.of(
                        "Недопустимое значение в идентификаторе позиции",
                        new ApiAuctionMarketSearchRecommender(),
                        RecommendationTarget.MARKET_SEARCH,
                        POSITIONS_INVALID_NUMBER,
                        "Position number should be positive and not greater than"
                ),
                Arguments.of(
                        "Недопустимое значение в идентификаторе позиции",
                        new ApiAuctionParallelSearchRecommender(Mockito.mock(BidRecommendator.class)),
                        RecommendationTarget.SEARCH,
                        POSITIONS_INVALID_NUMBER,
                        "Position number should be positive and not greater than"
                ),
                Arguments.of(
                        "Недопустимое значение в идентификаторе позиции",
                        new ApiAuctionModelCardRecommender(),
                        RecommendationTarget.MODEL_CARD,
                        POSITIONS_INVALID_NUMBER,
                        "Position number should be positive and not greater than"
                ),
                Arguments.of(
                        "Перечень позиций больше чем допустимые",
                        new ApiAuctionMarketSearchRecommender(),
                        RecommendationTarget.MARKET_SEARCH,
                        POSITIONS_LONG_LIST,
                        "Too many positions specified"
                ),
                Arguments.of(
                        "Перечень позиций больше чем допустимые",
                        new ApiAuctionParallelSearchRecommender(Mockito.mock(BidRecommendator.class)),
                        RecommendationTarget.SEARCH,
                        POSITIONS_LONG_LIST,
                        "Too many positions specified"
                ),
                Arguments.of(
                        "Перечень позиций больше чем допустимые",
                        new ApiAuctionModelCardRecommender(),
                        RecommendationTarget.MODEL_CARD,
                        POSITIONS_LONG_LIST,
                        "Too many positions specified"
                )
        );
    }

    @ParameterizedTest(name = "{2} - {0}")
    @MethodSource(value = "args")
    @DisplayName("Валидация запрашиваемых позиций в рекомендациях")
    void test_positionValidation(
            String description,
            ApiAuctionAbstractRecommender apiAuctionAbstractRecommender,
            RecommendationTarget target,
            Set<Integer> positions,
            String expectedExceptionString
    ) {
        final ApiRecommendationsRequest request = ApiRecommendationsRequest.builder()
                .withShopId(SHOP_ID)
                .withOffers(new HashSet<>(IRRELEVANT_PARTIAL_OFFERS))
                .withPositions(positions)
                .withTargets(ImmutableSet.of(target))
                .withRegion(new Region(SOME_REGION_ID, "Some region", null))
                .build();

        final InvalidRequestException ex = Assertions.assertThrows(
                InvalidRequestException.class,
                () -> apiAuctionAbstractRecommender.getRecommendations(request)
        );

        assertThat(ex.getMessage(), StringContains.containsString(expectedExceptionString));
    }

}
