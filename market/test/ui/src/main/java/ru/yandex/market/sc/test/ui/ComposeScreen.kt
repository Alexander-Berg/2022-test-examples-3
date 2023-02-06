package ru.yandex.market.sc.test.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.agoda.kakao.screen.Screen

abstract class ComposeScreen<out T : ComposeScreen<T, A>, A : ComponentActivity> : Screen<T>() {
    lateinit var composeRule: AndroidComposeTestRule<ActivityScenarioRule<A>, A>

    fun bindComposeRule(composeRule: AndroidComposeTestRule<ActivityScenarioRule<A>, A>) {
        this.composeRule = composeRule
    }

    inner class ComposeView(
        private val builder: AndroidComposeTestRule<ActivityScenarioRule<A>, A>.() -> SemanticsNodeInteraction
    ) {
        operator fun invoke(block: SemanticsNodeInteraction.() -> Unit) {
            builder(composeRule).block()
        }
    }

    companion object {
        inline fun <reified T : ComposeScreen<T, A>, A : ComponentActivity> onComposeScreen(
            composeRule: AndroidComposeTestRule<ActivityScenarioRule<A>, A>,
            noinline function: T.() -> Unit,
        ): T = onScreen {
            this.bindComposeRule(composeRule)
            function()
        }
    }
}
