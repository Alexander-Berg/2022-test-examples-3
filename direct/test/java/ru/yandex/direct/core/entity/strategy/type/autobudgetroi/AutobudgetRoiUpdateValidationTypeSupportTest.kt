package ru.yandex.direct.core.entity.strategy.type.autobudgetroi

import java.math.BigDecimal
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.PROFITABILITY_MAX
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.PROFITABILITY_MIN
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.RESERVE_RETURN_MAX
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.RESERVE_RETURN_MIN
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.RESERVE_RETURN_STEP
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.ROI_COEF_MIN
import ru.yandex.direct.core.entity.strategy.model.AutobudgetRoi
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetRoiStrategy.autobudgetRoi
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.result.MassResultMatcher
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.NumberDefects.greaterThan
import ru.yandex.direct.validation.defect.NumberDefects.inInterval
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@CoreTest
@RunWith(SpringRunner::class)
class AutobudgetRoiUpdateValidationTypeSupportTest : StrategyUpdateOperationTestBase() {
    private lateinit var user: UserInfo

    @Before
    fun init() {
        user = steps.userSteps().createDefaultUser()
        walletService.createWalletForNewClient(user.clientId, user.uid)
    }

    @Test
    fun `update to valid strategy`() {
        val goalId = RandomNumberUtils.nextPositiveInteger()
        val counterId = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counterId, goalId)

        val strategy = autobudgetRoi()
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withGoalId(goalId.toLong())
            .withRoiCoef(ROI_COEF_MIN.plus(BigDecimal.ONE))
            .withReserveReturn(RESERVE_RETURN_MIN)
            .withProfitability(PROFITABILITY_MIN)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, AutobudgetRoi::class.java)
            .process(ROI_COEF_MIN.plus(BigDecimal.TEN), AutobudgetRoi.ROI_COEF)
            .process(RESERVE_RETURN_MIN + RESERVE_RETURN_STEP, AutobudgetRoi.RESERVE_RETURN)
            .process(PROFITABILITY_MIN.plus(BigDecimal.TEN), AutobudgetRoi.PROFITABILITY)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()
        result.check(MassResultMatcher.isFullySuccessful())
    }

    @Test
    fun `fail on invalid field values`() {
        val goalId = RandomNumberUtils.nextPositiveInteger()
        val counterId = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counterId, goalId)

        val strategy = autobudgetRoi()
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withGoalId(goalId.toLong())
            .withRoiCoef(ROI_COEF_MIN.plus(BigDecimal.ONE))
            .withReserveReturn(RESERVE_RETURN_MIN)
            .withProfitability(PROFITABILITY_MIN)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, AutobudgetRoi::class.java)
            .process(ROI_COEF_MIN.minus(BigDecimal.ONE), AutobudgetRoi.ROI_COEF)
            .process(RESERVE_RETURN_MIN - RESERVE_RETURN_STEP, AutobudgetRoi.RESERVE_RETURN)
            .process(PROFITABILITY_MIN.minus(BigDecimal.TEN), AutobudgetRoi.PROFITABILITY)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()

        val marchers: List<Matcher<ValidationResult<Any, Defect<Any>>>> = listOf(
            Matchers.hasDefectDefinitionWith<Any>(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0), PathHelper.field(AutobudgetRoi.ROI_COEF)),
                    greaterThan(ROI_COEF_MIN)
                )
            ),
            Matchers.hasDefectDefinitionWith<Any>(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0), PathHelper.field(AutobudgetRoi.RESERVE_RETURN)),
                    inInterval(RESERVE_RETURN_MIN, RESERVE_RETURN_MAX)
                )
            ),
            Matchers.hasDefectDefinitionWith<Any>(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0), PathHelper.field(AutobudgetRoi.PROFITABILITY)),
                    inInterval(PROFITABILITY_MIN, PROFITABILITY_MAX)
                )
            )
        )
        marchers.forEach {
            (result.validationResult as ValidationResult<Any, Defect<Any>>).check(it)
        }
    }


    override fun getShard(): Int = user.shard

    override fun getClientId(): ClientId = user.clientId

    override fun getOperatorUid(): Long = user.uid
}
