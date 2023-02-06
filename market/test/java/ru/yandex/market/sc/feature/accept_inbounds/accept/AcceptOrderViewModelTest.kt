package ru.yandex.market.sc.feature.accept_inbounds.accept

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
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.feature.accept_inbounds.analytics.AppMetrica
import ru.yandex.market.sc.feature.accept_inbounds.presenter.scan_doc.ScanDocViewModel
import ru.yandex.market.sc.feature.accept_inbounds.presenter.scan_doc.data.ScanDocMode
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.network.mocks.errorResource
import ru.yandex.market.sc.test.network.mocks.successResource
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class AcceptOrderViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkInboundUseCases: NetworkInboundUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: ScanDocViewModel

    @Before
    fun setUp() {
        viewModel = ScanDocViewModel(
            appMetrica,
            networkInboundUseCases,
            stringManager,
        )

        viewModel.reset()

        assertThat(viewModel.state.getOrAwaitValue().errorText).isEqualTo(null)
        assertThat(viewModel.state.getOrAwaitValue().mode).isEqualTo(ScanDocMode.DocBarcode)
        assertThat(viewModel.state.getOrAwaitValue().status).isEqualTo(OverlayState.None)
    }

    @Test
    fun `success accept inbound`() = runTest {
        val transportationId = IdManager.getExternalId(1)
        val warehouseFrom = "test"
        val inbound = TestFactory.getTransportationInbound(transportationId, warehouseFrom)
        val scanResult = ScanResultFactory.getScanResultBarcode(transportationId)

        `when`(networkInboundUseCases.acceptInboundByDoc(transportationId)).thenReturn(
            successResource(inbound)
        )
        viewModel.onScan(scanResult)

        assertThat(viewModel.state.getOrAwaitValue().mode).isEqualTo(ScanDocMode.DocBarcode)
        assertThat(viewModel.state.getOrAwaitValue().warehouseFrom).isEqualTo(warehouseFrom)
        assertThat(viewModel.state.getOrAwaitValue().status).isEqualTo(OverlayState.Success)
    }

    @Test
    fun `scan inbound with error`() = runTest {
        val transportationId = IdManager.getExternalId()
        val warehouseFrom = "test"
        val errorText = "some error"
        val inbound = TestFactory.getTransportationInbound(transportationId, warehouseFrom)
        val scanResult = ScanResultFactory.getScanResultBarcode(transportationId)

        `when`(networkInboundUseCases.acceptInboundByDoc(transportationId)).thenReturn(
            errorResource(errorText, inbound)
        )
        viewModel.onScan(scanResult)

        assertThat(viewModel.state.getOrAwaitValue().mode).isEqualTo(ScanDocMode.DoNotScan)
        assertThat(viewModel.state.getOrAwaitValue().warehouseFrom).isEqualTo(null)
        assertThat(viewModel.state.getOrAwaitValue().errorText).isEqualTo(errorText)
        assertThat(viewModel.state.getOrAwaitValue().status).isEqualTo(OverlayState.Failure)

        viewModel.reset()
    }
}
