package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.SoftAssertions
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoalType
import ru.yandex.direct.core.entity.uac.model.UacSegmentInfo
import ru.yandex.direct.core.entity.uac.service.UacRetargetingService
import ru.yandex.direct.multitype.entity.LimitOffset
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.validation.defect.ids.StringDefectIds
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.entity.uac.model.RetargetingConditionRequest
import ru.yandex.direct.web.validation.model.WebValidationResult

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacRetargetingConditionsCreateControllerTest : UacRetargetingControllerTestBase() {
    @Test
    fun testCreate() {
        val retargetingConditionRequest = RetargetingConditionRequest(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(
                        createUacRetargetingConditionRuleGoal(id = mobileAppGoalIds[0], time = 540),
                        createUacRetargetingConditionRuleGoal(id = mobileAppGoalIds[8], time = 360),
                        createUacRetargetingConditionRuleGoal(
                            id = mobileAppGoalIds[9],
                            type = UacRetargetingConditionRuleGoalType.LAL
                        ),
                    ),
                ),
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.NOT,
                    goals = listOf(
                        createUacRetargetingConditionRuleGoal(id = mobileAppGoalIds[15], time = 15),
                    ),
                )
            ),
            name = "Условие ретаргетинга 1",
            mobileAppId = mobileApp.id,
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/retargeting/conditions?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(retargetingConditionRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val directRetargetingCondition = retargetingConditionService.getRetargetingConditions(
            userInfo.clientId, null,
            LimitOffset.maxLimited()
        )[0]
        val lalGoal = lalSegmentRepository.getLalSegmentsByParentIds(listOf(mobileAppGoalIds[9])).first()

        val retargetingCondition = UacRetargetingCondition(
            name = "Условие ретаргетинга 1",
            id = directRetargetingCondition.id,
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(
                        createUacRetargetingConditionRuleGoal(
                            id = mobileAppGoalIds[0],
                            name = mobileAppGoalNameById[mobileAppGoalIds[0]],
                        ),
                        createUacRetargetingConditionRuleGoal(
                            id = mobileAppGoalIds[8],
                            name = mobileAppGoalNameById[mobileAppGoalIds[8]],
                            time = 360,
                        ),
                        createUacRetargetingConditionRuleGoal(
                            id = mobileAppGoalIds[9],
                            name = mobileAppGoalNameById[mobileAppGoalIds[9]],
                            type = UacRetargetingConditionRuleGoalType.LAL,
                        ),
                    ),
                ),
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.NOT,
                    goals = listOf(
                        createUacRetargetingConditionRuleGoal(
                            id = mobileAppGoalIds[15],
                            name = mobileAppGoalNameById[mobileAppGoalIds[15]],
                            time = 15,
                        ),
                    )
                )
            ),
        )

        val response = JsonUtils.MAPPER.readTree(result)["result"]
        val soft = SoftAssertions()
        soft.assertThat(response).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(listOf(retargetingCondition))
            )
        )
        soft.assertThat(directRetargetingCondition.rules).hasSize(2)
        soft.assertThat(directRetargetingCondition.rules[0].goals).hasSize(3)
        soft.assertThat(directRetargetingCondition.rules[0].goals[0].id).isEqualTo(mobileAppGoalIds[0])
        soft.assertThat(directRetargetingCondition.rules[0].goals[1].id).isEqualTo(mobileAppGoalIds[8])
        soft.assertThat(directRetargetingCondition.rules[0].goals[2].id).isEqualTo(lalGoal.id)
        soft.assertThat(directRetargetingCondition.rules[0].goals[2].type).isEqualTo(GoalType.LAL_SEGMENT)

        val resultGet = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/retargeting/conditions?mobile_app_id=${mobileApp.id}&ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        val responseGet = JsonUtils.MAPPER.readTree(resultGet)["result"]
        soft.assertThat(responseGet).hasSize(1)
        soft.assertThat(responseGet[0]["condition_rules"][0]["goals"][2]["id"].asLong()).isEqualTo(mobileAppGoalIds[9])
        soft.assertThat(responseGet[0]["condition_rules"][0]["goals"][2]["type"].asText()).isEqualTo("LAL")
        soft.assertThat(responseGet[0]["condition_rules"][0]["goals"][2]["name"].asText())
            .isEqualTo(mobileAppGoalNameById[mobileAppGoalIds[9]])
        soft.assertAll()
    }

    @Test
    fun testCreate_WithAudienceGoals_WithoutAppId() {
        val retargetingConditionRequest = RetargetingConditionRequest(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(
                        createUacRetargetingConditionRuleGoal(
                            id = audienceGoals[0].id,
                            type = UacRetargetingConditionRuleGoalType.AUDIENCE
                        ),
                    ),
                ),
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.NOT,
                    goals = listOf(
                        createUacRetargetingConditionRuleGoal(
                            id = audienceGoals[1].id,
                            type = UacRetargetingConditionRuleGoalType.LAL_AUDIENCE
                        ),
                    ),
                )
            ),
            name = "Условие ретаргетинга 1",
            mobileAppId = null,
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/retargeting/conditions?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(retargetingConditionRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val directRetargetingCondition = retargetingConditionService.getRetargetingConditions(
            userInfo.clientId, null,
            LimitOffset.maxLimited()
        )[0]
        val lalSegment = lalSegmentRepository.getLalSegmentsByParentIds(listOf(audienceGoals[1].id)).first()

        val retargetingCondition = UacRetargetingCondition(
            name = "Условие ретаргетинга 1",
            id = directRetargetingCondition.id,
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(
                        createUacRetargetingConditionRuleGoal(
                            id = audienceGoals[0].id,
                            type = UacRetargetingConditionRuleGoalType.AUDIENCE,
                            time = 540,
                            name = null,
                        ),
                    ),
                ),
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.NOT,
                    goals = listOf(
                        createUacRetargetingConditionRuleGoal(
                            id = audienceGoals[1].id,
                            type = UacRetargetingConditionRuleGoalType.LAL_AUDIENCE,
                            time = 540,
                            name = null,
                        ),
                    )
                )
            ),
        )

        val response = JsonUtils.MAPPER.readTree(result)["result"]
        val soft = SoftAssertions()
        soft.assertThat(response).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(listOf(retargetingCondition))
            )
        )
        soft.assertThat(directRetargetingCondition.rules).hasSize(2)
        soft.assertThat(directRetargetingCondition.rules[0].goals).hasSize(1)
        soft.assertThat(directRetargetingCondition.rules[0].goals[0].id).isEqualTo(audienceGoals[0].id)
        soft.assertThat(directRetargetingCondition.rules[0].goals[0].type).isEqualTo(GoalType.AUDIENCE)
        soft.assertThat(directRetargetingCondition.rules[1].goals[0].id).isEqualTo(lalSegment.id)
        soft.assertThat(directRetargetingCondition.rules[1].goals[0].type).isEqualTo(GoalType.LAL_SEGMENT)

        val resultGet = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/retargeting/conditions?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        val responseGet = JsonUtils.MAPPER.readTree(resultGet)["result"]
        soft.assertThat(responseGet).hasSize(1)
        soft.assertThat(responseGet[0]["condition_rules"][1]["goals"][0]["id"].asLong()).isEqualTo(audienceGoals[1].id)
        soft.assertThat(responseGet[0]["condition_rules"][1]["goals"][0]["type"].asText()).isEqualTo("LAL_AUDIENCE")
        soft.assertAll()
    }

    @Test
    fun testCreate_WithMetrikaSegments() {
        val retargetingConditionRequest = RetargetingConditionRequest(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(
                            type = UacRetargetingConditionRuleGoalType.LAL_SEGMENT,
                            segmentInfo = UacSegmentInfo(
                                name = null,
                                counterId = 123,
                                domain = null,
                            ),
                            id = segment.id.toLong(),
                        ),
                    ),
                ),
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.NOT,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(
                            type = UacRetargetingConditionRuleGoalType.SEGMENT,
                            segmentInfo = UacSegmentInfo(
                                name = null,
                                counterId = 123,
                                domain = null,
                            ),
                            id = segment.id.toLong(),
                        ),
                    ),
                )
            ),
            name = "Условие ретаргетинга 1",
            mobileAppId = null,
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/retargeting/conditions?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(retargetingConditionRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val directRetargetingCondition = retargetingConditionService.getRetargetingConditions(
            userInfo.clientId, null,
            LimitOffset.maxLimited()
        )[0]
        val lalSegment = lalSegmentRepository.getLalSegmentsByParentIds(listOf(segment.id.toLong())).first()

        val retargetingCondition = UacRetargetingCondition(
            name = "Условие ретаргетинга 1",
            id = directRetargetingCondition.id,
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(
                            id = segment.id.toLong(),
                            name = null,
                            time = 540,
                            type = UacRetargetingConditionRuleGoalType.LAL_SEGMENT,
                            segmentInfo = UacSegmentInfo(
                                counterId = 123,
                                name = null,
                                domain = null,
                            )
                        ),
                    ),
                ),
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.NOT,
                    goals = listOf(
                        UacRetargetingConditionRuleGoal(
                            id = segment.id.toLong(),
                            name = null,
                            time = 540,
                            type = UacRetargetingConditionRuleGoalType.SEGMENT,
                            segmentInfo = UacSegmentInfo(
                                counterId = 123,
                                name = null,
                                domain = null,
                            )
                        ),
                    )
                )
            ),
        )

        val response = JsonUtils.MAPPER.readTree(result)["result"]
        val soft = SoftAssertions()
        soft.assertThat(response).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(listOf(retargetingCondition))
            )
        )
        soft.assertThat(directRetargetingCondition.rules).hasSize(2)
        soft.assertThat(directRetargetingCondition.rules[0].goals).hasSize(1)
        soft.assertThat(directRetargetingCondition.rules[0].goals[0].id).isEqualTo(lalSegment.id)
        soft.assertThat(directRetargetingCondition.rules[0].goals[0].type).isEqualTo(GoalType.LAL_SEGMENT)
        soft.assertThat(directRetargetingCondition.rules[1].goals[0].id).isEqualTo(segment.id.toLong())
        soft.assertThat(directRetargetingCondition.rules[1].goals[0].type).isEqualTo(GoalType.SEGMENT)

        val resultGet = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/retargeting/conditions?mobile_app_id=${mobileApp.id}&ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        val responseGet = JsonUtils.MAPPER.readTree(resultGet)["result"]
        soft.assertThat(responseGet).hasSize(1)
        soft.assertThat(responseGet[0]["condition_rules"][0]["goals"][0]["id"].asLong()).isEqualTo(segment.id.toLong())
        soft.assertThat(responseGet[0]["condition_rules"][0]["goals"][0]["type"].asText()).isEqualTo("LAL_SEGMENT")
        soft.assertAll()
    }

    @Test
    fun testCreateWithDuplicatedName() {
        val retargetingCondition1 = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(createUacRetargetingConditionRuleGoal(id = mobileAppGoalIds[1])),
                ),
            ),
            name = "Условие ретаргетинга 1"
        )
        val retargetingCondition2 = RetargetingConditionRequest(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.OR,
                    goals = listOf(createUacRetargetingConditionRuleGoal(id = mobileAppGoalIds[2])),
                ),
            ),
            name = "Условие ретаргетинга 1",
            mobileAppId = mobileApp.id,
        )
        val directRetargetingCondition1 = UacRetargetingService.toCoreRetargetingCondition(
            retargetingCondition1,
            userInfo.clientId.asLong(),
            type = ConditionType.metrika_goals
        )
        retargetingConditionService.addRetargetingConditions(listOf(directRetargetingCondition1), userInfo.clientId)
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/retargeting/conditions?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(retargetingCondition2))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString

        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )

        val soft = SoftAssertions()
        soft.assertThat(validationResult.errors).hasSize(1)
        soft.assertThat(validationResult.errors[0].path)
            .isEqualTo(path(index(0), field(UacRetargetingCondition::name.name)).toString())
        soft.assertThat(validationResult.errors[0].code)
            .isEqualTo(DefectIds.INCONSISTENT_STATE_ALREADY_EXISTS.code)
        soft.assertAll()
    }

    @Test
    fun testWithDuplicatedRules() {
        val retargetingCondition1 = UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(createUacRetargetingConditionRuleGoal(id = mobileAppGoalIds[1])),
                ),
            ),
            name = "Условие ретаргетинга 1"
        )
        val retargetingCondition2 = RetargetingConditionRequest(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(createUacRetargetingConditionRuleGoal(id = mobileAppGoalIds[1])),
                ),
            ),
            name = "Условие ретаргетинга 2",
            mobileAppId = mobileApp.id,
        )
        val directRetargetingCondition1 = UacRetargetingService.toCoreRetargetingCondition(
            retargetingCondition1,
            userInfo.clientId.asLong(),
            type = ConditionType.metrika_goals
        )
        retargetingConditionService.addRetargetingConditions(listOf(directRetargetingCondition1), userInfo.clientId)
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/retargeting/conditions?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(retargetingCondition2))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString

        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )

        val soft = SoftAssertions()
        soft.assertThat(validationResult.errors).hasSize(1)
        soft.assertThat(validationResult.errors[0].path)
            .isEqualTo(path(index(0), field(UacRetargetingCondition::conditionRules.name)).toString())
        soft.assertThat(validationResult.errors[0].code)
            .isEqualTo(DefectIds.INCONSISTENT_STATE_ALREADY_EXISTS.code)
        soft.assertAll()
    }

    @Test
    fun testCreateWithoutName() {
        val retargetingCondition = RetargetingConditionRequest(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(createUacRetargetingConditionRuleGoal(id = mobileAppGoalIds[1])),
                ),
            ),
            name = "",
            mobileAppId = mobileApp.id,
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/retargeting/conditions?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(retargetingCondition))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString

        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )

        val soft = SoftAssertions()
        soft.assertThat(validationResult.errors).hasSize(1)
        soft.assertThat(validationResult.errors[0].path)
            .isEqualTo(path(field(UacRetargetingCondition::name)).toString())
        soft.assertThat(validationResult.errors[0].code)
            .isEqualTo(StringDefectIds.CANNOT_BE_EMPTY.code)
        soft.assertAll()
    }

    @Test
    fun testCreateWithBadGoalId() {
        val retargetingCondition = RetargetingConditionRequest(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(createUacRetargetingConditionRuleGoal(id = 1)),
                ),
            ),
            name = "Условие ретаргетинга 1",
            mobileAppId = mobileApp.id,
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/retargeting/conditions?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(retargetingCondition))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString

        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )

        val soft = SoftAssertions()
        soft.assertThat(validationResult.errors).hasSize(1)
        soft.assertThat(validationResult.errors[0].path)
            .isEqualTo(
                path(
                    field(UacRetargetingCondition::conditionRules), index(0),
                    field(UacRetargetingConditionRule::goals), index(0),
                    field(UacRetargetingConditionRuleGoal::id)
                ).toString()
            )
        soft.assertThat(validationResult.errors[0].code)
            .isEqualTo(DefectIds.OBJECT_NOT_FOUND.code)
        soft.assertAll()
    }

    @Test
    fun testCreateWithTwoIncludeRules() {
        val retargetingConditionRequest = RetargetingConditionRequest(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(
                        createUacRetargetingConditionRuleGoal(id = mobileAppGoalIds[0]),
                    ),
                ),
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.OR,
                    goals = listOf(createUacRetargetingConditionRuleGoal(id = mobileAppGoalIds[8]))
                )
            ),
            name = "Условие ретаргетинга 1",
            mobileAppId = mobileApp.id,
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/retargeting/conditions?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(retargetingConditionRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )
        val soft = SoftAssertions()
        soft.assertThat(validationResult.errors).hasSize(1)
        soft.assertThat(validationResult.errors[0].path)
            .isEqualTo(path(field(UacRetargetingCondition::conditionRules)).toString())
        soft.assertThat(validationResult.errors[0].code)
            .isEqualTo(DefectIds.INVALID_VALUE.code)
        soft.assertAll()
    }

    @Test
    fun testCreateWithNullTime() {
        val retargetingConditionRequest = RetargetingConditionRequest(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(createUacRetargetingConditionRuleGoal(id = mobileAppGoalIds[0], time = null)),
                )
            ),
            name = "Условие ретаргетинга 1",
            mobileAppId = mobileApp.id,
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/retargeting/conditions?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(retargetingConditionRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )
        val soft = SoftAssertions()
        soft.assertThat(validationResult.errors).hasSize(1)
        soft.assertThat(validationResult.errors[0].path)
            .isEqualTo(
                path(
                    field(UacRetargetingCondition::conditionRules), index(0),
                    field(UacRetargetingConditionRule::goals), index(0),
                    field(UacRetargetingConditionRuleGoal::time)
                ).toString()
            )
        soft.assertThat(validationResult.errors[0].code)
            .isEqualTo(DefectIds.CANNOT_BE_NULL.code)
        soft.assertAll()
    }

    @Test
    fun testCreateWithNullType() {
        val retargetingConditionRequest = RetargetingConditionRequest(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    goals = listOf(createUacRetargetingConditionRuleGoal(id = mobileAppGoalIds[0], type = null)),
                )
            ),
            name = "Условие ретаргетинга 1",
            mobileAppId = mobileApp.id,
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/retargeting/conditions?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(retargetingConditionRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )
        val soft = SoftAssertions()
        soft.assertThat(validationResult.errors).hasSize(1)
        soft.assertThat(validationResult.errors[0].path)
            .isEqualTo(
                path(
                    field(UacRetargetingCondition::conditionRules), index(0),
                    field(UacRetargetingConditionRule::goals), index(0),
                    field(UacRetargetingConditionRuleGoal::type)
                ).toString()
            )
        soft.assertThat(validationResult.errors[0].code)
            .isEqualTo(DefectIds.CANNOT_BE_NULL.code)
        soft.assertAll()
    }

    @Test
    fun testCreateWithoutIncludeRules() {
        val retargetingConditionRequest = RetargetingConditionRequest(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.NOT,
                    goals = listOf(
                        createUacRetargetingConditionRuleGoal(id = mobileAppGoalIds[0]),
                    ),
                ),
            ),
            name = "Условие ретаргетинга 1",
            mobileAppId = mobileApp.id,
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/retargeting/conditions?ulogin=" + userInfo.clientInfo!!.login)
                .content(JsonUtils.toJson(retargetingConditionRequest))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(result)["validation_result"].toString(),
                WebValidationResult::class.java
            )
        val soft = SoftAssertions()
        soft.assertThat(validationResult.errors).hasSize(1)
        soft.assertThat(validationResult.errors[0].path)
            .isEqualTo(path(field(UacRetargetingCondition::conditionRules)).toString())
        soft.assertThat(validationResult.errors[0].code)
            .isEqualTo(DefectIds.INVALID_VALUE.code)
        soft.assertAll()
    }
}
