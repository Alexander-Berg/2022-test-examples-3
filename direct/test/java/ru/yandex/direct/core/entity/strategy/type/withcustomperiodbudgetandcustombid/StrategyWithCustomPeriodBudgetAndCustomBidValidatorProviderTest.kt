package ru.yandex.direct.core.entity.strategy.type.withcustomperiodbudgetandcustombid

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.mock
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.CampaignWithCustomStrategyValidator
import ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.CpmCampaignWithCustomStrategyBeforeApplyValidator
import ru.yandex.direct.core.entity.strategy.container.StrategyAddOperationContainer
import ru.yandex.direct.core.entity.strategy.container.StrategyUpdateOperationContainer
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpvCustomPeriod
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxImpressionsCustomPeriod
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReachCustomPeriod
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyWithCustomPeriodBudgetAndCustomBid
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.validation.defects.MoneyDefects
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.currency.Money
import ru.yandex.direct.currency.currencies.CurrencyRub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.PathNode
import ru.yandex.direct.validation.result.ValidationResult
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.IdentityHashMap

@CoreTest
@RunWith(Parameterized::class)
class StrategyWithCustomPeriodBudgetAndCustomBidValidatorProviderTest(
    private val strategy: StrategyWithCustomPeriodBudgetAndCustomBid,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Collection<Array<Any?>> = listOf(
            arrayOf(
                AutobudgetAvgCpvCustomPeriod().withId(1).withType(StrategyName.AUTOBUDGET_AVG_CPV_CUSTOM_PERIOD),
            ),
            arrayOf(
                AutobudgetMaxImpressionsCustomPeriod().withId(2)
                    .withType(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD),
            ),
            arrayOf(
                AutobudgetMaxReachCustomPeriod().withId(3).withType(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD),
            )
        )
    }

    private val clientId = mock(ClientId::class.java)
    private val operatorUid = 1L
    private val currency = CurrencyRub.getInstance()
    private val currentDateTime = LocalDateTime.now()
    private val currentLocalDate = currentDateTime.toLocalDate()
    private val minimalBudgetForStrategiesCampaigns =
        mapOf(1L to BigDecimal.ONE, 2L to BigDecimal.ONE, 3L to BigDecimal.ONE)

    @Test
    fun shouldValidateOk() {
        strategy.start = currentLocalDate
        strategy.finish = currentLocalDate.plusDays(1)
        strategy.budget = BigDecimal(1000)
        validateAndCheckOkOnAdd()
    }

    @Test
    fun shouldValidationFail_whenStrategyStartDateIsBeforeCampaignStartDate() {
        strategy.start = currentLocalDate.minusDays(1)
        strategy.finish = currentLocalDate
        strategy.budget = BigDecimal(1000)
        validateAndCheckErrorOnAdd(
            StrategyDefects.strategyStartDateIsBeforeCampaignStartDate(),
            PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.START)
        )
    }

    @Test
    fun shouldValidationFail_whenStrategyPeriodIsLessThanMinStrategyPeriodDaysCount() {
        strategy.start = currentLocalDate
        strategy.finish =
            currentLocalDate.plusDays(CampaignWithCustomStrategyValidator.MIN_STRATEGY_PERIOD_DAYS_COUNT.toLong() - 1)
        strategy.budget = BigDecimal(1000)
        validateAndCheckErrorOnAdd(
            StrategyDefects.strategyPeriodDaysCountLessThanMin(CampaignWithCustomStrategyValidator.MIN_STRATEGY_PERIOD_DAYS_COUNT),
            PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.FINISH)
        )
    }

    @Test
    fun shouldValidationFail_whenBudgetIsLessThanMin() {
        strategy.start = currentLocalDate
        strategy.finish = currentLocalDate.plusDays(7)
        strategy.budget = BigDecimal(2399)
        validateAndCheckErrorOnAdd(
            MoneyDefects.invalidValueCpmNotLessThan(Money.valueOf(BigDecimal(2400), currency.code)),
            PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET)
        )
    }

    @Test
    fun updatePreValidationSuccess() {
        strategy.start = currentLocalDate
        strategy.finish = currentLocalDate.plusDays(1)
        strategy.budget = BigDecimal(1000)
        strategy.dailyChangeCount = 3
        strategy.lastUpdateTime = currentDateTime

        val modelChanges = ModelChanges(strategy.id, strategy.javaClass)
        preValidateAndCheckOkOnUpdate(modelChanges = modelChanges)
    }

    @Test
    fun updatePreValidationFail_ChangingLimitWasExceeded() {
        strategy.start = currentLocalDate
        strategy.finish = currentLocalDate.plusDays(1)
        strategy.budget = BigDecimal(1000)
        strategy.dailyChangeCount = 4
        strategy.lastUpdateTime = currentDateTime

        val modelChanges = ModelChanges(strategy.id, strategy.javaClass)
        preValidateAndCheckErrorOnUpdate(
            StrategyDefects.strategyChangingLimitWasExceeded(CpmCampaignWithCustomStrategyBeforeApplyValidator.CPM_STRATEGY_MAX_DAILY_CHANGE_COUNT),
            modelChanges = modelChanges
        )
    }

    @Test
    fun updatePreValidationSuccess_BudgetLessThanNeeded_StrategyStartDateChanged() {
        strategy.start = currentLocalDate
        strategy.finish = currentLocalDate.plusDays(7)
        strategy.budget = BigDecimal(1000)
        strategy.dailyChangeCount = 3
        strategy.lastUpdateTime = currentDateTime

        val modelChanges = ModelChanges(strategy.id, strategy.javaClass)
        modelChanges
            .process(BigDecimal(-1), StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET)
            .process(LocalDate.now().plusDays(1), StrategyWithCustomPeriodBudgetAndCustomBid.START)
        preValidateAndCheckOkOnUpdate(modelChanges = modelChanges)
    }

    @Test
    fun updatePreValidationFail_BudgetLessThanNeeded_StrategyStartDateAndTypeNotChanged() {
        strategy.start = currentLocalDate
        strategy.finish = currentLocalDate.plusDays(7)
        strategy.budget = BigDecimal(1000)

        val modelChanges = ModelChanges(strategy.id, strategy.javaClass)
            .process(BigDecimal(-1), StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET)
        preValidateAndCheckErrorOnUpdate(
            MoneyDefects.invalidValueCpmNotLessThan(
                Money.valueOf(BigDecimal.valueOf(0), CurrencyCode.RUB)
            ),
            propertyPath = PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET),
            modelChanges = modelChanges,
            minimalBudgetForStrategiesCampaigns = mapOf(strategy.id to BigDecimal.ZERO)
        )
    }

    @Test
    fun updatePreValidationSuccess_BudgetLessThanNeeded_StrategyStartDateAfterNow() {
        strategy.start = currentLocalDate.plusDays(1)
        strategy.finish = currentLocalDate.plusDays(7)
        strategy.budget = BigDecimal(1000)
        strategy.dailyChangeCount = 3
        strategy.lastUpdateTime = currentDateTime

        val modelChanges = ModelChanges(strategy.id, strategy.javaClass)
            .process(BigDecimal(-1), StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET)
        preValidateAndCheckOkOnUpdate(modelChanges = modelChanges)
    }

    @Test
    fun updatePreValidationFail_BudgetLessThanNeeded_StrategyStartDateAndTypeNotChanged_MinimalAvailableBudgetNotZero() {
        strategy.start = currentLocalDate
        strategy.finish = currentLocalDate.plusDays(7)
        strategy.budget = BigDecimal(1000)

        val modelChanges = ModelChanges(strategy.id, strategy.javaClass)
            .process(BigDecimal(2000), StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET)
        preValidateAndCheckErrorOnUpdate(
            MoneyDefects.invalidValueCpmNotLessThan(
                Money.valueOf(BigDecimal.valueOf(2400), CurrencyCode.RUB)
            ),
            propertyPath = PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET),
            modelChanges = modelChanges,
            minimalBudgetForStrategiesCampaigns = mapOf(strategy.id to BigDecimal(2400))
        )
    }

    private fun validateAndCheckOkOnAdd(
        campaigns: List<CampaignWithPackageStrategy> = listOf(
            CpmBannerCampaign()
                .withType(CampaignType.CPM_BANNER)
                .withStartDate(currentLocalDate)
                .withEndDate(currentLocalDate.plusDays(7))
        )
    ) {
        val result = validateAndGetResultOnAdd(campaigns)

        Assert.assertThat(result, Matchers.hasNoDefectsDefinitions())
    }

    private fun validateAndCheckErrorOnAdd(
        defect: Defect<*>,
        propertyPath: PathNode.Field,
        campaigns: List<CampaignWithPackageStrategy> = listOf(
            CpmBannerCampaign()
                .withClientId(clientId.asLong())
                .withType(CampaignType.CPM_BANNER)
                .withStartDate(currentLocalDate)
                .withEndDate(
                    currentLocalDate.plusDays(CampaignWithCustomStrategyValidator.MAX_STRATEGY_PERIOD_DAYS_COUNT.toLong())
                )
        )
    ) {
        val result = validateAndGetResultOnAdd(campaigns)

        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(propertyPath),
                    defect
                )
            )
        )
    }

    private fun validateAndGetResultOnAdd(
        campaigns: List<CampaignWithPackageStrategy>
    ): ValidationResult<StrategyWithCustomPeriodBudgetAndCustomBid, Defect<*>> {
        val container = StrategyAddOperationContainer(1, clientId, operatorUid, operatorUid)
        container.typedCampaignsMap = mapOf(strategy to campaigns).toMap(IdentityHashMap())
        container.currency = currency

        val validator =
            StrategyWithCustomPeriodBudgetAndCustomBidValidatorProvider.createAddStrategyValidator(container)
        return validator.apply(strategy)
    }

    private fun preValidateAndCheckOkOnUpdate(
        campaigns: List<CampaignWithPackageStrategy> = listOf(
            CpmBannerCampaign()
                .withType(CampaignType.CPM_BANNER)
                .withStartDate(currentLocalDate)
                .withEndDate(currentLocalDate.plusDays(7))
        ),
        minimalBudgetForStrategiesCampaigns: Map<Long, BigDecimal> = this.minimalBudgetForStrategiesCampaigns,
        modelChanges: ModelChanges<StrategyWithCustomPeriodBudgetAndCustomBid>
    ) {
        val result = preValidateAndGetResultOnUpdate(campaigns, modelChanges, minimalBudgetForStrategiesCampaigns)

        Assert.assertThat(result, Matchers.hasNoDefectsDefinitions())
    }

    private fun preValidateAndCheckErrorOnUpdate(
        defect: Defect<*>,
        propertyPath: PathNode.Field? = null,
        campaigns: List<CampaignWithPackageStrategy> = listOf(
            CpmBannerCampaign()
                .withType(CampaignType.CPM_BANNER)
                .withClientId(clientId.asLong())
                .withStartDate(currentLocalDate)
                .withEndDate(
                    currentLocalDate.plusDays(CampaignWithCustomStrategyValidator.MAX_STRATEGY_PERIOD_DAYS_COUNT.toLong())
                )
        ),
        minimalBudgetForStrategiesCampaigns: Map<Long, BigDecimal> = this.minimalBudgetForStrategiesCampaigns,
        modelChanges: ModelChanges<StrategyWithCustomPeriodBudgetAndCustomBid>
    ) {
        val result = preValidateAndGetResultOnUpdate(campaigns, modelChanges, minimalBudgetForStrategiesCampaigns)

        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    propertyPath?.let { PathHelper.path(it) } ?: PathHelper.emptyPath(),
                    defect
                )
            )
        )
    }

    private fun preValidateAndGetResultOnUpdate(
        campaigns: List<CampaignWithPackageStrategy>,
        modelChanges: ModelChanges<StrategyWithCustomPeriodBudgetAndCustomBid>,
        minimalBudgetForStrategiesCampaigns: Map<Long, BigDecimal>
    ): ValidationResult<ModelChanges<StrategyWithCustomPeriodBudgetAndCustomBid>, Defect<*>> {
        val container = StrategyUpdateOperationContainer(1, clientId, operatorUid, operatorUid)
        container.typedCampaignsMap = mapOf(strategy.id to campaigns)
        container.currency = currency

        val additionalContainer = StrategyWithCustomPeriodBudgetAndCustomBidUpdateOperationContainer(
            mapOf(strategy.id to strategy),
            minimalBudgetForStrategiesCampaigns
        )

        val validator =
            StrategyWithCustomPeriodBudgetAndCustomBidValidatorProvider.createUpdateStrategyBeforeApplyValidator(
                container,
                additionalContainer
            )
        return validator.apply(modelChanges)
    }

}
