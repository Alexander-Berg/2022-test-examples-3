package ru.yandex.market.mapi.core.model.screen.showcondition

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.AbstractShowConditionTest
import ru.yandex.market.mapi.core.util.buildMockUser
import ru.yandex.market.mapi.core.util.mockUserInfo

class UserIsYandexStaffShowConditionTest : AbstractShowConditionTest() {

    @Test
    fun testUserIsNotYandexoid() {
        assertConditionHide<UserIsYandexStaffShowCondition> { _, context ->
            mockUserInfo(buildMockUser())
        }
    }

    @Test
    fun testUserIsYandexoid() {
        assertConditionShow<UserIsYandexStaffShowCondition> { _, context ->
            mockUserInfo(buildMockUser(isYandexoid = true))
        }
    }

    @Test
    fun testUserIsAnonymous() {
        assertConditionHide<UserIsYandexStaffShowCondition>()
    }
}
