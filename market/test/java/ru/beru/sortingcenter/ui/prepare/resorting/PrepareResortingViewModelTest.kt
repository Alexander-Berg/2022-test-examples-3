package ru.beru.sortingcenter.ui.prepare.resorting

import androidx.lifecycle.Observer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.*
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.beru.sortingcenter.ui.common.scannerbuttons.ScannerButtonsViewModel.CenterButtonState
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel.DescriptionStatus
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel.LabelStatus
import ru.beru.sortingcenter.ui.prepare.data.cache.PrepareCellCache
import ru.beru.sortingcenter.ui.prepare.data.cache.PrepareOrderListCache
import ru.beru.sortingcenter.ui.prepare.resorting.PrepareResortingAsserts.`assert buttons`
import ru.beru.sortingcenter.ui.prepare.resorting.PrepareResortingAsserts.`assert description`
import ru.beru.sortingcenter.ui.prepare.resorting.PrepareResortingAsserts.`assert label`
import ru.beru.sortingcenter.ui.prepare.resorting.PrepareResortingAsserts.`assert scanner`
import ru.beru.sortingcenter.ui.prepare.resorting.PrepareResortingAsserts.bind
import ru.yandex.market.sc.core.data.cell.CellWithOrders
import ru.yandex.market.sc.core.data.order.Order
import ru.yandex.market.sc.core.data.order.OrderItem
import ru.yandex.market.sc.core.data.order.RouteOrderId
import ru.yandex.market.sc.core.data.place.Place
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSortableUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.core.utils.ext.map
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.beru.sortingcenter.ui.prepare.resorting.model.scanner.ScannerModeImpl as ScannerMode

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class PrepareResortingViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var networkSortableUseCases: NetworkSortableUseCases

    @Mock
    private lateinit var cellCache: PrepareCellCache

    @Mock
    private lateinit var orderListCache: PrepareOrderListCache

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: PrepareResortingViewModel
    private val stringManager = TestStringManager()

    private val courierCell = TestFactory.getCourierCell()
    private val cell = TestFactory.mapToCellWithOrders(courierCell)

    private val routeOrderIds = TestFactory.getRouteOrderIds()

    private val universalObserver = Observer { _: Any -> }

    @Before
    fun setUp() = runBlocking {
        `when`(networkOrderUseCases.getRouteOrderIdsByCellId(anyLong())).thenReturn(routeOrderIds)

        initViewModel(cell)
        // наблюдаем orderItemsResult, потому что от него зависит, показывается ли кнопка со списком заказов
        viewModel.orderItemsResult.observeForever(universalObserver)
        viewModel.scanner.scanMode.observeForever(universalObserver)
    }

    @After
    fun tearDown() {
        viewModel.forceReset()
        `waiting for order`()
        viewModel.orderItemsResult.removeObserver(universalObserver)
        viewModel.scanner.scanMode.removeObserver(universalObserver)
    }

    private fun initViewModel(cell: CellWithOrders) {
        `when`(cellCache.value).thenReturn(cell)
        viewModel = PrepareResortingViewModel(
            networkOrderUseCases,
            networkSortableUseCases,
            cellCache,
            appMetrica,
            orderListCache,
            stringManager
        )
        bind(viewModel, stringManager)

        // чтобы при снанировании заказа уже быть подписанным на scanMode
        viewModel.apply {
            assertNotNull(scanner.scanMode.getOrAwaitValue())
        }
    }

    @Test
    fun `waiting for order`() = runTest {
        `assert scanner`(
            scanMode = ScannerMode.OrderBarcode,
        )

        `assert label`(
            label = R.string.scan_order_to_prepare,
            labelStatus = LabelStatus.Neutral,
        )

        `assert description`(
            description = R.string.waiting_dispatch_order,
            descriptionStatus = DescriptionStatus.None,
        )

        `assert buttons`(
            listButtonState = CenterButtonState.ShowList,
        )
    }

    @Test
    fun `prepare order not exist`() = runTest {
        val orderNotExist = TestFactory.getOrderNotFound(IdManager.getExternalId(-1))
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(orderNotExist.externalId)

        `when`(networkOrderUseCases.acceptOrder(orderNotExist.externalId)).thenReturn(orderNotExist)

        viewModel.processScanResult(scanResult)
        `order not exist`()
    }

    @Test
    fun `prepare order not in cell`() = runTest {
        val order = TestFactory.getOrderToCourier(cellTo = courierCell)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `order not in cell`()
    }

    @Test
    fun `prepare order in cell`() = runTest {
        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = courierCell)
        val place = order.places.first()
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `success scan order`(place)

        verify(networkOrderUseCases).preshipOrder(
            requireNotNull(order.id),
            cell.id,
            requireNotNull(cell.routeId),
            place.externalId,
        )
    }

    @Test
    fun `prepare multiplace order not in cell`() = runTest {
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `order not in cell`()
    }

    @Test
    fun `prepare multiplace order partial in cell scan not sorted place`() = runTest {
        val bufferCell = TestFactory.getBufferCell()
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = requireNotNull(order.places)
        val sortedPlace = places[0].copy(cell = courierCell)
        val notSortedPlace = places[1]
        val sortedOrder = order.copy(places = listOf(sortedPlace, notSortedPlace))
        val sortResponse = TestFactory.getSortResponse(bufferCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(sortedOrder.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(notSortedPlace.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        `when`(networkOrderUseCases.acceptOrder(sortedOrder.externalId)).thenReturn(sortedOrder)
        `when`(
            networkSortableUseCases.sort(
                sortedOrder.externalId,
                ExternalId(bufferCell.id),
                notSortedPlace.externalId,
                ignoreTodayRouteOnKeep = true,
            )
        ).thenReturn(sortResponse)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(sortedOrder)

        viewModel.forceReset()
        `waiting for place`(sortedOrder)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(notSortedPlace)

        viewModel.forceReset()
        `waiting for cell`()

        viewModel.processScanResult(scanCellResult)
        `order success accept`()
    }

    @Test
    fun `prepare multiplace order partial in cell scan sorted place`() = runTest {
        val bufferCell = TestFactory.getBufferCell()
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = requireNotNull(order.places)
        val sortedPlace = places[0].copy(cell = courierCell)
        val sortedOrder = order.copy(places = listOf(sortedPlace, places[1]))
        val sortResponse = TestFactory.getSortResponse(bufferCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(sortedOrder.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(sortedPlace.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        `when`(networkOrderUseCases.acceptOrder(sortedOrder.externalId)).thenReturn(sortedOrder)
        `when`(
            networkSortableUseCases.sort(
                sortedOrder.externalId,
                ExternalId(bufferCell.id),
                sortedPlace.externalId,
                ignoreTodayRouteOnKeep = true,
            )
        ).thenReturn(sortResponse)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(sortedOrder)

        viewModel.forceReset()
        `waiting for place`(sortedOrder)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(sortedPlace)

        viewModel.forceReset()
        `waiting for cell`()

        viewModel.processScanResult(scanCellResult)
        `order success accept`()
    }

    @Test
    fun `prepare place of partial order scan not sorted place`() = runTest {
        val bufferCell = TestFactory.getBufferCell()
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = requireNotNull(order.places)
        val sortedPlace = places[0].copy(cell = courierCell)
        val notSortedPlace = places[1]
        val sortedOrder = order.copy(places = listOf(sortedPlace, notSortedPlace))
        val sortResponse = TestFactory.getSortResponse(bufferCell)

        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(notSortedPlace.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        `when`(networkOrderUseCases.acceptOrder(notSortedPlace.externalId)).thenReturn(sortedOrder)
        `when`(
            networkSortableUseCases.sort(
                sortedOrder.externalId,
                ExternalId(bufferCell.id),
                notSortedPlace.externalId,
                ignoreTodayRouteOnKeep = true,
            )
        ).thenReturn(sortResponse)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(notSortedPlace)

        viewModel.forceReset()
        `waiting for cell`()

        viewModel.processScanResult(scanCellResult)
        `order success accept`()
    }

    @Test
    fun `prepare place of partial order scan sorted place`() = runTest {
        val bufferCell = TestFactory.getBufferCell()
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = requireNotNull(order.places)
        val sortedPlace = places[0].copy(cell = courierCell)
        val sortedOrder = order.copy(places = listOf(sortedPlace, places[1]))
        val sortResponse = TestFactory.getSortResponse(bufferCell)

        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(sortedPlace.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        `when`(networkOrderUseCases.acceptOrder(sortedPlace.externalId)).thenReturn(sortedOrder)
        `when`(
            networkSortableUseCases.sort(
                sortedOrder.externalId,
                ExternalId(bufferCell.id),
                sortedPlace.externalId,
                ignoreTodayRouteOnKeep = true,
            )
        ).thenReturn(sortResponse)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(sortedPlace)

        viewModel.forceReset()
        `waiting for cell`()

        viewModel.processScanResult(scanCellResult)
        `order success accept`()
    }

    @Test
    fun `prepare order partial in buffer cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell()
        val order = TestFactory.createOrderForToday(2)
            .sort(0, cell = bufferCell)
            .build()
        val place = order.places.first()
        val sortResponse = TestFactory.getSortResponse(bufferCell)

        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrder(place.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(bufferCell.id),
                place.externalId,
                ignoreTodayRouteOnKeep = true,
            )
        ).thenReturn(sortResponse)

        viewModel.processScanResult(scanPlaceResult)
        `order not in cell`()
    }

    @Test
    fun `prepare multiplace order full in cell`() = runTest {
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val sortedPlaces = order.places.map { it.copy(cell = courierCell) }
        val sortedPlace = sortedPlaces.first()
        val sortedOrder = order.copy(places = sortedPlaces)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(sortedOrder.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(sortedPlaces.first().externalId)

        `when`(networkOrderUseCases.acceptOrder(sortedOrder.externalId)).thenReturn(sortedOrder)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(sortedOrder)

        viewModel.forceReset()
        `waiting for place`(sortedOrder)

        viewModel.processScanResult(scanPlaceResult)
        `success scan order`(sortedPlace)

        verify(networkOrderUseCases).preshipOrder(
            orderId = requireNotNull(sortedOrder.id),
            cellId = cell.id,
            routeId = requireNotNull(cell.routeId),
            placeExternalId = sortedPlace.externalId,
        )
    }

    @Test
    fun `order item with status IN_RIGHT_CELL changes to PREPARED after prepare`() = runTest {
        val order = TestFactory.getOrderOk(cell = courierCell)
        val routeOrderId = TestFactory.getRouteOrderId(
            status = RouteOrderId.Status.IN_RIGHT_CELL,
            baseOnOrder = order
        )
        val routeOrderIds = listOf(routeOrderId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(networkOrderUseCases.getRouteOrderIdsByCellId(courierCell.id)).thenReturn(routeOrderIds)

        initViewModel(cell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(routeOrderId.externalId)
        viewModel.processScanResult(scanOrderResult)

        val orderItem = viewModel.orderItems.byId[routeOrderId.externalId]
        val placeItem = orderItem?.places?.first()
        assertEquals(OrderItem.Status.PREPARED, placeItem?.status)
    }

    @Test
    fun `order item with status NOT_ACCEPTED_AT_SORTING_CENTER or ACCEPTED_AT_SORTING_CENTER keeps it status after prepare`() =
        runTest {
            listOf(
                RouteOrderId.Status.NOT_ACCEPTED_AT_SORTING_CENTER,
                RouteOrderId.Status.ACCEPTED_AT_SORTING_CENTER,
            ).forEach { initialStatus ->
                val order = TestFactory.createOrderForToday(1).updateCellTo(courierCell).build()

                val routeOrderId = TestFactory.getRouteOrderId(
                    baseOnOrder = order,
                    status = initialStatus,
                )
                val routeOrderIds = listOf(routeOrderId)

                `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
                `when`(networkOrderUseCases.getRouteOrderIdsByCellId(courierCell.id)).thenReturn(routeOrderIds)
                `when`(cellCache.value).thenReturn(cell)
                viewModel = PrepareResortingViewModel(
                    networkOrderUseCases,
                    networkSortableUseCases,
                    cellCache,
                    appMetrica,
                    orderListCache,
                    stringManager
                )

                val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(routeOrderId.externalId)
                viewModel.processScanResult(scanOrderResult)

                val orderItem = viewModel.orderItems.byId[routeOrderId.externalId]
                val placeItem = orderItem?.places?.first()
                val expectedStatus = map<RouteOrderId.Status, OrderItem.Status>(initialStatus)
                assertEquals(expectedStatus, placeItem?.status)
            }
        }

    @Test
    fun `multiplace incomplete order item is put to buffer cell after prepare`() = runTest {
        val order = TestFactory.createOrderForToday(2)
            .updateCellTo(courierCell)
            .sort(0)
            .build()

        // тот же заказ, что и в order, но для ручки /orders/list
        val routeOrderId = TestFactory.getRouteOrderId(
            cell = courierCell,
            places = listOf(
                TestFactory.RoutePlaceIdBlueprint(
                    status = RouteOrderId.Status.IN_RIGHT_CELL,
                    cell = courierCell,
                ),
                TestFactory.RoutePlaceIdBlueprint(
                    status = RouteOrderId.Status.NOT_ACCEPTED_AT_SORTING_CENTER
                )
            ),
            baseOnOrder = order
        )
        val routeOrderIds = listOf(routeOrderId)

        val place0 = order.places[0]
        val place0ExternalId = order.places[0].externalId
        val place1ExternalId = order.places[1].externalId

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(routeOrderId.externalId)
        val scanPlace0Result = ScanResultFactory.getPlaceDefaultScanResult(place0ExternalId)

        // ячейка, куда будем перекладывать пришедшие на СЦ коробки неполного многоместного заказа
        val bufferCell = TestFactory.getBufferCell()
        val sortResponse = TestFactory.getSortResponse(bufferCell)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(networkOrderUseCases.getRouteOrderIdsByCellId(cell.id)).thenReturn(routeOrderIds)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(bufferCell.id),
                place0ExternalId,
                ignoreTodayRouteOnKeep = true
            )
        )
            .thenReturn(sortResponse)
        initViewModel(cell)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.processScanResult(scanPlace0Result)
        `ready to scan cell`(place0)

        viewModel.processScanResult(scanCellResult)

        val orderItem = viewModel.orderItems.byId[routeOrderId.externalId]

        val place0Item = orderItem?.placesByExternalId?.get(place0ExternalId.value)

        assertEquals(OrderItem.Status.SHOULD_BE_RESORTED, place0Item?.status)
        assertEquals(bufferCell.number, place0Item?.cellNumber)

        val place1Item = orderItem?.placesByExternalId?.get(place1ExternalId.value)

        assertEquals(OrderItem.Status.NOT_ACCEPTED_AT_SORTING_CENTER, place1Item?.status)
    }

    private fun `success scan order`(place: Place) {
        `assert scanner`(
            scanMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Success,
        )

        `assert label`(
            label = R.string.successfully,
            labelStatus = LabelStatus.Success,
        )

        `assert description`(
            externalId = place.externalId,
            description = R.string.order_success_dispatch,
            descriptionStatus = DescriptionStatus.Neutral,
        )

        `assert buttons`(
            listButtonState = CenterButtonState.ShowList,
        )
    }

    private fun `ready to scan place`(order: Order) {
        `assert scanner`(
            scanMode = ScannerMode.PlaceBarcode,
            overlayState = if (order.partialSorted) OverlayState.Warning else OverlayState.Success,
        )

        `assert label`(
            label = if (order.partialSorted) R.string.keep else R.string.successfully,
            labelStatus = LabelStatus.Neutral,
        )

        `assert description`(
            externalId = order.externalId,
            description = if (order.partialSorted) R.string.partial_multiplace_order else R.string.order_success_dispatch,
            descriptionStatus = DescriptionStatus.WithInfoButton,
        )

        `assert buttons`(
            listButtonState = CenterButtonState.ShowList,
        )
    }

    private fun `waiting for place`(order: Order) {
        `assert scanner`(
            scanMode = ScannerMode.PlaceBarcode,
            overlayState = OverlayState.None,
        )

        `assert label`(
            externalId = order.externalId,
            label = R.string.order_external_id,
            labelStatus = LabelStatus.Neutral,
        )

        `assert description`(
            description = R.string.scan_second_barcode,
            descriptionStatus = DescriptionStatus.WithInfoButton,
        )

        `assert buttons`(
            listButtonState = CenterButtonState.ShowList,
        )
    }

    private fun `ready to scan cell`(place: Place) {
        `assert scanner`(
            scanMode = ScannerMode.CellQRCode,
            overlayState = OverlayState.Warning,
        )

        `assert label`(
            label = R.string.keep,
            labelStatus = LabelStatus.Neutral,
        )

        `assert description`(
            externalId = place.externalId,
            description = R.string.partial_multiplace_order,
            descriptionStatus = DescriptionStatus.Neutral,
        )

        `assert buttons`(
            listButtonState = CenterButtonState.ShowList,
        )
    }

    private fun `waiting for cell`() = runTest {
        `assert scanner`(
            scanMode = ScannerMode.CellQRCode,
            overlayState = OverlayState.None,
        )

        `assert label`(
            label = R.string.cell_any_keep_suitable,
            labelStatus = LabelStatus.None,
        )

        `assert description`(
            description = R.string.partial_multiplace_order_to_keep_cell,
            descriptionStatus = DescriptionStatus.Neutral,
        )

        `assert buttons`(
            listButtonState = CenterButtonState.ShowList,
        )
    }

    private fun `order success accept`() = runTest {
        `assert scanner`(
            scanMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Success,
        )

        `assert label`(
            label = R.string.successfully,
            labelStatus = LabelStatus.Success,
        )

        `assert description`(
            description = R.string.empty,
            descriptionStatus = DescriptionStatus.None,
        )

        `assert buttons`(
            listButtonState = CenterButtonState.ShowList,
        )
    }

    private fun `order not in cell`() = runTest {
        `assert scanner`(
            scanMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )

        `assert label`(
            label = R.string.error,
            labelStatus = LabelStatus.Error,
        )

        `assert description`(
            description = R.string.order_from_wrong_cell,
            descriptionStatus = DescriptionStatus.Neutral,
        )

        `assert buttons`(
            listButtonState = CenterButtonState.ShowList,
        )
    }

    private fun `order not exist`() = runTest {
        `assert scanner`(
            scanMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )

        `assert label`(
            label = R.string.error,
            labelStatus = LabelStatus.Error,
        )

        `assert description`(
            description = R.string.order_not_found,
            descriptionStatus = DescriptionStatus.Neutral,
        )

        `assert buttons`(
            listButtonState = CenterButtonState.ShowList,
        )
    }
}
