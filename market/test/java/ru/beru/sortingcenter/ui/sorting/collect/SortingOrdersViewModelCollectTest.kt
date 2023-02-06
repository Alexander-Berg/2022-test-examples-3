package ru.beru.sortingcenter.ui.sorting.collect

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.ui.sorting.AssertScenario
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
import ru.yandex.market.sc.core.data.cell.Cell
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
import ru.beru.sortingcenter.ui.sorting.orders.models.scanner.ScannerModeImpl as ScannerMode

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class SortingOrdersViewModelCollectTest {
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
    private val possibleOutgoingDateRouteMock = "2020-12-01"
    private val selectedRouteIdMock = 1L

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

        viewModel = SortingOrdersViewModel(
            networkOrderUseCases,
            pathManager,
            networkSortableUseCases,
            networkCheckUserUseCases,
            networkSharedPreferencesUseCases,
            sharedPreferenceRepository,
            appMetricaService,
            stringManager
        )
        viewModel.init(Path.Collect(selectedRouteIdMock))

        Asserts.bind(viewModel, stringManager)
    }

    @After
    fun tearDown() {
        viewModel.forceReset()
        `wait for order (collect)`()
    }

    @Test
    fun `wait for order (collect)`() {
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
    fun `scan not existing order (collect)`() = runTest {
        val orderNotFound = TestFactory.getOrderNotFound(notExistedOrderId)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(notExistedOrderId)

        `when`(networkOrderUseCases.acceptOrder(notExistedOrderId)).thenReturn(orderNotFound)

        viewModel.processScanResult(scanResult)
        AssertScenario.`order not found`()
    }

    @Test
    fun `scan order from another route`() = runTest {
        val bufferReturnCell = TestFactory.getBufferCell("R-1", Cell.SubType.BUFFER_RETURNS)
        val notSelectedRouteId = 100L

        val order = TestFactory.createOrder()
            .updateStatus(PlaceStatus.KEEP_TO_WAREHOUSE)
            .updateCellTo(bufferReturnCell)
            .updatePossibleOutgoingRouteDate(possibleOutgoingDateRouteMock)
            .updateRouteTo(notSelectedRouteId)
            .withAvailableCells(listOf(bufferReturnCell))
            .build()

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanOrderResult)

        `assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure
        )
        `assert label`(
            isLabelAvailable = true,
            label = R.string.error,
            labelColor = R.color.red
        )
        `assert description`(
            isDescriptionVisible = true,
            description = R.string.order_from_another_route,
        )
        `assert sort destination info`(
            shouldShow = true,
            destinationNumber = R.string.empty,
        )
        `assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false
        )
    }

    @Test
    fun `sort order to buffer return cell (collect)`() = runTest {
        val bufferReturnCell = TestFactory.getBufferCell("R-1", Cell.SubType.BUFFER_RETURNS)

        val order = TestFactory.createOrder()
            .updateStatus(PlaceStatus.KEEP_TO_WAREHOUSE)
            .updateCellTo(bufferReturnCell)
            .updatePossibleOutgoingRouteDate(possibleOutgoingDateRouteMock)
            .updateRouteTo(selectedRouteIdMock)
            .withAvailableCells(listOf(bufferReturnCell))
            .build()
        val place = order.places.first()

        val destinationId = ExternalId(bufferReturnCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferReturnCell.id)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        AssertScenario.`ready to scan cell`(place)

        viewModel.forceReset()
        AssertScenario.`waiting for cell`(place)

        viewModel.processScanResult(scanCellResult)
        AssertScenario.`order sort to cell`()
    }

    @Test
    fun `sort order to buffer return cell without reset (collect)`() = runTest {
        val bufferReturnCell = TestFactory.getBufferCell("R-1", Cell.SubType.BUFFER_RETURNS)

        val order = TestFactory.createOrder()
            .updateStatus(PlaceStatus.KEEP_TO_WAREHOUSE)
            .updateCellTo(bufferReturnCell)
            .updatePossibleOutgoingRouteDate(possibleOutgoingDateRouteMock)
            .updateRouteTo(selectedRouteIdMock)
            .withAvailableCells(listOf(bufferReturnCell))
            .build()
        val place = order.places.first()

        val destinationId = ExternalId(bufferReturnCell.id)
        val sortable = TestFactory.createSortResponseWithCell(destinationId = destinationId)

        val scanOrderResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferReturnCell.id)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)
        `when`(
            networkSortableUseCases.sort(
                order.externalId,
                destinationId,
                place.externalId
            )
        ).thenReturn(sortable)

        viewModel.processScanResult(scanOrderResult)
        AssertScenario.`ready to scan cell`(place)

        viewModel.forceReset()
        AssertScenario.`waiting for cell`(place)

        viewModel.processScanResult(scanCellResult)
        AssertScenario.`order sort to cell`()
    }
}
