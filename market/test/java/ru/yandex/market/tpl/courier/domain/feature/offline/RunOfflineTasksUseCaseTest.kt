package ru.yandex.market.tpl.courier.domain.feature.offline

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import ru.yandex.market.tpl.courier.arch.common.seconds
import ru.yandex.market.tpl.courier.arch.fp.failure
import ru.yandex.market.tpl.courier.arch.fp.success
import ru.yandex.market.tpl.courier.data.feature.offline.OfflineTasksRepository
import ru.yandex.market.tpl.courier.domain.feature.network.NetworkAvailabilityUseCase
import ru.yandex.market.tpl.courier.domain.feature.user.UpdateUserPropertyUseCase
import ru.yandex.market.tpl.courier.extensions.advanceTimeBy
import ru.yandex.market.tpl.courier.extensions.someFailure
import ru.yandex.market.tpl.courier.presentation.feature.offline.OfflineTasksFlowAnalytics
import java.io.IOException

class RunOfflineTasksUseCaseTest {

    private val offlineTasksRepository: OfflineTasksRepository = mockk {
        coEvery { getScheduledTasksFlow() }.returnsMany(
            flowOf(listOf(offlineTaskTestInstance())),
            flowOf(emptyList()),
        )
        coEvery { removeTask(any()) } returns success()
        coEvery { removeAllTasks() } returns success()
        coEvery { reportTaskExecutionFailed(any(), any(), any()) } returns success()
    }
    private val networkAvailabilityUseCase: NetworkAvailabilityUseCase = mockk {
        coEvery { checkIsNetworkConnectionAvailable() } returns true
    }
    private val scheduleUseCase: ScheduleOfflineTasksRunUseCase = mockk {
        justRun { scheduleOfflineTasksRunWhenNetworkConnectionIsAvailable(any()) }
    }
    private val isNeedToClearUseCase: IsNeedToClearTasksServiceUseCase = mockk {
        coEvery { isNeedToClearOfflineTasks() } returns success(false)
    }
    private val updateUserPropertyUseCase: UpdateUserPropertyUseCase = mockk {
        coEvery { disableClearSchedulerProperty() } returns success(Unit)
    }

    private val configuration = RunOfflineTasksUseCase.Configuration(
        maxRetryAttempts = 1,
        retryDelay = 5.seconds,
        rescheduleDelay = 10.seconds,
    )
    private val analytics: OfflineTasksFlowAnalytics = mockk {
        justRun { reportOfflineTasksExecutionStarted() }
        justRun { reportNativeOfflineDisabled() }
        justRun { reportStartingExecutionAttempt(any()) }
        justRun { reportExecutionEndedDueToMissingNetwork() }
        justRun { reportExecutionEndedDueToFailedAttempts() }
        justRun { reportExecutionEndedDueToUnexpectedError(any()) }
        justRun { reportCurrentOfflineTasksDuringExecution(any()) }
        justRun { reportRunningOfflineTask(any()) }
        justRun { reportAllOfflineTasksExecuted() }
    }
    private val offlineTasksExecutor: OfflineTasksExecutor = mockk {
        coEvery { executeCurrentOfflineTasks(any()) } returns success()
    }
    private val useCase = RunOfflineTasksUseCase(
        offlineTasksRepository,
        networkAvailabilityUseCase,
        scheduleUseCase,
        isNeedToClearUseCase,
        updateUserPropertyUseCase,
        configuration,
        analytics,
        offlineTasksExecutor,
    )

    @Test
    fun `Возвращает положительный результат когда все задачи успешно выполнились`() = runBlockingTest {
        val result = useCase.runScheduledOfflineTasks()
        result shouldBe success(OfflineTasksRunReport(isEveryTaskCompleted = true))
    }

    @Test
    fun `Перезапускает выполнение задач при IOException`() = runBlockingTest {
        coEvery { offlineTasksRepository.getScheduledTasksFlow() }.returnsMany(
            flowOf(listOf(offlineTaskTestInstance())),
            flowOf(listOf(offlineTaskTestInstance())),
            flowOf(emptyList()),
        )
        coEvery { offlineTasksExecutor.executeCurrentOfflineTasks(any()) }
            .returnsMany(failure(IOException()), success())

        useCase.runScheduledOfflineTasks()

        coVerify(exactly = 2) { offlineTasksExecutor.executeCurrentOfflineTasks(any()) }
    }

    @Test
    fun `Не фейлится при ошибках отличных от IOException`() = runBlockingTest {
        val exception = RuntimeException()
        coEvery { offlineTasksExecutor.executeCurrentOfflineTasks(any()) } throws exception

        val result = useCase.runScheduledOfflineTasks()

        result shouldNotBe someFailure<OfflineTasksRunReport, Throwable>()
    }

    @Test
    fun `Регистрирует в планировщике следующий запуск при отсутствии интернета`() = runBlockingTest {
        coEvery { offlineTasksExecutor.executeCurrentOfflineTasks(any()) } throws IOException()
        coEvery { networkAvailabilityUseCase.checkIsNetworkConnectionAvailable() } returns false

        val result = useCase.runScheduledOfflineTasks()

        result shouldBe success(OfflineTasksRunReport(isEveryTaskCompleted = false))
        verify { scheduleUseCase.scheduleOfflineTasksRunWhenNetworkConnectionIsAvailable(null) }
    }

    @Test
    fun `Регистрирует в планировщике следующий запуск после максимального количества перезапусков`() = runBlockingTest {
        coEvery { offlineTasksRepository.getScheduledTasksFlow() } returns flowOf(listOf(offlineTaskTestInstance()))
        coEvery { offlineTasksExecutor.executeCurrentOfflineTasks(any()) } throws IOException()

        val result = useCase.runScheduledOfflineTasks()

        result shouldBe success(OfflineTasksRunReport(isEveryTaskCompleted = false))
        verify {
            scheduleUseCase.scheduleOfflineTasksRunWhenNetworkConnectionIsAvailable(configuration.rescheduleDelay)
        }
    }

    @Test
    fun `Ожидает некоторое время перед повторными запусками`() = runBlockingTest {
        coEvery { offlineTasksRepository.getScheduledTasksFlow() } returns flowOf(listOf(offlineTaskTestInstance()))
        coEvery { offlineTasksExecutor.executeCurrentOfflineTasks(any()) } throws IOException()

        val dispatcher = TestCoroutineDispatcher()
        val job = launch(context = dispatcher) { useCase.runScheduledOfflineTasks() }
        dispatcher.runCurrent()
        coVerify(exactly = 1) { offlineTasksExecutor.executeCurrentOfflineTasks(any()) }

        dispatcher.advanceTimeBy(configuration.retryDelay)
        coVerify(exactly = 2) { offlineTasksExecutor.executeCurrentOfflineTasks(any()) }

        dispatcher.advanceUntilIdle()
        job.isCompleted.shouldBeTrue()
    }

    @Test
    fun `Очищает очередь задач и выходит если включена очистка`() = runBlockingTest {
        coEvery { isNeedToClearUseCase.isNeedToClearOfflineTasks() } returns success(true)

        val result = useCase.runScheduledOfflineTasks()

        coVerify { offlineTasksRepository.removeAllTasks() }
        result shouldBe success(OfflineTasksRunReport(isEveryTaskCompleted = true))
    }

    @Test
    fun `Всё равно выполняет задачи если не удалось проверить включён ли нативный оффлайн`() = runBlockingTest {
        coEvery { isNeedToClearUseCase.isNeedToClearOfflineTasks() } returns failure(RuntimeException())

        val result = useCase.runScheduledOfflineTasks()

        coVerify(exactly = 0) { offlineTasksRepository.removeAllTasks() }
        result shouldBe success(OfflineTasksRunReport(isEveryTaskCompleted = true))
    }
}