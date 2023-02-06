package ru.beru.sortingcenter.ui.prepare.lot.scan

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.beru.sortingcenter.ui.prepare.data.cache.PrepareLotCache
import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.core.network.domain.NetworkLotUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.core.utils.data.event.OnceEvent
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.beru.sortingcenter.ui.prepare.lot.scan.models.scanner.ScannerModeImpl as ScannerMode

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class PrepareLotScanViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkLotUseCases: NetworkLotUseCases

    @Mock
    private lateinit var lotCache: PrepareLotCache

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: PrepareLotScanViewModel

    @Before
    fun setUp() {

        viewModel = PrepareLotScanViewModel(stringManager, networkLotUseCases, lotCache, appMetrica)
    }

    @After
    fun tearDown() {
        viewModel.forceReset()
    }

    @Test
    fun `scan order that can be pre-shipped`() {
        Mocks.Lots.preshippable.let { lot ->
            val lotScanResult = ScanResultFactory.getScanResultQR(lot.id.toString())

            runTest {
                `when`(networkLotUseCases.getLot(ExternalId(lot.id))).thenReturn(lot)

                viewModel.processScanResult(lotScanResult)

                assertEquals(
                    OnceEvent(Unit).get(),
                    viewModel.lotScanEvent.getOrAwaitValue().get()
                )
            }
        }
    }

    @Test
    fun `scan order that cannot be found`() {
        Mocks.Lots.notFound.let { lot ->
            val errorResponse = TestFactory.getResponseError<Lot>(code = 404)
            val lotScanResult = ScanResultFactory.getScanResultQR(lot.id.toString())

            runTest {
                `when`(networkLotUseCases.getLot(ExternalId(lot.id))).thenThrow(
                    HttpException(errorResponse)
                )

                viewModel.processScanResult(lotScanResult)

                assertNull(viewModel.lotScanEvent.value)
                assertNotNull(viewModel.label.text.getOrAwaitValue())
                assertEquals(
                    viewModel.scanner.overlayState.getOrAwaitValue(),
                    OverlayState.Failure
                )
                assertEquals(
                    ScannerMode.LotQRCode,
                    viewModel.scanner.mode.getOrAwaitValue()
                )
            }
        }
    }
}
