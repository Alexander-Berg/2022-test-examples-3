package ru.beru.sortingcenter.ui.dispatch.scan

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
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
import ru.beru.sortingcenter.ui.dispatch.data.cache.DispatchRouteCache
import ru.yandex.market.sc.core.data.cell.CellForRouteBase
import ru.yandex.market.sc.core.data.route.OutgoingCourierRouteType
import ru.yandex.market.sc.core.data.route.Route
import ru.yandex.market.sc.core.data.route.RouteType
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkRouteUseCases
import ru.yandex.market.sc.feature.dispatch_magistral_route.data.MagistralRouteInfo
import ru.yandex.market.sc.test.network.mocks.CellForRouteTestFactory
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.beru.sortingcenter.ui.dispatch.scan.models.scanner.ScannerModeImpl as ScannerMode

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class DispatchViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkRouteUseCases: NetworkRouteUseCases

    @Mock
    private lateinit var networkCheckUserUseCases: NetworkCheckUserUseCases

    @Mock
    private lateinit var routeCache: DispatchRouteCache

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: DispatchViewModel

    private val courier = TestFactory.getCourier("Ilia Mazan")
    private val warehouse = TestFactory.getWarehouse("Warehouse")
    private val cells = listOf(TestFactory.getReturnCell(), TestFactory.getReturnCell()).map {
        CellForRouteTestFactory.mapToCellForRouteBase(it, isEmpty = false)
    }
    private val route = TestFactory.getRoute(
        status = Route.Status.IN_PROGRESS,
        cells = cells,
    )

    @Before
    fun setUp() {
        viewModel = DispatchViewModel(
            networkRouteUseCases,
            networkCheckUserUseCases,
            routeCache,
            appMetrica,
            stringManager,
        )
    }

    @After
    fun tearDown() {
        viewModel.forceReset()
    }

    @Test
    fun `waiting for courier scan`() {
        viewModel.init(routeType = RouteType.OUTGOING_COURIER)

        assertScanner(
            scanMode = ScannerMode.CourierQRCode,
        )
        assertTitles()
        assertDestination()
        assertButtons(
            isSelectCourierButtonVisible = true,
            selectButtonLabel = R.string.select_courier,
        )
    }

    @Test
    fun `courier route not started`() = runTest {
        viewModel.init(routeType = RouteType.OUTGOING_COURIER)

        val courierRoute = route.copy(
            courier = courier,
            status = Route.Status.NOT_STARTED,
            cells = route.cells.map { it.copy(empty = true) }
        )
        val scanCourierResult = ScanResultFactory.getScanResultQR(courier.id)

        `when`(networkRouteUseCases.getRouteForCourier(courier.id.toString())).thenReturn(courierRoute)
        `waiting for courier scan`()

        viewModel.processScanResult(scanCourierResult)
        `warning scan not started`(courierRoute, true)

        viewModel.forceReset()
        `warning scan not started`(courierRoute, true)
    }

    @Test
    fun `courier not exist`() = runTest {
        viewModel.init(routeType = RouteType.OUTGOING_COURIER)

        val scanCourierResult = ScanResultFactory.getScanResultQR(courier.id)

        val errorMessage = "courier not exist!"
        val response = TestFactory.getResponseError<Int>(code = 404, errorMessage = errorMessage)
        `when`(networkRouteUseCases.getRouteForCourier(courier.id.toString())).thenThrow(HttpException(response))
        `waiting for courier scan`()

        viewModel.processScanResult(scanCourierResult)
        assertScanner(
            scanMode = ScannerMode.CourierQRCode,
            overlayState = OverlayState.Failure,
        )
        assertTitles(
            isErrorVisible = true,
            error = errorMessage,
        )
        assertDestination()
        assertButtons(
            isSelectCourierButtonVisible = true,
            selectButtonLabel = R.string.select_courier,
        )
    }

    @Test
    fun `cell from another route`() = runTest {
        val wrongCellId = -1L
        viewModel.init(routeType = RouteType.OUTGOING_WAREHOUSE)

        val warehouseRoute = route.copy(warehouseName = warehouse.name)
        val scanCellResult = ScanResultFactory.getScanResultQR(wrongCellId)

        `when`(networkRouteUseCases.getRoute(warehouseRoute.id)).thenReturn(warehouseRoute)
        viewModel.selectRoute(warehouseRoute.id)

        viewModel.processScanResult(scanCellResult)
        assertScanner(
            scanMode = ScannerMode.CellQRCode,
            overlayState = OverlayState.Failure,
        )
        assertTitles(
            isErrorVisible = true,
            error = R.string.cell_wrong_route,
            name = warehouseRoute.name,
        )
        assertDestination(
            destination = warehouseRoute.cells[0].number,
            scannedCells = 1,
            totalCells = warehouseRoute.cells.size,
        )
        assertButtons(
            isSkipButtonVisible = true,
            selectButtonLabel = R.string.select_warehouse,
        )
    }

    @Test
    fun `warehouse route not started`() = runTest {
        viewModel.init(routeType = RouteType.OUTGOING_WAREHOUSE)

        val warehouseRoute = route.copy(
            warehouseName = warehouse.name,
            status = Route.Status.NOT_STARTED,
            cells = route.cells.map { it.copy(empty = true) }
        )

        `when`(networkRouteUseCases.getRoute(warehouseRoute.id)).thenReturn(warehouseRoute)
        viewModel.selectRoute(warehouseRoute.id)
        `warning scan not started`(warehouseRoute, false)
    }

    @Test
    fun `courier route already dispatch`() = runTest {
        viewModel.init(routeType = RouteType.OUTGOING_COURIER)

        val courierRoute = route.copy(
            courier = courier,
            status = Route.Status.FINISHED,
            cells = route.cells.map { it.copy(empty = true) }
        )
        val scanCourierResult = ScanResultFactory.getScanResultQR(courier.id)

        `when`(networkRouteUseCases.getRouteForCourier(courier.id.toString())).thenReturn(courierRoute)
        `waiting for courier scan`()

        viewModel.processScanResult(scanCourierResult)
        `success scan fully dispatch`(courierRoute, true)

        viewModel.forceReset()
        `success scan fully dispatch`(courierRoute, true)
    }

    @Test
    fun `warehouse route already dispatch`() = runTest {
        viewModel.init(routeType = RouteType.OUTGOING_WAREHOUSE)

        val warehouseRoute = route.copy(
            warehouseName = warehouse.name,
            status = Route.Status.FINISHED,
            cells = route.cells.map { it.copy(empty = true) }
        )

        `when`(networkRouteUseCases.getRoute(warehouseRoute.id)).thenReturn(warehouseRoute)
        viewModel.selectRoute(warehouseRoute.id)
        `success scan fully dispatch`(warehouseRoute, false)
    }

    @Test
    fun `courier one not empty cell on route`() = runTest {
        viewModel.init(routeType = RouteType.OUTGOING_COURIER)

        val courierRoute = route.copy(
            courier = courier,
            cells = route.cells.subList(0, 1)
        )
        val scanCourierResult = ScanResultFactory.getScanResultQR(courier.id)

        `when`(networkRouteUseCases.getRouteForCourier(courier.id.toString())).thenReturn(courierRoute)
        `waiting for courier scan`()

        viewModel.processScanResult(scanCourierResult)
        `success courier scan`(courierRoute, courierRoute.cells[0])

        viewModel.forceReset()
        `wait for cell`(courierRoute, courierRoute.cells[0], isCourier = true)
    }

    @Test
    fun `warehouse one not empty cell on route`() = runTest {
        viewModel.init(routeType = RouteType.OUTGOING_WAREHOUSE)

        val warehouseRoute = route.copy(
            warehouseName = warehouse.name,
            cells = route.cells.subList(0, 1)
        )

        `when`(networkRouteUseCases.getRoute(warehouseRoute.id)).thenReturn(warehouseRoute)
        viewModel.selectRoute(warehouseRoute.id)
        `wait for cell`(warehouseRoute, warehouseRoute.cells[0])
    }

    @Test
    fun `courier many cells on route`() = runTest {
        viewModel.init(routeType = RouteType.OUTGOING_COURIER)

        val courierRoute = route.copy(courier = courier)
        val scanCourierResult = ScanResultFactory.getScanResultQR(courier.id)

        `when`(networkRouteUseCases.getRouteForCourier(courier.id.toString())).thenReturn(courierRoute)
        `waiting for courier scan`()

        viewModel.processScanResult(scanCourierResult)
        `success courier scan`(courierRoute, courierRoute.cells[0])

        viewModel.forceReset()
        `wait for cell`(courierRoute, courierRoute.cells[0], isCourier = true)
    }

    @Test
    fun `warehouse many cells on route`() = runTest {
        viewModel.init(routeType = RouteType.OUTGOING_WAREHOUSE)

        val warehouseRoute = route.copy(warehouseName = warehouse.name)

        `when`(networkRouteUseCases.getRoute(warehouseRoute.id)).thenReturn(warehouseRoute)
        viewModel.selectRoute(warehouseRoute.id)
        `wait for cell`(warehouseRoute, warehouseRoute.cells[0])
    }

    @Test
    fun `navigate to select route fragment`() {
        viewModel.init(routeType = RouteType.OUTGOING_COURIER)

        viewModel.selectRoute()
        assertThat(viewModel.selectRoute.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    @Test
    fun `auto navigation to confirmation screen on magistral route`() = runTest {
        viewModel.init(routeType = RouteType.OUTGOING_COURIER)

        val magistralRoute = route.copy(outgoingCourierRouteType = OutgoingCourierRouteType.MAGISTRAL)

        `when`(networkCheckUserUseCases.supportMagistralRoutes()).thenReturn(true)
        `when`(networkRouteUseCases.getRoute(magistralRoute.id)).thenReturn(magistralRoute)
        viewModel.selectRoute(magistralRoute.id)

        assertThat(viewModel.successfulMagistralCellScan.getOrAwaitValue().get())
            .isEqualTo(
                MagistralRouteInfo(
                    magistralRoute.id,
                    magistralRoute.name,
                    magistralRoute.outgoingCourierRouteType,
                    magistralRoute.lotsTotal,
                    magistralRoute.cells,
                )
            )
    }

    @Test
    fun `auto navigation to success screen on empty magistral route`() = runTest {
        viewModel.init(routeType = RouteType.OUTGOING_COURIER)

        val magistralRoute = route.copy(
            outgoingCourierRouteType = OutgoingCourierRouteType.MAGISTRAL,
            cells = route.cells.map { it.copy(empty = true) }
        )

        `when`(networkCheckUserUseCases.supportMagistralRoutes()).thenReturn(true)
        `when`(networkRouteUseCases.getRoute(magistralRoute.id)).thenReturn(magistralRoute)
        viewModel.selectRoute(magistralRoute.id)

        assertThat(viewModel.allCellsScanned.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    @Test
    fun `auto navigation to confirmation screen on dropoff route`() = runTest {
        viewModel.init(routeType = RouteType.OUTGOING_COURIER)

        val magistralRoute = route.copy(outgoingCourierRouteType = OutgoingCourierRouteType.DROPOFF)

        `when`(networkCheckUserUseCases.supportMagistralRoutes()).thenReturn(true)
        `when`(networkRouteUseCases.getRoute(magistralRoute.id)).thenReturn(magistralRoute)
        viewModel.selectRoute(magistralRoute.id)

        assertThat(viewModel.successfulMagistralCellScan.getOrAwaitValue().get()).isEqualTo(
            MagistralRouteInfo(
                magistralRoute.id,
                magistralRoute.name,
                magistralRoute.outgoingCourierRouteType,
                magistralRoute.lotsTotal,
                magistralRoute.cells,
            )
        )
    }

    @Test
    fun `auto navigation to confirmation screen on dropoff with outbounds route`() = runTest {
        viewModel.init(routeType = RouteType.OUTGOING_COURIER)

        val magistralRoute = route.copy(outgoingCourierRouteType = OutgoingCourierRouteType.DROPOFF_WITH_OUTBOUND)

        `when`(networkCheckUserUseCases.supportMagistralRoutes()).thenReturn(true)
        `when`(networkRouteUseCases.getRoute(magistralRoute.id)).thenReturn(magistralRoute)
        viewModel.selectRoute(magistralRoute.id)

        assertThat(viewModel.successfulMagistralCellScan.getOrAwaitValue().get()).isEqualTo(
            MagistralRouteInfo(
                magistralRoute.id,
                magistralRoute.name,
                magistralRoute.outgoingCourierRouteType,
                magistralRoute.lotsTotal,
                magistralRoute.cells,
            )
        )
    }

    @Test
    fun `auto navigation to success screen on empty dropoff route`() = runTest {
        viewModel.init(routeType = RouteType.OUTGOING_COURIER)

        val magistralRoute = route.copy(
            outgoingCourierRouteType = OutgoingCourierRouteType.DROPOFF,
            cells = route.cells.map { it.copy(empty = true) }
        )

        `when`(networkCheckUserUseCases.supportMagistralRoutes()).thenReturn(true)
        `when`(networkRouteUseCases.getRoute(magistralRoute.id)).thenReturn(magistralRoute)
        viewModel.selectRoute(magistralRoute.id)

        assertThat(viewModel.allCellsScanned.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    @Test
    fun `auto navigation to success screen on empty dropoff with outbound route`() = runTest {
        viewModel.init(routeType = RouteType.OUTGOING_COURIER)

        val magistralRoute = route.copy(
            outgoingCourierRouteType = OutgoingCourierRouteType.DROPOFF_WITH_OUTBOUND,
            cells = route.cells.map { it.copy(empty = true) }
        )

        `when`(networkCheckUserUseCases.supportMagistralRoutes()).thenReturn(true)
        `when`(networkRouteUseCases.getRoute(magistralRoute.id)).thenReturn(magistralRoute)
        viewModel.selectRoute(magistralRoute.id)

        assertThat(viewModel.allCellsScanned.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    private fun `success courier scan`(route: Route, cell: CellForRouteBase, scannedCellsCount: Int = 0) {
        assertScanner(
            scanMode = ScannerMode.CellQRCode,
            overlayState = OverlayState.Success,
        )
        assertTitles(
            isNameVisible = true,
            name = route.name,
        )
        assertDestination(
            isDestinationVisible = true,
            destination = cell.number,
            isCellCountLabelVisible = route.cells.size > 1,
            scannedCells = scannedCellsCount + 1,
            totalCells = route.cells.size,
        )
        assertButtons(
            isSkipButtonVisible = true,
            selectButtonLabel = R.string.select_courier
        )
    }

    private fun `wait for cell`(
        route: Route,
        cell: CellForRouteBase,
        scannedCellsCount: Int = 0,
        isCourier: Boolean = false,
    ) {
        assertScanner(
            scanMode = ScannerMode.CellQRCode,
            overlayState = OverlayState.None,
        )
        assertTitles(
            isNameVisible = true,
            name = route.name,
        )
        assertDestination(
            isDestinationVisible = true,
            destination = cell.number,
            isCellCountLabelVisible = route.cells.size > 1,
            scannedCells = scannedCellsCount + 1,
            totalCells = route.cells.size,
        )
        assertButtons(
            isSkipButtonVisible = true,
            selectButtonLabel = if (isCourier) R.string.select_courier else R.string.select_warehouse,
        )
    }

    private fun `warning scan not started`(route: Route, isCourier: Boolean = false) {
        assertScanner(
            scanMode = ScannerMode.DoNotScan,
            overlayState = OverlayState.Warning,
            overlayMessage = R.string.no_orders_to_dispatch,
        )
        assertTitles(
            isNameVisible = true,
            name = route.name,
        )
        assertDestination()
        assertButtons(
            isSkipButtonVisible = true,
            selectButtonLabel = if (isCourier) R.string.select_courier else R.string.select_warehouse,
        )
    }

    private fun `success scan fully dispatch`(route: Route, isCourier: Boolean = false) {
        assertScanner(
            scanMode = ScannerMode.DoNotScan,
            overlayState = OverlayState.Success,
            overlayMessage = R.string.route_already_fully_dispatch,
        )
        assertTitles(
            isNameVisible = true,
            name = route.name,
        )
        assertDestination()
        assertButtons(
            isSkipButtonVisible = true,
            selectButtonLabel = if (isCourier) R.string.select_courier else R.string.select_warehouse,
        )
    }

    private fun assertScanner(
        scanMode: ScannerMode = ScannerMode.DoNotScan,
        overlayState: OverlayState = OverlayState.None,
        overlayMessage: Int? = null,
    ) {
        viewModel.apply {
            assertEquals(scanMode, this.scanMode.getOrAwaitValue())
            assertEquals(overlayState, this.overlayState.getOrAwaitValue())
            val actualMessage = this.overlayMessage.getOrAwaitValue()
            if (overlayMessage != null) {
                requireNotNull(actualMessage)
                assertContextStringEquals(overlayMessage, actualMessage)
            } else {
                assertNull(actualMessage)
            }
        }
    }

    private fun assertTitles(
        isNameVisible: Boolean = false,
        isErrorVisible: Boolean = false,
        name: String = "",
        error: String,
    ) {
        viewModel.apply {
            assertEquals(isNameVisible, this.isNameVisible.getOrAwaitValue())
            assertEquals(isErrorVisible, this.isErrorVisible.getOrAwaitValue())
            assertEquals(name, this.name.getOrAwaitValue())
            assertEquals(
                error,
                this.error.getOrAwaitValue()
            )
        }
    }

    private fun assertTitles(
        isNameVisible: Boolean = false,
        isErrorVisible: Boolean = false,
        name: String = "",
        error: Int = R.string.error,
    ) {
        val errorString = stringManager.getString(error)

        assertTitles(isNameVisible, isErrorVisible, name, errorString)
    }

    private fun assertDestination(
        isDestinationVisible: Boolean = false,
        isCellCountLabelVisible: Boolean = false,
        destination: String = stringManager.getString(R.string.global_cell_without_number),
        scannedCells: Int = 0,
        totalCells: Int = 0,
    ) {
        viewModel.apply {
            assertEquals(isDestinationVisible, this.isDestinationVisible.getOrAwaitValue())
            assertEquals(isCellCountLabelVisible, this.isCellCountLabelVisible.getOrAwaitValue())
            assertEquals(destination, this.destination.getOrAwaitValue())
            assertEquals(scannedCells, this.processedCells.getOrAwaitValue())
            assertEquals(totalCells, this.totalCells.getOrAwaitValue())
        }
    }

    private fun assertButtons(
        isSkipButtonVisible: Boolean = false,
        isSelectCourierButtonVisible: Boolean = false,
        selectButtonLabel: Int = R.string.select_warehouse,
    ) {
        viewModel.apply {
            assertEquals(isSkipButtonVisible, this.isSkipButtonVisible.getOrAwaitValue())
            assertEquals(isSelectCourierButtonVisible, this.isSelectCourierButtonVisible.getOrAwaitValue())
            assertContextStringEquals(selectButtonLabel, this.selectButtonLabel)
        }
    }

    private fun assertContextStringEquals(
        expected: Int,
        actual: Int,
    ) {
        assertNotNull(stringManager.getString(expected))
        assertEquals(
            stringManager.getString(expected),
            stringManager.getString(actual),
        )
    }
}
