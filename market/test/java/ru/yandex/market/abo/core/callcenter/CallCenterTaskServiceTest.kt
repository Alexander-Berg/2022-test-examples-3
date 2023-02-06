package ru.yandex.market.abo.core.callcenter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.callcenter.core.CallCenterTask
import ru.yandex.market.abo.core.callcenter.core.CallCenterTaskService
import ru.yandex.market.abo.core.callcenter.core.CallCenterTaskState
import ru.yandex.market.abo.core.callcenter.core.CallCenterTaskType
import java.time.LocalDateTime

internal class CallCenterTaskServiceTest @Autowired constructor(
    private val callCenterTaskService: CallCenterTaskService,
) : EmptyTest() {

    @Test
    fun `get actual tasks`() {
        val newTask = callCenterTaskService.save(getExpressTask().apply {
            tryNumber = 0
            modificationTime = null
        })
        val shouldProcessAgainTask = callCenterTaskService.save(getExpressTask().apply {
            tryNumber = 1
            modificationTime = LocalDateTime.now().minusMinutes(2)
        })
        val justProcessedTask = callCenterTaskService.save(getExpressTask().apply {
            tryNumber = 1
            modificationTime = LocalDateTime.now()
        })
        val endedCollectedTask = callCenterTaskService.save(getExpressTask().apply {
            tryNumber = 2
            state = CallCenterTaskState.FINISHED
            modificationTime = LocalDateTime.now().minusMinutes(2)
        })
        val endedCancelledTask = callCenterTaskService.save(getExpressTask().apply {
            tryNumber = 3
            state = CallCenterTaskState.FINISHED
            modificationTime = LocalDateTime.now().minusMinutes(2)
        })

        val tasksForProcessing = callCenterTaskService.loadTasksForProcessing(
            taskType = CallCenterTaskType.EXPRESS,
            lastCalledBefore = LocalDateTime.now().minusMinutes(1)
        )

        assertThat(tasksForProcessing)
            .containsExactlyInAnyOrder(
                newTask,
                shouldProcessAgainTask,
            )
            .doesNotContain(
                justProcessedTask,
                endedCollectedTask,
                endedCancelledTask,
            )
    }

    companion object {
        private var sourceId = 0L
        private fun getExpressTask() = CallCenterTask(
            id = 0,
            type = CallCenterTaskType.EXPRESS,
            phone = "+71234567890",
            sourceId = sourceId++,
        )
    }
}
