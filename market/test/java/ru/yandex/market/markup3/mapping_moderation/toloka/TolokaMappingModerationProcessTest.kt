package ru.yandex.market.markup3.mapping_moderation.toloka

import com.googlecode.protobuf.format.JsonFormat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.market.markup3.api.Markup3ApiService
import ru.yandex.market.markup3.core.TolokaSource
import ru.yandex.market.markup3.core.dto.ProcessingStatus
import ru.yandex.market.markup3.core.dto.TaskGroup
import ru.yandex.market.markup3.core.dto.TaskGroupConfig
import ru.yandex.market.markup3.core.executor.TaskGroupExecutor
import ru.yandex.market.markup3.core.executor.TaskGroupExecutorProvider
import ru.yandex.market.markup3.core.services.CreateTask
import ru.yandex.market.markup3.core.services.CreateTaskRequest
import ru.yandex.market.markup3.core.services.TaskFacadeProvider
import ru.yandex.market.markup3.core.services.TaskService
import ru.yandex.market.markup3.mapping_moderation.yang.YangMappingModerationProcessTest
import ru.yandex.market.markup3.mboc.category.info.CategoryInfo
import ru.yandex.market.markup3.mboc.category.info.repository.CategoryInfoRepository
import ru.yandex.market.markup3.mboc.category.info.service.CategoryInfoService
import ru.yandex.market.markup3.mboc.moderation.MbocMappingModerationConstants
import ru.yandex.market.markup3.mboc.moderation.generator.TolokaMappingModerationGenerator
import ru.yandex.market.markup3.mboc.moderation.processor.TolokaMappingModerationResultProcessor
import ru.yandex.market.markup3.mboc.moderation.repository.TolokaActiveMappingModerationRepository
import ru.yandex.market.markup3.mocks.MboCategoryServiceMock
import ru.yandex.market.markup3.remote.CategoryModelsServiceTest
import ru.yandex.market.markup3.remote.HintsReader
import ru.yandex.market.markup3.remote.HoneypotsReader
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.TolokaMappingModerationHandler
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.dto.TolokaMappingModerationInput
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.dto.TolokaMappingModerationInputOffer
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.dto.TolokaMappingModerationState
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.ytlogwriter.TolokaMappingModerationLogQueue
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.users.profile.TolokaProfileRow
import ru.yandex.market.markup3.users.profile.repository.TolokaProfileRepository
import ru.yandex.market.markup3.utils.CommonObjectMapper
import ru.yandex.market.markup3.yang.TolokaClientMock
import ru.yandex.market.markup3.yang.dto.TolokaRecoverStatus
import ru.yandex.market.markup3.yang.repositories.TolokaRecoverQueueRepository
import ru.yandex.market.markup3.yang.services.TolokaResultsDownloader
import ru.yandex.market.markup3.yang.services.recover.RecoverTaskService
import ru.yandex.market.mbo.export.CategoryModelsService
import ru.yandex.market.mbo.export.MboExport
import ru.yandex.market.mbo.http.ModelStorage
import ru.yandex.market.mbo.http.ModelStorageService
import ru.yandex.market.mboc.http.MboCategory
import ru.yandex.market.mboc.http.SupplierOffer
import ru.yandex.toloka.client.v1.pool.MixerConfig
import ru.yandex.toloka.client.v1.pool.Pool
import ru.yandex.toloka.client.v1.pool.dynamicoverlap.BasicDynamicOverlapConfig
import java.io.InputStreamReader
import java.math.BigDecimal
import java.time.Instant
import java.util.Date

/**
 * @author shadoff
 * created on 9/1/21
 */
class TolokaMappingModerationProcessTest : CommonTaskTest() {
    @Autowired
    lateinit var taskGroupExecutorProvider: TaskGroupExecutorProvider

    @Autowired
    lateinit var tolokaClientMock: TolokaClientMock

    @Autowired
    lateinit var yangResultsDownloader: TolokaResultsDownloader

    @Autowired
    lateinit var tolokaRecoverQueueRepository: TolokaRecoverQueueRepository

    @Autowired
    lateinit var recoverTaskService: RecoverTaskService

    @Autowired
    lateinit var tolokaMappingModerationHandler: TolokaMappingModerationHandler

    @Autowired
    lateinit var tolokaMappingModerationGenerator: TolokaMappingModerationGenerator

    @Autowired
    lateinit var tolokaMappingModerationResultProcessor: TolokaMappingModerationResultProcessor

    @Autowired
    lateinit var tolokaActiveMappingModerationRepository: TolokaActiveMappingModerationRepository

    @Autowired
    lateinit var mboCategoryService: MboCategoryServiceMock

    @Autowired
    lateinit var tolokaProfileRepository: TolokaProfileRepository

    @Autowired
    lateinit var tolokaMappingModerationLogQueue: TolokaMappingModerationLogQueue

    @Autowired
    lateinit var remoteCategoryModelsService: CategoryModelsService

    @Autowired
    lateinit var remoteModelStorageService: ModelStorageService

    @Autowired
    lateinit var categoryInfoService: CategoryInfoService

    @Autowired
    lateinit var categoryInfoRepository: CategoryInfoRepository

    @Autowired
    lateinit var tolokaFacadeProvider: TaskFacadeProvider

    @Autowired
    lateinit var honeypotsReader: HoneypotsReader

    @Autowired
    lateinit var markup3ApiService: Markup3ApiService

    @Autowired
    lateinit var hintsReader: HintsReader

    lateinit var taskGroupExecutor: TaskGroupExecutor

    lateinit var taskGroup: TaskGroup

    companion object {
        private val defaultOfferIds = listOf(5L, 6L)
    }

    @Before
    fun setUp() {
        val basePool = createBasePool()

        taskGroup = taskGroupRegistry.getOrCreateTaskGroup(MbocMappingModerationConstants.TOLOKA_GROUP_KEY) {
            TaskGroup(
                key = MbocMappingModerationConstants.TOLOKA_GROUP_KEY,
                name = "toloka mm",
                taskType = TaskType.TOLOKA_MAPPING_MODERATION,
                config = TaskGroupConfig(basePoolId = basePool.id)
            )
        }
        taskGroupExecutor = taskGroupExecutorProvider.createTaskGroupExecutor(taskGroup, sleepTimeMs = 0)

        tolokaProfileRepository.insert(
            TolokaProfileRow(
                YangMappingModerationProcessTest.WORKER_ID,
                YangMappingModerationProcessTest.STAFF_LOGIN,
                YangMappingModerationProcessTest.UID
            )
        )

        updateMbocServiceGetTaskOffersAnswer(defaultTaskOffersAnswer())

        mboCategoryService.savedMappingModerationIds.clear()
        mboCategoryService.setDefaultSaveResponse()

        val exampleResponse = MboExport.GetCategoryModelsResponse.newBuilder()
        val resource = javaClass.getResourceAsStream("/remote/exporter-getSkus.json") ?: error("test file not found")
        JsonFormat.merge(InputStreamReader(resource), exampleResponse)
        doAnswer { invocation ->
            CategoryModelsServiceTest.filterCategoryModels(invocation, exampleResponse)
        }.`when`(remoteCategoryModelsService).getSkus(Mockito.any())
        doAnswer { invocation ->
            CategoryModelsServiceTest.filterCategoryModels(invocation, exampleResponse)
        }.`when`(remoteCategoryModelsService).getModels(Mockito.any())
        doAnswer { invocation ->
            val rq = invocation.arguments[0] as ModelStorage.FindModelsRequest
            ModelStorage.GetModelsResponse.newBuilder().apply {
                addAllModels(exampleResponse.modelsList.filter { rq.modelIdsList.contains(it.id) })
            }.build()
        }.`when`(remoteModelStorageService).findModels(Mockito.any())
    }

    @After
    fun tearDown() {
        mboCategoryService.setDefaultSaveResponse()
        mboCategoryService.taskOffersMap.clear()
    }

    @Test
    fun `test toloka mapping moderation full process`() {
        val categoryId = 90578L
        val requiredParamInCard = "Test requiredParamInCard"
        val requiredParamToCompare = "Test requiredParamToCompare"
        val decisionByPhoto = "Test decisionByPhoto"
        val additionalComments = "Test additionalComments"

        categoryInfoRepository.insert(
            CategoryInfo(
                categoryId, -1, "Сплит-системы", "", false, true, true, "", "", true,
                requiredParamInCard,
                requiredParamToCompare,
                decisionByPhoto,
                additionalComments,
            )
        )
        categoryInfoService.invalidateCache()
        categoryInfoService.getAllCategoryInfos()
        tolokaClientMock.resetAllTasks()

        tolokaMappingModerationGenerator.generate()

        tolokaMappingModerationLogQueue.getFromQueue(100) shouldHaveSize 0

        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe TaskService.INIT_STAGE

        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()

        val pool = getOnlyOnePool()

        val externalPool = tolokaClientMock.getPool(pool.externalPoolId)
        // Two offers: 1 blue 1 dsbs
        // 40 + (1/2 * (90 - 40)) = 65
        externalPool.priority shouldBe 65L

        val tolokaMappingModerationState = CommonObjectMapper.treeToValue(
            task.state, TolokaMappingModerationState::class.java
        )

        val taskTransferInfos = tolokaMappingModerationState.taskTransferInfos
        taskTransferInfos!! shouldHaveSize 2

        tolokaMappingModerationState.poolId shouldBe pool.id
        tolokaMappingModerationState.externalPoolId shouldBe pool.externalPoolId?.toLong()

        val offerId1 = 5L
        val taskTransferInfoOffer1 = taskTransferInfos[offerId1]!!
        taskTransferInfoOffer1.offerDestination shouldBe "BLUE"
        taskTransferInfoOffer1.sourceType shouldBe "GURU"
        taskTransferInfoOffer1.currentType shouldBe "SKU"

        val offerId2 = 6L
        val taskTransferInfoOffer2 = taskTransferInfos[offerId2]!!
        taskTransferInfoOffer2.offerDestination shouldBe "DSBS"
        taskTransferInfoOffer2.sourceType shouldBe "SKU"
        taskTransferInfoOffer2.currentType shouldBe "SKU"

        val logQueue = tolokaMappingModerationLogQueue.getFromQueue(100)
        logQueue shouldHaveSize 2
        logQueue[0].data["sku_id"] shouldBe 441852073
        logQueue[0].data["task_id"] shouldBe task.id
        logQueue[0].data["pool_id"] shouldBe pool.externalPoolId?.toInt()
        logQueue[0].data["offer_id"] shouldBe 5
        logQueue[0].data["offer_destination"] shouldBe "BLUE"
        logQueue[0].data["source_type"] shouldBe "GURU"
        logQueue[0].data["current_type"] shouldBe "SKU"
        logQueue[0].data["task_group_key"] shouldBe taskGroup.key

        logQueue[1].data["sku_id"] shouldBe 100898502993
        logQueue[1].data["task_id"] shouldBe task.id
        logQueue[1].data["pool_id"] shouldBe pool.externalPoolId?.toInt()
        logQueue[1].data["offer_id"] shouldBe 6
        logQueue[1].data["offer_destination"] shouldBe "DSBS"
        logQueue[1].data["source_type"] shouldBe "SKU"
        logQueue[1].data["current_type"] shouldBe "SKU"
        logQueue[0].data["task_group_key"] shouldBe taskGroup.key

        pool.source shouldBe TolokaSource.TOLOKA
        pool.active shouldBe true

        var taskInfos = tolokaTaskInfoRepository.findAll()
        taskInfos shouldHaveSize 2
        taskInfos.forEach { taskInfo ->
            taskInfo.taskId shouldBe task.id
            taskInfo.tolokaTaskId shouldNotBe null

            tolokaClientMock.getTask(taskInfo.tolokaTaskId) shouldNotBe null

            tolokaClientMock.addTaskAggregatedResult(taskInfo.tolokaTaskId, getSampleOutput())
        }

        tolokaClientMock.setPoolClosed(pool.externalPoolId)

        yangResultsDownloader.downloadAllDynamicPools() //start operation
        tolokaClientMock.finishAllOperations()
        yangResultsDownloader.downloadAllDynamicPools() //finish operation

        taskInfos = tolokaTaskInfoRepository.findAll()
        taskInfos.forEach { taskInfo ->
            taskInfo.solution shouldNotBe null
        }

        taskGroupExecutor.processSingleEvent()


        task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.DONE

        tolokaMappingModerationLogQueue.getFromQueue(100) shouldHaveSize 4

        tolokaMappingModerationResultProcessor.process()

        val active = tolokaActiveMappingModerationRepository.findAll()
        active shouldHaveSize 0

        val savedMappingModerationIds = mboCategoryService.savedMappingModerationIds
        savedMappingModerationIds shouldHaveSize 2
        savedMappingModerationIds.map { it.toLong() } shouldContainExactlyInAnyOrder defaultOfferIds

        val request = mboCategoryService.lastSavedMappingModerationRequest
        request!!.resultsList.forEach { it.fromToloka shouldBe true }

        val allTasks = tolokaClientMock.allTasks
        allTasks.values.filter { it.knownSolutions == null }
            .forEach {
                val instructions: Map<String, String> = it.inputValues["toloka_instructions"]!! as Map<String, String>
                instructions["required_param_in_card"] shouldBe requiredParamInCard
                instructions["required_param_to_compare"] shouldBe requiredParamToCompare
                instructions["decision_by_photo"] shouldBe decisionByPhoto
                instructions["additional_comments"] shouldBe additionalComments
            }
    }

    @Test
    fun `test toloka mapping moderation skipped offers`() {
        val exampleResponse = MboExport.GetCategoryModelsResponse.newBuilder()
        val resource = javaClass.getResourceAsStream("/remote/exporter-getSkus.json") ?: error("test file not found")
        JsonFormat.merge(InputStreamReader(resource), exampleResponse)
        val models = exampleResponse.modelsList.filter { model -> model.id == 100898502993 }.toList()
        val response = exampleResponse.clearModels().addAllModels(models).build()

        Mockito.doReturn(response)
            .`when`(remoteCategoryModelsService).getSkus(Mockito.any())
        Mockito.doReturn(response)
            .`when`(remoteCategoryModelsService).getModels(Mockito.any())
        tolokaMappingModerationGenerator.generate()

        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe TaskService.INIT_STAGE

        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()

        val state = CommonObjectMapper.treeToValue(task.state, tolokaMappingModerationHandler.handle.state)
        state.skippedIds shouldHaveSize 1
        state.skippedIds shouldContainExactly listOf(5L)

        val taskInfos = tolokaTaskInfoRepository.findAll()
        taskInfos shouldHaveSize 1
        taskInfos.forEach { taskInfo ->
            taskInfo.taskId shouldBe task.id
            taskInfo.tolokaTaskId shouldNotBe null

            tolokaClientMock.getTask(taskInfo.tolokaTaskId) shouldNotBe null

            tolokaClientMock.addTaskAggregatedResult(taskInfo.tolokaTaskId, getSampleOutput())
        }

        val pool = getOnlyOnePool()

        val externalPool = tolokaClientMock.getPool(pool.externalPoolId)
        // Two offers: 1 blue but is skipped, 1 dsbs
        // For only dsbs base priority is used - which is 20
        externalPool.priority shouldBe 20L

        tolokaClientMock.setPoolClosed(pool.externalPoolId)

        yangResultsDownloader.downloadAllDynamicPools() //start operation
        tolokaClientMock.finishAllOperations()
        yangResultsDownloader.downloadAllDynamicPools() //finish operation

        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.DONE
        tolokaMappingModerationResultProcessor.process()

        var active = tolokaActiveMappingModerationRepository.findAll()
        active shouldHaveSize 0

        val savedMappingModerationIds = mboCategoryService.savedMappingModerationIds
        savedMappingModerationIds shouldHaveSize 1
        savedMappingModerationIds[0] shouldBe "6"

        updateMbocServiceGetTaskOffersAnswer(taskOffersAnswer(1, listOf(5, 10)))

        tolokaMappingModerationGenerator.generate()

        active = tolokaActiveMappingModerationRepository.findAll()
        active shouldHaveSize 2
        active.map { it.offerId } shouldContainExactlyInAnyOrder listOf(5, 10)

        val tasks = taskDbService.findAll()
        tasks.map { it.processingStatus } shouldContainExactlyInAnyOrder listOf(
            ProcessingStatus.ACTIVE,
            ProcessingStatus.DONE
        )
    }

    @Test
    fun `test toloka mapping moderation with target sku id`() {
        val categoryId = 90578L
        val requiredParamInCard = "Test requiredParamInCard"
        val requiredParamToCompare = "Test requiredParamToCompare"
        val decisionByPhoto = "Test decisionByPhoto"
        val additionalComments = "Test additionalComments"

        categoryInfoRepository.insert(
            CategoryInfo(
                categoryId, -1, "Сплит-системы", "", false, true, true, "", "", true,
                requiredParamInCard,
                requiredParamToCompare,
                decisionByPhoto,
                additionalComments,
            )
        )
        categoryInfoService.invalidateCache()
        categoryInfoService.getAllCategoryInfos()
        tolokaClientMock.resetAllTasks()

        val tolokaFacade = tolokaFacadeProvider.getTaskFacade(TolokaMappingModerationHandler::class.java)
        tolokaFacade.createTasks(
            CreateTaskRequest(
                taskGroupId = taskGroup.id,
                tasks = listOf(
                    CreateTask(
                        input = TolokaMappingModerationInput(
                            offers = listOf(
                                TolokaMappingModerationInputOffer(
                                    offerId = 5,
                                    targetSkuId = 100898502993
                                ),
                                TolokaMappingModerationInputOffer(
                                    offerId = 6,
                                    targetSkuId = 100898502993
                                )
                            )
                        ),
                        uniqueKeys = listOf("5", "6")
                    )
                )
            )
        )

        tolokaMappingModerationLogQueue.getFromQueue(100) shouldHaveSize 0

        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe TaskService.INIT_STAGE

        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()

        val pool = getOnlyOnePool()

        val externalPool = tolokaClientMock.getPool(pool.externalPoolId)
        // Two offers: 2 with target_sku_id
        // 30 + (2/2 * (39 - 30)) = 39
        externalPool.priority shouldBe 39L

        val logQueue = tolokaMappingModerationLogQueue.getFromQueue(100)
        logQueue shouldHaveSize 2
        logQueue[0].data["sku_id"] shouldBe 100898502993
        logQueue[0].data["task_id"] shouldBe task.id
        logQueue[0].data["pool_id"] shouldBe pool.externalPoolId?.toInt()
        logQueue[0].data["offer_id"] shouldBe 5
        logQueue[0].data["offer_destination"] shouldBe "BLUE"
        logQueue[0].data["source_type"] shouldBe "SKU"
        logQueue[0].data["current_type"] shouldBe "SKU"
        logQueue[0].data["task_group_key"] shouldBe taskGroup.key

        logQueue[1].data["sku_id"] shouldBe 100898502993
        logQueue[1].data["task_id"] shouldBe task.id
        logQueue[1].data["pool_id"] shouldBe pool.externalPoolId?.toInt()
        logQueue[1].data["offer_id"] shouldBe 6
        logQueue[1].data["offer_destination"] shouldBe "DSBS"
        logQueue[1].data["source_type"] shouldBe "SKU"
        logQueue[1].data["current_type"] shouldBe "SKU"
        logQueue[0].data["task_group_key"] shouldBe taskGroup.key

        pool.source shouldBe TolokaSource.TOLOKA
        pool.active shouldBe true

        var taskInfos = tolokaTaskInfoRepository.findAll()
        taskInfos shouldHaveSize 2
        taskInfos.forEach { taskInfo ->
            taskInfo.taskId shouldBe task.id
            taskInfo.tolokaTaskId shouldNotBe null

            tolokaClientMock.getTask(taskInfo.tolokaTaskId) shouldNotBe null

            tolokaClientMock.addTaskAggregatedResult(taskInfo.tolokaTaskId, getSampleOutput())
        }

        tolokaClientMock.setPoolClosed(pool.externalPoolId)

        yangResultsDownloader.downloadAllDynamicPools() //start operation
        tolokaClientMock.finishAllOperations()
        yangResultsDownloader.downloadAllDynamicPools() //finish operation

        taskInfos = tolokaTaskInfoRepository.findAll()
        taskInfos.forEach { taskInfo ->
            taskInfo.solution shouldNotBe null
        }

        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.DONE

        tolokaMappingModerationLogQueue.getFromQueue(100) shouldHaveSize 4

        tolokaMappingModerationResultProcessor.process()

        val active = tolokaActiveMappingModerationRepository.findAll()
        active shouldHaveSize 0

        val savedMappingModerationIds = mboCategoryService.savedMappingModerationIds
        savedMappingModerationIds shouldHaveSize 2
        savedMappingModerationIds.map { it.toLong() } shouldContainExactlyInAnyOrder defaultOfferIds

        val request = mboCategoryService.lastSavedMappingModerationRequest
        request!!.resultsList.forEach { it.fromToloka shouldBe true }

        val allTasks = tolokaClientMock.allTasks
        allTasks.values.filter { it.knownSolutions == null }
            .forEach {
                val instructions: Map<String, String> = it.inputValues["toloka_instructions"]!! as Map<String, String>
                instructions["required_param_in_card"] shouldBe requiredParamInCard
                instructions["required_param_to_compare"] shouldBe requiredParamToCompare
                instructions["decision_by_photo"] shouldBe decisionByPhoto
                instructions["additional_comments"] shouldBe additionalComments
            }
    }

    @Test
    fun `test instructions disabled`() {
        keyValueService.putValue("add_instr_${taskGroup.key}", false)

        val categoryId = 90578L
        val requiredParamInCard = "Test requiredParamInCard"
        val requiredParamToCompare = "Test requiredParamToCompare"
        val decisionByPhoto = "Test decisionByPhoto"
        val additionalComments = "Test additionalComments"

        categoryInfoRepository.insert(
            CategoryInfo(
                categoryId, -1, "Сплит-системы", "", false, true, true, "", "", true,
                requiredParamInCard,
                requiredParamToCompare,
                decisionByPhoto,
                additionalComments,
            )
        )
        categoryInfoService.invalidateCache()
        categoryInfoService.getAllCategoryInfos()
        tolokaClientMock.resetAllTasks()

        val tolokaFacade = tolokaFacadeProvider.getTaskFacade(TolokaMappingModerationHandler::class.java)
        tolokaFacade.createTasks(
            CreateTaskRequest(
                taskGroupId = taskGroup.id,
                tasks = listOf(
                    CreateTask(
                        input = TolokaMappingModerationInput(
                            offers = listOf(
                                TolokaMappingModerationInputOffer(
                                    offerId = 5,
                                    targetSkuId = 100898502993
                                ),
                                TolokaMappingModerationInputOffer(
                                    offerId = 6,
                                    targetSkuId = 100898502993
                                )
                            )
                        ),
                        uniqueKeys = listOf("5", "6")
                    )
                )
            )
        )

        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe TaskService.INIT_STAGE

        taskGroupExecutor.processSingleEvent()

        val allTasks = tolokaClientMock.allTasks
        allTasks.values.filter { it.knownSolutions == null }
            .forEach {
                it.inputValues.containsKey("toloka_instructions") shouldBe false
            }
    }

    @Test
    fun `deduplication do not send statuses`() {
        taskGroup = taskGroup.copy(key = "asdfg")
        taskGroupRepository.update(taskGroup)
        taskGroupRegistry.refresh()

        val tolokaFacade = tolokaFacadeProvider.getTaskFacade(TolokaMappingModerationHandler::class.java)
        tolokaFacade.createTasks(
            CreateTaskRequest(
                taskGroupId = taskGroup.id,
                tasks = listOf(
                    CreateTask(
                        input = TolokaMappingModerationInput(
                            offers = listOf(
                                TolokaMappingModerationInputOffer(
                                    offerId = 5,
                                    targetSkuId = 100898502993
                                ),
                                TolokaMappingModerationInputOffer(
                                    offerId = 6,
                                    targetSkuId = 100898502993
                                )
                            )
                        ),
                        uniqueKeys = listOf("5", "6")
                    )
                )
            )
        )

        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe TaskService.INIT_STAGE

        taskGroupExecutor.processSingleEvent()

        var taskInfos = tolokaTaskInfoRepository.findAll()
        taskInfos shouldHaveSize 2
    }

    @Test
    fun `test toloka mapping moderation reprocess when mapping changed`() {
        val offerId = 5
        mboCategoryService.saveResponse = MboCategory.SaveMappingModerationResponse.newBuilder()
            .setResult(
                SupplierOffer.OperationResult.newBuilder()
                    .setStatus(SupplierOffer.OperationStatus.SUCCESS)
                    .addOfferStatuses(
                        SupplierOffer.OfferStatus.newBuilder()
                            .setOfferId(offerId.toString())
                            .setStatus(SupplierOffer.OperationStatus.REPROCESS)
                            .build()
                    )
            ).build()

        tolokaMappingModerationGenerator.generate()

        var task = getOnlyOneTask()
        taskGroupExecutor.processSingleEvent()

        val pool = getOnlyOnePool()

        val externalPool = tolokaClientMock.getPool(pool.externalPoolId)
        // Two offers: 1 blue 1 dsbs
        // 40 + (1/2 * (90 - 40)) = 65
        externalPool.priority shouldBe 65L

        val taskInfos = tolokaTaskInfoRepository.findAll()
        taskInfos shouldHaveSize 2
        taskInfos.forEach { taskInfo ->
            taskInfo.taskId shouldBe task.id
            taskInfo.tolokaTaskId shouldNotBe null

            tolokaClientMock.getTask(taskInfo.tolokaTaskId) shouldNotBe null
            tolokaClientMock.addTaskAggregatedResult(taskInfo.tolokaTaskId, getSampleOutput())
        }

        tolokaClientMock.setPoolClosed(pool.externalPoolId)

        yangResultsDownloader.downloadAllDynamicPools() //start operation
        tolokaClientMock.finishAllOperations()
        yangResultsDownloader.downloadAllDynamicPools() //finish operation

        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.DONE

        tolokaMappingModerationResultProcessor.process()

        var active = tolokaActiveMappingModerationRepository.findAll()
        active shouldHaveSize 0

        updateMbocServiceGetTaskOffersAnswer(taskOffersAnswer(1, listOf(5, 10)))

        tolokaMappingModerationGenerator.generate()

        active = tolokaActiveMappingModerationRepository.findAll()
        active shouldHaveSize 2
        active.map { it.offerId } shouldContainExactlyInAnyOrder listOf(5, 10)

        val tasks = taskDbService.findAll()
        tasks.map { it.processingStatus } shouldContainExactlyInAnyOrder listOf(
            ProcessingStatus.ACTIVE,
            ProcessingStatus.DONE
        )
    }

    @Test
    fun `test recover task - not found in pool`() {
        tolokaMappingModerationGenerator.generate()

        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe TaskService.INIT_STAGE

        tolokaClientMock.setShouldThrowOnce(true)
        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe TaskService.INIT_STAGE

        val pool = getOnlyOnePool()

        val externalPool = tolokaClientMock.getPool(pool.externalPoolId)
        // Two offers: 1 blue 1 dsbs
        // 40 + (1/2 * (90 - 40)) = 65
        externalPool.priority shouldBe 65L

        var initEvent = taskEventDbService.findAll()[0]
        initEvent.taskId shouldBe task.id
        initEvent.retries shouldBe 1
        taskEventDbService.update(
            initEvent.copy(
                nextRun = Instant.now()
            )
        )

        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe TaskService.INIT_STAGE

        val taskInfos = tolokaTaskInfoRepository.findAll()
        taskInfos.forEach { it.tolokaTaskId shouldBe null }

        var recoverQueue = tolokaRecoverQueueRepository.findAll()
        recoverQueue shouldHaveSize 2
        recoverQueue.forEach { it.poolId shouldBe pool.id }
        recoverQueue.forEach { it.taskId shouldBe task.id }
        recoverQueue.map { it.created } shouldContainExactly taskInfos.map { it.created }
        recoverQueue.forEach { it.tolokaRecoverStatus shouldBe TolokaRecoverStatus.NEW }

        recoverTaskService.recoverAll()

        recoverQueue = tolokaRecoverQueueRepository.findAll()
        recoverQueue shouldHaveSize 2
        recoverQueue.forEach { it.poolId shouldBe pool.id }
        recoverQueue.forEach { it.taskId shouldBe task.id }
        recoverQueue.forEach { it.tolokaRecoverStatus shouldBe TolokaRecoverStatus.NOT_RECOVERED }

        initEvent = taskEventDbService.findAll()[0]
        initEvent.taskId shouldBe task.id
        initEvent.retries shouldBe 2
        taskEventDbService.update(
            initEvent.copy(
                nextRun = Instant.now()
            )
        )
        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
    }

    @Test
    fun `test recover task - found in pool`() {
        tolokaMappingModerationGenerator.generate()

        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe TaskService.INIT_STAGE

        tolokaClientMock.setShouldThrowOnce(true)
        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe TaskService.INIT_STAGE

        val pool = getOnlyOnePool()

        val externalPool = tolokaClientMock.getPool(pool.externalPoolId)
        // Two offers: 1 blue 1 dsbs
        // 40 + (1/2 * (90 - 40)) = 65
        externalPool.priority shouldBe 65L

        var initEvent = taskEventDbService.findAll()[0]
        initEvent.taskId shouldBe task.id
        initEvent.retries shouldBe 1
        taskEventDbService.update(
            initEvent.copy(
                nextRun = Instant.now()
            )
        )

        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe TaskService.INIT_STAGE

        var taskInfos = tolokaTaskInfoRepository.findAll()
        taskInfos.forEach { it.tolokaTaskId shouldBe null }

        var recoverQueue = tolokaRecoverQueueRepository.findAll()
        recoverQueue shouldHaveSize 2
        recoverQueue.forEach { it.poolId shouldBe pool.id }
        recoverQueue.forEach { it.taskId shouldBe task.id }
        recoverQueue.map { it.created } shouldContainExactly taskInfos.map { it.created }
        recoverQueue.forEach { it.tolokaRecoverStatus shouldBe TolokaRecoverStatus.NEW }

        tolokaClientMock.setFindTasksResultFromTaskMap()
        recoverTaskService.recoverAll()

        recoverQueue = tolokaRecoverQueueRepository.findAll()
        recoverQueue shouldHaveSize 2
        recoverQueue.forEach { it.poolId shouldBe pool.id }
        recoverQueue.forEach { it.taskId shouldBe task.id }
        recoverQueue.forEach { it.tolokaRecoverStatus shouldBe TolokaRecoverStatus.RECOVERED }

        initEvent = taskEventDbService.findAll()[0]
        initEvent.taskId shouldBe task.id
        initEvent.retries shouldBe 2
        taskEventDbService.update(
            initEvent.copy(
                nextRun = Instant.now()
            )
        )
        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE

        taskInfos = tolokaTaskInfoRepository.findAll()
        taskInfos.forEach { it.tolokaTaskId shouldNotBe null }
    }

    @Test
    fun `test recover task - do not open pool until recover`() {
        tolokaMappingModerationGenerator.generate()

        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe TaskService.INIT_STAGE

        tolokaClientMock.setShouldThrowOnce(true)
        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe TaskService.INIT_STAGE

        var pool = getOnlyOnePool()

        val externalPool = tolokaClientMock.getPool(pool.externalPoolId)
        // Two offers: 1 blue 1 dsbs
        // 40 + (1/2 * (90 - 40)) = 65
        externalPool.priority shouldBe 65L

        var initEvent = taskEventDbService.findAll()[0]
        initEvent.taskId shouldBe task.id
        initEvent.retries shouldBe 1
        taskEventDbService.update(
            initEvent.copy(
                nextRun = Instant.now()
            )
        )

        taskGroupExecutor.processSingleEvent()

        pool.fullyDownloaded shouldBe true
        yangResultsDownloader.downloadAllDynamicPools() //start operation
        tolokaClientMock.finishAllOperations()
        yangResultsDownloader.downloadAllDynamicPools() //finish operation

        pool = getOnlyOnePool()
        pool.active shouldBe true // pool is not finished

        recoverTaskService.recoverAll()

        initEvent = taskEventDbService.findAll()[0]
        initEvent.taskId shouldBe task.id
        initEvent.retries shouldBe 2
        taskEventDbService.update(
            initEvent.copy(
                nextRun = Instant.now()
            )
        )
        taskGroupExecutor.processSingleEvent()

        pool = getOnlyOnePool()
        pool.fullyDownloaded shouldBe false

        val taskInfos = tolokaTaskInfoRepository.findAll()
        taskInfos shouldHaveSize 2
        taskInfos.forEach { taskInfo ->
            taskInfo.taskId shouldBe task.id
            taskInfo.tolokaTaskId shouldNotBe null

            tolokaClientMock.getTask(taskInfo.tolokaTaskId) shouldNotBe null

            tolokaClientMock.addTaskAggregatedResult(taskInfo.tolokaTaskId, getSampleOutput())
        }

        pool = getOnlyOnePool()
        tolokaClientMock.setPoolClosed(pool.externalPoolId)

        yangResultsDownloader.downloadAllDynamicPools() //start operation
        tolokaClientMock.finishAllOperations()
        yangResultsDownloader.downloadAllDynamicPools() //finish operation

        pool = getOnlyOnePool()
        pool.active shouldBe false
    }

    @Test
    fun `test recover task - skipped after recover - not recovered`() {
        `test skipped after recover base`(false)
    }

    @Test
    fun `test recover task - skipped after recover - recovered and lost`() {
        `test skipped after recover base`(true)
    }

    private fun `test skipped after recover base`(isRecoveredFlag: Boolean) {
        tolokaClientMock.allTasks.clear()
        tolokaMappingModerationGenerator.generate()

        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe TaskService.INIT_STAGE

        tolokaClientMock.setShouldThrowOnce(true)
        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe TaskService.INIT_STAGE

        val pool = getOnlyOnePool()

        val externalPool = tolokaClientMock.getPool(pool.externalPoolId)
        // Two offers: 1 blue 1 dsbs
        // 40 + (1/2 * (90 - 40)) = 65
        externalPool.priority shouldBe 65L

        var initEvent = taskEventDbService.findAll()[0]
        initEvent.taskId shouldBe task.id
        initEvent.retries shouldBe 1
        taskEventDbService.update(
            initEvent.copy(
                nextRun = Instant.now()
            )
        )

        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe TaskService.INIT_STAGE

        var taskInfos = tolokaTaskInfoRepository.findAll()
        taskInfos.forEach { it.tolokaTaskId shouldBe null }

        var recoverQueue = tolokaRecoverQueueRepository.findAll()
        recoverQueue shouldHaveSize 2
        recoverQueue.forEach { it.poolId shouldBe pool.id }
        recoverQueue.forEach { it.taskId shouldBe task.id }
        recoverQueue.map { it.created } shouldContainExactly taskInfos.map { it.created }
        recoverQueue.forEach { it.tolokaRecoverStatus shouldBe TolokaRecoverStatus.NEW }

        if (isRecoveredFlag) {
            tolokaClientMock.setFindTasksResultFromTaskMap()
        }
        recoverTaskService.recoverAll()

        recoverQueue = tolokaRecoverQueueRepository.findAll()
        recoverQueue shouldHaveSize 2
        recoverQueue.forEach { it.poolId shouldBe pool.id }
        recoverQueue.forEach { it.taskId shouldBe task.id }

        val exampleResponse = MboExport.GetCategoryModelsResponse.newBuilder()
        val resource =
            javaClass.getResourceAsStream("/remote/exporter-getSkus.json") ?: error("test file not found")
        JsonFormat.merge(InputStreamReader(resource), exampleResponse)
        val models = exampleResponse.modelsList.filter { model -> model.id == 100898502993 }.toList()
        val response = exampleResponse.clearModels().addAllModels(models).build()

        Mockito.doReturn(response)
            .`when`(remoteCategoryModelsService).getSkus(Mockito.any())
        Mockito.doReturn(response)
            .`when`(remoteCategoryModelsService).getModels(Mockito.any())

        initEvent = taskEventDbService.findAll()[0]
        initEvent.taskId shouldBe task.id
        initEvent.retries shouldBe 2
        taskEventDbService.update(
            initEvent.copy(
                nextRun = Instant.now()
            )
        )
        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE

        val state = CommonObjectMapper.treeToValue(task.state, tolokaMappingModerationHandler.handle.state)
        state.skippedIds shouldHaveSize 1
        state.skippedIds shouldContainExactlyInAnyOrder listOf(5L)

        taskInfos = tolokaTaskInfoRepository.findAll()
        taskInfos shouldHaveSize 2
        taskInfos.forEach { taskInfo ->
            taskInfo.taskId shouldBe task.id
            tolokaClientMock.addTaskAggregatedResult(taskInfo.tolokaTaskId, getSampleOutput())
        }
        tolokaClientMock.setPoolClosed(pool.externalPoolId)

        yangResultsDownloader.downloadAllDynamicPools() //start operation
        tolokaClientMock.finishAllOperations()
        yangResultsDownloader.downloadAllDynamicPools() //finish operation

        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.DONE

        tolokaMappingModerationResultProcessor.process()

        val active = tolokaActiveMappingModerationRepository.findAll()
        active shouldHaveSize 0

        val savedMappingModerationIds = mboCategoryService.savedMappingModerationIds
        savedMappingModerationIds shouldHaveSize 1
        savedMappingModerationIds.map { it.toLong() } shouldContainExactly listOf(6L)

        taskDbService.findNotCancelledIdsByUniqueKeys(taskGroup.id, listOf("5.1", "6.1", "7.1"))
            .map { it.first } shouldContainExactly listOf("6.1")
    }

    @Test
    fun `test alternative honeypots path`() {
        Mockito.clearInvocations(honeypotsReader)
        Mockito.clearInvocations(hintsReader)

        val pool = createBasePool()
        pool.mixerConfig = MixerConfig(10, 1, 1)
        tolokaClientMock.updatePool(pool.id, pool)

        val localTaskGroup = taskGroupRegistry.getOrCreateTaskGroup("alternative honeypots key") {
            TaskGroup(
                key = "alternative honeypots key",
                name = "tmp name",
                taskType = TaskType.TOLOKA_MAPPING_MODERATION,
                config = TaskGroupConfig(basePoolId = pool.id)
            )
        }
        taskGroupExecutor = taskGroupExecutorProvider.createTaskGroupExecutor(localTaskGroup, sleepTimeMs = 0)

        val secretPath = "secret/path/here"
        keyValueService.putValue("honeypots_${localTaskGroup.key}", secretPath)
        val hintsPath = "yt/home/hints"
        keyValueService.putValue("hints_${localTaskGroup.key}", hintsPath)

        val tolokaFacade = tolokaFacadeProvider.getTaskFacade(TolokaMappingModerationHandler::class.java)
        tolokaFacade.createTasks(
            CreateTaskRequest(
                taskGroupId = localTaskGroup.id,
                tasks = listOf(
                    CreateTask(
                        input = TolokaMappingModerationInput(
                            offers = listOf(
                                TolokaMappingModerationInputOffer(
                                    offerId = 5,
                                    targetSkuId = 100898502993
                                ),
                                TolokaMappingModerationInputOffer(
                                    offerId = 6,
                                    targetSkuId = 100898502993
                                )
                            )
                        ),
                        uniqueKeys = listOf("5", "6")
                    )
                )
            )
        )

        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe TaskService.INIT_STAGE

        taskGroupExecutor.processSingleEvent()

        val captor = argumentCaptor<String>()
        verify(honeypotsReader, times(1)).loadHoneypots(any(), any(), captor.capture())
        captor.lastValue shouldBe secretPath

        verify(hintsReader, times(1)).loadHints(any(), any(), captor.capture())
        captor.lastValue shouldBe hintsPath
    }

    private fun getSampleOutput(): Map<String, Any> {
        return mapOf("result" to "ACCEPTED")
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

    private fun defaultTaskOffersAnswer(): List<MboCategory.GetTaskOffersResponse.TaskOffer.Builder> {
        return taskOffersAnswer(1)
    }

    private fun taskOffersAnswer(processingTicketId: Int): List<MboCategory.GetTaskOffersResponse.TaskOffer.Builder> {
        return taskOffersAnswer(processingTicketId, defaultOfferIds)
    }

    private fun taskOffersAnswer(
        processingTicketId: Int,
        offerIds: List<Long>
    ): List<MboCategory.GetTaskOffersResponse.TaskOffer.Builder> {
        return offerIds.map { offerId ->
            MboCategory.GetTaskOffersResponse.TaskOffer.newBuilder().apply {
                ticketId = processingTicketId.toLong()
                businessId = 2
                suggestSkuType = SupplierOffer.SkuType.TYPE_PARTNER
                priority = 3.toDouble()
                categoryId = 4
                this.offerId = offerId
                processingCounter = processingTicketId.toLong()
            }
        }
    }

    private fun updateMbocServiceGetTaskOffersAnswer(
        taskOffers: List<MboCategory.GetTaskOffersResponse.TaskOffer.Builder>
    ) {
        mboCategoryService.taskOffersMap.clear()
        mboCategoryService.taskOffersMap.putAll(taskOffers.associateBy { it.offerId })
    }
}
