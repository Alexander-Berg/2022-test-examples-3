package ru.yandex.market.mobile.android.test.ui.kakao.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.agoda.kakao.screen.Screen

typealias ComposeTestRule<A> = AndroidComposeTestRule<ActivityScenarioRule<A>, A>

abstract class ComposeScreen<out T : ComposeScreen<T, A>, A : ComponentActivity> : Screen<T>() {
    lateinit var composeRule: AndroidComposeTestRule<ActivityScenarioRule<A>, A>

    fun bindComposeRule(composeRule: AndroidComposeTestRule<ActivityScenarioRule<A>, A>) {
        this.composeRule = composeRule
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
