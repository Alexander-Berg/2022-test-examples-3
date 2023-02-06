package ru.beru.sortingcenter.ui.palletization.information

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.beru.sortingcenter.ui.palletization.data.cache.PalletizationCellCache
import ru.yandex.market.sc.core.network.domain.NetworkCellUseCases
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class PalletizationInformationViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkCellUseCases: NetworkCellUseCases

    @Mock
    private lateinit var cellCache: PalletizationCellCache

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: PalletizationInformationViewModel

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

        viewModel =
            PalletizationInformationViewModel(
                networkCellUseCases,
                cellCache,
                appMetrica,
                stringManager
            )
    }

    @Test
    fun `cell without orders`() {
        val cell = cellWithOrders.copy(
            ordersAssignedToCell = 0,
            ordersInCell = 0,
            placeCount = 0,
            acceptedButNotSortedPlaceCount = 0
        )
        `when`(cellCache.value).thenReturn(cell)
        viewModel =
            PalletizationInformationViewModel(
                networkCellUseCases,
                cellCache,
                appMetrica,
                stringManager
            )

        assertCellInfo(cellName = cell.number)
        assertButtons(
            prepareButtonEnabled = false,
            showListButtonEnabled = false,
        )
    }

    @Test
    fun `cell with orders not prepared`() {
        assertCellInfo(
            cellName = cellWithOrders.number,
            ordersInCell = cellWithOrders.ordersInCell,
            placesInCell = cellWithOrders.placeCount,
        )
        assertButtons()
    }

    @Test
    fun `prepared cell`() {
        val cell = cellWithOrders.copy(cellPrepared = true, acceptedButNotSortedPlaceCount = 0)
        `when`(cellCache.value).thenReturn(cell)
        viewModel =
            PalletizationInformationViewModel(
                networkCellUseCases,
                cellCache,
                appMetrica,
                stringManager
            )

        assertCellInfo(
            cellName = cell.number,
            ordersInCell = cell.ordersInCell,
            placesInCell = cell.placeCount,
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
            ordersInCell = cell.ordersInCell,
            placesInCell = cell.placeCount,
        )
        assertButtons()
    }

    @Test
    fun `navigate to prepare resorting fragment`() {
        viewModel.onPrepare()

        assertThat(viewModel.prepareEvent.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    private fun assertCellInfo(
        cellName: String? = "без названия",
        ordersInCell: Int = 0,
        placesInCell: Int = 0,
        category: Int = R.string.category_default_orders,
    ) {
        viewModel.apply {
            assertEquals(cellName, this.cellName.getOrAwaitValue())
            assertEquals(ordersInCell.toString(), this.ordersInCell.getOrAwaitValue())
            assertEquals(placesInCell.toString(), this.placesInCell.getOrAwaitValue())
            assertContextStringEquals(category, this.category.getOrAwaitValue())
        }
    }

    private fun assertButtons(
        prepareButtonEnabled: Boolean = true,
        showListButtonEnabled: Boolean = true,
    ) {
        viewModel.apply {
            assertEquals(prepareButtonEnabled, this.shouldShowPrepareButton.getOrAwaitValue())
            assertEquals(showListButtonEnabled, this.shouldShowPrepareButton.getOrAwaitValue())
        }
    }

    private fun assertContextStringEquals(
        expected: Int,
        actual: String,
    ) {
        Assert.assertNotNull(stringManager.getString(expected))
        assertEquals(stringManager.getString(expected), actual)
    }
}
