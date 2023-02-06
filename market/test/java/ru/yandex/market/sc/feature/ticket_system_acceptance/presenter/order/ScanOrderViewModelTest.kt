package ru.yandex.market.sc.feature.ticket_system_acceptance.presenter.order

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.network.domain.NetworkAcceptanceUseCases
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.network.vo.Status
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.feature.ticket_system_acceptance.analytics.AppMetrica
import ru.yandex.market.sc.feature.ticket_system_acceptance.presenter.order.state.ScanOrderMode
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ScanOrderViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkAcceptanceUseCases: NetworkAcceptanceUseCases

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var networkCheckUserUseCases: NetworkCheckUserUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: ScanOrderViewModel

    private val ticketId = "ticketId"

    @Before
    fun setUp() {
        viewModel = ScanOrderViewModel(
            networkOrderUseCases,
            networkCheckUserUseCases,
            networkAcceptanceUseCases,
            appMetrica,
            stringManager
        )
        viewModel.init(ticketId)

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.mode).isEqualTo(ScanOrderMode.OrderBarcode)

    }

    @Test
    fun `finish acceptance with error`() = runTest {
        val message = "Что-то пошло не так"

        `when`(networkAcceptanceUseCases.finishAcceptance(ticketId)).thenThrow(RuntimeException(message))
        viewModel.onFinishAcceptance()

        val event = viewModel.finishAcceptanceEvent.getOrAwaitValue()
        assertThat(event.get()).isEqualTo(Status.Error)
    }

    @Test
    fun `finish acceptance successfully`() = runTest {
        `when`(networkAcceptanceUseCases.finishAcceptance(ticketId)).thenReturn(Unit)
        viewModel.onFinishAcceptance()

        val event = viewModel.finishAcceptanceEvent.getOrAwaitValue()
        assertThat(event.get()).isEqualTo(Status.Success)
    }

    @Test
    fun `scan order with wrong format`() = runTest {
        val order = TestFactory.createOrderForToday(1).build()
        val scanOrderResult = ScanResultFactory.getScanResultQR(order.externalId)

        viewModel.onScan(scanOrderResult)
        val uiState = viewModel.uiState.getOrAwaitValue()

        assertThat(uiState.mode).isEqualTo(ScanOrderMode.OrderBarcode)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Failure)
        assertThat(uiState.errorText).isNotEmpty()
    }

    @Test
    fun `scan place with wrong format`() = runTest {
        val order = TestFactory.createOrderForToday(2).build()
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getScanResultQR(place.externalId)

        `when`(networkOrderUseCases.acceptOrderWithTicketId(ticketId, order.externalId)).thenReturn(order)
        viewModel.onScan(scanOrderResult)
        assertThat(viewModel.uiState.getOrAwaitValue().mode).isEqualTo(ScanOrderMode.PlaceBarcode)

        viewModel.onScan(scanPlaceResult)
        val uiState = viewModel.uiState.getOrAwaitValue()

        assertThat(uiState.mode).isEqualTo(ScanOrderMode.PlaceBarcode)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Failure)
        assertThat(uiState.errorText).isNotEmpty()
    }

    @Test
    fun `scan not valid order`() = runTest {
        val order = TestFactory.getOrderNotFound()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptOrderWithTicketId(ticketId, order.externalId)).thenReturn(order)
        viewModel.onScan(scanOrderResult)
        val uiState = viewModel.uiState.getOrAwaitValue()

        assertThat(uiState.mode).isEqualTo(ScanOrderMode.OrderBarcode)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Failure)
        assertThat(uiState.errorText).isNotEmpty()
    }

    @Test
    fun `scan sorted order`() = runTest {
        val order = TestFactory.createOrderForToday(1).sort().build()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptOrderWithTicketId(ticketId, order.externalId)).thenReturn(order)
        viewModel.onScan(scanOrderResult)
        val uiState = viewModel.uiState.getOrAwaitValue()

        assertThat(uiState.mode).isEqualTo(ScanOrderMode.OrderBarcode)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Failure)
        assertThat(uiState.errorText).isNotEmpty()
    }

    @Test
    fun `scan place not from order`() = runTest {
        val order = TestFactory.createOrderForToday(2).sort(0).build()
        val fakePlaceExternalId = IdManager.getExternalId()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(fakePlaceExternalId)

        `when`(networkOrderUseCases.acceptOrderWithTicketId(ticketId, order.externalId)).thenReturn(order)
        viewModel.onScan(scanOrderResult)
        assertThat(viewModel.uiState.getOrAwaitValue().mode).isEqualTo(ScanOrderMode.PlaceBarcode)

        viewModel.onScan(scanPlaceResult)
        val uiState = viewModel.uiState.getOrAwaitValue()

        assertThat(uiState.mode).isEqualTo(ScanOrderMode.PlaceBarcode)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Failure)
        assertThat(uiState.errorText).isNotEmpty()
    }

    @Test
    fun `scan sorted place`() = runTest {
        val order = TestFactory.createOrderForToday(2).sort(0).build()
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrderWithTicketId(ticketId, order.externalId)).thenReturn(order)
        viewModel.onScan(scanOrderResult)
        assertThat(viewModel.uiState.getOrAwaitValue().mode).isEqualTo(ScanOrderMode.PlaceBarcode)

        viewModel.onScan(scanPlaceResult)
        val uiState = viewModel.uiState.getOrAwaitValue()

        assertThat(uiState.mode).isEqualTo(ScanOrderMode.PlaceBarcode)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Failure)
        assertThat(uiState.errorText).isNotEmpty()
    }

    @Test
    fun `scan order with one place`() = runTest {
        val order = TestFactory.createOrderForToday(1).build()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptOrderWithTicketId(ticketId, order.externalId)).thenReturn(order)
        viewModel.onScan(scanOrderResult)
        val uiState = viewModel.uiState.getOrAwaitValue()

        assertThat(uiState.mode).isEqualTo(ScanOrderMode.OrderBarcode)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Success)
        assertThat(uiState.order).isEqualTo(order)
        assertThat(viewModel.scannedOrders.getOrAwaitValue()).isEqualTo(1)
        assertThat(viewModel.scannedPlaces.getOrAwaitValue()).isEqualTo(1)
    }

    @Test
    fun `scan order with multiple places`() = runTest {
        val order = TestFactory.createOrderForToday(2).build()
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrderWithTicketId(ticketId, order.externalId)).thenReturn(order)
        viewModel.onScan(scanOrderResult)

        assertThat(viewModel.uiState.getOrAwaitValue().mode).isEqualTo(ScanOrderMode.PlaceBarcode)
        assertThat(viewModel.scannedOrders.getOrAwaitValue()).isEqualTo(0)
        assertThat(viewModel.scannedPlaces.getOrAwaitValue()).isEqualTo(0)

        `when`(networkOrderUseCases.acceptOrderWithTicketId(ticketId, order.externalId, place.externalId)).thenReturn(
            order
        )
        viewModel.onScan(scanPlaceResult)
        val uiState = viewModel.uiState.getOrAwaitValue()

        assertThat(uiState.mode).isEqualTo(ScanOrderMode.OrderBarcode)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Success)
        assertThat(uiState.order).isEqualTo(order)
        assertThat(uiState.place).isEqualTo(place)
        assertThat(viewModel.scannedOrders.getOrAwaitValue()).isEqualTo(1)
        assertThat(viewModel.scannedPlaces.getOrAwaitValue()).isEqualTo(1)
    }

    @Test
    fun `scan order by place`() = runTest {
        val order = TestFactory.createOrderForToday(2).build()
        val place = order.places.first()
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrderWithTicketId(ticketId, place.externalId)).thenReturn(order)
        viewModel.onScan(scanPlaceResult)
        val uiState = viewModel.uiState.getOrAwaitValue()

        assertThat(uiState.mode).isEqualTo(ScanOrderMode.OrderBarcode)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Success)
        assertThat(uiState.order).isEqualTo(order)
        assertThat(uiState.place).isEqualTo(place)
        assertThat(viewModel.scannedOrders.getOrAwaitValue()).isEqualTo(1)
        assertThat(viewModel.scannedPlaces.getOrAwaitValue()).isEqualTo(1)
    }
}
