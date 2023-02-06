package ru.yandex.market.mboc.processing.generator

import com.google.protobuf.Int64Value
import com.nhaarman.mockitokotlin2.any
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.mbo.storage.StorageKeyValueService
import ru.yandex.market.mboc.common.offers.model.OfferProcessingAssignment
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock
import ru.yandex.market.mboc.common.services.category.CategoryTree
import ru.yandex.market.mboc.common.services.category.models.Category
import ru.yandex.market.mboc.processing.BaseOfferProcessingTest
import ru.yandex.market.mboc.processing.Markup3ApiService
import ru.yandex.market.mboc.processing.OfferId
import ru.yandex.market.mboc.processing.ProcessingStrategyType
import ru.yandex.market.mboc.processing.assignment.OfferProcessingAssignmentRepository
import ru.yandex.market.mboc.processing.task.AbstractTaskGenerator
import ru.yandex.market.mboc.processing.task.OfferProcessingAssignmentAndTaskRepository
import ru.yandex.market.mboc.processing.task.OfferProcessingTask
import ru.yandex.market.mboc.processing.task.OfferProcessingTaskRepository

abstract class AbstractTaskGeneratorTest(
    protected val strategy: ProcessingStrategyType
) : BaseOfferProcessingTest() {

    @Autowired
    protected lateinit var markup3ApiServiceMock: Markup3ApiService
    @Autowired
    protected lateinit var offerProcessingTaskRepository: OfferProcessingTaskRepository
    @Autowired
    protected lateinit var offerProcessingAssignmentRepository: OfferProcessingAssignmentRepository
    @Autowired
    protected lateinit var offerProcessingAssignmentAndTaskRepository: OfferProcessingAssignmentAndTaskRepository
    @Autowired
    protected lateinit var skv: StorageKeyValueService

    protected lateinit var generator: AbstractTaskGenerator
    protected open lateinit var categoryCachingServiceMock: CategoryCachingServiceMock

    @Before
    open fun setUp() {
        categoryCachingServiceMock = Mockito.spy(CategoryCachingServiceMock())
        generator = initGenerator()
        setUpOptimalGroupBoundaries()
    }

    @Test
    open fun `generator should correctly process several groups`() {
        initOkMarkupApi()
        val existingAssignments = generateSeveralGroups(2, 20)

        generator.generate()

        val existingTasks = offerProcessingTaskRepository.findAll()
        checkTasksCorrectness(existingAssignments, existingTasks)
        checkTasksStrategyTypes(existingTasks)
    }

    @Test
    open fun `generator should regenerate not failed offers in uniqueness failed batches`() {
        initUniqFailMarkupApi()
        val existingAssignments = generateSeveralGroups(2, 20)
        val expectingNotFailedAssignments = existingAssignments.asSequence()
            .filterIndexed{ index, assignment -> index % 2 != 0 }
            .toList()

        generator.generate()

        val existingTasks = offerProcessingTaskRepository.findAll()
        checkTasksCorrectness(expectingNotFailedAssignments, existingTasks)
        checkTasksStrategyTypes(existingTasks)
    }

    @Test
    open fun `generator should regenerate only optimal by size failed groups`() {
        initUniqFailMarkupApi()
        setUpOptimalGroupBoundaries(min = 12, max = 20)
        generateSeveralGroups(2, 20)
        // could not recreate tasks because of boundaries
        val expectingNotFailedAssignments = emptyList<OfferProcessingAssignment>()

        generator.generate()

        val existingTasks = offerProcessingTaskRepository.findAll()
        checkTasksCorrectness(expectingNotFailedAssignments, existingTasks)
    }

    @Test
    fun `generator should process group with mixed categoryGroupId without exceptions`() {
        initOkMarkupApi()
        val existingAssignments = generateSeveralGroups(1, 10)

        existingAssignments.forEachIndexed { i, a -> a.categoryGroupId = i.toLong().takeIf { it % 2 == 0L } }
        offerProcessingAssignmentRepository.insertOrUpdateAll(existingAssignments)

        generator.generate()
    }

    abstract fun generateOneGroupAssignments(from: OfferId, to: OfferId): List<OfferProcessingAssignment>

    abstract fun initGenerator(): AbstractTaskGenerator

    protected fun initOkMarkupApi() {
        var idx = 0L

        Mockito.doAnswer { invocation ->
            val request = invocation.arguments[0] as Markup3Api.CreateTasksRequest
            return@doAnswer Markup3Api.CreateTasksResponse.newBuilder().apply {
                request.tasksList.map { task ->
                    Markup3Api.CreateTaskResponseItem.newBuilder().apply {
                        externalKey = task?.externalKey
                        taskId = Int64Value.of(++idx)
                        result = Markup3Api.CreateTaskResponseItem.CreateTaskResult.OK
                    }.build()
                }.let { addAllResponseItems(it) }
            }.build()
        }.`when`(markup3ApiServiceMock).createTask(any())
    }

    protected fun initUniqFailMarkupApi() {
        val offerIdPattern = Regex("\\..*")
        Mockito.doAnswer { invocation ->
            val request = invocation.arguments[0] as Markup3Api.CreateTasksRequest
            return@doAnswer Markup3Api.CreateTasksResponse.newBuilder().apply {
                request.tasksList.mapIndexed { index, task ->
                    Markup3Api.CreateTaskResponseItem.newBuilder().apply {
                        externalKey = task?.externalKey
                        taskId = Int64Value.of(index.toLong())
                        task.uniqKeysList.asSequence()
                            .filter { uniqKey -> offerIdPattern.replace(uniqKey, "").toInt() % 2 == 0 }
                            .map { it to index.toLong() }
                            .associateBy({ it.first }, { it.second })
                            .let { putAllFailedUniqKeys(it) }
                        result = if (failedUniqKeysCount > 0) {
                            Markup3Api.CreateTaskResponseItem.CreateTaskResult.FAIL_UNIQUE_KEY
                        } else {
                            Markup3Api.CreateTaskResponseItem.CreateTaskResult.OK
                        }
                    }.build()
                }.let { addAllResponseItems(it) }
            }.build()
        }.`when`(markup3ApiServiceMock).createTask(any())
    }

    protected fun setUpOptimalGroupBoundaries(min: Int = 1, max: Int = 10) {
        val minOffersInTaskKey = ReflectionTestUtils.getField(generator, "minOffersInTaskKey") as String
        val maxOffersInTaskKey = ReflectionTestUtils.getField(generator, "maxOffersInTaskKey") as String
        skv.putValue(minOffersInTaskKey, min)
        skv.putValue(maxOffersInTaskKey, max)
        skv.invalidateCache()
    }

    protected fun generateSeveralGroups(groupNumber: Int = 4, offersPerGroup: Int = 10) =
        (1..groupNumber).map {
            generateOneGroupAssignments(it.toLong() * offersPerGroup, (it + 1).toLong() * offersPerGroup - 1)
        }.flatten().toList()

    protected fun checkTasksCorrectness(
        existingAssignments: List<OfferProcessingAssignment>,
        existingTasks: List<OfferProcessingTask>
    ) {
        existingTasks.map { it.offerId } shouldContainExactlyInAnyOrder existingAssignments.map { it.offerId }
    }

    protected fun checkTasksStrategyTypes(existingTasks: List<OfferProcessingTask>) {
        val types = existingTasks.map { it.type to it.target }.distinct()
        types shouldContainExactlyInAnyOrder listOf(strategy.offerProcessingType to strategy.offerTarget)
    }

    /**
     * -------------(все категории)--------------------------
     * ------------/---------------\-------------------------
     * ----(одежда)-------------(мобильные устройства)-------
     * ----/-----\---------------/----------------\----------
     * -(тапки)(штаны)-------(телефоны)-------(аксессуары)---
     * -----------------------/------\----------/-----\------
     * ------------------(iphone)(android)--(чехлы)(зарядки)-
     */

    protected fun buildTestCategoryTree() {
        val rootCategoryId = CategoryTree.ROOT_CATEGORY_ID

        val fashionNodeCategory = Category()
            .setCategoryId(2)
            .setParentCategoryId(rootCategoryId)
            .setName("Одежда")
        val shoesCategory = Category()
            .setCategoryId(3)
            .setParentCategoryId(fashionNodeCategory.categoryId)
            .setName("Тапки")
            .setLeaf(true)
        val pantsCategory = Category()
            .setCategoryId(4)
            .setParentCategoryId(fashionNodeCategory.categoryId)
            .setName("Штаны")
            .setLeaf(true)

        val deviceNodeCategory = Category()
            .setCategoryId(5)
            .setParentCategoryId(rootCategoryId)
            .setName("Мобильные устройства")

        val cellphoneNodeCategory = Category()
            .setCategoryId(6)
            .setParentCategoryId(deviceNodeCategory.categoryId)
            .setName("Телефоны")
        val iphoneNodeCategory = Category()
            .setCategoryId(7)
            .setParentCategoryId(cellphoneNodeCategory.categoryId)
            .setName("IPhone")
            .setLeaf(true)
        val androidNodeCategory = Category()
            .setCategoryId(8)
            .setParentCategoryId(cellphoneNodeCategory.categoryId)
            .setName("Android")
            .setLeaf(true)

        val accessoryNodeCategory = Category()
            .setCategoryId(9)
            .setParentCategoryId(deviceNodeCategory.categoryId)
            .setName("Аксессуары")
        val coversNodeCategory = Category()
            .setCategoryId(10)
            .setParentCategoryId(accessoryNodeCategory.categoryId)
            .setName("Чехлы")
            .setLeaf(true)
        val chargesNodeCategory = Category()
            .setCategoryId(11)
            .setParentCategoryId(accessoryNodeCategory.categoryId)
            .setName("Зарядки")
            .setLeaf(true)

        categoryCachingServiceMock.addCategories(fashionNodeCategory, shoesCategory, pantsCategory,
            deviceNodeCategory, cellphoneNodeCategory, iphoneNodeCategory, androidNodeCategory, accessoryNodeCategory,
            chargesNodeCategory, coversNodeCategory
        )
    }
}
