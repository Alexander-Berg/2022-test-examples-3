package ru.yandex.market.markup3.yang.services

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.market.markup3.core.TolokaExternalPoolId
import ru.yandex.market.markup3.core.TolokaSource
import ru.yandex.market.markup3.core.TolokaTask
import ru.yandex.market.markup3.core.exceptions.NeedToRecoverException
import ru.yandex.market.markup3.mboc.category.info.CategoryInfo
import ru.yandex.market.markup3.mboc.category.info.repository.CategoryInfoRepository
import ru.yandex.market.markup3.mboc.category.info.service.CategoryInfoService
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.TolokaMappingModerationHandler
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.dto.TolokaInstruction
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.dto.TolokaMappingModerationOffer
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.dto.TolokaMappingModerationOfferInfo
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.dto.TolokaMappingModerationSkuInfo
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.utils.CommonObjectMapper
import ru.yandex.market.markup3.yang.TolokaClientMock
import ru.yandex.market.markup3.yang.dto.TolokaRecoverStatus.NEW
import ru.yandex.market.markup3.yang.repositories.TolokaRecoverQueueRepository
import ru.yandex.toloka.client.v1.pool.Pool
import ru.yandex.toloka.client.v1.pool.dynamicoverlap.BasicDynamicOverlapConfig
import ru.yandex.toloka.client.v1.task.KnownSolution
import java.math.BigDecimal
import java.util.Date

class TolokaTasksServiceTest : CommonTaskTest() {
    @Autowired
    lateinit var tolokaRecoverQueueRepository: TolokaRecoverQueueRepository

    @Autowired
    lateinit var tolokaClientMock: TolokaClientMock

    @Autowired
    lateinit var tolokaTasksService: TolokaTasksService

    @Autowired
    lateinit var categoryInfoRepository: CategoryInfoRepository

    @Autowired
    lateinit var categoryInfoService: CategoryInfoService

    @Autowired
    lateinit var tolokaMappingModerationHandler: TolokaMappingModerationHandler

    @Test
    fun createTasks() {
        tolokaClientMock.allTasks.clear()
        val testTaskId = createTestTask(createTestTaskGroup().id)
        val basePool = createBasePool()

        val pool = tolokaTasksService.getOrCreatePool(
            GetOrCreatePoolRequest(
                poolGroupId = "1",
                basePoolId = basePool.id,
                source = TolokaSource.TOLOKA,
                poolCustomizer = null,
                forbidDownload = false,
            )
        )
        val tasksPerBatch = 10
        val honeypotsPerBatch = 1
        val hintsPerBatch = 1
        val request = CreateTasksRequest(
            tolokaPoolInfo = pool,
            taskGroup = createTestTaskGroup(),
            taskId = testTaskId,
            tasks = create100Tasks(),
            honeypots = create100Honeypots(pool.externalPoolId!!),
            hints = create100Hints(pool.externalPoolId!!),
            tasksPerBatch = tasksPerBatch,
            honeypotsPerBatch = honeypotsPerBatch,
            hintsPerBatch = hintsPerBatch,
            openPool = true,
        )
        val createTasks = tolokaTasksService.createTasks(request)
        createTasks shouldHaveSize 100

        tolokaClientMock.allTasks.values shouldHaveSize request.tasks.size + request.honeypots.size + request.hints.size
    }

    @Test
    fun createSortedTasksEnoughForSuite() {
        val categoryList = listOf(
            CategoryInfo(1, -1, "Все товары", "", false, true, true, "", "", false),
            CategoryInfo(2, 1, "Обувь", "", false, true, true, "", "", false),
            CategoryInfo(3, 1, "Еда", "", false, true, true, "", "", false),
            CategoryInfo(4, 2, "Валенки", "", false, true, true, "", "", true),
            CategoryInfo(5, 2, "Тапки", "", false, true, true, "", "", true),
            CategoryInfo(6, 3, "Огурцы", "", false, true, true, "", "", true),
            CategoryInfo(7, 3, "Помидоры", "", false, true, true, "", "", true),
        )
        categoryInfoRepository.insertBatch(categoryList)
        categoryInfoService.invalidateCache()
        categoryInfoService.getAllCategoryInfos()

        tolokaClientMock.allTasks.clear()
        val testTaskId = createTestTask(createTestTaskGroup().id)
        val basePool = createBasePool()

        val pool = tolokaTasksService.getOrCreatePool(
            GetOrCreatePoolRequest(
                poolGroupId = "1",
                basePoolId = basePool.id,
                source = TolokaSource.TOLOKA,
                poolCustomizer = null,
                forbidDownload = false,
            )
        )

        val leafCategories = categoryList.filter { it.isLeaf }

        val tasksPerCategory = 8
        val honeyPotsByCategory = 1
        val hintsPerCategory = 1

        val allRealTaskCount = tasksPerCategory * leafCategories.size
        val allHoneyPotsCount = honeyPotsByCategory * leafCategories.size
        val allHintsCount = honeyPotsByCategory * leafCategories.size

        val generatedRealTasks = generateRealTasks(leafCategories, tasksPerCategory)
            .shuffled()
        val generatedHoneyPots = generateHoneyPots(pool.externalPoolId!!, leafCategories, honeyPotsByCategory)
            .shuffled()
        val generatedHints = generateHints(pool.externalPoolId!!, leafCategories, hintsPerCategory)
            .shuffled()

        val request = CreateTasksRequest(
            tolokaPoolInfo = pool,
            taskGroup = createTestTaskGroup(),
            taskId = testTaskId,
            tasks = generatedRealTasks,
            honeypots = generatedHoneyPots,
            hints = generatedHints,
            tasksPerBatch = tasksPerCategory + honeyPotsByCategory + hintsPerCategory,
            honeypotsPerBatch = honeyPotsByCategory,
            hintsPerBatch = hintsPerCategory,
            openPool = true,
            tasksCustomizer = tolokaMappingModerationHandler.sortAndCategorizeTaskCustomizer
        )
        val createTasks = tolokaTasksService.createTasks(request)
        createTasks shouldHaveSize allRealTaskCount
        val values = tolokaClientMock.allTasks.values
        values shouldHaveSize allRealTaskCount + allHoneyPotsCount * 2 + allHintsCount * 2

        // Exclude additional shuffled hints and honeypots for 3th overlap
        values.take(allRealTaskCount + allHoneyPotsCount + allHintsCount)
            .chunked(tasksPerCategory + honeyPotsByCategory + hintsPerCategory).forEach { taskSuite ->
                val taskSuiteCategoryId = taskSuite[0].inputValues["category_id"]

                taskSuite.forEach { it.inputValues["category_id"] shouldBe taskSuiteCategoryId }

                // honeypot and hint should have knownSolutions
                val subList = taskSuite.subList(tasksPerCategory, taskSuite.size)
                subList.forEach {
                    it.knownSolutions shouldNot beNull()
                }
                // hint should have message_on_unknown_solution
                subList.last().messageOnUnknownSolution.shouldNotBeNull()
            }
    }

    @Test
    fun createSortedTasksShouldChooseClosestCategory() {
        val categoryList = listOf(
            CategoryInfo(1, -1, "Все товары", "", false, true, true, "", "", false),
            CategoryInfo(2, 1, "Обувь", "", false, true, true, "", "", false),
            CategoryInfo(3, 1, "Еда", "", false, true, true, "", "", false),
            CategoryInfo(4, 2, "Валенки", "", false, true, true, "", "", true),
            CategoryInfo(5, 2, "Тапки", "", false, true, true, "", "", true),
            CategoryInfo(6, 3, "Огурцы", "", false, true, true, "", "", true),
            CategoryInfo(7, 3, "Помидоры", "", false, true, true, "", "", true),
        )
        categoryInfoRepository.insertBatch(categoryList)
        categoryInfoService.invalidateCache()
        categoryInfoService.getAllCategoryInfos()

        tolokaClientMock.resetAllTasks()
        val testTaskId = createTestTask(createTestTaskGroup().id)
        val basePool = createBasePool()

        val pool = tolokaTasksService.getOrCreatePool(
            GetOrCreatePoolRequest(
                poolGroupId = "1",
                basePoolId = basePool.id,
                source = TolokaSource.TOLOKA,
                poolCustomizer = null,
                forbidDownload = false,
            )
        )

        val leafCategories = categoryList.filter { it.isLeaf }

        val tasksPerCategory = 8
        val honeyPotsByCategory = 1
        val hintsPerCategory = 1

        val allRealTaskCount = tasksPerCategory * leafCategories.size
        val allHoneyPotsCount = honeyPotsByCategory * leafCategories.size
        val allHintsCount = honeyPotsByCategory * leafCategories.size

        val generatedRealTasks = generateRealTasks(leafCategories, tasksPerCategory)
            .shuffled()
        val generatedHoneyPots = generateHoneyPots(pool.externalPoolId!!, leafCategories, honeyPotsByCategory)
            .shuffled()
        val generatedHints = generateHints(pool.externalPoolId!!, leafCategories, hintsPerCategory)
            .shuffled()

        val request = CreateTasksRequest(
            tolokaPoolInfo = pool,
            taskGroup = createTestTaskGroup(),
            taskId = testTaskId,
            tasks = generatedRealTasks,
            honeypots = generatedHoneyPots,
            hints = generatedHints,
            tasksPerBatch = tasksPerCategory + honeyPotsByCategory + hintsPerCategory,
            honeypotsPerBatch = honeyPotsByCategory,
            hintsPerBatch = hintsPerCategory,
            openPool = true,
            tasksCustomizer = tolokaMappingModerationHandler.sortAndCategorizeTaskCustomizer
        )
        val createTasks = tolokaTasksService.createTasks(request)
        createTasks shouldHaveSize allRealTaskCount
        val values = tolokaClientMock.allTasks.values
        values shouldHaveSize allRealTaskCount + allHoneyPotsCount * 2 + allHintsCount * 2

        val expectedClosestCategories = mapOf("6" to "7", "7" to "6", "4" to "5", "5" to "4")
        values.take(allRealTaskCount + allHoneyPotsCount + allHintsCount)
            .chunked(tasksPerCategory + honeyPotsByCategory + hintsPerCategory).forEach { taskSuite ->
                val taskSuiteCategoryId = taskSuite[0].inputValues["category_id"].toString()

                taskSuite.forEach {
                    it.inputValues["category_id"].toString() shouldBeIn listOf(
                        taskSuiteCategoryId,
                        expectedClosestCategories[taskSuiteCategoryId]
                    )
                }

                // check honeypot contains
                val subList = taskSuite.subList(tasksPerCategory, taskSuite.size)
                subList.forEach {
                    it.knownSolutions shouldNot beNull()
                }
                // check hint contains
                subList.last().messageOnUnknownSolution.shouldNotBeNull()
            }
    }

    @Test
    fun restoreTasks() {
        tolokaClientMock.allTasks.clear()
        val testTaskId = createTestTask(createTestTaskGroup().id)
        val basePool = createBasePool()

        val pool = tolokaTasksService.getOrCreatePool(
            GetOrCreatePoolRequest(
                poolGroupId = "1",
                basePoolId = basePool.id,
                source = TolokaSource.TOLOKA,
                poolCustomizer = null,
                forbidDownload = false,
            )
        )
        val tasksPerBatch = 10
        val honeypotsPerBatch = 2
        val tasks = create100Tasks()
        val taskKeys = tasks.map { it.taskKey }
        val request = CreateTasksRequest(
            tolokaPoolInfo = pool,
            taskGroup = createTestTaskGroup(),
            taskId = testTaskId,
            tasks = tasks,
            honeypots = create100Honeypots(pool.externalPoolId!!),
            hints = listOf<TolokaTask>(),
            tasksPerBatch = tasksPerBatch,
            honeypotsPerBatch = honeypotsPerBatch,
            hintsPerBatch = 0,
            openPool = true,
        )
        tolokaTasksService.createTasks(request)
        try {
            tolokaTasksService.createTasks(request)
        } catch (e: NeedToRecoverException) {
            e.taskKeys shouldContainExactlyInAnyOrder taskKeys
            e.taskId shouldBe testTaskId
        }
        val recoverQueue = tolokaRecoverQueueRepository.findAll()
        recoverQueue shouldHaveSize 100
        recoverQueue.map { it.taskKey } shouldContainExactlyInAnyOrder taskKeys
        recoverQueue.forEach {
            it.taskId shouldBe testTaskId
            it.poolId shouldBe pool.id
            it.tolokaRecoverStatus shouldBe NEW
        }
    }

    private fun create100Tasks(): List<TaskInTaskSuite> {
        val result = mutableListOf<TaskInTaskSuite>()
        for (i in 1..100) {
            result.add(
                TaskInTaskSuite(
                    taskKey = i.toString(),
                    input = TolokaMappingModerationOffer(
                        i.toLong(),
                        "TestCategoryName_$i",
                        i.toLong(),
                        i.toLong(),
                        i.toLong(),
                        TolokaMappingModerationOfferInfo(
                            emptyList(),
                            "",
                            "",
                            "",
                            emptyList(),
                            HashMap<String, Any>(),
                            ""
                        ),
                        TolokaMappingModerationSkuInfo(
                            "",
                            0,
                            emptyList(),
                            "",
                            emptyList(),
                            emptyList(),
                            emptyList(),
                            "",
                            ""
                        ),
                        TolokaInstruction("", "", "", "")
                    )
                )
            )
        }
        return result
    }

    private fun create100Honeypots(poolId: TolokaExternalPoolId): List<TolokaTask> {
        val result = mutableListOf<TolokaTask>()
        for (i in 1..100) {
            result.add(TolokaTask(poolId, mapOf("category_id" to i)))
        }
        return result
    }

    private fun create100Hints(poolId: TolokaExternalPoolId): List<TolokaTask> {
        val result = mutableListOf<TolokaTask>()
        for (i in 1..100) {
            result.add(TolokaTask(poolId, mapOf("category_id" to i), listOf(), "TestHint$i"))
        }
        return result
    }

    private fun createBasePool(): Pool {
        val pool = Pool(
            "prj",
            "pr_name",
            true,
            Date(),
            BigDecimal.ONE,
            1,
            true,
            null
        )
        val basicDynamicOverlapConfig = BasicDynamicOverlapConfig()
        basicDynamicOverlapConfig.answerWeightSkillId = "1"
        basicDynamicOverlapConfig.fields = listOf()
        ReflectionTestUtils.setField(pool, "dynamicOverlapConfig", basicDynamicOverlapConfig)
        return tolokaClientMock.createPool(pool).result
    }

    private fun generateHoneyPots(
        poolId: TolokaExternalPoolId,
        categoryList: List<CategoryInfo>,
        honeyPotsByCategoryCount: Int
    ): MutableList<TolokaTask> {
        val honeypots = mutableListOf<TolokaTask>()
        for (category in categoryList) {
            for (i in 1..honeyPotsByCategoryCount) {
                honeypots.add(
                    TolokaTask(
                        poolId,
                        anyToInputValueMap(
                            TolokaMappingModerationOffer(
                                category.hid,
                                category.name + i,
                                i.toLong(),
                                i.toLong(),
                                i.toLong(),
                                TolokaMappingModerationOfferInfo(
                                    emptyList(),
                                    "",
                                    "",
                                    "",
                                    emptyList(),
                                    HashMap<String, Any>(),
                                    "testDestination$i"
                                ),
                                TolokaMappingModerationSkuInfo(
                                    "Test HoneyPot ${category.name + i}",
                                    i.toLong(),
                                    emptyList(),
                                    "",
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    "",
                                    ""
                                ),
                                TolokaInstruction("", "", "", "")
                            )
                        ),
                        listOf(
                            KnownSolution(
                                mapOf("result" to "ACCEPTED"),
                                1.0
                            ),
                            KnownSolution(
                                mapOf("result" to "REJECTED"),
                                0.25
                            ),
                        )
                    )
                )
            }
        }
        return honeypots
    }

    private fun generateHints(
        poolId: TolokaExternalPoolId,
        categoryList: List<CategoryInfo>,
        hintsByCategoryCount: Int
    ): MutableList<TolokaTask> {
        val honeypots = mutableListOf<TolokaTask>()
        for (category in categoryList) {
            for (i in 1..hintsByCategoryCount) {
                honeypots.add(
                    TolokaTask(
                        poolId,
                        anyToInputValueMap(
                            TolokaMappingModerationOffer(
                                category.hid,
                                category.name + i,
                                i.toLong(),
                                i.toLong(),
                                i.toLong(),
                                TolokaMappingModerationOfferInfo(
                                    emptyList(),
                                    "",
                                    "",
                                    "",
                                    emptyList(),
                                    HashMap<String, Any>(),
                                    "testDestination$i"
                                ),
                                TolokaMappingModerationSkuInfo(
                                    "Test Hint ${category.name + i}",
                                    i.toLong(),
                                    emptyList(),
                                    "",
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    "",
                                    ""
                                ),
                                TolokaInstruction("", "", "", "")
                            )
                        ),
                        listOf(
                            KnownSolution(
                                mapOf("result" to "ACCEPTED"),
                                1.0
                            ),
                            KnownSolution(
                                mapOf("result" to "REJECTED"),
                                0.25
                            ),
                        ),
                        "TestHint$i"
                    )
                )
            }
        }
        return honeypots
    }

    private fun generateRealTasks(
        categoryList: List<CategoryInfo>,
        tasksPerCategoryCount: Int
    ): MutableList<TaskInTaskSuite> {
        var taskKey = 0
        val realTasks = mutableListOf<TaskInTaskSuite>()
        for (category in categoryList) {
            for (i in 1..tasksPerCategoryCount) {
                realTasks.add(
                    TaskInTaskSuite(
                        taskKey = taskKey++.toString(),
                        input = TolokaMappingModerationOffer(
                            category.hid,
                            category.name + i,
                            i.toLong(),
                            i.toLong(),
                            i.toLong(),
                            TolokaMappingModerationOfferInfo(
                                emptyList(),
                                "",
                                "",
                                "",
                                emptyList(),
                                HashMap<String, Any>(),
                                "testDestination$i"
                            ),
                            TolokaMappingModerationSkuInfo(
                                "Test sku ${category.name + i}",
                                i.toLong(),
                                emptyList(),
                                "",
                                emptyList(),
                                emptyList(),
                                emptyList(),
                                "",
                                ""
                            ),
                            TolokaInstruction("", "", "", "")
                        )
                    )
                )
            }
        }
        return realTasks
    }

    private fun anyToInputValueMap(any: Any): MutableMap<String, Any?> {
        val mapTypeRef = object : TypeReference<Map<String, Any?>>() {}
        return CommonObjectMapper.convertValue<Map<String, Any?>>(any, mapTypeRef).toMutableMap()
    }
}
