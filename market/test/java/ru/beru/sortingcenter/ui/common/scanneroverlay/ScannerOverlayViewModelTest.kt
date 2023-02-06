package ru.beru.sortingcenter.ui.common.scanneroverlay

import androidx.compose.ui.graphics.Color
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.R
import ru.yandex.market.sc.core.scanner.presentation.ui.scanneroverlay.ScannerOverlayViewModel
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.ui.theme.Colors
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ScannerOverlayViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private lateinit var viewModel: ScannerOverlayViewModel

    @Before
    fun setUp() {
        viewModel = ScannerOverlayViewModel()
    }

    @After
    fun tearDown() {
        viewModel.setOverlay(OverlayState.None)
        viewModel.setMessage(null)
        assertOverlay(isOverlayVisible = false)
        assertMessage()
    }

    @Test
    fun `default state`() {
        assertOverlay(isOverlayVisible = false)
    }

    @Test
    fun `setOverlay success`() {
        viewModel.setOverlay(OverlayState.Success)
        assertOverlay(overlayColor = Colors.Green)
    }

    @Test
    fun `setOverlay warning`() {
        viewModel.setOverlay(OverlayState.Warning)
        assertOverlay(overlayColor = Colors.Yellow)
    }

    @Test
    fun `setOverlay failure`() {
        viewModel.setOverlay(OverlayState.Failure)
        assertOverlay(overlayColor = Colors.Red)
    }

    @Test
    fun setMessage() {
        val message = R.string.cell_not_active
        viewModel.setMessage(message)
        assertMessage(showMessage = true, message = message)
    }

    private fun assertOverlay(
        isOverlayVisible: Boolean = true,
        overlayColor: Color = Color.Transparent,
    ) {
        viewModel.apply {
            assertEquals(isOverlayVisible, this.isOverlayVisible.getOrAwaitValue())
            assertEquals(overlayColor, this.overlayColor.getOrAwaitValue())
        }
    }

    private fun assertMessage(
        showMessage: Boolean = false,
        message: Int? = null,
    ) {
        viewModel.apply {
            assertEquals(showMessage, this.showMessage.getOrAwaitValue())
            assertEquals(message, this.message.getOrAwaitValue())
        }
    }
}
