package ru.yandex.market.sc.feature.dispatch_magistral_route.presenter.lots

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.data.outbound.OutboundIdentifier
import ru.yandex.market.sc.core.data.outbound.OutboundIdentifierMapper
import ru.yandex.market.sc.core.data.route.OutgoingCourierRouteType
import ru.yandex.market.sc.core.network.domain.NetworkLotUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOutboundUseCases
import ru.yandex.market.sc.core.ui.data.formatter.LotDimensionsFormatter
import ru.yandex.market.sc.feature.dispatch_magistral_route.analytics.AppMetrica
import ru.yandex.market.sc.feature.dispatch_magistral_route.data.MagistralRouteInfo
import ru.yandex.market.sc.feature.dispatch_magistral_route.data.MagistralRouteInfoCache
import ru.yandex.market.sc.feature.dispatch_magistral_route.data.lot.LotIdentifier
import ru.yandex.market.sc.feature.dispatch_magistral_route.data.lot.ScanLotMode
import ru.yandex.market.sc.test.network.mocks.CellForRouteTestFactory
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ScanLotViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkLotUseCases: NetworkLotUseCases

    @Mock
    private lateinit var networkOutboundUseCases: NetworkOutboundUseCases

    @Mock
    private lateinit var lotDimensionsFormatter: LotDimensionsFormatter

    @Mock
    private lateinit var appMetrica: AppMetrica

    private val magistralRouteInfoCache = MagistralRouteInfoCache()
    private val stringManager = TestStringManager()
    private lateinit var viewModel: ScanLotViewModel

    private val routeId = 1L
    private val cellWithOneLot = CellForRouteTestFactory.mapToCellForRouteBase(
        TestFactory.getCourierCell(),
        null,
        1,
        false,
    )
    private val magistralRouteInfo = MagistralRouteInfo(
        routeId = routeId,
        name = "СЦ МК Краснодар",
        type = OutgoingCourierRouteType.MAGISTRAL,
        lotsTotal = 9,
        cells = (1..3).map {
            CellForRouteTestFactory.mapToCellForRouteBase(
                TestFactory.getCourierCell(),
                null,
                3,
                false
            )
        }
    )
    private val lot = TestFactory.createLot().build()
    private val outboundIdentifier = OutboundIdentifier("outbound-external-id", "EXTERNAL_ID")
    private val vehicleIdentifier =
        OutboundIdentifier("outbound-external-id", OutboundIdentifier.VEHICLE_NUM)

    @Before
    fun setUp() {
        magistralRouteInfoCache.value = magistralRouteInfo
        initViewModel()
    }

    private fun initViewModel() {
        viewModel = ScanLotViewModel(
            networkLotUseCases,
            networkOutboundUseCases,
            magistralRouteInfoCache,
            stringManager,
            lotDimensionsFormatter,
            appMetrica,
        )
    }

    @Test
    fun `decrease rest lot count after bind to outbound`() = runTest {
        assertScannerMode(ScanLotMode.LotQrCode)
        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)
        val outboundJson = OutboundIdentifierMapper.getOutboundIdentifierJson(outboundIdentifier)
        val scanOutboundResult = ScanResultFactory.getScanResultQR(outboundJson)

        `when`(networkLotUseCases.getAvailableLotDimensions()).thenReturn(emptySet())
        `when`(networkLotUseCases.getLotForOutbound(lot.externalId)).thenReturn(lot)
        viewModel.processScan(scanLotResult)
        assertLotIdentifier(LotIdentifier.LotId(lot.externalId))

        `when`(
            networkOutboundUseCases.bindLotToOutbound(
                routeId,
                lot.externalId,
                outboundIdentifier
            )
        )
            .thenReturn(Unit)
        viewModel.processScan(scanOutboundResult)

        assertLoadingState(false)
        assertScannerMode(ScanLotMode.LotQrCode)
        assertLotsRest(magistralRouteInfo.lotsTotal - 1)
    }

    @Test
    fun `decrease rest lot count after bind to vehicle`() = runTest {
        assertScannerMode(ScanLotMode.LotQrCode)
        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)
        val scanVehicleResult = ScanResultFactory.getScanResultManual(vehicleIdentifier.value)

        `when`(networkLotUseCases.getAvailableLotDimensions()).thenReturn(emptySet())
        `when`(networkLotUseCases.getLotForOutbound(lot.externalId)).thenReturn(lot)
        viewModel.processScan(scanLotResult)
        assertLotIdentifier(LotIdentifier.LotId(lot.externalId))

        `when`(
            networkOutboundUseCases.bindLotToOutboundByVehicleNum(
                routeId,
                lot.externalId,
                vehicleIdentifier.value
            )
        )
            .thenReturn(Unit)
        viewModel.processScan(scanVehicleResult)

        assertLotsRest(magistralRouteInfo.lotsTotal - 1)
        assertLoadingState(false)
        assertScannerMode(ScanLotMode.LotQrCode)
    }

    @Test
    fun `finish cell after bound all lots to outbound`() = runTest {
        assertScannerMode(ScanLotMode.LotQrCode)
        val routeWithOneCell = magistralRouteInfo.copy(cells = listOf(cellWithOneLot))
        magistralRouteInfoCache.value = routeWithOneCell
        initViewModel()

        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)
        val outboundJson = OutboundIdentifierMapper.getOutboundIdentifierJson(outboundIdentifier)
        val scanOutboundResult = ScanResultFactory.getScanResultQR(outboundJson)

        `when`(networkLotUseCases.getAvailableLotDimensions()).thenReturn(emptySet())
        `when`(networkLotUseCases.getLotForOutbound(lot.externalId)).thenReturn(lot)
        viewModel.processScan(scanLotResult)
        assertLotIdentifier(LotIdentifier.LotId(lot.externalId))

        `when`(
            networkOutboundUseCases.bindLotToOutbound(
                routeId,
                lot.externalId,
                outboundIdentifier
            )
        )
            .thenReturn(Unit)
        viewModel.processScan(scanOutboundResult)

        assertThat(viewModel.finishRouteEvent.getOrAwaitValue().get()).isEqualTo(Unit)
        assertLoadingState(false)
        assertScannerMode(ScanLotMode.LotQrCode)
    }

    @Test
    fun `finish cell after bound all lots to vehicle`() = runTest {
        assertScannerMode(ScanLotMode.LotQrCode)
        val routeWithOneCell = magistralRouteInfo.copy(cells = listOf(cellWithOneLot))
        magistralRouteInfoCache.value = routeWithOneCell
        initViewModel()

        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)
        val scanVehicleResult = ScanResultFactory.getScanResultManual(vehicleIdentifier.value)

        `when`(networkLotUseCases.getAvailableLotDimensions()).thenReturn(emptySet())
        `when`(networkLotUseCases.getLotForOutbound(lot.externalId)).thenReturn(lot)
        viewModel.processScan(scanLotResult)
        assertLotIdentifier(LotIdentifier.LotId(lot.externalId))

        `when`(
            networkOutboundUseCases.bindLotToOutboundByVehicleNum(
                routeId,
                lot.externalId,
                vehicleIdentifier.value
            )
        )
            .thenReturn(Unit)
        viewModel.processScan(scanVehicleResult)

        assertThat(viewModel.finishRouteEvent.getOrAwaitValue().get()).isEqualTo(Unit)
        assertLoadingState(false)
        assertScannerMode(ScanLotMode.LotQrCode)
    }

    @Test
    fun `error during get lot request`() = runTest {
        assertScannerMode(ScanLotMode.LotQrCode)
        val errorMessage = "Lot not found"
        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)

        `when`(networkLotUseCases.getLotForOutbound(lot.externalId)).thenThrow(
            RuntimeException(
                errorMessage
            )
        )
        viewModel.processScan(scanLotResult)

        assertLoadingState(false)
        assertLotIdentifier(null)
        assertErrorMessage(errorMessage)
        assertScannerMode(ScanLotMode.LotQrCode)
    }

    @Test
    fun `error during bind to outbound request`() = runTest {
        assertScannerMode(ScanLotMode.LotQrCode)
        val errorMessage = "Outbound not found"
        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)
        val outboundJson = OutboundIdentifierMapper.getOutboundIdentifierJson(outboundIdentifier)
        val scanOutboundResult = ScanResultFactory.getScanResultQR(outboundJson)

        `when`(networkLotUseCases.getAvailableLotDimensions()).thenReturn(emptySet())
        `when`(networkLotUseCases.getLotForOutbound(lot.externalId)).thenReturn(lot)
        viewModel.processScan(scanLotResult)
        assertLotIdentifier(LotIdentifier.LotId(lot.externalId))

        `when`(
            networkOutboundUseCases.bindLotToOutbound(
                routeId,
                lot.externalId,
                outboundIdentifier
            )
        )
            .thenThrow(RuntimeException(errorMessage))
        viewModel.processScan(scanOutboundResult)

        assertLoadingState(false)
        assertLotIdentifier(null)
        assertErrorMessage(errorMessage)
        assertScannerMode(ScanLotMode.LotQrCode)
    }

    @Test
    fun `error during scan lot - wrong lot status`() = runTest {
        assertScannerMode(ScanLotMode.LotQrCode)
        val errorMessage = "Wrong lot status"
        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)

        `when`(networkLotUseCases.getLotForOutbound(lot.externalId)).thenThrow(
            RuntimeException(
                errorMessage
            )
        )
        viewModel.processScan(scanLotResult)

        assertLoadingState(false)
        assertLotIdentifier(null)
        assertErrorMessage(errorMessage)
        assertScannerMode(ScanLotMode.LotQrCode)
    }

    @Test
    fun `error during bind to vehicle request`() = runTest {
        assertScannerMode(ScanLotMode.LotQrCode)
        val errorMessage = "Outbound not found"
        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)
        val scanVehicleResult = ScanResultFactory.getScanResultManual(vehicleIdentifier.value)

        `when`(networkLotUseCases.getAvailableLotDimensions()).thenReturn(emptySet())
        `when`(networkLotUseCases.getLotForOutbound(lot.externalId)).thenReturn(lot)
        viewModel.processScan(scanLotResult)
        assertLotIdentifier(LotIdentifier.LotId(lot.externalId))

        `when`(
            networkOutboundUseCases.bindLotToOutboundByVehicleNum(
                routeId,
                lot.externalId,
                vehicleIdentifier.value
            )
        )
            .thenThrow(RuntimeException(errorMessage))
        viewModel.processScan(scanVehicleResult)
        assertLotsRest(magistralRouteInfo.lotsTotal)

        assertLoadingState(false)
        assertLotIdentifier(null)
        assertErrorMessage(errorMessage)
        assertScannerMode(ScanLotMode.LotQrCode)
    }

    @Test
    fun `success scan stamp on dropoff type`() = runTest {
        magistralRouteInfoCache.value = magistralRouteInfo.copy(type = OutgoingCourierRouteType.DROPOFF)
        initViewModel()

        assertScannerMode(ScanLotMode.StampBarcode)
        val stampId = IdManager.getExternalId()
        val scanStampResult = ScanResultFactory.getScanResultBarcode(stampId)

        viewModel.processScan(scanStampResult)

        assertScannerMode(ScanLotMode.StampBarcode)
        assertLoadingState(false)
        assertScannerMode(ScanLotMode.StampBarcode)
    }

    @Test
    fun `error during scan stamp on dropoff type`() = runTest {
        magistralRouteInfoCache.value = magistralRouteInfo.copy(type = OutgoingCourierRouteType.DROPOFF)
        initViewModel()

        assertScannerMode(ScanLotMode.StampBarcode)
        val stampId = IdManager.getExternalId()
        val scanStampResult = ScanResultFactory.getScanResultBarcode(stampId)

        val errorMessage = "Wrong stamp"
        `when`(networkOutboundUseCases.bindLotWithStampToOutbound(routeId, stampId, null))
            .thenThrow(RuntimeException(errorMessage))
        viewModel.processScan(scanStampResult)

        assertLoadingState(false)
        assertLotIdentifier(null)
        assertErrorMessage(errorMessage)
        assertScannerMode(ScanLotMode.StampBarcode)
    }

    @Test
    fun `success scan stamp on dropoff with outbound type`() = runTest {
        magistralRouteInfoCache.value = magistralRouteInfo.copy(
            type = OutgoingCourierRouteType.DROPOFF_WITH_OUTBOUND,
            cells = listOf(cellWithOneLot),
        )
        initViewModel()

        assertScannerMode(ScanLotMode.StampBarcode)
        val stampId = IdManager.getExternalId()
        val scanStampResult = ScanResultFactory.getScanResultBarcode(stampId)
        val outboundJson = OutboundIdentifierMapper.getOutboundIdentifierJson(outboundIdentifier)
        val scanOutboundResult = ScanResultFactory.getScanResultQR(outboundJson)

        viewModel.processScan(scanStampResult)
        assertScannerMode(ScanLotMode.OutboundQrCode)
        assertLoadingState(false)

        viewModel.processScan(scanOutboundResult)

        assertLoadingState(false)
        assertScannerMode(ScanLotMode.StampBarcode)
        assertThat(viewModel.finishRouteEvent.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    @Test
    fun `error during scan stamp on dropoff with outbound type`() = runTest {
        magistralRouteInfoCache.value = magistralRouteInfo.copy(type = OutgoingCourierRouteType.DROPOFF_WITH_OUTBOUND)
        initViewModel()

        assertScannerMode(ScanLotMode.StampBarcode)
        val stampId = IdManager.getExternalId()
        val scanStampResult = ScanResultFactory.getScanResultBarcode(stampId)
        val outboundJson = OutboundIdentifierMapper.getOutboundIdentifierJson(outboundIdentifier)
        val scanOutboundResult = ScanResultFactory.getScanResultQR(outboundJson)

        viewModel.processScan(scanStampResult)
        assertScannerMode(ScanLotMode.OutboundQrCode)

        val errorMessage = "Wrong outbound"
        `when`(
            networkOutboundUseCases.bindLotWithStampToOutbound(
                routeId,
                stampId,
                outboundIdentifier
            )
        ).thenThrow(RuntimeException(errorMessage))

        viewModel.processScan(scanOutboundResult)

        assertLoadingState(false)
        assertLotIdentifier(null)
        assertErrorMessage(errorMessage)
        assertScannerMode(ScanLotMode.StampBarcode)
    }

    private fun assertScannerMode(mode: ScanLotMode) {
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.mode).isEqualTo(mode)
    }

    private fun assertLotsRest(rest: Int) {
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.lotsRest).isEqualTo(rest)
    }

    private fun assertLotIdentifier(lotIdentifier: LotIdentifier?) {
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.lotIdentifier).isEqualTo(lotIdentifier)
    }

    private fun assertLoadingState(isLoading: Boolean = false) {
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.isLoading).isEqualTo(isLoading)
    }

    private fun assertErrorMessage(message: String) {
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.errorText).isEqualTo(message)
    }
}
