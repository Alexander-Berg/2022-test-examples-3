package ru.yandex.market.markup3.core.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.Test
import ru.yandex.market.markup3.core.events.Event
import ru.yandex.market.markup3.testutils.CommonTaskTest

class TaskEventServiceTest : CommonTaskTest() {
    @Test
    fun `it should fail sending messages to not existing tasks`() {
        shouldThrow<IllegalArgumentException> {
            taskEventService.sendEvents(
                listOf(SendEvent(taskId = 42, TestEventObject))
            )
        }
    }

    @Test
    fun `it should create messages`() {
        val taskId = createTestTask(createTestTaskGroup().id)
        taskEventService.sendEvents(
            SendEvent(taskId, TestEvent("Hey there")),
            SendEvent(taskId, TestEventObject),
        )

        // 2 + Init
        taskEventDbService.findAll() shouldHaveSize 3
        taskEventDbService.findAllData() shouldHaveSize 3
        // TODO: check for data/reading events
    }
}

class TestEvent(val data: String) : Event()
object TestEventObject : Event()
