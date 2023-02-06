package ru.yandex.market.contentmapping.services.rules

import io.kotest.assertions.asClue
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue.NumericValue
import ru.yandex.market.contentmapping.dto.model.*

class MarketValuesResolverTest {
    private val resolver = MarketValuesResolver()
    private val location = ValueSourceLocation(ShopModel.SHOP_OFFER_DESCRIPTION, 1, 2, -1, -1, -1, -1)

    @Test
    fun `it should select value with maximum source`() {
        val formalized1 = MarketParameterValue(1L, ValueSource.FORMALIZATION, value = NumericValue(10.0), rank = 10)
        val rule1 = MarketParameterValue(1L, ValueSource.RULE, value = NumericValue(20.0), rank = 1)
        val rule2 = MarketParameterValue(2L, ValueSource.RULE, value = NumericValue(10.0), rank = 1)
        val values = listOf(formalized1, rule1, rule2).groupBy { it.parameterId }

        resolver.resolveMarketValues(values) shouldContainAll mapOf(
                1L to listOf(rule1),
                2L to listOf(rule2)
        )
    }

    @Test
    fun `it should clean positions for manual values without formalization`() {
        val formalized = MarketParameterValue(1L, ValueSource.FORMALIZATION, NumericValue(10.0), valPos = location)
        val manualOther = MarketParameterValue(1L, ValueSource.MANUAL, NumericValue(20.0), valPos = location)

        val values = listOf(formalized, manualOther).groupBy { it.parameterId }

        val resolved = resolver.resolveMarketValues(values)[1L]!!
        resolved[0].asClue {
            it.valueSource shouldBe ValueSource.MANUAL
            it.value shouldBe NumericValue(20.0)
            it.valPos shouldBe null
        }
    }

    @Test
    fun `it should copy positions for manual values with formalization`() {
        val formalized = MarketParameterValue(1L, ValueSource.FORMALIZATION, NumericValue(10.0), valPos = location)
        val manualOther = MarketParameterValue(1L, ValueSource.MANUAL, NumericValue(10.0))

        val values = listOf(formalized, manualOther).groupBy { it.parameterId }

        val resolved = resolver.resolveMarketValues(values)[1L]!!
        resolved[0].asClue {
            it.valueSource shouldBe ValueSource.MANUAL
            it.value shouldBe NumericValue(10.0)
            it.valPos shouldBe formalized.valPos
        }
    }
}
