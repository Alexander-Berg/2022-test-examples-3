package ru.yandex.market.test.arch.ext

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import ru.yandex.market.test.utils.MainDispatcherRule

fun commonViewModelTestRules(
    mainDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
): CommonViewModelTestRules {
    val dispatcherRule = MainDispatcherRule(mainDispatcher)
    val ruleChain = RuleChain.outerRule(InstantTaskExecutorRule()).around(dispatcherRule)
    return object : CommonViewModelTestRules {

        override val testDispatcher: TestDispatcher get() = dispatcherRule.testDispatcher

        override fun apply(base: Statement, description: Description): Statement {
            return ruleChain.apply(base, description)
        }
    }
}

interface CommonViewModelTestRules : TestRule {

    val testDispatcher: TestDispatcher
}
