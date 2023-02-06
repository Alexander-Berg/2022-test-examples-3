package ru.yandex.market.contentmapping.services

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue
import ru.yandex.market.contentmapping.dto.model.MarketParameterValue
import ru.yandex.market.contentmapping.dto.model.Picture
import ru.yandex.market.contentmapping.dto.model.ShopModel
import ru.yandex.market.contentmapping.dto.model.ValueSource
import ru.yandex.market.contentmapping.dto.rating.ShopModelRating
import ru.yandex.market.contentmapping.services.rules.MarketValuesResolver
import ru.yandex.market.contentmapping.utils.MboParameterConstants
import ru.yandex.market.contentmapping.utils.associateCombine

class ShopModelRequiredParamsRatingCalculatorServiceTest {

    @Test
    fun testWithEmptyValue() {
        val vendorParam1 = makeParam(
                MboParameterConstants.VENDOR_PARAM_ID,
                ValueSource.MANUAL,
                MarketParamValue.OptionValue(123, null))
        val vendorParam2 = makeParam(
                MboParameterConstants.VENDOR_PARAM_ID,
                ValueSource.MANUAL,
                MarketParamValue.EmptyValue()
        )

        val allValues = sequenceOf(vendorParam1, vendorParam2)
        val model = ShopModel(
                name = "Some name",
                shopSku = SHOP_SKU,
                description = "Some desc",
                pictures = listOf(Picture("123", ValueSource.FORMALIZATION, 0, true)),
                marketValues = allValues.associateCombine({ it.parameterId }, { listOf(it) }) { a, b -> a + b }
        )
        val calculatorService = ShopModelRequiredParamsRatingCalculatorService(MarketValuesResolver())

        val rating = calculatorService.calculateRequiredParamsRating(model)

        rating.vendorRating.asClue {
            it.paramId shouldBe MboParameterConstants.VENDOR_PARAM_ID
            it.paramName shouldBe "Торговая марка"
            it.state shouldBe ShopModelRating.ParameterState.NO_VALUE
        }
    }

    @Test
    fun testVendorValue() {
        val vendorParam = makeParam(
                MboParameterConstants.VENDOR_PARAM_ID,
                ValueSource.MANUAL,
                MarketParamValue.OptionValue(123, null))

        val allValues = sequenceOf(vendorParam)
        val model = ShopModel(
                name = "Some name",
                shopSku = SHOP_SKU,
                description = "Some desc",
                pictures = listOf(Picture("123", ValueSource.FORMALIZATION, 0, true)),
                marketValues = allValues.associateCombine({ it.parameterId }, { listOf(it) }) { a, b -> a + b }
        )
        val calculatorService = ShopModelRequiredParamsRatingCalculatorService(MarketValuesResolver())
        val rating = calculatorService.calculateRequiredParamsRating(model)

        rating.vendorRating.asClue {
            it.paramId shouldBe MboParameterConstants.VENDOR_PARAM_ID
            it.paramName shouldBe "Торговая марка"
            it.state shouldBe ShopModelRating.ParameterState.CHECKED
        }
    }

    private fun makeParam(paramId: Long, valueSource: ValueSource, value: MarketParamValue): MarketParameterValue {
        return MarketParameterValue(paramId, valueSource, value)
    }

    companion object {
        private const val SHOP_SKU = "shopSku"
    }
}
