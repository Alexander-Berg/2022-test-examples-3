package ru.yandex.direct.grid.processing.service.constant;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdCampaignAttributionModel;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudget;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudgetPeriod;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyAvgCpm;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyManual;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyRoi;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyType;
import ru.yandex.direct.grid.model.campaign.strategy.GdStrategyOptimizeClicks;
import ru.yandex.direct.grid.model.campaign.strategy.GdStrategyOptimizeConversions;
import ru.yandex.direct.grid.model.campaign.strategy.GdStrategyOptimizeInstalls;
import ru.yandex.direct.grid.model.campaign.strategy.GdStrategyType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.constants.GdCampaignDefaultValues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.getCampaignDefaultValues;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.DEFAULT_TIMEZONE;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.UA_DEFAULT_TIMEZONE;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class DefaultValuesUtilsTest {

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    PpcProperty<Boolean> crossDeviceDefaultAttributionTypeEnabled;

    private CampaignConstantsService campaignConstantsService;

    private static CampaignAttributionModel defaultAttributionModel;

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));

        doReturn(crossDeviceDefaultAttributionTypeEnabled).when(ppcPropertiesSupport).get(PpcPropertyNames.CROSS_DEVICE_DEFAULT_ATTRIBUTION_TYPE_ENABLED, Duration.ofMinutes(1));
        campaignConstantsService = new CampaignConstantsService(ppcPropertiesSupport);

        doReturn(false).when(crossDeviceDefaultAttributionTypeEnabled).getOrDefault(false);
        defaultAttributionModel = campaignConstantsService.getDefaultAttributionModel();
    }

    @After
    public void afterTest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void testDefaultTimezoneForRuDomain() {
        doReturn("yandex.ru").when(httpServletRequest).getServerName();
        var timetarget = defaultGdTimeTarget();
        assertThat(timetarget.getIdTimeZone()).isEqualTo(DEFAULT_TIMEZONE);
    }

    @Test
    public void testDefaultTimezoneForUaDomain() {
        doReturn("yandex.ua").when(httpServletRequest).getServerName();
        var timetarget = defaultGdTimeTarget();
        assertThat(timetarget.getIdTimeZone()).isEqualTo(UA_DEFAULT_TIMEZONE);
    }

    @Test
    public void testDefaultTimezoneForTurkishDomain() {
        doReturn("yandex.com.tr").when(httpServletRequest).getServerName();
        var timetarget = defaultGdTimeTarget();
        assertThat(timetarget.getIdTimeZone()).isEqualTo(DEFAULT_TIMEZONE);
    }

    @Test
    public void testDefaultTimezoneForSomeOtherDomain() {
        doReturn("somedomain.com").when(httpServletRequest).getServerName();
        var timetarget = defaultGdTimeTarget();
        assertThat(timetarget.getIdTimeZone()).isEqualTo(DEFAULT_TIMEZONE);
    }

    @Test
    public void testDefaultValuesSpecificForCpmPrice() {
        var expectedDefaultValues = new GdCampaignDefaultValues()
                .withStartDate(LocalDate.now())
                .withEndDate(LocalDate.now())
                .withPricePackageId(0L);
        var actualDefaultValues = getCampaignDefaultValues(List.of(GdCampaignType.CPM_PRICE),
                Set.of(FeatureName.DEFAULT_AUTOBUDGET_AVG_CPA.name()), Set.of(), null, defaultAttributionModel).get(0);

        assertThat(actualDefaultValues).is(matchedBy(
                beanDiffer(expectedDefaultValues).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
        ));
    }

    @Test
    public void testDefaultValuesSpecificForInternalCampaigns() {
        var actualDefaultValues = getCampaignDefaultValues(List.of(
                        GdCampaignType.INTERNAL_FREE, GdCampaignType.INTERNAL_DISTRIB,
                        GdCampaignType.INTERNAL_AUTOBUDGET),
                Collections.emptySet(), Set.of(), null, defaultAttributionModel);

        assertThat(actualDefaultValues)
                .extracting(GdCampaignDefaultValues::getAttributionModel)
                .containsExactly(GdCampaignAttributionModel.LAST_SIGNIFICANT_CLICK,
                        GdCampaignAttributionModel.LAST_SIGNIFICANT_CLICK,
                        GdCampaignAttributionModel.LAST_SIGNIFICANT_CLICK);
    }

    @Test
    public void testDefaultStrategyOptimizeConversionsIfFeatureIsSet() {
        var expectedStrategy = new GdStrategyOptimizeConversions()
                .withStrategyType(GdStrategyType.OPTIMIZE_CONVERSIONS)
                .withType(GdCampaignStrategyType.AVG_CPA)
                .withIsAutoBudget(true)
                .withPlatform(GdCampaignPlatform.BOTH);

        var actualDefaultValues = getCampaignDefaultValues(List.of(GdCampaignType.TEXT),
                Set.of(FeatureName.DEFAULT_AUTOBUDGET_AVG_CPA.getName()),
                Set.of(), null, defaultAttributionModel).get(0);

        assertSoftly(soft -> {
            soft.assertThat(actualDefaultValues.getStrategy()).isExactlyInstanceOf(GdStrategyOptimizeConversions.class);
            soft.assertThat((GdStrategyOptimizeConversions) actualDefaultValues.getStrategy()).is(matchedBy(
                    beanDiffer(expectedStrategy).useCompareStrategy(DefaultCompareStrategies.allFields())
            ));
        });
    }

    @Test
    public void testDefaultStrategyOptimizeClicksWithWeekBudgetIfFeatureIsSet() {
        var expectedStrategy = new GdStrategyOptimizeClicks()
                .withStrategyType(GdStrategyType.OPTIMIZE_CLICKS)
                .withType(GdCampaignStrategyType.AVG_CLICK)
                .withIsAutoBudget(true)
                .withBudget(new GdCampaignBudget()
                        .withSum(BigDecimal.ZERO)
                        .withPeriod(GdCampaignBudgetPeriod.WEEK)
                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                .withPlatform(GdCampaignPlatform.BOTH);

        var actualDefaultValues = getCampaignDefaultValues(List.of(GdCampaignType.TEXT),
                Set.of(FeatureName.DEFAULT_AUTOBUDGET_AVG_CLICK_WITH_WEEK_BUDGET.getName()),
                Set.of(), null, defaultAttributionModel).get(0);

        assertSoftly(soft -> {
            soft.assertThat(actualDefaultValues.getStrategy()).isExactlyInstanceOf(GdStrategyOptimizeClicks.class);
            soft.assertThat((GdStrategyOptimizeClicks) actualDefaultValues.getStrategy()).is(matchedBy(
                    beanDiffer(expectedStrategy).useCompareStrategy(DefaultCompareStrategies.allFields())
            ));
        });
    }

    @Test
    public void testDefaultStrategyRoiIfFeatureIsSet() {
        var expectedStrategy = new GdCampaignStrategyRoi()
                .withStrategyType(GdStrategyType.ROI)
                .withType(GdCampaignStrategyType.ROI)
                .withIsAutoBudget(true)
                .withReserveReturn(100L)
                .withPlatform(GdCampaignPlatform.BOTH);

        var actualDefaultValues = getCampaignDefaultValues(List.of(GdCampaignType.TEXT),
                Set.of(FeatureName.DEFAULT_AUTOBUDGET_ROI.getName()), Set.of(), null, defaultAttributionModel).get(0);

        assertSoftly(soft -> {
            soft.assertThat(actualDefaultValues.getStrategy()).isExactlyInstanceOf(GdCampaignStrategyRoi.class);
            soft.assertThat((GdCampaignStrategyRoi) actualDefaultValues.getStrategy()).is(matchedBy(
                    beanDiffer(expectedStrategy).useCompareStrategy(DefaultCompareStrategies.allFields())
            ));
        });
    }

    @Test
    public void testDefaultStrategyOptimizeClicksPriorityWhenTwoFeaturesIsSet() {
        var expectedStrategy = new GdStrategyOptimizeConversions()
                .withStrategyType(GdStrategyType.OPTIMIZE_CONVERSIONS)
                .withType(GdCampaignStrategyType.AVG_CPA)
                .withIsAutoBudget(true)
                .withPlatform(GdCampaignPlatform.BOTH);

        var actualDefaultValues = getCampaignDefaultValues(List.of(GdCampaignType.TEXT),
                Set.of(FeatureName.DEFAULT_AUTOBUDGET_AVG_CLICK_WITH_WEEK_BUDGET.getName(),
                        FeatureName.DEFAULT_AUTOBUDGET_AVG_CPA.getName()), Set.of(), null, defaultAttributionModel).get(0);

        assertSoftly(soft -> {
            soft.assertThat(actualDefaultValues.getStrategy()).isExactlyInstanceOf(GdStrategyOptimizeConversions.class);
            soft.assertThat((GdStrategyOptimizeConversions) actualDefaultValues.getStrategy()).is(matchedBy(
                    beanDiffer(expectedStrategy).useCompareStrategy(DefaultCompareStrategies.allFields())
            ));
        });
    }

    @Test
    public void testDefaultStrategyOptimizeClicksPriorityWhenAllFeaturesIsSet() {
        var expectedStrategy = new GdCampaignStrategyRoi()
                .withStrategyType(GdStrategyType.ROI)
                .withType(GdCampaignStrategyType.ROI)
                .withIsAutoBudget(true)
                .withReserveReturn(100L)
                .withPlatform(GdCampaignPlatform.BOTH);

        var actualDefaultValues = getCampaignDefaultValues(List.of(GdCampaignType.TEXT),
                Set.of(FeatureName.DEFAULT_AUTOBUDGET_ROI.getName(),
                        FeatureName.DEFAULT_AUTOBUDGET_AVG_CLICK_WITH_WEEK_BUDGET.getName(),
                        FeatureName.DEFAULT_AUTOBUDGET_AVG_CPA.getName()), Set.of(), null, defaultAttributionModel).get(0);

        assertSoftly(soft -> {
            soft.assertThat(actualDefaultValues.getStrategy()).isExactlyInstanceOf(GdCampaignStrategyRoi.class);
            soft.assertThat((GdCampaignStrategyRoi) actualDefaultValues.getStrategy()).is(matchedBy(
                    beanDiffer(expectedStrategy).useCompareStrategy(DefaultCompareStrategies.allFields())
            ));
        });
    }

    @Test
    public void testDefaultCpmMaxImpressionsCustomPeriodStrategy() {
        var expectedStrategy = new GdCampaignStrategyAvgCpm()
                .withPlatform(GdCampaignPlatform.CONTEXT)
                .withIsAutoBudget(true)
                .withType(GdCampaignStrategyType.CPM_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                .withStrategyType(GdStrategyType.CPM_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                .withAvgCpm(new BigDecimal(100))
                .withBudget(new GdCampaignBudget()
                        .withPeriod(GdCampaignBudgetPeriod.CUSTOM)
                        .withAutoProlongation(true)
                        .withSum(BigDecimal.ZERO)
                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT))
                .withDailyChangeCount(0L);

        var cpmYndxFrontpageDefaultValues = getCampaignDefaultValues(List.of(GdCampaignType.CPM_YNDX_FRONTPAGE),
                Set.of(), Set.of(), null, defaultAttributionModel).get(0);
        var cpmBannerDefaultValues = getCampaignDefaultValues(List.of(GdCampaignType.CPM_BANNER), Set.of(),
                Set.of(), null, defaultAttributionModel).get(0);

        assertSoftly(soft -> {
            soft.assertThat(cpmYndxFrontpageDefaultValues.getStrategy()).isExactlyInstanceOf(GdCampaignStrategyAvgCpm.class);
            soft.assertThat((GdCampaignStrategyAvgCpm) cpmYndxFrontpageDefaultValues.getStrategy()).is(matchedBy(
                    beanDiffer(expectedStrategy).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
            ));
            soft.assertThat(cpmBannerDefaultValues.getStrategy()).isExactlyInstanceOf(GdCampaignStrategyAvgCpm.class);
            soft.assertThat((GdCampaignStrategyAvgCpm) cpmBannerDefaultValues.getStrategy()).is(matchedBy(
                    beanDiffer(expectedStrategy).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
            ));
        });
    }

    @Test
    public void shouldReturnClickStrategyForDynamic_IfSimplifyStrategyFeatureIsEnabledAndProViewIsDisabled() {
        var expectedStrategy = new GdStrategyOptimizeClicks()
                .withPlatform(GdCampaignPlatform.SEARCH)
                .withIsAutoBudget(true)
                .withType(GdCampaignStrategyType.AVG_CLICK)
                .withStrategyType(GdStrategyType.OPTIMIZE_CLICKS);
        var features = Set.of(FeatureName.SIMPLIFIED_STRATEGY_VIEW_ENABLED.getName());

        var result = getCampaignDefaultValues(List.of(GdCampaignType.DYNAMIC), Set.of(), features, false,
                defaultAttributionModel)
                .get(0);

        assertSoftly(soft -> {
            soft.assertThat(result.getStrategy())
                    .isExactlyInstanceOf(GdStrategyOptimizeClicks.class);
            soft.assertThat((GdStrategyOptimizeClicks) result.getStrategy())
                    .is(matchedBy(
                            beanDiffer(expectedStrategy).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                    ));
        });
    }

    @Test
    public void shouldReturnManualStrategyForDynamic_IfSimplifyStrategyFeatureIsEnabledAndProViewIsEnabled() {
        var expectedStrategy = new GdCampaignStrategyManual()
                .withPlatform(GdCampaignPlatform.SEARCH)
                .withIsAutoBudget(false)
                .withType(GdCampaignStrategyType.DEFAULT)
                .withStrategyType(GdStrategyType.DEFAULT);
        var features = Set.of(FeatureName.SIMPLIFIED_STRATEGY_VIEW_ENABLED.getName());


        var result = getCampaignDefaultValues(List.of(GdCampaignType.DYNAMIC), Set.of(), features, true,
                defaultAttributionModel)
                .get(0);

        assertSoftly(soft -> {
            soft.assertThat(result.getStrategy())
                    .isExactlyInstanceOf(GdCampaignStrategyManual.class);
            soft.assertThat((GdCampaignStrategyManual) result.getStrategy())
                    .is(matchedBy(
                            beanDiffer(expectedStrategy).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                    ));
        });
    }

    @Test
    public void shouldReturnManualStrategyForDynamic_ifSimplifyStrategyFeatureIsDisabled() {
        var expectedStrategy = new GdCampaignStrategyManual()
                .withPlatform(GdCampaignPlatform.SEARCH)
                .withIsAutoBudget(false)
                .withType(GdCampaignStrategyType.DEFAULT)
                .withStrategyType(GdStrategyType.DEFAULT);
        Set<String> features = Set.of();

        var resultForPro = getCampaignDefaultValues(List.of(GdCampaignType.DYNAMIC), features, features, true,
                defaultAttributionModel)
                .get(0);
        var resultForNewbie = getCampaignDefaultValues(List.of(GdCampaignType.DYNAMIC), features, features, false,
                defaultAttributionModel)
                .get(0);

        assertSoftly(soft -> {
            soft.assertThat(resultForPro.getStrategy())
                    .isExactlyInstanceOf(GdCampaignStrategyManual.class);
            soft.assertThat((GdCampaignStrategyManual) resultForPro.getStrategy())
                    .is(matchedBy(
                            beanDiffer(expectedStrategy).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                    ));
        });
        assertSoftly(soft -> {
            soft.assertThat(resultForNewbie.getStrategy())
                    .isExactlyInstanceOf(GdCampaignStrategyManual.class);
            soft.assertThat((GdCampaignStrategyManual) resultForNewbie.getStrategy())
                    .is(matchedBy(
                            beanDiffer(expectedStrategy).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                    ));
        });
    }

    @Test
    public void checkInternalAutobudgetDefaultStrategy() {
        var actualDefaultValues = getCampaignDefaultValues(
                List.of(GdCampaignType.INTERNAL_AUTOBUDGET), Set.of(), Set.of(), null, defaultAttributionModel).get(0);
        assertThat(actualDefaultValues.getStrategy()).isExactlyInstanceOf(GdStrategyOptimizeClicks.class);
    }

    @Test
    public void checkMobileDefaultStrategy() {
        var actualDefaultValues = getCampaignDefaultValues(
                List.of(GdCampaignType.MOBILE_CONTENT), Set.of(), Set.of(), null, defaultAttributionModel).get(0);
        assertThat(actualDefaultValues.getStrategy()).isExactlyInstanceOf(GdStrategyOptimizeClicks.class);
    }

    @Test
    public void checkMobileDefaultStrategyIfFeatureIsSet() {
        var actualDefaultValues = getCampaignDefaultValues(
                List.of(GdCampaignType.MOBILE_CONTENT), Set.of(),
                Set.of(FeatureName.CPA_PAY_FOR_CONVERSIONS_MOBILE_APPS_ALLOWED.getName()), null,
                defaultAttributionModel).get(0);
        assertThat(actualDefaultValues.getStrategy()).isExactlyInstanceOf(GdStrategyOptimizeInstalls.class);
    }

    @Test
    public void checkDefaultAttributionType() {
        var actualDefaultValues = getCampaignDefaultValues(
                List.of(GdCampaignType.TEXT), Set.of(),
                Set.of(), null, defaultAttributionModel).get(0);
        assertThat(actualDefaultValues.getAttributionModel()).isEqualByComparingTo(GdCampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK);
    }

    @Test
    public void checkCrossDeviceDefaultAttributionType() {
        doReturn(true).when(crossDeviceDefaultAttributionTypeEnabled).getOrDefault(false);
        defaultAttributionModel = campaignConstantsService.getDefaultAttributionModel();
        var actualDefaultValues = getCampaignDefaultValues(
                List.of(GdCampaignType.TEXT), Set.of(),
                Set.of(), null, defaultAttributionModel).get(0);
        assertThat(actualDefaultValues.getAttributionModel()).isEqualByComparingTo(GdCampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE);
    }
}
