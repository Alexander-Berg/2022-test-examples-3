package ru.yandex.market.markup3.mboc.moderation.saver

import com.fasterxml.jackson.databind.node.TextNode
import com.google.protobuf.Int64Value
import com.google.protobuf.StringValue
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3Api.YangMappingModerationResult
import ru.yandex.market.markup3.core.dto.ProcessingStatus
import ru.yandex.market.markup3.core.dto.Task
import ru.yandex.market.markup3.core.dto.TaskRow
import ru.yandex.market.markup3.core.services.TaskService
import ru.yandex.market.markup3.mboc.moderation.MbocMappingModerationConstants
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTask
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskStatus
import ru.yandex.market.markup3.mboc.offertask.repository.OfferTaskRepository
import ru.yandex.market.markup3.mboc.offertask.service.OfferTaskService
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.tasks.mapping_moderation.ModerationTaskType
import ru.yandex.market.markup3.tasks.mapping_moderation.dto.MappingModerationTaskResult
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.dto.YangMappingModerationInput
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.dto.YangMappingModerationInputData
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.dto.YangMappingModerationInputDataOffer
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.dto.YangMappingModerationState
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper
import ru.yandex.market.mboc.http.MboCategory
import ru.yandex.market.mboc.http.MboCategoryService
import ru.yandex.market.mboc.http.SupplierOffer
import ru.yandex.market.mboc.http.SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult
import java.time.Instant

class MappingModerationSaverTest : CommonTaskTest() {

    @Autowired
    private lateinit var offerTaskRepository: OfferTaskRepository

    @Autowired
    private lateinit var offerTaskService: OfferTaskService

    private val mboc = mock<MboCategoryService>()

    lateinit var taskServiceSpy: TaskService
    lateinit var saver: MappingModerationSaver

    @Before
    fun setUp() {
        taskServiceSpy = spy(taskService)
        saver = MappingModerationSaver(
            taskServiceSpy,
            mboc,
            offerTaskService,
            TransactionHelper.MOCK,
        )
    }

    @Test
    fun `Saves result with correctly built request`() {
        doReturn(createTask())
            .`when`(taskServiceSpy)
            .readTask<YangMappingModerationInput, YangMappingModerationState, MappingModerationTaskResult>(1)
        doReturn(
            MboCategory.SaveMappingModerationResponse.newBuilder()
                .setResult(
                    SupplierOffer.OperationResult.newBuilder()
                        .setStatus(SupplierOffer.OperationStatus.SUCCESS)
                        .build()
                )
                .build()
        ).`when`(mboc).saveMappingsModeration(any())

        offerTaskRepository.insert(
            OfferTask(
                TaskType.YANG_MAPPING_MODERATION,
                groupKey = MbocMappingModerationConstants.YANG_GROUP_KEY,
                1,
                OfferTaskStatus.WAITING_FOR_RESULTS,
                1,
                1,
                null
            )
        )

        val response = saver.saveResults(
            MbocMappingModerationConstants.YANG_GROUP_KEY,
            listOf(
                MappingModerationSaver.ResultToSave(
                    resultId = 1,
                    taskId = 1,
                    data = YangMappingModerationResult.newBuilder().apply {
                        staffLogin = "dashie"
                        workerId = "wid001"
                        addResults(YangMappingModerationResult.MappingModerationResultItem.newBuilder().apply {
                            msku = Int64Value.of(4)
                            status = YangMappingModerationResult.MappingModerationStatus.ACCEPTED
                            offerId = "5"
                            addContentComment(YangMappingModerationResult.YangContentComment.newBuilder().apply {
                                type = StringValue.of("test")
                                addItems("test")
                            })
                            skuModifiedTs = Int64Value.of(6)
                        })
                    }.build()
                )
            )
        )
        response.saved shouldHaveSize 1
        response.failed shouldHaveSize 0

        val rqCaptor = argumentCaptor<MboCategory.SaveMappingsModerationRequest>()
        verify(mboc, times(1)).saveMappingsModeration(rqCaptor.capture())
        val request = rqCaptor.lastValue
        request.resultsCount shouldBe 1
        val result = request.resultsList.first()
        result.offerId shouldBe "5"
        result.supplierId shouldBe 0
        result.shopSkuId shouldBe ""
        result.marketSkuId shouldBe 4
        result.status shouldBe SupplierMappingModerationResult.ACCEPTED
        result.staffLogin shouldBe "dashie"
        result.contentCommentCount shouldBe 1
        val contentComment = result.contentCommentList.first()
        contentComment.type shouldBe "test"
        contentComment.itemsList shouldContainExactly listOf("test")
        result.taskId shouldBe 1
        result.skuModifiedTs shouldBe 6
    }

    @Test
    fun `Handles errors on sending`() {
        doReturn(createTask())
            .`when`(taskServiceSpy)
            .readTask<YangMappingModerationInput, YangMappingModerationState, MappingModerationTaskResult>(1)
        doReturn(
            MboCategory.SaveMappingModerationResponse.newBuilder()
                .setResult(
                    SupplierOffer.OperationResult.newBuilder()
                        .setStatus(SupplierOffer.OperationStatus.ERROR)
                        .setMessage("error msg")
                        .build()
                )
                .build()
        ).`when`(mboc).saveMappingsModeration(any())

        offerTaskRepository.insert(
            OfferTask(
                TaskType.YANG_MAPPING_MODERATION,
                groupKey = MbocMappingModerationConstants.YANG_GROUP_KEY,
                1,
                OfferTaskStatus.WAITING_FOR_RESULTS,
                1,
                1,
                null
            )
        )

        val response = saver.saveResults(
            MbocMappingModerationConstants.YANG_GROUP_KEY,
            listOf(
                MappingModerationSaver.ResultToSave(
                    resultId = 1,
                    taskId = 1,
                    data = YangMappingModerationResult.newBuilder().apply {
                        staffLogin = "sunnie"
                        workerId = "wid001"
                        addResults(YangMappingModerationResult.MappingModerationResultItem.newBuilder().apply {
                            msku = Int64Value.of(4)
                            status = YangMappingModerationResult.MappingModerationStatus.ACCEPTED
                            offerId = "5"
                            skuModifiedTs = Int64Value.of(6)
                        })
                    }.build()
                )
            )
        )
        response.saved shouldHaveSize 0
        response.failed[1] shouldContain "Error on sending task result"

        verify(mboc, times(1)).saveMappingsModeration(any())
    }

    private fun createTask(): Task<YangMappingModerationInput, YangMappingModerationState> {
        return Task(
            taskRow = TaskRow(
                id = 1,
                externalKey = null,
                taskGroupId = 2,
                processingStatus = ProcessingStatus.DONE,
                stage = "success",
                state = TextNode("state"),
                lastActive = Instant.now(),
            ),
            state = YangMappingModerationState(tolokaTaskId = 8),
            input = YangMappingModerationInput(
                data = YangMappingModerationInputData(
                    offers = listOf(
                        YangMappingModerationInputDataOffer(
                            id = 3,
                            offerId = "5",
                            categoryId = 9,
                            categoryName = "categoryName"
                        )
                    ),
                    taskType = ModerationTaskType.MAPPING_MODERATION,
                    taskSubtype = null,
                ),
                categoryId = 9,
            )
        )
    }
}
