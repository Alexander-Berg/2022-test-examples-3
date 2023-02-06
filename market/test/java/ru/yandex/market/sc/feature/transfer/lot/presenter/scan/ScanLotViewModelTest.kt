package ru.yandex.market.sc.feature.transfer.lot.presenter.scan

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.network.domain.NetworkLotUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.feature.transfer.lot.analytics.AppMetrica
import ru.yandex.market.sc.feature.transfer.lot.presenter.scan.data.ScanLotMode
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.yandex.market.test.utils.isNeverSet

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ScanLotViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var appMetrica: AppMetrica

    @Mock
    private lateinit var networkLotUseCases: NetworkLotUseCases

    private lateinit var viewModel: ScanLotViewModel
    private val externalIdFromLot = TestFactory.createLot().build().externalId

    @Before
    fun setUp() {
        viewModel = ScanLotViewModel(networkLotUseCases, appMetrica, stringManager)
        viewModel.init(externalIdFromLot)
    }

    @Test
    fun `success scan lot to transfer`() = runTest {
        val lotTo = TestFactory.createLot().build()
        val scanLot = ScanResultFactory.getScanResultQR(lotTo.externalId)
        `when`(networkLotUseCases.transferLot(externalIdFromLot, lotTo.externalId)).thenReturn(Unit)

        viewModel.onScan(scanLot)
        assertThat(viewModel.successTransferEvent.getOrAwaitValue().get()).isNotNull()
    }

    @Test
    fun `validate scan format`() = runTest {
        val lotTo = TestFactory.createLot().build()
        val scanLot = ScanResultFactory.getScanResultBarcode(lotTo.externalId)

        viewModel.onScan(scanLot)
        assertThat(viewModel.successTransferEvent.isNeverSet()).isTrue()
        assertThat(viewModel.uiState.getOrAwaitValue().mode).isEqualTo(ScanLotMode.LotQr)
        assertThat(viewModel.uiState.getOrAwaitValue().overlayStatus).isEqualTo(OverlayState.Failure)
    }

    @Test
    fun `transfer lot with error`() = runTest {
        val lotTo = TestFactory.createLot().build()
        val scanLot = ScanResultFactory.getScanResultQR(lotTo.externalId)
        `when`(networkLotUseCases.transferLot(externalIdFromLot, lotTo.externalId))
            .thenThrow(RuntimeException())

        viewModel.onScan(scanLot)
        assertThat(viewModel.successTransferEvent.isNeverSet()).isTrue()
        assertThat(viewModel.uiState.getOrAwaitValue().mode).isEqualTo(ScanLotMode.LotQr)
        assertThat(viewModel.uiState.getOrAwaitValue().overlayStatus).isEqualTo(OverlayState.Failure)
    }
}
