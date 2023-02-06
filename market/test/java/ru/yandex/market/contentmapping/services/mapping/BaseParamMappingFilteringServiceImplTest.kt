package ru.yandex.market.contentmapping.services.mapping

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.contentmapping.dto.mapping.MarketParam
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue
import ru.yandex.market.contentmapping.dto.mapping.ParamMapping
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingRule
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingWithRules
import ru.yandex.market.contentmapping.dto.mapping.ShopParam
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.repository.ParamMappingRepository
import ru.yandex.market.contentmapping.repository.ShopRepository
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass

class BaseParamMappingFilteringServiceImplTest : BaseAppTestClass() {
    @Autowired
    private lateinit var shopRepository: ShopRepository

    @Autowired
    private lateinit var mappingRepository: ParamMappingRepository

    @Autowired
    private lateinit var baseParamMappingFilteringServiceImpl: BaseParamMappingFilteringServiceImpl

    @Test
    fun `Filters string and matching option rules`() {
        val (shopId) = shopRepository.insert(Shop(1000L, "Shop1"))

        val mapping1 = ParamMapping(
                shopId = shopId,
                id = 770,
                shopParams = listOf(ShopParam("990")),
                marketParams = listOf(MarketParam(990))
        )
        val mapping2 = ParamMapping(
                shopId = shopId,
                id = 771,
                shopParams = listOf(ShopParam("991")),
                marketParams = listOf(MarketParam(991))
        )
        mappingRepository.insertBatch(mapping1.copy(rank = -100), mapping2.copy(rank = -100))

        val matchingStringRule = ParamMappingRule(
                id = 780,
                paramMappingId = 770,
                shopValues = mapOf("990" to "shop value"),
                marketValues = mapOf(990L to setOf(MarketParamValue.StringValue("shop value")))
        )
        val nonMatchingStringRule = ParamMappingRule(
                id = 781,
                paramMappingId = 770,
                shopValues = mapOf("990" to "str"),
                marketValues = mapOf(990L to setOf(MarketParamValue.StringValue("non-matching")))
        )
        val boolRule = ParamMappingRule(
                id = 782,
                paramMappingId = 770,
                shopValues = mapOf("990" to "Да"),
                marketValues = mapOf(990L to setOf(MarketParamValue.BooleanValue(true)))
        )
        val hypothesisRule = ParamMappingRule(
                id = 783,
                paramMappingId = 770,
                shopValues = mapOf("990" to "hypothesis"),
                marketValues = mapOf(990L to setOf(MarketParamValue.HypothesisValue("hypothesis")))
        )
        val optionRule = ParamMappingRule(
                id = 784,
                paramMappingId = 771,
                shopValues = mapOf("991" to "  OptiOn    "),
                marketValues = mapOf(991L to setOf(MarketParamValue.OptionValue(1, "  OpTiOn ")))
        )

        val result = baseParamMappingFilteringServiceImpl.filter(shopId, mapOf(
                mapping1 to listOf(matchingStringRule, nonMatchingStringRule, boolRule, hypothesisRule),
                mapping2 to listOf(optionRule)
        ))
        result shouldHaveSize 1
        val mappingResult = result.entries.first().let { ParamMappingWithRules(it.key, it.value) }
        mappingResult.rules shouldHaveSize 3
        mappingResult.rules shouldContainExactly listOf(nonMatchingStringRule, boolRule, hypothesisRule)
    }
}
