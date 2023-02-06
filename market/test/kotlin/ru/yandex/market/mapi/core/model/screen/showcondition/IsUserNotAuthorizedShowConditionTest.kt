package ru.yandex.market.mapi.core.model.screen.showcondition

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.AbstractShowConditionTest
import ru.yandex.market.mapi.core.util.buildMockUser
import ru.yandex.market.mapi.core.util.mockUserInfo

class IsUserNotAuthorizedShowConditionTest : AbstractShowConditionTest() {

    @Test
    fun testContextHasNoOAuthInfo() {
        assertConditionShow<IsUserNotAuthorizedShowCondition> { _, context ->
            // nothing
        }
    }

    @Test
    fun testContextHasOauthInfo() {
        assertConditionHide<IsUserNotAuthorizedShowCondition> { _, context ->
            mockUserInfo(buildMockUser())
        }
    }
}
