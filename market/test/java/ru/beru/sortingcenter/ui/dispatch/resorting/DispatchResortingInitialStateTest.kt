package ru.beru.sortingcenter.ui.dispatch.resorting

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.ui.dispatch.resorting.Asserts.`assert initial state`
import ru.beru.sortingcenter.ui.dispatch.resorting.mocks.NetworkUseCasesMock
import ru.beru.sortingcenter.ui.dispatch.resorting.mocks.StringManagerMock
import ru.beru.sortingcenter.ui.dispatch.resorting.mocks.ViewModelMockBuilder
import ru.yandex.market.sc.core.data.order.OrderShipStatus
import ru.yandex.market.sc.core.data.route.RouteType
import ru.yandex.market.sc.test.network.mocks.CellForRouteTestFactory
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class DispatchResortingInitialStateTest {
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
    fun `initial state (no actions) (OUTGOING_COURIER)`() {
        val cell = TestFactory.getCourierCell("C-1")

        val cellForRoute = CellForRouteTestFactory.forCell(cell)
            .withOrder(OrderShipStatus.OK)
            .withPlace(inCell = true)
            .build()
            .build()
        val cellForRouteBase = CellForRouteTestFactory.mapToCellForRouteBase(cell, null, 2, false)
        val route = TestFactory.getRoute(cells = listOf(cellForRouteBase))

        // необходимо, чтобы забиндить Assert
        viewModelMockBuilder
            .create(cellForRoute, route, RouteType.OUTGOING_COURIER)

        `assert initial state`(dispatchCanBeFinished = false)
    }
}
