package ru.beru.sortingcenter.ui.dispatch.resorting

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.ui.common.scannerbuttons.ScannerButtonsViewModel
import ru.beru.sortingcenter.ui.dispatch.resorting.Asserts.`assert initial state`
import ru.beru.sortingcenter.ui.dispatch.resorting.mocks.NetworkUseCasesMock
import ru.beru.sortingcenter.ui.dispatch.resorting.mocks.StringManagerMock
import ru.beru.sortingcenter.ui.dispatch.resorting.mocks.ViewModelMockBuilder
import ru.beru.sortingcenter.ui.dispatch.resorting.models.ScannerModeImpl
import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.core.data.route.RouteType
import ru.yandex.market.sc.core.data.user.PropertyName
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.test.network.mocks.CellForRouteTestFactory
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class DispatchResortingScanLotAndTransportTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val networkUseCases = NetworkUseCasesMock()

    private val stringManager = StringManagerMock(
        R.string.error,
        R.string.finish_dispatch,
        R.string.successfully,
    )

    private val viewModelMockBuilder = ViewModelMockBuilder(stringManager, networkUseCases)

    private val routeOrderIds = TestFactory.getRouteOrderIds()

    @Before
    fun setUp() {
        runBlocking {
            Mockito.`when`(networkUseCases.order.getRouteOrderIdsByCellId(ArgumentMatchers.anyLong()))
                .thenReturn(routeOrderIds)
        }
    }

    @Test
    fun `scan lot with error dispatch (OUTGOING_COURIER + requiredTransport)`() {
        `scan lot and transport with error dispatch`(RouteType.OUTGOING_COURIER)
    }

    @Test
    fun `scan lot with error dispatch (OUTGOING_WAREHOUSE + requiredTransport)`() {
        `scan lot and transport with error dispatch`(RouteType.OUTGOING_WAREHOUSE)
    }

    private fun `scan lot and transport with error dispatch`(routeType: RouteType) = runTest {
        val lot = TestFactory.createLot().build()

        val cell = TestFactory.getCourierCell("C-1")
        val cellForRoute = CellForRouteTestFactory.forCell(cell)
            .withLot(Lot.Status.READY, lot.externalId)
            .build()
        val cellForRouteBase = CellForRouteTestFactory.mapToCellForRouteBase(cell, null, 2, false)
        val route = TestFactory.getRoute(cells = listOf(cellForRouteBase))

        val viewModel = viewModelMockBuilder
            .create(cellForRoute, route, routeType)

        val scannedTransport = "Car-123"
        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)
        val scanTransportResult = ScanResultFactory.getScanResultBarcode(scannedTransport)

        val errorMessage = "lot not ready to dispatch"
        val response = TestFactory.getResponseError<Int>(code = 400, errorMessage = errorMessage)
        val property = when (routeType) {
            RouteType.OUTGOING_COURIER -> PropertyName.REQUIRE_SCAN_TRANSPORT_FOR_COURIER
            RouteType.OUTGOING_WAREHOUSE -> PropertyName.REQUIRE_SCAN_TRANSPORT_FOR_WAREHOUSE
        }

        Mockito.`when`(networkUseCases.checkUser.isPropertyEnabled(property)).thenReturn(true)
        Mockito.`when`(networkUseCases.lot.getLot(lot.externalId)).thenReturn(lot)
        Mockito.`when`(
            networkUseCases.route.shipRouteWithLot(
                route.id,
                cellForRoute.id,
                lot.externalId,
                scannedTransport,
            )
        )
            .thenThrow(HttpException(response))
        viewModel.processScanResult(scanLotResult)


        Asserts.`assert scanner fragment`(
            scannerMode = ScannerModeImpl.TransportBarcode,
            overlayState = OverlayState.Success,
        )
        Asserts.`assert button fragment`(
            centerButtonState = ScannerButtonsViewModel.CenterButtonState.ShowList,
        )
        Asserts.`assert label`(
            isLabelAvailable = true,
            label = R.string.successfully,
        )
        Asserts.`assert scanned place count`(
            isScannedPlaceCountTextVisible = false,
            isResetPlaceTextVisible = false
        )
        Asserts.`assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )

        viewModel.processScanResult(scanTransportResult)

        Asserts.`assert scanner fragment`(
            scannerMode = ScannerModeImpl.Dispatchable,
            overlayState = OverlayState.Failure,
        )
        Asserts.`assert button fragment`(
            centerButtonState = ScannerButtonsViewModel.CenterButtonState.None,
        )
        Asserts.`assert label`(
            isLabelAvailable = true,
            label = R.string.error,
        )
        Asserts.`assert scanned place count`(
            isScannedPlaceCountTextVisible = false,
            isResetPlaceTextVisible = false
        )
        Asserts.`assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )

        viewModel.forceReset()
        `assert initial state`(dispatchCanBeFinished = true)
    }
}
