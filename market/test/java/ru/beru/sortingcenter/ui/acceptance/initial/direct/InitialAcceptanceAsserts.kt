package ru.beru.sortingcenter.ui.acceptance.initial.direct

import org.junit.Assert
import ru.yandex.market.sc.core.resources.StringManager
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.beru.sortingcenter.ui.acceptance.initial.direct.scanner.ScannerModeImpl as ScannerMode

object InitialAcceptanceAsserts {
    private lateinit var viewModel: InitialAcceptanceViewModel
    private lateinit var stringManager: StringManager

    fun bind(viewModel: InitialAcceptanceViewModel, stringManager: StringManager) {
        InitialAcceptanceAsserts.viewModel = viewModel
        InitialAcceptanceAsserts.stringManager = stringManager

        // чтобы при снанировании заказа уже быть подписанным на scanMode
        viewModel.apply {
            Assert.assertNotNull(scanMode.getOrAwaitValue())
        }
    }

    fun `assert scanner fragment`(
        scannerMode: ScannerMode = ScannerMode.DoNotScan,
        overlayState: OverlayState = OverlayState.None,
        overlayMessage: Int? = null,
    ) {
        viewModel.apply {
            Assert.assertEquals(scannerMode, this.scanMode.getOrAwaitValue())
            Assert.assertEquals(overlayState, this.overlayState.getOrAwaitValue())
            if (overlayMessage != null) {
                val overlayMessageActual = this.overlayMessage.getOrAwaitValue()
                requireNotNull(stringManager.getString(overlayMessage))
                requireNotNull(overlayMessageActual)

                Assert.assertEquals(
                    stringManager.getString(overlayMessage),
                    stringManager.getString(overlayMessageActual)
                )
            } else {
                Assert.assertNull(this.overlayMessage.getOrAwaitValue())
            }
        }
    }

    fun `assert label text`(
        isLabelAvailable: Boolean,
        label: String? = null,
    ) {
        viewModel.apply {
            if (isLabelAvailable) {
                requireNotNull(label)

                Assert.assertTrue(this.isLabelAvailable.getOrAwaitValue())
                Assert.assertNotNull(label)
                Assert.assertEquals(
                    label,
                    this.label.getOrAwaitValue()
                )
            } else {
                Assert.assertFalse(this.isLabelAvailable.getOrAwaitValue())
            }
        }
    }

    fun `assert label`(
        isLabelAvailable: Boolean,
        label: Int? = null,
    ) {
        viewModel.apply {
            if (isLabelAvailable) {
                requireNotNull(label)

                Assert.assertTrue(this.isLabelAvailable.getOrAwaitValue())
                Assert.assertNotNull(stringManager.getString(label))
                Assert.assertEquals(
                    stringManager.getString(label),
                    this.label.getOrAwaitValue()
                )
            } else {
                Assert.assertFalse(this.isLabelAvailable.getOrAwaitValue())
            }
        }
    }

    fun `assert description`(
        isDescriptionVisible: Boolean,
        description: Int? = null,
        isInfoButtonVisible: Boolean = false,
        deliveryServiceName: String? = null,
    ) {
        viewModel.apply {
            if (isDescriptionVisible) {
                requireNotNull(description)

                Assert.assertTrue(this.isDescriptionVisible.getOrAwaitValue())
                Assert.assertNotNull(stringManager.getString(description))
                when {
                    deliveryServiceName != null -> Assert.assertEquals(
                        stringManager.getString(description, deliveryServiceName),
                        this.description.getOrAwaitValue()
                    )
                    else -> Assert.assertEquals(
                        stringManager.getString(description),
                        this.description.getOrAwaitValue()
                    )
                }
                Assert.assertEquals(isInfoButtonVisible, this.isInfoButtonVisible.getOrAwaitValue())
            } else {
                Assert.assertFalse(this.isDescriptionVisible.getOrAwaitValue())
            }
        }
    }

    fun `assert cell`(
        shouldShowCell: Boolean,
        cell: String? = null,
        shouldShowCurrentCell: Boolean = false,
        currentCell: String? = null,
    ) {
        viewModel.apply {
            if (shouldShowCell) {
                Assert.assertTrue(this.shouldShowCell.getOrAwaitValue())
                Assert.assertEquals(cell, this.cell.getOrAwaitValue())
            } else {
                Assert.assertFalse(this.shouldShowCell.getOrAwaitValue())
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

    fun `assert expected date`(
        shouldShowExpectedDate: Boolean,
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
        }
    }
}
