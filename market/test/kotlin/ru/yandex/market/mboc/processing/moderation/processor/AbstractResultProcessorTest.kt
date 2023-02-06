package ru.yandex.market.mboc.processing.moderation.processor

import com.google.protobuf.StringValue
import com.nhaarman.mockitokotlin2.any
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.mbo.storage.StorageKeyValueService
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferProcessingType
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferTarget
import ru.yandex.market.mboc.common.offers.model.Offer
import ru.yandex.market.mboc.common.offers.model.OfferProcessingAssignment
import ru.yandex.market.mboc.processing.BaseOfferProcessingTest
import ru.yandex.market.mboc.processing.Markup3ApiService
import ru.yandex.market.mboc.processing.SaveResultInfo
import ru.yandex.market.mboc.processing.TaskResultMbocSaveStatus
import ru.yandex.market.mboc.processing.TaskResultProcessor
import ru.yandex.market.mboc.processing.TaskResultSaver
import ru.yandex.market.mboc.processing.assignment.OfferProcessingAssignmentRepository
import ru.yandex.market.mboc.processing.services.UniqueKeysConstructorService
import ru.yandex.market.mboc.processing.task.OfferProcessingTaskRepository

abstract class AbstractResultProcessorTest: BaseOfferProcessingTest() {
    @Autowired
    protected lateinit var keyValueService: StorageKeyValueService
    @Autowired
    protected lateinit var markup3ApiTaskService: Markup3ApiService
    @Autowired
    protected lateinit var uniqueKeysConstructorService: UniqueKeysConstructorService
    @Autowired
    protected lateinit var offerProcessingAssignmentRepository: OfferProcessingAssignmentRepository
    @Autowired
    protected lateinit var offerProcessingTaskRepository: OfferProcessingTaskRepository

    protected lateinit var taskResultSaver: TaskResultSaver
    protected lateinit var processor: TaskResultProcessor

    private var consumeRequestCaptor = com.nhaarman.mockitokotlin2.argumentCaptor<Markup3Api.ConsumeResultRequest>()
    private var markUniqueKeyCancelledRequestCaptor =
        com.nhaarman.mockitokotlin2.argumentCaptor<Markup3Api.MarkUniqueKeysCancelledRequest>()

    @Before
    fun setUp() {
        initMockedSaver()
        Mockito.doAnswer { invocation ->
            mapOf(
                TaskResultMbocSaveStatus.SUCCEED to listOf(
                    SaveResultInfo(
                        100L,
                        TaskResultMbocSaveStatus.SUCCEED,
                        taskId = 1L,
                        cancelledOffers = listOf(1,2,3)
                    )
                ),
                TaskResultMbocSaveStatus.SEMI_FAILED to listOf(
                    SaveResultInfo(
                        200L,
                        TaskResultMbocSaveStatus.SEMI_FAILED,
                        taskId = 2L,
                        cancelledOffers = listOf(4, 5, 6)
                    )
                ),
                TaskResultMbocSaveStatus.FAILED to listOf(
                    SaveResultInfo(
                        300L,
                        TaskResultMbocSaveStatus.FAILED,
                    taskId = 3L,
                    cancelledOffers = listOf(7, 8, 9)
                    )
                )
            )
        }.`when`(taskResultSaver).saveResults(any(), any())

        Mockito.doAnswer { invocation ->
            Markup3Api.TasksResultPollResponse.newBuilder().apply {
                for (i in 1..3) {
                    val result = Markup3Api.TasksResultPollResponse.TaskResult.newBuilder().apply {
                        taskid = i.toLong()
                        taskResultId = (i * 100).toLong()
                        externalKey = StringValue.of(i.toString())
                        result = whenPollResultsDoAnswer()
                    }.build()
                    addResults(result)
                }
            }.build()
        }.`when`(markup3ApiTaskService).pollResults(any())

        Mockito.doAnswer { invocation ->
            Markup3Api.ConsumeResultResponse.newBuilder().build()
        }.`when`(markup3ApiTaskService).consumeResults(consumeRequestCaptor.capture())

        Mockito.doAnswer{ invocation ->
            Markup3Api.MarkUniqueKeysCancelledResponse.newBuilder().build()
        }.`when`(markup3ApiTaskService).markUniqKeysCancelled(markUniqueKeyCancelledRequestCaptor.capture())

        initProcessor()
        generateOfferProcessingAssignments(3, 1L)
    }

    @Test
    fun `should consume only successfully saved results`() {
        processor.process()

        consumeRequestCaptor.allValues[0].taskResultIdsList shouldContainExactlyInAnyOrder listOf(100L)
    }

    @Test
    fun `should mark cancelled only unique keys of cancelled tasks`() {
        processor.process()

        markUniqueKeyCancelledRequestCaptor.allValues[0].requestItemsList shouldHaveSize 1
        markUniqueKeyCancelledRequestCaptor.allValues[0].requestItemsList shouldContainExactlyInAnyOrder
            listOf(Markup3Api.MarkUniqueKeysCancelledRequest.MarkUniqueKeysCancelledRequestItem.newBuilder().apply {
                taskId = 1L
                addAllUniqKeys(listOf("1.1", "2.1", "3.1"))
            }.build())
    }

    abstract fun initProcessor()

    abstract fun initMockedSaver()

    abstract fun whenPollResultsDoAnswer(): Markup3Api.TaskResultData

    private fun generateOfferProcessingAssignments(number: Int, taskId: Long = 2L): List<OfferProcessingAssignment> {
        return (1..number).map {
            OfferProcessingAssignment.builder().apply {
                offerId(it.toLong())
                targetSkuId(it * 100L)
                skuType(Offer.SkuType.MARKET)
                target(OfferTarget.YANG)
                type(OfferProcessingType.IN_MODERATION)
                processingTicketId(number)
                processingCounter(1)
            }.build()
        }.toList().also {
            offerProcessingAssignmentRepository.insertBatch(it)
        }
    }

}
