package ru.yandex.market.contentmapping.services

import org.assertj.core.api.Assertions
import org.assertj.core.data.Offset
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import ru.yandex.market.contentmapping.dto.data.category.parameter.CategoryParameterInfo
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue.HypothesisValue
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue.OptionValue
import ru.yandex.market.contentmapping.dto.model.MarketParameterValue
import ru.yandex.market.contentmapping.dto.model.Picture
import ru.yandex.market.contentmapping.dto.model.ShopModel
import ru.yandex.market.contentmapping.dto.model.ValueSource
import ru.yandex.market.contentmapping.dto.rating.ParamRating
import ru.yandex.market.contentmapping.dto.rating.ShopModelRating
import ru.yandex.market.contentmapping.dto.rating.ShopModelRating.RatingKind
import ru.yandex.market.contentmapping.services.rules.MarketValuesResolver
import ru.yandex.market.contentmapping.testdata.CategoryParameterInfoTestInstance
import ru.yandex.market.contentmapping.utils.MboParameterConstants

class ShopModelRatingServiceTest {
    private var shopModelRatingService: ShopModelRatingService? = null
    private var paramId: Long = 0

    @Before
    fun setup() {
        shopModelRatingService = ShopModelRatingService(
                Mockito.mock(ComplexCategoryDataService::class.java),
                ShopModelRequiredParamsRatingCalculatorService(MarketValuesResolver())
        )
        paramId = 0
    }

    @Test
    fun testItWorks() {
        val vendorParam = makeParam(
                MboParameterConstants.VENDOR_PARAM_ID,
                ValueSource.MANUAL,
                OptionValue(123, null))
        val formalizedParam = makeParam(
                ValueSource.FORMALIZATION, MarketParamValue.BooleanValue(true, null))
        val hypothesisParam = makeParam(
                ValueSource.MANUAL, HypothesisValue("Some hypothesis"))
        val manualCheckedParam = makeParam(
                ValueSource.MANUAL, OptionValue(123, null))
        val otherParam = makeParam(
                ValueSource.MANUAL, OptionValue(123, null))
        val otherHypothesis = makeParam(
                ValueSource.MANUAL, HypothesisValue("other"))
        val nameParam = MarketParameterValue(
                MboParameterConstants.SKU_NAME_PARAM_ID, ValueSource.RULE, MarketParamValue.StringValue("name")
        )
        val descriptionParam = MarketParameterValue(
                MboParameterConstants.DESCRIPTION_PARAM_ID, ValueSource.RULE, MarketParamValue.StringValue("desc")
        )

        // NOTE: Linked to previous, should win
        val otherHypothesisChecked = makeParam(
                otherHypothesis.parameterId, ValueSource.MANUAL, MarketParamValue.StringValue("other"))
        val ruleParam = makeParam(
                ValueSource.RULE, MarketParamValue.BooleanValue(true, null))
        val allParams = listOf(
                vendorParam,
                formalizedParam,
                hypothesisParam,
                manualCheckedParam,
                otherParam,
                otherHypothesis,
                otherHypothesisChecked,
                ruleParam,
                nameParam,
                descriptionParam
        )
        val model = ShopModel(
                name = "Some name",
                shopSku = SHOP_SKU,
                description = "Some desc",
                pictures = listOf(Picture("123", ValueSource.FORMALIZATION, 0, true)),
                marketValues = allParams.groupBy { it.parameterId },
        )
        val rating = shopModelRatingService!!.calculateRating(model, ParamsForRatingCacheItem(listOf(
                categoryParameter(vendorParam) { it.copy(commonFilterIndex = 1) },
                categoryParameter(formalizedParam) { it.copy(commonFilterIndex = 1) },
                categoryParameter(hypothesisParam) { it.copy(isMandatoryForSignature = true) },
                categoryParameter(manualCheckedParam) { it.copy(isMandatoryForSignature = true) },
                categoryParameter(otherHypothesis),
                categoryParameter(ruleParam),  // Params not in model, to make it interesting
                categoryParameter(descriptionParam),
                categoryParameter(justSomeParam()) { it.copy(isMandatoryForSignature = true) },
                categoryParameter(justSomeParam()) { it.copy(isMandatoryForSignature = true) },
                categoryParameter(justSomeParam()) { it.copy(commonFilterIndex = 1) },
                categoryParameter(justSomeParam()),
                categoryParameter(justSomeParam()),
                categoryParameter(justSomeParam())
        )))
        // Total: 4 required, 4 important, 2 filter, 5 others

        // To checkout what's happenning
        // System.out.println(
        //    JsonUtils.commonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(rating));

        // Check some basic things
        Assertions.assertThat(rating.ratingKind).isEqualTo(RatingKind.GOOD)
        Assertions.assertThat(rating.currentRating).isEqualTo(47.5, OFFSET)
        Assertions.assertThat(rating.requiredParams.rating).isEqualTo(20.0, OFFSET)
        // 1.5 / 4 * 20 (one hypothesis)
        Assertions.assertThat(rating.importantParams.rating).isEqualTo(7.5, OFFSET)
        // should be zero - only one not checked
        Assertions.assertThat(rating.filterParams.rating).isEqualTo(0.0, OFFSET)
        Assertions.assertThat(rating.filterParams.params.stream()
                .filter { p: ParamRating -> p.state == ShopModelRating.ParameterState.NOT_CHECKED }.count()).isEqualTo(1)

        // 2 filled out of 5 (3 important), one with hypothesis which is ok here, so 2/3 * 30
        Assertions.assertThat(rating.otherParams.rating).isEqualTo(20.0, OFFSET)
        // Description should not be present in other params
        Assertions.assertThat(rating.otherParams.params
                .any { it.paramId == MboParameterConstants.DESCRIPTION_PARAM_ID }).isFalse
    }

    @Test
    fun testGetStateForValue() {
        val marketParameterValue = MarketParameterValue(
                parameterId = 0,
                valueSource = ValueSource.FORMALIZATION,
                value = MarketParamValue.StringValue("")
        )
        Assertions.assertThat(ShopModelRatingService.getStateForValue(marketParameterValue))
                .isEqualTo(ShopModelRating.ParameterState.NOT_CHECKED)
        Assertions.assertThat(ShopModelRatingService.getStateForValue(marketParameterValue.copy(
                valueSource = ValueSource.RULE,
                value = OptionValue(0, "")
        ))).isEqualTo(ShopModelRating.ParameterState.CHECKED)
        Assertions.assertThat(ShopModelRatingService.getStateForValue(marketParameterValue.copy(
                valueSource = ValueSource.RULE,
                value = HypothesisValue("")
        ))).isEqualTo(ShopModelRating.ParameterState.CHECKED_HYPOTHESIS)
    }

    private fun justSomeParam(): MarketParameterValue {
        return makeParam(ValueSource.RULE, MarketParamValue.BooleanValue(true, null))
    }

    private fun categoryParameter(
            value: MarketParameterValue,
            setup: (CategoryParameterInfo) -> CategoryParameterInfo = { it },
    ): CategoryParameterInfo {
        val parameterInfo = CategoryParameterInfoTestInstance().copy(
                parameterId = value.parameterId,
                name = "param #${value.parameterId}",
                commonFilterIndex = -1,
        )
        return setup(parameterInfo)
    }

    private fun makeParam(valueSource: ValueSource, value: MarketParamValue): MarketParameterValue {
        return makeParam(++paramId, valueSource, value)
    }

    private fun makeParam(paramId: Long, valueSource: ValueSource, value: MarketParamValue): MarketParameterValue {
        return MarketParameterValue(paramId, valueSource, value)
    }

    companion object {
        private val OFFSET = Offset.offset(0.01)
        private const val SHOP_SKU = "shopSku"
    }
}
