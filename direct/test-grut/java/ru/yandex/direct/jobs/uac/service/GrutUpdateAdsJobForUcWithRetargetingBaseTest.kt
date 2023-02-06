package ru.yandex.direct.jobs.uac.service

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.params.provider.Arguments
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalScope
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType.short_term
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.retargeting.model.RuleType
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingGoalsRepository
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRule
import ru.yandex.direct.core.entity.uac.model.UacRetargetingConditionRuleGoal
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestCampaignRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageCpaStrategy
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName.NEW_CUSTOM_AUDIENCE_ENABLED
import ru.yandex.direct.feature.FeatureName.UAC_MULTIPLE_AD_GROUPS_ENABLED
import ru.yandex.direct.feature.FeatureName.UC_UAC_CREATE_BRIEF_IN_GRUT_INSTEAD_OF_YDB
import ru.yandex.direct.jobs.uac.UpdateAdsJob
import ru.yandex.direct.multitype.entity.LimitOffset

abstract class GrutUpdateAdsJobForUcWithRetargetingBaseTest {

    companion object {
        const val RETARGETING_CONDITION_NAME = "Интересы и привычки"

        val GOAL_INTEREST = Goal()
            .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND + 100_000L)
            as Goal

        val GOAL_HOST = Goal()
            .withId(Goal.HOST_LOWER_BOUND)
            as Goal
    }

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var updateAdsJob: UpdateAdsJob

    @Autowired
    lateinit var adGroupService: AdGroupService

    @Autowired
    lateinit var grutSteps: GrutSteps

    @Autowired
    lateinit var grutUacCampaignService: GrutUacCampaignService

    @Autowired
    lateinit var grutApiService: GrutApiService

    @Autowired
    lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    lateinit var retargetingRepository: RetargetingRepository

    @Autowired
    lateinit var retargetingConditionRepository: RetargetingConditionRepository

    @Autowired
    lateinit var retargetingGoalsRepository: RetargetingGoalsRepository

    @Autowired
    lateinit var testCampaignRepository: TestCampaignRepository

    lateinit var clientInfo: ClientInfo
    lateinit var clientId: ClientId
    lateinit var uacCampaignId: String
    var shard: Int = 0
    var uid: Long = 0L
    var ucCampaignId = 0L

    fun init(
        groupBriefEnabled: Boolean = false,
    ) {
        clientInfo = steps.clientSteps().createDefaultClient()
        shard = clientInfo.shard
        clientId = clientInfo.clientId!!
        uid = clientInfo.chiefUserInfo!!.uid
        grutSteps.createClient(clientInfo)

        steps.cryptaGoalsSteps().addGoals(listOf(GOAL_INTEREST), setOf(CryptaGoalScope.PERFORMANCE))

        steps.featureSteps().addClientFeature(clientId, UC_UAC_CREATE_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true)
        steps.featureSteps().addClientFeature(clientId, NEW_CUSTOM_AUDIENCE_ENABLED, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, UAC_MULTIPLE_AD_GROUPS_ENABLED, groupBriefEnabled)

        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withStrategy(
                AverageCpaStrategy()
                    .withMaxWeekSum(1000.toBigDecimal())
                    .withAverageCpa(100.toBigDecimal())
            )
        ucCampaignId = steps.campaignSteps().createCampaign(campaign, clientInfo).campaignId

        ppcPropertiesSupport.get(PpcPropertyNames.IS_GRUT_ENABLED).set(true)
    }

    @AfterEach
    fun after() {
        ppcPropertiesSupport.get(PpcPropertyNames.IS_GRUT_ENABLED).set(false)
        ppcPropertiesSupport.remove(PpcPropertyNames.MAX_BANNERS_IN_UAC_TEXT_AD_GROUP)
    }

    @Suppress("unused")
    fun casesForCustomAudience() = listOf(
        Arguments.of("Интерес", true, listOf(GOAL_INTEREST)),
        Arguments.of("Сайт", true, listOf(GOAL_HOST)),
        Arguments.of("Интерес и сайт", true, listOf(GOAL_INTEREST, GOAL_HOST)),
        Arguments.of("Без ретаргетинга", true, null),

        Arguments.of("Интерес", false, listOf(GOAL_INTEREST)),
        Arguments.of("Сайт", false, listOf(GOAL_HOST)),
        Arguments.of("Интерес и сайт", false, listOf(GOAL_INTEREST, GOAL_HOST)),
        Arguments.of("Без ретаргетинга", false, null),
    )

    fun checkRetargeting(
        expectGoals: List<Goal>?,
    ) {
        val soft = SoftAssertions()
        val adGroups = adGroupService.getAdGroupsBySelectionCriteria(
            AdGroupsSelectionCriteria()
                .withCampaignIds(setOf(ucCampaignId)), LimitOffset.maxLimited(), false
        )
        soft.assertThat(adGroups)
            .hasSize(1)

        // Проверка bid_retargetings
        val retargetings = retargetingRepository.getRetargetingsByAdGroups(shard, adGroups.map { it.id })

        if (expectGoals == null) {
            soft.assertThat(retargetings)
                .isNullOrEmpty()
        } else {
            soft.assertThat(retargetings)
                .hasSize(1)

            // Проверка retargeting_conditions
            val retargetingConditions = retargetingConditionRepository
                .getConditions(shard, retargetings.map { it.retargetingConditionId })
            soft.assertThat(retargetingConditions).hasSize(1)
            soft.assertThat(retargetingConditions[0].name).isEqualTo(RETARGETING_CONDITION_NAME)
            soft.assertThat(retargetingConditions[0].type).isEqualTo(ConditionType.interests)
            soft.assertThat(retargetingConditions[0].rules).hasSize(1)
            soft.assertThat(retargetingConditions[0].rules[0].type).isEqualTo(RuleType.OR)
            soft.assertThat(retargetingConditions[0].rules[0].interestType).isEqualTo(short_term)
            soft.assertThat(retargetingConditions[0].rules[0].goals).hasSize(expectGoals.size)

            val goalTypeToTime = retargetingConditions[0].rules[0].goals
                .groupBy({ it.type }, { it.time })
            val expectGoalTypeToTime = expectGoals
                .groupBy(
                    { if (it.id == GOAL_INTEREST.id) GoalType.INTERESTS else GoalType.HOST },
                    { if (it.id == GOAL_INTEREST.id) 0 else 540 }
                )
            soft.assertThat(goalTypeToTime).isEqualTo(expectGoalTypeToTime)

            // Проверка retargeting_goals
            val retargetingGoals = retargetingGoalsRepository
                .getGoalIds(shard, retargetings[0].retargetingConditionId)
                .toSet()
            val expectGoalIds = retargetingConditions[0].rules[0].goals
                .map { it.id }
                .toSet()
            soft.assertThat(retargetingGoals).isEqualTo(expectGoalIds)
        }
        soft.assertAll()
    }

    fun getUacRetargetingCondition(
        goals: List<Goal>?
    ) = if (goals == null) null else UacRetargetingCondition(
        name = RETARGETING_CONDITION_NAME,
        conditionRules = listOf(
            UacRetargetingConditionRule(
                type = UacRetargetingConditionRule.RuleType.OR,
                interestType = short_term,
                goals = goals.map { UacRetargetingConditionRuleGoal(id = it.id) }
            ),
        ),
    )
}
