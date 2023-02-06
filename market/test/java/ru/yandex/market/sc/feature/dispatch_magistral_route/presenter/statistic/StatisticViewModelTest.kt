package ru.yandex.market.sc.feature.dispatch_magistral_route.presenter.statistic

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.data.route.OutgoingCourierRouteType
import ru.yandex.market.sc.core.network.domain.NetworkRouteUseCases
import ru.yandex.market.sc.feature.dispatch_magistral_route.data.MagistralRouteInfo
import ru.yandex.market.sc.feature.dispatch_magistral_route.data.MagistralRouteInfoCache
import ru.yandex.market.sc.feature.dispatch_magistral_route.data.MagistralRouteInfoMapper
import ru.yandex.market.sc.test.network.mocks.CellForRouteTestFactory.mapToCellForRouteBase
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class StatisticViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkRouteUseCases: NetworkRouteUseCases

    private val stringManager = TestStringManager()
    private lateinit var viewModel: StatisticViewModel
    private val magistralRouteInfoCache = MagistralRouteInfoCache()

    private val cell1 = mapToCellForRouteBase(TestFactory.getCourierCell(), null, 4, false)
    private val cell2 = mapToCellForRouteBase(TestFactory.getCourierCell(), null, 3, false)
    private val emptyCell = mapToCellForRouteBase(TestFactory.getCourierCell(), null, 0, true)
    private val magistralRouteInfo = MagistralRouteInfo(
        routeId = 1L,
        name = "СЦ МК Краснодар",
        type = OutgoingCourierRouteType.MAGISTRAL,
        lotsTotal = 20,
        cells = listOf(cell1, cell2, emptyCell)
    )

    @Before
    fun setUp() {
        viewModel = StatisticViewModel(networkRouteUseCases, magistralRouteInfoCache, stringManager)
        viewModel.init(magistralRouteInfo)

        assertThat(magistralRouteInfoCache.value).isEqualTo(magistralRouteInfo)
    }

    @Test
    fun `not show empty cells`() = runTest {
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.cells).isEqualTo(listOf(cell1, cell2))
    }

    @Test
    fun `update route info with error`() = runTest {
        val message = "Что-то пошло не так"
        `when`(networkRouteUseCases.getRoute(magistralRouteInfo.routeId)).thenThrow(
            RuntimeException(
                message
            )
        )
        viewModel.updateRouteInfo()

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.errorText).isEqualTo(message)
        assertThat(uiState.isLoading).isFalse()
    }

    @Test
    fun `update route info with new cell`() = runTest {
        val cell4 = mapToCellForRouteBase(TestFactory.getCourierCell(), null, 3, false)
        val cell5 = mapToCellForRouteBase(TestFactory.getCourierCell(), null, 0, true)
        val route = TestFactory.getRoute(cells = listOf(cell1, cell2, cell4, cell5))
        `when`(networkRouteUseCases.getRoute(magistralRouteInfo.routeId)).thenReturn(route)
        viewModel.updateRouteInfo()

        assertThat(magistralRouteInfoCache.value).isEqualTo(MagistralRouteInfoMapper.map(route))

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.cells).isEqualTo(listOf(cell1, cell2, cell4))
        assertThat(uiState.isLoading).isFalse()
    }

    @Test
    fun `update route info with finished cell`() = runTest {
        val finishedCell = cell2.copy(lotCount = 0, empty = true)
        val route = TestFactory.getRoute(cells = listOf(cell1, finishedCell))
        `when`(networkRouteUseCases.getRoute(magistralRouteInfo.routeId)).thenReturn(route)
        viewModel.updateRouteInfo()

        assertThat(magistralRouteInfoCache.value).isEqualTo(MagistralRouteInfoMapper.map(route))

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.cells).isEqualTo(listOf(cell1, finishedCell))
        assertThat(uiState.isLoading).isFalse()
    }
}
