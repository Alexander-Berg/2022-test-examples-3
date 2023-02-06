package ru.yandex.market.sc.test.scanner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import ru.yandex.market.sc.core.data.scanner.ScanGlobal.SCANNER_CONTENT_DESCRIPTION
import ru.yandex.market.sc.core.data.scanner.ScanGlobal.SCANNER_OVERLAY_CONTENT_DESCRIPTION

fun ComposeContentTestRule.assertScannerTitleText(text: String) {
    onNodeWithContentDescription(SCANNER_CONTENT_DESCRIPTION)
        .assertIsDisplayed()
        .assertTextEquals(text)
}

fun ComposeContentTestRule.assertScannerOverlayText(text: String) {
    onNodeWithContentDescription(SCANNER_OVERLAY_CONTENT_DESCRIPTION)
        .assertIsDisplayed()
        .assertTextEquals(text)
}
