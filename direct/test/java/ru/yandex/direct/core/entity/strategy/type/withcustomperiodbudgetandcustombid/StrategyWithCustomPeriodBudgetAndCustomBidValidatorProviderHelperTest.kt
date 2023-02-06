package ru.yandex.direct.core.entity.strategy.type.withcustomperiodbudgetandcustombid

import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.entity.strategy.container.StrategyUpdateOperationContainer
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyWithCustomPeriodBudget
import ru.yandex.direct.core.entity.strategy.model.StrategyWithCustomPeriodBudgetAndCustomBid
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxImpressionsCustomPeriodStrategy.clientAutobudgetMaxImpressionsCustomPeriodStrategy
import ru.yandex.direct.core.validation.defects.MoneyDefects
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.currency.Money
import ru.yandex.direct.currency.currencies.CurrencyRub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.DateDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.PathNode
import java.math.BigDecimal
import java.time.LocalDateTime

@CoreTest
class StrategyWithCustomPeriodBudgetAndCustomBidValidatorProviderHelperTest {

    private val clientId = Mockito.mock(ClientId::class.java)
    private val operatorUid = 1L
    private val currency = CurrencyRub.getInstance()
    private val currentDateTime = LocalDateTime.now()
    private val now = currentDateTime.toLocalDate()

    @Test
    fun `budget update validation ok, when budget less than min, but finish is null`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now)
            .withFinish(null)
            .withBudget(BigDecimal.ONE)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)

        preValidateAndGetResultOnUpdate(strategy, modelChanges, mapOf(strategy.id to BigDecimal.TEN))
    }

    @Test
    fun `budget update validation ok, when budget less than min, but now is before start`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now.plusDays(1))
            .withFinish(now)
            .withBudget(BigDecimal.ONE)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)

        preValidateAndGetResultOnUpdate(strategy, modelChanges, mapOf(strategy.id to BigDecimal.TEN))
    }

    @Test
    fun `budget update validation ok, when budget less than min, but now is after finish`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now.minusDays(2))
            .withFinish(now.minusDays(1))
            .withBudget(BigDecimal.ONE)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)

        preValidateAndGetResultOnUpdate(strategy, modelChanges, mapOf(strategy.id to BigDecimal.TEN))
    }


    @Test
    fun `budget update validation fail, when budget less than min and finish and start ok`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now)
            .withFinish(now.plusDays(1))
            .withBudget(BigDecimal.ONE)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN),
            MoneyDefects.invalidValueCpmNotLessThan(Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB)),
            PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET)
        )
    }

    @Test
    fun `budget update validation ok, when budget less than min, but strategy type changed`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now)
            .withFinish(now.plusDays(1))
            .withBudget(BigDecimal.ONE)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD, CommonStrategy.TYPE)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN)
        )
    }

    @Test
    fun `budget update validation ok, when budget less than min, but start changed`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now)
            .withFinish(now.plusDays(2))
            .withBudget(BigDecimal.ONE)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(now.plusDays(1), StrategyWithCustomPeriodBudgetAndCustomBid.START)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN)
        )
    }

    @Test
    fun `budget update validation fail, when budget less than min and start and type not changed`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now)
            .withFinish(now.plusDays(2))
            .withBudget(BigDecimal.ONE)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN),
            MoneyDefects.invalidValueCpmNotLessThan(Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB)),
            PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET)
        )
    }

    @Test
    fun `start update validation fail, when start before now and strategy type changed`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now.minusDays(1))
            .withFinish(now.plusDays(2))
            .withBudget(BigDecimal.TEN)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD, CommonStrategy.TYPE)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN),
            DateDefects.greaterThanOrEqualTo(now),
            PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.START)
        )
    }

    @Test
    fun `start update validation fail, when start before now and start changed`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now)
            .withFinish(now.plusDays(2))
            .withBudget(BigDecimal.TEN)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(now.minusDays(1), StrategyWithCustomPeriodBudgetAndCustomBid.START)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN),
            DateDefects.greaterThanOrEqualTo(now),
            PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.START)
        )
    }

    @Test
    fun `start update validation fail, when start before now and autoProlongation changed and strategy type changed not to one with custom period budget and custom bid`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now.minusDays(1))
            .withFinish(now.plusDays(2))
            .withBudget(BigDecimal.TEN)
            .withAutoProlongation(true)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(StrategyName.PERIOD_FIX_BID, CommonStrategy.TYPE)
            .process(false, StrategyWithCustomPeriodBudget.AUTO_PROLONGATION)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN),
            DateDefects.greaterThanOrEqualTo(now),
            PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.START)
        )
    }

    @Test
    fun `start update validation success, when start before now and autoProlongation changed, but strategy type not changed`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now.minusDays(1))
            .withFinish(now.plusDays(2))
            .withBudget(BigDecimal.TEN)
            .withAutoProlongation(true)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(false, StrategyWithCustomPeriodBudget.AUTO_PROLONGATION)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN)
        )
    }

    @Test
    fun `start update validation fail, when start before now and budget changed and strategy type changed not to one with custom period budget and custom bid`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now.minusDays(1))
            .withFinish(now.plusDays(2))
            .withBudget(BigDecimal.TEN)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(StrategyName.PERIOD_FIX_BID, CommonStrategy.TYPE)
            .process(BigDecimal.valueOf(100), StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN),
            DateDefects.greaterThanOrEqualTo(now),
            PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.START)
        )
    }

    @Test
    fun `finish update validation fail, when finish before now and finish changed`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now.minusDays(3))
            .withFinish(now.minusDays(2))
            .withBudget(BigDecimal.TEN)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(now.minusDays(1), StrategyWithCustomPeriodBudgetAndCustomBid.FINISH)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN),
            DateDefects.greaterThanOrEqualTo(now),
            PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.FINISH)
        )
    }

    @Test
    fun `finish update validation ok, when finish before now and finish not changed`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now.minusDays(3))
            .withFinish(now.minusDays(2))
            .withBudget(BigDecimal.TEN)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN)
        )
    }

    @Test
    fun `finish update validation fail, when finish before now and start changed`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now.minusDays(3))
            .withFinish(now.minusDays(2))
            .withBudget(BigDecimal.TEN)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(now.minusDays(4), StrategyWithCustomPeriodBudgetAndCustomBid.START)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN),
            DateDefects.greaterThanOrEqualTo(now),
            PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.FINISH)
        )
    }

    @Test
    fun `finish update validation fail, when finish before now and budget changed`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now.minusDays(3))
            .withFinish(now.minusDays(2))
            .withBudget(BigDecimal.TEN)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(BigDecimal.valueOf(11), StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN),
            DateDefects.greaterThanOrEqualTo(now),
            PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.FINISH)
        )
    }

    @Test
    fun `finish update validation fail, when finish before now and strategy type changed`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now.minusDays(3))
            .withFinish(now.minusDays(2))
            .withBudget(BigDecimal.TEN)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD, CommonStrategy.TYPE)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN),
            DateDefects.greaterThanOrEqualTo(now),
            PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.FINISH)
        )
    }

    @Test
    fun `finish update validation fail, when finish before now and autoProlongation changed and strategy type changed not to one with custom period budget and custom bid`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now.minusDays(3))
            .withFinish(now.minusDays(2))
            .withBudget(BigDecimal.TEN)
            .withAutoProlongation(true)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(StrategyName.PERIOD_FIX_BID, CommonStrategy.TYPE)
            .process(false, StrategyWithCustomPeriodBudgetAndCustomBid.AUTO_PROLONGATION)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN),
            DateDefects.greaterThanOrEqualTo(now),
            PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.FINISH)
        )
    }

    @Test
    fun `finish update validation success, when finish before now and autoProlongation changed and strategy type with custom period budget and custom bid`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now.minusDays(3))
            .withFinish(now.minusDays(2))
            .withBudget(BigDecimal.TEN)
            .withAutoProlongation(true)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(false, StrategyWithCustomPeriodBudgetAndCustomBid.AUTO_PROLONGATION)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN)
        )
    }

    @Test
    fun `finish update validation fail, when finish before now and autoProlongation changed and strategy type changed to one with custom period budget and custom bid`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(getRandomId())
            .withStart(now.minusDays(3))
            .withFinish(now.minusDays(2))
            .withBudget(BigDecimal.TEN)
            .withAutoProlongation(true)

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(StrategyName.PERIOD_FIX_BID, CommonStrategy.TYPE)
            .process(false, StrategyWithCustomPeriodBudgetAndCustomBid.AUTO_PROLONGATION)

        preValidateAndGetResultOnUpdate(
            strategy,
            modelChanges,
            mapOf(strategy.id to BigDecimal.TEN),
            DateDefects.greaterThanOrEqualTo(now),
            PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.FINISH)
        )
    }

    private fun preValidateAndGetResultOnUpdate(
        strategy: StrategyWithCustomPeriodBudgetAndCustomBid,
        modelChanges: ModelChanges<StrategyWithCustomPeriodBudgetAndCustomBid>,
        minimalBudgetForStrategiesCampaigns: Map<Long, BigDecimal>,
        defect: Defect<*>? = null,
        propertyPath: PathNode.Field? = null
    ) {
        val container = StrategyUpdateOperationContainer(1, clientId, operatorUid, operatorUid)
        val campaigns = listOf(
            CpmBannerCampaign()
                .withType(CampaignType.CPM_BANNER)
                .withStartDate(now)
                .withEndDate(now.plusDays(7))
        )
        container.typedCampaignsMap = mapOf(strategy.id to campaigns)
        container.currency = currency

        val strategyWithCustomPeriodBudgetAndCustomBidContainer =
            StrategyWithCustomPeriodBudgetAndCustomBidUpdateOperationContainer(
                mapOf(strategy.id to strategy),
                minimalBudgetForStrategiesCampaigns
            )

        val validator =
            StrategyWithCustomPeriodBudgetAndCustomBidValidatorProvider.createUpdateStrategyBeforeApplyValidator(
                container,
                strategyWithCustomPeriodBudgetAndCustomBidContainer
            )

        val validationResult = validator.apply(modelChanges)

        defect?.let {
            Assert.assertThat(
                validationResult,
                Matchers.hasDefectDefinitionWith(
                    Matchers.validationError(
                        propertyPath?.let { PathHelper.path(propertyPath) } ?: PathHelper.emptyPath(),
                        it
                    )
                )
            )
        } ?: Assert.assertThat(validationResult, Matchers.hasNoDefectsDefinitions())
    }

    private fun getRandomId() = (0..1000L).random()

}
