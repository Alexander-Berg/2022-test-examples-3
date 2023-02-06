package ru.yandex.market.mboc.processing.task

import com.nhaarman.mockitokotlin2.any
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.mbo.storage.StorageKeyValueService
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferProcessingTaskStatus
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferProcessingType
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferTarget
import ru.yandex.market.mboc.processing.BaseOfferProcessingTest
import ru.yandex.market.mboc.processing.Markup3ApiService
import java.time.LocalDateTime

class CancelledOfferProcessingTaskCleanerTest : BaseOfferProcessingTest() {

    @Autowired
    private lateinit var offerProcessingTaskRepository: OfferProcessingTaskRepository
    @Autowired
    private lateinit var markup3ApiServiceMock: Markup3ApiService
    @Autowired
    private lateinit var skv: StorageKeyValueService

    private lateinit var taskCleaner: CancelledOfferProcessingTaskCleaner

    @Before
    open fun setUp() {
        Mockito.doAnswer { invocation ->
            Markup3Api.GetTasksStatusResponse.newBuilder().apply {
                val request = invocation.arguments[0] as Markup3Api.GetTasksStatusRequest
                request.requestItemList.associate {
                    when {
                        it <= 8 -> it to Markup3Api.GetTasksStatusResponse.TaskProcessingStatus.ACTIVE
                        it in 9..10 -> it to Markup3Api.GetTasksStatusResponse.TaskProcessingStatus.DONE
                        else -> it to Markup3Api.GetTasksStatusResponse.TaskProcessingStatus.CANCELLED
                    }
                }.let{ this.putAllTaskStatus(it) }
            }.build()
        }.`when`(markup3ApiServiceMock).getTasksStatuses(any())

        taskCleaner = CancelledOfferProcessingTaskCleaner(
            offerProcessingTaskRepository,
            markup3ApiServiceMock,
            skv
        )
    }

    @Test
    open fun `should not clean when empty cancelled markup tasks`() {
        val tasks = generateTasks(10)
        taskCleaner.cleanCancelledTasks()

        offerProcessingTaskRepository.findAll() shouldContainExactlyInAnyOrder tasks
    }

    @Test
    open fun `should correctly clean cancelled markup tasks`() {
        val tasks = generateTasks(20)
        taskCleaner.cleanCancelledTasks()

        offerProcessingTaskRepository.findAll() shouldContainExactlyInAnyOrder tasks.take(10)
    }

    @Test
    open fun `cleaner should correctly process several batches`() {
        skv.putValue("cancelledTaskCleaner.batchSize", 3)
        val tasks = generateTasks()
        taskCleaner.cleanCancelledTasks()

        offerProcessingTaskRepository.findAll() shouldContainExactlyInAnyOrder tasks.take(10)
    }

    private fun generateTasks(to: Int = 20) =
        (1..to).map {
            OfferProcessingTask(
                it.toLong(),
                OfferProcessingType.IN_PROCESS,
                OfferTarget.TOLOKA,
                LocalDateTime.now(),
                1,
                100,
                it.toLong(),
                OfferProcessingTaskStatus.ACTIVE
            )
        }.let{ offerProcessingTaskRepository.insertOrUpdateAll(it) }

}
