package ru.beru.sortingcenter.ui.prepare.resorting

import org.junit.Assert
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.ui.common.scannerbuttons.ScannerButtonsViewModel
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel
import ru.beru.sortingcenter.ui.prepare.resorting.model.scanner.ScannerModeImpl
import ru.yandex.market.sc.core.resources.StringManager
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.test.utils.getOrAwaitValue

object PrepareResortingAsserts {
    private lateinit var viewModel: PrepareResortingViewModel
    private lateinit var stringManager: StringManager

    fun bind(viewModel: PrepareResortingViewModel, stringManager: StringManager) {
        this.viewModel = viewModel
        this.stringManager = stringManager

        // чтобы при снанировании заказа уже быть подписанным на scanMode
        viewModel.apply {
            Assert.assertNotNull(scanner.scanMode.getOrAwaitValue())
        }
    }

    fun `assert scanner`(
        scanMode: ScannerModeImpl = ScannerModeImpl.DoNotScan,
        overlayState: OverlayState = OverlayState.None,
        overlayMessage: Int? = null,
    ) {
        viewModel.apply {
            Assert.assertEquals(scanMode, this.scanner.scanMode.getOrAwaitValue())
            Assert.assertEquals(overlayState, this.overlayState.getOrAwaitValue())
            assertContextStringEquals(overlayMessage, this.overlayMessage.getOrAwaitValue())
        }
    }

    fun `assert label`(
        label: Int = R.string.empty,
        externalId: ExternalId? = null,
        labelStatus: ScannerDestinationViewModel.LabelStatus = ScannerDestinationViewModel.LabelStatus.None,
    ) {
        viewModel.apply {
            val expectedLabel = if (externalId != null) {
                stringManager.getString(label, externalId)
            } else {
                stringManager.getString(label)
            }
            Assert.assertEquals(expectedLabel, this.label.getOrAwaitValue())
            Assert.assertEquals(labelStatus, this.labelStatus.getOrAwaitValue())
        }
    }

    fun `assert description`(
        description: Int = R.string.empty,
        externalId: ExternalId? = null,
        descriptionStatus: ScannerDestinationViewModel.DescriptionStatus = ScannerDestinationViewModel.DescriptionStatus.None,
    ) {
        viewModel.apply {
            val expectedDescription = if (externalId != null) {
                stringManager.getString(description, externalId)
            } else {
                stringManager.getString(description)
            }
            Assert.assertEquals(expectedDescription, this.description.getOrAwaitValue())
            Assert.assertEquals(descriptionStatus, this.descriptionStatus.getOrAwaitValue())
        }
    }

    fun `assert buttons`(
        listButtonState: ScannerButtonsViewModel.CenterButtonState = ScannerButtonsViewModel.CenterButtonState.None,
    ) {
        viewModel.apply {
            Assert.assertEquals(listButtonState, this.listButtonState.getOrAwaitValue())
        }
    }

    fun assertContextStringEquals(expected: Int?, actual: Int?) {
        if (expected == null) {
            return
        }

        Assert.assertNotNull(stringManager.getString(expected))
        requireNotNull(actual)
        Assert.assertEquals(
            stringManager.getString(expected),
            stringManager.getString(actual),
        )
    }
}
