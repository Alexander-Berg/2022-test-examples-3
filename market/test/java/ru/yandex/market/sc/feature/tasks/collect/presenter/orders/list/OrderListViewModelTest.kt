package ru.yandex.market.sc.feature.tasks.collect.presenter.orders.list

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.data.tasks.TaskType
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class OrderListViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    private val stringManager = TestStringManager()
    private lateinit var viewModel: OrderListViewModel

    private val routeId = 1L
    private val cellId = 2L
    private val routeOrderIds = TestFactory.getRouteOrderIds()

    @Before
    fun setUp() {
        viewModel = OrderListViewModel(networkOrderUseCases, stringManager)
    }

    @Test
    fun successLoadList() = runTest {
        `when`(networkOrderUseCases.getRouteOrderIds(routeId, cellId, TaskType.BUFFER_RETURN))
            .thenReturn(routeOrderIds)
        viewModel.init(routeId, cellId)

        assertThat(viewModel.orderItems.getOrAwaitValue().size).isEqualTo(routeOrderIds.size)
    }

    @Test
    fun failureLoadList() = runTest {
        val errorMessage = "Неверный пароль"
        `when`(networkOrderUseCases.getRouteOrderIds(routeId, cellId, TaskType.BUFFER_RETURN))
            .thenThrow(RuntimeException(errorMessage))
        viewModel.init(routeId, cellId)

        assertThat(viewModel.orderItems.getOrAwaitValue().size).isEqualTo(0)
        assertThat(viewModel.errorText.getOrAwaitValue()).isEqualTo(errorMessage)
    }
}
