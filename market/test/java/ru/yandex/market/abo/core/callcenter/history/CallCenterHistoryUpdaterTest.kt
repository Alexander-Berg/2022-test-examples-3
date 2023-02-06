package ru.yandex.market.abo.core.callcenter.history

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.core.callcenter.core.CallCenterCaller

internal class CallCenterHistoryUpdaterTest {

    private val callCenterHistoryService: CallCenterHistoryService = mock()
    private val callCenterCaller: CallCenterCaller = mock()

    private val callCenterHistoryUpdater = CallCenterHistoryUpdater(
        callCenterHistoryService = callCenterHistoryService,
        callCenterCaller = callCenterCaller,
    )

    @BeforeEach
    fun init() {
        whenever(callCenterHistoryService.findAllByStatus(any())).thenReturn(listOf(CallCenterHistory()))
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "{state: \"completed\"}, COMPLETED",
            "{state: \"failed\"}, FAILED",
            "{state: \"processing\"}, PROCESSING",
            "{}, FAILED",
        ]
    )
    fun `received status completed`(message: String, status: CallCenterHistoryStatus) {
        whenever(callCenterCaller.getCallScript(any())).thenReturn(message)

        callCenterHistoryUpdater.update()

        argumentCaptor<CallCenterHistory>().apply {
            verify(callCenterHistoryService, times(1)).save(capture())
            val savedHistory = firstValue

            assertThat(savedHistory.status).isEqualTo(status)
            assertThat(savedHistory.message).isEqualTo(message)
        }
    }

    @Test
    fun `set failed on exception`() {
        whenever(callCenterCaller.getCallScript(any())).doThrow(Exception("not completed"))

        callCenterHistoryUpdater.update()

        argumentCaptor<CallCenterHistory>().apply {
            verify(callCenterHistoryService, times(1)).save(capture())
            val savedHistory = firstValue

            assertThat(savedHistory.status).isEqualTo(CallCenterHistoryStatus.FAILED)
            assertThat(savedHistory.message).isEqualTo("not completed")
        }
    }
}
