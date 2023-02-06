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

class OfferProcessingTaskPriorityUpdaterTest : BaseOfferProcessingTest() {
    @Autowired
    private lateinit var assignmentRepository: OfferProcessingAssignmentRepository

    @Autowired
    private lateinit var taskRepository: OfferProcessingTaskRepository

    @Autowired
    private lateinit var markup3ApiServiceMock: Markup3ApiService

    @Autowired
    private lateinit var updater: OfferProcessingTaskPriorityUpdater

    @Before
    fun setUp() {
        reset(markup3ApiServiceMock)
    }

    @Test
    fun `Updates priorities even if one batch fails`() {
        val opa = LongRange(1, 350).map {
            OfferProcessingAssignment.builder()
                .offerId(it)
                .businessId(1)
                .categoryId(2)
                .processingStatus(Offer.ProcessingStatus.IN_PROCESS)
                .type(OfferProcessingType.IN_PROCESS)
                .target(OfferTarget.YANG)
                .processingTicketId(it.toInt())
                .ticketDeadline(LocalDate.now())
                .ticketCritical(false)
                .modelId(1)
                .vendorId(2)
                .hideFromToloka(false)
                .trackerTicket(it.toString())
                .priority(
                    when {
                        it <= 300 -> it.toInt() + 1000
                        else -> it.toInt()
                    }
                )
                .processingCounter(1)
                .build()
        }
        assignmentRepository.insertBatch(opa)
        val opt = LongRange(1, 350).map {
            OfferProcessingTask(
                offerId = it,
                type = OfferProcessingType.IN_PROCESS,
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
            Markup3Api.UpdateTasksPrioritiesResponse.newBuilder().setUpdateInitiated(true).build(),
            Markup3Api.UpdateTasksPrioritiesResponse.newBuilder().setUpdateInitiated(false).build(),
            Markup3Api.UpdateTasksPrioritiesResponse.newBuilder().setUpdateInitiated(true).build()
        ).`when`(markup3ApiServiceMock).updateTasksPriorities(any())

        shouldThrow<IllegalStateException> {
            updater.updateTaskPriorities()
        }

        taskRepository.findAll().groupingBy { it.taskPriority >= 1000 }.eachCount().let {
            it[false] shouldBe 150
            it[true] shouldBe 200
        }
    }
}
