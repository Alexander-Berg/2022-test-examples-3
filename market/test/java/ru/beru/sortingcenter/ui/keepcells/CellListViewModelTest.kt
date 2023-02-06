package ru.beru.sortingcenter.ui.keepcells

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import ru.yandex.market.sc.core.data.cell.CellToSort
import ru.yandex.market.sc.core.data.cell.CellsSortData
import ru.yandex.market.sc.core.network.domain.NetworkCellUseCases
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class CellListViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkCellUseCases: NetworkCellUseCases

    private lateinit var viewModel: CellListViewModel

    @Test
    fun `get sort cell`() = runTest {
        val cellToSort = TestFactory.getCellToSort("C-1", 3, 2)
        `when`(networkCellUseCases.getCellsSort()).thenReturn(
            CellsSortData(
                cells = listOf(cellToSort),
                totalCells = 1,
                totalOrders = cellToSort.ordersCount,
                totalOrdersToSort = cellToSort.ordersToSortCount,
            )
        )

        viewModel = CellListViewModel(networkCellUseCases)

        viewModel.apply {
            assertEquals(listOf(cellToSort), this.cellList.getOrAwaitValue())
            assertEquals(1, this.cellCount.getOrAwaitValue())
        }
    }

    @Test
    fun `get sort cell with error`() = runTest {
        val response = TestFactory.getResponseError<Int>(code = 401)
        `when`(networkCellUseCases.getCellsSort()).thenThrow(HttpException(response))

        viewModel = CellListViewModel(networkCellUseCases)

        viewModel.apply {
            assertEquals(emptyList<CellToSort>(), this.cellList.getOrAwaitValue())
            assertEquals(0, this.cellCount.getOrAwaitValue())
        }
    }
}
