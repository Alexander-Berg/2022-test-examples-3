package ru.yandex.market.sc.feature.print.presenter.task.list

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
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.network.mocks.successResource
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class PrintPrinterTaskListViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkPrintTaskUseCases: NetworkPrintTaskUseCases
    private lateinit var viewModel: PrintTaskListViewModel

    private val taskList = PrinterTask.Status.values()
        .map { TestFactory.createPrinterTask(status = it) }
        .map { TestFactory.mapToBase(printerTask = it) }

    @Before
    fun setUp() {
        `when`(networkPrintTaskUseCases.getTasks()).thenReturn(successResource(taskList))
        viewModel = PrintTaskListViewModel(networkPrintTaskUseCases)
    }

    @Test
    fun onRefresh() {
        val updatedTasks = taskList.first()
        `when`(networkPrintTaskUseCases.getTasks()).thenReturn(
            successResource(
                listOf(
                    updatedTasks
                )
            )
        )
        viewModel.onRefresh()

        assertThat(viewModel.tasks.getOrAwaitValue().size).isEqualTo(1)
    }

    @Test
    fun onClick() {
        val taskId = 1L
        viewModel.onClick(taskId)

        assertThat(viewModel.redirectTaskEvent.getOrAwaitValue().get()).isEqualTo(taskId)
    }
}
