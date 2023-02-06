package ru.yandex.market.api.partner.controllers.auction;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.api.partner.controllers.auction.model.ApiRecommendationsRequest;
import ru.yandex.market.api.partner.controllers.auction.model.AuctionOffer;
import ru.yandex.market.api.partner.controllers.auction.model.RecommendationTarget;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.geobase.model.Region;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тесты, фиксирующие специфику построения контейнера {@link ApiRecommendationsRequest}.
 */
class ApiRecommendationsRequestTest {

    private ApiRecommendationsRequest.Builder unfinishedBuilder;

    private static Stream<Arguments> partialBuilderArgs() {
        return Stream.of(
                Arguments.of(
                        (Function<ApiRecommendationsRequest.Builder, ApiRecommendationsRequest.Builder>) builder
                                -> builder.withShopId(null),
                        NullPointerException.class,
                        "ShopId must not be null"
                ),
                Arguments.of(
                        (Function<ApiRecommendationsRequest.Builder, ApiRecommendationsRequest.Builder>) builder
                                -> builder.withPositions(null),
                        NullPointerException.class,
                        "Positions must not be null"
                ),
                Arguments.of(
                        (Function<ApiRecommendationsRequest.Builder, ApiRecommendationsRequest.Builder>) builder
                                -> builder.withRegion(null),
                        NullPointerException.class,
                        "Region must not be null"
                ),
                Arguments.of(
                        (Function<ApiRecommendationsRequest.Builder, ApiRecommendationsRequest.Builder>) builder
                                -> builder.withOffers(null),
                        NullPointerException.class,
                        "Offers must not be null"
                )
        );
    }

    @BeforeEach
    void beforeEach() {
        unfinishedBuilder = ApiRecommendationsRequest.builder()
                .withRegion(new Region(1L, "region", null))
                .withPositions(ImmutableSet.of(123))
                .withTargets(ImmutableSet.of(RecommendationTarget.MARKET_SEARCH))
                .withOffers(Collections.emptySet())
                .withShopId(45L);
    }

    @MethodSource(value = "partialBuilderArgs")
    @ParameterizedTest(name = "{2}")
    void test_partialBuilder(Function<ApiRecommendationsRequest.Builder, ApiRecommendationsRequest.Builder> fieldResetter,
                             Class<Exception> expectedExceptionClazz,
                             String expectedMsg
    ) {

        final Exception response = assertThrows(
                expectedExceptionClazz,
                () -> fieldResetter.apply(unfinishedBuilder).build()
        );

        assertEquals(expectedMsg, response.getMessage());
    }

    /**
     * Внутреннее хранение должно сохранять переданную коллекцию as-is.
     */
    @DisplayName("Порядок ТП не меняется")
    @Test
    void test_build_when_passedOffers_should_savePassedCollectionAsIs() {
        AuctionOffer offer1 = new AuctionOffer();
        offer1.setOfferId(new AuctionOfferId("someId1"));
        AuctionOffer offer2 = new AuctionOffer();
        offer2.setOfferId(new AuctionOfferId("someId2"));

        Set<AuctionOffer> PROBE_OFFERS_SET = ImmutableSet.of(
                offer1, offer2
        );

        ApiRecommendationsRequest request = unfinishedBuilder
                .withOffers(PROBE_OFFERS_SET)
                .build();

        assertThat(PROBE_OFFERS_SET, is(request.getOffers()));
    }

}
