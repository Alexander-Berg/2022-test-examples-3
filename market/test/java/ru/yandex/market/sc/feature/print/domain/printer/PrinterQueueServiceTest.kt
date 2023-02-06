package ru.yandex.market.sc.feature.print.domain.printer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.data.tasks.PrinterTask
import ru.yandex.market.sc.core.network.domain.NetworkPrintTaskUseCases
import ru.yandex.market.sc.feature.print.domain.SharedPreferenceUseCases
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.network.mocks.successResource
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class PrinterQueueServiceTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var networkPrintTaskUseCases: NetworkPrintTaskUseCases

    @Mock
    private lateinit var sharedPreferenceUseCases: SharedPreferenceUseCases

    private lateinit var printerQueueService: PrinterQueueService

    private val createdTask = TestFactory.createPrinterTask(status = PrinterTask.Status.CREATED)
    private val createdTaskBase = TestFactory.mapToBase(createdTask)
    private val completedTask = createdTaskBase.copy(status = PrinterTask.Status.COMPLETED)
    private val failedTask = createdTaskBase.copy(status = PrinterTask.Status.FAILED)

    @Before
    fun setUp() {
        printerQueueService =
            PrinterQueueService(networkPrintTaskUseCases, sharedPreferenceUseCases)
    }

    @Test
    fun getTaskFailedEvent() = runTest {
        `when`(networkPrintTaskUseCases.getTasks()).thenReturn(
            successResource(
                listOf(
                    failedTask
                )
            )
        )
        printerQueueService.addToQueue(createdTask)

        assertThat(sharedPreferenceUseCases.getTaskList()).isEmpty()
        assertThat(printerQueueService.taskFailedEvent.getOrAwaitValue()).isNotNull()
    }

    @Test
    fun getTaskSuccessEvent() = runTest {
        `when`(networkPrintTaskUseCases.getTasks()).thenReturn(
            successResource(
                listOf(
                    completedTask
                )
            )
        )
        printerQueueService.addToQueue(createdTask)

        assertThat(sharedPreferenceUseCases.getTaskList()).isEmpty()
    }
}
