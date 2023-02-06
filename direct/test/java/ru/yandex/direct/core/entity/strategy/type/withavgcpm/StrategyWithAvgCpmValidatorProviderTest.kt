package ru.yandex.direct.core.entity.strategy.type.withavgcpm

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.mock
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign
import ru.yandex.direct.core.entity.campaign.model.CpmYndxFrontpageCampaign
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.CpmYndxFrontpageAdGroupPriceRestrictions
import ru.yandex.direct.core.entity.strategy.container.StrategyAddOperationContainer
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxImpressions
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxImpressionsCustomPeriod
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReach
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReachCustomPeriod
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgCpm
import ru.yandex.direct.core.validation.defects.MoneyDefects
import ru.yandex.direct.currency.Money
import ru.yandex.direct.currency.currencies.CurrencyRub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult
import java.math.BigDecimal
import java.util.*

@RunWith(Parameterized::class)
class StrategyWithAvgCpmValidatorProviderTest(
    private val testStrategy: String,
    private val strategy: StrategyWithAvgCpm
) {

    companion object {

        private val testCampaigns: Set<CampaignWithPackageStrategy> = setOf(
            CpmBannerCampaign().withType(CampaignType.CPM_BANNER),
            CpmYndxFrontpageCampaign().withType(CampaignType.CPM_YNDX_FRONTPAGE),
            CpmPriceCampaign().withType(CampaignType.CPM_PRICE)
        )

        private val campaignTypesForMinCpmPriceCheck = setOf(
            CampaignType.CPM_YNDX_FRONTPAGE,
            CampaignType.CPM_PRICE
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Collection<Array<Any?>> = listOf(
            arrayOf(
                "AutobudgetMaxImpressions",
                AutobudgetMaxImpressions().withType(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS),
            ),
            arrayOf(
                "AutobudgetMaxImpressionsCustomPeriod",
                AutobudgetMaxImpressionsCustomPeriod().withType(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD),
            ),
            arrayOf(
                "AutobudgetMaxReach",
                AutobudgetMaxReach().withType(StrategyName.AUTOBUDGET_MAX_REACH),
            ),
            arrayOf(
                "AutobudgetMaxReachCustomPeriod",
                AutobudgetMaxReachCustomPeriod().withType(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD),
            )
        )
    }

    private val clientId = mock(ClientId::class.java)
    private val operatorUid = 1L
    private val currency = CurrencyRub.getInstance()

    @Test
    fun shouldValidateOk_whenMinValue() {
        strategy.avgCpm = currency.minAutobudgetAvgCpm
        validateAndCheckOk()
    }

    @Test
    fun shouldValidateOk_whenMaxValue() {
        strategy.avgCpm = currency.maxCpmPrice
        validateAndCheckOk()
    }

    @Test
    fun shouldValidationFail_whenValueIsLessThanMin_forceValidateAvgCpmMin() {
        strategy.avgCpm = BigDecimal("4")
        validateAndCheckError(NumberDefects.greaterThanOrEqualTo(currency.minAutobudgetAvgCpm), true)
    }

    @Test
    fun shouldValidateOk_whenValueIsLessThanMin_when() {
        strategy.avgCpm = BigDecimal("4")
        validateAndCheckError(NumberDefects.greaterThanOrEqualTo(currency.minAutobudgetAvgCpm), true)
    }

    @Test
    fun shouldValidationFail_whenValueIsLessThanMin_campaignTypesForMinCpmPriceCheck() {
        strategy.avgCpm = BigDecimal("4")

        testCampaigns.forEach { testCamp ->
            if (testCamp.type in campaignTypesForMinCpmPriceCheck) {
                validateAndCheckError(
                    NumberDefects.greaterThanOrEqualTo(currency.minAutobudgetAvgCpm),
                    campaign = testCamp
                )
            } else {
                validateAndCheckOk(campaign = testCamp)
            }
        }
    }

    @Test
    fun shouldValidationFail_whenValueIsGreaterThanMax() {
        strategy.avgCpm = BigDecimal("3001")
        validateAndCheckError(NumberDefects.lessThanOrEqualTo(currency.maxCpmPrice))
    }

    @Test
    fun shouldValidationFail_whenValueIsNull() {
        strategy.avgCpm = null
        validateAndCheckError(CommonDefects.notNull())
    }

    @Test
    fun shouldValidateOk_whenValueBetweenCpmYndxFrontpageMinAndMaxPrice() {
        strategy.avgCpm = currency.maxCpmPrice
        validateAndCheckOk(
            CpmYndxFrontpageCampaign().withType(CampaignType.CPM_YNDX_FRONTPAGE),
            currency.maxCpmPrice
        )
    }

    @Test
    fun shouldValidationFail_whenAvgCpmGreaterThanCpmYndxFrontpageMaxPrice() {
        strategy.avgCpm = currency.maxCpmPrice
        validateAndCheckError(
            MoneyDefects.invalidValueCpmNotGreaterThan(
                Money.valueOf(
                    currency.minCpmPrice,
                    currency.code
                ),
            ),
            campaign = CpmYndxFrontpageCampaign().withType(CampaignType.CPM_YNDX_FRONTPAGE),
            cpmYndxFrontpageMinAndMaxPrice = currency.minCpmPrice,
        )
    }

    private fun validateAndCheckOk(
        campaign: CampaignWithPackageStrategy = CpmBannerCampaign().withType(CampaignType.CPM_BANNER),
        cpmYndxFrontpageMinAndMaxPrice: BigDecimal? = null
    ) {
        val result =
            validateAndGetResult(campaign = campaign, cpmYndxFrontpageMinAndMaxPrice = cpmYndxFrontpageMinAndMaxPrice)

        Assert.assertThat(result, Matchers.hasNoDefectsDefinitions())
    }

    private fun validateAndCheckError(
        defect: Defect<*>,
        forceValidateAvgCpmMin: Boolean = false,
        campaign: CampaignWithPackageStrategy = CpmBannerCampaign().withType(CampaignType.CPM_BANNER),
        cpmYndxFrontpageMinAndMaxPrice: BigDecimal? = null
    ) {
        val result = validateAndGetResult(forceValidateAvgCpmMin, campaign, cpmYndxFrontpageMinAndMaxPrice)

        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(PathHelper.field(StrategyWithAvgCpm.AVG_CPM)),
                    defect
                )
            )
        )
    }

    private fun validateAndGetResult(
        forceValidateAvgCpmMin: Boolean = false,
        campaign: CampaignWithPackageStrategy,
        cpmYndxFrontpageMinAndMaxPrice: BigDecimal?
    ): ValidationResult<StrategyWithAvgCpm, Defect<Any>> {
        val container =
            StrategyAddOperationContainer(1, clientId, operatorUid, operatorUid, StrategyOperationOptions(forceValidateAvgCpmMin))
        container.currency = currency
        campaign.clientId = clientId.asLong()
        container.typedCampaignsMap = mapOf(strategy to listOf(campaign)).toMap(IdentityHashMap())
        container.cpmYndxFrontpageAdGroupPriceRestrictions =
            CpmYndxFrontpageAdGroupPriceRestrictions(cpmYndxFrontpageMinAndMaxPrice, cpmYndxFrontpageMinAndMaxPrice)
                .withClientCurrency(container.currency)

        val validator = StrategyWithAvgCpmValidatorProvider.createStrategyValidator(container)
        return validator.apply(strategy)
    }

}
