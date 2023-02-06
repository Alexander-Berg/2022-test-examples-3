package ru.yandex.direct.core.entity.retargeting.service.validation2

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalScope
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestRetargetingConditions
import ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRule
import ru.yandex.direct.core.testing.data.TestRetargetings
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.result.DefectId
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.ValidationResult

@CoreTest
@RunWith(SpringRunner::class)
class UpdateRetargetingValidationServiceForCustomAudienceTest {

    companion object {
        private val GOAL_INTEREST_COMMON = Goal()
            .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND - 2L)
            .withCryptaScope(setOf(CryptaGoalScope.COMMON)) as Goal
        private val GOAL_INTEREST_1 = Goal()
            .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND)
            .withCryptaScope(setOf(CryptaGoalScope.PERFORMANCE)) as Goal
        private val GOAL_INTEREST_2 = Goal()
            .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND + 1L)
            .withCryptaScope(setOf(CryptaGoalScope.PERFORMANCE)) as Goal
        private val GOAL_HOST = Goal()
            .withId(Goal.HOST_LOWER_BOUND) as Goal
        private val INVALID_GOAL_HOST = Goal()
            .withId(Goal.HOST_LOWER_BOUND + 10L) as Goal
        private val INVALID_GOAL_INTEREST = Goal()
            .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND + 1000L) as Goal
    }

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var updateRetargetingConditionService: UpdateRetargetingConditionValidationService2

    private lateinit var clientInfo: ClientInfo
    private lateinit var retargetingCondition: RetargetingCondition
    private lateinit var modelChanges: ModelChanges<RetargetingCondition>

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()

        val activeTextAdGroup = steps.adGroupSteps().createActiveTextAdGroup()

        retargetingCondition = TestRetargetingConditions.defaultCpmRetCondition()
            .withClientId(clientInfo.clientId?.asLong())
            .withRules(listOf(
                defaultRule(listOf(GOAL_INTEREST_1), CryptaInterestType.short_term)
            )) as RetargetingCondition

        val retConditionInfo = steps.retConditionSteps().createRetCondition(retargetingCondition, clientInfo)
        val retargeting = TestRetargetings.defaultRetargeting(
            activeTextAdGroup.campaignId,
            activeTextAdGroup.adGroupId,
            retConditionInfo.retConditionId
        )

        modelChanges = ModelChanges(retConditionInfo.retConditionId, RetargetingCondition::class.java)

        steps.retargetingSteps()
            .createRetargeting(retargeting, CampaignInfo().withClientInfo(clientInfo), retConditionInfo)

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.NEW_CUSTOM_AUDIENCE_ENABLED, true)
        // костыль, чтобы skipGoalExistenceCheck == false
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UNIVERSAL_CAMPAIGNS_BETA_DISABLED, true)
        val interestGoals = (1..40).map { Goal().withId(GOAL_INTEREST_1.id + it) as Goal }
        steps.cryptaGoalsSteps().addGoals(interestGoals, setOf(CryptaGoalScope.PERFORMANCE))
        steps.cryptaGoalsSteps().addGoals(GOAL_INTEREST_COMMON)
    }

    @Test
    fun shouldUpdateRetargeting_interestAndHost() {
        val newRule = defaultRule(listOf(GOAL_INTEREST_2, GOAL_HOST), CryptaInterestType.short_term)
        retargetingCondition.rules = listOf(newRule)
        modelChanges.process(listOf(newRule), RetargetingCondition.RULES)

        checkRetargeting()
    }

    @Test
    fun shouldValidateRetargeting_hostNotFoundError() {
        val newRule = defaultRule(listOf(INVALID_GOAL_HOST), CryptaInterestType.short_term)
        retargetingCondition.rules = listOf(newRule)
        modelChanges.process(listOf(newRule), RetargetingCondition.RULES)

        checkRetargeting(DefectIds.OBJECT_NOT_FOUND)
    }

    @Test
    fun shouldValidateRetargeting_interestNotFoundError() {
        val newRule = defaultRule(listOf(INVALID_GOAL_INTEREST), CryptaInterestType.short_term)
        retargetingCondition.rules = listOf(newRule)
        modelChanges.process(listOf(newRule), RetargetingCondition.RULES)

        checkRetargeting(DefectIds.OBJECT_NOT_FOUND)
    }

    @Test
    fun shouldUpdateRetargeting_commonInterest() {
        val newRule = defaultRule(listOf(GOAL_INTEREST_COMMON, GOAL_INTEREST_2), CryptaInterestType.short_term)
        retargetingCondition.rules = listOf(newRule)
        modelChanges.process(listOf(newRule), RetargetingCondition.RULES)

        checkRetargeting()
    }

    @Test
    fun shouldValidateGoalsSize_error() {
        val interestGoals = (1..40).map { Goal().withId(GOAL_INTEREST_1.id + it) as Goal }

        val newRule = defaultRule(interestGoals, CryptaInterestType.short_term)
        retargetingCondition.rules = listOf(newRule)
        modelChanges.process(listOf(newRule), RetargetingCondition.RULES)

        checkRetargeting(CollectionDefectIds.Size.INVALID_COLLECTION_SIZE)
    }

    private fun checkRetargeting(defectId: DefectId<*>? = null) {
        val retargetings = listOf(retargetingCondition)

        val result = updateRetargetingConditionService.validateUpdateElements(
            ValidationResult.success(retargetings),
            retargetings,
            listOf(modelChanges.applyTo(retargetingCondition)),
            true,
            true,
            clientInfo.clientId,
            clientInfo.shard
        )

        val matcher = if (defectId != null) {
            hasDefectDefinitionWith(validationError(defectId))
        } else {
            hasNoErrors<List<RetargetingCondition>>()
        }

        Assert.assertThat(result, matcher)
    }
}
