package ru.yandex.market.contentmapping.services.rules.v2.stratategy

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.junit.Test
import ru.yandex.market.contentmapping.dto.data.category.parameter.CategoryParameterInfo
import ru.yandex.market.contentmapping.dto.mapping.MarketParam
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue.BooleanValue
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue.HypothesisValue
import ru.yandex.market.contentmapping.dto.mapping.ParamMapping
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingRule
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingType
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingWithRules
import ru.yandex.market.contentmapping.dto.mapping.ShopParam
import ru.yandex.market.contentmapping.utils.MboParameterConstants
import ru.yandex.market.mbo.export.MboParameters

class MappingMappingStrategyTest {

    @Test
    fun applyTest() {
        val strategy = MappingMappingStrategy()
        val mapping = paramMappingWithRulesForVendor()
        val hypothesys = mapping.rules[0].marketValues[MboParameterConstants.VENDOR_PARAM_ID]?.first() as HypothesisValue
        val result = strategy.apply(
                mapping = mapping,
                shopValues = mapOf("#Торговая марка (ассортимент)" to "TopGiper"),
                categoryParameters = categoryParameters()
        )?.toList() ?: emptyList()
        result shouldHaveSize 1
        val values = result[0].second.toList()
        values shouldHaveSize 1
        val value = values[0].value
        value should { it is HypothesisValue }
        value shouldBe hypothesys
        (value as HypothesisValue).hypothesis shouldBe hypothesys.hypothesis
    }

    @Test
    fun applyBooleanValueTest() {
        val strategy = MappingMappingStrategy()
        val mapping = paramMappingWithRulesForBoolean()
        val bolleanValue = mapping.rules[0].marketValues[MboParameterConstants.VENDOR_PARAM_ID]?.first() as BooleanValue
        val result = strategy.apply(
                mapping = mapping,
                shopValues = mapOf(BOOL_SHOP_PARAM_NAME to BOOL_SHOP_PARAM_YES),
                categoryParameters = categoryParameters()
        )?.toList() ?: emptyList()
        result shouldHaveSize 1
        val values = result[0].second.toList()
        values shouldHaveSize 1
        val value = values[0].value
        value should { it is HypothesisValue }
        value shouldBe bolleanValue
        (value as BooleanValue).booleanValue shouldBe bolleanValue.booleanValue
    }

    private fun categoryParameters() = mapOf(
            MboParameterConstants.VENDOR_PARAM_ID to CategoryParameterInfo(
                    parameterId = MboParameterConstants.VENDOR_PARAM_ID,
                    xslName = "vendor",
                    name = "Торговая марка",
                    unitName = null,
                    valueType = MboParameters.ValueType.ENUM,
                    isImportant = false,
                    isMultivalue = false,
                    isService = false,
                    isRequiredForModelCreation = true,
                    isMandatoryForSignature = true,
                    commonFilterIndex = 0,
                    commentForOperator = null,
                    commentForPartner = null,
                    options = Int2ObjectOpenHashMap(),
                    minValue = null,
                    maxValue = null,
            ),
            BOOL_PARAM_ID to CategoryParameterInfo(
                    parameterId = BOOL_PARAM_ID,
                    xslName = "testBool",
                    name = "бул",
                    unitName = null,
                    valueType = MboParameters.ValueType.BOOLEAN,
                    isImportant = false,
                    isMultivalue = false,
                    isService = false,
                    isRequiredForModelCreation = true,
                    isMandatoryForSignature = true,
                    commonFilterIndex = 0,
                    commentForOperator = null,
                    commentForPartner = null,
                    options = Int2ObjectOpenHashMap(),
                    minValue = null,
                    maxValue = null,
            )
    )

    private fun paramMappingWithRulesForVendor() = ParamMappingWithRules(
            ParamMapping(
                    id = 1,
                    shopId = 1,
                    ParamMappingType.MAPPING,
                    shopParams = listOf(
                            ShopParam("#Торговая марка (ассортимент)", null)
                    ),
                    marketParams = listOf(
                            MarketParam(MboParameterConstants.VENDOR_PARAM_ID)
                    ),
                    isHypothesis = false,
                    isDeleted = false,
                    rank = 0
            ),
            listOf(
                    ParamMappingRule(
                            id = 1,
                            paramMappingId = 1,
                            shopValues = mapOf("#Торговая марка (ассортимент)" to "topgiper"),
                            marketValues = mapOf(
                                    MboParameterConstants.VENDOR_PARAM_ID to setOf(HypothesisValue("TopGiper"))
                            ),
                            isHypothesis = false,
                            isDeleted = false
                    )
            )
    )

    private fun paramMappingWithRulesForBoolean() = ParamMappingWithRules(
            ParamMapping(
                    id = 1,
                    shopId = 1,
                    ParamMappingType.MAPPING,
                    shopParams = listOf(
                            ShopParam(BOOL_SHOP_PARAM_NAME, null)
                    ),
                    marketParams = listOf(
                            MarketParam(BOOL_PARAM_ID)
                    ),
                    isHypothesis = false,
                    isDeleted = false,
                    rank = 0
            ),
            listOf(
                    ParamMappingRule(
                            id = 1,
                            paramMappingId = 1,
                            shopValues = mapOf(BOOL_SHOP_PARAM_NAME to BOOL_SHOP_PARAM_YES),
                            marketValues = mapOf(
                                    MboParameterConstants.VENDOR_PARAM_ID to setOf(BooleanValue(true))
                            ),
                            isHypothesis = false,
                            isDeleted = false
                    )
            )
    )

    companion object {
        const val BOOL_PARAM_ID = 1L
        const val BOOL_SHOP_PARAM_NAME = "бул"
        const val BOOL_SHOP_PARAM_YES = "ага"
    }
}
