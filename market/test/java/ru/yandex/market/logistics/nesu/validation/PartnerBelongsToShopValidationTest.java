package ru.yandex.market.logistics.nesu.validation;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.model.entity.Shop;

@DisplayName("Проверка принадлежности партнера магазину")
public class PartnerBelongsToShopValidationTest extends AbstractTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("arguments")
    @DisplayName("Проверка принадлежности партнера магазину")
    void partnerAndShopWithoutBusinessId(
        @SuppressWarnings("unused") String name,
        Shop shop,
        PartnerResponse partnerResponse,
        boolean result
    ) {
        softly.assertThat(PartnerValidations.belongsToShop(shop).check(partnerResponse).isValid()).isEqualTo(result);
    }

    @Nonnull
    private static Stream<Arguments> arguments() {
        return Stream.of(
            Arguments.of(
                "У магазина нет businessId, у партнера есть",
                new Shop().setMarketId(100L),
                PartnerResponse.newBuilder().businessId(200L).marketId(100L).build(),
                true
            ),
            Arguments.of(
                "У партнера нет businessId, у магазина есть",
                new Shop().setMarketId(100L).setBusinessId(200L),
                PartnerResponse.newBuilder().marketId(100L).build(),
                true
            ),
            Arguments.of(
                "У партнера и магазина нет businessId, marketId не совпадает",
                new Shop().setMarketId(200L),
                PartnerResponse.newBuilder().marketId(100L).build(),
                false
            ),
            Arguments.of(
                "У партнера и магазина есть businessId, marketId не совпадает",
                new Shop().setMarketId(200L).setBusinessId(300L),
                PartnerResponse.newBuilder().marketId(100L).businessId(300L).build(),
                true
            ),
            Arguments.of(
                "У партнера и магазина есть businessId, marketId и businessId не совпадает",
                new Shop().setMarketId(200L).setBusinessId(400L),
                PartnerResponse.newBuilder().marketId(100L).businessId(300L).build(),
                false
            )
        );
    }
}
