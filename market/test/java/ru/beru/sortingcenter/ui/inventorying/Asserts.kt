package ru.beru.sortingcenter.ui.inventorying

import org.junit.Assert
import ru.beru.sortingcenter.ui.common.scannerbuttons.ScannerButtonsViewModel.CenterButtonState
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel
import ru.beru.sortingcenter.ui.inventorying.resorting.InventoryingResortingViewModel
import ru.beru.sortingcenter.ui.inventorying.resorting.models.scanner.ScannerModeImpl
import ru.yandex.market.sc.core.resources.StringManager
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.test.utils.getOrAwaitValue

object Asserts {
    private lateinit var viewModel: InventoryingResortingViewModel
    private lateinit var stringManager: StringManager

    fun bind(viewModel: InventoryingResortingViewModel, stringManager: StringManager) {
        this.viewModel = viewModel
        this.stringManager = stringManager

        // чтобы при снанировании заказа уже быть подписанным на scanMode
        viewModel.apply {
            Assert.assertNotNull(scanMode.getOrAwaitValue())
        }
    }

    fun `assert scanner fragment`(
        scannerMode: ScannerModeImpl = ScannerModeImpl.DoNotScan,
        overlayState: OverlayState = OverlayState.None,
        overlayMessage: Int? = null,
    ) {
        viewModel.apply {
            Assert.assertEquals(scannerMode, this.scanMode.getOrAwaitValue())
            Assert.assertEquals(overlayState, this.overlayState.getOrAwaitValue())
            if (overlayMessage != null) {
                val overlayMessageValue = this.overlayMessage.getOrAwaitValue()
                requireNotNull(overlayMessageValue)
                requireNotNull(stringManager.getString(overlayMessage))

                Assert.assertEquals(
                    stringManager.getString(overlayMessage),
                    stringManager.getString(overlayMessageValue),
                )
            } else {
                Assert.assertNull(this.overlayMessage.getOrAwaitValue())
            }
        }
    }

    fun `assert button fragment`(
        centerButtonState: CenterButtonState = CenterButtonState.None,
    ) {
        viewModel.apply {
            Assert.assertEquals(centerButtonState, this.centerButtonState.getOrAwaitValue())
        }
    }

    fun `assert label`(
        labelStatus: ScannerDestinationViewModel.LabelStatus,
        label: Int? = null,
        externalId: ExternalId? = null,
    ) {
        viewModel.apply {
            Assert.assertEquals(
                labelStatus, this.labelStatus.getOrAwaitValue()
            )

            if (labelStatus != ScannerDestinationViewModel.LabelStatus.None) {
                requireNotNull(label)

                val expectedLabel = if (externalId != null) {
                    stringManager.getString(label, externalId)
                } else {
                    stringManager.getString(label)
                }

                Assert.assertEquals(
                    expectedLabel,
                    this.label.getOrAwaitValue()
                )
            }
        }
    }

    fun `assert description text`(
        descriptionStatus: ScannerDestinationViewModel.DescriptionStatus = ScannerDestinationViewModel.DescriptionStatus.None,
        description: String,
        cellState: ScannerDestinationViewModel.CellState = ScannerDestinationViewModel.CellState.None,
        cellTitle: String? = null,
    ) {
        viewModel.apply {
            Assert.assertEquals(descriptionStatus, this.descriptionStatus.getOrAwaitValue())
            Assert.assertEquals(cellState, this.cellState.getOrAwaitValue())

            if (descriptionStatus != ScannerDestinationViewModel.DescriptionStatus.None) {
                Assert.assertEquals(
                    description,
                    this.description.getOrAwaitValue()
                )
            }

            if (cellState != ScannerDestinationViewModel.CellState.None) {
                requireNotNull(cellTitle)

                Assert.assertEquals(cellTitle, this.cellTitle.getOrAwaitValue())
            }
        }
    }

    fun `assert description`(
        descriptionStatus: ScannerDestinationViewModel.DescriptionStatus = ScannerDestinationViewModel.DescriptionStatus.None,
        description: Int? = null,
        cellState: ScannerDestinationViewModel.CellState = ScannerDestinationViewModel.CellState.None,
        cellTitle: String? = null,
    ) {
        viewModel.apply {
            Assert.assertEquals(descriptionStatus, this.descriptionStatus.getOrAwaitValue())
            Assert.assertEquals(cellState, this.cellState.getOrAwaitValue())

            if (descriptionStatus != ScannerDestinationViewModel.DescriptionStatus.None) {
                requireNotNull(description)

                Assert.assertNotNull(stringManager.getString(description))
                Assert.assertEquals(
                    stringManager.getString(description),
                    this.description.getOrAwaitValue()
                )
            }

            if (cellState != ScannerDestinationViewModel.CellState.None) {
                Assert.assertEquals(cellTitle, this.cellTitle.getOrAwaitValue())
            }
        }
    }
}
