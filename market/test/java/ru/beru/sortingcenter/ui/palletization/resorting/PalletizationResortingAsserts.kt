package ru.beru.sortingcenter.ui.palletization.resorting

import org.junit.Assert
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel
import ru.beru.sortingcenter.ui.palletization.resorting.model.scanner.ScannerModeImpl
import ru.yandex.market.sc.core.resources.StringManager
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.test.utils.getOrAwaitValue

object PalletizationResortingAsserts {
    private lateinit var viewModel: PalletizationResortingViewModel
    private lateinit var stringManager: StringManager

    fun bind(viewModel: PalletizationResortingViewModel, stringManager: StringManager) {
        PalletizationResortingAsserts.viewModel = viewModel
        PalletizationResortingAsserts.stringManager = stringManager

        // чтобы при снанировании заказа уже быть подписанным на scanMode
        viewModel.scanner.getOrAwaitValue()
    }

    fun `assert scanner`(
        scanMode: ScannerModeImpl = ScannerModeImpl.DoNotScan,
        overlayState: OverlayState = OverlayState.None,
    ) {
        val scanner = viewModel.scanner.getOrAwaitValue()
        Assert.assertEquals(scanMode, scanner.mode)
        Assert.assertEquals(overlayState, scanner.overlayState)
    }

    fun `assert label`(
        label: Int = R.string.empty,
        externalId: ExternalId? = null,
        labelStatus: ScannerDestinationViewModel.LabelStatus = ScannerDestinationViewModel.LabelStatus.None,
    ) {
        val expectedLabel = if (externalId != null) {
            stringManager.getString(label, externalId)
        } else {
            stringManager.getString(label)
        }
        val actualLabel = viewModel.label.getOrAwaitValue()
        Assert.assertEquals(expectedLabel, actualLabel.text)
        Assert.assertEquals(labelStatus, actualLabel.status)
    }

    fun `assert description`(
        description: Int = R.string.empty,
        externalId: String? = null,
        descriptionStatus: ScannerDestinationViewModel.DescriptionStatus = ScannerDestinationViewModel.DescriptionStatus.None,
    ) {
        viewModel.apply {
            val expectedDescription = if (externalId != null) {
                stringManager.getString(description, externalId)
            } else stringManager.getString(description)
            `assert description`(expectedDescription, descriptionStatus)
        }
    }

    fun `assert description`(
        description: String,
        descriptionStatus: ScannerDestinationViewModel.DescriptionStatus = ScannerDestinationViewModel.DescriptionStatus.None,
    ) {
        val actualDescription = viewModel.description.getOrAwaitValue()
        Assert.assertEquals(description, actualDescription.text)
        Assert.assertEquals(descriptionStatus, actualDescription.status)
    }
}
