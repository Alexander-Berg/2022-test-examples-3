package ru.yandex.market.markup3.core.executor

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.markup3.core.dto.TaskGroup
import ru.yandex.market.markup3.core.dto.TaskGroupConfig
import ru.yandex.market.markup3.core.executor.TaskGroupExecutorProvider.Companion.DEFAULT_SLEEP_TIME_MS
import ru.yandex.market.markup3.core.services.TaskGroupRegistry
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.utils.CommonObjectMapper

class TaskGroupExecutorManagerTest : CommonTaskTest() {
    @Test
    fun `should not process different owners`() {
        val taskGroupExecutorProvider = mock<TaskGroupExecutorProvider> {
            doReturn(mock<TaskGroupExecutor>()).`when`(it).createTaskGroupExecutor(any(), eq(DEFAULT_SLEEP_TIME_MS))
        }
        val taskGroupExecutorManager = TaskGroupExecutorManager(
            taskGroupRegistry,
            taskGroupExecutorProvider
        )
        taskGroupExecutorManager.start()

        taskGroupExecutorManager.getTaskGroupIdsWithExecutors() shouldContainExactlyInAnyOrder
            taskGroupRepository.findAllForOwner().map { it.id }

        val taskType = testHandler.handle.taskType
        val defaultGroup = createTestTaskGroup()
        val otherGroup = taskGroupRegistry.getOrCreateTaskGroup("otherGroup") {
            TaskGroup(key = "otherGroup", name = "other", owner = "other owner", taskType = taskType)
        }
        taskGroupRegistry.refresh()
        taskGroupExecutorManager.getTaskGroupIdsWithExecutors() shouldContain defaultGroup.id
        taskGroupExecutorManager.getTaskGroupIdsWithExecutors() shouldNotContain otherGroup.id

        taskGroupRepository.delete(defaultGroup)
        taskGroupRegistry.refresh()
        taskGroupExecutorManager.getTaskGroupIdsWithExecutors() shouldNotContain defaultGroup.id
    }

    @Test
    fun `should update executor if properties changed`() {
        val taskGroupExecutorProvider = mock<TaskGroupExecutorProvider> {
            doReturn(mock<TaskGroupExecutor>()).`when`(it).createTaskGroupExecutor(any(), eq(DEFAULT_SLEEP_TIME_MS))
        }
        val taskGroupExecutorManager = TaskGroupExecutorManager(
            taskGroupRegistry,
            taskGroupExecutorProvider
        )
        taskGroupExecutorManager.start()

        taskGroupExecutorManager.getTaskGroupIdsWithExecutors() shouldContainExactlyInAnyOrder
            taskGroupRepository.findAllForOwner().map { it.id }

        val actions = mutableListOf<TaskGroupRegistry.Action>()
        taskGroupRegistry.subscribe { taskGroup, action ->
            taskGroup.key shouldBe "test_key" // from createTestTaskGroup()
            actions.add(action)
        }

        val defaultGroup = createTestTaskGroup()

        actions shouldContainExactly listOf(TaskGroupRegistry.Action.CREATE)
        actions.clear()

        taskGroupRegistry.refresh()
        actions shouldHaveSize 0

        val newGroup = defaultGroup.copy(
            config = TaskGroupConfig(
                mapOf(TaskGroupConfig.BASE_POOL_ID to CommonObjectMapper.valueToTree("asdf")))
        )
        taskGroupRepository.update(newGroup)
        taskGroupRegistry.refresh()

        actions shouldContainExactly listOf(TaskGroupRegistry.Action.UPDATE)
        actions.clear()

        taskGroupRegistry.refresh()
        actions shouldHaveSize 0

        taskGroupRegistry.clearSubscribersInTest()
    }
}
