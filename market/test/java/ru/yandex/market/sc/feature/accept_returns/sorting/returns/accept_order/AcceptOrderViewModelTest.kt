package ru.yandex.market.sc.feature.accept_returns.sorting.returns.accept_order

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSortableUseCases
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.feature.accept_returns.alanytics.AppMetrica
import ru.yandex.market.sc.feature.accept_returns.presenter.accept_order.AcceptOrderViewModel
import ru.yandex.market.sc.feature.accept_returns.presenter.accept_order.data.AcceptScannerMode
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class AcceptOrderViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var networkCheckUserUseCases: NetworkCheckUserUseCases

    @Mock
    private lateinit var networkSortableUseCases: NetworkSortableUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: AcceptOrderViewModel

    private val courierId = 1L

    @Before
    fun setUp() {
        viewModel = AcceptOrderViewModel(
            networkOrderUseCases,
            networkSortableUseCases,
            appMetrica,
            networkCheckUserUseCases,
            stringManager,
        )

        viewModel.init(courierId)

        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(AcceptScannerMode.OrderBarcode)
    }

    @Test
    fun `success scan single order`() = runTest {
        val returnCell = TestFactory.getReturnCell("R-1")
        val bufferReturnCell =
            TestFactory.getBufferCell("АХ-1", subType = Cell.SubType.BUFFER_RETURNS)
        val sortResponse = TestFactory.getSortResponse(returnCell)
        val order = TestFactory.getOrderToReturn(cell = bufferReturnCell, cellTo = returnCell)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(returnCell.id)

        `when`(
            networkOrderUseCases.acceptReturnOrder(
                order.externalId,
                courierId = courierId
            )
        ).thenReturn(order)
        viewModel.onScan(scanOrderResult)

        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(AcceptScannerMode.CellQRCode)

        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationExternalId = ExternalId(returnCell.id),
                place.externalId
            )
        )
            .thenReturn(sortResponse)
        viewModel.onScan(scanCellResult)
        assertThat(viewModel.scannedOrderItemCount.getOrAwaitValue()).isEqualTo(1)
    }

    @Test
    fun `success scan multiplace order`() = runTest {
        val returnCell = TestFactory.getReturnCell("R-1")
        val bufferReturnCell =
            TestFactory.getBufferCell("АХ-1", subType = Cell.SubType.BUFFER_RETURNS)
        val sortResponse = TestFactory.getSortResponse(returnCell)
        val order = TestFactory.getOrderToReturn(
            numberOfPlaces = 2,
            cell = bufferReturnCell,
            cellTo = returnCell
        )

        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(returnCell.id)

        `when`(
            networkOrderUseCases.acceptReturnOrder(
                order.externalId,
                courierId = courierId
            )
        ).thenReturn(order)
        viewModel.onScan(scanOrderResult)

        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(AcceptScannerMode.PlaceBarcode)

        `when`(
            networkOrderUseCases.acceptReturnOrder(
                order.externalId,
                place.externalId,
                courierId
            )
        ).thenReturn(order)
        viewModel.onScan(scanPlaceResult)

        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(AcceptScannerMode.CellQRCode)

        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationExternalId = ExternalId(returnCell.id),
                place.externalId
            )
        )
            .thenReturn(sortResponse)
        viewModel.onScan(scanCellResult)

        assertThat(viewModel.scannedOrderItemCount.getOrAwaitValue()).isEqualTo(1)
    }

    @Test
    fun `scan cell with error`() = runTest {
        val returnCell = TestFactory.getReturnCell("R-1")
        val errorCellScan = 125L
        val bufferReturnCell =
            TestFactory.getBufferCell("АХ-1", subType = Cell.SubType.BUFFER_RETURNS)
        val sortResponse = TestFactory.getSortResponse(returnCell)
        val order = TestFactory.getOrderToReturn(cell = bufferReturnCell, cellTo = returnCell)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(returnCell.id)
        val errorScanResult = ScanResultFactory.getScanResultQR(errorCellScan)

        `when`(
            networkOrderUseCases.acceptReturnOrder(
                order.externalId,
                courierId = courierId
            )
        ).thenReturn(order)
        viewModel.onScan(scanOrderResult)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationExternalId = ExternalId(errorScanResult.value),
                place.externalId
            )
        )
            .thenThrow(RuntimeException("Нет такой ячейки"))
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationExternalId = ExternalId(returnCell.id),
                place.externalId
            )
        )
            .thenReturn(sortResponse)

        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(AcceptScannerMode.CellQRCode)

        viewModel.onScan(errorScanResult)
        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(AcceptScannerMode.CellQRCode)

        viewModel.onScan(scanCellResult)
        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(AcceptScannerMode.OrderBarcode)
        assertThat(viewModel.scannedOrderItemCount.getOrAwaitValue()).isEqualTo(1)
    }
}
