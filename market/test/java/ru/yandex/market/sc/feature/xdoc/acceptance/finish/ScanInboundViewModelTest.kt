package ru.yandex.market.sc.feature.xdoc.acceptance.finish

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.network.domain.NetworkInboundUseCases
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.feature.xdoc.acceptance.finish.presenter.scan.ScanInboundViewModel
import ru.yandex.market.sc.feature.xdoc.acceptance.finish.presenter.scan.data.NavigationEvent
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ScanInboundViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkInboundUseCases: NetworkInboundUseCases

    private lateinit var viewModel: ScanInboundViewModel

    @Before
    fun setUp() {
        viewModel = ScanInboundViewModel(
            networkInboundUseCases = networkInboundUseCases
        )
    }

    @Test
    fun `successful inbound scan`() = runTest {
        val inbound = TestFactory.getInbound()
        val scanResult = ScanResultFactory.getScanResultBarcode(inbound.externalId)
        `when`(networkInboundUseCases.getInbound(ExternalId(scanResult.value)))
            .thenReturn(inbound)
        viewModel.navigationEvent.test {
            viewModel.onInboundScan(scanResult)
            assertThat(awaitItem()).isEqualTo(
                NavigationEvent.AcceptanceResults(
                    inboundExternalId = inbound.externalId
                )
            )
        }
    }
}
