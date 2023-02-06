package ru.beru.sortingcenter.ui.inventorying

import androidx.lifecycle.Observer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.yandex.market.sc.core.data.order.OrderItem
import ru.yandex.market.sc.core.data.order.RouteOrderIdMapper
import ru.yandex.market.sc.core.network.domain.NetworkCellUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue


@RunWith(MockitoJUnitRunner.StrictStubs::class)
class InventoryingSharedViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkCellUseCases: NetworkCellUseCases

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private val stringManager = TestStringManager()
    private lateinit var sharedViewModelInventorying: InventoryingShareViewModel
    private val routeOrderIds = TestFactory.getRouteOrderIds()
    private val courierCell = TestFactory.getCourierCell("C-1")
    private var cellWithOrders = TestFactory.mapToCellWithOrders(
        cell = courierCell,
        ordersAssignedToCell = 3,
        ordersInCell = 2
    )
    private val orderItemsObserver = Observer { _: Result -> }


    @Before
    fun setUp() {

        sharedViewModelInventorying =
            InventoryingShareViewModel(networkCellUseCases, networkOrderUseCases, appMetrica, stringManager)

        runTest {
            `when`(networkOrderUseCases.getRouteOrderIdsByCellId(ArgumentMatchers.anyLong())).thenReturn(routeOrderIds)
        }
        sharedViewModelInventorying.orderItemsResult.observeForever(orderItemsObserver)
        sharedViewModelInventorying.prepareCommonData(cellWithOrders)
    }


    @After
    fun tearDown() {
        sharedViewModelInventorying.orderItemsResult.removeObserver(orderItemsObserver)
    }

    @Test
    fun `get orderItems`() {
        val orderItems = routeOrderIds.map(RouteOrderIdMapper::map)
        assertThat(sharedViewModelInventorying.orderItemsList.getOrAwaitValue()).isEqualTo(orderItems)
        assertThat(sharedViewModelInventorying.orderItemsResult.getOrAwaitValue()).isEqualTo(Result.Success)
    }

    @Test
    fun `list with statuses`() {
        val orderItems = routeOrderIds.map(RouteOrderIdMapper::map)
        sharedViewModelInventorying.inventoriedItemsExternalIdSet =
            orderItems[0].places.map { it.externalId }.toSet()
        sharedViewModelInventorying.setListWithStatusesToShow()
        val firstOrder = sharedViewModelInventorying.orderItemsList.getOrAwaitValue()[0]
        val firstElementStatus = firstOrder.places.first().status
        assertThat(firstElementStatus).isEqualTo(OrderItem.Status.INVENTORIED)
    }
}
