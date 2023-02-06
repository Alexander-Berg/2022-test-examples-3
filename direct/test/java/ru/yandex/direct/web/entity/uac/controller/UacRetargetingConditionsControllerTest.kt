package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoalType
import ru.yandex.direct.core.entity.uac.service.UacRetargetingService.toCoreRetargetingCondition
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacRetargetingConditionsControllerTest : UacRetargetingControllerTestBase() {

    @Test
    fun testGet() {
        val retargetingCondition1 = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(
                            id = mobileAppGoalIds[0], name = mobileAppGoalNameById[mobileAppGoalIds[0]],
                            type = UacRetargetingConditionRuleGoalType.MOBILE
                        )
                    ),
                ),
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.NOT,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(
                            id = mobileAppGoalIds[8], name = mobileAppGoalNameById[mobileAppGoalIds[8]],
                            type = UacRetargetingConditionRuleGoalType.MOBILE
                        )
                    ),
                ),
            ),
            name = "Условие ретаргетинга 1"
        )
        val retargetingCondition2 = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.OR,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(
                            id = mobileAppGoalIds[0], name = mobileAppGoalNameById[mobileAppGoalIds[0]],
                            type = UacRetargetingConditionRuleGoalType.MOBILE
                        )
                    ),
                ),
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.NOT,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(
                            id = mobileAppGoalIds[1], name = mobileAppGoalNameById[mobileAppGoalIds[1]],
                            type = UacRetargetingConditionRuleGoalType.MOBILE
                        )
                    ),
                ),
            ),
            name = "Условие ретаргетинга 2"
        )
        val retargetingCondition3 = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.OR,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(
                            id = mobileAppGoalIds[0], name = mobileAppGoalNameById[mobileAppGoalIds[0]],
                            type = UacRetargetingConditionRuleGoalType.MOBILE
                        )
                    ),
                ),
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(
                            id = mobileAppGoalIds[2], name = mobileAppGoalNameById[mobileAppGoalIds[2]],
                            type = UacRetargetingConditionRuleGoalType.MOBILE
                        )
                    ),
                ),
            ),
            name = "Условие ретаргетинга 3"
        )
        val directRetargetingCondition1 = toCoreRetargetingCondition(
            retargetingCondition1,
            userInfo.clientId.asLong(),
            type = ConditionType.metrika_goals
        )
        val directRetargetingCondition2 = toCoreRetargetingCondition(
            retargetingCondition2,
            userInfo.clientId.asLong(),
            type = ConditionType.metrika_goals
        )
        val directRetargetingCondition3 = toCoreRetargetingCondition(
            retargetingCondition3,
            userInfo.clientId.asLong(),
            type = ConditionType.metrika_goals
        )
        val directRetargetingCondition4 = RetargetingCondition()
            .withName("Условие ретаргетинга 4")
            .withType(ConditionType.metrika_goals)
            .withClientId(userInfo.clientId.asLong())
            .withTargetInterest(TargetInterest().withAdGroupId(0L))
            .withRules(listOf()) as RetargetingCondition

        val ids = retargetingConditionService.addRetargetingConditions(
            listOf(
                directRetargetingCondition1,
                directRetargetingCondition2,
                directRetargetingCondition3,
                directRetargetingCondition4
            ), userInfo.clientId
        )
        val result = getResult()
        val response = JsonUtils.MAPPER.readTree(result)["result"]
        val expectedResponse = listOf(
            UacRetargetingCondition(
                conditionRules = retargetingCondition1.conditionRules,
                name = retargetingCondition1.name,
                id = ids.get(0).result
            ),
            UacRetargetingCondition(
                conditionRules = retargetingCondition2.conditionRules,
                name = retargetingCondition2.name,
                id = ids.get(1).result
            ),
        )
        Assertions.assertThat(response).isEqualTo(
            JsonUtils.MAPPER.readTree(JsonUtils.toJson(expectedResponse))
        )
    }

    /**
     * Проверяем корректность фильтра авторетаргетингов из списка условий ретаргетингов
     */
    @Test
    fun test_WithAutoRetargeting_NoResults() {
        val retargetingCondition = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.OR,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(
                            id = mobileAppGoalIds[0], name = mobileAppGoalNameById[mobileAppGoalIds[0]],
                            type = UacRetargetingConditionRuleGoalType.MOBILE
                        )
                    ),
                ),
            ),
            name = "Авторетаргетинг"
        )
        val directRetargetingCondition = toCoreRetargetingCondition(
            retargetingCondition,
            userInfo.clientId.asLong(),
            type = ConditionType.metrika_goals
        )
            .withAutoRetargeting(true) as RetargetingCondition
        retargetingConditionService.addRetargetingConditions(
            listOf(
                directRetargetingCondition
            ), userInfo.clientId
        )
        val result = getResult()

        val response = JsonUtils.MAPPER.readTree(result)["result"]
        Assertions.assertThat(response).hasSize(0)
    }

    /**
     * Проверяем корректность фильтра георетаргетингов из списка условий ретаргетингов
     */
    @Test
    fun test_WithGeoRetargeting_NoResults() {
        val retargetingCondition = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.OR,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(
                            id = audienceGoals[0].id,
                            type = UacRetargetingConditionRuleGoalType.AUDIENCE
                        )
                    ),
                ),
            ),
            name = "Гео-ретаргетинг"
        )
        val directRetargetingCondition = toCoreRetargetingCondition(
            retargetingCondition,
            userInfo.clientId.asLong(),
            type = ConditionType.geo_segments
        )
        retargetingConditionService.addRetargetingConditions(
            listOf(
                directRetargetingCondition
            ), userInfo.clientId
        )
        val result = getResult()

        val response = JsonUtils.MAPPER.readTree(result)["result"]
        Assertions.assertThat(response).hasSize(0)
    }

    @Test
    fun testWithoutRetargetingConditions() {
        val result = getResult()

        val response = JsonUtils.MAPPER.readTree(result)["result"]
        Assertions.assertThat(response).hasSize(0)
    }

    @Test
    fun testNonExistentMobileApp() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/retargeting/conditions?mobile_app_id=111&ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    private fun getResult() : String {
        return mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/retargeting/conditions?mobile_app_id=${mobileApp.id}&ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
    }
}
