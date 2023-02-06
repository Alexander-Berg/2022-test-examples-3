package ru.yandex.market.sc.feature.tasks.collect.presenter.orders.sorting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.data.order.RouteOrderIdMapper
import ru.yandex.market.sc.core.data.tasks.TaskType
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.network.domain.NetworkRouteTaskUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSortableUseCases
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.feature.tasks.collect.analytics.AppMetrica
import ru.yandex.market.sc.feature.tasks.collect.presenter.orders.sorting.data.OrdersSortingScannerMode
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.network.mocks.successResource
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class OrdersSortingViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkCheckUserUseCases: NetworkCheckUserUseCases

    @Mock
    private lateinit var networkRouteTaskUseCases: NetworkRouteTaskUseCases

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var networkSortableUseCases: NetworkSortableUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: OrdersSortingViewModel

    private val routeId = 1L
    private val cellId = 2L
    private val routeOrderIds = (0..3).map { TestFactory.getRouteOrderId() }

    @Before
    fun setUp() = runBlocking {
        viewModel = OrdersSortingViewModel(
            networkCheckUserUseCases,
            networkRouteTaskUseCases,
            networkOrderUseCases,
            networkSortableUseCases,
            appMetrica,
            stringManager,
        )

        `when`(networkOrderUseCases.getRouteOrderIds(routeId, cellId, TaskType.BUFFER_RETURN))
            .thenReturn(routeOrderIds)
        viewModel.init(routeId, cellId)

        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(OrdersSortingScannerMode.OrderBarcode)
    }

    @Test
    fun `success load route order ids`() = runTest {
        val orderItems = routeOrderIds.map { RouteOrderIdMapper.map(it) }
        assertThat(viewModel.totalPlaceCount.getOrAwaitValue()).isEqualTo(orderItems.size)
    }

    @Test
    fun `finish cell event`() = runTest {
        `when`(networkRouteTaskUseCases.finishCell(routeId, cellId)).thenReturn(successResource(Unit))
        viewModel.onFinishCell()
        assertThat(viewModel.successFinishCellEvent.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    @Test
    fun `success scan single order`() = runTest {
        viewModel.init(routeId, cellId)

        val returnCell = TestFactory.getReturnCell("R-1")
        val bufferReturnCell = TestFactory.getBufferCell("АХ-1", subType = Cell.SubType.BUFFER_RETURNS)
        val sortResponse = TestFactory.getSortResponse(returnCell)
        val firstOrderItem = routeOrderIds.first()
        val order = TestFactory.getOrderToReturn(
            cell = bufferReturnCell,
            cellTo = returnCell,
            routeId = routeId,
            placeExternalIds = listOf(firstOrderItem.places.first().externalId.value),
        ).copy(externalId = firstOrderItem.externalId)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(returnCell.id)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        viewModel.onScan(scanOrderResult)

        assertThat(viewModel.sortedPlacesCount.getOrAwaitValue()).isEqualTo(0)
        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(OrdersSortingScannerMode.CellQRCode)

        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationExternalId = ExternalId(returnCell.id),
                place.externalId
            )
        )
            .thenReturn(sortResponse)
        viewModel.onScan(scanCellResult)

        assertThat(viewModel.sortedPlacesCount.getOrAwaitValue()).isEqualTo(1)
    }

    @Test
    fun `success scan multiplace order`() = runTest {
        viewModel.init(routeId, cellId)

        val returnCell = TestFactory.getReturnCell("R-1")
        val bufferReturnCell = TestFactory.getBufferCell("АХ-1", subType = Cell.SubType.BUFFER_RETURNS)
        val sortResponse = TestFactory.getSortResponse(returnCell)
        val firstOrderItem = routeOrderIds.first()
        val order = TestFactory.getOrderToReturn(
            cell = bufferReturnCell,
            cellTo = returnCell,
            routeId = routeId,
            placeExternalIds = listOf(firstOrderItem.places.first().externalId.value, "123"),
        ).copy(externalId = firstOrderItem.externalId)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(returnCell.id)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        viewModel.onScan(scanOrderResult)

        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(OrdersSortingScannerMode.PlaceBarcode)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, place.externalId)).thenReturn(order)
        viewModel.onScan(scanPlaceResult)

        assertThat(viewModel.sortedPlacesCount.getOrAwaitValue()).isEqualTo(0)
        assertThat(viewModel.scanMode.getOrAwaitValue()).isEqualTo(OrdersSortingScannerMode.CellQRCode)

        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationExternalId = ExternalId(returnCell.id),
                place.externalId
            )
        )
            .thenReturn(sortResponse)
        viewModel.onScan(scanCellResult)

        assertThat(viewModel.sortedPlacesCount.getOrAwaitValue()).isEqualTo(1)
    }
}
