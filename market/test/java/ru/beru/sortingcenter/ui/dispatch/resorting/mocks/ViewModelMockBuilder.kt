package ru.beru.sortingcenter.ui.dispatch.resorting.mocks

import androidx.lifecycle.SavedStateHandle
import org.junit.Rule
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.quality.Strictness
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.beru.sortingcenter.ui.dispatch.data.cache.DispatchCellCache
import ru.beru.sortingcenter.ui.dispatch.data.cache.DispatchOrderListCache
import ru.beru.sortingcenter.ui.dispatch.data.cache.DispatchRouteCache
import ru.beru.sortingcenter.ui.dispatch.resorting.Asserts
import ru.beru.sortingcenter.ui.dispatch.resorting.DispatchResortingViewModel
import ru.yandex.market.sc.core.data.cell.CellForRoute
import ru.yandex.market.sc.core.data.route.Route
import ru.yandex.market.sc.core.data.route.RouteType
import ru.yandex.market.sc.core.resources.StringManager
import ru.yandex.market.test.utils.getOrAwaitValue

class ViewModelMockBuilder(
    private val stringManager: StringManager,
    private val networkUseCases: NetworkUseCasesMock,
) {
    @Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

    private val appMetrica: AppMetrica = Mockito.mock(AppMetrica::class.java)

    private val savedStateHandle: SavedStateHandle = Mockito.mock(SavedStateHandle::class.java)

    fun create(
        cell: CellForRoute,
        route: Route,
        routeType: RouteType,
    ): DispatchResortingViewModel {
        val cellCache = Mockito.mock(DispatchCellCache::class.java)
        val routeCache = Mockito.mock(DispatchRouteCache::class.java)
        val orderListCache = Mockito.mock(DispatchOrderListCache::class.java)

        `when`(routeCache.value).thenReturn(route)
        `when`(cellCache.value).thenReturn(cell)

        val viewModel = DispatchResortingViewModel(
            networkUseCases.route,
            networkUseCases.order,
            networkUseCases.lot,
            networkUseCases.sortable,
            networkUseCases.checkUser,
            routeCache,
            cellCache,
            orderListCache,
            appMetrica,
            savedStateHandle,
            stringManager,
        )

        viewModel.init(routeType)
        // наблюдаем orderItemsResult, потому что от него зависит, показывается ли кнопка со списком заказов
        viewModel.orderListService.items.getOrAwaitValue()

        Asserts.bind(viewModel, stringManager)

        return viewModel
    }
}
