package ru.yandex.market.mapi.core.model.screen.showcondition

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.AbstractShowConditionTest

class UserRegionShowConditionTest : AbstractShowConditionTest() {

    @Test
    fun testNullRegions() {
        assertConditionFail<UserRegionShowCondition>(
            expectedMessage = "Section UserRegionShowCondition has no regions"
        ) { condition, _ ->
            condition.regions = null
        }
    }

    @Test
    fun testEmptyRegions() {
        assertConditionFail<UserRegionShowCondition>(
            expectedMessage = "Section UserRegionShowCondition has no regions"
        ) { condition, _ ->
            condition.regions = ""
        }
    }

    @Test
    fun testNeedShow() {
        assertConditionShow<UserRegionShowCondition> { condition, context ->
            condition.regions = "1,2,3"
            context.regionId = 2
        }
    }

    @Test
    fun testDontShow() {
        assertConditionHide<UserRegionShowCondition> { condition, context ->
            condition.regions = "1,3"
            context.regionId = 2
        }
    }
}
