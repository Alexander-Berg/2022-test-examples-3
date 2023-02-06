package ru.yandex.market.tpl.courier.domain.feature.offline

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import ru.yandex.market.tpl.courier.react.scheduler.ReactOfflineTasksProxy

class AwaitPendingOfflineTasksUseCaseTest {

    private val nativeTasksUseCase: GetOfflineTasksUseCase = mockk()
    private val reactProxy: ReactOfflineTasksProxy = mockk()
    private val useCase = AwaitPendingOfflineTasksUseCase(nativeTasksUseCase, reactProxy)

    @Test
    fun `Ждёт пока полностью выполнятся обе очереди`() = runBlockingTest {
        val reactFlow = MutableStateFlow(10)
        val nativeFlow = MutableStateFlow(listOf(offlineTaskTestInstance()))
        coEvery { reactProxy.getReactQueueSizeFlow() } returns reactFlow
        coEvery { nativeTasksUseCase.getScheduledTasksFlow() } returns nativeFlow

        val job = launch { useCase.awaitPendingOfflineTasks() }
        withClue("Expected to await both queues to become empty") {
            job.isCompleted shouldBe false
        }

        reactFlow.value = 0
        withClue("Expected to await native queue to become empty") {
            job.isCompleted shouldBe false
        }

        nativeFlow.value = emptyList()
        withClue("Expected to complete") {
            job.isCompleted shouldBe true
        }
    }
}