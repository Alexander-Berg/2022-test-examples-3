package ru.yandex.market.sc.feature.xdoc.fix_inbound.presenter.scan

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.network.domain.NetworkInboundUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.feature.xdoc.fix_inbound.analytics.AppMetrica
import ru.yandex.market.sc.feature.xdoc.fix_inbound.data.cache.FixInboundCache
import ru.yandex.market.sc.feature.xdoc.fix_inbound.presenter.scan.state.InboundScanMode
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.yandex.market.test.utils.isNeverSet

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class XdocInboundScanViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkInboundUseCases: NetworkInboundUseCases

    @Mock
    private lateinit var fixInboundCache: FixInboundCache

    @Mock
    private lateinit var appMetrica: AppMetrica

    private val stringManager = TestStringManager()

    private lateinit var viewModel: XdocInboundScanViewModel
    private val bogusInboundId = IdManager.getExternalId(-1)
    private val inbound = TestFactory.getInbound()

    @Before
    fun setUp() {
        viewModel = XdocInboundScanViewModel(
            networkInboundUseCases,
            fixInboundCache,
            appMetrica,
            stringManager
        )

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.mode).isEqualTo(InboundScanMode.InboundCode)
    }

    @Test
    fun `scan inbound with error`() = runTest {
        val message = "Не получилось получить поставку"
        val scanResult = ScanResultFactory.getScanResultBarcode(bogusInboundId)

        `when`(networkInboundUseCases.getInbound(bogusInboundId)).thenThrow(RuntimeException(message))
        viewModel.onScan(scanResult)

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.mode).isEqualTo(InboundScanMode.InboundCode)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Failure)
        assertThat(uiState.errorText).isEqualTo(message)
        assertThat(viewModel.successScanInboundEvent.isNeverSet()).isTrue()
    }

    @Test
    fun `scan inbound successfully`() = runTest {
        val scanResult = ScanResultFactory.getScanResultBarcode(inbound.externalId)

        `when`(networkInboundUseCases.getInbound(inbound.externalId)).thenReturn(inbound)
        viewModel.onScan(scanResult)

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.errorText).isEmpty()
        verify(fixInboundCache).value = inbound

        assertThat(viewModel.successScanInboundEvent.getOrAwaitValue().get()).isEqualTo(Unit)
    }
}
