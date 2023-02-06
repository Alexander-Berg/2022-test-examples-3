package ru.beru.sortingcenter.ui.prepare.lot.stamp

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.beru.sortingcenter.ui.prepare.lot.stamp.models.ScanStampAction
import ru.beru.sortingcenter.ui.prepare.lot.stamp.models.ScanStampMode
import ru.beru.sortingcenter.ui.prepare.lot.stamp.models.UiState
import ru.yandex.market.sc.core.network.domain.NetworkLotUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ScanStampViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var appMetrica: AppMetrica

    @Mock
    private lateinit var networkLotUseCases: NetworkLotUseCases

    private lateinit var viewModel: ScanStampViewModel

    private val lot = TestFactory.createLot().build()

    @Test
    fun `success stamp scan with ADD_STAMP action`() = runTest {
        initViewModel(
            lotExternalId = lot.externalId,
            lotName = lot.name,
            scanStampAction = ScanStampAction.ADD_STAMP
        )
        viewModel.onScan(ScanResultFactory.getScanResultBarcode(IdManager.getExternalId()))

        val resultUiState = viewModel.uiState.getOrAwaitValue()

        assertThat(resultUiState.status).isEqualTo(OverlayState.Success)
        assertThat(resultUiState.mode).isEqualTo(ScanStampMode.DoNotScan)
        assertThat(viewModel.updateLotEvent.getOrAwaitValue().get()).isEqualTo(lot.externalId)
    }

    @Test
    fun `success stamp scan with DELETE_STAMP action`() = runTest {
        initViewModel(
            lotExternalId = lot.externalId,
            lotName = lot.name,
            scanStampAction = ScanStampAction.DELETE_STAMP
        )
        viewModel.onScan(ScanResultFactory.getScanResultBarcode(IdManager.getExternalId()))

        val resultUiState = viewModel.uiState.getOrAwaitValue()

        assertThat(resultUiState.status).isEqualTo(OverlayState.Success)
        assertThat(resultUiState.mode).isEqualTo(ScanStampMode.DoNotScan)
        assertThat(viewModel.updateLotEvent.getOrAwaitValue().get()).isEqualTo(lot.externalId)
    }

    @Test
    fun `fail stamp scan with ADD_STAMP action`() = runTest {
        val stampId = IdManager.getExternalId()
        initViewModel(
            lotExternalId = lot.externalId,
            lotName = lot.name,
            scanStampAction = ScanStampAction.ADD_STAMP
        )
        val response = TestFactory.getResponseError<Int>(code = 400)
        `when`(networkLotUseCases.addStamp(lotExternalId = lot.externalId, stampId = stampId))
            .thenThrow(HttpException(response))
        viewModel.onScan(ScanResultFactory.getScanResultBarcode(stampId))

        val resultUiState = viewModel.uiState.getOrAwaitValue()

        assertThat(resultUiState.status).isEqualTo(OverlayState.Failure)
        assertThat(resultUiState.mode).isEqualTo(ScanStampMode.StampBarcode)
    }

    @Test
    fun `fail stamp scan with DELETE_STAMP action`() = runTest {
        val stampId = IdManager.getExternalId()
        initViewModel(
            lotExternalId = lot.externalId,
            lotName = lot.name,
            scanStampAction = ScanStampAction.DELETE_STAMP
        )
        val response = TestFactory.getResponseError<Int>(code = 400)
        `when`(networkLotUseCases.deleteStamp(lotExternalId = lot.externalId, stampId = stampId))
            .thenThrow(HttpException(response))
        viewModel.onScan(ScanResultFactory.getScanResultBarcode(stampId))

        val resultUiState = viewModel.uiState.getOrAwaitValue()

        assertThat(resultUiState.status).isEqualTo(OverlayState.Failure)
        assertThat(resultUiState.mode).isEqualTo(ScanStampMode.StampBarcode)
    }

    @Test
    fun `wrong scan format`() = runTest {
        val stampId = IdManager.getExternalId()
        initViewModel(
            lotExternalId = lot.externalId,
            lotName = lot.name,
            scanStampAction = ScanStampAction.ADD_STAMP
        )

        val errorMessage = stringManager.getString(R.string.wrong_scan_format_barcode)

        viewModel.onScan(ScanResultFactory.getScanResultQR(stampId))

        val resultUiState = viewModel.uiState.getOrAwaitValue()

        assertThat(resultUiState.status).isEqualTo(OverlayState.Failure)
        assertThat(resultUiState.errorText).isEqualTo(errorMessage)
        assertThat(resultUiState.mode).isEqualTo(ScanStampMode.StampBarcode)
    }

    private fun initViewModel(
        lotExternalId: ExternalId,
        lotName: String,
        scanStampAction: ScanStampAction
    ) {
        viewModel = ScanStampViewModel(
            appMetrica = appMetrica,
            networkLotUseCases = networkLotUseCases,
            stringManager = stringManager,
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "lotExternalId" to lotExternalId.value,
                    "lotName" to lotName,
                    "scanStampAction" to scanStampAction,
                    "title" to "Title"
                )
            )
        )
        assertThat(viewModel.uiState.getOrAwaitValue()).isEqualTo(UiState.idle(lotName = lotName))
    }
}
