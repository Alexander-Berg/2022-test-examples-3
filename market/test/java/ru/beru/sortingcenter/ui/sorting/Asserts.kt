package ru.beru.sortingcenter.ui.sorting

import org.junit.Assert
import ru.beru.sortingcenter.ui.sorting.orders.SortingOrdersViewModel
import ru.beru.sortingcenter.ui.sorting.orders.models.scanner.ScannerModeImpl
import ru.yandex.market.sc.core.resources.StringManager
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.test.utils.getOrAwaitValue

object Asserts {
    private lateinit var viewModel: SortingOrdersViewModel
    private lateinit var stringManager: StringManager

    fun bind(viewModel: SortingOrdersViewModel, stringManager: StringManager) {
        this.viewModel = viewModel
        this.stringManager = stringManager

        // чтобы при снанировании заказа уже быть подписанным на scanner.mode
        viewModel.apply {
            Assert.assertNotNull(scanner.mode.getOrAwaitValue())
        }
    }

    fun `assert scanner fragment`(
        scannerMode: ScannerModeImpl = ScannerModeImpl.DoNotScan,
        overlayState: OverlayState = OverlayState.None,
        overlayMessage: Int? = null,
    ) {
        viewModel.apply {
            Assert.assertEquals(scannerMode, this.scanner.mode.getOrAwaitValue())
            Assert.assertEquals(overlayState, this.scanner.overlayState.getOrAwaitValue())
            if (overlayMessage != null) {
                val overlayMessageValue = this.scanner.overlayMessage.getOrAwaitValue()
                requireNotNull(overlayMessageValue)
                Assert.assertEquals(
                    stringManager.getString(overlayMessage),
                    stringManager.getString(overlayMessageValue),
                )
            } else {
                Assert.assertNull(this.scanner.overlayMessage.getOrAwaitValue())
            }
        }
    }

    fun `assert current lot information`(
        isTextAvailable: Boolean = false,
        name: String? = null,
        status: Int? = null
    ) {
        viewModel.apply {
            if (isTextAvailable) {
                requireNotNull(name)
                requireNotNull(status)

                Assert.assertTrue(this.currentLot.isTextAvailable.getOrAwaitValue())
                Assert.assertEquals(name, this.currentLot.name.getOrAwaitValue())
                Assert.assertEquals(
                    stringManager.getString(status),
                    this.currentLot.status.getOrAwaitValue()
                )
            } else {
                Assert.assertFalse(this.currentLot.isTextAvailable.getOrAwaitValue())
            }
        }
    }

    fun `assert label`(
        isLabelAvailable: Boolean,
        label: Int? = null,
        labelColor: Int? = null,
        externalId: ExternalId? = null,
    ) {
        viewModel.apply {
            if (isLabelAvailable) {
                requireNotNull(label)
                requireNotNull(labelColor)

                Assert.assertTrue(this.label.isTextAvailable.getOrAwaitValue())

                val expectedLabel = if (externalId != null) {
                    stringManager.getString(label, externalId)
                } else {
                    stringManager.getString(label)
                }
                Assert.assertEquals(
                    expectedLabel,
                    this.label.text.getOrAwaitValue()
                )
                Assert.assertEquals(
                    labelColor,
                    this.label.textColor.getOrAwaitValue()
                )
            } else {
                Assert.assertFalse(this.label.isTextAvailable.getOrAwaitValue())
            }
        }
    }

    fun `assert description text`(
        isDescriptionVisible: Boolean,
        description: Int,
        lotName: String,
        isInfoButtonVisible: Boolean = false,
    ) {
        val expectedDescription = stringManager.getString(description, lotName)
        `assert description text`(isDescriptionVisible, expectedDescription, isInfoButtonVisible)
    }

    fun `assert description text`(
        isDescriptionVisible: Boolean,
        description: String? = null,
        isInfoButtonVisible: Boolean = false,
    ) {
        viewModel.apply {
            if (isDescriptionVisible) {
                requireNotNull(description)

                Assert.assertTrue(this.description.isTextVisible.getOrAwaitValue())
                Assert.assertNotNull(description)
                Assert.assertEquals(
                    description,
                    this.description.text.getOrAwaitValue()
                )
                Assert.assertEquals(
                    isInfoButtonVisible,
                    this.description.isInfoButtonVisible.getOrAwaitValue()
                )
            } else {
                Assert.assertFalse(this.description.isTextVisible.getOrAwaitValue())
            }
        }
    }

    fun `assert description visible`(isDescriptionVisible: Boolean, ) {
        Assert.assertEquals(viewModel.description.isTextVisible.getOrAwaitValue(), isDescriptionVisible)
    }

    fun `assert description`(
        isDescriptionVisible: Boolean,
        description: Int? = null,
        isInfoButtonVisible: Boolean = false,
    ) {
        viewModel.apply {
            if (isDescriptionVisible) {
                requireNotNull(description)

                Assert.assertTrue(this.description.isTextVisible.getOrAwaitValue())
                Assert.assertNotNull(stringManager.getString(description))
                Assert.assertEquals(
                    stringManager.getString(description),
                    this.description.text.getOrAwaitValue()
                )
                Assert.assertEquals(
                    isInfoButtonVisible,
                    this.description.isInfoButtonVisible.getOrAwaitValue()
                )
            } else {
                Assert.assertFalse(this.description.isTextVisible.getOrAwaitValue())
            }
        }
    }

    fun `assert sort destination info`(
        shouldShow: Boolean,
        destinationNumber: Int,
    ) {
        `assert sort destination info`(shouldShow, stringManager.getString(destinationNumber))
    }

    fun `assert sort destination info`(
        shouldShow: Boolean,
        destinationNumber: String? = null,
    ) {
        viewModel.apply {
            if (shouldShow) {
                Assert.assertTrue(this.sortDestinationInfo.isNumberAvailable.getOrAwaitValue())
                Assert.assertEquals(
                    destinationNumber,
                    this.sortDestinationInfo.number.getOrAwaitValue()
                )
            } else {
                Assert.assertFalse(this.sortDestinationInfo.isNumberAvailable.getOrAwaitValue())
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

                Assert.assertTrue(this.expectedDate.shouldShowDate.getOrAwaitValue())
                Assert.assertNotNull(stringManager.getString(dateFormat))
                Assert.assertEquals(
                    stringManager.getString(dateFormat),
                    stringManager.getString(this.expectedDate.dateFormat.getOrAwaitValue())
                )
                Assert.assertEquals(expectedDate, this.expectedDate.date.getOrAwaitValue())
            } else {
                Assert.assertFalse(this.expectedDate.shouldShowDate.getOrAwaitValue())
            }

            Assert.assertEquals(
                shouldShowExpectedDateTitle,
                this.expectedDate.shouldShowTitle.getOrAwaitValue()
            )
        }
    }
}
