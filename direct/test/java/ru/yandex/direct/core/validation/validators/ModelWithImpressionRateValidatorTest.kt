package ru.yandex.direct.core.validation.validators

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations.openMocks
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup
import ru.yandex.direct.core.entity.campaign.model.InternalCampaignWithImpressionRate
import ru.yandex.direct.core.entity.campaign.model.InternalFreeCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.validation.defects.Defects.requiredImpressionRateDueToAdsHasCloseCounter
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects.inconsistentState
import ru.yandex.direct.validation.defect.NumberDefects.greaterThan
import ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo
import ru.yandex.direct.validation.defect.params.NumberDefectParams
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.path

@CoreTest
@RunWith(JUnitParamsRunner::class)
class ModelWithImpressionRateValidatorTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var featureService: FeatureService

    private lateinit var clientId: ClientId

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val springMethodRule = SpringMethodRule()

    @Before
    fun before() {
        openMocks(this)
        val clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.clientId!!;
        steps.featureSteps().addClientFeature(clientId, FeatureName.CPM_SHOWS_FREQUENCY_50_PER_30, true);
    }

    fun parametrizedTestData(): List<List<Any?>> = listOf(
            listOf("empty bannerWithCloseCounterVarIds", emptyList<Long>(), null, false),
            listOf("empty bannerWithCloseCounterVarIds and adGroup has rf", emptyList<Long>(), 123, false),
            listOf("with bannerWithCloseCounterVarIds and adGroup with rf", listOf(123L, 34L), 23, false),
            listOf("with bannerWithCloseCounterVarIds and adGroup without rf", listOf(123L, 34L), null, true)
    )

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("checkValidateImpressionRate: {0}")
    fun checkValidateImpressionRate(@Suppress("UNUSED_PARAMETER") description: String,
                                    bannerWithCloseCounterVarIds: List<Long>,
                                    rfValue: Int?,
                                    hasError: Boolean) {
        val adGroup = InternalAdGroup()
                .withRf(rfValue)
        val validationResult = getValidatorForAdGroup(bannerWithCloseCounterVarIds).apply(adGroup)

        if (!hasError) {
            assertThat(validationResult, hasNoErrorsAndWarnings())
        } else {
            assertThat(validationResult, hasDefectDefinitionWith(
                    validationError(path(field(InternalAdGroup.RF)),
                            requiredImpressionRateDueToAdsHasCloseCounter(bannerWithCloseCounterVarIds))))
        }
    }

    private fun getValidatorForAdGroup(bannerWithCloseCounterVarIds: List<Long>): ModelWithInternalImpressionRateValidator<InternalAdGroup> {
        val isCpmShowsFrequency50Per30On = featureService.isEnabledForClientId(clientId, FeatureName.CPM_SHOWS_FREQUENCY_50_PER_30)
        return ModelWithInternalImpressionRateValidator(
            InternalAdGroup.RF, InternalAdGroup.RF_RESET,
            InternalAdGroup.MAX_CLICKS_COUNT, InternalAdGroup.MAX_CLICKS_PERIOD,
            InternalAdGroup.MAX_STOPS_COUNT, InternalAdGroup.MAX_STOPS_PERIOD,
            null,
            bannerWithCloseCounterVarIds)
    }


    fun parametrizedTestData_ForCheckImpressionRateCountValue(): List<List<Any?>> = listOf(
            listOf("less than min value", 0, greaterThan(0)),
            listOf("min value", 1, null),
            listOf("max value", CampaignConstants.MAX_IMPRESSION_RATE_COUNT_BANANA, null),
            listOf("greater than max value", CampaignConstants.MAX_IMPRESSION_RATE_COUNT_BANANA + 1,
                lessThanOrEqualTo(CampaignConstants.MAX_IMPRESSION_RATE_COUNT_BANANA)),
    )

    @Test
    @Parameters(method = "parametrizedTestData_ForCheckImpressionRateCountValue")
    @TestCaseName("checkImpressionRateCountValue: {0}")
    fun checkImpressionRateCountValue(@Suppress("UNUSED_PARAMETER") description: String,
                                      impressionRateCount: Int,
                                      defect: Defect<NumberDefectParams>?) {
        val campaign = getCampaign(impressionRateCount)
        val validationResult = getValidatorForCampaign().apply(campaign)

        if (defect != null) {
            assertThat(validationResult, hasDefectDefinitionWith(
                    validationError(path(field(InternalCampaignWithImpressionRate.IMPRESSION_RATE_COUNT)), defect)))
        } else {
            assertThat(validationResult, hasNoErrorsAndWarnings())
        }
    }

    fun parametrizedTestData_ForCheckImpressionRateIntervalDaysValue(): List<List<Any?>> = listOf(
            listOf("less than min value", 0, greaterThan(0)),
            listOf("min value", 1, null),
            listOf("max value", CampaignConstants.MAX_IMPRESSION_RATE_INTERVAL_DAYS_FOR_INTERNAL_CAMPAIGNS, null),
            listOf("greater than max value",
                    CampaignConstants.MAX_IMPRESSION_RATE_INTERVAL_DAYS_FOR_INTERNAL_CAMPAIGNS + 1,
                    lessThanOrEqualTo(CampaignConstants.MAX_IMPRESSION_RATE_INTERVAL_DAYS_FOR_INTERNAL_CAMPAIGNS)),
    )

    @Test
    @Parameters(method = "parametrizedTestData_ForCheckImpressionRateIntervalDaysValue")
    @TestCaseName("checkImpressionRateIntervalDaysValue: {0}")
    fun checkImpressionRateIntervalDaysValue(@Suppress("UNUSED_PARAMETER") description: String,
                                             intervalDays: Int,
                                             defect: Defect<NumberDefectParams>?) {
        val campaign = getCampaign(impressionRateIntervalDays = intervalDays)
        val validationResult = getValidatorForCampaign().apply(campaign)

        if (defect != null) {
            assertThat(validationResult, hasDefectDefinitionWith(validationError(
                    path(field(InternalCampaignWithImpressionRate.IMPRESSION_RATE_INTERVAL_DAYS)), defect)))
        } else {
            assertThat(validationResult, hasNoErrorsAndWarnings())
        }
    }

    fun parametrizedTestData_ForCheckMaxClicksCountValue(): List<List<Any?>> = listOf(
        listOf("less than min value", 0, greaterThan(0)),
        listOf("min value", 1, null),
        listOf("max value", CampaignConstants.MAX_IMPRESSION_RATE_COUNT_WITHOUT_FEATURE, null),
        listOf("greater than max value",
            CampaignConstants.MAX_IMPRESSION_RATE_COUNT_WITHOUT_FEATURE + 1,
            lessThanOrEqualTo(CampaignConstants.MAX_IMPRESSION_RATE_COUNT_WITHOUT_FEATURE)),
    )

    @Test
    @Parameters(method = "parametrizedTestData_ForCheckMaxClicksCountValue")
    @TestCaseName("checkMaxClicksCountValue: {0}")
    fun checkMaxClicksCountValue(@Suppress("UNUSED_PARAMETER") description: String,
        maxClicksCount: Int,
        defect: Defect<NumberDefectParams>?) {
        val campaign = getCampaign(maxClicksCount = maxClicksCount)
        val validationResult = getValidatorForCampaign().apply(campaign)

        if (defect != null) {
            assertThat(validationResult, hasDefectDefinitionWith(validationError(
                path(field(InternalCampaignWithImpressionRate.MAX_CLICKS_COUNT)), defect)))
        } else {
            assertThat(validationResult, hasNoErrorsAndWarnings())
        }
    }

    fun parametrizedTestData_ForCheckMaxClicksPeriodValue(): List<List<Any?>> = listOf(
        listOf("less than min value", 0, greaterThan(0)),
        listOf("min range value", 1, null),
        listOf("max range value", CampaignConstants.MAX_IMPRESSION_RATE_INTERVAL_DAYS_FOR_INTERNAL_CAMPAIGNS_IN_SECONDS, null),
        listOf("greater than max value",
            CampaignConstants.MAX_IMPRESSION_RATE_INTERVAL_DAYS_FOR_INTERNAL_CAMPAIGNS_IN_SECONDS + 1,
            lessThanOrEqualTo(CampaignConstants.MAX_IMPRESSION_RATE_INTERVAL_DAYS_FOR_INTERNAL_CAMPAIGNS_IN_SECONDS)),
        listOf("max value of integer", CampaignConstants.MAX_CLICKS_AND_STOPS_PERIOD_WHOLE_CAMPAIGN_VALUE, null)
    )

    @Test
    @Parameters(method = "parametrizedTestData_ForCheckMaxClicksPeriodValue")
    @TestCaseName("checkMaxClicksPeriodValue: {0}")
    fun checkMaxClicksPeriodValue(@Suppress("UNUSED_PARAMETER") description: String,
        maxClicksPeriod: Int,
        defect: Defect<NumberDefectParams>?) {
        val campaign = getCampaign(maxClicksPeriod = maxClicksPeriod)
        val validationResult = getValidatorForCampaign().apply(campaign)

        if (defect != null) {
            assertThat(validationResult, hasDefectDefinitionWith(validationError(
                path(field(InternalCampaignWithImpressionRate.MAX_CLICKS_PERIOD)), defect)))
        } else {
            assertThat(validationResult, hasNoErrorsAndWarnings())
        }
    }

    fun parametrizedTestData_ForCheckMaxClicksCountAndMaxClicksPeriodValuesTogether(): List<List<Any?>> = listOf(
        listOf("together null", null, null, null),
        listOf("together non null", 5, 5, null),
        listOf("count value is null, period value is non null", null, 5, inconsistentState()),
        listOf("count value is null, period value is non null", 5, null, inconsistentState()),
    )

    @Test
    @Parameters(method = "parametrizedTestData_ForCheckMaxClicksCountAndMaxClicksPeriodValuesTogether")
    @TestCaseName("checkMaxClicksCountAndMaxClicksPeriodValuesTogether: {0}")
    fun checkMaxClicksCountAndMaxClicksPeriodValuesTogether(@Suppress("UNUSED_PARAMETER") description: String,
        maxClicksCount: Int?,
        maxClicksPeriod: Int?,
        defect: Defect<NumberDefectParams>?) {
        val campaign = getCampaign(maxClicksCount = maxClicksCount, maxClicksPeriod = maxClicksPeriod)
        val validationResult = getValidatorForCampaign().apply(campaign)

        if (defect != null) {
            assertThat(validationResult, hasDefectDefinitionWith(validationError(path(), defect)))
        } else {
            assertThat(validationResult, hasNoErrorsAndWarnings())
        }
    }

    private fun getValidatorForCampaign(): ModelWithInternalImpressionRateValidator<InternalCampaignWithImpressionRate> {
        val isCpmShowsFrequency50Per30On = featureService.isEnabledForClientId(clientId, FeatureName.CPM_SHOWS_FREQUENCY_50_PER_30)
        return ModelWithInternalImpressionRateValidator(
            InternalCampaignWithImpressionRate.IMPRESSION_RATE_COUNT,
            InternalCampaignWithImpressionRate.IMPRESSION_RATE_INTERVAL_DAYS,
            InternalCampaignWithImpressionRate.MAX_CLICKS_COUNT,
            InternalCampaignWithImpressionRate.MAX_CLICKS_PERIOD,
            InternalCampaignWithImpressionRate.MAX_STOPS_COUNT,
            InternalCampaignWithImpressionRate.MAX_STOPS_PERIOD,
            null,
            emptyList(),
        )
    }

    private fun getCampaign(
        impressionRateCount: Int? = 1,
        impressionRateIntervalDays: Int? = 1,
        maxClicksCount: Int? = 1,
        maxClicksPeriod: Int? = 1,
        maxStopsCount: Int? = 1,
        maxStopsPeriod: Int? = 1
    ): InternalCampaignWithImpressionRate {
        return InternalFreeCampaign()
            .withImpressionRateCount(impressionRateCount)
            .withImpressionRateIntervalDays(impressionRateIntervalDays)
            .withMaxClicksCount(maxClicksCount)
            .withMaxClicksPeriod(maxClicksPeriod)
            .withMaxStopsCount(maxStopsCount)
            .withMaxStopsPeriod(maxStopsPeriod)
    }

}
