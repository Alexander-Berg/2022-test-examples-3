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
import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.core.data.route.RouteType
import ru.yandex.market.sc.core.data.user.PropertyName
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.test.network.mocks.CellForRouteTestFactory
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class DispatchResortingScanLotTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val networkUseCases = NetworkUseCasesMock()

    private val stringManager = StringManagerMock(
        R.string.error,
        R.string.finish_dispatch,
        R.string.successfully,
        R.string.dispatch_not_ready,
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
    fun `scan lot from another cell (OUTGOING_COURIER)`() {
        `scan lot from another cell`(RouteType.OUTGOING_COURIER)
    }

    @Test
    fun `scan lot from another cell (OUTGOING_WAREHOUSE)`() {
        `scan lot from another cell`(RouteType.OUTGOING_WAREHOUSE)
    }

    private fun `scan lot from another cell`(routeType: RouteType) = runTest {
        val cell = TestFactory.getCourierCell("C-1")
        val cellForRoute = CellForRouteTestFactory.forCell(cell).build()
        val cellForRouteBase = CellForRouteTestFactory.mapToCellForRouteBase(cell, null, 2, true)
        val route = TestFactory.getRoute(cells = listOf(cellForRouteBase))

        val viewModel = viewModelMockBuilder.create(cellForRoute, route, routeType)

        val scanLotResult = ScanResultFactory.getScanResultQR(IdManager.getExternalId())

        viewModel.processScanResult(scanLotResult)

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
    fun `scan lot not ready to dispatch (OUTGOING_COURIER)`() {
        `scan lot not ready to dispatch`(RouteType.OUTGOING_COURIER)
    }

    @Test
    fun `scan lot not ready to dispatch (OUTGOING_WAREHOUSE)`() {
        `scan lot not ready to dispatch`(RouteType.OUTGOING_WAREHOUSE)
    }

    private fun `scan lot not ready to dispatch`(routeType: RouteType) = runTest {
        val lotExternalId = IdManager.getExternalId()

        val cell = TestFactory.getCourierCell("C-1")
        val cellForRoute = CellForRouteTestFactory.forCell(cell)
            .withLot(Lot.Status.CREATED, lotExternalId)
            .build()
        val cellForRouteBase = CellForRouteTestFactory.mapToCellForRouteBase(cell, null, 2, false)
        val route = TestFactory.getRoute(cells = listOf(cellForRouteBase))

        val viewModel = viewModelMockBuilder
            .create(cellForRoute, route, routeType)

        val scanLotResult = ScanResultFactory.getScanResultQR(lotExternalId)

        viewModel.processScanResult(scanLotResult)

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
        `assert initial state`(dispatchCanBeFinished = true)
    }

    @Test
    fun `scan lot ready to dispatch (OUTGOING_COURIER + requiredTransport)`() {
        `scan lot ready to dispatch`(RouteType.OUTGOING_COURIER, requiredTransport = false)
    }

    @Test
    fun `scan lot ready to dispatch (OUTGOING_COURIER)`() {
        `scan lot ready to dispatch`(RouteType.OUTGOING_COURIER, requiredTransport = true)
    }

    @Test
    fun `scan lot ready to dispatch (OUTGOING_WAREHOUSE + requiredTransport)`() {
        `scan lot ready to dispatch`(RouteType.OUTGOING_WAREHOUSE, requiredTransport = true)
    }

    @Test
    fun `scan lot ready to dispatch (OUTGOING_WAREHOUSE)`() {
        `scan lot ready to dispatch`(RouteType.OUTGOING_WAREHOUSE, requiredTransport = false)
    }

    private fun `scan lot ready to dispatch`(routeType: RouteType, requiredTransport: Boolean) = runTest {
        val lot = TestFactory.createLot().build()

        val cell = TestFactory.getCourierCell("C-1")
        val cellForRoute = CellForRouteTestFactory.forCell(cell)
            .withLot(Lot.Status.READY, lot.externalId)
            .build()
        val cellForRouteBase = CellForRouteTestFactory.mapToCellForRouteBase(cell, null, 2, false)
        val route = TestFactory.getRoute(cells = listOf(cellForRouteBase))

        val viewModel = viewModelMockBuilder.create(cellForRoute, route, routeType)


        val property = when (routeType) {
            RouteType.OUTGOING_COURIER -> PropertyName.REQUIRE_SCAN_TRANSPORT_FOR_COURIER
            RouteType.OUTGOING_WAREHOUSE -> PropertyName.REQUIRE_SCAN_TRANSPORT_FOR_WAREHOUSE
        }

        `when`(networkUseCases.checkUser.isPropertyEnabled(property))
            .thenReturn(requiredTransport)
        `when`(networkUseCases.lot.getLot(lot.externalId)).thenReturn(lot)

        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)

        viewModel.processScanResult(scanLotResult)

        `assert scanner fragment`(
            scannerMode = if (requiredTransport) ScannerModeImpl.TransportBarcode else ScannerModeImpl.Dispatchable,
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

        if (requiredTransport) {
            val scanTransportResult = ScanResultFactory.getScanResultBarcode("car-123")

            viewModel.processScanResult(scanTransportResult)

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
        }

        viewModel.forceReset()
        `assert initial state`(dispatchCanBeFinished = true)
    }

    @Test
    fun `scan lot with error on dispatch (OUTGOING_COURIER)`() {
        `scan lot with error on dispatch`(RouteType.OUTGOING_COURIER)
    }

    @Test
    fun `scan lot with error on dispatch (OUTGOING_WAREHOUSE)`() {
        `scan lot with error on dispatch`(RouteType.OUTGOING_WAREHOUSE)
    }

    private fun `scan lot with error on dispatch`(routeType: RouteType) = runTest {
        val lotExternalId = IdManager.getExternalId()
        val cell = TestFactory.getCourierCell("C-1")
        val cellForRoute = CellForRouteTestFactory.forCell(cell)
            .withLot(Lot.Status.READY, lotExternalId)
            .build()
        val cellForRouteBase = CellForRouteTestFactory.mapToCellForRouteBase(cell, null, 2, false)
        val route = TestFactory.getRoute(cells = listOf(cellForRouteBase))

        val viewModel = viewModelMockBuilder.create(cellForRoute, route, routeType)

        val errorMessage = "lot not ready to dispatch"
        val response = TestFactory.getResponseError<Int>(code = 400, errorMessage = errorMessage)
        val property = when (routeType) {
            RouteType.OUTGOING_COURIER -> PropertyName.REQUIRE_SCAN_TRANSPORT_FOR_COURIER
            RouteType.OUTGOING_WAREHOUSE -> PropertyName.REQUIRE_SCAN_TRANSPORT_FOR_WAREHOUSE
        }

        `when`(networkUseCases.checkUser.isPropertyEnabled(property)).thenReturn(false)
        `when`(
            networkUseCases.route.shipRouteWithLot(
                route.id,
                cellForRoute.id,
                lotShippedExternalId = lotExternalId
            )
        )
            .thenThrow(HttpException(response))

        val scanLotResult = ScanResultFactory.getScanResultQR(lotExternalId)

        viewModel.processScanResult(scanLotResult)

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
        `assert initial state`(dispatchCanBeFinished = true)
    }
}
