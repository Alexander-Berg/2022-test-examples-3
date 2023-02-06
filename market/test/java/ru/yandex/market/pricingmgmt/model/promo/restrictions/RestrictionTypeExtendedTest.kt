package ru.yandex.market.pricingmgmt.model.promo.restrictions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.market.pricingmgmt.model.promo.AssortmentLoadMethod
import ru.yandex.market.pricingmgmt.model.promo.PromoMechanicsType
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RestrictionTypeExtendedTest {

    private fun getTestArguments(): Stream<Arguments> {
        val arguments = mutableListOf<Arguments>()
        for (restrictionType in RestrictionType.values()) {
            for (promoMechanicsType in PromoMechanicsType.values().filter { it != PromoMechanicsType.UNKNOWN }) {
                for (assortmentLoadMethod in AssortmentLoadMethod.values().filter { it != AssortmentLoadMethod.UNKNOWN }
                    .plus(null)) {

                    var restrictionTypeExtended =
                        when (restrictionType) {
                            RestrictionType.MSKU -> RestrictionTypeExtended.MSKU
                            RestrictionType.VENDOR -> RestrictionTypeExtended.VENDOR
                            RestrictionType.PARTNER -> RestrictionTypeExtended.PARTNER
                            RestrictionType.CATEGORY -> RestrictionTypeExtended.CATEGORY
                            RestrictionType.WAREHOUSE -> RestrictionTypeExtended.WAREHOUSE
                        }

                    if (restrictionTypeExtended == RestrictionTypeExtended.CATEGORY) {
                        if (promoMechanicsType == PromoMechanicsType.DIRECT_DISCOUNT) {
                            restrictionTypeExtended = RestrictionTypeExtended.CATEGORY_DISCOUNT
                        }

                        if (promoMechanicsType == PromoMechanicsType.BLUE_FLASH && assortmentLoadMethod == AssortmentLoadMethod.PI) {
                            restrictionTypeExtended = RestrictionTypeExtended.CATEGORY_DISCOUNT
                        }
                    }

                    arguments.add(
                        Arguments.of(
                            restrictionType,
                            promoMechanicsType,
                            assortmentLoadMethod,
                            restrictionTypeExtended
                        )
                    )
                }
            }
        }

        return arguments.stream()
    }

    @ParameterizedTest
    @MethodSource("getTestArguments")
    fun getTest(
        restrictionType: RestrictionType,
        promoMechanicsType: PromoMechanicsType,
        assortmentLoadMethod: AssortmentLoadMethod?,
        expectedRestrictionTypeExtended: RestrictionTypeExtended
    ) {
        assertEquals(
            expectedRestrictionTypeExtended,
            RestrictionTypeExtended.get(restrictionType, promoMechanicsType, assortmentLoadMethod)
        )
    }
}
