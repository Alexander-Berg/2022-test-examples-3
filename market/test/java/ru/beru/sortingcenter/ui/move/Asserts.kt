package ru.beru.sortingcenter.ui.move

import org.junit.Assert
import ru.beru.sortingcenter.ui.common.scannerdestination.ScannerDestinationViewModel.*
import ru.beru.sortingcenter.ui.move.orders.MoveOrdersViewModel
import ru.beru.sortingcenter.ui.move.orders.models.scanner.ScannerModeImpl
import ru.yandex.market.sc.core.resources.StringManager
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.test.utils.getOrAwaitValue

object Asserts {
    private lateinit var viewModel: MoveOrdersViewModel
    private lateinit var stringManager: StringManager

    fun bind(viewModel: MoveOrdersViewModel, stringManager: StringManager) {
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
        description: String,
        cellState: CellState = CellState.None,
        cellTitle: String? = null,
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
                requireNotNull(cellTitle)

                Assert.assertEquals(cellTitle, this.cellTitle.getOrAwaitValue())
            }
        }
    }

    fun `assert description`(
        descriptionStatus: DescriptionStatus = DescriptionStatus.None,
        description: Int? = null,
        cellState: CellState = CellState.None,
        cellTitle: String? = null,
    ) {
        viewModel.apply {
            Assert.assertEquals(descriptionStatus, this.descriptionStatus.getOrAwaitValue())
            Assert.assertEquals(cellState, this.cellState.getOrAwaitValue())

            if (descriptionStatus != DescriptionStatus.None) {
                requireNotNull(description)

                Assert.assertNotNull(stringManager.getString(description))
                Assert.assertEquals(
                    stringManager.getString(description),
                    this.description.getOrAwaitValue()
                )
            }

            if (cellState != CellState.None) {
                Assert.assertEquals(cellTitle, this.cellTitle.getOrAwaitValue())
            }
        }
    }

    fun `assert expected date`(
        shouldShowExpectedDate: Boolean,
        shouldShowExpectedDateTitle: Boolean,
        dateFormat: Int? = null,
        expectedDate: String? = null,
    ) {
        viewModel.apply {
            if (shouldShowExpectedDate) {
                requireNotNull(dateFormat)

                Assert.assertTrue(this.shouldShowExpectedDate.getOrAwaitValue())
                Assert.assertNotNull(stringManager.getString(dateFormat))
                Assert.assertEquals(
                    stringManager.getString(dateFormat),
                    stringManager.getString(this.dateFormat.getOrAwaitValue())
                )
                Assert.assertEquals(expectedDate, this.expectedDate.getOrAwaitValue())
            } else {
                Assert.assertFalse(this.shouldShowExpectedDate.getOrAwaitValue())
            }

            Assert.assertEquals(
                shouldShowExpectedDateTitle,
                this.shouldShowExpectedDateTitle.getOrAwaitValue()
            )
        }
    }

}
