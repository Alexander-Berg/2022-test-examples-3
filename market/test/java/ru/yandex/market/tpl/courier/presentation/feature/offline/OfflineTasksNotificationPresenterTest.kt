package ru.yandex.market.tpl.courier.presentation.feature.offline

import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import ru.yandex.market.tpl.courier.arch.coroutine.TestPresentationDispatchers
import ru.yandex.market.tpl.courier.domain.feature.offline.offlineTaskTestInstance

class OfflineTasksNotificationPresenterTest {

    private val dispatchers = TestPresentationDispatchers()
    private val useCases: OfflineTasksNotificationUseCases = mockk {
        coEvery { getScheduledTasksFlow() } returns flowOf(emptyList(), listOf(offlineTaskTestInstance()))
        every { isOfflineTasksServiceRunning() } returns false
    }
    private val view: OfflineTasksNotificationView = mockk {
        justRun { showPendingOfflineTasksNotification() }
    }
    private val analytics: OfflineTasksFlowAnalytics = mockk {
        justRun { reportCurrentOfflineTasks(any(), any()) }
    }
    private val presenter = OfflineTasksNotificationPresenter(dispatchers, useCases, analytics)


    @Test
    fun `Показывает нотификацию когда сервис не запущен и есть незавершённые задачи`() {
        presenter.attachView(view)

        verify { view.showPendingOfflineTasksNotification() }
    }

    @Test
    fun `Не показывает нотификацию когда сервис запущен`() {
        every { useCases.isOfflineTasksServiceRunning() } returns true

        presenter.attachView(view)

        verify(exactly = 0) { view.showPendingOfflineTasksNotification() }
    }

    @Test
    fun `Не показывает нотификацию когда нет задач в очереди`() {
        coEvery { useCases.getScheduledTasksFlow() } returns flowOf(emptyList())

        presenter.attachView(view)

        verify(exactly = 0) { view.showPendingOfflineTasksNotification() }
    }
}