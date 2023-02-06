package ru.yandex.market.tpl.courier.arch.ext

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import ru.yandex.market.tpl.courier.arch.rule.InstrumentedTestRuleManager
import ru.yandex.market.tpl.courier.data.feature.TestDataRepository

val testDataRepository: TestDataRepository get() = testComponent.testDataRepository

val instrumentedTestRuleManager: InstrumentedTestRuleManager get() = testComponent.instrumentedTestRuleManager

val resources: Resources get() = testComponent.context.resources

fun getQuantityString(@PluralsRes pluralsRes: Int, quantity: Int) =
    testComponent.context.resources.getQuantityString(pluralsRes, quantity, quantity)

fun getFormattedString(@StringRes stringResourceId: Int, vararg formatArguments: Any) =
    testComponent.context.resources.getString(stringResourceId, *formatArguments)