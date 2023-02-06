package ru.yandex.market.mboc.processing.task

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.reset
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferProcessingTaskStatus
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferProcessingType
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferTarget
import ru.yandex.market.mboc.common.offers.model.Offer
import ru.yandex.market.mboc.common.offers.model.OfferProcessingAssignment
import ru.yandex.market.mboc.processing.BaseOfferProcessingTest
import ru.yandex.market.mboc.processing.Markup3ApiService
import ru.yandex.market.mboc.processing.assignment.OfferProcessingAssignmentRepository
import java.time.LocalDate
import java.time.LocalDateTime

class OfferProcessingTasksWithoutAssignmentsWatcherTest : BaseOfferProcessingTest() {
    @Autowired
    private lateinit var assignmentRepository: OfferProcessingAssignmentRepository

    @Autowired
    private lateinit var taskRepository: OfferProcessingTaskRepository

    @Autowired
    private lateinit var watcher: OfferProcessingTasksWithoutAssignmentsWatcher

    @Autowired
    private lateinit var markup3ApiServiceMock: Markup3ApiService

    @Before
    fun setUp() {
        reset(markup3ApiServiceMock)
    }

    @Test
    fun `Cancels tasks even if one batch fails`() {
        val opa = LongRange(301, 350).map {
            OfferProcessingAssignment.builder()
                .offerId(it)
                .businessId(1)
                .categoryId(2)
                .processingStatus(Offer.ProcessingStatus.IN_CLASSIFICATION)
                .type(OfferProcessingType.IN_CLASSIFICATION)
                .target(OfferTarget.YANG)
                .processingTicketId(it.toInt())
                .ticketDeadline(LocalDate.now())
                .ticketCritical(false)
                .modelId(1)
                .vendorId(2)
                .hideFromToloka(false)
                .trackerTicket(it.toString())
                .priority(it.toInt())
                .processingCounter(1)
                .build()
        }
        assignmentRepository.insertBatch(opa)
        val opt = LongRange(1, 350).map {
            OfferProcessingTask(
                offerId = it,
                type = OfferProcessingType.IN_CLASSIFICATION,
                target = OfferTarget.YANG,
                created = LocalDateTime.now(),
                processingTicketId = it.toInt(),
                taskPriority = it.toInt(),
                taskId = it,
                status = OfferProcessingTaskStatus.ACTIVE
            )
        }
        taskRepository.insertBatch(opt)

        doReturn(
            Markup3Api.CancelTasksResponse.newBuilder().setCancelInitiated(true).build(),
            Markup3Api.CancelTasksResponse.newBuilder().setCancelInitiated(false).build(),
            Markup3Api.CancelTasksResponse.newBuilder().setCancelInitiated(true).build()
        ).`when`(markup3ApiServiceMock).cancelTasks(any())

        shouldThrow<IllegalStateException> {
            watcher.cancelTasksWithoutAssignments()
        }

        taskRepository.findAll().groupingBy { it.status }.eachCount().let {
            it[OfferProcessingTaskStatus.ACTIVE] shouldBe 150
            it[OfferProcessingTaskStatus.CANCELLING] shouldBe 200
        }
    }
}
