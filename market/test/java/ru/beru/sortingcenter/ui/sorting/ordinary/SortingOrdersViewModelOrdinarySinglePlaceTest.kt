package ru.beru.sortingcenter.ui.sorting.ordinary

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`already scanned order`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`cell not active`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`order not found`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`order sort to cell`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`order sorted to lot`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`ready to scan cell`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`scan cell with error`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`scan destination with error`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`scan order with error`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`scan with wrong format`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`wait for destination`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`wait for parent cell`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`waiting for cell`
import ru.beru.sortingcenter.ui.sorting.Asserts
import ru.beru.sortingcenter.ui.sorting.Asserts.`assert description`
import ru.beru.sortingcenter.ui.sorting.Asserts.`assert expected date`
import ru.beru.sortingcenter.ui.sorting.Asserts.`assert label`
import ru.beru.sortingcenter.ui.sorting.Asserts.`assert scanner fragment`
import ru.beru.sortingcenter.ui.sorting.Asserts.`assert sort destination info`
import ru.beru.sortingcenter.ui.sorting.orders.PathManager
import ru.beru.sortingcenter.ui.sorting.orders.SortingOrdersViewModel
import ru.beru.sortingcenter.ui.sorting.orders.enums.Path
import ru.beru.sortingcenter.ui.sorting.orders.paths.CollectPath
import ru.beru.sortingcenter.ui.sorting.orders.paths.OrdinaryPath
import ru.beru.sortingcenter.ui.sorting.orders.paths.ReturnsPath
import ru.beru.sortingcenter.ui.sorting.orders.paths.UtilizationPath
import ru.beru.sortingcenter.ui.sorting.orders.services.AppMetricaService
import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.data.sortable.SortResponse
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSharedPreferencesUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSortableUseCases
import ru.yandex.market.sc.core.network.repository.SharedPreferenceRepository
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.beru.sortingcenter.ui.sorting.orders.models.scanner.ScannerModeImpl as ScannerMode

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class SortingOrdersViewModelOrdinarySinglePlaceTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var pathManager: PathManager

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var networkSortableUseCases: NetworkSortableUseCases

    @Mock
    private lateinit var networkCheckUserUseCases: NetworkCheckUserUseCases

    @Mock
    private lateinit var networkSharedPreferencesUseCases: NetworkSharedPreferencesUseCases

    @Mock
    private lateinit var sharedPreferenceRepository: SharedPreferenceRepository

    @Mock
    private lateinit var appMetricaService: AppMetricaService

    private val stringManager = TestStringManager()
    private lateinit var viewModel: SortingOrdersViewModel

    private val notExistedOrderId = IdManager.getExternalId(-1)
    private val notExistedCellId = 12345L
    private val notExistedLotId = IdManager.getExternalId(-2)
    private val possibleOutgoingDateRouteMock = "2020-12-01"

    @Before
    fun setUp() {
        pathManager = PathManager(
            ordinaryPath = OrdinaryPath(networkSharedPreferencesUseCases, networkOrderUseCases),
            collectPath = CollectPath(networkSharedPreferencesUseCases, networkOrderUseCases),
            returnsPath = ReturnsPath(networkSharedPreferencesUseCases, networkOrderUseCases),
            utilizationPath = UtilizationPath(
                networkSharedPreferencesUseCases,
                networkOrderUseCases
            ),
        )
        viewModel =
            SortingOrdersViewModel(
                networkOrderUseCases,
                pathManager,
                networkSortableUseCases,
                networkCheckUserUseCases,
                networkSharedPreferencesUseCases,
                sharedPreferenceRepository,
                appMetricaService,
                stringManager
            )
        viewModel.init(Path.Ordinary)
        Asserts.bind(viewModel, stringManager)
    }

    @After
    fun tearDown() {
        viewModel.forceReset()
        `wait for order`()
    }

    @Test
    fun `sort order to lot`() = runTest {
        val order = TestFactory.createOrderForTodayWithLotsAvailable()
        val place = order.places.first()
        val lot = place.availableLots.first()

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)
        val sortable = TestFactory.createSortResponseWithLot(destinationId = lot.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                lot.externalId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `wait for destination`(place)

        viewModel.forceReset()
        `wait for destination`(place, true)

        viewModel.processScanResult(scanLotResult)
        `order sorted to lot`(place.externalId, sortable.destination.name)
    }

    @Test
    fun `sort order to lot when already in lot`() = runTest {
        val order = TestFactory.createOrderInLot(status = Lot.Status.READY)
        val place = order.places.first()
        val lot = place.availableLots.find { lot -> lot.id != place.currentLot?.id }!!

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)
        val sortable = TestFactory.createSortResponseWithLot(destinationId = lot.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                lot.externalId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `wait for destination`(place)

        viewModel.forceReset()
        `wait for destination`(place, true)

        viewModel.processScanResult(scanLotResult)
        `order sorted to lot`(place.externalId, sortable.destination.name)
    }

    @Test
    fun `sort order to lot(scan wrong lot) with reset`() = runTest {
        val order = TestFactory.createOrderForTodayWithLotsAvailable()
        val place = order.places.first()
        val lot = place.availableLots.first()

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)
        val scanLotResultNotExist = ScanResultFactory.getScanResultQR(notExistedLotId)
        val sortable = TestFactory.createSortResponseWithLot(destinationId = lot.externalId)
        val errorMessage = "Ничего не найдено"
        val response = TestFactory.getResponseError<Int>(
            code = 404,
            errorMessage = errorMessage
        )

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                lot.externalId,
                place.externalId
            )
        ).thenReturn(sortable)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                notExistedLotId,
                place.externalId
            )
        ).thenThrow(HttpException(response))

        viewModel.processScanResult(scanOrderResult)
        `wait for destination`(place)

        viewModel.forceReset()
        `wait for destination`(place, true)

        viewModel.processScanResult(scanLotResultNotExist)
        `scan destination with error`(errorMessage, type = SortResponse.DestinationType.LOT)
        viewModel.forceReset()
        `wait for destination`(place, withReset = true)

        viewModel.processScanResult(scanLotResult)
        `order sorted to lot`(place.externalId, sortable.destination.name)
    }

    @Test
    fun `sort order to lot(scan wrong lot) without reset`() = runTest {
        val order = TestFactory.createOrderForTodayWithLotsAvailable()
        val place = order.places.first()
        val lot = place.availableLots.first()

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)
        val scanLotResultNotExist = ScanResultFactory.getScanResultQR(notExistedLotId)
        val sortable = TestFactory.createSortResponseWithLot(destinationId = lot.externalId)
        val errorMessage = "Ничего не найдено"
        val response = TestFactory.getResponseError<Int>(
            code = 404,
            errorMessage = errorMessage
        )

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                lot.externalId,
                place.externalId
            )
        ).thenReturn(sortable)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                notExistedLotId,
                place.externalId
            )
        ).thenThrow(HttpException(response))

        viewModel.processScanResult(scanOrderResult)
        `wait for destination`(place)

        viewModel.forceReset()
        `wait for destination`(place, true)

        viewModel.processScanResult(scanLotResultNotExist)
        `scan destination with error`(errorMessage, type = SortResponse.DestinationType.LOT)

        viewModel.processScanResult(scanLotResult)
        `order sorted to lot`(place.externalId, sortable.destination.name)
    }

    @Test
    fun `sort order to orphan lot`() = runTest {
        val sortable = TestFactory.createSortResponseWithLot(parentRequired = true)
        val sortableAfterLinkToParentCell = TestFactory.createSortResponseWithLot()
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.createOrderForTodayWithLotsAvailable()
        val place = order.places.first()
        val lotId = sortable.destination.externalId
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanLotResult = ScanResultFactory.getScanResultQR(lotId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(networkSortableUseCases.sort(order.externalId, lotId, place.externalId)).thenReturn(
            sortable
        )

        viewModel.processScanResult(scanOrderResult)
        `wait for destination`(place)

        viewModel.forceReset()
        `wait for destination`(place, true)

        viewModel.processScanResult(scanLotResult)
        `wait for parent cell`(place)

        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                placeExternalId = place.externalId,
                destinationExternalId = lotId,
                parentDestinationExternalId = ExternalId(bufferCell.id),
            )
        ).thenReturn(sortableAfterLinkToParentCell)

        viewModel.processScanResult(scanCellResult)
        `order sorted to lot`(place.externalId, sortableAfterLinkToParentCell.destination.name)
    }

    @Test
    fun `try to sort order to lot with flag off`() = runTest {
        val sortable = TestFactory.createSortResponseWithCell()

        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.createOrderForToday(1)
            .updatePalletizationRequired(false)
            .withAvailableLots(listOf(TestFactory.createCellLot()))
            .build()
        val place = order.places.first()

        val lotId = sortable.destination.externalId
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanLotResult = ScanResultFactory.getScanResultQR(lotId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        val responseMessage = "Ничего не найдено"
        val response = TestFactory.getResponseError<Int>(
            code = 404,
            errorMessage = responseMessage
        )

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(networkSortableUseCases.sort(order.externalId, lotId, place.externalId)).thenThrow(
            HttpException(response)
        )

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanLotResult)
        `scan destination with error`(responseMessage)

        viewModel.forceReset()
        `waiting for cell`(place)

        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(bufferCell.id),
                place.externalId
            )
        ).thenReturn(sortable)
        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to orphan lot with reset`() = runTest {
        val sortable = TestFactory.createSortResponseWithLot(parentRequired = true)
        val sortableAfterLinkToParentCell = TestFactory.createSortResponseWithLot()
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.createOrderForTodayWithLotsAvailable()
        val place = order.places.first()
        val lotId = sortable.destination.externalId
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanLotResult = ScanResultFactory.getScanResultQR(lotId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(networkSortableUseCases.sort(order.externalId, lotId, place.externalId)).thenReturn(
            sortable
        )

        viewModel.processScanResult(scanOrderResult)
        `wait for destination`(place)

        viewModel.forceReset()
        `wait for destination`(place, true)

        viewModel.processScanResult(scanLotResult)
        viewModel.forceReset()
        `wait for parent cell`(place, withReset = true)

        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationExternalId = lotId,
                placeExternalId = place.externalId,
                parentDestinationExternalId = ExternalId(bufferCell.id)
            )
        ).thenReturn(sortableAfterLinkToParentCell)

        viewModel.processScanResult(scanCellResult)
        `order sorted to lot`(place.externalId, sortableAfterLinkToParentCell.destination.name)
    }

    @Test
    fun `sort order to orphan lot with scan wrong parent cell(with reset)`() = runTest {
        val sortable = TestFactory.createSortResponseWithLot(parentRequired = true)
        val sortableAfterLinkToParentCell = TestFactory.createSortResponseWithLot()
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.createOrderForTodayWithLotsAvailable()
        val place = order.places.first()
        val lotId = sortable.destination.externalId
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanLotResult = ScanResultFactory.getScanResultQR(lotId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)
        val responseMessage = "Ничего не найдено"
        val scanCellResultNotExisted = ScanResultFactory.getScanResultQR(notExistedCellId)
        val response = TestFactory.getResponseError<Int>(
            code = 404,
            errorMessage = responseMessage
        )

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `wait for destination`(place)

        viewModel.forceReset()
        `wait for destination`(place, true)

        `when`(networkSortableUseCases.sort(order.externalId, lotId, place.externalId)).thenReturn(
            sortable
        )
        viewModel.processScanResult(scanLotResult)
        `wait for parent cell`(place)

        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationExternalId = lotId,
                placeExternalId = place.externalId,
                parentDestinationExternalId = ExternalId(notExistedCellId),
            )
        ).thenThrow(HttpException(response))

        viewModel.processScanResult(scanCellResultNotExisted)
        `scan destination with error`(responseMessage)
        viewModel.forceReset()
        `wait for parent cell`(place, withReset = true)

        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationExternalId = lotId,
                placeExternalId = place.externalId,
                parentDestinationExternalId = ExternalId(bufferCell.id),
            )
        ).thenReturn(sortableAfterLinkToParentCell)
        viewModel.processScanResult(scanCellResult)
        `order sorted to lot`(place.externalId, sortableAfterLinkToParentCell.destination.name)
    }

    @Test
    fun `sort order to orphan lot with scan wrong parent cell(without reset)`() = runTest {
        val sortable = TestFactory.createSortResponseWithLot(parentRequired = true)
        val sortableAfterLinkToParentCell = TestFactory.createSortResponseWithLot()
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.createOrderForTodayWithLotsAvailable()
        val place = order.places.first()
        val lotId = sortable.destination.externalId
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanLotResult = ScanResultFactory.getScanResultQR(lotId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)
        val responseMessage = "Ничего не найдено"
        val scanCellResultNotExisted = ScanResultFactory.getScanResultQR(notExistedCellId)
        val response = TestFactory.getResponseError<Int>(
            code = 404,
            errorMessage = responseMessage
        )

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `wait for destination`(place)

        viewModel.forceReset()
        `wait for destination`(place, true)

        `when`(networkSortableUseCases.sort(order.externalId, lotId, place.externalId)).thenReturn(
            sortable
        )
        viewModel.processScanResult(scanLotResult)
        `wait for parent cell`(place)

        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationExternalId = lotId,
                placeExternalId = place.externalId,
                parentDestinationExternalId = ExternalId(notExistedCellId)
            )
        ).thenThrow(HttpException(response))

        viewModel.processScanResult(scanCellResultNotExisted)
        `scan destination with error`(responseMessage)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationExternalId = lotId,
                placeExternalId = place.externalId,
                parentDestinationExternalId = ExternalId(bufferCell.id)
            )
        ).thenReturn(sortableAfterLinkToParentCell)
        viewModel.processScanResult(scanCellResult)
        `order sorted to lot`(place.externalId, sortableAfterLinkToParentCell.destination.name)
    }

    @Test
    fun `wait for order`() = runTest {
        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.None,
        )

        `assert label`(
            isLabelAvailable = true,
            label = R.string.empty,
            labelColor = R.color.black,
        )

        `assert description`(
            isDescriptionVisible = false,
        )

        `assert sort destination info`(
            shouldShow = true,
            destinationNumber = stringManager.getString(R.string.empty),
        )

        `assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    @Test
    fun `scan not exist order`() = runTest {
        val orderNotFound = TestFactory.getOrderNotFound(notExistedOrderId)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(notExistedOrderId)

        `when`(networkOrderUseCases.acceptOrder(notExistedOrderId)).thenReturn(orderNotFound)

        viewModel.processScanResult(scanResult)
        `order not found`()
    }

    @Test
    fun `scan order already returned`() = runTest {
        val order = TestFactory.getOrderReturnAlreadyDispatched(notExistedOrderId)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(notExistedOrderId)

        `when`(networkOrderUseCases.acceptOrder(notExistedOrderId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `order not found`()
    }

    @Test
    fun `sort order to courier cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)
        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to courier cell with isLotsAvailable true `() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell, isLotsAvailable = true)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)
        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `wait for destination`(place)

        viewModel.forceReset()
        `wait for destination`(place, true)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to courier cell without reset`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)
        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to keep cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order =
            TestFactory.getOrderToKeep(possibleOutgoingRouteDate = possibleOutgoingDateRouteMock)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)
        val destinationId = ExternalId(bufferCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to keep cell without reset`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order =
            TestFactory.getOrderToKeep(possibleOutgoingRouteDate = possibleOutgoingDateRouteMock)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)
        val destinationId = ExternalId(bufferCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to dropped cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDropped(
            cellTo = droppedCell,
            possibleOutgoingRouteDate = possibleOutgoingDateRouteMock,
        )
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(droppedCell.id)
        val destinationId = ExternalId(droppedCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to dropped cell without reset`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDropped(
            cellTo = droppedCell,
            possibleOutgoingRouteDate = possibleOutgoingDateRouteMock,
        )
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(droppedCell.id)
        val destinationId = ExternalId(droppedCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to courier cell already in cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order =
            TestFactory.getOrderToCourier(cellTo = courierCell, cell = courierCell)
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `already scanned order`(courierCell)
    }

    @Test
    fun `sort order to keep cell already in cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.getOrderToKeepInCell(
            cell = bufferCell,
            possibleOutgoingRouteDate = possibleOutgoingDateRouteMock,
        )
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `already scanned order`(bufferCell)
    }

    @Test
    fun `sort order when lot sort available and order already in cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.createSortedOrderWithLotSortAvailable(cell = bufferCell)
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `already scanned order`(bufferCell)
    }

    @Test
    fun `sort order to dropped cell already in cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order =
            TestFactory.getOrderDroppedInCell(cellTo = droppedCell, cell = droppedCell)
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `already scanned order`(droppedCell)
    }

    @Test
    fun `sort order to courier cell from keep cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = bufferCell)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)
        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to courier cell from keep cell without reset`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = bufferCell)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)
        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(courierCell.id),
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to keep cell from courier cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order =
            TestFactory.getOrderToKeepInCell(
                cell = courierCell,
                possibleOutgoingRouteDate = possibleOutgoingDateRouteMock
            )
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)
        val destinationId = ExternalId(bufferCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to keep cell from courier cell without reset`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order =
            TestFactory.getOrderToKeepInCell(
                cell = courierCell,
                possibleOutgoingRouteDate = possibleOutgoingDateRouteMock
            )
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)
        val destinationId = ExternalId(bufferCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to courier cell from dropped cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = droppedCell)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)
        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to buffer cell from dropped cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell()
        val bufferCell = TestFactory.getBufferCell()
        val order = TestFactory.getOrderToKeepInCell(
            cell = droppedCell,
            possibleOutgoingRouteDate = possibleOutgoingDateRouteMock,
        )
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)
        val destinationId = ExternalId(bufferCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to dropped cell from buffer cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell()
        val bufferCell = TestFactory.getBufferCell()
        val order = TestFactory.getOrderDroppedInCell(
            cellTo = droppedCell,
            cell = bufferCell,
        )
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(droppedCell.id)
        val destinationId = ExternalId(droppedCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to courier cell from dropped cell without reset`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = droppedCell)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)
        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort order to wrong cell`() = runTest {
        val bufferCell = TestFactory.getCourierCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)
        val responseErrorMessage = "Order sorted to wrong cell!"
        val response = TestFactory.getResponseError<Int>(
            code = 400,
            errorMessage = responseErrorMessage
        )

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(bufferCell.id),
                place.externalId
            )
        ).thenThrow(HttpException(response))

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanCellResult)
        `scan cell with error`(responseErrorMessage)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.handleSkip()
    }

    @Test
    fun `sort order to wrong cell without reset`() = runTest {
        val bufferCell = TestFactory.getCourierCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)
        val responseErrorMessage = "Order sorted to wrong cell without reset!"
        val response = TestFactory.getResponseError<Int>(
            code = 400,
            errorMessage = responseErrorMessage
        )

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(bufferCell.id),
                place.externalId
            )
        ).thenThrow(HttpException(response))

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.processScanResult(scanCellResult)
        `scan cell with error`(responseErrorMessage)

        viewModel.handleSkip()
    }

    @Test
    fun `sort order to not existed cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(notExistedCellId)
        val response = TestFactory.getResponseError<Int>(
            code = 404,
            errorMessage = "Ничего не найдено"
        )

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(notExistedCellId),
                place.externalId
            )
        ).thenThrow(HttpException(response))

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanCellResult)
        `scan cell with error`("Ничего не найдено")

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.handleSkip()
    }

    @Test
    fun `sort order to not existed cell without reset`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell)
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(notExistedCellId)
        val response = TestFactory.getResponseError<Int>(
            code = 404,
            errorMessage = "Ничего не найдено"
        )

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                ExternalId(notExistedCellId),
                place.externalId
            )
        ).thenThrow(HttpException(response))

        viewModel.processScanResult(scanOrderResult)
        `ready to scan cell`(place)

        viewModel.processScanResult(scanCellResult)
        `scan cell with error`("Ничего не найдено")

        viewModel.handleSkip()
    }

    @Test
    fun `sort order to not active cell`() = runTest {
        val courierCell = TestFactory.getNotActiveCell()
        val order = TestFactory.getOrderToCourier(cellTo = courierCell)
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)
        `cell not active`()
    }

    @Test
    fun `sort order with error`() = runTest {
        val response = TestFactory.getResponseError<Int>(code = 400)
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(notExistedOrderId)

        `when`(networkOrderUseCases.acceptOrder(notExistedOrderId)).thenThrow(HttpException(response))

        viewModel.processScanResult(scanOrderResult)
        `scan order with error`()
    }

    @Test
    fun `scan wrong format and finish sort`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell)
        val place = order.places.first()
        val scanOrderResultBySupportedFormat = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanOrderResultByUnsupportedFormat = ScanResultFactory.getUnsupportedScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)
        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId,
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResultBySupportedFormat)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanOrderResultByUnsupportedFormat)
        `scan with wrong format`(ScannerMode.CellQRCode)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `scan wrong format without reset and finish sort`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell)
        val place = order.places.first()
        val scanOrderResultBySupportedFormat = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanOrderResultByUnsupportedFormat = ScanResultFactory.getUnsupportedScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)
        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResultBySupportedFormat)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanOrderResultByUnsupportedFormat)
        `scan with wrong format`(ScannerMode.CellQRCode)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort middle mile order`() = runTest {
        val order = TestFactory.createOrderForToday(1)
            .updateIsMiddleMile(true)
            .build()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkCheckUserUseCases.isAllowToSkipMiddleMileSort()).thenReturn(true)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        assertThat(viewModel.putAsideButton.isVisible.getOrAwaitValue()).isFalse()
        viewModel.processScanResult(scanOrderResult)
        assertThat(viewModel.putAsideButton.isVisible.getOrAwaitValue()).isTrue()

        viewModel.handleSkip()
    }

    @Test
    fun `sort middle mile order without property`() = runTest {
        val order = TestFactory.createOrderForToday(1)
            .updateIsMiddleMile(true)
            .build()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkCheckUserUseCases.isAllowToSkipMiddleMileSort()).thenReturn(false)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        assertThat(viewModel.putAsideButton.isVisible.getOrAwaitValue()).isFalse()
        viewModel.processScanResult(scanOrderResult)
        assertThat(viewModel.putAsideButton.isVisible.getOrAwaitValue()).isFalse()

        viewModel.handleSkip()
    }

    @Test
    fun `sort courier order`() = runTest {
        val order = TestFactory.createOrderForToday(1).build()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkCheckUserUseCases.isAllowToSkipMiddleMileSort()).thenReturn(true)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        assertThat(viewModel.putAsideButton.isVisible.getOrAwaitValue()).isFalse()
        viewModel.processScanResult(scanOrderResult)
        assertThat(viewModel.putAsideButton.isVisible.getOrAwaitValue()).isFalse()

        viewModel.handleSkip()
    }
}
