package ru.beru.sortingcenter.ui.prepare

import androidx.lifecycle.Observer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyLong
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.beru.sortingcenter.arch.ext.Result
import ru.beru.sortingcenter.ui.prepare.data.cache.PrepareCellCache
import ru.beru.sortingcenter.ui.prepare.data.cache.PrepareOrderListCache
import ru.yandex.market.sc.core.data.order.OrderItem
import ru.yandex.market.sc.core.network.domain.NetworkCellUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class PrepareCellViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkCellUseCases: NetworkCellUseCases

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var cellCache: PrepareCellCache

    @Mock
    private lateinit var orderListCache: PrepareOrderListCache

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: PrepareCellViewModel

    private val courierCell = TestFactory.getCourierCell()
    private var cellWithOrders = TestFactory.mapToCellWithOrders(
        cell = courierCell,
        ordersAssignedToCell = 1,
        placeCount = 3,
        acceptedButNotSortedPlaceCount = 2,
        cellPrepared = false,
    )

    private val routeOrderIds = TestFactory.getRouteOrderIds()

    private val orderItemsObserver = Observer { _: Result<Array<OrderItem>> -> }

    @Before
    fun setUp() {
        runBlocking {
            `when`(cellCache.value).thenReturn(cellWithOrders)
            `when`(networkOrderUseCases.getRouteOrderIdsByCellId(anyLong())).thenReturn(
                routeOrderIds
            )
        }

        viewModel = initViewModel()
        // наблюдаем orderItemsResult, потому что от него зависит, показывается ли кнопка со списком заказов
        viewModel.orderItemsResult.observeForever(orderItemsObserver)
    }

    @After
    fun tearDown() {
        viewModel.orderItemsResult.removeObserver(orderItemsObserver)
    }

    @Test
    fun `cell without orders`() {
        val cell = cellWithOrders.copy(
            ordersAssignedToCell = 0,
            placeCount = 0,
            acceptedButNotSortedPlaceCount = 0
        )

        `when`(cellCache.value).thenReturn(cell)
        viewModel = initViewModel()

        assertCellInfo(
            cellName = cell.number,
        )

        assertPreparingStatus(
            cellStatusText = R.string.cell_has_not_orders_to_dispatch,
            cellStatusTint = R.color.red_light,
            cellStatusVisible = false,
        )

        assertButtons(
            prepareButtonEnabled = false,
            showListButtonEnabled = false,
        )
    }

    @Test
    fun `cell without orders but has assigned orders`() {
        val cell = cellWithOrders.copy(placeCount = 0, acceptedButNotSortedPlaceCount = 0)

        `when`(cellCache.value).thenReturn(cell)
        viewModel = initViewModel()

        assertCellInfo(
            cellName = cell.number,
        )

        assertPreparingStatus(
            cellStatusText = R.string.cell_not_prepared,
            cellStatusTint = R.color.yellow_light,
        )

        assertButtons()
    }

    @Test
    fun `cell with orders not prepared`() {
        assertCellInfo(
            cellName = cellWithOrders.number,
            placesInCell = cellWithOrders.placeCount,
            acceptedButNotSortedPlacesVisible = true,
            acceptedButNotSortedPlaces = cellWithOrders.acceptedButNotSortedPlaceCount,
        )

        assertPreparingStatus(
            cellStatusText = R.string.cell_not_prepared,
            cellStatusTint = R.color.yellow_light,
        )

        assertButtons()
    }

    @Test
    fun `prepared cell`() {
        val cell = cellWithOrders.copy(cellPrepared = true, acceptedButNotSortedPlaceCount = 0)

        `when`(cellCache.value).thenReturn(cell)
        viewModel = initViewModel()

        assertCellInfo(
            cellName = cell.number,
            placesInCell = cell.placeCount,
        )

        assertPreparingStatus(
            cellStatusText = R.string.cell_successfully_prepared,
            cellStatusTint = R.color.green_light,
        )

        assertButtons()
    }

    @Test
    fun `update cell`() = runTest {
        `cell with orders not prepared`()

        val cell = cellWithOrders.copy(cellPrepared = true, acceptedButNotSortedPlaceCount = 0)
        `when`(networkCellUseCases.getCell(cell.id)).thenReturn(cell)

        viewModel.updateCell()

        assertCellInfo(
            cellName = cell.number,
            placesInCell = cell.placeCount,
        )

        assertPreparingStatus(
            cellStatusText = R.string.cell_successfully_prepared,
            cellStatusTint = R.color.green_light,
        )

        assertButtons()
    }

    @Test
    fun `navigate to show list`() {
        viewModel.onShowList()
        assertThat(viewModel.showListEvent.getOrAwaitValue(5).get()).isEqualTo(Unit)
    }

    @Test
    fun `navigate to prepare resorting fragment`() {
        viewModel.onPrepare()

        assertThat(viewModel.prepareEvent.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    private fun assertCellInfo(
        cellName: String? = "без названия",
        placesInCell: Int = 0,
        acceptedButNotSortedPlacesVisible: Boolean = false,
        acceptedButNotSortedPlaces: Int = 0,
    ) {
        viewModel.apply {
            assertEquals(cellName, this.cellName.getOrAwaitValue())
            assertEquals(placesInCell.toString(), this.placesInCell.getOrAwaitValue())
            assertEquals(
                acceptedButNotSortedPlacesVisible,
                this.acceptedButNotSortedPlacesVisible.getOrAwaitValue()
            )
            assertEquals(
                acceptedButNotSortedPlaces.toString(),
                this.acceptedButNotSortedPlaces.getOrAwaitValue()
            )
        }
    }

    private fun assertPreparingStatus(
        cellStatusText: Int = R.string.cell_successfully_prepared,
        cellStatusTint: Int = R.color.green_light,
        cellStatusVisible: Boolean = true,
    ) {
        viewModel.apply {
            assertEquals(cellStatusVisible, this.cellHasAssignedOrders.getOrAwaitValue())
            assertEquals(cellStatusTint, this.cellStatusTint.getOrAwaitValue())
            assertContextStringEquals(cellStatusText, this.cellStatusText.getOrAwaitValue())
        }
    }

    private fun assertButtons(
        prepareButtonEnabled: Boolean = true,
        showListButtonEnabled: Boolean = true,
    ) {
        viewModel.apply {
            assertEquals(prepareButtonEnabled, this.cellHasAssignedOrders.getOrAwaitValue())
            assertEquals(showListButtonEnabled, this.cellHasAssignedOrders.getOrAwaitValue())
        }
    }

    private fun assertContextStringEquals(
        expected: Int,
        actual: Int,
    ) {
        Assert.assertNotNull(stringManager.getString(expected))
        assertEquals(
            stringManager.getString(expected),
            stringManager.getString(actual),
        )
    }

    private fun initViewModel() = PrepareCellViewModel(
        networkCellUseCases,
        networkOrderUseCases,
        orderListCache,
        cellCache,
        appMetrica
    )
}
