package ru.beru.sortingcenter.ui.dispatch.resorting

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.ui.common.scannerbuttons.ScannerButtonsViewModel.CenterButtonState
import ru.beru.sortingcenter.ui.dispatch.resorting.Asserts.`assert button fragment`
import ru.beru.sortingcenter.ui.dispatch.resorting.Asserts.`assert expected date`
import ru.beru.sortingcenter.ui.dispatch.resorting.Asserts.`assert initial state`
import ru.beru.sortingcenter.ui.dispatch.resorting.Asserts.`assert label`
import ru.beru.sortingcenter.ui.dispatch.resorting.Asserts.`assert scanned place count`
import ru.beru.sortingcenter.ui.dispatch.resorting.Asserts.`assert scanner fragment`
import ru.beru.sortingcenter.ui.dispatch.resorting.mocks.NetworkUseCasesMock
import ru.beru.sortingcenter.ui.dispatch.resorting.mocks.StringManagerMock
import ru.beru.sortingcenter.ui.dispatch.resorting.mocks.ViewModelMockBuilder
import ru.beru.sortingcenter.ui.dispatch.resorting.models.ScannerModeImpl
import ru.yandex.market.sc.core.data.order.OrderShipStatus
import ru.yandex.market.sc.core.data.route.RouteType
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.test.network.mocks.CellForRouteTestFactory
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.OrderResortInfoTestFactory
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class DispatchResortingScanOrderTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val networkUseCases = NetworkUseCasesMock()

    private val stringManager = StringManagerMock(
        R.string.dispatch_not_ready,
        R.string.error,
        R.string.finish_dispatch,
        R.string.order_already_scanned,
        R.string.successfully,
    )

    private val viewModelMockBuilder = ViewModelMockBuilder(stringManager, networkUseCases)

    private val routeOrderIds = TestFactory.getRouteOrderIds()

    @Before
    fun setUp() {
        runBlocking {
            `when`(networkUseCases.order.getRouteOrderIdsByCellId(ArgumentMatchers.anyLong()))
                .thenReturn(routeOrderIds)
        }
    }

    @Test
    fun `scan order ready to dispatch (OUTGOING_COURIER)`() {
        `scan order ready to dispatch`(RouteType.OUTGOING_COURIER)
    }

    @Test
    fun `scan order ready to dispatch (OUTGOING_WAREHOUSE)`() {
        `scan order ready to dispatch`(RouteType.OUTGOING_WAREHOUSE)
    }

    private fun `scan order ready to dispatch`(routeType: RouteType) = runTest {
        val cell = TestFactory.getCourierCell("C-1")

        val orderResortInfo = OrderResortInfoTestFactory.get()
        val orderExternalId = orderResortInfo.orderExternalId

        val cellForRoute = CellForRouteTestFactory.forCell(cell)
            .withOrder(OrderShipStatus.OK, false, orderExternalId)
            .withPlace(inCell = true)
            .build()
            .build()
        val cellForRouteBase = CellForRouteTestFactory.mapToCellForRouteBase(cell, null, 2, false)
        val route = TestFactory.getRoute(cells = listOf(cellForRouteBase))

        val viewModel = viewModelMockBuilder
            .create(cellForRoute, route, routeType)

        val scanResult = ScanResultFactory.getOrderDefaultScanResult(orderExternalId)

        `when`(networkUseCases.order.getOrderResortInfo(orderExternalId, null, route.id))
            .thenReturn(orderResortInfo)
        viewModel.processScanResult(scanResult)

        `assert scanner fragment`(
            scannerMode = ScannerModeImpl.Dispatchable,
            overlayState = OverlayState.Success,
        )
        `assert button fragment`(
            centerButtonState = CenterButtonState.ShowList,
        )
        `assert label`(
            isLabelAvailable = true,
            label = R.string.successfully,
        )
        `assert scanned place count`(
            isScannedPlaceCountTextVisible = false,
            isResetPlaceTextVisible = false
        )
        `assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )

        viewModel.forceReset()
        `assert initial state`(dispatchCanBeFinished = true)
    }

    @Test
    fun `scan order not in cell (OUTGOING_COURIER)`() {
        `scan order not in cell`(RouteType.OUTGOING_COURIER)
    }

    @Test
    fun `scan order not in cell (OUTGOING_WAREHOUSE)`() {
        `scan order not in cell`(RouteType.OUTGOING_WAREHOUSE)
    }

    private fun `scan order not in cell`(routeType: RouteType) = runTest {
        val cell = TestFactory.getCourierCell("C-1")

        val cellForRoute = CellForRouteTestFactory.mapToCellForRoute(cell, orders = mapOf())
        val cellForRouteBase = CellForRouteTestFactory.mapToCellForRouteBase(cell, null, 2, false)
        val route = TestFactory.getRoute(cells = listOf(cellForRouteBase))

        val viewModel = viewModelMockBuilder
            .create(cellForRoute, route, routeType)

        val orderExternalId = IdManager.getId().let(IdManager::getExternalId)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(orderExternalId)

        viewModel.processScanResult(scanResult)

        `assert scanner fragment`(
            scannerMode = ScannerModeImpl.Dispatchable,
            overlayState = OverlayState.Failure,
        )
        `assert button fragment`(
            centerButtonState = CenterButtonState.None,
        )
        `assert label`(
            isLabelAvailable = true,
            label = R.string.dispatch_not_ready,
        )
        `assert scanned place count`(
            isScannedPlaceCountTextVisible = false,
            isResetPlaceTextVisible = false
        )
        `assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )

        viewModel.forceReset()
        `assert initial state`(dispatchCanBeFinished = true)
    }

    @Test
    fun `scan order already scanned after waiting for reset (OUTGOING_COURIER)`() {
        `scan order already scanned after waiting for reset`(RouteType.OUTGOING_COURIER)
    }

    @Test
    fun `scan order already scanned after waiting for reset (OUTGOING_WAREHOUSE)`() {
        `scan order already scanned after waiting for reset`(RouteType.OUTGOING_WAREHOUSE)
    }

    private fun `scan order already scanned after waiting for reset`(routeType: RouteType) = runTest {
        val cell = TestFactory.getCourierCell("C-1")

            val orderResortInfo = OrderResortInfoTestFactory.get()
            val orderExternalId = orderResortInfo.orderExternalId

        val cellForRoute = CellForRouteTestFactory.forCell(cell)
            .withOrder(OrderShipStatus.OK, false, orderExternalId)
            .withPlace(inCell = true)
            .build()
            .build()
        val cellForRouteBase = CellForRouteTestFactory.mapToCellForRouteBase(cell, null, 2, false)
        val route = TestFactory.getRoute(cells = listOf(cellForRouteBase))

            val viewModel = viewModelMockBuilder
                .create(cellForRoute, route, routeType)

            val scanResult = ScanResultFactory.getOrderDefaultScanResult(orderExternalId)

            `when`(networkUseCases.order.getOrderResortInfo(orderExternalId, null, route.id))
                .thenReturn(orderResortInfo)

            viewModel.processScanResult(scanResult)
            viewModel.forceReset()

            viewModel.processScanResult(scanResult)

            `assert scanner fragment`(
                scannerMode = ScannerModeImpl.Dispatchable,
                overlayState = OverlayState.Failure,
            )
            `assert button fragment`(
                centerButtonState = CenterButtonState.None,
            )
            `assert label`(
                isLabelAvailable = true,
                label = R.string.order_already_scanned,
            )
            `assert scanned place count`(
                isScannedPlaceCountTextVisible = false,
                isResetPlaceTextVisible = false
            )
            `assert expected date`(
                shouldShowExpectedDate = false,
                shouldShowExpectedDateTitle = false,
            )

            viewModel.forceReset()
            `assert initial state`(dispatchCanBeFinished = true)
        }

    @Test
    fun `scan order already scanned without waiting for reset (OUTGOING_COURIER)`() {
        `scan order already scanned without waiting for reset`(RouteType.OUTGOING_COURIER)
    }

    @Test
    fun `scan order already scanned without waiting for reset (OUTGOING_WAREHOUSE)`() {
        `scan order already scanned without waiting for reset`(RouteType.OUTGOING_WAREHOUSE)
    }

    private fun `scan order already scanned without waiting for reset`(routeType: RouteType) = runTest {
        val cell = TestFactory.getCourierCell("C-1")

            val orderResortInfo = OrderResortInfoTestFactory.get()
            val orderExternalId = orderResortInfo.orderExternalId

        val cellForRoute = CellForRouteTestFactory.forCell(cell)
            .withOrder(OrderShipStatus.OK, false, orderExternalId)
            .withPlace(inCell = true)
            .build()
            .build()
        val cellForRouteBase = CellForRouteTestFactory.mapToCellForRouteBase(cell, null, 2, false)
        val route = TestFactory.getRoute(cells = listOf(cellForRouteBase))

            val viewModel = viewModelMockBuilder
                .create(cellForRoute, route, routeType)

            val scanResult = ScanResultFactory.getOrderDefaultScanResult(orderExternalId)

            `when`(networkUseCases.order.getOrderResortInfo(orderExternalId, null, route.id))
                .thenReturn(orderResortInfo)
            viewModel.processScanResult(scanResult)
            viewModel.processScanResult(scanResult)

            `assert scanner fragment`(
                scannerMode = ScannerModeImpl.Dispatchable,
                overlayState = OverlayState.Failure,
            )
            `assert button fragment`(
                centerButtonState = CenterButtonState.None,
            )
            `assert label`(
                isLabelAvailable = true,
                label = R.string.order_already_scanned,
            )
            `assert scanned place count`(
                isScannedPlaceCountTextVisible = false,
                isResetPlaceTextVisible = false
            )
            `assert expected date`(
                shouldShowExpectedDate = false,
                shouldShowExpectedDateTitle = false,
            )

            viewModel.forceReset()
            `assert initial state`(dispatchCanBeFinished = true)
        }

    @Test
    fun `scan order with error (OUTGOING_COURIER)`() {
        `scan order with error`(RouteType.OUTGOING_COURIER)
    }

    @Test
    fun `scan order with error (OUTGOING_WAREHOUSE)`() {
        `scan order with error`(RouteType.OUTGOING_WAREHOUSE)
    }

    private fun `scan order with error`(routeType: RouteType) = runTest {
        val cell = TestFactory.getCourierCell("C-1")

        val orderResortInfo = OrderResortInfoTestFactory.get()
        val orderExternalId = orderResortInfo.orderExternalId

        val cellForRoute = CellForRouteTestFactory.forCell(cell)
            .withOrder(OrderShipStatus.OK, false, orderExternalId)
            .withPlace(inCell = true)
            .build()
            .build()
        val cellForRouteBase = CellForRouteTestFactory.mapToCellForRouteBase(cell, null, 2, false)
        val route = TestFactory.getRoute(cells = listOf(cellForRouteBase))

        val viewModel = viewModelMockBuilder
            .create(cellForRoute, route, routeType)

        val response = TestFactory.getResponseError<Int>(code = 400)

        `when`(networkUseCases.order.getOrderResortInfo(orderExternalId, null, route.id))
            .thenThrow(HttpException(response))

        val scanResult = ScanResultFactory.getOrderDefaultScanResult(orderExternalId)

        viewModel.processScanResult(scanResult)

        `assert scanner fragment`(
            scannerMode = ScannerModeImpl.Dispatchable,
            overlayState = OverlayState.Failure,
        )
        `assert button fragment`(
            centerButtonState = CenterButtonState.None,
        )
        `assert label`(
            isLabelAvailable = true,
            label = R.string.error,
        )
        `assert scanned place count`(
            isScannedPlaceCountTextVisible = false,
            isResetPlaceTextVisible = false
        )
        `assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )

        viewModel.forceReset()
        `assert initial state`(dispatchCanBeFinished = false)
    }
}
