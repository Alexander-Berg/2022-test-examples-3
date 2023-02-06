package ru.beru.sortingcenter.ui.inventorying

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.yandex.market.sc.core.data.order.RouteOrderIdMapper
import ru.yandex.market.sc.core.network.domain.NetworkCellUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSharedPreferencesUseCases
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class InventoryingCellViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkCellUseCases: NetworkCellUseCases

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var networkSharedPreferencesUseCases: NetworkSharedPreferencesUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private val stringManger = TestStringManager()
    private lateinit var viewModelInventorying: InventoryingCellViewModel
    private lateinit var sharedViewModelInventorying: InventoryingShareViewModel

    private val courierCell = TestFactory.getCourierCell("C-1")
    private val bufferCell = TestFactory.getBufferCell("B-1")
    private var cellWithOrders = TestFactory.mapToCellWithOrders(
        cell = courierCell,
        ordersAssignedToCell = 3,
        ordersInCell = 2
    )

    @Before
    fun setUp() {
        sharedViewModelInventorying =
            InventoryingShareViewModel(networkCellUseCases, networkOrderUseCases, appMetrica, stringManger)
        viewModelInventorying =
            InventoryingCellViewModel(networkCellUseCases, networkSharedPreferencesUseCases, appMetrica, stringManger)
    }

    @Test
    fun `cell without orders`() {
        cellWithOrders = TestFactory.mapToCellWithOrders(cell = courierCell)
        viewModelInventorying.setCell(cellWithOrders)

        assertCellGroup(
            cellName = cellWithOrders.number,
        )

        assertAssignedGroup(
            isAssignedGroupVisible = false,
        )

        assertButtons()
    }

    @Test
    fun `cell without orders but has assigned orders`() {
        cellWithOrders = TestFactory.mapToCellWithOrders(
            cell = courierCell,
            ordersAssignedToCell = 2,
        )

        viewModelInventorying.setCell(cellWithOrders)

        assertCellGroup(
            cellName = cellWithOrders.number,
        )

        assertAssignedGroup(
            numberAssigned = cellWithOrders.ordersAssignedToCell,
        )

        assertButtons(
            cellHasAssignedOrders = true,
        )
    }

    @Test
    fun `courier cell`() {
        cellWithOrders = TestFactory.mapToCellWithOrders(
            cell = courierCell,
            ordersAssignedToCell = 3,
            ordersInCell = 2
        )

        viewModelInventorying.setCell(cellWithOrders)

        assertCellGroup(
            cellName = cellWithOrders.number,
            numberInCell = cellWithOrders.ordersInCell,
        )

        assertAssignedGroup(
            numberAssigned = cellWithOrders.ordersAssignedToCell,
        )

        assertButtons(
            cellContainOrders = true,
            cellHasAssignedOrders = true,
            canFreeCell = true,
        )
    }

    @Test
    fun `buffer cell`() {
        cellWithOrders = TestFactory.mapToCellWithOrders(
            cell = bufferCell,
            ordersAssignedToCell = 3,
            ordersInCell = 2
        )

        viewModelInventorying.setCell(cellWithOrders)

        assertCellGroup(
            cellName = cellWithOrders.number,
            numberInCell = cellWithOrders.ordersInCell,
        )

        assertAssignedGroup(
            numberAssigned = cellWithOrders.ordersAssignedToCell,
        )

        assertButtons(
            cellContainOrders = true,
            cellHasAssignedOrders = true,
        )
    }

    @Test
    fun `update cell`() = runTest {
        `courier cell`()

        cellWithOrders = TestFactory.mapToCellWithOrders(
            cell = courierCell,
            ordersAssignedToCell = 0,
            ordersInCell = 0,
        )

        sharedViewModelInventorying.updateCell()
        viewModelInventorying.setCell(cellWithOrders)

        assertCellGroup(
            cellName = cellWithOrders.number,
        )

        assertAssignedGroup(
            isAssignedGroupVisible = false,
        )

        assertButtons()
    }

    @Test
    fun `navigate to show list`() {
        viewModelInventorying.setCell(cellWithOrders)
        viewModelInventorying.onShowList()
        assertThat(viewModelInventorying.showList.getOrAwaitValue().get()).isEqualTo(true)
    }

    @Test
    fun `navigate to resort fragment`() {
        viewModelInventorying.onResort()

        assertThat(
            viewModelInventorying.resortEvent.getOrAwaitValue()
                .get()
        ).isEqualTo(ResortType.INVENTORYING)
    }

    @Test
    fun `navigate to free fragment`() {
        viewModelInventorying.onFreeCell()

        assertThat(viewModelInventorying.resortEvent.getOrAwaitValue().get()).isEqualTo(ResortType.FREE_CELL)
    }

    private fun assertCellGroup(
        cellName: String?,
        numberInCell: Int = 0,
    ) {
        viewModelInventorying.apply {
            assertEquals(cellName, this.cellName.getOrAwaitValue())
            assertEquals(numberInCell.toString(), this.numberInCell.getOrAwaitValue())
        }
    }

    private fun assertAssignedGroup(
        isAssignedGroupVisible: Boolean = true,
        numberAssigned: Int = 0,
    ) {
        viewModelInventorying.apply {
            assertEquals(isAssignedGroupVisible, this.isAssignedGroupVisible.getOrAwaitValue())
            assertEquals(numberAssigned.toString(), this.numberAssigned.getOrAwaitValue())
        }
    }

    private fun assertButtons(
        cellContainOrders: Boolean = false,
        cellHasAssignedOrders: Boolean = false,
        canFreeCell: Boolean = false,
    ) {
        viewModelInventorying.apply {
            assertEquals(cellContainOrders, this.cellContainOrders.getOrAwaitValue())
            assertEquals(cellHasAssignedOrders, this.cellHasAssignedOrders.getOrAwaitValue())
            assertEquals(canFreeCell, this.canFreeCell.getOrAwaitValue())
        }
    }
}
