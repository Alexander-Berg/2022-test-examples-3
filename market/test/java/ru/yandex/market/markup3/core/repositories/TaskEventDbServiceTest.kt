package ru.yandex.market.markup3.core.repositories

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.Test
import ru.yandex.market.markup3.core.EventId
import ru.yandex.market.markup3.core.TaskId
import ru.yandex.market.markup3.core.dto.ProcessingStatus
import ru.yandex.market.markup3.core.dto.TaskEventRow
import ru.yandex.market.markup3.core.dto.TaskEventStatus
import ru.yandex.market.markup3.testutils.CommonTaskTest
import java.time.Instant

class TaskEventDbServiceTest : CommonTaskTest() {
    @Test
    fun `Test delete old events`() {
        val taskGroup = createTestTaskGroup()
        val testTaskActive = createTestTask(taskGroup.id)
        val testTaskDone = createTestTask(taskGroup.id)
        taskDbService.updateTask(taskDbService.findById(testTaskDone).copy(processingStatus = ProcessingStatus.DONE))


        val date = Instant.ofEpochSecond(1639040146)
        val beforeDate = date.minusSeconds(1)
        val afterDate = date.plusSeconds(1)

        fun createEvent(taskId: TaskId, status: TaskEventStatus, lastRun: Instant): EventId {
            return taskEventDbService.insertEvents(
                listOf(
                    TaskEventRow(
                        taskGroupId = taskGroup.id,
                        taskId = taskId,
                        eventType = "test",
                        status = status,
                        lastRun = lastRun
                    )
                )
            ).map { it.id }.first()
        }

        // Active
        val active = createEvent(testTaskDone, TaskEventStatus.ACTIVE, beforeDate)
        // Failed
        val failed = createEvent(testTaskDone, TaskEventStatus.FAILED, beforeDate)
        // Processed but not old enough
        val processedNew = createEvent(testTaskDone, TaskEventStatus.PROCESSED, afterDate)
        // Processed and old - won't be deleted bc of limit
        val processedOld = createEvent(testTaskDone, TaskEventStatus.PROCESSED, beforeDate)
        // Processed and even older
        val processedOlder = createEvent(testTaskDone, TaskEventStatus.PROCESSED, beforeDate.minusSeconds(1))
        // Processed and the oldest but on active task
        val processedOldestOnActiveTask = createEvent(
            testTaskActive, TaskEventStatus.PROCESSED,
            beforeDate.minusSeconds(10)
        )

        taskEventDbService.deleteProcessedEventsOlderThan(date, 1)

        taskEventDbService.findAll()
            .filter { it.eventType == "test" }
            .map { it.id } shouldContainExactlyInAnyOrder
            listOf(active, failed, processedNew, processedOld, processedOldestOnActiveTask)
    }
}
