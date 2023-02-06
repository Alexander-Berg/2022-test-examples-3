package ru.yandex.market.sc.feature.common_info

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class CommonInfoViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    private lateinit var viewModel: CommonInfoViewModel

    private val state
        get() = viewModel.uiState.getOrAwaitValue()

    @Before
    fun setUp() {
        viewModel = CommonInfoViewModel(networkOrderUseCases)
    }

    @Test
    fun `success scan order`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        assertThat(state.order).isNull()

        `when`(networkOrderUseCases.getOrder(order.externalId)).thenReturn(order)
        viewModel.getOrderInformation(scanResult)

        assertThat(state.order).isEqualTo(order)
    }

    @Test
    fun `success scan place`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val place = order.places.first()
        val scanResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        assertThat(state.order).isNull()

        `when`(networkOrderUseCases.getOrder(place.externalId)).thenReturn(order)
        viewModel.getOrderInformation(scanResult)

        assertThat(state.order).isEqualTo(order)
    }
}
