package ru.beru.sortingcenter.ui.acceptance.initial.direct

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.beru.sortingcenter.ui.acceptance.initial.direct.InitialAcceptanceAsserts.`assert cell`
import ru.beru.sortingcenter.ui.acceptance.initial.direct.InitialAcceptanceAsserts.`assert description`
import ru.beru.sortingcenter.ui.acceptance.initial.direct.InitialAcceptanceAsserts.`assert expected date`
import ru.beru.sortingcenter.ui.acceptance.initial.direct.InitialAcceptanceAsserts.`assert label text`
import ru.beru.sortingcenter.ui.acceptance.initial.direct.InitialAcceptanceAsserts.`assert label`
import ru.beru.sortingcenter.ui.acceptance.initial.direct.InitialAcceptanceAsserts.`assert scanner fragment`
import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.data.order.Order
import ru.yandex.market.sc.core.data.place.Place
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.feature.blocking.data.OrderInformation
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.beru.sortingcenter.ui.acceptance.initial.direct.scanner.ScannerModeImpl as ScannerMode

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class InitialAcceptanceViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var networkCheckUserUseCases: NetworkCheckUserUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private val stringManager = TestStringManager()
    private lateinit var viewModel: InitialAcceptanceViewModel

    private val notExistedOrderId = IdManager.getExternalId(-1)
    private val possibleOutgoingDateRouteMock = "2020-12-01"
    private val expectedDateMock = "01.12.2020"

    @Before
    fun setUp() {
        viewModel =
            InitialAcceptanceViewModel(
                networkOrderUseCases,
                networkCheckUserUseCases,
                appMetrica,
                stringManager
            )
        InitialAcceptanceAsserts.bind(viewModel, stringManager)
    }

    @After
    fun tearDown() {
        viewModel.forceReset()
    }

    @Test
    fun `wait for order`() = runTest {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.None,
        )
        `assert label`(
            isLabelAvailable = false,
        )
        `assert description`(
            isDescriptionVisible = false,
        )
        `assert cell`(
            shouldShowCell = false,
        )
        `assert expected date`(
            shouldShowExpectedDate = false,
        )
    }

    private fun `wait for order with cell data`(place: Place, cell: Cell? = null) {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = when {
                place.isSortedInCell -> OverlayState.None
                place.isToKeep -> OverlayState.Warning
                else -> OverlayState.Success
            },
        )
        `assert label text`(
            isLabelAvailable = !place.isSortedInCell,
            label = stringManager.getString(R.string.order_external_id, place.externalId),
        )
        `assert description`(
            isDescriptionVisible = false,
        )
        `assert expected date`(
            shouldShowExpectedDate = false,
        )
        `assert cell`(
            shouldShowCell = cell != null,
            cell = cell?.number,
        )
    }

    @Test
    fun `accept order with error`() = runTest {
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(notExistedOrderId)
        val response = TestFactory.getResponseError<Int>(code = 400)

        `when`(networkOrderUseCases.acceptOrder(notExistedOrderId)).thenThrow(HttpException(response))

        viewModel.processScanResult(scanResult)

        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )
        `assert label text`(
            isLabelAvailable = true,
            label = TestFactory.ERROR_MESSAGE,
        )
        `assert description`(
            isDescriptionVisible = false,
        )
        `assert cell`(
            shouldShowCell = false,
        )
        `assert expected date`(
            shouldShowExpectedDate = false,
        )
    }

    @Test
    fun `order not found`() = runTest {
        val orderNotFound = TestFactory.getOrderNotFound(notExistedOrderId)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(notExistedOrderId)
        `when`(networkOrderUseCases.acceptOrder(notExistedOrderId)).thenReturn(orderNotFound)

        viewModel.processScanResult(scanResult)
        `failure order not found`()
    }

    @Test
    fun `scan wrong format without blocking`() = runTest {
        `when`(networkCheckUserUseCases.shouldBlockOnAcceptance()).thenReturn(false)
        val scanResult = ScanResultFactory.getUnsupportedScanResult(123)

        viewModel.processScanResult(scanResult)
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )
        `assert label`(isLabelAvailable = true, label = R.string.wrong_scan_format_barcode)
    }

    @Test
    fun `scan wrong format with blocking`() = runTest {
        `when`(networkCheckUserUseCases.shouldBlockOnAcceptance()).thenReturn(true)
        val scanResult = ScanResultFactory.getUnsupportedScanResult(123)

        viewModel.processScanResult(scanResult)
        assertThat(viewModel.wrongScanEvent.getOrAwaitValue().get()).isEqualTo(
            OrderInformation(null)
        )
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.None,
        )
    }

    @Test
    fun `scan wrong format after scan order without blocking`() = runTest {
        val courierCell = TestFactory.getCourierCell()
        val order = TestFactory.createOrderForToday(1).updateCellTo(courierCell).build()
        val place = order.places.first()
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(networkCheckUserUseCases.shouldBlockOnAcceptance()).thenReturn(false)

        val scanOrder = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanResult = ScanResultFactory.getUnsupportedScanResult(123)

        viewModel.processScanResult(scanOrder)
        `use sorting orders screen`(courierCell, place)

        viewModel.processScanResult(scanResult)
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )
        `assert label`(isLabelAvailable = true, label = R.string.wrong_scan_format_barcode)
    }

    @Test
    fun `scan wrong format after scan order with blocking`() = runTest {
        val courierCell = TestFactory.getCourierCell()
        val order = TestFactory.createOrderForToday(1).updateCellTo(courierCell).build()
        val place = order.places.first()
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(networkCheckUserUseCases.shouldBlockOnAcceptance()).thenReturn(true)

        val scanOrder = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanResult = ScanResultFactory.getUnsupportedScanResult(123)

        viewModel.processScanResult(scanOrder)
        `use sorting orders screen`(courierCell, place)

        viewModel.processScanResult(scanResult)
        assertThat(viewModel.wrongScanEvent.getOrAwaitValue().get()).isEqualTo(
            OrderInformation(order.externalId, place.externalId)
        )
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Success,
        )
    }

    @Test
    fun `order return already dispatched`() = runTest {
        val order = TestFactory.getOrderReturnAlreadyDispatched(notExistedOrderId)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(notExistedOrderId)
        `when`(networkOrderUseCases.acceptOrder(notExistedOrderId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `failure order not found`()
    }

    @Test
    fun `accept order but cell not active`() = runTest {
        val courierCell = TestFactory.getNotActiveCell()
        val order = TestFactory.getOrderToCourier(cellTo = courierCell)
        val place = order.places.first()
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `cell not active`(place)
    }

    @Test
    fun `accept multiplace order but cell not active`() = runTest {
        val courierCell = TestFactory.getNotActiveCell()
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 3, cellTo = courierCell)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        viewModel.processScanResult(scanOrderResult)

        `ready for scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, place.externalId)).thenReturn(order)
        viewModel.processScanResult(scanPlaceResult)

        `cell not active`(place)
    }

    @Test
    fun `accept multiplace order by place but cell not active`() = runTest {
        val courierCell = TestFactory.getNotActiveCell()
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 3, cellTo = courierCell)
        val place = order.places.first()
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrder(place.externalId)).thenReturn(order)
        viewModel.processScanResult(scanPlaceResult)

        `cell not active`(place)
    }

    @Test
    fun `accept order with cellTo`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell)
        val place = order.places.first()
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `use sorting orders screen`(courierCell, place)

        viewModel.forceReset()
        `wait for order with cell data`(place, cell = courierCell)
    }

    @Test
    fun `accept order with cellTo and cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = courierCell)
        val place = order.places.first()
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `use sorting orders screen`(courierCell, place)

        viewModel.forceReset()
        `wait for order with cell data`(place)
    }

    @Test
    fun `accept order to courier but in keep cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val bufferCell = TestFactory.getCourierCell("B-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = bufferCell)
        val place = order.places.first()
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `use sorting orders screen`(courierCell, place)

        viewModel.forceReset()
        `wait for order with cell data`(place, cell = courierCell)
    }

    @Test
    fun `accept order to keep not in cell`() = runTest {
        val order =
            TestFactory.getOrderToKeep(possibleOutgoingRouteDate = possibleOutgoingDateRouteMock)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `sort to keep cell`()
    }

    @Test
    fun `accept order to keep in cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.getOrderToKeepInCell(
            cell = bufferCell,
            possibleOutgoingRouteDate = possibleOutgoingDateRouteMock
        )
        val place = order.places.first()
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `order should go to cell`(bufferCell, place)
    }

    @Test
    fun `accept order to keep but in courier cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order =
            TestFactory.getOrderToKeepInCell(
                cell = courierCell,
                possibleOutgoingRouteDate = possibleOutgoingDateRouteMock
            )
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `sort to keep cell`()
    }

    @Test
    fun `accept order dropped`() = runTest {
        val droppedOrder = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDropped(cellTo = droppedOrder)
        val place = order.places.first()
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `order should go to cell`(droppedOrder, place)
    }

    @Test
    fun `accept order dropped in cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDroppedInCell(cellTo = droppedCell, cell = droppedCell)
        val place = order.places.first()
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `order should go to cell`(droppedCell, place)
    }

    @Test
    fun `accept order to courier but in dropped cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = droppedCell)
        val place = order.places.first()
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `use sorting orders screen`(courierCell, place)

        viewModel.forceReset()
        `wait for order with cell data`(place, cell = courierCell)
    }

    @Test
    fun `accept order to buffer but in dropped cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell()
        val order = TestFactory.getOrderToKeepInCell(
            cell = droppedCell,
            possibleOutgoingRouteDate = possibleOutgoingDateRouteMock,
        )
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `sort to keep cell`()
    }


    @Test
    fun `accept multiplace order by place`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToReturn(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val scanPlaceResult = ScanResultFactory.getOrderDefaultScanResult(places[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(places.first().externalId)).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `use sorting orders screen`(courierCell, places[0])
    }

    @Test
    fun `accept multiplace order with cellTo without reset`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(
            order
        )

        viewModel.processScanResult(scanPlaceResult)
        `use sorting orders screen`(courierCell, places[0])
        viewModel.forceReset()
        `wait for order with cell data`(places[0], courierCell)
    }

    @Test
    fun `accept multiplace order with cellTo`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToReturn(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(
            order
        )

        viewModel.processScanResult(scanPlaceResult)
        `use sorting orders screen`(courierCell, places[0])
    }

    @Test
    fun `accept multiplace order with cellTo and cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val placesInCell = places.map { it.copy(cell = courierCell) }
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        viewModel.processScanResult(scanOrderResult)

        val place = placesInCell.first()
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)
        `when`(networkOrderUseCases.acceptOrder(place.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `use sorting orders screen`(courierCell, place)
    }

    @Test
    fun `accept multiplace order to keep not in cell`() = runTest {
        val order =
            TestFactory.getOrderToKeep(
                numberOfPlaces = 2,
                possibleOutgoingRouteDate = possibleOutgoingDateRouteMock
            )
        val places = order.places

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(
            order
        )

        viewModel.processScanResult(scanPlaceResult)
        `sort to keep cell`()
    }

    @Test
    fun `accept multiplace order to keep not in cell without reset`() = runTest {
        val order =
            TestFactory.getOrderToKeep(
                numberOfPlaces = 2,
                possibleOutgoingRouteDate = possibleOutgoingDateRouteMock
            )
        val places = order.places

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(
            order
        )

        viewModel.processScanResult(scanPlaceResult)
        `sort to keep cell`()
    }

    @Test
    fun `accept multiplace order to keep in cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.createOrderForToday(2)
            .keep()
            .sort(bufferCell)
            .updatePossibleOutgoingRouteDate(possibleOutgoingDateRouteMock)
            .build()
        val place = order.places.first()

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        viewModel.processScanResult(scanOrderResult)

        `ready for scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, place.externalId)).thenReturn(order)
        viewModel.processScanResult(scanPlaceResult)

        `use sorting orders screen`(bufferCell, place)
    }

    @Test
    fun `accept multiplace order by place to keep in cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.createOrderForToday(2)
            .keep()
            .sort(bufferCell)
            .updatePossibleOutgoingRouteDate(possibleOutgoingDateRouteMock)
            .build()
        val place = order.places.first()

        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrder(place.externalId)).thenReturn(order)
        viewModel.processScanResult(scanPlaceResult)

        `use sorting orders screen`(bufferCell, place)
    }

    @Test
    fun `accept multiplace order to keep in cell without reset`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.createOrderForToday(2)
            .keep()
            .sort(bufferCell)
            .updatePossibleOutgoingRouteDate(possibleOutgoingDateRouteMock)
            .build()
        val place = order.places.first()

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        viewModel.processScanResult(scanOrderResult)

        `ready for scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, place.externalId)).thenReturn(order)
        viewModel.processScanResult(scanPlaceResult)

        `use sorting orders screen`(bufferCell, place)
    }

    @Test
    fun `accept multiplace order by place to keep in cell without reset`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.createOrderForToday(2)
            .keep()
            .sort(bufferCell)
            .updatePossibleOutgoingRouteDate(possibleOutgoingDateRouteMock)
            .build()
        val place = order.places.first()

        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrder(place.externalId)).thenReturn(order)
        viewModel.processScanResult(scanPlaceResult)

        `use sorting orders screen`(bufferCell, place)
    }

    @Test
    fun `accept multiplace order to courier but in keep cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val placesInCell = places.map { it.copy(cell = bufferCell) }
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult =
            ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        viewModel.forceReset()
        `waiting for place`(orderInCell)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `use sorting orders screen`(courierCell, placesInCell[0])
        viewModel.forceReset()
        `wait for order with cell data`(places[0], courierCell)
    }

    @Test
    fun `accept multiplace order to courier but in keep cell without reset`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val placesInCell = places.map { it.copy(cell = bufferCell) }
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult =
            ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(
            order
        )

        viewModel.processScanResult(scanPlaceResult)
        `use sorting orders screen`(courierCell, placesInCell[0])
        viewModel.forceReset()
        `wait for order with cell data`(places[0], courierCell)
    }

    @Test
    fun `accept multiplace order to courier but partial in keep cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val place1InCell = places[0].copy(cell = bufferCell)
        val place2InCell = places[1].copy(cell = courierCell)

        val placesInCell = listOf(place1InCell, place2InCell)
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult =
            ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        viewModel.forceReset()
        `waiting for place`(orderInCell)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `use sorting orders screen`(courierCell, placesInCell[0])
        viewModel.forceReset()
        `wait for order with cell data`(places[0], courierCell)
    }

    @Test
    fun `accept multiplace order to courier but partial in keep cell without reset`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val place1InCell = places[0].copy(cell = bufferCell)
        val place2InCell = places[1].copy(cell = courierCell)

        val placesInCell = listOf(place1InCell, place2InCell)
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult =
            ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(
            order
        )

        viewModel.processScanResult(scanPlaceResult)
        `use sorting orders screen`(courierCell, placesInCell[0])
        viewModel.forceReset()
        `wait for order with cell data`(places[0], courierCell)

    }

    @Test
    fun `accept multiplace order to courier but in drop cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val placesInCell = places.map { it.copy(cell = droppedCell) }
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult =
            ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        viewModel.forceReset()
        `waiting for place`(orderInCell)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(
            order
        )

        viewModel.processScanResult(scanPlaceResult)
        `use sorting orders screen`(courierCell, placesInCell[0])
        viewModel.forceReset()
        `wait for order with cell data`(places[0], courierCell)
    }

    @Test
    fun `accept multiplace order to courier but in drop cell without reset`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val placesInCell = places.map { it.copy(cell = droppedCell) }
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult =
            ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `use sorting orders screen`(courierCell, placesInCell[0])
        viewModel.forceReset()
        `wait for order with cell data`(places[0], courierCell)
    }

    @Test
    fun `accept multiplace order to courier but partial in drop cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val place1InCell = places[0].copy(cell = droppedCell)
        val place2InCell = places[1].copy(cell = courierCell)

        val placesInCell = listOf(place1InCell, place2InCell)
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult =
            ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        viewModel.forceReset()
        `waiting for place`(orderInCell)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(
            order
        )

        viewModel.processScanResult(scanPlaceResult)
        `use sorting orders screen`(courierCell, placesInCell[0])
        viewModel.forceReset()
        `wait for order with cell data`(places[0], courierCell)
    }

    @Test
    fun `accept multiplace order to courier but partial in drop cell without reset`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val place1InCell = places[0].copy(cell = droppedCell)
        val place2InCell = places[1].copy(cell = courierCell)

        val placesInCell = listOf(place1InCell, place2InCell)
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult =
            ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `use sorting orders screen`(courierCell, placesInCell[0])
        viewModel.forceReset()
        `wait for order with cell data`(places[0], courierCell)
    }

    @Test
    fun `accept multiplace order to buffer but in drop cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell()
        val order = TestFactory.getOrderToKeep(
            numberOfPlaces = 2,
            possibleOutgoingRouteDate = possibleOutgoingDateRouteMock,
        )

        val placesInCell = order.places.map { it.copy(cell = droppedCell) }
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult =
            ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        viewModel.forceReset()
        `waiting for place`(orderInCell)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                order.places[0].externalId
            )
        ).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `sort to keep cell`()
    }

    @Test
    fun `accept multiplace order to buffer but partial in drop cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell()
        val droppedCell = TestFactory.getDroppedCell()
        val order = TestFactory.getOrderToKeep(
            numberOfPlaces = 2,
            possibleOutgoingRouteDate = possibleOutgoingDateRouteMock,
        )
        val places = order.places

        val place1InCell = places[0].copy(cell = droppedCell)
        val place2InCell = places[1].copy(cell = bufferCell)

        val placesInCell = listOf(place1InCell, place2InCell)
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult =
            ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        viewModel.forceReset()
        `waiting for place`(orderInCell)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(
            order
        )

        viewModel.processScanResult(scanPlaceResult)
        `sort to keep cell`()
    }

    @Test
    fun `accept multiplace order to keep but in courier cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order =
            TestFactory.getOrderToKeep(
                numberOfPlaces = 2,
                possibleOutgoingRouteDate = possibleOutgoingDateRouteMock
            )
        val places = order.places

        val placesInCell = places.map { it.copy(cell = courierCell) }
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        viewModel.forceReset()
        `waiting for place`(orderInCell)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `sort to keep cell`()
    }

    @Test
    fun `accept multiplace order to keep but in courier cell without reset`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order =
            TestFactory.getOrderToKeep(
                numberOfPlaces = 2,
                possibleOutgoingRouteDate = possibleOutgoingDateRouteMock
            )
        val places = order.places

        val placesInCell = places.map { it.copy(cell = courierCell) }
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(
            order
        )

        viewModel.processScanResult(scanPlaceResult)
        `sort to keep cell`()
    }

    @Test
    fun `accept multiplace order to keep but partial in courier cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order =
            TestFactory.getOrderToKeep(
                numberOfPlaces = 2,
                possibleOutgoingRouteDate = possibleOutgoingDateRouteMock
            )
        val places = order.places

        val place1InCell = places[0].copy(cell = courierCell)
        val place2InCell = places[1].copy(cell = bufferCell)

        val placesInCell = listOf(place1InCell, place2InCell)
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        viewModel.forceReset()
        `waiting for place`(orderInCell)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(
            order
        )

        viewModel.processScanResult(scanPlaceResult)
        `sort to keep cell`()
    }

    @Test
    fun `accept multiplace order to keep but partial in courier cell without reset`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order =
            TestFactory.getOrderToKeep(
                numberOfPlaces = 2,
                possibleOutgoingRouteDate = possibleOutgoingDateRouteMock
            )
        val places = order.places

        val place1InCell = places[0].copy(cell = courierCell)
        val place2InCell = places[1].copy(cell = bufferCell)

        val placesInCell = listOf(place1InCell, place2InCell)
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `sort to keep cell`()
    }

    @Test
    fun `accept multiplace order to drop not in cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDropped(numberOfPlaces = 2, cellTo = droppedCell)
        val places = order.places

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(
            order
        )

        viewModel.processScanResult(scanPlaceResult)

        `order should go to cell`(droppedCell, places[0])
    }

    @Test
    fun `accept multiplace order to drop not in cell without reset`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDropped(numberOfPlaces = 2, cellTo = droppedCell)
        val places = order.places

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(
            order
        )

        viewModel.processScanResult(scanPlaceResult)
        `order should go to cell`(droppedCell, places[0])
    }

    @Test
    fun `accept multiplace order to drop in cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.createOrderForToday(2)
            .drop(droppedCell)
            .sort(droppedCell)
            .updatePossibleOutgoingRouteDate(possibleOutgoingDateRouteMock)
            .build()
        val place = order.places.first()

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        viewModel.processScanResult(scanOrderResult)

        `ready for scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, place.externalId)).thenReturn(order)
        viewModel.processScanResult(scanPlaceResult)

        `use sorting orders screen`(droppedCell, place)
    }

    @Test
    fun `accept multiplace order by place to drop in cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.createOrderForToday(2)
            .drop(droppedCell)
            .sort(droppedCell)
            .updatePossibleOutgoingRouteDate(possibleOutgoingDateRouteMock)
            .build()
        val place = order.places.first()

        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrder(place.externalId)).thenReturn(order)
        viewModel.processScanResult(scanPlaceResult)

        `use sorting orders screen`(droppedCell, place)
    }

    @Test
    fun `accept multiplace order to drop in cell without reset`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.createOrderForToday(2)
            .drop(droppedCell)
            .sort(droppedCell)
            .updatePossibleOutgoingRouteDate(possibleOutgoingDateRouteMock)
            .build()
        val place = order.places.first()

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        viewModel.processScanResult(scanOrderResult)

        `ready for scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, place.externalId)).thenReturn(order)
        viewModel.processScanResult(scanPlaceResult)

        `use sorting orders screen`(droppedCell, place)
    }

    @Test
    fun `accept multiplace order by place to drop in cell without reset`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.createOrderForToday(2)
            .drop(droppedCell)
            .sort(droppedCell)
            .updatePossibleOutgoingRouteDate(possibleOutgoingDateRouteMock)
            .build()
        val place = order.places.first()

        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrder(place.externalId)).thenReturn(order)
        viewModel.processScanResult(scanPlaceResult)

        `use sorting orders screen`(droppedCell, place)
    }

    @Test
    fun `accept multiplace order to drop but in courier cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDropped(numberOfPlaces = 2, cellTo = droppedCell)
        val places = order.places

        val placesInCell = places.map { it.copy(cell = courierCell) }
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        viewModel.forceReset()
        `waiting for place`(orderInCell)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `order should go to cell`(droppedCell, placesInCell[0])
    }

    @Test
    fun `accept multiplace order to drop but in courier cell without reset`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDropped(numberOfPlaces = 2, cellTo = droppedCell)
        val places = order.places

        val placesInCell = places.map { it.copy(cell = courierCell) }
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(
            order
        )

        viewModel.processScanResult(scanPlaceResult)
        `order should go to cell`(droppedCell, placesInCell[0])
    }

    @Test
    fun `accept multiplace order to drop but partial in courier cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDropped(numberOfPlaces = 2, cellTo = droppedCell)
        val places = order.places

        val place1InCell = places[0].copy(cell = courierCell)
        val place2InCell = places[1].copy(cell = droppedCell)

        val placesInCell = listOf(place1InCell, place2InCell)
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        viewModel.forceReset()
        `waiting for place`(orderInCell)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(
            order
        )

        viewModel.processScanResult(scanPlaceResult)
        `order should go to cell`(droppedCell, placesInCell[0])
    }

    @Test
    fun `accept multiplace order to drop but partial in courier cell without reset`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDropped(numberOfPlaces = 2, cellTo = droppedCell)
        val places = order.places

        val place1InCell = places[0].copy(cell = courierCell)
        val place2InCell = places[1].copy(cell = droppedCell)

        val placesInCell = listOf(place1InCell, place2InCell)
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready for scan place`(orderInCell)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `order should go to cell`(droppedCell, placesInCell[0])
    }

    private fun `failure order not found`() = runTest {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )
        `assert label`(
            isLabelAvailable = true,
            label = R.string.order_not_found,
        )
        `assert description`(
            isDescriptionVisible = false,
        )
        `assert cell`(
            shouldShowCell = false,
        )
        `assert expected date`(
            shouldShowExpectedDate = false,
        )
    }

    private fun `cell not active`(place: Place) {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Warning,
            overlayMessage = R.string.cell_not_active_warning,
        )
        `assert label text`(
            isLabelAvailable = true,
            label = stringManager.getString(
                R.string.order_external_id,
                place.externalId
            ),
        )
        `assert description`(
            isDescriptionVisible = false,
        )
        `assert cell`(
            shouldShowCell = true,
            cell = "null",
        )
        `assert expected date`(
            shouldShowExpectedDate = false,
        )
    }

    private fun `order should go to cell`(cell: Cell, place: Place) {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = when {
                place.isSortedInCell -> OverlayState.Failure
                place.isToKeep -> OverlayState.Warning
                else -> OverlayState.Success
            },
        )
        `assert label text`(
            isLabelAvailable = true,
            label = when {
                place.isSortedInCell -> stringManager.getString(R.string.order_already_scanned)
                place.isToKeep -> stringManager.getString(R.string.keep)
                else -> stringManager.getString(R.string.order_external_id, place.externalId)
            },
        )
        `assert description`(
            isDescriptionVisible = false,
        )
        `assert cell`(
            shouldShowCell = true,
            cell = cell.number,
        )
        `assert expected date`(
            shouldShowExpectedDate = place.isToKeep,
            expectedDate = if (place.isToKeep) expectedDateMock else null,
            dateFormat = if (place.isToKeep) R.string.dispatch_data_unknown else null,
        )
    }

    private fun `sort to keep cell`() = runTest {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Warning,
        )
        `assert label`(
            isLabelAvailable = true,
            label = R.string.keep,
        )
        `assert description`(
            isDescriptionVisible = true,
            description = R.string.cell_any_keep_suitable,
        )
        `assert cell`(
            shouldShowCell = false,
        )
        `assert expected date`(
            shouldShowExpectedDate = true,
            expectedDate = expectedDateMock,
            dateFormat = R.string.dispatch_data_unknown,
        )
    }

    private fun `ready for scan place`(order: Order) {
        `assert scanner fragment`(
            scannerMode = ScannerMode.PlaceBarcode,
            overlayState = OverlayState.Success,
        )
        `assert label text`(
            isLabelAvailable = true,
            label = stringManager.getString(
                R.string.order_external_id,
                order.externalId
            ),
        )
        `assert description`(
            isDescriptionVisible = true,
            description = R.string.scan_second_barcode,
            isInfoButtonVisible = true,
        )
        `assert cell`(
            shouldShowCell = false,
        )
        `assert expected date`(
            shouldShowExpectedDate = false,
            expectedDate = expectedDateMock,
            dateFormat = R.string.dispatch_data_unknown,
        )
    }

    private fun `waiting for place`(order: Order) {
        `assert scanner fragment`(
            scannerMode = ScannerMode.PlaceBarcode,
            overlayState = OverlayState.Success,
        )
        `assert label text`(
            isLabelAvailable = true,
            label = stringManager.getString(
                R.string.order_external_id,
                order.externalId
            ),
        )
        `assert description`(
            isDescriptionVisible = true,
            description = R.string.scan_second_barcode,
            isInfoButtonVisible = true,
        )
        `assert cell`(
            shouldShowCell = false,
        )
        `assert expected date`(
            shouldShowExpectedDate = false,
            expectedDate = expectedDateMock,
            dateFormat = R.string.dispatch_data_unknown,
        )
    }

    private fun `use sorting orders screen`(cell: Cell, place: Place) {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = when {
                place.isSortedInCell -> OverlayState.Failure
                place.isToKeep -> OverlayState.Warning
                else -> OverlayState.Success
            },
        )
        `assert label text`(
            isLabelAvailable = true,
            label = when {
                place.isSortedInCell -> stringManager.getString(R.string.order_already_scanned)
                else -> stringManager.getString(R.string.order_external_id, place.externalId)
            },
        )
        `assert description`(
            isDescriptionVisible = false,
        )
        `assert cell`(
            shouldShowCell = true,
            cell = cell.number,
        )
        `assert expected date`(
            shouldShowExpectedDate = place.isToKeep,
            expectedDate = expectedDateMock,
            dateFormat = R.string.dispatch_data_unknown,
        )
    }
}
