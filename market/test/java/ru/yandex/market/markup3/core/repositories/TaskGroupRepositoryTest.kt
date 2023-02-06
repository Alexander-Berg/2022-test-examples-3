package ru.yandex.market.markup3.core.repositories

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.markup3.core.dto.TaskGroup
import ru.yandex.market.markup3.core.dto.TaskGroupConfig
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.utils.CommonObjectMapper

class TaskGroupRepositoryTest : CommonTaskTest() {
    @Test
    fun `should correctly store and read overlap properties`() {
        val groupTestKey = "test_key"
        val overlapConfig = TaskGroupConfig.OverlapProperties(
            minOverlap = 10, maxOverlap = 100, confidenceProportion = 0.45f, childTaskGroupKey = "abc"
        ).let { TaskGroupConfig(mapOf(TaskGroupConfig.OVERLAP_PROPERTIES to CommonObjectMapper.valueToTree(it))) }
        taskGroupRepository.createOrGetForKey(groupTestKey) {
            TaskGroup(
                key = groupTestKey,
                name = "test key name",
                taskType = TaskType.DICE,
                config = overlapConfig
            )
        }

        taskGroupRegistry.refresh()

        taskGroupRegistry.getTaskGroupOrThrow(TaskType.DICE, groupTestKey).let {
            it.config.getOverlapProperties() shouldBe overlapConfig.getOverlapProperties()
            it.key shouldBe groupTestKey
            it.taskType shouldBe TaskType.DICE
        }
    }
}
