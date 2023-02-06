package ru.beru.sortingcenter.ui.move

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel.*
import ru.beru.sortingcenter.ui.move.Asserts.`assert description text`
import ru.beru.sortingcenter.ui.move.Asserts.`assert description`
import ru.beru.sortingcenter.ui.move.Asserts.`assert expected date`
import ru.beru.sortingcenter.ui.move.Asserts.`assert label`
import ru.beru.sortingcenter.ui.move.Asserts.`assert scanner fragment`
import ru.beru.sortingcenter.ui.move.orders.MoveOrdersViewModel
import ru.yandex.market.sc.core.data.order.Order
import ru.yandex.market.sc.core.data.place.Place
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSortableUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.beru.sortingcenter.ui.move.orders.models.scanner.ScannerModeImpl as ScannerMode

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class MoveOrdersViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var networkSortableUseCases: NetworkSortableUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: MoveOrdersViewModel

    private val notExistedOrderId = IdManager.getExternalId(-1)
    private val possibleOutgoingDateRouteMock = "2020-12-01"
    private val expectedDateMock = "01.12.2020"

    @Before
    fun setUp() {
        viewModel = MoveOrdersViewModel(networkOrderUseCases, networkSortableUseCases, appMetrica, stringManager)
        Asserts.bind(viewModel, stringManager)
    }

    @After
    fun tearDown() {
        viewModel.forceReset()
        `wait for order`()
    }

    @Test
    fun `wait for order`() = runTest {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.None,
        )

        `assert label`(
            labelStatus = LabelStatus.None,
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.None,
            cellState = CellState.None
        )

        `assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    @Test
    fun `scan not exist order`() = runTest {
        val orderNotFound = TestFactory.getOrderNotFound(notExistedOrderId)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(notExistedOrderId)

        `when`(networkOrderUseCases.getOrder(notExistedOrderId)).thenReturn(orderNotFound)

        viewModel.processScanResult(scanResult)
        `order not found`()
    }

    @Test
    fun `scan order with wrong status`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `wrong order status`()
    }

    @Test
    fun `keep order move`() = runTest {
        val status = "KEEP"
        val bufferCellBefore = TestFactory.getBufferCell("B-1")
        val order = TestFactory.getOrderToKeepInCell(
            possibleOutgoingRouteDate = possibleOutgoingDateRouteMock,
            cell = bufferCellBefore
        )
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val bufferCellAfter = TestFactory.getBufferCell("B-2")
        val sortResponse = TestFactory.getSortResponse(bufferCellAfter)
        val movedPlace = place.copy(cell = bufferCellAfter)
        val movedOrder = order.copy(places = listOf(movedPlace))
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCellAfter.id)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(bufferCellAfter.id),
                place.externalId
            )
        ).thenReturn(sortResponse)

        viewModel.processScanResult(scanOrderResult)
        `waiting for action`(place, status)

        viewModel.move()
        `waiting for cell`(status)

        viewModel.processScanResult(scanCellResult)
        `success moving`(movedOrder)
    }

    @Test
    fun `multiplace keep order move`() = runTest {
        val status = "KEEP"
        val bufferCellBefore = TestFactory.getBufferCell("B-1")
        val order = TestFactory.getOrderToKeepInCell(
            numberOfPlaces = 2,
            possibleOutgoingRouteDate = possibleOutgoingDateRouteMock,
            cell = bufferCellBefore
        )
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        val bufferCellAfter = TestFactory.getBufferCell("B-1")
        val sortResponse = TestFactory.getSortResponse(bufferCellAfter)
        val movedPlaces = order.places.map {
            if (it.externalId == place.externalId) place.copy(cell = bufferCellAfter) else it
        }
        val movedOrder = order.copy(places = movedPlaces)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCellAfter.id)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(bufferCellAfter.id),
                place.externalId
            )
        ).thenReturn(sortResponse)

        viewModel.processScanResult(scanOrderResult)
        `waiting for scan place`()

        `when`(networkOrderUseCases.getOrder(order.externalId, place.externalId)).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `waiting for action`(place, status)

        viewModel.move()
        `waiting for cell`(status)

        viewModel.processScanResult(scanCellResult)
        `success moving`(movedOrder)
    }


    @Test
    fun `return order move`() = runTest {
        val status = "RETURN"
        val returnCellBefore = TestFactory.getReturnCell("R-1")
        val returnCellAfter = TestFactory.getReturnCell("R-2")
        val order =
            TestFactory.getCanceledOrder(amountOfAvailableCells = 3, cell = returnCellBefore)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val sortResponse = TestFactory.getSortResponse(returnCellAfter)
        val movedPlace = place.copy(cell = returnCellAfter)
        val movedOrder = order.copy(places = listOf(movedPlace))
        val scanCellResult = ScanResultFactory.getScanResultQR(returnCellAfter.id)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(returnCellAfter.id),
                place.externalId
            )
        ).thenReturn(sortResponse)

        viewModel.processScanResult(scanOrderResult)
        `waiting for action`(place, status)

        viewModel.move()
        `waiting for cell`(status)

        viewModel.processScanResult(scanCellResult)
        `success moving`(movedOrder)
    }

    @Test
    fun `multiplace return order move`() = runTest {
        val status = "RETURN"
        val returnCellBefore = TestFactory.getReturnCell("R-1")
        val returnCellAfter = TestFactory.getReturnCell("R-1")
        val order =
            TestFactory.getCanceledOrder(numberOfPlaces = 2, amountOfAvailableCells = 3, cell = returnCellBefore)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)
        val sortResponse = TestFactory.getSortResponse(returnCellAfter)
        val movedPlaces = order.places.map {
            if (it.externalId == place.externalId) place.copy(cell = returnCellAfter) else it
        }
        val movedOrder = order.copy(places = movedPlaces)
        val scanCellResult = ScanResultFactory.getScanResultQR(returnCellAfter.id)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(returnCellAfter.id),
                place.externalId
            )
        ).thenReturn(sortResponse)

        viewModel.processScanResult(scanOrderResult)
        `waiting for scan place`()

        `when`(networkOrderUseCases.getOrder(order.externalId, place.externalId)).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `waiting for action`(place, status)

        viewModel.move()
        `waiting for cell`(status)

        viewModel.processScanResult(scanCellResult)
        `success moving`(movedOrder)
    }

    @Test
    fun `multiplace order scan by place`() = runTest {
        val status = "RETURN"
        val returnCellBefore = TestFactory.getReturnCell("R-1")
        val returnCellAfter = TestFactory.getReturnCell("R-1")
        val order =
            TestFactory.getCanceledOrder(numberOfPlaces = 2, amountOfAvailableCells = 3, cell = returnCellBefore)
        val place = order.places.first()
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)
        val sortResponse = TestFactory.getSortResponse(returnCellAfter)
        val movedPlaces = order.places.map {
            if (it.externalId == place.externalId) place.copy(cell = returnCellAfter) else it
        }
        val movedOrder = order.copy(places = movedPlaces)
        val scanCellResult = ScanResultFactory.getScanResultQR(returnCellAfter.id)

        `when`(networkOrderUseCases.getOrder(place.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(returnCellAfter.id),
                place.externalId
            )
        ).thenReturn(sortResponse)

        viewModel.processScanResult(scanPlaceResult)
        `waiting for action`(place, status)

        viewModel.move()
        `waiting for cell`(status)

        viewModel.processScanResult(scanCellResult)
        `success moving`(movedOrder)
    }

    @Test
    fun `return order without available cells to move`() = runTest {
        val returnCellBefore = TestFactory.getReturnCell("R-1")
        val returnCellAfter = TestFactory.getReturnCell("R-2")
        val order =
            TestFactory.getCanceledOrder(amountOfAvailableCells = 0, cell = returnCellBefore)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val sortResponse = TestFactory.getSortResponse(returnCellAfter)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(returnCellAfter.id),
                place.externalId
            )
        ).thenReturn(sortResponse)

        viewModel.processScanResult(scanOrderResult)
        `return order without available cells`()
    }

    @Test
    fun `scan order with error`() = runTest {
        val order = TestFactory.getOrderNotFound(notExistedOrderId)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        val response = TestFactory.getResponseError<Int>(code = 400)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenThrow(HttpException(response))

        viewModel.processScanResult(scanResult)
        `scan error`()
    }

    private fun `waiting for action`(place: Place, status: String) {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Warning,
        )

        `assert label`(
            labelStatus = LabelStatus.Neutral,
            label = when (status) {
                "KEEP" -> R.string.keep
                "RETURN" -> R.string.return_str
                else -> R.string.error
            }
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = R.string.move_order_click_btn,
            cellState = CellState.None
        )

        `assert expected date`(
            shouldShowExpectedDate = place.isToKeep,
            shouldShowExpectedDateTitle = false,
            dateFormat = if (place.isToKeep) R.string.dispatch_data_unknown else null,
            expectedDate = if (place.isToKeep) expectedDateMock else null,
        )
    }

    private fun `waiting for cell`(status: String) {
        `assert scanner fragment`(
            scannerMode = ScannerMode.CellQRCode,
            overlayState = OverlayState.None,
        )

        `assert label`(
            labelStatus = LabelStatus.None,
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = when (status) {
                "KEEP" -> R.string.move_order_pick_cell
                "RETURN" -> R.string.move_order_pick_cell_from_list
                else -> R.string.error
            },
            cellState = CellState.None
        )

        `assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }


    private fun `success moving`(order: Order) {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Success,
        )

        `assert label`(
            labelStatus = LabelStatus.Success,
            label = R.string.successfully
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = R.string.order_moved_scan_next,
            cellState = CellState.Neutral,
            cellTitle = requireNotNull(order.places.first().cell?.number)
        )

        `assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    private fun `return order without available cells`() = runTest {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )

        `assert label`(
            labelStatus = LabelStatus.Error,
            label = R.string.error
        )
        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = R.string.move_order_without_cells,
            cellState = CellState.None,
        )
    }

    private fun `order not found`() = runTest {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )

        `assert label`(
            labelStatus = LabelStatus.Error,
            label = R.string.error
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = R.string.order_not_found,
        )

        `assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    private fun `waiting for scan place`() = runTest {
        `assert scanner fragment`(
            scannerMode = ScannerMode.PlaceBarcode,
            overlayState = OverlayState.None,
        )

        `assert label`(
            labelStatus = LabelStatus.None,
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.WithInfoButton,
            description = R.string.scan_second_barcode,
        )

        `assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    private fun `wrong order status`() = runTest {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )

        `assert label`(
            labelStatus = LabelStatus.Error,
            label = R.string.error,
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = R.string.order_have_wrong_status_to_move,
            cellState = CellState.None
        )
    }

    private fun `scan error`() = runTest {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )

        `assert label`(
            labelStatus = LabelStatus.Error,
            label = R.string.error,
        )

        `assert description text`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = TestFactory.ERROR_MESSAGE,
            cellState = CellState.None
        )
    }
}
