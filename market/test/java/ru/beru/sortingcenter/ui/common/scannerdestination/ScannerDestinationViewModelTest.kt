package ru.beru.sortingcenter.ui.common.scannerdestination

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel.*
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ScannerDestinationViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    private lateinit var viewModel: ScannerDestinationViewModel

    @Before
    fun setUp() {
        viewModel = ScannerDestinationViewModel()

    }

    @Test
    fun setLabelTitle() {
        val label = "Заказ %s"
        viewModel.apply {
            setLabelTitle(label)
            assertEquals(label, this.label.getOrAwaitValue())
        }
    }

    @Test
    fun setLabelState() {
        viewModel.apply {
            setLabelStatus(LabelStatus.Error)
            assertEquals(R.color.red, this.labelColor.getOrAwaitValue())
            assertTrue(this.isLabelVisible.getOrAwaitValue())
        }
    }

    @Test
    fun setDescription() {
        viewModel.apply {
            setDescriptionStatus(DescriptionStatus.Neutral)
            assertTrue(this.isDescriptionVisible.getOrAwaitValue())
            assertFalse(this.isInfoButtonVisible.getOrAwaitValue())
        }
    }

    @Test
    fun `setDescription with info button`() {
        viewModel.apply {
            setDescriptionStatus(DescriptionStatus.WithInfoButton)
            assertTrue(this.isDescriptionVisible.getOrAwaitValue())
            assertTrue(this.isInfoButtonVisible.getOrAwaitValue())
        }
    }

    @Test
    fun `setCellState Courier`() {
        viewModel.apply {
            setCellState(CellState.Courier)
            assertTrue(this.isCellVisible.getOrAwaitValue())
            assertEquals(
                stringManager.getString(R.string.courier),
                stringManager.getString(this.cellLabelId.getOrAwaitValue())
            )
        }
    }

    @Test
    fun `setCellState Neutral`() {
        viewModel.apply {
            setCellState(CellState.Neutral)
            assertTrue(this.isCellVisible.getOrAwaitValue())
            assertEquals(
                stringManager.getString(R.string.cell),
                stringManager.getString(this.cellLabelId.getOrAwaitValue())
            )
        }
    }

    @Test
    fun setCellTitle() {
        val title = "title_cell"
        viewModel.apply {
            setCellTitle(title)
            assertEquals(title, this.cellTitle.getOrAwaitValue())
        }
    }
}
