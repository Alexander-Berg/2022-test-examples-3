package ru.beru.sortingcenter.ui.dispatch.confirmation

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.beru.sortingcenter.arch.ext.Result
import ru.beru.sortingcenter.ui.dispatch.data.cache.DispatchCellCache
import ru.beru.sortingcenter.ui.dispatch.data.cache.DispatchOrderListCache
import ru.beru.sortingcenter.ui.dispatch.data.cache.DispatchRouteCache
import ru.yandex.market.sc.core.data.cell.CellForRoute
import ru.yandex.market.sc.core.data.order.OrderItem
import ru.yandex.market.sc.core.data.order.OrderShipStatus
import ru.yandex.market.sc.core.data.route.Route
import ru.yandex.market.sc.core.data.route.RouteType
import ru.yandex.market.sc.core.network.domain.NetworkCellUseCases
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.network.domain.NetworkRouteUseCases
import ru.yandex.market.sc.test.network.mocks.CellForRouteTestFactory
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.yandex.market.test.utils.mockObserver

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class DispatchConfirmationViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var networkRouteUseCases: NetworkRouteUseCases

    @Mock
    private lateinit var networkCellUseCases: NetworkCellUseCases

    @Mock
    private lateinit var networkCheckUserUseCases: NetworkCheckUserUseCases

    @Mock
    private lateinit var orderListCache: DispatchOrderListCache

    @Mock
    private lateinit var routeCache: DispatchRouteCache

    @Mock
    private lateinit var cellCache: DispatchCellCache

    @Mock
    private lateinit var appMetrica: AppMetrica

    @Mock
    private lateinit var savedStateHandle: SavedStateHandle

    private lateinit var viewModel: DispatchConfirmationViewModel

    private val cell = TestFactory.getCourierCell()
    private val order = TestFactory.getOrderToCourier(cellTo = cell, cell = cell)
    private val cellForRoute = CellForRouteTestFactory.mapToCellForRoute(
        cell = cell,
        ordersNotInCell = 2,
        actions = listOf(CellForRoute.Action.SHIP_ALL, CellForRoute.Action.SHIP_AND_RESORT),
        orders = mapOf(order.externalId to OrderShipStatus.OK)
    )
    private val routeOrderIds = TestFactory.getRouteOrderIds()

    private val courier = TestFactory.getCourier("Ilia Mazan")
    private val warehouse = TestFactory.getWarehouse("Warehouse")
    private val routeToCourier =
        TestFactory.getRoute(status = Route.Status.IN_PROGRESS, courier = courier)
    private val routeToWarehouse = TestFactory.getRoute(
        status = Route.Status.IN_PROGRESS,
        warehouseName = warehouse.name,
    )

    private val orderItemsObserver = Observer { _: Result<Array<OrderItem>> -> }

    @Before
    fun setUp() {
        runBlocking {
            viewModel = DispatchConfirmationViewModel(
                networkOrderUseCases,
                networkRouteUseCases,
                networkCellUseCases,
                routeCache,
                orderListCache,
                cellCache,
                appMetrica,
                networkCheckUserUseCases,
                savedStateHandle,
                stringManager
            )

            viewModel.apply {
                orderItemsResult.observeForever(orderItemsObserver)
                canShipAllButtonVisible.observeForever(mockObserver)
                canShipAndResortButtonVisible.observeForever(mockObserver)
                canShipAndResortButtonOutlineVisible.observeForever(mockObserver)
            }

            `when`(routeCache.value).thenReturn(routeToCourier)
            `when`(networkCellUseCases.getCellForRoute(cellForRoute.id, routeToCourier.id)).thenReturn(cellForRoute)

            `when`(networkOrderUseCases.getRouteOrderIdsByCellId(cellForRoute.id)).thenReturn(
                routeOrderIds
            )
        }
    }

    @After
    fun tearDown() {
        viewModel.apply {
            orderItemsResult.removeObserver(orderItemsObserver)
            canShipAllButtonVisible.removeObserver(mockObserver)
            canShipAndResortButtonVisible.removeObserver(mockObserver)
            canShipAndResortButtonOutlineVisible.removeObserver(mockObserver)
        }
    }

    @Test
    fun `dispatch confirmation courier cell`() = runTest {
        viewModel.init(cellForRoute.id, RouteType.OUTGOING_COURIER)

        assertTitles(
            name = courier.name,
            orderCount = cellForRoute.orders.size,
            ordersNotInCell = cellForRoute.ordersNotInCell,
            hasMissingOrders = true,
        )

        assertButtons(
            canShipAllButtonVisible = true,
            canShipAndResortButtonVisible = false,
            canShipAndResortButtonOutlineVisible = true,
            shipButtonEnabled = true,
            shouldViewOrderList = true,
        )
    }

    @Test
    fun `dispatch confirmation return cell`() = runTest {
        `when`(routeCache.value).thenReturn(routeToWarehouse)
        `when`(
            networkCellUseCases.getCellForRoute(
                cellForRoute.id,
                routeToWarehouse.id
            )
        ).thenReturn(cellForRoute)
        viewModel.init(cellForRoute.id, RouteType.OUTGOING_WAREHOUSE)

        assertTitles(
            name = warehouse.name,
            orderCount = cellForRoute.orders.size,
            ordersNotInCell = cellForRoute.ordersNotInCell,
            hasMissingOrders = true,
        )

        assertButtons(
            canShipAllButtonVisible = true,
            canShipAndResortButtonVisible = false,
            canShipAndResortButtonOutlineVisible = true,
            shipButtonEnabled = true,
            shouldViewOrderList = true,
        )
    }

    @Test
    fun `update return cell`() = runTest {
        `when`(routeCache.value).thenReturn(routeToWarehouse)
        viewModel.init(cellForRoute.id, RouteType.OUTGOING_WAREHOUSE)

        `when`(networkRouteUseCases.getRoute(routeToWarehouse.id)).thenReturn(routeToWarehouse)
        viewModel.updateRoute()

        verify(networkRouteUseCases).getRoute(routeToWarehouse.id)
    }

    @Test
    fun `require resorting false`() = runTest {
        val cell = cellForRoute.copy(actions = listOf(CellForRoute.Action.SHIP_AND_RESORT))
        `when`(networkCellUseCases.getCellForRoute(cell.id, routeToCourier.id)).thenReturn(cell)
        viewModel.init(cellForRoute.id, RouteType.OUTGOING_COURIER)

        assertTitles(
            name = courier.name,
            orderCount = cell.orders.size,
            ordersNotInCell = cell.ordersNotInCell,
            hasMissingOrders = true,
        )

        assertButtons(
            canShipAllButtonVisible = false,
            canShipAndResortButtonVisible = true,
            canShipAndResortButtonOutlineVisible = false,
            shipButtonEnabled = true,
            shouldViewOrderList = true,
        )
    }

    @Test
    fun `orders with do not ship status`() = runTest {
        val orderDoNotShip = TestFactory.getOrderToCourier(cellTo = cell, cell = cell)
        val orders = mapOf(
            order.externalId to OrderShipStatus.OK,
            orderDoNotShip.externalId to OrderShipStatus.DO_NOT_SHIP
        )
        val cell = cellForRoute.copy(orders = orders)

        viewModel.init(cellForRoute.id, RouteType.OUTGOING_COURIER)

        assertTitles(
            name = courier.name,
            orderCount = 1,
            ordersNotInCell = cell.ordersNotInCell,
            hasMissingOrders = true,
        )

        assertButtons(
            canShipAllButtonVisible = true,
            canShipAndResortButtonVisible = false,
            canShipAndResortButtonOutlineVisible = true,
            shipButtonEnabled = true,
            shouldViewOrderList = true,
        )
    }

    @Test
    fun `empty orders map`() = runTest {
        val cell = cellForRoute.copy(orders = mapOf())
        `when`(networkCellUseCases.getCellForRoute(cell.id, routeToCourier.id)).thenReturn(cell)
        viewModel.init(cellForRoute.id, RouteType.OUTGOING_COURIER)

        assertTitles(
            name = courier.name,
            orderCount = cell.orders.size,
            ordersNotInCell = cell.ordersNotInCell,
            hasMissingOrders = true,
        )

        assertButtons(
            canShipAllButtonVisible = true,
            canShipAndResortButtonVisible = false,
            canShipAndResortButtonOutlineVisible = true,
            shipButtonEnabled = false,
            shouldViewOrderList = false,
        )
    }

    @Test
    fun `has not missing orders`() = runTest {
        val cell = cellForRoute.copy(ordersNotInCell = 0)
        `when`(networkCellUseCases.getCellForRoute(cell.id, routeToCourier.id)).thenReturn(cell)
        viewModel.init(cellForRoute.id, RouteType.OUTGOING_COURIER)

        assertTitles(
            name = courier.name,
            orderCount = cell.orders.size,
            ordersNotInCell = cell.ordersNotInCell,
            hasMissingOrders = false,
        )

        assertButtons(
            canShipAllButtonVisible = true,
            canShipAndResortButtonVisible = false,
            canShipAndResortButtonOutlineVisible = true,
            shipButtonEnabled = true,
            shouldViewOrderList = true,
        )
    }

    @Test
    fun `ship with empty orders map`() = runTest {
        val cell = cellForRoute.copy(orders = mapOf())
        `when`(networkCellUseCases.getCellForRoute(cell.id, routeToCourier.id)).thenReturn(cell)
        viewModel.init(cellForRoute.id, RouteType.OUTGOING_COURIER)

        viewModel.ship()
        assertThat(
            viewModel.hasError.getOrAwaitValue().get()
        ).isEqualTo(stringManager.getString(R.string.error))
    }

    @Test
    fun `ship orders with error`() = runTest {
        val cell = cellForRoute.copy(actions = listOf(CellForRoute.Action.SHIP_ALL))
        viewModel.init(cellForRoute.id, RouteType.OUTGOING_COURIER)

        val responseErrorMessage = "Order shipped with error!"
        val response =
            TestFactory.getResponseError<Int>(code = 401, errorMessage = responseErrorMessage)

        `when`(networkRouteUseCases.shipRouteFull(routeToCourier.id, cell.id)).thenThrow(
            HttpException(response)
        )
        viewModel.ship()
        assertThat(viewModel.hasError.getOrAwaitValue().get()).isEqualTo(responseErrorMessage)
    }

    @Test
    fun `ship orders`() = runTest {
        val cell = cellForRoute.copy(actions = listOf(CellForRoute.Action.SHIP_ALL))
        viewModel.init(cellForRoute.id, RouteType.OUTGOING_COURIER)

        `when`(networkRouteUseCases.shipRouteFull(routeToCourier.id, cell.id)).thenReturn(Unit)
        viewModel.ship()
        assertThat(viewModel.isSuccessful.getOrAwaitValue().get()).isEqualTo(cell.id)
    }

    @Test
    fun `ship and resort orders`() = runTest {
        viewModel.init(cellForRoute.id, RouteType.OUTGOING_COURIER)

        viewModel.shipAndResort()
        assertThat(viewModel.startResorting.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    @Test
    fun `navigate to order list`() = runTest {
        viewModel.init(cellForRoute.id, RouteType.OUTGOING_COURIER)
        viewModel.showOrderList()

        assertThat(viewModel.showOrderList.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    private fun assertTitles(
        name: String,
        orderCount: Int = 0,
        ordersNotInCell: Int = 0,
        hasMissingOrders: Boolean = false,
    ) {
        viewModel.apply {
            assertEquals(name, this.name.getOrAwaitValue())
            assertEquals(orderCount.toString(), this.orderCount.getOrAwaitValue())
            assertEquals(ordersNotInCell.toString(), this.ordersNotInCell.getOrAwaitValue())
            assertEquals(hasMissingOrders, this.hasMissingOrders.getOrAwaitValue())
        }
    }

    private fun assertButtons(
        canShipAllButtonVisible: Boolean = false,
        canShipAndResortButtonVisible: Boolean = false,
        canShipAndResortButtonOutlineVisible: Boolean = false,
        shipButtonEnabled: Boolean = false,
        shouldViewOrderList: Boolean = false,
    ) {
        viewModel.apply {
            assertEquals(canShipAllButtonVisible, this.canShipAllButtonVisible.getOrAwaitValue())
            assertEquals(
                canShipAndResortButtonVisible,
                this.canShipAndResortButtonVisible.getOrAwaitValue()
            )
            assertEquals(
                canShipAndResortButtonOutlineVisible,
                this.canShipAndResortButtonOutlineVisible.getOrAwaitValue()
            )
            assertEquals(shouldViewOrderList, this.shouldViewOrderList)
            assertEquals(shipButtonEnabled, this.shipButtonEnabled.getOrAwaitValue())
        }
    }
}
