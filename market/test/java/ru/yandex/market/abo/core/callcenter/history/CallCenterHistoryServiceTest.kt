package ru.yandex.market.abo.core.callcenter.history

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.callcenter.core.CallCenterTask
import ru.yandex.market.abo.core.callcenter.core.CallCenterTaskService

internal class CallCenterHistoryServiceTest @Autowired constructor(
    private val callCenterHistoryService: CallCenterHistoryService,
    private val callCenterTaskService: CallCenterTaskService,
) : EmptyTest() {

    @Test
    fun `load by status`() {
        val taskId = callCenterTaskService.save(CallCenterTask()).id
        saveHistory(taskId, CallCenterHistoryStatus.COMPLETED)
        saveHistory(taskId, CallCenterHistoryStatus.FAILED)
        val processingGuid = saveHistory(taskId, CallCenterHistoryStatus.PROCESSING)

        val result = callCenterHistoryService.findAllByStatus(CallCenterHistoryStatus.PROCESSING)

        assertThat(result).singleElement()
        assertThat(result.first().guid).isEqualTo(processingGuid)
    }

    private fun saveHistory(taskId: Long, status: CallCenterHistoryStatus): String =
        callCenterHistoryService.save(
            CallCenterHistory(
                guid = (nextGuid++).toString(),
                taskId = taskId,
                status = status,
            )
        ).guid

    companion object {
        private var nextGuid = 0
    }
}
