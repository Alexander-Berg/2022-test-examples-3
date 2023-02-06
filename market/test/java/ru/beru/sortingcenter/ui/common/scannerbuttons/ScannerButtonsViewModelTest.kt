package ru.beru.sortingcenter.ui.common.scannerbuttons

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.ui.common.scannerbuttons.ScannerButtonsViewModel.CenterButtonState
import ru.beru.sortingcenter.ui.common.scannerbuttons.ScannerButtonsViewModel.UpButtonState
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ScannerButtonsViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private lateinit var viewModel: ScannerButtonsViewModel

    @Before
    fun setUp() {
        viewModel = ScannerButtonsViewModel()
    }

    @Test
    fun `default state`() {
        assertFalse(viewModel.isCenterVisible.getOrAwaitValue())
        assertFalse(viewModel.isUpVisible.getOrAwaitValue())
        assertFalse(viewModel.isUpEnabled.getOrAwaitValue())
    }

    @Test
    fun setCenterButton() {
        viewModel.setCenterButton(CenterButtonState.ShowList)

        assertTrue(viewModel.isCenterVisible.getOrAwaitValue())
    }

    @Test
    fun `setUpButton enabled`() {
        viewModel.setUpButton(UpButtonState.Enable)

        assertTrue(viewModel.isUpVisible.getOrAwaitValue())
        assertTrue(viewModel.isUpEnabled.getOrAwaitValue())
    }

    @Test
    fun `setUpButton disable`() {
        viewModel.setUpButton(UpButtonState.Disable)

        assertTrue(viewModel.isUpVisible.getOrAwaitValue())
        assertFalse(viewModel.isUpEnabled.getOrAwaitValue())
    }
}
