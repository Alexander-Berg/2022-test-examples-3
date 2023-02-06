package ru.yandex.direct.core.entity.metrika.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoals
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions
import ru.yandex.direct.core.entity.campaign.service.CampaignService
import ru.yandex.direct.core.entity.metrika.model.GoalCampUsages
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.TestUtils.assumeThat
import ru.yandex.direct.testing.matchers.result.MassResultMatcher
import java.math.BigDecimal


@CoreTest
@ExtendWith(SpringExtension::class)
class MetrikaCampaignRepositoryUsedGoalsTest {

    @Autowired
    lateinit var testedRepository: MetrikaCampaignRepository

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var campaignService: CampaignService

    @Autowired
    lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    lateinit var campaignOperationService: CampaignOperationService


    lateinit var clientInfo: ClientInfo

    @BeforeEach
    internal fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        steps.featureSteps().setCurrentClient(clientInfo.clientId)
    }

    @Test
    internal fun getGoalsUsedInCampaignsByClientId_returnMeaningfulGoals() {
        val campaign = steps.campaignSteps().createActiveCampaign(clientInfo)

        // добавляем ключевые цели к кампании, чтобы они вернулись в тестируемом getGoalsUsedInCampaignsByClientId
        val meaningfulGoalId1 = 1L
        val meaningfulGoalId2 = 2L
        val meaningfulGoals = listOf(
            MeaningfulGoal().apply {
                goalId = meaningfulGoalId1
                conversionValue = BigDecimal.ONE
            },
            MeaningfulGoal().apply {
                goalId = meaningfulGoalId2
                conversionValue = BigDecimal.TEN
            }
        )
        val campaignModelChanges = ModelChanges(campaign.campaignId, TextCampaign::class.java)
            .process(meaningfulGoals, CampaignWithMeaningfulGoals.MEANINGFUL_GOALS)
        applyModelChangesToTextCampaign(campaignModelChanges)

        // проверка начинается тут
        val usedGoals = testedRepository.getGoalsUsedInCampaignsByClientId(clientInfo.shard, clientInfo.clientId)
        assertThat(usedGoals).containsExactlyEntriesOf(
            mapOf(
                meaningfulGoalId1 to GoalCampUsages(isMeaningful = true, isUsedInCampaignStrategy = false),
                meaningfulGoalId2 to GoalCampUsages(isMeaningful = true, isUsedInCampaignStrategy = false),
            )
        )
    }

    @Test
    internal fun getGoalsUsedInCampaignsByClientId_returnStrategyGoals() {
        val goalId = 3L
        val campaign = TestCampaigns.activeTextCampaign(clientInfo.clientId, clientInfo.uid)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
            .withStrategy(averageCpaStrategy().withGoalId(goalId))
        steps.campaignSteps().createCampaign(campaign, clientInfo)

        val usedGoals = testedRepository.getGoalsUsedInCampaignsByClientId(clientInfo.shard, clientInfo.clientId)
        assertThat(usedGoals).containsExactlyEntriesOf(
            mapOf(
                goalId to GoalCampUsages(isMeaningful = false, isUsedInCampaignStrategy = true),
            )
        )
    }

    @Test
    internal fun getGoalsUsedInCampaignsByClientId_returnStrategyAndMeaningfulGoals() {
        val meaningfulGoalId1 = 1L
        val meaningfulGoalForStrategyId = 2L

        val campaign = TestCampaigns.activeTextCampaign(clientInfo.clientId, clientInfo.uid)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
            .withStrategy(averageCpaStrategy().withGoalId(meaningfulGoalForStrategyId))
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        val meaningfulGoals = listOf(
            MeaningfulGoal().apply {
                goalId = meaningfulGoalId1
                conversionValue = BigDecimal.ONE
            },
            MeaningfulGoal().apply {
                goalId = meaningfulGoalForStrategyId
                conversionValue = BigDecimal.TEN
            }
        )
        val campaignModelChanges = ModelChanges(campaignInfo.campaignId, TextCampaign::class.java)
            .process(meaningfulGoals, CampaignWithMeaningfulGoals.MEANINGFUL_GOALS)
        applyModelChangesToTextCampaign(campaignModelChanges)

        // проверка начинается тут
        val usedGoals = testedRepository.getGoalsUsedInCampaignsByClientId(clientInfo.shard, clientInfo.clientId)
        assertThat(usedGoals).containsExactlyEntriesOf(
            mapOf(
                meaningfulGoalId1 to GoalCampUsages(isMeaningful = true, isUsedInCampaignStrategy = false),
                meaningfulGoalForStrategyId to GoalCampUsages(isMeaningful = true, isUsedInCampaignStrategy = true),
            )
        )
    }

    private fun applyModelChangesToTextCampaign(campaignModelChanges: ModelChanges<TextCampaign>) {
        val textCampaign =
            campaignTypedRepository.getTypedCampaigns(clientInfo.shard, listOf(campaignModelChanges.id))
                .first() as TextCampaign

        val updateOperation = campaignOperationService.createRestrictedCampaignUpdateOperation(
            listOf(campaignModelChanges),
            textCampaign.uid,
            UidAndClientId.of(textCampaign.uid, ClientId.fromLong(textCampaign.clientId)),
            CampaignOptions()
        )
        val result = updateOperation.apply()
        assumeThat(result, MassResultMatcher.isFullySuccessful())
    }
}
