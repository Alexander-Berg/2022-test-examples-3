package ru.beru.sortingcenter.ui.dispatch.resorting.mocks

import org.junit.Rule
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.quality.Strictness
import ru.yandex.market.sc.core.network.domain.*

class NetworkUseCasesMock {
    @Rule
    val mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

    val route: NetworkRouteUseCases = Mockito.mock(NetworkRouteUseCases::class.java)

    val order: NetworkOrderUseCases = Mockito.mock(NetworkOrderUseCases::class.java)

    val lot: NetworkLotUseCases = Mockito.mock(NetworkLotUseCases::class.java)

    val sortable: NetworkSortableUseCases = Mockito.mock(NetworkSortableUseCases::class.java)

    val checkUser: NetworkCheckUserUseCases = Mockito.mock(NetworkCheckUserUseCases::class.java)
}