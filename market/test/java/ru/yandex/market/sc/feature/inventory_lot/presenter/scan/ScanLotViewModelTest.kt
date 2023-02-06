package ru.yandex.market.sc.feature.inventory_lot.presenter.scan

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.data.lot.LotInfo
import ru.yandex.market.sc.core.network.domain.NetworkLotUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.feature.inventory_lot.analytics.AppMetrica
import ru.yandex.market.sc.feature.inventory_lot.presenter.scan.state.LotScanMode
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ScanLotViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkLotUseCases: NetworkLotUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: ScanLotViewModel

    private val lotInfo = TestFactory.createLotInfo()
        .setLotStatus(LotInfo.Status.PROCESSING)
        .build()

    @Before
    fun setUp() {
        viewModel = ScanLotViewModel(networkLotUseCases, appMetrica, stringManager)

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.mode).isEqualTo(LotScanMode.LotQRCode)
    }

    @Test
    fun `scan with error`() = runTest {
        val message = "Что-то пошло не так"
        val scanResult = ScanResultFactory.getScanResultQR(lotInfo.externalId)

        `when`(networkLotUseCases.getLotInfo(lotInfo.externalId)).thenThrow(RuntimeException(message))
        viewModel.onScan(scanResult)

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.mode).isEqualTo(LotScanMode.LotQRCode)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Failure)
        assertThat(uiState.errorText).isEqualTo(message)
    }

    @Test
    fun `success scan lot`() = runTest {
        val scanResult = ScanResultFactory.getScanResultQR(lotInfo.externalId)

        `when`(networkLotUseCases.getLotInfo(lotInfo.externalId)).thenReturn(lotInfo)
        viewModel.onScan(scanResult)

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.mode).isEqualTo(LotScanMode.LotQRCode)
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Success)
        assertThat(uiState.lotInfo).isEqualTo(lotInfo)
    }
}
