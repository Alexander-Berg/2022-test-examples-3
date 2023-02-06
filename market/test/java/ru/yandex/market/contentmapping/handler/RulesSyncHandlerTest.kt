package ru.yandex.market.contentmapping.handler

import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.contentmapping.dto.mapping.ParamMapping
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingRule
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingType
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingWithRulesDiff
import ru.yandex.market.contentmapping.dto.mapping.ShopParam
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.repository.ParamMappingRepository
import ru.yandex.market.contentmapping.services.VersionLockService
import ru.yandex.market.contentmapping.services.mapping.ParamMappingService
import ru.yandex.market.contentmapping.services.rules.RulesLoadService
import ru.yandex.market.contentmapping.services.rules.RulesSyncHandler
import ru.yandex.market.contentmapping.services.rules.RulesSyncTask
import ru.yandex.market.contentmapping.services.shop.ShopService
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import ru.yandex.market.contentmapping.utils.JsonUtils
import ru.yandex.market.mbo.taskqueue.TaskQueueRepository
import java.io.IOException

open class RulesSyncHandlerTest : BaseAppTestClass() {
    @Autowired
    lateinit var paramMappingService: ParamMappingService

    @Autowired
    lateinit var mappingRepository: ParamMappingRepository

    @Autowired
    lateinit var rulesLoadService: RulesLoadService

    @Autowired
    lateinit var taskRepository: TaskQueueRepository

    @Autowired
    lateinit var mappingVersionLockService: VersionLockService

    @Autowired
    lateinit var shopService: ShopService

    @Autowired
    lateinit var taskQueueRepository: TaskQueueRepository

    @Autowired
    lateinit var rulesSyncHandler: RulesSyncHandler

    val mapper = JsonUtils.commonObjectMapper()

    @Test
    open fun shouldCreateTaskOnMappingsUpdate() {
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
            isDeleted = false
        )

        paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping1, listOf(rule1)))
        val task = taskQueueRepository.findNextTask(false, listOf(SYNC_TASK))
        assert(task.isPresent)
        val taskData = readTask(task.get().taskData, task.get().taskDataVersion)
        taskData shouldNotBe null
        taskData!!.shopId shouldBe id
        taskData.version shouldBe mappingVersionLockService.getVersion("1")
    }

    @Before
    open fun setup() {
        rulesLoadService.invalidateCache()
    }

    @Test
    open fun shouldUpdateAllAtOnce() {
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
            isDeleted = false
        )
        val mapping2 = ParamMapping(
            shopId = id,
            mappingType = ParamMappingType.MAPPING,
            shopParams = listOf(ShopParam("e", null)),
            marketParams = emptyList(),
        )
        val rule2 = ParamMappingRule(
            shopValues = mapOf("a" to "b"),
            marketValues = emptyMap(),
            isDeleted = false
        )
        paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping1, listOf(rule1)))
        val version1 = mappingVersionLockService.getVersion("1")
        paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping2, listOf(rule2)))
        val version2 = mappingVersionLockService.getVersion("1")
        version1 shouldNotBe version2
        val task = taskQueueRepository.findNextTask(false, listOf(SYNC_TASK)).orElseThrow()
        val taskData = readTask(task.taskData, task.taskDataVersion)
        val allTasks = taskQueueRepository.findAll(true)
        taskData!!.version shouldBe version1
        allTasks.size shouldBeGreaterThanOrEqual 2
        rulesSyncHandler.handle(taskData, task)
        val cachedVersion = rulesLoadService.getCachedVersion(1L)
        cachedVersion shouldBe version2
    }

    @Test
    open fun shouldUpdateCacheData() {
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
            isDeleted = false
        )
        val cachedBefore = rulesLoadService.loadRules(1L)
        cachedBefore.size shouldBe 5 // insertAndAddOfferUpdateTask creates initial mappings
        val saved =
            paramMappingService.saveParamMappingWithRulesDiff(ParamMappingWithRulesDiff(mapping1, listOf(rule1)))
        val version1 = mappingVersionLockService.getVersion("1")
        val task = taskQueueRepository.findNextTask(false, listOf(SYNC_TASK)).orElseThrow()
        val taskData = readTask(task.taskData, task.taskDataVersion)
        taskData!!.version shouldBe version1
        rulesSyncHandler.handle(taskData, task)
        val cachedVersion = rulesLoadService.getCachedVersion(1L)
        cachedVersion shouldBe version1
        val cachedData = rulesLoadService.loadRules(1L)
        assert(cachedData != cachedBefore)
        cachedData.size shouldBe 6
        cachedData[5].rules.size shouldBe 1
        cachedData[5].paramMapping.id shouldBe saved.paramMapping.id
        cachedData[5].rules[0].id shouldBe saved.rules[0].id
    }

    @Throws(IOException::class)
    open fun readTask(taskData: String?, schemaVersion: Int): RulesSyncTask? {
        return mapper.readValue(taskData, RulesSyncTask::class.java)
    }

    companion object {
        private const val SYNC_TASK = "RulesSyncTask"
    }
}
