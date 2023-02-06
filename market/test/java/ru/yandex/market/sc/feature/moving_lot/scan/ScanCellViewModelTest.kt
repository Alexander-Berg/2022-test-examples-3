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
import ru.yandex.market.sc.core.data.lot.MovingLot
import ru.yandex.market.sc.core.data.lot.MovingLotInfo.ErrorCode.*
import ru.yandex.market.sc.core.network.domain.NetworkLotUseCases
import ru.yandex.market.sc.core.resources.StringManager
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.feature.moving_lot.analytics.AppMetrica
import ru.yandex.market.sc.feature.moving_lot.presenter.scan.cell.ScanCellViewModel
import ru.yandex.market.sc.feature.moving_lot.presenter.scan.cell.data.ScanCellMode
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.yandex.market.test.utils.isNeverSet

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ScanCellViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var stringManager: StringManager

    @Mock
    private lateinit var appMetrica: AppMetrica

    @Mock
    private lateinit var networkLotUseCases: NetworkLotUseCases

    private lateinit var viewModel: ScanCellViewModel
    private val movingLot = TestFactory.createMovingLotWithErrorCode(MovingLot.ErrorCode.UNKNOWN)

    @Before
    fun setUp() {
        viewModel = ScanCellViewModel(networkLotUseCases, appMetrica, stringManager)
        assertThat(viewModel.uiState.getOrAwaitValue().mode).isEqualTo(ScanCellMode.CellQRCode)
    }

    @Test
    fun `success scan cell to move lot with error code UNKNOWN`() = runTest {
        val movingLotInfo = TestFactory.createMovingLotInoWithErrorCode(UNKNOWN)
        val cell = TestFactory.createCellLot("lot")
        val scanCell = ScanResultFactory.getScanResultQR(cell.externalId)

        `when`(
            networkLotUseCases.moveLot(
                movingLot.externalId,
                ExternalId(scanCell.value)
            )
        ).thenReturn(
            movingLotInfo
        )

        viewModel.onScan(scanCell, movingLot.externalId)
        assertThat(viewModel.successScanEvent.getOrAwaitValue().get()).isNotNull()
        assertThat(viewModel.uiState.getOrAwaitValue().errorCode).isEqualTo(UNKNOWN)
    }

    @Test
    fun `success scan cell to move lot with error code WRONG_CELL_DESTINATION`() = runTest {
        val movingLotInfo = TestFactory.createMovingLotInoWithErrorCode(WRONG_CELL_DESTINATION)
        val cell = TestFactory.createCellLot("lot")
        val scanCell = ScanResultFactory.getScanResultQR(cell.externalId)

        `when`(
            networkLotUseCases.moveLot(
                movingLot.externalId,
                ExternalId(scanCell.value)
            )
        ).thenReturn(
            movingLotInfo
        )

        viewModel.onScan(scanCell, movingLot.externalId)
        assertThat(viewModel.successScanEvent.isNeverSet()).isTrue()
        assertThat(viewModel.uiState.getOrAwaitValue().errorCode).isEqualTo(WRONG_CELL_DESTINATION)
        assertThat(viewModel.uiState.getOrAwaitValue().cellAddress).isEqualTo(movingLotInfo.cellAddress)
        assertThat(viewModel.uiState.getOrAwaitValue().cellDestination).isEqualTo(movingLotInfo.cellDestination)
    }

    @Test
    fun `success scan cell to move lot with error code ERROR_BY_CELL_CAPACITY`() = runTest {
        val movingLotInfo = TestFactory.createMovingLotInoWithErrorCode(ERROR_BY_CELL_CAPACITY)
        val cell = TestFactory.createCellLot("lot")
        val scanCell = ScanResultFactory.getScanResultQR(cell.externalId)

        `when`(
            networkLotUseCases.moveLot(
                movingLot.externalId,
                ExternalId(scanCell.value)
            )
        ).thenReturn(
            movingLotInfo
        )

        viewModel.onScan(scanCell, movingLot.externalId)
        assertThat(viewModel.successScanEvent.isNeverSet()).isTrue()
        assertThat(viewModel.uiState.getOrAwaitValue().errorCode).isEqualTo(ERROR_BY_CELL_CAPACITY)
        assertThat(viewModel.uiState.getOrAwaitValue().lotsInCell).isEqualTo(movingLotInfo.lotsInCell)
        assertThat(viewModel.uiState.getOrAwaitValue().cellCapacity).isEqualTo(movingLotInfo.cellCapacity)
    }

    @Test
    fun `scan lot with error`() = runTest {
        val cell = TestFactory.createCellLot("lot")
        val scanCell = ScanResultFactory.getScanResultQR(cell.externalId)

        `when`(
            networkLotUseCases.moveLot(
                movingLot.externalId,
                ExternalId(scanCell.value)
            )
        ).thenThrow(RuntimeException("Что-то пошло не так"))

        viewModel.onScan(scanCell, movingLot.externalId)

        assertThat(viewModel.successScanEvent.isNeverSet()).isTrue()
        assertThat(viewModel.uiState.getOrAwaitValue().overlayStatus).isEqualTo(OverlayState.Failure)
    }
}
