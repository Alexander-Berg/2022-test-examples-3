package ru.yandex.market.mapi.core

import ru.yandex.market.mapi.core.model.screen.AbstractShowCondition
import ru.yandex.market.mapi.core.model.screen.ShowResultType
import ru.yandex.market.mapi.core.model.screen.showcondition.AppVersionMatcher
import kotlin.reflect.full.createInstance
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

abstract class AbstractShowConditionTest : AbstractNonSpringTest() {

    fun appVersionMatcher(init: AppVersionMatcher.() -> Unit) = AppVersionMatcher().also(init)

    inline fun <reified T : AbstractShowCondition> showCondition(init: T.() -> Unit = {}): T {
        val instanse = T::class.createInstance()
        instanse.init()
        return instanse
    }

    inline fun <reified T : AbstractShowCondition> assertConditionShow(
        init: (T, MapiContextRw) -> Unit = { _, _ -> }
    ) {
        assertCondition(init) { condition, context ->
            val result = condition.needShow(context)
            assertEquals(ShowResultType.SHOW, result.showResult)
            assertNull(result.error)
        }
    }

    inline fun <reified T : AbstractShowCondition> assertConditionHide(
        init: (T, MapiContextRw) -> Unit = { _, _ -> }
    ) {
        assertCondition(init) { condition, context ->
            val result = condition.needShow(context)
            assertEquals(ShowResultType.HIDE, result.showResult)
            assertNull(result.error)
        }
    }

    inline fun <reified T : AbstractShowCondition> assertConditionFail(
        expectedMessage: String,
        init: (T, MapiContextRw) -> Unit = { _, _ -> }
    ) {
        assertCondition(init) { condition, context ->
            val result = condition.needShow(context)
            assertEquals(ShowResultType.FAIL, result.showResult)
            assertNotNull(result.error)
            assertEquals(expectedMessage, result.error?.message)
        }
    }

    inline fun <reified T : AbstractShowCondition> assertCondition(
        init: (T, MapiContextRw) -> Unit = { _, _ -> },
        checker: (T, MapiContext) -> Unit
    ) {
        val condition = showCondition<T>()
        val context = MapiContext.get() as MapiContextRw
        init(condition, context)
        return checker(condition, context)
    }
}
