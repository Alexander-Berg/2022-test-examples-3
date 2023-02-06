package ru.yandex.market.sc.test.ui.arch.ext

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import ru.yandex.market.sc.test.ui.ComposeScreen

fun <T : ComposeScreen<T, A>, A : ComponentActivity> ComposeScreen<T, A>.composableForTag(
    tag: String
) = ComposeView {
    onNodeWithTag(tag)
}

fun <T : ComposeScreen<T, A>, A : ComponentActivity> ComposeScreen<T, A>.composableForText(
    text: String
) = ComposeView {
    onNodeWithText(text)
}

fun <T : ComposeScreen<T, A>, A : ComponentActivity> ComposeScreen<T, A>.composableForContentDescription(
    contentDescription: String
) = ComposeView {
    onNodeWithContentDescription(contentDescription)
}

fun <T : ComposeScreen<T, A>, A : ComponentActivity> ComposeScreen<T, A>.applyOnComposableWithTag(
    tag: String,
    block: SemanticsNodeInteraction.() -> Unit
) = composableForTag(tag).invoke(block)

fun <T : ComposeScreen<T, A>, A : ComponentActivity> ComposeScreen<T, A>.applyOnComposableWithText(
    text: String,
    block: SemanticsNodeInteraction.() -> Unit
) = composableForText(text).invoke(block)