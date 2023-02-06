package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader

import java.util.stream.Stream
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsType

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerPromoExtensionTypeConversionTest {
    @ParameterizedTest
    @MethodSource("promoExtensionTypeConversionParams")
    fun testPromoExtensionTypeConversion(type: PromoactionsType, associatedNumberExpected: Int) {

        Assertions.assertThat(corePromoExtensionTypeToBsType(type)).isEqualTo(associatedNumberExpected)
    }

    private fun promoExtensionTypeConversionParams(): Stream<Arguments> =
        Stream.of(
            Arguments.arguments(PromoactionsType.discount, 0),
            Arguments.arguments(PromoactionsType.profit, 1),
            Arguments.arguments(PromoactionsType.cashback, 2),
            Arguments.arguments(PromoactionsType.gift, 3),
            Arguments.arguments(PromoactionsType.free, 4),
            Arguments.arguments(PromoactionsType.installment, 5),
        )
}
