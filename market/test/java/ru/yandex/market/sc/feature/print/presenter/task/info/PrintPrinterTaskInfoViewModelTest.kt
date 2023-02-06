package ru.yandex.market.sc.feature.print.presenter.task.info

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.data.tasks.PrinterTask
import ru.yandex.market.sc.core.network.domain.NetworkPrintTaskUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSharedPreferencesUseCases
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.network.mocks.successResource
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class PrintPrinterTaskInfoViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkPrintTaskUseCases: NetworkPrintTaskUseCases

    @Mock
    private lateinit var networkSharedPreferencesUseCases: NetworkSharedPreferencesUseCases

    private lateinit var viewModel: PrintTaskInfoViewModel

    private val createdTask = TestFactory.createPrinterTask(status = PrinterTask.Status.CREATED)
    private val completedTask = TestFactory.createPrinterTask(status = PrinterTask.Status.COMPLETED)

    @Before
    fun setUp() {
        `when`(networkSharedPreferencesUseCases.haveRightsToShowAdditionalPrintTaskInfo()).thenReturn(true)
        `when`(networkPrintTaskUseCases.getTaskById(createdTask.id)).thenReturn(
            successResource(createdTask)
        )
        `when`(networkPrintTaskUseCases.getTaskById(completedTask.id)).thenReturn(
            successResource(completedTask)
        )
        viewModel =
            PrintTaskInfoViewModel(networkPrintTaskUseCases, networkSharedPreferencesUseCases)
    }

    @Test
    fun onRefresh() {
        viewModel.init(createdTask.id)
        assertThat(viewModel.printerTask.getOrAwaitValue()).isEqualTo(createdTask)

        `when`(networkPrintTaskUseCases.getTaskById(createdTask.id)).thenReturn(
            successResource(completedTask)
        )
        viewModel.onRefresh()
        assertThat(viewModel.printerTask.getOrAwaitValue()).isEqualTo(completedTask)
    }

    @Test
    fun onRetry() {
        viewModel.init(completedTask.id)
        assertThat(viewModel.printerTask.getOrAwaitValue()).isEqualTo(completedTask)

        `when`(networkPrintTaskUseCases.retryTask(completedTask.id)).thenReturn(
            successResource(createdTask)
        )
        viewModel.onRetry()
        assertThat(viewModel.printerTask.getOrAwaitValue()).isEqualTo(createdTask)
    }
}
