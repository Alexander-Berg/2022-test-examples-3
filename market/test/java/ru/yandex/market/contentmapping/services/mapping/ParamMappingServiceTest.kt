package ru.yandex.market.contentmapping.services.mapping

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.contentmapping.controllers.exceptions.BadRequest
import ru.yandex.market.contentmapping.dto.mapping.MarketParam
import ru.yandex.market.contentmapping.dto.mapping.MarketParamConstants
import ru.yandex.market.contentmapping.dto.mapping.ParamMapping
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingRule
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingType
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingWithRulesDiff
import ru.yandex.market.contentmapping.dto.mapping.ShopParam
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.repository.ParamMappingRepository
import ru.yandex.market.contentmapping.services.rules.RulesLoadService
import ru.yandex.market.contentmapping.services.shop.ShopService
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import ru.yandex.market.mbo.taskqueue.TaskQueueRepository

open class ParamMappingServiceTest : BaseAppTestClass() {
    @Autowired
    lateinit var paramMappingService: ParamMappingService

    @Autowired
    lateinit var mappingRepository: ParamMappingRepository

    @Autowired
    lateinit var rulesLoadService: RulesLoadService

    @Autowired
    lateinit var taskRepository: TaskQueueRepository

    @Autowired
    lateinit var shopService: ShopService

    @Before
    fun init() {
        rulesLoadService.invalidateCache()
    }

    @Test
    fun testMappingWithDuplicateKeySaveWithoutException() {
        val (id) = shopService.insertAndAddOfferUpdateTask(Shop(1, "shop"))
        val paramMapping1 = ParamMapping(
                shopId = id,
                mappingType = ParamMappingType.MAPPING,
                shopParams = listOf(ShopParam("p1", null)),
                marketParams = listOf(MarketParam(MARKET_VALUE_KEY)),
                isHypothesis = true,
                isDeleted = true,
        )
        val paramMapping2 = paramMapping1.copy(isDeleted = false)
        val paramMapping3 = paramMapping1.copy(
                isDeleted = false, shopParams = paramMapping1.shopParams + ShopParam("p2"))
        val savedParamMapping1 = paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(paramMapping1)).paramMapping
        val savedParamMapping2 = paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(paramMapping2)).paramMapping
        val savedParamMapping3 = paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(paramMapping3)).paramMapping

        savedParamMapping1.id shouldBe savedParamMapping2.id
        savedParamMapping1.isDeleted shouldBe true
        savedParamMapping1.id shouldNotBe savedParamMapping3.id
        savedParamMapping2.isDeleted shouldBe false
        savedParamMapping3.isDeleted shouldBe false
    }

    @Test
    fun `simple saveParamMappingWithRules with split test`() {
        val (id) = shopService.insertAndAddOfferUpdateTask(Shop(1, "shop"))
        val paramMapping = ParamMapping(
                shopId = id,
                mappingType = ParamMappingType.MAPPING,
                shopParams = listOf(ShopParam("p1", ",")),
                marketParams = listOf(MarketParam(MARKET_VALUE_KEY)),
                isHypothesis = false,
                isDeleted = false,
        )


        paramMappingService.saveParamMappingWithRulesDiff(
                ParamMappingWithRulesDiff(
                        paramMapping,
                        listOf(ParamMappingRule(shopValues = mapOf("p1" to "val1_val2")))
                )
        )
    }

    @Test
    fun `shouldn't create trademark mapping hypothesis`() {
        val (id) = shopService.insertAndAddOfferUpdateTask(Shop(1, "shop"))
        val paramMapping = ParamMapping(
            shopId = id,
            mappingType = ParamMappingType.MAPPING,
            shopParams = listOf(ShopParam("p1", ",")),
            marketParams = listOf(MarketParamConstants.TRADEMARK),
            isHypothesis = true,
            isDeleted = false,
        )

        val rule1 = ParamMappingRule(
            shopValues = mapOf("a" to "b"),
            marketValues = emptyMap(),
        )

        val exception = assertThrows<BadRequest> {
            paramMappingService.saveParamMappingWithRulesDiff(
                ParamMappingWithRulesDiff(
                    paramMapping,
                    listOf(rule1)
                )
            )
        }

        assert(exception.message.equals("Создание гипотезного маппинга на торговый знак не разрешено"))

        paramMappingService.mergeAndSaveNewParamMappingsAndRules(id, mapOf(paramMapping to listOf(rule1)));
        val savedMappings = mappingRepository.findBaseByShopId(id)
        assert(!savedMappings.any { it.isHypothesis && it.marketParams.contains(MarketParamConstants.TRADEMARK) })
    }

    @Test
    fun `saveParamMappingWithRulesDiff with empty rules test`() {
        val (id) = shopService.insertAndAddOfferUpdateTask(Shop(1, "shop"))
        val mapping = ParamMapping(
                shopId = id,
                mappingType = ParamMappingType.MAPPING,
                shopParams = listOf(ShopParam("a", null)),
                marketParams = emptyList()
        )
        val saved = paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping))
        val loaded = rulesLoadService.loadRules(1).filter { it.paramMapping.key == saved.paramMapping.key }

        loaded.size shouldBe 1
        saved.paramMapping shouldBe loaded.first().paramMapping
    }

    @Test
    fun `saveParamMappingWithRulesDiff with some rules test`() {
        val (id) = shopService.insertAndAddOfferUpdateTask(Shop(1, "shop"))
        val mapping = ParamMapping(
                shopId = id,
                mappingType = ParamMappingType.MAPPING,
                shopParams = listOf(ShopParam("a", null)),
                marketParams = emptyList()
        )
        val rule1 = ParamMappingRule(
                shopValues = mapOf("a" to "b"),
                marketValues = emptyMap(),
        )
        val rule2 = ParamMappingRule(
                shopValues = mapOf("a" to "c"),
                marketValues = emptyMap(),
        )
        val saved = paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping, listOf(rule1, rule2)))
        val loaded = rulesLoadService.loadRules(1).filter { it.paramMapping.key == saved.paramMapping.key }

        loaded.size shouldBe 1
        saved.paramMapping shouldBe loaded.first().paramMapping
        saved.rules shouldContainExactlyInAnyOrder loaded.first().rules
    }

    @Test
    fun `saveParamMappingWithRulesDiff with some rules Idempotency test`() {
        val (id) = shopService.insertAndAddOfferUpdateTask(Shop(1, "shop"))
        val mapping = ParamMapping(
                shopId = id,
                mappingType = ParamMappingType.MAPPING,
                shopParams = listOf(ShopParam("a", null)),
                marketParams = emptyList()
        )
        val rule1 = ParamMappingRule(
                shopValues = mapOf("a" to "b"),
                marketValues = emptyMap(),
        )
        val rule2 = ParamMappingRule(
                shopValues = mapOf("a" to "c"),
                marketValues = emptyMap(),
        )
        paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping, listOf(rule1, rule2)))
        val saved = paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping, listOf(rule1, rule2)))
        val loaded = rulesLoadService.loadRules(1).filter { it.paramMapping.key == saved.paramMapping.key }

        loaded.size shouldBe 1
        saved.paramMapping shouldBe loaded.first().paramMapping
        saved.rules shouldContainExactlyInAnyOrder loaded.first().rules
    }

    @Test
    fun `saveParamMappingWithRulesDiff with some rules add test`() {
        val (id) = shopService.insertAndAddOfferUpdateTask(Shop(1, "shop"))
        val mapping = ParamMapping(
                shopId = id,
                mappingType = ParamMappingType.MAPPING,
                shopParams = listOf(ShopParam("a", null)),
                marketParams = emptyList()
        )
        val rule1 = ParamMappingRule(
                shopValues = mapOf("a" to "b"),
                marketValues = emptyMap(),
        )
        val rule2 = ParamMappingRule(
                shopValues = mapOf("a" to "c"),
                marketValues = emptyMap(),
        )
        val saved = paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping, listOf(rule1)))
        val loaded = rulesLoadService.loadRules(1).filter { it.paramMapping.key == saved.paramMapping.key }

        loaded.size shouldBe 1
        saved.paramMapping shouldBe loaded.first().paramMapping
        saved.rules.size shouldBe 1
        saved.rules shouldContainExactlyInAnyOrder loaded.first().rules

        val saved2 = paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping, listOf(rule2)))

        val updateRulesTask = taskRepository.findLatestByTaskType("RulesSyncTask")
        assert(updateRulesTask.isPresent)
        updateRulesTask.get().taskData shouldContain "\"shopId\":$id"
        rulesLoadService.invalidateCache()
        val loaded2 = rulesLoadService.loadRules(1).filter { it.paramMapping.key == saved.paramMapping.key }
        loaded2.size shouldBe 1
        saved2.paramMapping shouldBe loaded2.first().paramMapping
        saved2.rules.size shouldBe 2
        saved2.rules shouldContainExactlyInAnyOrder loaded2.first().rules
    }

    @Test
    fun `saveParamMappingWithRulesDiff with some rules remove test`() {
        val (id) = shopService.insertAndAddOfferUpdateTask(Shop(1, "shop"))
        val mapping = ParamMapping(
                shopId = id,
                mappingType = ParamMappingType.MAPPING,
                shopParams = listOf(ShopParam("a", null)),
                marketParams = emptyList()
        )
        val rule1 = ParamMappingRule(
                shopValues = mapOf("a" to "b"),
                marketValues = emptyMap(),
        )
        val rule2 = ParamMappingRule(
                shopValues = mapOf("a" to "c"),
                marketValues = emptyMap(),
        )
        val saved = paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping, listOf(rule1, rule2)))
        val loaded = rulesLoadService.loadRules(1).filter { it.paramMapping.key == saved.paramMapping.key }

        loaded.size shouldBe 1
        saved.paramMapping shouldBe loaded.first().paramMapping
        saved.rules shouldContainExactlyInAnyOrder loaded.first().rules

        val saved2 = paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping, rulesToRemove = saved.rules.map { it.id }))
        rulesLoadService.invalidateCache()
        val loaded2 = rulesLoadService.loadRules(1).filter { it.paramMapping.key == saved.paramMapping.key }
        val updateRulesTask = taskRepository.findLatestByTaskType("RulesSyncTask")

        loaded2.size shouldBe 1
        saved2.paramMapping shouldBe loaded2.first().paramMapping
        saved2.rules.size shouldBe 0
        assert(updateRulesTask.isPresent)
        updateRulesTask.get().taskData shouldContain "\"shopId\":$id"
        loaded2.first().rules.size shouldBe 0
    }

    @Test
    fun mergeNewParamMappingsAndRulesTestAddSame() {
        val (id) = shopService.insertAndAddOfferUpdateTask(Shop(1, "shop"))
        val mapping1 = ParamMapping(
                shopId = id,
                mappingType = ParamMappingType.MAPPING,
                shopParams = listOf(ShopParam("a", null)),
                marketParams = emptyList()
        )
        val rule1 = ParamMappingRule(
                shopValues = mapOf("a" to "b"),
                marketValues = emptyMap(),
        )
        val saved = paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping1, listOf(rule1)))
        val savedId = saved.paramMapping.id
        val rulesWithNewMapping = rulesLoadService.loadRules(1).filter { it.paramMapping.id == savedId }
        paramMappingService.mergeAndSaveNewParamMappingsAndRules(1, mapOf(mapping1 to listOf(rule1)))
        val rulesAfterMergeNewMapping = rulesLoadService.loadRules(1).filter { it.paramMapping.id == savedId }

        rulesWithNewMapping.size shouldBe 1
        rulesAfterMergeNewMapping.size shouldBe 1
    }

    @Test
    fun mergeNewParamMappingsAndRulesTestAddIfDeletedMappingExists() {
        val (id) = shopService.insertAndAddOfferUpdateTask(Shop(1, "shop"))
        val mapping1 = ParamMapping(
                shopId = id,
                mappingType = ParamMappingType.MAPPING,
                shopParams = listOf(ShopParam("c", null)),
                marketParams = emptyList(),
        )
        val rule1 = ParamMappingRule(
                shopValues = mapOf("c" to "d"),
                marketValues = emptyMap(),
        )
        val saved = paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping1, listOf(rule1)))
        val savedId = saved.paramMapping.id
        paramMappingService.deleteParamMapping(saved.paramMapping)
        val rulesWithNewMapping = rulesLoadService.loadRules(1).filter { it.paramMapping.id == savedId }
        paramMappingService.mergeAndSaveNewParamMappingsAndRules(1, mapOf(mapping1 to listOf(rule1)))
        val rulesAfterMergeNewMapping = rulesLoadService.loadRules(1).filter { it.paramMapping.id == savedId }

        rulesWithNewMapping.size shouldBe 0
        rulesAfterMergeNewMapping.size shouldBe 0
    }

    @Test
    fun mergeNewParamMappingsAndRulesTestAddIfDeletedRuleExists() {
        val (id) = shopService.insertAndAddOfferUpdateTask(Shop(1, "shop"))
        val mapping1 = ParamMapping(
                shopId = id,
                mappingType = ParamMappingType.MAPPING,
                shopParams = listOf(ShopParam("e", null)),
                marketParams = emptyList(),
        )
        val rule1 = ParamMappingRule(
                shopValues = mapOf("e" to "f"),
                marketValues = emptyMap(),
                isDeleted = true
        )
        val rule2 = rule1.copy(isDeleted = false)
        val saved = paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping1, listOf(rule1)))
        val savedId = saved.paramMapping.id
        val cachedRules = rulesLoadService.loadRules(1)
        cachedRules.size shouldBeGreaterThan 0
        val ruleWithNewMapping = cachedRules.first { it.paramMapping.id == savedId }
        paramMappingService.mergeAndSaveNewParamMappingsAndRules(1, mapOf(mapping1 to listOf(rule2)))
        val ruleAfterMergeNewMapping = rulesLoadService.loadRules(1).first { it.paramMapping.id == savedId }

        ruleWithNewMapping.rules.size shouldBe 0
        ruleAfterMergeNewMapping.rules.size shouldBe 0
    }

    @Test
    fun mergeNewParamMappingsAndRulesTestAddIfNoRuleExists() {
        val (id) = shopService.insertAndAddOfferUpdateTask(Shop(1, "shop"))
        val mapping1 = ParamMapping(
                shopId = id,
                mappingType = ParamMappingType.MAPPING,
                shopParams = listOf(ShopParam("g", null)),
                marketParams = emptyList(),
        )
        val rule1 = ParamMappingRule(
                shopValues = mapOf("g" to "h"),
                marketValues = emptyMap()
        )
        val saved = paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping1))
        val savedId = saved.paramMapping.id
        val rulesWithNewMapping = rulesLoadService.loadRules(1).first { it.paramMapping.id == savedId }
        paramMappingService.mergeAndSaveNewParamMappingsAndRules(1, mapOf(mapping1 to listOf(rule1)))

        val updateRulesTask = taskRepository.findLatestByTaskType("RulesSyncTask")


        rulesWithNewMapping.rules.size shouldBe 0
        assert(updateRulesTask.isPresent)
        updateRulesTask.get().taskData shouldContain "\"shopId\":$id"
    }

    companion object {
        private const val MARKET_VALUE_KEY = 123L
        private const val OPTION_ID = 456
    }
}
