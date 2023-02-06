package ru.yandex.market.mapi.core.model.screen.showcondition

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.AbstractShowConditionTest
import ru.yandex.market.mapi.core.UserPlusInfo
import ru.yandex.market.mapi.core.util.mockPlusInfo

class UserHasYandexPlusShowConditionTest : AbstractShowConditionTest() {

    @Test
    fun testContextHasNoUserPlusInfo() {
        assertConditionHide<UserHasYandexPlusShowCondition> { _, context ->
        }
    }

    @Test
    fun testUserHasNoPlus() {
        assertConditionHide<UserHasYandexPlusShowCondition> { _, context ->
            mockPlusInfo(UserPlusInfo(hasPlus = false, plusInfo = null))
        }
    }

    @Test
    fun testUserHasPlus() {
        assertConditionShow<UserHasYandexPlusShowCondition> { _, context ->
            mockPlusInfo(UserPlusInfo(hasPlus = true, plusInfo = null))
        }
    }
}
