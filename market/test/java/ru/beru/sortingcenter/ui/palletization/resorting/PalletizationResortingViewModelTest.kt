package ru.beru.sortingcenter.ui.palletization.resorting

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
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel.DescriptionStatus
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel.LabelStatus
import ru.beru.sortingcenter.ui.palletization.data.cache.PalletizationCellCache
import ru.beru.sortingcenter.ui.palletization.resorting.PalletizationResortingAsserts.`assert description`
import ru.beru.sortingcenter.ui.palletization.resorting.PalletizationResortingAsserts.`assert label`
import ru.beru.sortingcenter.ui.palletization.resorting.PalletizationResortingAsserts.`assert scanner`
import ru.beru.sortingcenter.ui.palletization.resorting.PalletizationResortingAsserts.bind
import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSortableUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.beru.sortingcenter.ui.palletization.resorting.model.scanner.ScannerModeImpl as ScannerMode

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class PalletizationResortingViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var networkSortableUseCases: NetworkSortableUseCases

    @Mock
    private lateinit var cellCache: PalletizationCellCache

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: PalletizationResortingViewModel

    private val returnCell = TestFactory.getReturnCell()
    private var cellWithOrders = TestFactory.mapToCellWithOrders(
        cell = returnCell,
        ordersAssignedToCell = 1,
        ordersInCell = 2,
        placeCount = 3,
        acceptedButNotSortedPlaceCount = 2,
        cellPrepared = false,
    )

    @Before
    fun setUp() {
        `when`(cellCache.value).thenReturn(cellWithOrders)

        viewModel = PalletizationResortingViewModel(
            networkOrderUseCases,
            networkSortableUseCases,
            cellCache,
            appMetrica,
            stringManager,
        )
        bind(viewModel, stringManager)
    }

    @After
    fun tearDown() {
        viewModel.forceReset()
        `waiting for order`()
    }

    @Test
    fun `waiting for order`() {
        `assert scanner`(
            scanMode = ScannerMode.OrderBarcode,
        )

        `assert label`(
            label = R.string.scan_order_to_prepare,
            labelStatus = LabelStatus.Neutral,
        )

        `assert description`(
            description = R.string.empty,
            descriptionStatus = DescriptionStatus.Neutral,
        )
    }

    @Test
    fun `prepare order without cell`() = runTest {
        val order = TestFactory.getOrderWithLotsNotInCell(routeId = cellWithOrders.routeId)
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `use sorting order flow`()
    }

    @Test
    fun `prepare order without lots list`() = runTest {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.getOrderWithLots(cell = bufferCell, routeId = cellWithOrders.routeId)
        val place = order.places.first()
        val scanResult = ScanResultFactory.getOrderDefaultScanResult(order.externalId)

        `when`(networkOrderUseCases.acceptOrder(order.externalId)).thenReturn(order)

        viewModel.processScanResult(scanResult)
        `create lots for cell error`(place.cell!!)
    }

    private fun `use sorting order flow`() {
        `assert scanner`(
            scanMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )

        `assert label`(
            label = R.string.error,
            labelStatus = LabelStatus.Error,
        )

        `assert description`(
            description = R.string.use_sorting_orders_screen,
            descriptionStatus = DescriptionStatus.Neutral,
        )
    }

    private fun `create lots for cell error`(cell: Cell) {
        `assert scanner`(
            scanMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )

        `assert label`(
            label = R.string.error,
            labelStatus = LabelStatus.Error,
        )

        `assert description`(
            description = R.string.order_with_empty_lots_list,
            externalId = cell.number,
            descriptionStatus = DescriptionStatus.Neutral,
        )
    }

}
