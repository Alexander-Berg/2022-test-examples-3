package ru.yandex.market.sc.feature.outbounds.presenter.details

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.data.outbound.OutboundTask
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOutboundUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.feature.outbounds.analytics.AppMetrica
import ru.yandex.market.sc.feature.outbounds.data.ScanOrderPayload
import ru.yandex.market.sc.feature.outbounds.data.cache.ScanOrderCache
import ru.yandex.market.sc.feature.outbounds.presenter.details.state.OutboundOrderScanMode
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class XdocOutboundDetailsViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkOutboundUseCases: NetworkOutboundUseCases

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var scanOrderCache: ScanOrderCache

    @Mock
    private lateinit var appMetrica: AppMetrica

    private val stringManager = TestStringManager()

    private lateinit var viewModel: XdocOutboundDetailsViewModel

    private val cell = TestFactory.getCourierCell()
    private val outboundTask = OutboundTask(
        outbound = TestFactory.getOutbound(),
        inbounds = (0..3).map { TestFactory.getInboundTask(cells = listOf(requireNotNull(cell.number))) }
    )
    private val outboundExternalId = outboundTask.outbound.externalId

    @Before
    fun setUp() = runBlocking {
        `when`(networkOutboundUseCases.getOutbound(outboundExternalId)).thenReturn(outboundTask)
        viewModel = XdocOutboundDetailsViewModel(
            networkOutboundUseCases,
            networkOrderUseCases,
            scanOrderCache,
            appMetrica,
            stringManager,
        )
        viewModel.init(outboundExternalId)
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.outboundTask).isEqualTo(outboundTask)
    }

    @Test
    fun `failed to request outbound task on init`() = runTest {
        val message = "Не удалось получить задание"
        `when`(networkOutboundUseCases.getOutbound(outboundExternalId)).thenThrow(
            RuntimeException(
                message
            )
        )

        viewModel.init(outboundExternalId)
        assertThat(viewModel.returnToListEvent.getOrAwaitValue().get()).isEqualTo(message)
    }

    @Test
    fun `request update outbound task`() = runTest {
        val updatedOutboundTask = outboundTask.copy(inbounds = listOf())
        `when`(networkOutboundUseCases.getOutbound(outboundExternalId)).thenReturn(
            updatedOutboundTask
        )

        viewModel.requestOutboundTaskUpdate()
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.outboundTask).isEqualTo(updatedOutboundTask)
    }

    @Test
    fun `request update outbound task with error`() = runTest {
        val message = "Не удалось получить задание"
        `when`(networkOutboundUseCases.getOutbound(outboundExternalId)).thenThrow(
            RuntimeException(
                message
            )
        )

        viewModel.requestOutboundTaskUpdate()
        assertThat(viewModel.returnToListEvent.getOrAwaitValue().get()).isEqualTo(message)
    }

    @Test
    fun `scan order from another inbound`() = runTest {
        val anotherCell = TestFactory.getCourierCell()
        val order = TestFactory.createOrderForToday(1).sort(anotherCell).build()
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.onScan(scanResult)
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.errorText).isNotEmpty()
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Failure)
        assertThat(uiState.mode).isEqualTo(OutboundOrderScanMode.OrderMode)
        assertThat(uiState.outboundTask).isEqualTo(outboundTask)
    }

    @Test
    fun `scan order with error`() = runTest {
        val message = "Can not get order"
        val orderExternalId = IdManager.getExternalId(-1)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(orderExternalId)
        `when`(networkOrderUseCases.acceptOrder(orderExternalId)).thenThrow(RuntimeException(message))

        viewModel.onScan(scanResult)
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.errorText).isNotEmpty()
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Failure)
        assertThat(uiState.mode).isEqualTo(OutboundOrderScanMode.OrderMode)
        assertThat(uiState.outboundTask).isEqualTo(outboundTask)
    }

    @Test
    fun `scan order with one place`() = runTest {
        val order = TestFactory.createOrderForToday(1).sort(cell).build()
        val place = order.places.first()
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.onScan(scanResult)
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.outboundTask).isEqualTo(outboundTask)
        assertThat(uiState.mode).isEqualTo(OutboundOrderScanMode.OrderMode)
        assertThat(viewModel.startSortingEvent.getOrAwaitValue().get()).isEqualTo(Unit)
        verify(scanOrderCache).value = ScanOrderPayload(order.externalId, place)
    }

    @Test
    fun `scan multiplace order`() = runTest {
        val order = TestFactory.createOrderForToday(2).sort(cell).build()
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        viewModel.onScan(scanOrderResult)

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.outboundTask).isEqualTo(outboundTask)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Success)
        assertThat(uiState.mode).isEqualTo(OutboundOrderScanMode.PlaceMode)
        assertThat(uiState.order).isEqualTo(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, place.externalId)).thenReturn(
            order
        )
        viewModel.onScan(scanPlaceResult)

        val uiState2 = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState2.outboundTask).isEqualTo(outboundTask)
        assertThat(uiState2.mode).isEqualTo(OutboundOrderScanMode.OrderMode)
        assertThat(viewModel.startSortingEvent.getOrAwaitValue().get()).isEqualTo(Unit)
        verify(scanOrderCache).value = ScanOrderPayload(order.externalId, place)
    }

    @Test
    fun `scan multiplace order by place`() = runTest {
        val order = TestFactory.createOrderForToday(2).sort(cell).build()
        val place = order.places.first()
        val scanResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)
        `when`(networkOrderUseCases.acceptOrder(place.externalId)).thenReturn(order)

        viewModel.onScan(scanResult)
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.outboundTask).isEqualTo(outboundTask)
        assertThat(uiState.mode).isEqualTo(OutboundOrderScanMode.OrderMode)
        assertThat(viewModel.startSortingEvent.getOrAwaitValue().get()).isEqualTo(Unit)
        verify(scanOrderCache).value = ScanOrderPayload(order.externalId, place)
    }
}
