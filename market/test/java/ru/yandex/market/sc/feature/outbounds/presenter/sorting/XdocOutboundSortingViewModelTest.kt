package ru.yandex.market.sc.feature.outbounds.presenter.sorting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.network.domain.NetworkSortableUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.feature.outbounds.analytics.AppMetrica
import ru.yandex.market.sc.feature.outbounds.data.ScanOrderPayload
import ru.yandex.market.sc.feature.outbounds.data.cache.ScanOrderCache
import ru.yandex.market.sc.feature.outbounds.presenter.sorting.state.SortingOrderScanMode
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class XdocOutboundSortingViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkSortableUseCases: NetworkSortableUseCases

    @Mock
    private lateinit var scanOrderCache: ScanOrderCache

    @Mock
    private lateinit var appMetrica: AppMetrica

    private val stringManager = TestStringManager()
    private lateinit var viewModel: XdocOutboundSortingViewModel

    private val order = TestFactory.createOrder().build()
    private val place = order.places.first()

    @Before
    fun setUp() {
        `when`(scanOrderCache.value).thenReturn(ScanOrderPayload(order.externalId, place))
        viewModel = XdocOutboundSortingViewModel(
            networkSortableUseCases,
            appMetrica,
            stringManager,
            scanOrderCache,
        )
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.mode).isEqualTo(SortingOrderScanMode.DispatchableQRCode)
    }

    @Test
    fun `failed to sort order`() = runTest {
        val message = "Не удалось отсортировать"
        val destination = IdManager.getExternalId()
        val scanResult = ScanResultFactory.getScanResultQR(destination)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destination,
                place.externalId
            )
        ).thenThrow(RuntimeException(message))

        viewModel.onScan(scanResult)
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.errorText).isEqualTo(message)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Failure)
        assertThat(uiState.mode).isEqualTo(SortingOrderScanMode.DispatchableQRCode)
    }

    @Test
    fun `scan wrong format`() = runTest {
        val destination = IdManager.getExternalId()
        val scanResult = ScanResultFactory.getScanResultBarcode(destination)

        viewModel.onScan(scanResult)
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.errorText).isNotEmpty()
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Failure)
        assertThat(uiState.mode).isEqualTo(SortingOrderScanMode.DispatchableQRCode)
    }

    @Test
    fun `sort order to destination`() = runTest {
        val cell = TestFactory.getCourierCell()
        val sortResponse = TestFactory.getSortResponse(cell, false)
        val destination = IdManager.getExternalId()
        val scanResult = ScanResultFactory.getScanResultQR(destination)

        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destination,
                place.externalId
            )
        ).thenReturn(sortResponse)
        viewModel.onScan(scanResult)

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.errorText).isEmpty()
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Success)

        advanceUntilIdle()
        assertThat(viewModel.finishSortingEvent.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    @Test
    fun `sort order to destination with parent`() = runTest {
        val lot = TestFactory.createCellLot()
        val sortResponse1 = TestFactory.getSortResponse(lot, true)
        val sortResponse2 = TestFactory.getSortResponse(lot, false)

        val destination = IdManager.getExternalId()
        val parentDestination = IdManager.getExternalId()
        val scanDestinationResult = ScanResultFactory.getScanResultQR(destination)
        val scanParentDestinationResult = ScanResultFactory.getScanResultQR(parentDestination)

        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destination,
                place.externalId
            )
        ).thenReturn(sortResponse1)
        viewModel.onScan(scanDestinationResult)

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.errorText).isEmpty()
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Success)
        assertThat(uiState.destination?.externalId).isEqualTo(lot.externalId)
        assertThat(uiState.mode).isEqualTo(SortingOrderScanMode.CellQRCode)

        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                lot.externalId,
                place.externalId,
                parentDestination,
            )
        ).thenReturn(sortResponse2)
        viewModel.onScan(scanParentDestinationResult)

        advanceUntilIdle()
        assertThat(viewModel.finishSortingEvent.getOrAwaitValue().get()).isEqualTo(Unit)
    }
}
