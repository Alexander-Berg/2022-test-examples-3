package ru.yandex.market.markup3.mboc.blueclassification.saver

import com.google.protobuf.Int64Value
import com.google.protobuf.StringValue
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
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
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.markup3.api.Markup3Api.BlueClassificationResult.BlueClassificationResultItem
import ru.yandex.market.markup3.mboc.blueclassification.MbocBlueClassificationConstants
import ru.yandex.market.markup3.mboc.blueclassification.saver.BlueClassificationSaver.ResultToSave
import ru.yandex.market.markup3.mboc.bluelogs.MbocBlueLogsConstants
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTask
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskStatus.WAITING_FOR_RESULTS
import ru.yandex.market.markup3.mboc.offertask.repository.OfferTaskRepository
import ru.yandex.market.markup3.mboc.offertask.service.OfferTaskService
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.testutils.BaseAppTest
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper
import ru.yandex.market.mboc.http.MboCategory.UpdateSupplierOfferCategoryRequest
import ru.yandex.market.mboc.http.MboCategory.UpdateSupplierOfferCategoryResponse
import ru.yandex.market.mboc.http.MboCategoryService
import ru.yandex.market.mboc.http.SupplierOffer
import kotlin.properties.Delegates

class BlueClassificationSaverTest : BaseAppTest() {
    @Autowired
    private lateinit var offerTaskRepository: OfferTaskRepository

    @Autowired
    private lateinit var offerTaskService: OfferTaskService

    private var mboc by Delegates.notNull<MboCategoryService>()

    private var saver by Delegates.notNull<BlueClassificationSaver>()

    companion object {
        const val STAFF_LOGIN = "staff"
    }

    @Before
    fun setup() {
        mboc = mock()
        saver = BlueClassificationSaver(
            mboc,
            offerTaskService,
            TransactionHelper.MOCK,
        )
    }

    @Test
    fun `Saves result with correctly built request`() {
        doReturn(
            UpdateSupplierOfferCategoryResponse.newBuilder().setResult(
                SupplierOffer.OperationResult.newBuilder().setStatus(SupplierOffer.OperationStatus.SUCCESS).build()
            ).build()
        ).`when`(mboc).updateSupplierOfferCategory(any())

        offerTaskRepository.insert(
            OfferTask(
                TaskType.BLUE_CLASSIFICATION,
                groupKey = MbocBlueLogsConstants.GROUP_KEY,
                1,
                WAITING_FOR_RESULTS,
                1,
                1,
                null
            )
        )

        val response = saver.saveResults(
            listOf(
                ResultToSave(
                    1, 1, Markup3Api.BlueClassificationResult.newBuilder().apply {
                        staffLogin = STAFF_LOGIN
                        addOutput(
                            BlueClassificationResultItem.newBuilder().apply {
                                offerId = "1"
                                workerId = StringValue.of("1")
                                fixedCategoryId = Int64Value.of(10)
                                addContentComment(Markup3Api.Comment.newBuilder().apply {
                                    type = StringValue.of("test")
                                    addItems("test")
                                })
                            }.build()
                        )
                    }.build()
                )
            )
        )
        response.saved shouldHaveSize 1
        response.failed shouldHaveSize 0

        val rqCaptor = argumentCaptor<UpdateSupplierOfferCategoryRequest>()
        verify(mboc, times(1)).updateSupplierOfferCategory(rqCaptor.capture())
        val request = rqCaptor.lastValue
        request.resultCount shouldBe 1
        val result = request.resultList.first()
        result.staffLogin shouldBe STAFF_LOGIN
        result.offerId shouldBe "1"
        result.supplierId shouldBe 0
        result.fixedCategoryId shouldBe 10
        result.contentCommentCount shouldBe 1
        val contentComment = result.contentCommentList.first()
        contentComment.type shouldBe "test"
        contentComment.itemsList shouldContainExactly listOf("test")
    }

    @Test
    fun `Handles errors on sending`() {
        doReturn(
            UpdateSupplierOfferCategoryResponse.newBuilder().setResult(
                SupplierOffer.OperationResult.newBuilder().setStatus(SupplierOffer.OperationStatus.ERROR)
                    .setMessage("error msg").build()
            ).build()
        ).`when`(mboc).updateSupplierOfferCategory(any())

        offerTaskRepository.insert(
            OfferTask(
                TaskType.BLUE_CLASSIFICATION,
                groupKey = MbocBlueClassificationConstants.GROUP_KEY,
                1,
                WAITING_FOR_RESULTS,
                1,
                1,
                null
            )
        )

        val response = saver.saveResults(
            listOf(
                ResultToSave(
                    1, 1, Markup3Api.BlueClassificationResult.newBuilder().apply {
                        staffLogin = STAFF_LOGIN
                        addOutput(
                            BlueClassificationResultItem.newBuilder().apply {
                                offerId = "1"
                                workerId = StringValue.of("1")
                                fixedCategoryId = Int64Value.of(10)
                            }.build()
                        )
                    }.build()
                )
            )
        )
        response.saved shouldHaveSize 0
        response.failed[1] shouldContain "Error on sending classification result"

        verify(mboc, times(1)).updateSupplierOfferCategory(any())
    }
}
