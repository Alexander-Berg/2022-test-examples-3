package ru.yandex.direct.web.entity.uac.controller

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalScope
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefectIds.Gen.INTERESTS_TYPE_IS_NOT_SPECIFIED
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.checkNotEmpty
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.validation.result.DefectId
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.createUcCampaignRequest
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest
import ru.yandex.direct.web.validation.model.WebValidationResult

@GrutDirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignGrutCreateControllerUcRetargetingTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()

        private val GOAL_INTEREST = Goal()
            .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND + 100_000L)
            as Goal

        private val WRONG_GOAL_INTEREST = Goal()
            .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND + 10L)
            as Goal

        private val GOAL_HOST = Goal()
            .withId(Goal.HOST_LOWER_BOUND)
            as Goal

        private val WRONG_GOAL = Goal()
            .withId(Goal.HOST_LOWER_BOUND + 10L)
            as Goal

        private val INVALID_RETARGETING_CONDITIONL_PATH = path(field("retargetingCondition"))
        private val INVALID_GOAL_ID_PATH = path(field("retargetingCondition"), field("rules"),
            index(0), field("goals"), index(0), field("id"))
        private val INVALID_GOALS_PATH = path(field("retargetingCondition"), field("rules"),
            index(0), field("goals"))
    }


    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var grutUacCampaignService: GrutUacCampaignService

    private lateinit var mockMvc: MockMvc

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        clientInfo = testAuthHelper.createDefaultUser().clientInfo!!
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )

        steps.cryptaGoalsSteps().addGoals(listOf(GOAL_INTEREST), setOf(CryptaGoalScope.PERFORMANCE))

        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.UC_UAC_CREATE_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.NEW_CUSTOM_AUDIENCE_ENABLED, true)
        // костыль, чтобы skipGoalExistenceCheck == false
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UNIVERSAL_CAMPAIGNS_BETA_DISABLED, true)

        grutSteps.createClient(clientInfo)
    }

    private fun casesForCustomAudience(): List<List<Any?>> {
        return listOf(
            listOf("Интерес", true, listOf(GOAL_INTEREST), 200, null, null),
            listOf("Сайт", true, listOf(GOAL_HOST), 200, null, null),
            listOf("Интерес и сайт", true, listOf(GOAL_INTEREST, GOAL_HOST), 200, null, null),
            listOf("Не существующая цель интереса", true, listOf(WRONG_GOAL_INTEREST), 400, INVALID_GOAL_ID_PATH, DefectIds.OBJECT_NOT_FOUND),
            listOf("Не существующая цель", true, listOf(WRONG_GOAL), 400, INVALID_GOAL_ID_PATH, DefectIds.OBJECT_NOT_FOUND),
            listOf("Без ретаргетинга", true, null, 200, null, null),

            listOf("Интерес", false, listOf(GOAL_INTEREST), 400, INVALID_RETARGETING_CONDITIONL_PATH, DefectIds.MUST_BE_NULL),
            listOf("Сайт", false, listOf(GOAL_HOST), 400, INVALID_RETARGETING_CONDITIONL_PATH, DefectIds.MUST_BE_NULL),
            listOf("Интерес и сайт", false, listOf(GOAL_INTEREST, GOAL_HOST), 400, INVALID_RETARGETING_CONDITIONL_PATH, DefectIds.MUST_BE_NULL),
            listOf("Не существующая цель интереса", false, listOf(WRONG_GOAL_INTEREST), 400, INVALID_RETARGETING_CONDITIONL_PATH, DefectIds.MUST_BE_NULL),
            listOf("Не существующая цель", false, listOf(WRONG_GOAL), 400, INVALID_RETARGETING_CONDITIONL_PATH, DefectIds.MUST_BE_NULL),
            listOf("Без ретаргетинга", false, null, 200, null, null),
        )
    }

    /**
     * Проверка разных кейсов отправки ретаргетинга с custom audience
     */
    @Test
    @TestCaseName("Feature {1}: {0} -> expect {3} status")
    @Parameters(method = "casesForCustomAudience")
    fun createUcCampaignTest(
        description: String,
        featureEnabled: Boolean,
        goals: List<Goal>?,
        expectResultStatus: Int,
        expectDefectPath: Path?,
        expectDefectId: DefectId<*>?,
    ) {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UC_CUSTOM_AUDIENCE_ENABLED, featureEnabled)

        val retargetingCondition = if (goals == null) null else UacRetargetingCondition(
            name = "Интересы и привычки",
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.OR,
                    interestType = CryptaInterestType.short_term,
                    goals = goals.map { UacRetargetingConditionRuleGoal(id = it.id) }
                ),
            ),
        )

        val request = createUcCampaignRequest(retargetingCondition)
        if (expectDefectPath == null || expectDefectId == null) {
            sendAndCheckSuccessRequest(request, retargetingCondition)
        } else {
            sendAndCheckBadRequest(request, expectResultStatus, expectDefectPath, expectDefectId)
        }
    }

    /**
     * При отправке ретаргетинга с правилом без типа интереса - получаем ошибку
     */
    @Test
    fun createUcCampaign_RetargetingWithoutInterestType() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UC_CUSTOM_AUDIENCE_ENABLED, true)

        val retargetingCondition = UacRetargetingCondition(
            name = "Интересы и привычки",
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.OR,
                    goals = listOf(UacRetargetingConditionRuleGoal(id = GOAL_INTEREST.id))
                ),
            ),
        )

        val request = createUcCampaignRequest(retargetingCondition)
        sendAndCheckBadRequest(request, 400, INVALID_GOALS_PATH, INTERESTS_TYPE_IS_NOT_SPECIFIED)
    }

    private fun sendAndCheckSuccessRequest(
        request: CreateCampaignRequest,
        expectRetargetingCondition: UacRetargetingCondition?,
    ) {
        val result = sendAndCheckRequest(request, 201)
        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val campaignId = resultJsonTree["result"]["id"].asText()

        val ucCampaign = grutUacCampaignService.getCampaignById(campaignId)

        assertThat(ucCampaign!!.retargetingCondition)
            .`as`("Ретаргетинг")
            .isEqualTo(expectRetargetingCondition)
    }

    private fun sendAndCheckBadRequest(
        request: CreateCampaignRequest,
        expectResultStatus: Int,
        path: Path,
        defectId: DefectId<*>,
    ) {
        val result = sendAndCheckRequest(request, expectResultStatus)
        val validationResult = JsonUtils
            .fromJson(JsonUtils.fromJson(result)["validation_result"].toString(), WebValidationResult::class.java)
        validationResult.errors.checkNotEmpty()

        val pathToDefect = validationResult.errors
            .associateBy({ it.path }, { it.code })
        assertThat(pathToDefect)
            .containsEntry(path.toString(), defectId.code)
    }

    private fun sendAndCheckRequest(
        request: CreateCampaignRequest,
        expectResultStatus: Int,
    ) = mockMvc.perform(
        MockMvcRequestBuilders
            .post("/uac/campaigns?ulogin=" + clientInfo.login)
            .content(JsonUtils.toJson(request))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().`is`(expectResultStatus))
        .andReturn()
        .response
        .contentAsString
}
