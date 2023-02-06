package ru.yandex.market.sc.feature.moving_lot.scan

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.core.data.lot.MovingLot.ErrorCode.*
import ru.yandex.market.sc.core.network.domain.NetworkLotUseCases
import ru.yandex.market.sc.core.resources.StringManager
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.feature.moving_lot.analytics.AppMetrica
import ru.yandex.market.sc.feature.moving_lot.presenter.scan.lot.ScanLotViewModel
import ru.yandex.market.sc.feature.moving_lot.presenter.scan.lot.ScanLotViewModel.PossibleDirection
import ru.yandex.market.sc.feature.moving_lot.presenter.scan.lot.data.ScanLotMode
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.yandex.market.test.utils.isNeverSet

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ScanLotViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var appMetrica: AppMetrica

    @Mock
    private lateinit var networkLotUseCases: NetworkLotUseCases

    private val stringManager = TestStringManager()
    private lateinit var viewModel: ScanLotViewModel

    @Before
    fun setUp() {
        viewModel = ScanLotViewModel(networkLotUseCases, appMetrica, stringManager)
    }

    @Test
    fun `success scan lot to move with error code UNKNOWN`() = runTest {
        val movingLot = TestFactory.createMovingLotWithErrorCode(UNKNOWN)
        val scanLot = ScanResultFactory.getScanResultQR(movingLot.externalId)
        `when`(networkLotUseCases.getLotForMoving(movingLot.externalId)).thenReturn(movingLot)

        viewModel.onScan(scanLot)
        assertThat(viewModel.toAnotherScreenEvent.getOrAwaitValue().get()).isEqualTo(
            PossibleDirection.ToScanCellScreen
        )
        assertThat(viewModel.uiState.getOrAwaitValue().errorCode).isEqualTo(UNKNOWN)
    }

    @Test
    fun `success scan lot to move with error code NO_CELLS_FOR_DESTINATION`() = runTest {
        val movingLot = TestFactory.createMovingLotWithErrorCode(NO_CELLS_FOR_DESTINATION)
        val scanLot = ScanResultFactory.getScanResultQR(movingLot.externalId)
        `when`(networkLotUseCases.getLotForMoving(movingLot.externalId)).thenReturn(movingLot)

        viewModel.onScan(scanLot)
        assertThat(viewModel.toAnotherScreenEvent.getOrAwaitValue().get()).isEqualTo(
            PossibleDirection.ToNoCellsForDestinationScreen
        )
        assertThat(viewModel.uiState.getOrAwaitValue().errorCode).isEqualTo(NO_CELLS_FOR_DESTINATION)
    }

    @Test
    fun `success scan lot to move with error code ALREADY_SHIPPED`() = runTest {
        val movingLot = TestFactory.createMovingLotWithErrorCode(ALREADY_SHIPPED)
        val scanLot = ScanResultFactory.getScanResultQR(movingLot.externalId)
        `when`(networkLotUseCases.getLotForMoving(movingLot.externalId)).thenReturn(movingLot)

        viewModel.onScan(scanLot)
        assertThat(viewModel.uiState.getOrAwaitValue().overlayStatus).isEqualTo(OverlayState.Warning)
        assertThat(viewModel.uiState.getOrAwaitValue().errorCode).isEqualTo(ALREADY_SHIPPED)
    }

    @Test
    fun `success scan lot to move with error code ALREADY_IN_CELL`() = runTest {
        val movingLot = TestFactory.createMovingLotWithErrorCode(ALREADY_IN_CELL)
        val scanLot = ScanResultFactory.getScanResultQR(movingLot.externalId)
        `when`(networkLotUseCases.getLotForMoving(movingLot.externalId)).thenReturn(movingLot)

        viewModel.onScan(scanLot)
        assertThat(viewModel.uiState.getOrAwaitValue().overlayStatus).isEqualTo(OverlayState.Warning)
        assertThat(viewModel.uiState.getOrAwaitValue().cellAddress).isEqualTo(movingLot.cellAddress)
        assertThat(viewModel.uiState.getOrAwaitValue().errorCode).isEqualTo(ALREADY_IN_CELL)
    }


    @Test
    fun `success scan lot to move with error code WRONG_LOT_STATUS`() = runTest {
        val movingLot = TestFactory.createMovingLotWithErrorCode(WRONG_LOT_STATUS)
        val scanLot = ScanResultFactory.getScanResultQR(movingLot.externalId)
        `when`(networkLotUseCases.getLotForMoving(movingLot.externalId)).thenReturn(movingLot)

        viewModel.onScan(scanLot)
        assertThat(viewModel.uiState.getOrAwaitValue().overlayStatus).isEqualTo(OverlayState.Warning)
        assertThat(viewModel.uiState.getOrAwaitValue().lotStatus).isEqualTo(Lot.Status.CREATED)
        assertThat(viewModel.uiState.getOrAwaitValue().errorCode).isEqualTo(WRONG_LOT_STATUS)
    }

    @Test
    fun `validate scan format`() = runTest {
        val movingLot = TestFactory.createMovingLotWithErrorCode(UNKNOWN)
        val scanLot = ScanResultFactory.getScanResultBarcode(movingLot.externalId)

        viewModel.onScan(scanLot)
        assertThat(viewModel.toAnotherScreenEvent.isNeverSet()).isTrue()
        assertThat(viewModel.uiState.getOrAwaitValue().mode).isEqualTo(ScanLotMode.LotQr)
        assertThat(viewModel.uiState.getOrAwaitValue().overlayStatus).isEqualTo(OverlayState.Failure)
    }

    @Test
    fun `scan lot with error`() = runTest {
        val movingLot = TestFactory.createLot().build()
        val scanLot = ScanResultFactory.getScanResultQR(movingLot.externalId)
        `when`(networkLotUseCases.getLotForMoving(movingLot.externalId))
            .thenThrow(RuntimeException())

        viewModel.onScan(scanLot)
        assertThat(viewModel.toAnotherScreenEvent.isNeverSet()).isTrue()
        assertThat(viewModel.uiState.getOrAwaitValue().mode).isEqualTo(ScanLotMode.LotQr)
        assertThat(viewModel.uiState.getOrAwaitValue().overlayStatus).isEqualTo(OverlayState.Failure)
    }
}
