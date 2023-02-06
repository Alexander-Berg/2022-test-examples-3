package ru.yandex.market.sc.feature.accept.lots

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
import ru.yandex.market.sc.feature.accept.lots.analytics.AppMetrica
import ru.yandex.market.sc.feature.accept.lots.presenter.AcceptLotsViewModel
import ru.yandex.market.sc.feature.accept.lots.presenter.data.AcceptLotMode
import ru.yandex.market.sc.feature.accept.lots.presenter.data.UiState
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class AcceptLotsViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkLotUseCases: NetworkLotUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: AcceptLotsViewModel

    @Before
    fun setUp() {
        viewModel = AcceptLotsViewModel(
            stringManager,
            appMetrica,
            networkLotUseCases
        )

        assertThat(uiStateValue.mode).isEqualTo(AcceptLotMode.StampBarcode)
        assertThat(uiStateValue.overlayStatus).isEqualTo(OverlayState.None)
    }

    @Test
    fun `success accept lot`() = runTest {
        val stampId = IdManager.getExternalId()
        val lot = TestFactory.createLot().setName("Test lot name").build()
        val scanResult = ScanResultFactory.getScanResultBarcode(stampId)

        `when`(networkLotUseCases.acceptLot(stampId)).thenReturn(lot)
        viewModel.onScan(scanResult)

        assertThat(uiStateValue.lotName).isEqualTo(lot.name)
        assertThat(uiStateValue.stampId).isEqualTo(stampId.value)
        assertThat(uiStateValue.overlayStatus).isEqualTo(OverlayState.Success)
        assertThat(uiStateValue.mode).isEqualTo(AcceptLotMode.StampBarcode)
    }

    @Test
    fun `scan stamp id with error`() = runTest {
        val stampId = IdManager.getExternalId()
        val scanResult = ScanResultFactory.getScanResultBarcode(stampId)

        val errorMessage = "Test error message"
        `when`(networkLotUseCases.acceptLot(stampId)).thenThrow(RuntimeException(errorMessage))
        viewModel.onScan(scanResult)

        assertThat(uiStateValue.errorText).isEqualTo(errorMessage)
        assertThat(uiStateValue.overlayStatus).isEqualTo(OverlayState.Failure)
        assertThat(uiStateValue.mode).isEqualTo(AcceptLotMode.StampBarcode)
    }

    private val uiStateValue: UiState get() = viewModel.uiState.value
}
