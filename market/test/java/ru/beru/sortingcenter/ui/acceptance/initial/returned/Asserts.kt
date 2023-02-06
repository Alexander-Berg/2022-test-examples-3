package ru.beru.sortingcenter.ui.acceptance.initial.returned

import org.junit.Assert
import ru.beru.sortingcenter.ui.acceptance.initial.returned.model.scanner.ScannerModeImpl
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel.*
import ru.yandex.market.sc.core.resources.StringManager
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.test.utils.getOrAwaitValue

object Asserts {
    private lateinit var viewModel: InitialReturnAcceptViewModel
    private lateinit var stringManager: StringManager

    fun bind(viewModel: InitialReturnAcceptViewModel, stringManager: StringManager) {
        this.viewModel = viewModel
        this.stringManager = stringManager

        viewModel.apply {
            Assert.assertNotNull(scanMode.getOrAwaitValue())
        }
    }

    fun `assert scanner fragment`(
        scannerMode: ScannerModeImpl = ScannerModeImpl.DoNotScan,
        overlayState: OverlayState = OverlayState.None,
    ) {
        viewModel.apply {
            Assert.assertEquals(scannerMode, this.scanMode.getOrAwaitValue())
            Assert.assertEquals(overlayState, this.overlayState.getOrAwaitValue())
        }
    }

    fun `assert label`(
        labelStatus: LabelStatus,
        label: Int? = null,
        externalId: ExternalId? = null,
    ) {
        viewModel.apply {
            Assert.assertEquals(
                labelStatus, this.labelStatus.getOrAwaitValue()
            )

            if (labelStatus != LabelStatus.None) {
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
        descriptionStatus: DescriptionStatus = DescriptionStatus.None,
        description: String? = null,
        cellState: CellState = CellState.None,
        shouldShowCurrentCell: Boolean = false,
        currentCell: String? = null,
    ) {
        viewModel.apply {
            Assert.assertEquals(descriptionStatus, this.descriptionStatus.getOrAwaitValue())
            Assert.assertEquals(cellState, this.cellState.getOrAwaitValue())

            if (descriptionStatus != DescriptionStatus.None) {
                Assert.assertEquals(
                    description,
                    this.description.getOrAwaitValue()
                )
            }

            if (cellState != CellState.None) {
                requireNotNull(currentCell)

                Assert.assertEquals(currentCell, this.currentCell.getOrAwaitValue())
            }

            if (shouldShowCurrentCell) {
                requireNotNull(currentCell)

                Assert.assertEquals(
                    shouldShowCurrentCell,
                    this.shouldShowCurrentCell.getOrAwaitValue()
                )
                Assert.assertEquals(currentCell, this.currentCell.getOrAwaitValue())
            }
        }
    }

    fun `assert description`(
        descriptionStatus: DescriptionStatus = DescriptionStatus.None,
        description: Int? = null,
        cellState: CellState = CellState.None,
        externalId: ExternalId? = null,
        shouldShowCurrentCell: Boolean = false,
        currentCell: String? = null,
    ) {
        viewModel.apply {
            Assert.assertEquals(descriptionStatus, this.descriptionStatus.getOrAwaitValue())
            Assert.assertEquals(cellState, this.cellState.getOrAwaitValue())

            if (descriptionStatus != DescriptionStatus.None) {
                requireNotNull(description)

                val expectedDescription = if (externalId != null) {
                    stringManager.getString(description, externalId)
                } else {
                    stringManager.getString(description)
                }

                Assert.assertNotNull(expectedDescription)
                Assert.assertEquals(
                    expectedDescription,
                    this.description.getOrAwaitValue()
                )
            }

            if (shouldShowCurrentCell) {
                requireNotNull(currentCell)

                Assert.assertEquals(
                    shouldShowCurrentCell,
                    this.shouldShowCurrentCell.getOrAwaitValue()
                )
                Assert.assertEquals(currentCell, this.currentCell.getOrAwaitValue())
            }
        }
    }
}
