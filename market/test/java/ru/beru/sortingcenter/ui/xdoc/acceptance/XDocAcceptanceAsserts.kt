package ru.beru.sortingcenter.ui.xdoc.acceptance

import org.junit.Assert
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel.DescriptionStatus
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel.LabelStatus
import ru.beru.sortingcenter.ui.xdoc.acceptance.model.scanner.ScannerModeImpl
import ru.yandex.market.sc.core.resources.StringManager
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.test.utils.getOrAwaitValue

object XDocAcceptanceAsserts {
    private lateinit var viewModel: XDocAcceptanceViewModel
    private lateinit var stringManager: StringManager

    fun bind(viewModel: XDocAcceptanceViewModel, stringManager: StringManager) {
        XDocAcceptanceAsserts.viewModel = viewModel
        XDocAcceptanceAsserts.stringManager = stringManager

        // чтобы при снанировании заказа уже быть подписанным на scanner
        viewModel.apply {
            Assert.assertNotNull(scanner.getOrAwaitValue())
        }
    }

    fun `assert scanner`(
        mode: ScannerModeImpl = ScannerModeImpl.DoNotScan,
        overlayState: OverlayState = OverlayState.None,
    ) {
        val scanner = viewModel.scanner.getOrAwaitValue()
        Assert.assertEquals(mode, scanner.mode)
        Assert.assertEquals(overlayState, scanner.overlayState)
    }

    fun `assert label`(
        text: Int,
        externalId: ExternalId? = null,
        status: LabelStatus = LabelStatus.None
    ) {
        val expectedText = if (externalId != null) stringManager.getString(
            text,
            externalId
        ) else stringManager.getString(text)
        `assert label`(expectedText, status)
    }

    fun `assert label`(text: String, status: LabelStatus = LabelStatus.None) {
        val label = viewModel.label.getOrAwaitValue()
        Assert.assertEquals(text, label.text)
        Assert.assertEquals(status, label.status)
    }

    fun `assert description`(
        text: Int,
        externalId: ExternalId? = null,
        status: DescriptionStatus = DescriptionStatus.None,
    ) {
        val expectedText = if (externalId != null) stringManager.getString(
            text,
            externalId
        ) else stringManager.getString(text)
        `assert description`(expectedText, status)
    }

    fun `assert description`(text: String, status: DescriptionStatus = DescriptionStatus.None) {
        val description = viewModel.description.getOrAwaitValue()
        Assert.assertEquals(text, description.text)
        Assert.assertEquals(status, description.status)
    }
}
