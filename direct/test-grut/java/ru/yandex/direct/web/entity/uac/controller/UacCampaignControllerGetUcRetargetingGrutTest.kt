package ru.yandex.direct.web.entity.uac.controller

import ru.yandex.direct.core.entity.retargeting.model.Rule as RetargetingConditionRule
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
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
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition
import ru.yandex.direct.core.entity.retargeting.model.RuleType
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoalType
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoalType.HOST
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoalType.INTERESTS
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestRetargetingConditions
import ru.yandex.direct.core.testing.data.TestRetargetings
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.CampaignSteps
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.crypta.client.impl.CryptaClientStub
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.utils.fromJson
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@GrutDirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignControllerGetUcRetargetingGrutTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()

        private const val TRANSLATED_INTEREST_NAME = "Kitchen hoods"
        private const val TRANSLATED_INTEREST_DESCRIPTION = "People interested in kitchen extraction fans."

        private val GOAL_INTEREST = Goal()
            .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND + 100_000L)
            .withType(GoalType.INTERESTS)
            .withName("interest name")
            .withTime(0)
            .withTankerNameKey("crypta_interest_kitchen_hoods_name")
            .withTankerDescriptionKey("crypta_interest_kitchen_hoods_description")
            as Goal

        private val NOT_EXIST_GOAL_INTEREST = Goal()
            .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND + 100_001L)
            .withType(GoalType.INTERESTS)
            .withName("not exist interest name")
            .withTime(0)
            .withTankerNameKey("not exist interest tanker name")
            .withTankerDescriptionKey("not exist interest tanker description")
            as Goal

        private val GOAL_HOST = Goal()
            .withId(Goal.HOST_LOWER_BOUND)
            .withType(GoalType.HOST)
            .withName("host name")
            .withTime(540)
            .withTankerNameKey("host tanker name")
            .withTankerDescriptionKey("host tanker description")
            as Goal

        private val NOT_EXIST_GOAL_HOST = Goal()
            .withId(Goal.HOST_LOWER_BOUND + 1)
            .withType(GoalType.HOST)
            .withName("not exist host name")
            .withTime(540)
            .withTankerNameKey("not exist host tanker name")
            .withTankerDescriptionKey("not exist host tanker description")
            as Goal

        private val INTERESTS_GOALS = setOf(GOAL_INTEREST, NOT_EXIST_GOAL_INTEREST)
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var campaignSteps: CampaignSteps

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var cryptaClient: CryptaClientStub

    @Autowired
    private lateinit var mockMvc: MockMvc

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = testAuthHelper.createDefaultUser().clientInfo!!
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        grutSteps.createClient(clientInfo)

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.NEW_CUSTOM_AUDIENCE_ENABLED, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UC_CUSTOM_AUDIENCE_ENABLED, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UNIVERSAL_CAMPAIGNS_BETA_DISABLED, true)


        steps.cryptaGoalsSteps().addGoals(GOAL_INTEREST)
        cryptaClient.addHosts(listOf(GOAL_HOST.id))
    }

    fun casesForCustomAudienceRetargeting(): List<List<Any?>> {
        return listOf(
            listOf("Interest goal", listOf(GOAL_INTEREST),
                listOf(TRANSLATED_INTEREST_NAME),
                listOf(TRANSLATED_INTEREST_DESCRIPTION)
            ),
            listOf("Host goal", listOf(GOAL_HOST),
                listOf("yandex${GOAL_HOST.id}.ru"), listOf(GOAL_HOST.tankerDescriptionKey)),
            listOf("Host and interest goals", listOf(GOAL_HOST, GOAL_INTEREST),
                listOf("yandex${GOAL_HOST.id}.ru", TRANSLATED_INTEREST_NAME),
                listOf(GOAL_HOST.tankerDescriptionKey, TRANSLATED_INTEREST_DESCRIPTION)),

            listOf("Wrong interest goal", listOf(NOT_EXIST_GOAL_INTEREST),
                listOf(NOT_EXIST_GOAL_INTEREST.name), listOf(NOT_EXIST_GOAL_INTEREST.tankerDescriptionKey)),
            listOf("Wrong host goal", listOf(NOT_EXIST_GOAL_HOST),
                listOf(NOT_EXIST_GOAL_HOST.name), listOf(NOT_EXIST_GOAL_HOST.tankerDescriptionKey)),
            listOf("Wrong host and interest goals", listOf(NOT_EXIST_GOAL_HOST, NOT_EXIST_GOAL_INTEREST),
                listOf(NOT_EXIST_GOAL_HOST.name, NOT_EXIST_GOAL_INTEREST.name),
                listOf(NOT_EXIST_GOAL_HOST.tankerDescriptionKey, NOT_EXIST_GOAL_INTEREST.tankerDescriptionKey)),
        )
    }

    /**
     * Проверяем получение custom audience в зависимости от сохраненных данных в заявке,
     * когда уже есть группа с ретаргетингом
     */
    @Test
    @TestCaseName("{0}")
    @Parameters(method = "casesForCustomAudienceRetargeting")
    fun getCustomAudienceRetargetings(
        description: String,
        goals: List<Goal>,
        expectNames: List<String>,
        expectDescriptions: List<String>,
    ) {
        val ucRetargetingCondition = createUcRetargetingCondition(goals)
        val campaignInfo = createUcCampaign(ucRetargetingCondition)
        val retargetingCondition = createAdGroupWithRetargetingCondition(campaignInfo)

        val receivedRetargetingCondition = sendRequestAndGetRetargetingCondition(campaignInfo.campaignId)

        val expectRetargetingCondition = expectUcRetargetingCondition(
            goals, expectNames, expectDescriptions, retargetingCondition.id, retargetingCondition.name)

        assertThat(receivedRetargetingCondition)
            .`as`("Custom audience retargeting condition")
            .isEqualTo(expectRetargetingCondition)
    }

    fun casesForCustomAudienceRetargetingWithoutGroup(): List<List<Any?>> {
        return listOf(
            listOf("Interest goal", listOf(GOAL_INTEREST), null, "Условие нацеливания",
                listOf(TRANSLATED_INTEREST_NAME), listOf(TRANSLATED_INTEREST_DESCRIPTION)),
            listOf("Host goal", listOf(GOAL_HOST), 5L, null,
                listOf("yandex${GOAL_HOST.id}.ru"), listOf(GOAL_HOST.tankerDescriptionKey)),
            listOf("Host and interest goals", listOf(GOAL_HOST, GOAL_INTEREST), null, null,
                listOf("yandex${GOAL_HOST.id}.ru", TRANSLATED_INTEREST_NAME),
                listOf(GOAL_HOST.tankerDescriptionKey, TRANSLATED_INTEREST_DESCRIPTION)),

            listOf("Wrong interest goal", listOf(NOT_EXIST_GOAL_INTEREST), 15L, "Условие нацеливания",
                listOf(NOT_EXIST_GOAL_INTEREST.name), listOf(NOT_EXIST_GOAL_INTEREST.tankerDescriptionKey)),
            listOf("Wrong host goal", listOf(NOT_EXIST_GOAL_HOST), null, null,
                listOf(NOT_EXIST_GOAL_HOST.name), listOf(NOT_EXIST_GOAL_HOST.tankerDescriptionKey)),
            listOf("Wrong host and interest goals", listOf(NOT_EXIST_GOAL_HOST, NOT_EXIST_GOAL_INTEREST), null, "Условие нацеливания",
                listOf(NOT_EXIST_GOAL_HOST.name, NOT_EXIST_GOAL_INTEREST.name),
                listOf(NOT_EXIST_GOAL_HOST.tankerDescriptionKey, NOT_EXIST_GOAL_INTEREST.tankerDescriptionKey)),
        )
    }

    /**
     * Проверяем получение custom audience в зависимости от сохраненных данных в заявке, когда еще нет группы
     */
    @Test
    @TestCaseName("{0}")
    @Parameters(method = "casesForCustomAudienceRetargetingWithoutGroup")
    fun getCustomAudienceRetargetings_WithoutGroup(
        description: String,
        goals: List<Goal>,
        ucRetConditionId: Long?,
        ucRetConditionName: String?,
        expectNames: List<String>,
        expectDescriptions: List<String>,
    ) {
        val ucRetargetingCondition = createUcRetargetingCondition(goals, ucRetConditionId, ucRetConditionName)
        val campaignInfo = createUcCampaign(ucRetargetingCondition)

        val receivedRetargetingCondition = sendRequestAndGetRetargetingCondition(campaignInfo.campaignId)

        val expectRetargetingCondition =
            expectUcRetargetingCondition(goals, expectNames, expectDescriptions, ucRetConditionId, ucRetConditionName)

        assertThat(receivedRetargetingCondition)
            .`as`("Custom audience")
            .isEqualTo(expectRetargetingCondition)
    }

    fun casesForCustomAudienceRetargetingWithoutFeature(): List<List<Any?>> {
        return listOf(
            listOf("Interest goal", listOf(GOAL_INTEREST), null, "Условие нацеливания",
                listOf(GOAL_INTEREST.name), listOf(GOAL_INTEREST.tankerDescriptionKey)),
            listOf("Host goal", listOf(GOAL_HOST), 555L, null,
                listOf(GOAL_HOST.name), listOf(GOAL_HOST.tankerDescriptionKey)),
            listOf("Host and interest goals", listOf(GOAL_HOST, GOAL_INTEREST), 556L, "Условие нацеливания",
                listOf(GOAL_HOST.name, GOAL_INTEREST.name),
                listOf(GOAL_HOST.tankerDescriptionKey, GOAL_INTEREST.tankerDescriptionKey)),

            listOf("Wrong interest goal", listOf(NOT_EXIST_GOAL_INTEREST), 557L, null,
                listOf(NOT_EXIST_GOAL_INTEREST.name), listOf(NOT_EXIST_GOAL_INTEREST.tankerDescriptionKey)),
            listOf("Wrong host goal", listOf(NOT_EXIST_GOAL_HOST), 558L, "Условие нацеливания",
                listOf(NOT_EXIST_GOAL_HOST.name), listOf(NOT_EXIST_GOAL_HOST.tankerDescriptionKey)),
            listOf("Wrong host and interest goals", listOf(NOT_EXIST_GOAL_HOST, NOT_EXIST_GOAL_INTEREST),
                null, "Условие нацеливания",
                listOf(NOT_EXIST_GOAL_HOST.name, NOT_EXIST_GOAL_INTEREST.name),
                listOf(NOT_EXIST_GOAL_HOST.tankerDescriptionKey, NOT_EXIST_GOAL_INTEREST.tankerDescriptionKey)),
        )
    }

    /**
     * Проверяем получение custom audience в зависимости от сохраненных данных в заявке,
     * когда уже есть группа с ретаргетингом и выклюена фича
     */
    @Test
    @TestCaseName("{0}")
    @Parameters(method = "casesForCustomAudienceRetargetingWithoutFeature")
    fun getCustomAudienceRetargetings_WithoutFeature(
        description: String,
        goals: List<Goal>,
        ucRetConditionId: Long?,
        ucRetConditionName: String?,
        expectNames: List<String>,
        expectDescriptions: List<String>,
    ) {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UC_CUSTOM_AUDIENCE_ENABLED, false)

        val ucRetargetingCondition = createUcRetargetingCondition(goals, ucRetConditionId, ucRetConditionName)
        val campaignInfo = createUcCampaign(ucRetargetingCondition)
        createAdGroupWithRetargetingCondition(campaignInfo)

        val receivedRetargetingCondition = sendRequestAndGetRetargetingCondition(campaignInfo.campaignId)

        val expectRetargetingCondition = expectUcRetargetingCondition(
            goals, expectNames, expectDescriptions, ucRetConditionId, ucRetConditionName)

        assertThat(receivedRetargetingCondition)
            .`as`("Custom audience retargeting condition")
            .isEqualTo(expectRetargetingCondition)
    }

    private fun expectUcRetargetingCondition(
        goals: List<Goal>,
        expectGoalNames: List<String?>,
        expectGoalDescriptions: List<String?>,
        expectRetConditionId: Long?,
        expectRetConditionName: String?,
    ): UacRetargetingCondition {
        val expectGoals = goals
            .mapIndexed { i, goal ->
                val expectType = if (INTERESTS_GOALS.contains(goal)) INTERESTS else HOST
                createRetargetingConditionGoal(
                    goal,
                    name = expectGoalNames[i],
                    description = expectGoalDescriptions[i],
                    type = expectType
                )
            }

        return UacRetargetingCondition(
            conditionRules = listOf(
                UacRetargetingConditionRule(
                    type = UacRetargetingConditionRule.RuleType.ALL,
                    interestType = CryptaInterestType.short_term,
                    goals = expectGoals,
                ),
            ),
            name = expectRetConditionName,
            id = expectRetConditionId,
        )
    }

    private fun createUcRetargetingCondition(
        goals: List<Goal>,
        retConditionId: Long? = null,
        retConditionName: String? = null,
    ): UacRetargetingCondition {
        val rule = createRetargetingConditionRule(goals)
        return UacRetargetingCondition(
            conditionRules = listOf(rule),
            name = retConditionName,
            id = retConditionId,
        )
    }

    private fun createRetargetingConditionRule(
        goals: List<Goal>
    ): UacRetargetingConditionRule {
        val ucGoals = goals
            .map { createRetargetingConditionGoal(it) }
        return UacRetargetingConditionRule(
            type = UacRetargetingConditionRule.RuleType.ALL,
            interestType = CryptaInterestType.short_term,
            goals = ucGoals,
        )
    }

    private fun createRetargetingConditionGoal(
        goal: Goal,
        name: String? = goal.name,
        description: String? = goal.tankerDescriptionKey,
        type: UacRetargetingConditionRuleGoalType? = if (INTERESTS_GOALS.contains(goal)) INTERESTS else HOST,
    ) = UacRetargetingConditionRuleGoal(
        id = goal.id,
        name = name,
        description = description,
        type = type,
        time = goal.time,
    )

    private fun createUcCampaign(uacRetargetingCondition: UacRetargetingCondition?): CampaignInfo {
        val directCampaign = TestCampaigns.activeTextCampaign(clientInfo.clientId, clientInfo.uid)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
            .withStrategy(TestCampaigns.averageCpaStrategy())
            .withSource(CampaignSource.UAC)
        val campaignInfo = campaignSteps.createCampaign(directCampaign, clientInfo)
        val ucCampaign = createYdbCampaign(
            id = campaignInfo.campaignId.toIdString(),
            retargetingCondition = uacRetargetingCondition,
            targetStatus = TargetStatus.STARTED,
            accountId = clientInfo.clientId!!.toString(),
            startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
        )
        grutSteps.createTextCampaign(clientInfo, ucCampaign)

        return campaignInfo
    }

    private fun createAdGroupWithRetargetingCondition(campaignInfo: CampaignInfo): RetargetingCondition {
        val rule = RetargetingConditionRule()
            .withGoals(listOf(GOAL_INTEREST, GOAL_HOST))
            .withType(RuleType.OR)
        val retCondition = TestRetargetingConditions.defaultRetCondition(clientInfo.clientId)
            .withType(ConditionType.interests)
            .withName("Interest Retargeting Condition Name")
            .withRules(listOf(rule))
            .withAvailable(true) as RetargetingCondition

        val retargetingConditionInfo = steps.retConditionSteps().createRetCondition(retCondition, clientInfo)

        val adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo)
        val retargeting = TestRetargetings.defaultRetargeting(campaignInfo.campaignId, adGroupInfo.adGroupId,
            retargetingConditionInfo.retConditionId)
        steps.retargetingSteps().createRetargeting(retargeting, adGroupInfo, retargetingConditionInfo)
        return retCondition
    }

    private fun sendRequestAndGetRetargetingCondition(ucCampaignId: Long): UacRetargetingCondition? {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/${ucCampaignId}?ulogin=${clientInfo.login}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        return resultJsonTree["result"]["retargeting_condition"]?.let { fromJson(it.toString()) }
    }
}

