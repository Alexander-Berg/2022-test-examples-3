package ru.yandex.market.contentmapping.controllers

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletWebRequest
import ru.yandex.market.contentmapping.controllers.helper.ControllerAccessHelper
import ru.yandex.market.contentmapping.dto.mapping.MarketParam
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue
import ru.yandex.market.contentmapping.dto.mapping.ParamMapping
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingRule
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingType
import ru.yandex.market.contentmapping.dto.mapping.ShopParam
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.repository.ShopRepository
import ru.yandex.market.contentmapping.services.mapping.ParamMappingService
import ru.yandex.market.contentmapping.services.rules.RulesLoadService
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass

class MigrationControllerTests : BaseAppTestClass() {

    val testShop1 = Shop(1L, "test shop1")
    val testShop2 = Shop(2L, "test shop2")

    @Autowired
    lateinit var shopRepository: ShopRepository

    @Autowired
    lateinit var rulesLoadService: RulesLoadService

    @Autowired
    lateinit var paramMappingService: ParamMappingService

    lateinit var migrationController: MigrationController

    @Before
    fun startup() {
        RequestContextHolder.setRequestAttributes(ServletWebRequest(ControllerTestUtils.mockHttpRequest()))
        val accessHelper = mock<ControllerAccessHelper>()
        shopRepository.insert(testShop1)
        shopRepository.insert(testShop2)
        migrationController = MigrationController(accessHelper, rulesLoadService, paramMappingService)
        rulesLoadService.invalidateCache()
    }

    @After
    fun cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun `should migrate correctly`() {
        val mapping = ParamMapping(
            id = 1,
            shopId = testShop1.id,
            categoryId = 1L,
            mappingType = ParamMappingType.MAPPING,
            isDeleted = false,
            isHypothesis = false,
            rank = 10,
            shopParams = listOf(
                ShopParam("color")
            ),
            marketParams = listOf(
                MarketParam(15L)
            )
        )
        val rule = ParamMappingRule(
            mapping.id,
            shopValues = mapOf("color" to "цвет"),
            isHypothesis = false,
            isDeleted = false,
            marketValues = mapOf(15L to setOf(MarketParamValue.StringValue("цвет")))
        )

        val mapping2 = ParamMapping(
            id = 1,
            shopId = testShop2.id,
            categoryId = 1L,
            mappingType = ParamMappingType.MAPPING,
            isDeleted = false,
            isHypothesis = false,
            rank = 10,
            shopParams = listOf(
                ShopParam("color")
            ),
            marketParams = listOf(
                MarketParam(15L)
            )
        )
        val rule2 = ParamMappingRule(
            mapping2.id,
            shopValues = mapOf("color" to "цвет"),
            isHypothesis = false,
            isDeleted = false,
            marketValues = mapOf(15L to setOf(MarketParamValue.StringValue("цвет")))
        )

        paramMappingService.mergeAndSaveNewParamMappingsAndRules(testShop1.id, mapOf(mapping to listOf(rule)))
        paramMappingService.mergeAndSaveNewParamMappingsAndRules(testShop2.id, mapOf(mapping2 to listOf(rule2)))

        val mapping3 = ParamMapping(
            id = 2,
            shopId = testShop1.id,
            categoryId = 1L,
            mappingType = ParamMappingType.MAPPING,
            isDeleted = false,
            isHypothesis = false,
            rank = 10,
            shopParams = listOf(
                ShopParam("weight")
            ),
            marketParams = listOf(
                MarketParam(16L)
            )
        )
        val rule3 = ParamMappingRule(
            mapping3.id,
            shopValues = mapOf("weight" to "вес"),
            isHypothesis = false,
            isDeleted = false,
            marketValues = mapOf(16L to setOf(MarketParamValue.NumericValue(1.56)))
        )

        paramMappingService.mergeAndSaveNewParamMappingsAndRules(testShop1.id, mapOf(mapping3 to listOf(rule3)))

        migrationController.migrateRules(testShop1.id, testShop2.id)

        val rulesLoaded1 = rulesLoadService.loadRules(testShop1.id)
        val rulesLoaded2 = rulesLoadService.loadRules(testShop2.id)

        Assertions.assertThat(rulesLoaded1).hasSize(rulesLoaded2.size)

        for (i in rulesLoaded1.indices) {
            val rules1 = rulesLoaded1[i]
            val rules2 = rulesLoaded2[i]
            Assertions.assertThat(rules1)
                .usingRecursiveComparison()
                .ignoringFields("id", "shopId", "paramMappingId", "paramMapping", "rules")
                .isEqualTo(rules2)

            Assertions.assertThat(rules1.paramMapping)
                .usingRecursiveComparison()
                .ignoringFields("id", "shopId", "key", "key\$delegate", "key\$delegate.initializer")
                .isEqualTo(rules2.paramMapping)

            for (j in rules1.rules.indices) {
                val rule1 = rules1.rules[j]
                val rule2 = rules2.rules[j]
                Assertions.assertThat(rule1)
                    .usingRecursiveComparison()
                    .ignoringFields("id", "paramMappingId", "key", "key\$delegate", "key\$delegate.initializer")
                    .isEqualTo(rule2)
            }
        }
    }
}
