package ru.beru.sortingcenter.ui.acceptance.initial.returned

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.beru.sortingcenter.ui.acceptance.initial.returned.Asserts.`assert description text`
import ru.beru.sortingcenter.ui.acceptance.initial.returned.Asserts.`assert description`
import ru.beru.sortingcenter.ui.acceptance.initial.returned.Asserts.`assert label`
import ru.beru.sortingcenter.ui.acceptance.initial.returned.Asserts.`assert scanner fragment`
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel.*
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.feature.blocking.data.OrderInformation
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.beru.sortingcenter.ui.acceptance.initial.returned.model.scanner.ScannerModeImpl as ScannerMode

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class InitialReturnAcceptViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var networkCheckUserUseCases: NetworkCheckUserUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: InitialReturnAcceptViewModel

    private val notExistedOrderId = IdManager.getExternalId(-1)

    @Before
    fun setUp() {
        viewModel = InitialReturnAcceptViewModel(
            networkOrderUseCases,
            networkCheckUserUseCases,
            appMetrica,
            stringManager
        )
        Asserts.bind(viewModel, stringManager)

        setShouldSkipCellInfo(true)
    }

    @Test
    fun `wait for order`() {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.None,
        )

        `assert label`(
            labelStatus = LabelStatus.None,
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.None,
            cellState = CellState.None
        )
    }

    @Test
    fun `scan not exist order`() = runTest {
        val orderNotFound = TestFactory.getOrderNotFound(notExistedOrderId)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(notExistedOrderId)

        `when`(networkOrderUseCases.acceptReturnOrder(notExistedOrderId)).thenReturn(orderNotFound)

        viewModel.processScanResult(scanResult)
        `order not found`()

        viewModel.forceReset()
        `wait for order`()
    }

    @Test
    fun `scan wrong format without blocking`() = runTest {
        `when`(networkCheckUserUseCases.shouldBlockOnAcceptance()).thenReturn(false)
        val scanResult = ScanResultFactory.getUnsupportedScanResult(123)

        viewModel.processScanResult(scanResult)
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )
        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = R.string.wrong_scan_format_qr_or_barcode,
        )

        viewModel.forceReset()
        `wait for order`()
    }

    @Test
    fun `scan wrong format with blocking`() = runTest {
        `when`(networkCheckUserUseCases.shouldBlockOnAcceptance()).thenReturn(true)
        val scanResult = ScanResultFactory.getUnsupportedScanResult(123)

        viewModel.processScanResult(scanResult)
        assertThat(viewModel.wrongScanEvent.getOrAwaitValue().get()).isEqualTo(
            OrderInformation(null)
        )
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.None,
        )

        viewModel.forceReset()
        `wait for order`()
    }

    @Test
    fun `scan wrong format after scan order without blocking`() = runTest {
        val order = TestFactory.getReturnFromCourier(amountOfAvailableCells = 3)
        val place = order.places.first()
        `when`(networkOrderUseCases.acceptReturnOrder(order.externalId)).thenReturn(order)
        `when`(networkCheckUserUseCases.shouldBlockOnAcceptance()).thenReturn(false)

        val scanOrder = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanResult = ScanResultFactory.getUnsupportedScanResult(123)

        viewModel.processScanResult(scanOrder)
        `success acceptance`(place.joinedCellInfo, place.externalId)

        viewModel.processScanResult(scanResult)
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )
        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = R.string.wrong_scan_format_qr_or_barcode,
        )

        viewModel.forceReset()
        `wait for order`()
    }

    @Test
    fun `scan wrong format after scan order with blocking`() = runTest {
        val order = TestFactory.getReturnFromCourier(amountOfAvailableCells = 3)
        val place = order.places.first()
        `when`(networkOrderUseCases.acceptReturnOrder(order.externalId)).thenReturn(order)
        `when`(networkCheckUserUseCases.shouldBlockOnAcceptance()).thenReturn(true)

        val scanOrder = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanResult = ScanResultFactory.getUnsupportedScanResult(123)

        viewModel.processScanResult(scanOrder)
        `success acceptance`(place.joinedCellInfo, place.externalId)

        viewModel.processScanResult(scanResult)
        assertThat(viewModel.wrongScanEvent.getOrAwaitValue().get()).isEqualTo(
            OrderInformation(null)
        )
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.None,
        )
        viewModel.forceReset()
        `wait for order`()
    }

    @Test
    fun `one place order initial return acceptance`() = runTest {
        val order = TestFactory.getReturnFromCourier(amountOfAvailableCells = 3)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptReturnOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `success acceptance`(place.joinedCellInfo, place.externalId)

        viewModel.forceReset()
        `wait for order`()
    }

    @Test
    fun `multi place order initial return acceptance`() = runTest {
        val order = TestFactory.getReturnFromCourier(
            numberOfPlaces = 2,
            amountOfAvailableCells = 3
        )
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptReturnOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `waiting for scan place`()

        `when`(
            networkOrderUseCases.acceptReturnOrder(
                order.externalId,
                place.externalId
            )
        ).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `success acceptance`(place.joinedCellInfo, place.externalId)

        viewModel.forceReset()
        `wait for order`()
    }

    @Test
    fun `multi place order initial return acceptance with reset`() = runTest {
        val order = TestFactory.getReturnFromCourier(
            numberOfPlaces = 2,
            amountOfAvailableCells = 3
        )
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptReturnOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `waiting for scan place`()

        `when`(
            networkOrderUseCases.acceptReturnOrder(
                order.externalId,
                place.externalId
            )
        ).thenReturn(order)
        viewModel.forceReset()

        viewModel.processScanResult(scanPlaceResult)
        `success acceptance`(place.joinedCellInfo, place.externalId)

        viewModel.forceReset()
        `wait for order`()
    }

    @Test
    fun `scan order with error`() = runTest {
        val order = TestFactory.getOrderNotFound(notExistedOrderId)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        val response = TestFactory.getResponseError<Int>(code = 400)

        `when`(networkOrderUseCases.acceptReturnOrder(order.externalId)).thenThrow(
            HttpException(
                response
            )
        )

        viewModel.processScanResult(scanResult)
        `scan error`()

        viewModel.forceReset()
        `wait for order`()
    }

    @Test
    fun `scan after single place order with not skippable cell info`() = runTest {
        setShouldSkipCellInfo(false)

        val order = TestFactory.getReturnFromCourier(amountOfAvailableCells = 3)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptReturnOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `success acceptance`(place.joinedCellInfo, place.externalId, false)

        val orderForSkip = TestFactory.getReturnFromCourier(amountOfAvailableCells = 3)
        val scanOrderForSkipResult =
            ScanResultFactory.getOrderDefaultScanResult(orderForSkip.externalId)

        viewModel.processScanResult(scanOrderForSkipResult)
        `success acceptance`(place.joinedCellInfo, place.externalId, false)
    }

    @Test
    fun `one place order initial return acceptance with not skippable cell info`() = runTest {
        setShouldSkipCellInfo(false)

        val order = TestFactory.getReturnFromCourier(amountOfAvailableCells = 3)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptReturnOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `success acceptance`(place.joinedCellInfo, place.externalId, false)
    }

    @Test
    fun `multi place order initial return acceptance with not skippable cell info`() = runTest {
        setShouldSkipCellInfo(false)
        val order = TestFactory.getReturnFromCourier(
            numberOfPlaces = 2,
            amountOfAvailableCells = 3
        )
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptReturnOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `waiting for scan place`()

        `when`(
            networkOrderUseCases.acceptReturnOrder(
                order.externalId,
                place.externalId
            )
        ).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `success acceptance`(place.joinedCellInfo, place.externalId, false)
    }

    @Test
    fun `multi place order initial return acceptance with reset and not skippable cell info`() =
        runTest {
            setShouldSkipCellInfo(false)
            val order = TestFactory.getReturnFromCourier(
                numberOfPlaces = 2,
                amountOfAvailableCells = 3
            )
            val place = order.places.first()
            val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
            val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

            `when`(networkOrderUseCases.acceptReturnOrder(order.externalId)).thenReturn(order)

            viewModel.processScanResult(scanOrderResult)
            `waiting for scan place`()

            `when`(
                networkOrderUseCases.acceptReturnOrder(
                    order.externalId,
                    place.externalId
                )
            ).thenReturn(order)
            viewModel.forceReset()

        viewModel.processScanResult(scanPlaceResult)
        `success acceptance`(place.joinedCellInfo, place.externalId, false)
    }

    private fun setShouldSkipCellInfo(shouldSkipCellInfo: Boolean) {
        `when`(networkCheckUserUseCases.shouldAcceptAndSortReturn()).thenReturn(!shouldSkipCellInfo)
    }

    private fun `order not found`() {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )

        `assert label`(
            labelStatus = LabelStatus.Error,
            label = R.string.error
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = R.string.order_not_found,
        )
    }

    private fun `waiting for scan place`() {
        `assert scanner fragment`(
            scannerMode = ScannerMode.PlaceBarcode,
            overlayState = OverlayState.Success,
        )

        `assert label`(
            labelStatus = LabelStatus.None,
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.WithInfoButton,
            description = R.string.scan_second_barcode,
        )
    }

    private fun `scan error`() {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )

        `assert label`(
            labelStatus = LabelStatus.Error,
            label = R.string.error,
        )

        `assert description text`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = TestFactory.ERROR_MESSAGE,
            cellState = CellState.None
        )
    }

    private fun `success acceptance`(
        cellTitle: String,
        externalId: ExternalId,
        shouldSkipCellInfo: Boolean = true
    ) {
        `assert scanner fragment`(
            scannerMode = if (shouldSkipCellInfo) ScannerMode.OrderBarcode else ScannerMode.DoNotScan,
            overlayState = OverlayState.Success,
        )

        `assert label`(
            labelStatus = LabelStatus.Success,
            label = R.string.successfully,
        )

        `assert description`(
            descriptionStatus = DescriptionStatus.Neutral,
            description = R.string.order_external_id,
            cellState = CellState.Neutral,
            currentCell = cellTitle,
            externalId = externalId
        )
    }
}
