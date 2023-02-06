package ru.yandex.direct.core.entity.retargeting.service.validation2

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalScope
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultCpmRetCondition
import ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRule
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CollectionDefects
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@CoreTest
@RunWith(SpringRunner::class)
class AddRetargetingValidationServiceForCustomAudienceTest {

    companion object {
        private val GOAL_INTEREST = Goal()
            .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND + 100_000L)
                as Goal

        private val GOAL_HOST = Goal()
            .withId(Goal.HOST_LOWER_BOUND)
                as Goal

        private val INVALID_GOAL_PATH = path(index(0), field("rules"), index(0), field("goals"), index(0), field("id"))

        private val INVALID_GOALS_SIZE_PATH = path(index(0), field("rules"), index(0), field("goals"))
    }
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var addRetargetingConditionValidationService2: AddRetargetingConditionValidationService2

    private lateinit var clientInfo: ClientInfo

    @Before
    fun setup() {
        clientInfo = steps.clientSteps().createDefaultClient()

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.NEW_CUSTOM_AUDIENCE_ENABLED, true)
        // костыль, чтобы skipGoalExistenceCheck == false
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UNIVERSAL_CAMPAIGNS_BETA_DISABLED, true)
        steps.cryptaGoalsSteps().addGoals(listOf(GOAL_INTEREST), setOf(CryptaGoalScope.PERFORMANCE))
    }

    @Test
    fun shouldValidateCpcRetargeting_interests() {
        val rule = defaultRule(listOf(GOAL_INTEREST))
        val retargeting = defaultCpmRetCondition()
            .withClientId(clientInfo.clientId?.asLong())
            .withRules(listOf(rule))
                as RetargetingCondition

        checkRetargeting(retargeting)
    }

    @Test
    fun shouldValidateCpcRetargetingError_interests() {
        val notExistingGoal = Goal()
            .withId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND + 100L)
                as Goal
        val rule = defaultRule(listOf(notExistingGoal))
        val retargeting = defaultCpmRetCondition()
            .withClientId(clientInfo.clientId?.asLong())
            .withRules(listOf(rule))
                as RetargetingCondition

        checkRetargeting(retargeting, true)
    }

    @Test
    fun shouldValidateCpcRetargeting_hosts() {
        val rule = defaultRule(listOf(GOAL_HOST))
        val retargeting = defaultCpmRetCondition()
            .withClientId(clientInfo.clientId?.asLong())
            .withRules(listOf(rule))
                as RetargetingCondition

        checkRetargeting(retargeting)
    }

    @Test
    fun shouldValidateCpcRetargetingError_hosts() {
        val invalidHostGoal = Goal().withId(Goal.HOST_LOWER_BOUND + 10L) as Goal
        val rule = defaultRule(listOf(invalidHostGoal))
        val retargeting = defaultCpmRetCondition()
            .withClientId(clientInfo.clientId?.asLong())
            .withRules(listOf(rule))
                as RetargetingCondition

        checkRetargeting(retargeting, true)
    }

    @Test
    fun shouldValidateCpcRetargeting_all() {
        val rule = defaultRule(listOf(GOAL_INTEREST, GOAL_HOST))
        val retargeting = defaultCpmRetCondition()
            .withClientId(clientInfo.clientId?.asLong())
            .withRules(listOf(rule))
                as RetargetingCondition

        checkRetargeting(retargeting)
    }

    @Test
    fun shouldValidateMaxLimitError() {
        val goals = (1..40).map { Goal().withId(GOAL_INTEREST.id + it) as Goal }
        steps.cryptaGoalsSteps().addGoals(goals, setOf(CryptaGoalScope.PERFORMANCE))
        val rule = defaultRule(goals)
        val retargeting = defaultCpmRetCondition()
            .withClientId(clientInfo.clientId?.asLong())
            .withRules(listOf(rule)) 
                as RetargetingCondition
        val result = addRetargetingConditionValidationService2.validate(listOf(retargeting), clientInfo.clientId)
        Assert.assertThat(result, hasDefectWithDefinition(validationError(INVALID_GOALS_SIZE_PATH, CollectionDefects.collectionSizeIsValid(0, 30))))
    }

    private fun checkRetargeting(retargeting: RetargetingCondition, hasError: Boolean = false) {
        val result = addRetargetingConditionValidationService2.validate(listOf(retargeting), clientInfo.clientId)
        val matcher = if (hasError) {
            hasDefectWithDefinition(validationError(INVALID_GOAL_PATH, CommonDefects.objectNotFound()))
        } else {
            hasNoErrors<List<RetargetingCondition>>()
        }

        Assert.assertThat(result, matcher)
    }
}
