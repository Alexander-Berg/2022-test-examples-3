package ru.yandex.market.mboc.processing.moderation.saver

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3Api.TasksResultPollResponse
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferProcessingTaskStatus
import ru.yandex.market.mboc.processing.AbstractTaskResultSaver
import ru.yandex.market.mboc.processing.BaseOfferProcessingTest
import ru.yandex.market.mboc.processing.ProcessingStrategyType
import ru.yandex.market.mboc.processing.SaveResultInfo
import ru.yandex.market.mboc.processing.SaverMode
import ru.yandex.market.mboc.processing.TaskId
import ru.yandex.market.mboc.processing.TaskResultId
import ru.yandex.market.mboc.processing.TaskResultMbocSaveStatus
import ru.yandex.market.mboc.processing.task.OfferProcessingTask
import ru.yandex.market.mboc.processing.task.OfferProcessingTaskRepository
import java.time.LocalDateTime

abstract class AbstractSaverTest<TaskType>(
    strategy: ProcessingStrategyType
) : BaseOfferProcessingTest() {

    protected val offerProcessingType = strategy.offerProcessingType
    protected val target = strategy.offerTarget

    @Autowired
    protected lateinit var offerProcessingTaskRepository: OfferProcessingTaskRepository

    protected lateinit var saver: AbstractTaskResultSaver<TaskType>

    protected var cancelledOffersDeleted: Boolean = true

    @Before
    open fun setUp() {
        offerProcessingTaskRepository.deleteAll()
    }

    @Test
    open fun `save several results when response has succeed status`() {
        // 20 success taskResults
        generateSucceedSaver()
        var processingTasks = generateActiveOfferProcessingTasks(20)
        var saveResult = saver.saveResults(
            buildResultsToSaveList(
                processingTasks,
                List(20) { it.toLong() }),
            SaverMode.SAVE_ONLY_EXISTING
        )

        saveResult[TaskResultMbocSaveStatus.SUCCEED]!! shouldHaveSize processingTasks.size
        offerProcessingTaskRepository.findAll().filter { it.status == OfferProcessingTaskStatus.CONSUMED } shouldHaveSize 20
        offerProcessingTaskRepository.deleteConsumedByTaskIds(listOf(2))

        // 10 success taskResults + 3 canceled tasks
        processingTasks = generateActiveOfferProcessingTasks(13)
        saveResult = saver.saveResults(
            buildResultsToSaveList(
                processingTasks.take(10),
                List(20) { it.toLong() },
                listOf(11L, 12L, 13L)
            ),
            SaverMode.SAVE_ONLY_EXISTING
        )

        saveResult[TaskResultMbocSaveStatus.SUCCEED]!! shouldHaveSize 10
        offerProcessingTaskRepository.findNonConsumed() shouldHaveSize if (cancelledOffersDeleted) 0 else 3
        if (cancelledOffersDeleted) {
            val cancelledOffersList = (1..10).map { 2L to listOf(11L, 12L, 13L) }
            saveResult[TaskResultMbocSaveStatus.SUCCEED]!!.map {
                it.taskId to it.cancelledOffers
            } shouldContainExactlyInAnyOrder cancelledOffersList
        }

        // polled only 10 success results
        processingTasks = generateActiveOfferProcessingTasks(13)
        saver.saveResults(
            buildResultsToSaveList(
                processingTasks.take(10),
                List(20) { it.toLong() }
            ),
            SaverMode.SAVE_ONLY_EXISTING
        )

        saveResult[TaskResultMbocSaveStatus.SUCCEED]!! shouldHaveSize 10
        offerProcessingTaskRepository.findNonConsumed() shouldContainExactlyInAnyOrder processingTasks.takeLast(3)
    }

    @Test
    open fun `save one result when response has succeed status`() {
        generateSucceedSaver()
        var processingTasks = generateActiveOfferProcessingTasks(20)
        var saveResult = saver.saveResults(
            listOf(buildOneResultsToSaveList(processingTasks, 666L, 2L)),
            SaverMode.SAVE_ONLY_EXISTING
        )

        saveResult[TaskResultMbocSaveStatus.SUCCEED]!! shouldHaveSize 1
        offerProcessingTaskRepository.findAll().filter { it.status == OfferProcessingTaskStatus.CONSUMED } shouldHaveSize 20

        processingTasks = generateActiveOfferProcessingTasks(13)
        offerProcessingTaskRepository.deleteConsumedByTaskIds(listOf(2))
        saveResult =
            saver.saveResults(
                listOf(
                    buildOneResultsToSaveList(
                        processingTasks.take(10), 666L, 2L,
                        listOf(11L, 12L, 13L)
                    )
                ),
                SaverMode.SAVE_ONLY_EXISTING
            )

        saveResult[TaskResultMbocSaveStatus.SUCCEED]!! shouldHaveSize 1
        offerProcessingTaskRepository.findNonConsumed() shouldHaveSize if (cancelledOffersDeleted) 0 else 3
        if (cancelledOffersDeleted) {
            saveResult[TaskResultMbocSaveStatus.SUCCEED]!!.map {
                it.taskId to it.cancelledOffers
            } shouldContainExactlyInAnyOrder listOf((2L to listOf(11L, 12L, 13L)))
        }

        processingTasks = generateActiveOfferProcessingTasks(13)
        saveResult =
            saver.saveResults(
                listOf(buildOneResultsToSaveList(processingTasks.take(10), 666L, 2L)),
                SaverMode.SAVE_ONLY_EXISTING
            )

        saveResult[TaskResultMbocSaveStatus.SUCCEED]!! shouldHaveSize 1
        offerProcessingTaskRepository.findNonConsumed() shouldContainExactlyInAnyOrder processingTasks.takeLast(3)
    }

    @Test
    open fun `don't delete failed to save`() {
        generateFailedSaver()
        val processingTasks = generateActiveOfferProcessingTasks(20)
        val saveResult = saver.saveResults(
            buildResultsToSaveList(
                processingTasks,
                List(20) { it.toLong() }),
            SaverMode.SAVE_ONLY_EXISTING
        )

        offerProcessingTaskRepository.findAll() shouldContainExactlyInAnyOrder processingTasks
        saveResult[TaskResultMbocSaveStatus.FAILED]!! shouldHaveSize processingTasks.size
    }

    @Test
    open fun `should save all polled task results`() {
        generateSucceedSaver()
        val processingTasks = generateActiveOfferProcessingTasks(20, 2L)
        val saveResult = saver.saveResults(
            listOf(
                buildOneResultsToSaveList(processingTasks, 666L, 13L),
                buildOneResultsToSaveList(processingTasks.take(10), 777L, 2L)
            ),
            SaverMode.SAVE_ONLY_EXISTING
        )

        val expectedTasks = processingTasks.takeLast(10)

        offerProcessingTaskRepository.findNonConsumed() shouldHaveSize 0
        saveResult.containsKey(TaskResultMbocSaveStatus.SEMI_FAILED) shouldBeEqualComparingTo false
        saveResult.containsKey(TaskResultMbocSaveStatus.FAILED) shouldBeEqualComparingTo false
        saveResult[TaskResultMbocSaveStatus.SUCCEED]!! shouldHaveSize 2
    }

    @Test
    open fun `when empty shouldnot fail`() {
        generateSucceedSaver()
        saver.saveResults(listOf(), SaverMode.SAVE_ALL)
    }

    @Test
    open fun `should save all results in save_all mode`() {
        generateSucceedSaver()
        val processingTasks = generateCancellingOfferProcessingTasks(20, 2L)

        val saveResult = saver.saveResults(
            listOf(buildOneResultsToSaveList(processingTasks, 777L, 2L)),
            SaverMode.SAVE_ALL
        )

        offerProcessingTaskRepository.findAll() shouldContainExactlyInAnyOrder
            processingTasks.map { it.copy(status = OfferProcessingTaskStatus.CONSUMED) }
        saveResult[TaskResultMbocSaveStatus.SEMI_FAILED] shouldBe null
        saveResult[TaskResultMbocSaveStatus.FAILED] shouldBe null
        saveResult[TaskResultMbocSaveStatus.SUCCEED] shouldContainExactly
            listOf(SaveResultInfo(777, TaskResultMbocSaveStatus.SUCCEED, null, 2))
        saveResult[TaskResultMbocSaveStatus.NOOP] shouldBe null
    }

    abstract fun generateSucceedSaver()

    abstract fun generateFailedSaver()

    protected fun generateActiveOfferProcessingTasks(number: Int, taskId: Long = 2L): List<OfferProcessingTask> {
        return generateProcessingTasksWithStatus(number, taskId, OfferProcessingTaskStatus.ACTIVE)
    }

    protected fun generateCancellingOfferProcessingTasks(number: Int, taskId: Long = 2L): List<OfferProcessingTask> {
        return generateProcessingTasksWithStatus(number, taskId, OfferProcessingTaskStatus.CANCELLING)
    }

    private fun generateProcessingTasksWithStatus(
        number: Int, taskId: Long = 2L, status: OfferProcessingTaskStatus
    ): List<OfferProcessingTask> {
        val result: MutableList<OfferProcessingTask> = mutableListOf()
        for (i in 1..number) {
            OfferProcessingTask(
                i.toLong(),
                offerProcessingType,
                target,
                LocalDateTime.now(),
                1,
                100,
                taskId,
                status
            ).also { result.add(it) }
        }
        offerProcessingTaskRepository.insertOrUpdateAll(result)
        return result
    }

    abstract fun buildResultsToSaveList(
        offerProcessingTasks: List<OfferProcessingTask>,
        taskResultIds: List<TaskResultId>,
        cancelledOffers: List<Long> = listOf(),
        mskuId: Long? = 235
    ): List<TasksResultPollResponse.TaskResult>

    abstract fun buildOneResultsToSaveList(
        offerProcessingTasks: List<OfferProcessingTask>,
        taskResultId: TaskResultId,
        taskId: TaskId,
        cancelledOffers: List<Long> = listOf(),
        mskuId: Long? = 235
    ): TasksResultPollResponse.TaskResult
}
