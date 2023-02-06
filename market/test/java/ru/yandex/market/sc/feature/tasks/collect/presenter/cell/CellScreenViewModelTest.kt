package ru.yandex.market.sc.feature.tasks.collect.presenter.cell

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkRouteTaskUseCases
import ru.yandex.market.sc.feature.tasks.collect.analytics.AppMetrica
import ru.yandex.market.sc.feature.tasks.collect.presenter.cell.data.CellScannerMode
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.network.mocks.errorResource
import ru.yandex.market.sc.test.network.mocks.successResource
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class CellScreenViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkCheckUserUseCases: NetworkCheckUserUseCases

    @Mock
    private lateinit var networkRouteTaskUseCases: NetworkRouteTaskUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private val stringManager = TestStringManager()
    private lateinit var viewModel: CellScreenViewModel

    private val routeTask = TestFactory.createRouteTask()
    private val finishedRouteTask =
        TestFactory.createRouteTask(cell = null, cellIndex = 1, totalCellCount = 0)
    private val routeId = 1L
    private val cellId = requireNotNull(routeTask.cell?.id)

    @Before
    fun setUp() {
        viewModel = CellScreenViewModel(
            networkCheckUserUseCases,
            networkRouteTaskUseCases,
            appMetrica,
            stringManager,
        )
    }

    @Test
    fun waitCellScan() {
        `when`(networkRouteTaskUseCases.getNextCell(routeId)).thenReturn(successResource(routeTask))
        viewModel.init(routeId)

        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(CellScannerMode.CellQRCode)
        assertThat(viewModel.status.getOrAwaitValue()).isEqualTo(OverlayState.None)
        assertThat(viewModel.cellName.getOrAwaitValue()).isEqualTo(routeTask.cell?.number)
        assertThat(viewModel.cellIndex.getOrAwaitValue()).isEqualTo(routeTask.cellIndex)
        assertThat(viewModel.totalCellCount.getOrAwaitValue()).isEqualTo(routeTask.totalCellCount)
        assertThat(viewModel.canTerminateTask.getOrAwaitValue()).isFalse()
    }

    @Test
    fun allCellsScanned() {
        `when`(networkRouteTaskUseCases.getNextCell(routeId)).thenReturn(
            successResource(
                finishedRouteTask
            )
        )
        viewModel.init(routeId)

        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(CellScannerMode.DoNotScan)
        assertThat(viewModel.cellIndex.getOrAwaitValue()).isEqualTo(finishedRouteTask.cellIndex)
        assertThat(viewModel.totalCellCount.getOrAwaitValue()).isEqualTo(finishedRouteTask.totalCellCount)
        assertThat(viewModel.status.getOrAwaitValue()).isEqualTo(OverlayState.Warning)
        assertThat(viewModel.canTerminateTask.getOrAwaitValue()).isTrue()
    }

    @Test
    fun errorLoadRouteTask() {
        val message = "Failed to load route task"
        `when`(networkRouteTaskUseCases.getNextCell(routeId)).thenReturn(errorResource(message))
        viewModel.init(routeId)

        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(CellScannerMode.DoNotScan)
        assertThat(viewModel.status.getOrAwaitValue()).isEqualTo(OverlayState.Failure)
        assertThat(viewModel.errorText.getOrAwaitValue()).isEqualTo(message)
        assertThat(viewModel.canTerminateTask.getOrAwaitValue()).isTrue()
    }

    @Test
    fun successCellScan() {
        `when`(networkRouteTaskUseCases.getNextCell(routeId)).thenReturn(successResource(routeTask))
        viewModel.init(routeId)

        val scanResult = ScanResultFactory.getScanResultQR(cellId)
        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(CellScannerMode.CellQRCode)
        viewModel.onScan(scanResult)
        assertThat(
            viewModel.successScanCellEvent.getOrAwaitValue().get()
        ).isEqualTo(cellId)
    }

    @Test
    fun failureCellScan() {
        `when`(networkRouteTaskUseCases.getNextCell(routeId)).thenReturn(successResource(routeTask))
        viewModel.init(routeId)

        val scanResult = ScanResultFactory.getScanResultQR(cellId + 1)
        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(CellScannerMode.CellQRCode)
        viewModel.onScan(scanResult)
        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(CellScannerMode.CellQRCode)
        assertThat(viewModel.status.getOrAwaitValue()).isEqualTo(OverlayState.Failure)
        assertThat(viewModel.canTerminateTask.getOrAwaitValue()).isFalse()
    }
}
