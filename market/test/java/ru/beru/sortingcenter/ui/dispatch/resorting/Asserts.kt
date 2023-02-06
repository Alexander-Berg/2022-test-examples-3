package ru.beru.sortingcenter.ui.dispatch.resorting

import org.junit.Assert
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.ui.common.scannerbuttons.ScannerButtonsViewModel.CenterButtonState
import ru.beru.sortingcenter.ui.common.scannerbuttons.ScannerButtonsViewModel.UpButtonState
import ru.beru.sortingcenter.ui.dispatch.resorting.models.ScannerModeImpl
import ru.yandex.market.sc.core.resources.StringManager
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.test.utils.getOrAwaitValue

object Asserts {
    private lateinit var viewModel: DispatchResortingViewModel
    private lateinit var stringManager: StringManager

    fun bind(viewModel: DispatchResortingViewModel, stringManager: StringManager) {
        Asserts.viewModel = viewModel
        Asserts.stringManager = stringManager

        // чтобы при снанировании заказа уже быть подписанным на scanMode
        viewModel.apply {
            Assert.assertNotNull(scanner.mode.getOrAwaitValue())
        }
    }

    fun `assert initial state`(
        dispatchCanBeFinished: Boolean,
    ) {
        `assert scanner fragment`(
            scannerMode = ScannerModeImpl.Dispatchable,
            overlayState = OverlayState.None,
        )
        `assert button fragment`(
            centerButtonState = CenterButtonState.ShowList,
            upButtonState = if (dispatchCanBeFinished) UpButtonState.Enable else UpButtonState.Disable,
            upButtonTitle = R.string.finish_dispatch,
        )
        `assert label`(
            isLabelAvailable = false
        )
        `assert scanned place count`(
            isScannedPlaceCountTextVisible = false,
            isResetPlaceTextVisible = false
        )
        `assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    fun `assert scanner fragment`(
        scannerMode: ScannerModeImpl = ScannerModeImpl.DoNotScan,
        overlayState: OverlayState = OverlayState.None,
    ) {
        viewModel.apply {
            Assert.assertEquals(scannerMode, this.scanner.mode.getOrAwaitValue())
            Assert.assertEquals(overlayState, this.scanner.overlayState.getOrAwaitValue())
        }
    }

    fun `assert button fragment`(
        centerButtonState: CenterButtonState = CenterButtonState.None,
        upButtonState: UpButtonState = UpButtonState.None,
        upButtonTitle: Int? = null,
    ) {
        viewModel.apply {
            Assert.assertEquals(centerButtonState, this.showListButton.state.getOrAwaitValue())
            Assert.assertEquals(upButtonState, this.upButtonState.getOrAwaitValue())

            if (upButtonState != UpButtonState.None) {
                requireNotNull(upButtonTitle)

                Assert.assertNotNull(stringManager.getString(upButtonTitle))
                Assert.assertEquals(
                    stringManager.getString(upButtonTitle),
                    stringManager.getString(this.upButtonTitle.getOrAwaitValue())
                )
            }
        }
    }

    fun `assert label`(
        isLabelAvailable: Boolean,
        label: Int? = null,
        externalId: ExternalId? = null,
    ) {
        viewModel.apply {
            if (isLabelAvailable) {
                requireNotNull(label)

                val expectedLabel = if (externalId != null) {
                    stringManager.getString(label, externalId)
                } else {
                    stringManager.getString(label)
                }

                Assert.assertTrue(this.label.isTextVisible.getOrAwaitValue())
                Assert.assertEquals(
                    expectedLabel,
                    this.label.text.getOrAwaitValue()
                )
            } else {
                Assert.assertFalse(this.label.isTextVisible.getOrAwaitValue())
            }
        }
    }

    fun `assert description`(
        isDescriptionVisible: Boolean,
        description: String? = null,
        isInfoButtonVisible: Boolean = false,
    ) {
        viewModel.apply {
            if (isDescriptionVisible) {
                requireNotNull(description)
                Assert.assertTrue(this.description.isTextVisible.getOrAwaitValue())
                Assert.assertEquals(description, this.description.text.getOrAwaitValue())
                Assert.assertEquals(
                    isInfoButtonVisible,
                    this.description.isInfoButtonVisible.getOrAwaitValue()
                )
            } else {
                Assert.assertFalse(this.description.isTextVisible.getOrAwaitValue())
            }
        }
    }

    fun `assert description`(
        isDescriptionVisible: Boolean,
        description: Int,
        externalId: ExternalId? = null,
        isInfoButtonVisible: Boolean = false,
    ) {
        val expectedDescription = if (externalId != null) {
            stringManager.getString(description, externalId)
        } else {
            stringManager.getString(description)
        }

        `assert description`(isDescriptionVisible, expectedDescription, isInfoButtonVisible)
    }

    fun `assert scanned place count`(
        isScannedPlaceCountTextVisible: Boolean = false,
        scannedPlaces: Int = 0,
        totalPlaces: Int = 0,
        isResetPlaceTextVisible: Boolean = false,
        restPlaceExternalId: ExternalId? = null,
    ) {
        viewModel.apply {
            if (isScannedPlaceCountTextVisible) {
                Assert.assertTrue(this.placeScanHints.scannedPlaceCountTextVisible.getOrAwaitValue())
                val scannedPlaceCountText =
                    stringManager.getString(
                        R.string.scanned_places_of_all_places,
                        scannedPlaces,
                        totalPlaces
                    )
                Assert.assertEquals(
                    scannedPlaceCountText,
                    this.placeScanHints.scannedPlaceCountText.getOrAwaitValue()
                )
            } else {
                Assert.assertFalse(this.placeScanHints.scannedPlaceCountTextVisible.getOrAwaitValue())
            }

            if (isResetPlaceTextVisible) {
                Assert.assertTrue(this.placeScanHints.resetPlaceTextVisible.getOrAwaitValue())
                val resetPlaceText =
                    stringManager.getString(R.string.rest_place_to_scan, restPlaceExternalId)
                Assert.assertEquals(
                    resetPlaceText,
                    this.placeScanHints.resetPlaceText.getOrAwaitValue()
                )
            } else {
                Assert.assertFalse(this.placeScanHints.resetPlaceTextVisible.getOrAwaitValue())
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
            Assert.assertEquals(
                shouldShowExpectedDateTitle,
                this.dispatchDate.shouldShowTitle.getOrAwaitValue()
            )

            if (shouldShowExpectedDate) {
                requireNotNull(dateFormat)

                Assert.assertTrue(this.dispatchDate.shouldShowExpectedDate.getOrAwaitValue())
                Assert.assertNotNull(stringManager.getString(dateFormat))
                Assert.assertEquals(
                    stringManager.getString(dateFormat),
                    stringManager.getString(this.dispatchDate.format.getOrAwaitValue())
                )
                Assert.assertEquals(expectedDate, this.dispatchDate.expectedDate.getOrAwaitValue())
            } else {
                Assert.assertFalse(this.dispatchDate.shouldShowExpectedDate.getOrAwaitValue())
            }
        }
    }
}
