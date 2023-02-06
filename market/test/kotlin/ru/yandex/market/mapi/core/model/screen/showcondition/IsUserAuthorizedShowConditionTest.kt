package ru.yandex.market.mapi.core.model.screen.showcondition

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.AbstractShowConditionTest
import ru.yandex.market.mapi.core.util.buildMockUser
import ru.yandex.market.mapi.core.util.mockUserInfo

class IsUserAuthorizedShowConditionTest : AbstractShowConditionTest() {
    @Test
    fun testContextHasNoOAuthInfo() {
        assertConditionHide<IsUserAuthorizedShowCondition> { _, context ->
            // nothing
        }
    }

    @Test
    fun testContextHasOauthInfo() {
        assertConditionShow<IsUserAuthorizedShowCondition> { _, context ->
            mockUserInfo(buildMockUser())
        }
    }
}
