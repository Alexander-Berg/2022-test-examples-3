package ru.yandex.market.sc.feature.ticket_system_acceptance.presenter.ticket

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
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.feature.ticket_system_acceptance.analytics.AppMetrica
import ru.yandex.market.sc.feature.ticket_system_acceptance.presenter.ticket.state.ScanTicketMode
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.yandex.market.test.utils.isNeverSet

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ScanTicketViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkAcceptanceUseCases: NetworkAcceptanceUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: ScanTicketViewModel

    private val ticketId = "ticketId"

    @Before
    fun setUp() {
        viewModel = ScanTicketViewModel(networkAcceptanceUseCases, appMetrica, stringManager)

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.mode).isEqualTo(ScanTicketMode.TicketQr)

    }

    @Test
    fun `scan wrong format`() = runTest {
        val scanResult = ScanResultFactory.getScanResultBarcode(ticketId)
        viewModel.onScan(scanResult)

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.mode).isEqualTo(ScanTicketMode.TicketQr)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Failure)
        assertThat(uiState.errorText).isNotEmpty()
        assertThat(viewModel.successTicketEvent.isNeverSet()).isTrue()
    }

    @Test
    fun `scan with error`() = runTest {
        val message = "Что-то пошло не так"
        val scanResult = ScanResultFactory.getScanResultQR(ticketId)

        `when`(networkAcceptanceUseCases.startAcceptance(ticketId)).thenThrow(
            RuntimeException(
                message
            )
        )
        viewModel.onScan(scanResult)

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.mode).isEqualTo(ScanTicketMode.TicketQr)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Failure)
        assertThat(uiState.errorText).isEqualTo(message)
        assertThat(viewModel.successTicketEvent.isNeverSet()).isTrue()
    }

    @Test
    fun `success scan ticket id`() = runTest {
        val scanResult = ScanResultFactory.getScanResultQR(ticketId)

        `when`(networkAcceptanceUseCases.startAcceptance(ticketId)).thenReturn(Unit)
        viewModel.onScan(scanResult)

        val event = viewModel.successTicketEvent.getOrAwaitValue()
        assertThat(event.get()).isEqualTo(ticketId)
    }
}
