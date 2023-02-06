package ru.beru.sortingcenter.ui.inventorying

import com.google.common.truth.Truth.assertThat
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
import ru.beru.sortingcenter.ui.common.scannerbuttons.ScannerButtonsViewModel.CenterButtonState
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel.*
import ru.beru.sortingcenter.ui.inventorying.Asserts.`assert button fragment`
import ru.beru.sortingcenter.ui.inventorying.Asserts.`assert description text`
import ru.beru.sortingcenter.ui.inventorying.Asserts.`assert description`
import ru.beru.sortingcenter.ui.inventorying.Asserts.`assert label`
import ru.beru.sortingcenter.ui.inventorying.Asserts.`assert scanner fragment`
import ru.beru.sortingcenter.ui.inventorying.resorting.InventoryingResortingViewModel
import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.data.log.LogCell
import ru.yandex.market.sc.core.data.order.Order
import ru.yandex.market.sc.core.data.place.PlaceStatus
import ru.yandex.market.sc.core.data.order.RouteOrderId
import ru.yandex.market.sc.core.data.order.RouteOrderIdMapper
import ru.yandex.market.sc.core.data.place.Place
import ru.yandex.market.sc.core.network.domain.NetworkLogUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSortableUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.beru.sortingcenter.ui.inventorying.resorting.models.scanner.ScannerModeImpl as ScannerMode

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class InventoryingResortingViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var networkSortableUseCases: NetworkSortableUseCases

    @Mock
    private lateinit var networkLogUseCases: NetworkLogUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    @Mock
    private lateinit var viewModel: InventoryingResortingViewModel

    private val notExistedOrderId = IdManager.getExternalId(-1)
    private val possibleOutgoingDateMock = "2020-12-01"

    @Before
    fun setUp() {
        viewModel = InventoryingResortingViewModel(
            networkOrderUseCases,
            networkSortableUseCases,
            networkLogUseCases,
            appMetrica,
            stringManager
        )
        viewModel.setResult(Result.Success)
        Asserts.bind(viewModel, stringManager)
    }

    @After
    fun tearDown() {
        viewModel.forceReset()
        `wait for order after reset`(isFreeCell = viewModel.isFreeCellMode)
    }

    @Test
    fun `wait for order (INVENTORYING)`() = runTest {
        val cell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(cell)
        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.None,
        )

        `assert button fragment`(
            centerButtonState = CenterButtonState.ShowList,
        )

        `assert label`(
            labelStatus = LabelStatus.Neutral,
            label = R.string.inventorying_resorting_label
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.None,
            cellState = CellState.None,
        )
    }

    @Test
    fun `wait for order (FREE_CELL)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        viewModel.init(ResortType.FREE_CELL, false, arrayOf(), cellWithOrders)

        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.None,
        )

        `assert button fragment`(
            centerButtonState = CenterButtonState.None,
        )

        `assert label`(
            labelStatus = LabelStatus.Neutral,
            label = R.string.scan_to_free_cell_label
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.None,
            cellState = CellState.None,
        )
    }

    @Test
    fun `inventorying order not found (INVENTORYING)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderNotFound(notExistedOrderId)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `order not found`()
    }

    @Test
    fun `inventorying order with error (INVENTORYING)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderNotFound(notExistedOrderId)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        val response = TestFactory.getResponseError<Int>(code = 400)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenThrow(HttpException(response))

        viewModel.processScanResult(scanResult)
        `scan order with error`()
    }

    @Test
    fun `inventorying order to courier (INVENTORYING)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = courierCell)
        val logOrder = viewModel.getLogOrder(order, PlaceStatus.OK)
        val logCell = LogCell(courierCell.id)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)

        viewModel.processScanResult(scanResult)
        `success inventorying cell`()
    }

    @Test
    fun `inventorying multiplace order to courier (INVENTORYING)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell, cell = courierCell)
        val place = order.places.first()

        val logOrder = viewModel.getLogOrder(order, PlaceStatus.OK)
        val logOrderWithPlace = viewModel.getLogOrder(order, PlaceStatus.OK, place, PlaceStatus.OK)

        val logCell = LogCell(courierCell.id)
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)
        viewModel.processScanResult(scanOrderResult)

        `assert scanner fragment`(
            scannerMode = ScannerMode.PlaceBarcode,
            overlayState = OverlayState.Success,
        )

        `when`(networkOrderUseCases.getOrder(order.externalId, place.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrderWithPlace, logCell)).thenReturn(Unit)
        viewModel.processScanResult(scanPlaceResult)

        `success inventorying cell`()
    }

    @Test
    fun `inventorying order to keep (INVENTORYING)`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderToKeepInCell(
            cell = bufferCell,
            possibleOutgoingRouteDate = possibleOutgoingDateMock,
        )
        val logOrder = viewModel.getLogOrder(order, PlaceStatus.KEEP)
        val logCell = LogCell(bufferCell.id)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)

        viewModel.processScanResult(scanResult)
        `success inventorying cell`()
    }

    @Test
    fun `inventorying multiplace order to keep (INVENTORYING)`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderToKeepInCell(
            numberOfPlaces = 2,
            cell = bufferCell,
            possibleOutgoingRouteDate = possibleOutgoingDateMock,
        )
        val place = order.places.first()

        val logOrder = viewModel.getLogOrder(order, PlaceStatus.OK)
        val logOrderWithPlace = viewModel.getLogOrder(order, PlaceStatus.KEEP, place, PlaceStatus.KEEP)
        val logCell = LogCell(bufferCell.id)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)
        viewModel.processScanResult(scanOrderResult)

        `assert scanner fragment`(
            scannerMode = ScannerMode.PlaceBarcode,
            overlayState = OverlayState.Success,
        )

        `when`(networkOrderUseCases.getOrder(order.externalId, place.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrderWithPlace, logCell)).thenReturn(Unit)
        viewModel.processScanResult(scanPlaceResult)

        `success inventorying cell`()
    }

    @Test
    fun `check if inventorying ended when there are all right orders scanned`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = courierCell)
        val routeOrderId = TestFactory.getRouteOrderId(
            RouteOrderId.Status.IN_RIGHT_CELL,
            courierCell,
            baseOnOrder = order
        )
        val orderItems = listOf(routeOrderId).map(RouteOrderIdMapper::map)
        viewModel.setList(orderItems)
        viewModel.init(ResortType.INVENTORYING, true, arrayOf(), cellWithOrders)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        assertThat(viewModel.finishInventorying.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    @Test
    fun `check if inventorying not ended when there are only wrong orders left`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)
        val order = TestFactory.getOrderToKeep(possibleOutgoingRouteDate = possibleOutgoingDateMock)
        val routeOrderId = TestFactory.getRouteOrderId(
            RouteOrderId.Status.NOT_ACCEPTED_AT_SORTING_CENTER,
            bufferCell,
            baseOnOrder = order
        )
        val orderItems = listOf(routeOrderId).map(RouteOrderIdMapper::map)
        viewModel.setList(orderItems)
        viewModel.init(ResortType.INVENTORYING, true, arrayOf(), cellWithOrders)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        assertThat(viewModel.finishInventorying.value).isEqualTo(null)
    }

    @Test
    fun `inventorying order to courier not in cell (INVENTORYING)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderToCourier(cellTo = courierCell)
        val place = order.places.first()
        val logOrder = viewModel.getLogOrder(order, PlaceStatus.ERROR)
        val logCell = LogCell(courierCell.id)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)

        viewModel.processScanResult(scanResult)
        `order must be sorted`(courierCell, place)
    }

    @Test
    fun `inventorying order to keep not in cell (INVENTORYING)`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderToKeep(possibleOutgoingRouteDate = possibleOutgoingDateMock)
        val place = order.places.first()
        val logOrder = viewModel.getLogOrder(order, PlaceStatus.ERROR)
        val logCell = LogCell(bufferCell.id)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)

        viewModel.processScanResult(scanResult)
        `order should be in another cell`(bufferCell, place)
    }

    @Test
    fun `inventorying order to courier in wrong cell (INVENTORYING)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val actualCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = actualCell)
        val place = order.places.first()
        val logOrder = viewModel.getLogOrder(order, PlaceStatus.ERROR)
        val logCell = LogCell(courierCell.id)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)

        viewModel.processScanResult(scanResult)
        `order must be sorted`(courierCell, place)
    }

    @Test
    fun `inventorying order to courier but in keep cell (INVENTORYING)`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = bufferCell)
        val place = order.places.first()
        val logOrder = viewModel.getLogOrder(order, PlaceStatus.ERROR)
        val logCell = LogCell(courierCell.id)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)

        viewModel.processScanResult(scanResult)
        `order must be sorted`(courierCell, place)
    }

    @Test
    fun `inventorying order to keep but in courier cell (INVENTORYING)`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderToKeepInCell(
            cell = courierCell,
            possibleOutgoingRouteDate = possibleOutgoingDateMock
        )
        val place = order.places.first()
        val logOrder = viewModel.getLogOrder(order, PlaceStatus.ERROR)
        val logCell = LogCell(bufferCell.id)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)

        viewModel.processScanResult(scanResult)
        `order should be in another cell`(bufferCell, place)
    }

    @Test
    fun `inventorying order to drop but in keep cell (INVENTORYING)`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(droppedCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderDroppedInCell(cellTo = droppedCell, cell = bufferCell)
        val place = order.places.first()
        val logOrder = viewModel.getLogOrder(order, PlaceStatus.ERROR)
        val logCell = LogCell(droppedCell.id)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)

        viewModel.processScanResult(scanResult)
        `order must be sorted`(droppedCell, place)
    }

    @Test
    fun `inventorying order to drop from keep cell (INVENTORYING)`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderDropped(cellTo = droppedCell)
        val place = order.places.first()
        val logOrder = viewModel.getLogOrder(order, PlaceStatus.ERROR)
        val logCell = LogCell(bufferCell.id)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)

        viewModel.processScanResult(scanResult)
        `order should be in another cell`(droppedCell, place)
    }

    @Test
    fun `inventorying order to keep but in drop cell (INVENTORYING)`() = runTest {
        val bufferCell = TestFactory.getBufferCell()
        val droppedCell = TestFactory.getDroppedCell()
        val cellWithOrders = TestFactory.mapToCellWithOrders(droppedCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderToKeepInCell(
            cell = droppedCell,
            possibleOutgoingRouteDate = possibleOutgoingDateMock,
        )
        val place = order.places.first()
        val logOrder = viewModel.getLogOrder(order, PlaceStatus.KEEP)
        val logCell = LogCell(droppedCell.id)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)

        viewModel.processScanResult(scanResult)
        `order should be in another cell`(bufferCell, place)
    }

    @Test
    fun `inventorying order not found (FREE_CELL)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        viewModel.init(ResortType.FREE_CELL, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderNotFound(notExistedOrderId)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `order not found`()
    }

    @Test
    fun `inventorying order with error (FREE_CELL)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        viewModel.init(ResortType.FREE_CELL, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderNotFound(notExistedOrderId)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        val response = TestFactory.getResponseError<Int>(code = 400)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenThrow(HttpException(response))

        viewModel.processScanResult(scanResult)
        `scan order with error`()
    }

    @Test
    fun `inventorying order to courier (FREE_CELL)`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        viewModel.init(ResortType.FREE_CELL, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = courierCell)
        val place = order.places.first()
        val sortResponse = TestFactory.getSortResponse(bufferCell)
        val logOrder = viewModel.getLogOrder(order, PlaceStatus.OK)
        val logCell = LogCell(courierCell.id)
        val scanResultOrderId = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanResultCellId = ScanResultFactory.getScanResultQR(bufferCell.id)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(bufferCell.id),
                place.externalId,
                ignoreTodayRouteOnKeep = true,
            )
        ).thenReturn(sortResponse)

        viewModel.processScanResult(scanResultOrderId)
        `ready to free in buffer cell`(order)

        viewModel.forceReset()
        `waiting for cell (FREE_CELL)`(order)

        viewModel.processScanResult(scanResultCellId)
        `success free cell`()
    }

    @Test
    fun `inventorying order to courier without reset (FREE_CELL)`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        viewModel.init(ResortType.FREE_CELL, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = courierCell)
        val place = order.places.first()
        val sortResponse = TestFactory.getSortResponse(bufferCell)
        val logOrder = viewModel.getLogOrder(order, PlaceStatus.OK)
        val logCell = LogCell(courierCell.id)
        val scanResultOrderId = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanResultCellId = ScanResultFactory.getScanResultQR(bufferCell.id)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(bufferCell.id),
                place.externalId,
                ignoreTodayRouteOnKeep = true,
            )
        ).thenReturn(sortResponse)

        viewModel.processScanResult(scanResultOrderId)
        `ready to free in buffer cell`(order)

        viewModel.processScanResult(scanResultCellId)
        `success free cell`()
    }

    @Test
    fun `inventorying order to courier from another cell (FREE_CELL)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val actualCell = TestFactory.getCourierCell("C-2")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        viewModel.init(ResortType.FREE_CELL, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderToCourier(cellTo = actualCell, cell = actualCell)
        val place = order.places.first()
        val logOrder = viewModel.getLogOrder(order, PlaceStatus.ERROR)
        val logCell = LogCell(courierCell.id)
        val scanResultOrderId = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)

        viewModel.processScanResult(scanResultOrderId)
        `order should be in another cell`(actualCell, place)
    }

    @Test
    fun `inventorying order to courier sort in wrong cell (FREE_CELL)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        viewModel.init(ResortType.FREE_CELL, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = courierCell)
        val place = order.places.first()
        val logOrder = viewModel.getLogOrder(order, PlaceStatus.OK)
        val logCell = LogCell(courierCell.id)
        val response = TestFactory.getResponseError<Int>(code = 403)
        val scanResultOrderId = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanResultCellId = ScanResultFactory.getScanResultQR(returnCell.id)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(returnCell.id),
                place.externalId,
                ignoreTodayRouteOnKeep = true,
            )
        ).thenThrow(HttpException(response))

        viewModel.processScanResult(scanResultOrderId)
        `ready to free in buffer cell`(order)

        viewModel.forceReset()
        `waiting for cell (FREE_CELL)`(order)

        viewModel.processScanResult(scanResultCellId)
        `scan cell with error`()

        viewModel.forceReset()
        `waiting for cell (FREE_CELL)`(order)

        viewModel.handleSkip()
    }

    @Test
    fun `inventorying order to courier sort in wrong cell without reset (FREE_CELL)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        viewModel.init(ResortType.FREE_CELL, false, arrayOf(), cellWithOrders)

        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = courierCell)
        val place = order.places.first()
        val logOrder = viewModel.getLogOrder(order, PlaceStatus.OK)
        val logCell = LogCell(courierCell.id)
        val response = TestFactory.getResponseError<Int>(code = 403)
        val scanResultOrderId = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanResultCellId = ScanResultFactory.getScanResultQR(returnCell.id)

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        `when`(networkLogUseCases.sendLog(logOrder, logCell)).thenReturn(Unit)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(returnCell.id),
                place.externalId,
                ignoreTodayRouteOnKeep = true,
            )
        ).thenThrow(HttpException(response))

        viewModel.processScanResult(scanResultOrderId)
        `ready to free in buffer cell`(order)

        viewModel.processScanResult(scanResultCellId)
        `scan cell with error`()

        viewModel.handleSkip()
    }

    @Test
    fun `inventorying multiplace order by place from another cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)
        val order = TestFactory.getOrderDropped(numberOfPlaces = 3, cellTo = droppedCell)
        val place = order.places.first()

        val scanResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)
        `when`(networkOrderUseCases.getOrder(place.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `order should be in another cell`(droppedCell, place)
    }

    @Test
    fun `inventorying multiplace order by place not in cell`() = runTest {
        val courierCell = TestFactory.getCourierCell()
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 3, cellTo = courierCell)
        val place = order.places.first()

        val scanResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)
        `when`(networkOrderUseCases.getOrder(place.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `order must be sorted`(courierCell, place)
    }

    @Test
    fun `inventorying multiplace order by place not in right cell`() = runTest {
        val courierCell = TestFactory.getCourierCell()
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        viewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 3, cellTo = courierCell, cell = courierCell)
        val place = order.places.first()

        val scanResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)
        `when`(networkOrderUseCases.getOrder(place.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `success inventorying cell`()
    }

    private fun `ready to free in buffer cell`(order: Order) {
        `assert scanner fragment`(
            scannerMode = ScannerMode.CellQRCode,
            overlayState = OverlayState.Warning,
        )

        `assert button fragment`(
            centerButtonState = CenterButtonState.None,
        )

        `assert label`(
            labelStatus = LabelStatus.None,
            label = R.string.cell_any_keep_suitable,
            externalId = order.externalId,
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = R.string.cell_any_keep_suitable,
            cellState = CellState.None,
        )
    }

    private fun `wait for order after reset`(isFreeCell: Boolean) {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.None,
        )

        `assert button fragment`(
            centerButtonState = if (isFreeCell) CenterButtonState.None else CenterButtonState.ShowList,
        )

        `assert label`(
            labelStatus = LabelStatus.Neutral,
            label = if (isFreeCell) R.string.scan_to_free_cell_label else R.string.inventorying_resorting_label,
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.None,
            cellState = CellState.None,
        )
    }

    private fun `waiting for cell (FREE_CELL)`(order: Order) {
        `assert scanner fragment`(
            scannerMode = ScannerMode.CellQRCode,
            overlayState = OverlayState.None,
        )

        `assert button fragment`(
            centerButtonState = CenterButtonState.None,
        )

        `assert label`(
            labelStatus = LabelStatus.None,
            label = R.string.cell_any_keep_suitable,
            externalId = order.externalId,
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = R.string.cell_any_keep_suitable,
            cellState = CellState.None,
        )
    }

    private fun `order not found`() = runTest {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )

        `assert button fragment`(
            centerButtonState = CenterButtonState.None,
        )

        `assert label`(
            labelStatus = LabelStatus.Error,
            label = R.string.error,
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = R.string.order_not_found,
            cellState = CellState.None,
        )
    }

    private fun `scan order with error`() = runTest {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )

        `assert button fragment`(
            centerButtonState = CenterButtonState.None,
        )

        `assert label`(
            labelStatus = LabelStatus.Error,
            label = R.string.error,
        )

        `assert description text`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = TestFactory.ERROR_MESSAGE,
            cellState = CellState.None,
        )
    }

    private fun `scan cell with error`() = runTest {
        `assert scanner fragment`(
            scannerMode = ScannerMode.CellQRCode,
            overlayState = OverlayState.Failure,
        )

        `assert button fragment`(
            centerButtonState = CenterButtonState.None,
        )

        `assert label`(
            labelStatus = LabelStatus.Error,
            label = R.string.error,
        )

        `assert description text`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = TestFactory.ERROR_MESSAGE,
            cellState = CellState.None,
        )
    }

    private fun `success inventorying cell`() = runTest {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Success,
        )

        `assert button fragment`(
            centerButtonState = CenterButtonState.None,
        )

        `assert label`(
            labelStatus = LabelStatus.Success,
            label = R.string.successfully,
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = R.string.inventorying_success,
            cellState = CellState.None,
        )
    }

    private fun `success free cell`() = runTest {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Success,
        )

        `assert button fragment`(
            centerButtonState = CenterButtonState.None,
        )

        `assert label`(
            labelStatus = LabelStatus.Success,
            label = R.string.successfully,
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = R.string.order_accepted_in_keep_cell,
            cellState = CellState.None,
        )
    }

    private fun `order must be sorted`(cell: Cell, place: Place) {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Warning,
            overlayMessage = R.string.order_must_be_sorted,
        )

        `assert button fragment`(
            centerButtonState = CenterButtonState.None,
        )

        `assert label`(
            labelStatus = LabelStatus.Neutral,
            label = R.string.order_external_id,
            externalId = place.externalId,
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = R.string.empty,
            cellState = CellState.Neutral,
            cellTitle = cell.number,
        )
    }

    private fun `order should be in another cell`(cell: Cell, place: Place) {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Warning,
            overlayMessage = R.string.order_should_be_in_another_cell,
        )

        `assert button fragment`(
            centerButtonState = CenterButtonState.None,
        )

        `assert label`(
            labelStatus = LabelStatus.Neutral,
            label = R.string.order_external_id,
            externalId = place.externalId,
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = if (place.isToKeep) R.string.cell_any_keep_suitable else R.string.empty,
            cellState = if (place.isToKeep) CellState.None else CellState.Neutral,
            cellTitle = if (!place.isToKeep) cell.number else null,
        )
    }
}
