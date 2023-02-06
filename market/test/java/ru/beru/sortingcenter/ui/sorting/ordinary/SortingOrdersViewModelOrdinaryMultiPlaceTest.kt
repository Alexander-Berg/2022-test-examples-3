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
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`order sort to cell`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`order sorted to lot`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`ready to scan cell`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`ready to scan place`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`scan cell with error`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`wait for destination`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`wait for parent cell`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`waiting for cell`
import ru.beru.sortingcenter.ui.sorting.AssertScenario.`waiting for place`
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
import ru.yandex.market.sc.core.data.place.PlaceStatus
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSharedPreferencesUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSortableUseCases
import ru.yandex.market.sc.core.network.repository.SharedPreferenceRepository
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.beru.sortingcenter.ui.sorting.orders.models.scanner.ScannerModeImpl as ScannerMode

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class SortingOrdersViewModelOrdinaryMultiPlaceTest {
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

    private val possibleOutgoingDateRouteMock = "2020-12-01"

    @Before
    fun setUp() {
        pathManager = PathManager(
            ordinaryPath = OrdinaryPath(networkSharedPreferencesUseCases, networkOrderUseCases),
            collectPath = CollectPath(networkSharedPreferencesUseCases, networkOrderUseCases),
            returnsPath = ReturnsPath(networkSharedPreferencesUseCases, networkOrderUseCases),
            utilizationPath = UtilizationPath(networkSharedPreferencesUseCases, networkOrderUseCases),
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
    fun `sort multiplace order to lot`() {
        val orderAvailableLots = listOf(TestFactory.createCellLot(externalId = IdManager.getExternalId()))
        val order = TestFactory.createOrderForToday(2)
            .updatePalletizationRequired(true)
            .withAvailableLots(orderAvailableLots)
            .build()
        val places = order.places
        val lot = orderAvailableLots.first()

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)

        val sortable = TestFactory.createSortResponseWithLot(destinationId = lot.externalId)

        runTest {
            `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
            `when`(
                networkSortableUseCases.sort(
                    order.externalId,
                    lot.externalId,
                    places[0].externalId,
                )
            ).thenReturn(sortable)

            viewModel.processScanResult(scanOrderResult)
            `ready to scan place`(order)

            viewModel.forceReset()
            `waiting for place`(order)

            `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(order)

            viewModel.processScanResult(scanPlaceResult)
            `wait for destination`(places[0])

            viewModel.forceReset()
            viewModel.processScanResult(scanLotResult)
            `order sorted to lot`(order.places[0].externalId, sortable.destination.name)
        }
    }

    @Test
    fun `sort multiplace order to lot when already in lot`() = runTest {
        val order = TestFactory.createOrderWithOnePlaceInLot(numberOfPlaces = 3, status = Lot.Status.READY)
        val placeInLot = order.places.first { it.currentLot != null }
        val lot = placeInLot.availableLots.first { lot -> lot.id != placeInLot.currentLot?.id }

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placeInLot.externalId)
        val scanLotResult = ScanResultFactory.getScanResultQR(lot.externalId)
        val sortable = TestFactory.createSortResponseWithLot(destinationId = lot.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                lot.externalId,
                placeInLot.externalId,
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, placeInLot.externalId)).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `wait for destination`(placeInLot)

        viewModel.forceReset()
        viewModel.processScanResult(scanLotResult)
        `order sorted to lot`(placeInLot.externalId, sortable.destination.name)
    }

    @Test
    fun `sort multiplace order to orphan lot`() = runTest {
        val sortable = TestFactory.createSortResponseWithLot(parentRequired = true)
        val sortableAfterLinkToParentCell = TestFactory.createSortResponseWithLot()
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.createOrderForToday(2)
            .updatePalletizationRequired(true)
            .withAvailableLots(listOf(TestFactory.createCellLot()))
            .build()

        val lotId = sortable.destination.externalId
        val places = order.places
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        val scanLotResult = ScanResultFactory.getScanResultQR(lotId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(networkSortableUseCases.sort(order.externalId, lotId, places[0].externalId)).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `wait for destination`(places[0])

        viewModel.processScanResult(scanLotResult)
        `wait for parent cell`(places[0])

        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                placeExternalId = places[0].externalId,
                destinationExternalId = lotId,
                parentDestinationExternalId = ExternalId(bufferCell.id)
            )
        ).thenReturn(sortableAfterLinkToParentCell)

        viewModel.processScanResult(scanCellResult)
        `order sorted to lot`(places[0].externalId, sortableAfterLinkToParentCell.destination.name)
    }

    @Test
    fun `wait for order`() {
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
            isDescriptionVisible = false
        )

        `assert sort destination info`(
            shouldShow = true,
            destinationNumber = R.string.empty,
        )

        `assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    @Test
    fun `sort multiplace order to courier cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val place = order.places.first()

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)
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
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, place.externalId)).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(place)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order by place`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val place = order.places.first()

        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)

        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(place.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(place)

        viewModel.forceReset()
        `waiting for cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order to courier cell without reset`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val place = order.places.first()

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)
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
        `ready to scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, place.externalId)).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(place)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order to courier partial in cell (scan new place)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val place1 = places[1].copy(cell = courierCell)
        val orderInCell = order.copy(places = listOf(places[0], place1))

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)

        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                places[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order to courier partial in cell without reset (scan new place)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val place1 = places[1].copy(cell = courierCell)
        val orderInCell = order.copy(places = listOf(places[0], place1))

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)

        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                places[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order to courier partial in cell (scan sorted place)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val place0 = places[0].copy(cell = courierCell)
        val orderInCell = order.copy(places = listOf(place0, places[1]))

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        viewModel.processScanResult(scanPlaceResult)
        `already scanned order`(courierCell)
    }

    @Test
    fun `sort multiplace order to courier partial in cell without reset (scan sorted place)`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val place0 = places[0].copy(cell = courierCell)
        val orderInCell = order.copy(places = listOf(place0, places[1]))

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.processScanResult(scanPlaceResult)
        `already scanned order`(courierCell)

        viewModel.handleSkip()
    }

    @Test
    fun `sort multiplace order to courier partial in cell (scan sorted place cell != cellTo)`() = runTest {
        val actualCell = TestFactory.getCourierCell("C-1")
        val courierCell = TestFactory.getCourierCell("C-2")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val place0 = places[0].copy(cell = courierCell)
        val place1 = places[1].copy(cell = actualCell)
        val orderInCell = order.copy(places = listOf(place0, place1))

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        viewModel.processScanResult(scanPlaceResult)
        `already scanned order`(courierCell)
    }

    @Test
    fun `sort multiplace order to courier partial in cell without reset (scan sorted place cell != cellTo)`() =
        runTest {
            val actualCell = TestFactory.getCourierCell("C-1")
            val courierCell = TestFactory.getCourierCell("C-2")
            val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
            val places = order.places

            val place0 = places[0].copy(cell = courierCell)
            val place1 = places[1].copy(cell = actualCell)
            val orderInCell = order.copy(places = listOf(place0, place1))

            val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
            val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)

            `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

            viewModel.processScanResult(scanOrderResult)
            `ready to scan place`(order)

            viewModel.processScanResult(scanPlaceResult)
            `already scanned order`(courierCell)

            viewModel.handleSkip()
        }

    @Test
    fun `sort multiplace order to courier already in cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val placesInCell = places.map { it.copy(cell = courierCell) }
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `already scanned order`(courierCell)
    }

    @Test
    fun `sort multiplace order to keep cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order =
            TestFactory.getOrderToKeep(numberOfPlaces = 2, possibleOutgoingRouteDate = possibleOutgoingDateRouteMock)
        val places = order.places

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        val destinationId = ExternalId(bufferCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                places[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order to keep cell without reset`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order =
            TestFactory.getOrderToKeep(numberOfPlaces = 2, possibleOutgoingRouteDate = possibleOutgoingDateRouteMock)
        val places = order.places

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        val destinationId = ExternalId(bufferCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                places[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order to keep partial in cell (scan new place)`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order =
            TestFactory.getOrderToKeep(numberOfPlaces = 2, possibleOutgoingRouteDate = possibleOutgoingDateRouteMock)
        val places = order.places

        val place1 = places[1].copy(cell = bufferCell)
        val orderInCell = order.copy(places = listOf(places[0], place1))

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        val destinationId = ExternalId(bufferCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                places[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order to keep partial in cell without reset (scan new place)`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order =
            TestFactory.getOrderToKeep(numberOfPlaces = 2, possibleOutgoingRouteDate = possibleOutgoingDateRouteMock)
        val places = order.places

        val place1 = places[1].copy(cell = bufferCell)
        val orderInCell = order.copy(places = listOf(places[0], place1))

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        val destinationId = ExternalId(bufferCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                places[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order to keep partial in cell (scan sorted place)`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order =
            TestFactory.getOrderToKeep(numberOfPlaces = 2, possibleOutgoingRouteDate = possibleOutgoingDateRouteMock)
        val places = order.places

        val place0 = places[0].copy(cell = bufferCell)
        val orderInCell = order.copy(places = listOf(place0, places[1]))

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        viewModel.processScanResult(scanPlaceResult)
        `already scanned order`(bufferCell)
    }

    @Test
    fun `sort multiplace order to keep partial in cell without reset (scan sorted place)`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order =
            TestFactory.getOrderToKeep(numberOfPlaces = 2, possibleOutgoingRouteDate = possibleOutgoingDateRouteMock)
        val places = order.places

        val place0 = places[0].copy(cell = bufferCell)
        val orderInCell = order.copy(places = listOf(place0, places[1]))

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.processScanResult(scanPlaceResult)
        `already scanned order`(bufferCell)

        viewModel.handleSkip()
    }

    @Test
    fun `sort multiplace order to keep already in cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.getOrderToKeep(
            numberOfPlaces = 2,
            possibleOutgoingRouteDate = possibleOutgoingDateRouteMock
        )
        val places = order.places

        val placesInCell = places.map { it.copy(cell = bufferCell) }
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `already scanned order`(bufferCell)
    }

    @Test
    fun `sort multiplace order to keep already in several cells`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val anotherBufferCell = TestFactory.getBufferCell("B-2")
        val order = TestFactory.getOrderToKeep(
            numberOfPlaces = 2,
            possibleOutgoingRouteDate = possibleOutgoingDateRouteMock
        )
        val places = order.places

        val placesInCell = listOf(places[0].copy(cell = bufferCell), places[1].copy(cell = anotherBufferCell))
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        viewModel.processScanResult(scanPlaceResult)
        `already scanned order`(bufferCell)
    }

    @Test
    fun `sort multiplace order to drop cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order =
            TestFactory.getOrderDropped(numberOfPlaces = 2, cellTo = droppedCell)
        val places = order.places

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(droppedCell.id)
        val destinationId = ExternalId(droppedCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                places[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order to drop cell without reset`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order =
            TestFactory.getOrderDropped(numberOfPlaces = 2, cellTo = droppedCell)
        val places = order.places

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(droppedCell.id)

        val destinationId = ExternalId(droppedCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                places[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order to drop partial in cell (scan new place)`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDropped(
            numberOfPlaces = 2,
            cellTo = droppedCell,
        )
        val places = order.places

        val place1 = places[1].copy(cell = droppedCell)
        val orderInCell = order.copy(places = listOf(places[0], place1))

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(droppedCell.id)

        val destinationId = ExternalId(droppedCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                places[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order to drop partial in cell without reset (scan new place)`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDropped(
            numberOfPlaces = 2,
            cellTo = droppedCell,
        )
        val places = order.places

        val place1 = places[1].copy(cell = droppedCell)
        val orderInCell = order.copy(places = listOf(places[0], place1))

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(droppedCell.id)

        val destinationId = ExternalId(droppedCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                places[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order to drop partial in cell (scan sorted place)`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDropped(
            numberOfPlaces = 2,
            cellTo = droppedCell
        )
        val places = order.places

        val place0 = places[0].copy(cell = droppedCell)
        val orderInCell = order.copy(places = listOf(place0, places[1]))

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        viewModel.processScanResult(scanPlaceResult)
        `already scanned order`(droppedCell)
    }

    @Test
    fun `sort multiplace order to drop partial in cell without reset (scan sorted place)`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDropped(
            numberOfPlaces = 2,
            cellTo = droppedCell
        )
        val places = order.places

        val place0 = places[0].copy(cell = droppedCell)
        val orderInCell = order.copy(places = listOf(place0, places[1]))

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.processScanResult(scanPlaceResult)
        `already scanned order`(droppedCell)

        viewModel.handleSkip()
    }

    @Test
    fun `sort multiplace order to drop already in cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDropped(
            numberOfPlaces = 2,
            cellTo = droppedCell,
        )
        val places = order.places

        val placesInCell = places.map { it.copy(cell = droppedCell) }
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)

        viewModel.processScanResult(scanOrderResult)
        `already scanned order`(droppedCell)
    }

    @Test
    fun `sort multiplace order intended for drop but in keep cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell()
        val droppedCell = TestFactory.getDroppedCell()
        val order = TestFactory.getOrderDroppedInCell(
            numberOfPlaces = 2,
            cellTo = droppedCell,
            cell = bufferCell,
        )
        val places = order.places

        val placesInCell = places.map { it.copy(cell = bufferCell, status = PlaceStatus.KEEP) }
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(droppedCell.id)

        val destinationId = ExternalId(droppedCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(placesInCell[0])

        viewModel.forceReset()
        `waiting for cell`(placesInCell[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for drop but in keep cell without reset`() = runTest {
        val bufferCell = TestFactory.getBufferCell()
        val droppedCell = TestFactory.getDroppedCell()
        val order = TestFactory.getOrderDroppedInCell(
            numberOfPlaces = 2,
            cellTo = droppedCell,
            cell = bufferCell,
        )
        val places = order.places

        val placesInCell = places.map { it.copy(cell = bufferCell, status = PlaceStatus.KEEP) }
        val orderInCell = order.copy(places = placesInCell)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(droppedCell.id)

        val destinationId = ExternalId(droppedCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for courier cell but in buffer cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val placesInCell = places.map { it.copy(cell = bufferCell) }
        val orderInCell = order.copy(places = places)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)

        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for courier cell but in buffer cell without reset`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val placesInCell = places.map { it.copy(cell = bufferCell) }
        val orderInCell = order.copy(places = places)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)

        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for courier cell but partial in buffer cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val place0 = places[0].copy(cell = bufferCell)
        val place1 = places[1].copy(cell = courierCell)
        val placesInCell = listOf(place0, place1)
        val orderInCell = order.copy(places = places)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)

        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for courier cell but partial in buffer cell without reset`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val place0 = places[0].copy(cell = bufferCell)
        val place1 = places[1].copy(cell = courierCell)
        val placesInCell = listOf(place0, place1)
        val orderInCell = order.copy(places = places)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)

        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for courier cell but in dropped cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val placesInCell = places.map { it.copy(cell = droppedCell) }
        val orderInCell = order.copy(places = places)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)

        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for courier cell but in dropped cell without reset`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val placesInCell = places.map { it.copy(cell = droppedCell) }
        val orderInCell = order.copy(places = places)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)

        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for courier cell but partial in dropped cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell("D-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val place0 = places[0].copy(cell = droppedCell)
        val place1 = places[1].copy(cell = courierCell)
        val placesInCell = listOf(place0, place1)
        val orderInCell = order.copy(places = places)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)

        val destinationId = ExternalId(courierCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for courier cell but partial in dropped cell without reset`() =
        runTest {
            val droppedCell = TestFactory.getDroppedCell("D-1")
            val courierCell = TestFactory.getCourierCell("C-1")
            val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
            val places = order.places

            val place0 = places[0].copy(cell = droppedCell)
            val place1 = places[1].copy(cell = courierCell)
            val placesInCell = listOf(place0, place1)
            val orderInCell = order.copy(places = places)

            val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
            val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
            val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)

            val destinationId = ExternalId(courierCell.id)
            val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

            `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
            `when`(
                networkSortableUseCases.sort(
                    orderInCell.externalId,
                    destinationId,
                    placesInCell[0].externalId
                )
            ).thenReturn(sortable)

            viewModel.processScanResult(scanOrderResult)
            `ready to scan place`(order)

            `when`(
                networkOrderUseCases.acceptOrder(
                    order.externalId,
                    places[0].externalId
                )
            ).thenReturn(orderInCell)

            viewModel.processScanResult(scanPlaceResult)
            `ready to scan cell`(places[0])

            viewModel.processScanResult(scanCellResult)
            `order sort to cell`()
        }

    @Test
    fun `sort multiplace order intended for buffer cell but in dropped cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell()
        val bufferCell = TestFactory.getBufferCell()
        val order = TestFactory.getOrderToKeep(
            numberOfPlaces = 2,
            possibleOutgoingRouteDate = possibleOutgoingDateRouteMock,
        )
        val places = order.places

        val placesInCell = places.map { it.copy(cell = droppedCell) }
        val orderInCell = order.copy(places = places)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        val destinationId = ExternalId(bufferCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for buffer cell but partial in dropped cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell()
        val bufferCell = TestFactory.getBufferCell()
        val order = TestFactory.getOrderToKeep(
            numberOfPlaces = 2,
            possibleOutgoingRouteDate = possibleOutgoingDateRouteMock,
        )
        val places = order.places

        val place0 = places[0].copy(cell = droppedCell)
        val place1 = places[1].copy(cell = bufferCell)
        val placesInCell = listOf(place0, place1)
        val orderInCell = order.copy(places = places)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        val destinationId = ExternalId(bufferCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for dropped cell but in buffer cell`() = runTest {
        val droppedCell = TestFactory.getDroppedCell()
        val bufferCell = TestFactory.getBufferCell()
        val order = TestFactory.getOrderDroppedInCell(numberOfPlaces = 2, cellTo = droppedCell, cell = bufferCell)
        val places = order.places

        val placesInCell = places.map { it.copy(cell = bufferCell) }
        val orderInCell = order.copy(places = places)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(droppedCell.id)

        val destinationId = ExternalId(droppedCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for dropped cell but partial in buffer cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell()
        val droppedCell = TestFactory.getDroppedCell()
        val order = TestFactory.getOrderDroppedInCell(numberOfPlaces = 2, cellTo = droppedCell, cell = bufferCell)
        val places = order.places

        val place0 = places[0].copy(cell = bufferCell)
        val place1 = places[1].copy(cell = droppedCell)
        val placesInCell = listOf(place0, place1)
        val orderInCell = order.copy(places = places)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(droppedCell.id)

        val destinationId = ExternalId(droppedCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for keep cell but in courier cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order =
            TestFactory.getOrderToKeep(numberOfPlaces = 2, possibleOutgoingRouteDate = possibleOutgoingDateRouteMock)
        val places = order.places

        val placesInCell = places.map { it.copy(cell = courierCell) }
        val orderInCell = order.copy(places = places)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        val destinationId = ExternalId(bufferCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for keep cell but in courier cell without reset`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order =
            TestFactory.getOrderToKeep(numberOfPlaces = 2, possibleOutgoingRouteDate = possibleOutgoingDateRouteMock)
        val places = order.places

        val placesInCell = places.map { it.copy(cell = courierCell) }
        val orderInCell = order.copy(places = places)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        val destinationId = ExternalId(bufferCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for keep cell but partial in courier cell`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order =
            TestFactory.getOrderToKeep(numberOfPlaces = 2, possibleOutgoingRouteDate = possibleOutgoingDateRouteMock)
        val places = order.places

        val place0 = places[0].copy(cell = courierCell)
        val place1 = places[1].copy(cell = bufferCell)
        val placesInCell = listOf(place0, place1)
        val orderInCell = order.copy(places = places)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)

        val destinationId = ExternalId(bufferCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order intended for keep cell but partial in courier cell without reset`() = runTest {
        val courierCell = TestFactory.getCourierCell("C-1")
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order =
            TestFactory.getOrderToKeep(numberOfPlaces = 2, possibleOutgoingRouteDate = possibleOutgoingDateRouteMock)
        val places = order.places

        val place0 = places[0].copy(cell = courierCell)
        val place1 = places[1].copy(cell = bufferCell)
        val placesInCell = listOf(place0, place1)
        val orderInCell = order.copy(places = places)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(orderInCell.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(placesInCell[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)
        val destinationId = ExternalId(bufferCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(orderInCell.externalId)).thenReturn(orderInCell)
        `when`(
            networkSortableUseCases.sort(
                orderInCell.externalId,
                destinationId,
                placesInCell[0].externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        `when`(
            networkOrderUseCases.acceptOrder(
                order.externalId,
                places[0].externalId
            )
        ).thenReturn(orderInCell)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `scan multiplace order to return not accepted`() = runTest {
        val droppedCell = TestFactory.getDroppedCell()
        val returnCell = TestFactory.getReturnCell()
        val order = TestFactory.getOrderDropped(numberOfPlaces = 2, cellTo = droppedCell)
        val place = order.places.first()
        val acceptedPlace = place.copy(status = PlaceStatus.SORT_TO_WAREHOUSE, availableCells = listOf(returnCell))
        val acceptedOrder = order.copy(places = order.places.map { acceptedPlace })

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(returnCell.id)

        val destinationId = ExternalId(returnCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                acceptedOrder.externalId,
                destinationId,
                acceptedPlace.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(acceptedOrder)

        viewModel.forceReset()
        `waiting for place`(acceptedOrder)

        `when`(
            networkOrderUseCases.acceptOrder(
                acceptedOrder.externalId,
                acceptedPlace.externalId
            )
        ).thenReturn(acceptedOrder)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(acceptedPlace)

        viewModel.forceReset()
        `waiting for cell`(acceptedPlace)

        viewModel.processScanResult(scanCellResult)
        `order sort to cell`()
    }

    @Test
    fun `sort multiplace order to wrong cell`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)
        val responseErorMessage = "Multiplace order sorted to wrong cell!"
        val response = TestFactory.getResponseError<Int>(
            code = 400,
            errorMessage = responseErorMessage
        )

        val destinationId = ExternalId(bufferCell.id)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                places[0].externalId
            )
        ).thenThrow(HttpException(response))

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        viewModel.forceReset()
        `waiting for place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `scan cell with error`(responseErorMessage)

        viewModel.forceReset()
        `waiting for cell`(places[0])

        viewModel.handleSkip()
    }

    @Test
    fun `sort multiplace order to not active cell`() = runTest {
        val courierCell = TestFactory.getNotActiveCell()
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val place = order.places.first()

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        viewModel.processScanResult(scanOrderResult)

        `ready to scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, place.externalId)).thenReturn(order)
        viewModel.processScanResult(scanPlaceResult)

        `cell not active`()
    }

    @Test
    fun `sort multiplace order by place to not active cell`() = runTest {
        val courierCell = TestFactory.getNotActiveCell()
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val place = order.places.first()

        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkOrderUseCases.acceptOrder(place.externalId)).thenReturn(order)
        viewModel.processScanResult(scanPlaceResult)

        `cell not active`()
    }

    @Test
    fun `sort multiplace order to wrong cell without reset`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val courierCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(numberOfPlaces = 2, cellTo = courierCell)
        val places = order.places

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(places[0].externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)
        val responseErorMessage = "Multiplace order sorted to wrong cell without reset"
        val response = TestFactory.getResponseError<Int>(
            code = 400,
            errorMessage = responseErorMessage
        )

        val destinationId = ExternalId(bufferCell.id)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                places[0].externalId
            )
        ).thenThrow(HttpException(response))

        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, places[0].externalId)).thenReturn(order)

        viewModel.processScanResult(scanPlaceResult)
        `ready to scan cell`(places[0])

        viewModel.processScanResult(scanCellResult)
        `scan cell with error`(responseErorMessage)

        viewModel.handleSkip()
    }

    @Test
    fun `sort middle mile multiplace order`() = runTest {
        val order = TestFactory.createOrderForToday(3)
            .updateIsMiddleMile(true)
            .build()
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkCheckUserUseCases.isAllowToSkipMiddleMileSort()).thenReturn(true)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        assertThat(viewModel.putAsideButton.isVisible.getOrAwaitValue()).isFalse()
        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, place.externalId)).thenReturn(order)
        viewModel.processScanResult(scanPlaceResult)

        assertThat(viewModel.putAsideButton.isVisible.getOrAwaitValue()).isTrue()

        viewModel.handleSkip()
    }

    @Test
    fun `sort middle mile multiplace order without property`() = runTest {
        val order = TestFactory.createOrderForToday(3)
            .updateIsMiddleMile(true)
            .build()
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkCheckUserUseCases.isAllowToSkipMiddleMileSort()).thenReturn(false)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        assertThat(viewModel.putAsideButton.isVisible.getOrAwaitValue()).isFalse()
        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, place.externalId)).thenReturn(order)
        viewModel.processScanResult(scanPlaceResult)

        assertThat(viewModel.putAsideButton.isVisible.getOrAwaitValue()).isFalse()

        viewModel.handleSkip()
    }

    @Test
    fun `sort courier multiplace order`() = runTest {
        val order = TestFactory.createOrderForToday(3).build()
        val place = order.places.first()
        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanPlaceResult = ScanResultFactory.getPlaceDefaultScanResult(place.externalId)

        `when`(networkCheckUserUseCases.isAllowToSkipMiddleMileSort()).thenReturn(true)
        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        assertThat(viewModel.putAsideButton.isVisible.getOrAwaitValue()).isFalse()
        viewModel.processScanResult(scanOrderResult)
        `ready to scan place`(order)

        `when`(networkOrderUseCases.acceptOrder(order.externalId, place.externalId)).thenReturn(order)
        viewModel.processScanResult(scanPlaceResult)

        assertThat(viewModel.putAsideButton.isVisible.getOrAwaitValue()).isFalse()

        viewModel.handleSkip()
    }
}
