package ru.yandex.market.mapi.core.model.screen.showcondition

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.AbstractShowConditionTest
import ru.yandex.market.mapi.core.util.buildMockUser
import ru.yandex.market.mapi.core.util.mockUserInfo

class CombinedShowConditionTest : AbstractShowConditionTest() {

    @Test
    fun testCombinationIsNull() {
        assertConditionFail<CombinedShowCondition>(
            expectedMessage = "CombinedShowCondition. Unsupported combination"
        ) { condition, context ->
            condition.combination = null
            condition.conditions = listOf(
                showCondition<UserRegionShowCondition> {
                    regions = "1,2"
                },
                showCondition<IsUserAuthorizedShowCondition>()
            )

            context.regionId = 1
            mockUserInfo(buildMockUser())
        }
    }

    @Test
    fun testConditionsIsNull() {
        assertConditionShow<CombinedShowCondition> { condition, context ->
            condition.combination = Combination.AND
            condition.conditions = null

            context.regionId = 1
            mockUserInfo(buildMockUser())
        }
    }

    @Test
    fun testConditionsIsEmpty() {
        assertConditionShow<CombinedShowCondition> { condition, context ->
            condition.combination = Combination.AND
            condition.conditions = emptyList()

            context.regionId = 1
            mockUserInfo(buildMockUser())
        }
    }

    @Test
    fun testAndCombinationPositive() {
        assertConditionShow<CombinedShowCondition> { condition, context ->
            condition.combination = Combination.AND
            condition.conditions = listOf(
                showCondition<UserRegionShowCondition> {
                    regions = "1,2"
                },
                showCondition<IsUserAuthorizedShowCondition>()
            )

            context.regionId = 1
            mockUserInfo(buildMockUser())
        }
    }

    @Test
    fun testAndCombinationNegative() {
        assertConditionHide<CombinedShowCondition> { condition, context ->
            condition.combination = Combination.AND
            condition.conditions = listOf(
                showCondition<UserRegionShowCondition> {
                    regions = "1,2"
                },
                showCondition<IsUserAuthorizedShowCondition>()
            )

            context.regionId = 3
            mockUserInfo(buildMockUser())
        }
    }

    @Test
    fun testOrCombinationPositive() {
        assertConditionShow<CombinedShowCondition> { condition, context ->
            condition.combination = Combination.OR
            condition.conditions = listOf(
                showCondition<UserRegionShowCondition> {
                    regions = "1,2"
                },
                showCondition<IsUserAuthorizedShowCondition>()
            )

            context.regionId = 1
        }
    }

    @Test
    fun testOrCombinationNegative() {
        assertConditionHide<CombinedShowCondition> { condition, context ->
            condition.combination = Combination.OR
            condition.conditions = listOf(
                showCondition<UserRegionShowCondition> {
                    regions = "1,2"
                },
                showCondition<IsUserAuthorizedShowCondition>()
            )

            context.regionId = 3
        }
    }

    @Test
    fun testOrCombination3ConditionsPositive() {
        assertConditionShow<CombinedShowCondition> { condition, context ->
            condition.combination = Combination.OR
            condition.conditions = listOf(
                showCondition<UserRegionShowCondition> {
                    regions = "1,2"
                },
                showCondition<IsUserAuthorizedShowCondition>(),
                showCondition<UserHasYandexPlusShowCondition>()
            )

            context.regionId = 3
            mockUserInfo(buildMockUser())
        }
    }

    @Test
    fun testOrCombinationHasBrokenCondition() {
        assertConditionFail<CombinedShowCondition>(
            expectedMessage = "Section UserRegionShowCondition has no regions"
        ) { condition, context ->
            condition.combination = Combination.OR
            condition.conditions = listOf(
                showCondition<UserRegionShowCondition> {
                    regions = null
                },
                showCondition<IsUserAuthorizedShowCondition>(),
                showCondition<UserHasYandexPlusShowCondition>()
            )

            context.regionId = 3
            mockUserInfo(buildMockUser())
        }
    }

    @Test
    fun testAndCombinationHasBrokenCondition() {
        assertConditionFail<CombinedShowCondition>(
            expectedMessage = "Section UserRegionShowCondition has no regions"
        ) { condition, context ->
            condition.combination = Combination.AND
            condition.conditions = listOf(
                showCondition<UserRegionShowCondition> {
                    regions = null
                },
                showCondition<IsUserAuthorizedShowCondition>(),
                showCondition<UserHasYandexPlusShowCondition>()
            )

            context.regionId = 3
            mockUserInfo(buildMockUser())
        }
    }
}
