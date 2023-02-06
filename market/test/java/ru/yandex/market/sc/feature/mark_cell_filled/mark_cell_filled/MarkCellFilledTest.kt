package ru.yandex.market.sc.feature.mark_cell_filled.mark_cell_filled


import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.network.domain.NetworkCellUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.feature.mark_cell_filled.analytics.AppMetrica
import ru.yandex.market.sc.feature.mark_cell_filled.presenter.scan_cell.ScanCellViewModel
import ru.yandex.market.sc.feature.mark_cell_filled.presenter.scan_cell.data.CellScannerMode
import ru.yandex.market.sc.test.network.mocks.errorResource
import ru.yandex.market.sc.test.network.mocks.successResource
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class MarkCellFilledTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkCellUseCases: NetworkCellUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: ScanCellViewModel

    private val cellId = 1L

    @Before
    fun setUp() {
        viewModel = ScanCellViewModel(
            appMetrica,
            networkCellUseCases,
            stringManager,
        )
        viewModel.reset()

        Truth.assertThat(viewModel.state.getOrAwaitValue().mode)
            .isEqualTo(CellScannerMode.CellQRCode)
    }

    @Test
    fun `success scan cell`() = runTest {
        val scanCellResult = ScanResultFactory.getScanResultQR(cellId)

        `when`(networkCellUseCases.markFilledStatus(cellId, true)).thenReturn(successResource(Unit))
        viewModel.onScan(scanCellResult, cellId)
        Truth.assertThat(viewModel.state.getOrAwaitValue().mode)
            .isEqualTo(CellScannerMode.CellQRCode)
        Truth.assertThat(viewModel.successMark.getOrAwaitValue().get()).isEqualTo(Unit)
        Truth.assertThat(viewModel.state.getOrAwaitValue().status).isEqualTo(OverlayState.Success)
    }

    @Test
    fun `scan with response error`() = runTest {
        val scanCellResult = ScanResultFactory.getScanResultQR(cellId)
        val error = "Some problem"

        `when`(networkCellUseCases.markFilledStatus(cellId, true)).thenReturn(errorResource(error))
        viewModel.onScan(scanCellResult, cellId)
        Truth.assertThat(viewModel.state.getOrAwaitValue().mode)
            .isEqualTo(CellScannerMode.CellQRCode)
        Truth.assertThat(viewModel.state.getOrAwaitValue().status).isEqualTo(OverlayState.Failure)
    }

    @Test
    fun `scan with error on scan`() = runTest {
        val scanWrongFormatResult = ScanResultFactory.getScanResultBarcode("wrongFormat")

        viewModel.onScan(scanWrongFormatResult, cellId)
        Truth.assertThat(viewModel.errorOnScan.getOrAwaitValue().get()).isEqualTo(Unit)
        Truth.assertThat(viewModel.state.getOrAwaitValue().mode)
            .isEqualTo(CellScannerMode.CellQRCode)
        Truth.assertThat(viewModel.state.getOrAwaitValue().status).isEqualTo(OverlayState.Warning)
    }

    @Test
    fun `scan with error not selected cell`() = runTest {
        val wronglySelectedCell = 2L
        val scanCellResult = ScanResultFactory.getScanResultQR(wronglySelectedCell)

        viewModel.onScan(scanCellResult, cellId)
        Truth.assertThat(viewModel.scanNotSelectedCell.getOrAwaitValue().get()).isEqualTo(Unit)
        Truth.assertThat(viewModel.state.getOrAwaitValue().mode)
            .isEqualTo(CellScannerMode.CellQRCode)
        Truth.assertThat(viewModel.state.getOrAwaitValue().status).isEqualTo(OverlayState.Warning)
    }
}
