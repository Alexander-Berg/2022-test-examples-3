package ru.yandex.market.sc.feature.tasks.collect.presenter.routes

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.network.arch.data.Pageable
import ru.yandex.market.sc.core.network.domain.NetworkRouteTaskUseCases
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.network.mocks.errorResource
import ru.yandex.market.sc.test.network.mocks.successResource
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@FlowPreview
@RunWith(MockitoJUnitRunner.StrictStubs::class)
class RouteListScreenViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkRouteTaskUseCases: NetworkRouteTaskUseCases

    private lateinit var viewModel: RouteListScreenViewModel

    private val routeEntryList = (0..3).map { TestFactory.createRouteTaskEntry("route-entry-$it") }

    @Before
    fun setUp() {
        viewModel = RouteListScreenViewModel(networkRouteTaskUseCases)
    }

    @Ignore("Flow does not emit events")
    @Test
    fun loadAllRoutes() = runTest {
        `when`(networkRouteTaskUseCases.getBufferReturnsRouteTaskList("", Pageable.unpaged()))
            .thenReturn(successResource(TestFactory.getPage(routeEntryList)))

        viewModel.onSearch("")
        assertThat(viewModel.isSuccess.getOrAwaitValue()).isTrue()
        assertThat(viewModel.routeList.getOrAwaitValue().size).isEqualTo(routeEntryList.size)
    }

    @Ignore("Flow does not emit events")
    @Test
    fun debounceSearchRoutes() = runTest {
        `when`(networkRouteTaskUseCases.getBufferReturnsRouteTaskList("", Pageable.unpaged()))
            .thenReturn(successResource(TestFactory.getPage(routeEntryList)))

        viewModel.onSearch("")
        viewModel.onSearch("1")
        assertThat(viewModel.isSuccess.getOrAwaitValue()).isTrue()
        verify(networkRouteTaskUseCases).getBufferReturnsRouteTaskList("", Pageable.unpaged())
    }

    @Ignore("Flow does not emit events")
    @Test
    fun failedLoadRoutes() = runTest {
        val message = "Routes do not loaded"
        `when`(networkRouteTaskUseCases.getBufferReturnsRouteTaskList("", Pageable.unpaged()))
            .thenReturn(errorResource(message))

        viewModel.onSearch("")
        assertThat(viewModel.isSuccess.getOrAwaitValue()).isFalse()
        assertThat(viewModel.isError.getOrAwaitValue()).isTrue()
    }

}
