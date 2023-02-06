package ru.yandex.market.mapi.client.uaas

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.AbstractClientTest
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.mapiContext
import ru.yandex.market.mapi.core.util.getMockContextRw
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author Ilya Kislitsyn / ilyakis@ / 28.06.2022
 */
class UaasConditionCheckerTest  : AbstractClientTest(){
    private val conditionChecker = UaasConditionChecker()

    @BeforeEach
    fun initOauth() {

        getMockContextRw()?.appVersionRaw = "3.5.1"
        getMockContextRw()?.appPlatform = MapiHeaders.PLATFORM_IOS

        conditionChecker.clearCache()
    }

    @Test
    fun testConditionParseCache() {
        val expression = "android || ios"

        // check cache works
        val orig = conditionChecker.parseExpression(expression)
        assertTrue { orig == conditionChecker.parseExpression(expression) }

        // reset cache
        conditionChecker.clearCache()
        assertFalse { orig == conditionChecker.parseExpression(expression) }
    }

    @Test
    fun testConditionCache() {
        val expression = "android || ios"
        val expression2 = "android && v > '1.1'"

        // prepare contexts
        val context1 = UaasConditionChecker.ConditionAstContext(mapiContext())

        // another version
        getMockContextRw()?.appVersionRaw = "1.02"
        val context2 = UaasConditionChecker.ConditionAstContext(mapiContext())

        // another platform
        getMockContextRw()?.appPlatform = MapiHeaders.PLATFORM_ANDROID
        val context3 = UaasConditionChecker.ConditionAstContext(mapiContext())

        val orig = conditionChecker.checkIfExpApplicable(expression, context1) {}

        // check cache works
        assertTrue { orig == conditionChecker.checkIfExpApplicable(expression, context1) {} }

        // check version/platform/expression changes misses cache
        assertTrue { orig == conditionChecker.checkIfExpApplicable(expression2, context1) {} }
        assertTrue { orig == conditionChecker.checkIfExpApplicable(expression, context2) {} }
        assertTrue { orig == conditionChecker.checkIfExpApplicable(expression, context3) {} }

        // next call will return same value for them
        assertTrue {
            conditionChecker.checkIfExpApplicable(expression2, context1) {} ==
                conditionChecker.checkIfExpApplicable(expression2, context1) {}
        }
        assertTrue {
            conditionChecker.checkIfExpApplicable(expression, context2) {} ==
                conditionChecker.checkIfExpApplicable(expression, context2) {}
        }

        assertTrue {
            conditionChecker.checkIfExpApplicable(expression, context3) {} ==
                conditionChecker.checkIfExpApplicable(expression, context3) {}
        }
    }
}