package ru.beru.sortingcenter.ui.dispatch.resorting

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.ui.dispatch.resorting.Asserts.`assert description`
import ru.beru.sortingcenter.ui.dispatch.resorting.Asserts.`assert scanner fragment`
import ru.beru.sortingcenter.ui.dispatch.resorting.mocks.NetworkUseCasesMock
import ru.beru.sortingcenter.ui.dispatch.resorting.mocks.StringManagerMock
import ru.beru.sortingcenter.ui.dispatch.resorting.mocks.ViewModelMockBuilder
import ru.beru.sortingcenter.ui.dispatch.resorting.models.ScannerModeImpl
import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.data.order.OrderShipStatus
import ru.yandex.market.sc.core.data.order.ResortReasonCode
import ru.yandex.market.sc.core.data.route.RouteType
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.test.network.mocks.CellForRouteTestFactory
import ru.yandex.market.sc.test.network.mocks.OrderResortInfoTestFactory
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class DispatchResortingResortPlaceTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val networkUseCases = NetworkUseCasesMock()

    private val stringManager = StringManagerMock(
        R.string.dispatch_not_ready,
        R.string.error,
        R.string.finish_dispatch,
        R.string.order_already_scanned,
        R.string.order,
        R.string.place,
        R.string.resort_entity_to_any_keep_cell,
        R.string.resort_entity_to_any_return_cell,
        R.string.resort_entity_to_one_of_specific_keep_cells,
        R.string.resort_entity_to_one_of_specific_return_cells,
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
    fun `resort place (RESORT_CANCELED_ON_DO) (availableCells empty) (OUTGOING_COURIER)`() {
        `resort place (RESORT_CANCELED_ON_DO) (availableCells empty)`(
            RouteType.OUTGOING_COURIER,
        )
    }

    @Test
    fun `resort place (RESORT_CANCELED_ON_DO) (availableCells empty) (OUTGOING_WAREHOUSE)`() {
        `resort place (RESORT_CANCELED_ON_DO) (availableCells empty)`(
            RouteType.OUTGOING_WAREHOUSE,
        )
    }

    private fun `resort place (RESORT_CANCELED_ON_DO) (availableCells empty)`(
        routeType: RouteType
    ) {
        `resort place`(
            cellsForResort = listOf(),
            expectedDescriptionOnPlaceScan = "Отсортируйте место в любую ячейку возврата",
            resortReasonCode = ResortReasonCode.RESORT_CANCELED_ON_DO,
            routeType = routeType,
        )
    }

    @Test
    fun `resort place (RESORT_CANCELED_ON_DO) (availableCells not empty) (OUTGOING_COURIER)`() {
        `resort place (RESORT_CANCELED_ON_DO) (availableCells not empty)`(
            RouteType.OUTGOING_COURIER,
        )
    }

    @Test
    fun `resort place (RESORT_CANCELED_ON_DO) (availableCells not empty) (OUTGOING_WAREHOUSE)`() {
        `resort place (RESORT_CANCELED_ON_DO) (availableCells not empty)`(
            RouteType.OUTGOING_WAREHOUSE,
        )
    }

    private fun `resort place (RESORT_CANCELED_ON_DO) (availableCells not empty)`(
        routeType: RouteType
    ) {
        val cellsForResort = listOf(
            TestFactory.getReturnCell(),
            TestFactory.getReturnCell(),
        )
        val expectedDescription =
            """
                Отсортируйте место в одну из следующих ячеек возврата:
                ${cellsForResort[0].number} ${cellsForResort[1].number}
            """.trimIndent()

        `resort place`(
            cellsForResort = cellsForResort,
            expectedDescriptionOnPlaceScan = expectedDescription,
            resortReasonCode = ResortReasonCode.RESORT_CANCELED_ON_DO,
            routeType = routeType,
        )
    }

    @Test
    fun `resort place (RESORT_TO_BUFFER) (availableCells empty) (OUTGOING_COURIER)`() {
        `resort place (RESORT_TO_BUFFER) (availableCells empty)`(
            RouteType.OUTGOING_COURIER,
        )
    }

    @Test
    fun `resort place (RESORT_TO_BUFFER) (availableCells empty) (OUTGOING_WAREHOUSE)`() {
        `resort place (RESORT_TO_BUFFER) (availableCells empty)`(
            RouteType.OUTGOING_WAREHOUSE,
        )
    }

    private fun `resort place (RESORT_TO_BUFFER) (availableCells empty)`(
        routeType: RouteType
    ) {
        `resort place`(
            cellsForResort = listOf(),
            expectedDescriptionOnPlaceScan = "Отсортируйте место в любую ячейку хранения",
            resortReasonCode = ResortReasonCode.RESORT_TO_BUFFER,
            routeType = routeType,
        )
    }

    @Test
    fun `resort place (RESORT_TO_BUFFER) (availableCells not empty) (OUTGOING_COURIER)`() {
        `resort place (RESORT_TO_BUFFER) (availableCells not empty)`(
            RouteType.OUTGOING_COURIER,
        )
    }

    @Test
    fun `resort place (RESORT_TO_BUFFER) (availableCells not empty) (OUTGOING_WAREHOUSE)`() {
        `resort place (RESORT_TO_BUFFER) (availableCells not empty)`(
            RouteType.OUTGOING_WAREHOUSE,
        )
    }

    private fun `resort place (RESORT_TO_BUFFER) (availableCells not empty)`(
        routeType: RouteType,
    ) {
        val cellsForResort = listOf(
            TestFactory.getBufferCell(),
            TestFactory.getBufferCell(),
        )
        val expectedDescription =
            """
                Отсортируйте место в одну из следующих ячеек хранения:
                ${cellsForResort[0].number} ${cellsForResort[1].number}
            """.trimIndent()

        `resort place`(
            cellsForResort = cellsForResort,
            expectedDescriptionOnPlaceScan = expectedDescription,
            resortReasonCode = ResortReasonCode.RESORT_TO_BUFFER,
            routeType = routeType,
        )
    }

    private fun `resort place`(
        cellsForResort: List<Cell>,
        expectedDescriptionOnPlaceScan: String,
        resortReasonCode: ResortReasonCode,
        routeType: RouteType,
    ) = runTest {
        val dataForResortPlaceTest = setupDataForResortPlaceTest(
            routeType,
            resortReasonCode,
            cellsForResort
        )
        val viewModel = dataForResortPlaceTest.viewModel
        val orderExternalId = dataForResortPlaceTest.orderExternalId
        val placeExternalId = dataForResortPlaceTest.placeExternalId
        val cellForResort = dataForResortPlaceTest.cellForResort

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderExternalId)

        viewModel.processScanResult(scanOrderResult)

        `assert scanner fragment`(
            scannerMode = ScannerModeImpl.PlaceBarcode,
            overlayState = OverlayState.Success,
        )

        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placeExternalId)

        viewModel.processScanResult(scanPlaceResult)

        `assert scanner fragment`(
            scannerMode = ScannerModeImpl.CellQRCode,
            overlayState = OverlayState.Warning,
        )
        `assert description`(true, expectedDescriptionOnPlaceScan)

        val scanCellResult = ScanResultFactory.getScanResultQR(cellForResort.id)

        viewModel.processScanResult(scanCellResult)

        `assert scanner fragment`(
            scannerMode = ScannerModeImpl.Dispatchable,
            overlayState = OverlayState.Success
        )

        verify(networkUseCases.sortable, times(1)).sort(
            orderExternalId,
            ExternalId(cellForResort.id),
            placeExternalId,
            ignoreTodayRouteOnKeep = true,
        )

        viewModel.forceReset()

        `assert scanner fragment`(scannerMode = ScannerModeImpl.Dispatchable)
    }

    private suspend fun setupDataForResortPlaceTest(
        routeType: RouteType,
        resortReasonCode: ResortReasonCode,
        cellsForResort: List<Cell>
    ): DataForResortPlaceTest {
        val placeResortInfo = OrderResortInfoTestFactory.get(
            availableCells = cellsForResort,
            forPlace = true,
            resortReasonCode = resortReasonCode,
        )
        val orderExternalId = placeResortInfo.orderExternalId
        val placeExternalId = placeResortInfo.placeExternalId

        requireNotNull(placeExternalId)

        val cell = TestFactory.getCourierCell("C-1")
        val cellForRoute = CellForRouteTestFactory.forCell(cell)
            .withOrder(OrderShipStatus.DO_NOT_SHIP, false, orderExternalId)
            .withPlace(true, placeExternalId)
            .withPlace(true)
            .build()
            .build()
        val cellForRouteBase = CellForRouteTestFactory.mapToCellForRouteBase(cell, null, 2, false)
        val route = TestFactory.getRoute(cells = listOf(cellForRouteBase))

        val viewModel = viewModelMockBuilder
            .create(cellForRoute, route, routeType)

        val cellForResort =
            if (cellsForResort.isEmpty()) {
                when (resortReasonCode) {
                    ResortReasonCode.RESORT_TO_BUFFER ->
                        TestFactory.getBufferCell()
                    ResortReasonCode.RESORT_CANCELED_ON_DO ->
                        TestFactory.getReturnCell()
                    ResortReasonCode.UNKNOWN ->
                        throw IllegalArgumentException("Result of resort when resortReasonCode == UNKNOWN is not defined, therefore it cannot be tested")
                }
            } else {
                cellsForResort.first()
            }

        `when`(networkUseCases.order.getOrderResortInfo(orderExternalId, placeExternalId, route.id))
            .thenReturn(placeResortInfo)

        return DataForResortPlaceTest(
            cellForResort = cellForResort,
            orderExternalId = orderExternalId,
            placeExternalId = placeExternalId,
            viewModel = viewModel,
        )
    }

    companion object {
        private data class DataForResortPlaceTest(
            val viewModel: DispatchResortingViewModel,
            val orderExternalId: ExternalId,
            val placeExternalId: ExternalId,
            val cellForResort: Cell,
        )
    }
}
