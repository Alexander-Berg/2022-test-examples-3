package ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmCampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects;
import ru.yandex.direct.core.validation.defects.MoneyDefects;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.defect.DateDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class CpmCampaignWithCustomStrategyBeforeApplyValidatorTest {

    public static final LocalDateTime NOW = LocalDateTime.now();

    @SuppressWarnings("unused")
    private static Object[] invalidParametrizedTestData() {
        return new Object[][]{
                {
                        "From autobudget_max_reach_custom_period to autobudget_max_reach_custom_period. " +
                                "Strategy changed only autoProlongation. " +
                                "Campaign finish equals now",
                        BigDecimal.valueOf(10_000),
                        CurrencyCode.RUB,
                        new CpmBannerCampaign()
                                .withEndDate(NOW.toLocalDate())
                                .withStartDate(NOW.toLocalDate())
                                .withId(RandomNumberUtils.nextPositiveLong())
                                .withStrategy((DbStrategy) new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                        new StrategyData()
                                                .withBudget(BigDecimal.valueOf(11_000))
                                                .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                                .withDailyChangeCount(3L)
                                                .withLastUpdateTime(NOW)
                                                .withAutoProlongation(0L)
                                                .withStart(NOW.toLocalDate().minusDays(3))
                                                .withFinish(NOW.toLocalDate()))),
                        NOW,
                        new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                new StrategyData()
                                        .withAutoProlongation(1L)
                                        .withBudget(BigDecimal.valueOf(11_000))
                                        .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                        .withStart(NOW.toLocalDate().minusDays(2))
                                        .withFinish(NOW.toLocalDate().minusDays(1))),
                        path(field(CpmCampaignWithCustomStrategy.STRATEGY.name()), field(DbStrategy.STRATEGY_DATA),
                                field(StrategyData.START)),
                        DateDefects.greaterThanOrEqualTo(NOW.toLocalDate())
                },
                {
                        "From autobudget_max_reach_custom_period to autobudget_max_reach_custom_period. " +
                                "Strategy changed" +
                                "Strategy start data before now",
                        BigDecimal.valueOf(10_000),
                        CurrencyCode.RUB,
                        new CpmBannerCampaign()
                                .withEndDate(NOW.toLocalDate().plusDays(2))
                                .withStartDate(NOW.toLocalDate())
                                .withId(RandomNumberUtils.nextPositiveLong())
                                .withStrategy((DbStrategy) new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                        new StrategyData()
                                                .withBudget(BigDecimal.valueOf(11_000))
                                                .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                                .withDailyChangeCount(3L)
                                                .withLastUpdateTime(NOW)
                                                .withAutoProlongation(0L)
                                                .withStart(NOW.toLocalDate().minusDays(1))
                                                .withFinish(NOW.toLocalDate().plusDays(2)))),
                        NOW,
                        new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                new StrategyData()
                                        .withAutoProlongation(1L)
                                        .withBudget(BigDecimal.valueOf(11_000))
                                        .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                        .withStart(NOW.toLocalDate().minusDays(2))
                                        .withFinish(NOW.toLocalDate().plusDays(2))),
                        path(field(CpmCampaignWithCustomStrategy.STRATEGY.name()), field(DbStrategy.STRATEGY_DATA),
                                field(StrategyData.START)),
                        DateDefects.greaterThanOrEqualTo(NOW.toLocalDate())
                },
                {
                        "From cpm_default strategy to autobudget_max_reach_custom_period. Budget less than min " +
                                "without restarting",
                        BigDecimal.valueOf(10_000),
                        CurrencyCode.RUB,
                        new CpmBannerCampaign()
                                .withId(RandomNumberUtils.nextPositiveLong())
                                .withStartDate(NOW.toLocalDate())
                                .withStrategy((DbStrategy) new DbStrategy()
                                .withStrategyName(StrategyName.CPM_DEFAULT)
                                .withStrategyData(new StrategyData()
                                        .withBudget(BigDecimal.valueOf(9_999))
                                        .withLastUpdateTime(NOW)
                                        .withDailyChangeCount(1L)
                                        .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                        .withStart(NOW.toLocalDate())
                                        .withFinish(NOW.toLocalDate().plusDays(10)))),
                        NOW,
                        new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                new StrategyData()
                                        .withBudget(BigDecimal.valueOf(9_999))
                                        .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                        .withStart(NOW.toLocalDate())
                                        .withFinish(NOW.toLocalDate().plusDays(10))),
                        path(field(CpmCampaignWithCustomStrategy.STRATEGY.name()), field(DbStrategy.STRATEGY_DATA),
                                field(StrategyData.BUDGET)),
                        MoneyDefects.invalidValueCpmNotLessThan(Money.valueOf(10_000, CurrencyCode.RUB))
                },
                {
                        "From cpm_default strategy to autobudget_max_reach_custom_period. Strategy change limit " +
                                "exceeded",
                        BigDecimal.valueOf(10_000),
                        CurrencyCode.RUB,
                        new CpmBannerCampaign()
                                .withId(RandomNumberUtils.nextPositiveLong())
                                .withStartDate(NOW.toLocalDate())
                                .withStrategy((DbStrategy) new DbStrategy()
                                .withStrategyName(StrategyName.CPM_DEFAULT)
                                .withStrategyData(new StrategyData()
                                        .withBudget(BigDecimal.valueOf(9_999))
                                        .withLastUpdateTime(NOW)
                                        .withDailyChangeCount(4L)
                                        .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                        .withStart(NOW.toLocalDate())
                                        .withFinish(NOW.toLocalDate().plusDays(10)))),
                        NOW,
                        new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                new StrategyData()
                                        .withBudget(BigDecimal.valueOf(10_000))
                                        .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                        .withStart(NOW.toLocalDate())
                                        .withFinish(NOW.toLocalDate().plusDays(10))),
                        path(field(CpmCampaignWithCustomStrategy.STRATEGY.name()), field(DbStrategy.STRATEGY_DATA)),
                        StrategyDefects.strategyChangingLimitWasExceeded(4)
                }
        };
    }

    @SuppressWarnings("unused")
    private static Object[] validParametrizedTestData() {
        return new Object[][]{
                {
                        "From autobudget_max_reach_custom_period to autobudget_max_reach_custom_period. " +
                                "Strategy changed only autoProlongation",
                        BigDecimal.valueOf(10_000),
                        CurrencyCode.RUB,
                        new CpmBannerCampaign()
                                .withEndDate(NOW.toLocalDate())
                                .withStartDate(NOW.toLocalDate().minusDays(1))
                                .withId(RandomNumberUtils.nextPositiveLong())
                                .withStrategy((DbStrategy) new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                        new StrategyData()
                                                .withBudget(BigDecimal.valueOf(11_000))
                                                .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                                .withDailyChangeCount(3L)
                                                .withLastUpdateTime(NOW)
                                                .withStart(NOW.toLocalDate())
                                                .withFinish(NOW.toLocalDate().plusDays(2)))),
                        NOW,
                        new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                new StrategyData()
                                        .withBudget(BigDecimal.valueOf(11_000))
                                        .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                        .withStart(NOW.toLocalDate())
                                        .withFinish(NOW.toLocalDate().plusDays(2)))
                },
                {
                        "From autobudget_max_reach_custom_period to autobudget_max_reach_custom_period. " +
                                "Strategy changed only autoProlongation and budget",
                        BigDecimal.valueOf(10_000),
                        CurrencyCode.RUB,
                        new CpmBannerCampaign()
                                .withEndDate(NOW.toLocalDate().plusDays(2))
                                .withStartDate(NOW.toLocalDate().minusDays(1))
                                .withId(RandomNumberUtils.nextPositiveLong())
                                .withStrategy((DbStrategy) new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                        new StrategyData()
                                                .withBudget(BigDecimal.valueOf(11_000))
                                                .withDailyChangeCount(3L)
                                                .withLastUpdateTime(NOW)
                                                .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                                .withStart(NOW.toLocalDate())
                                                .withFinish(NOW.toLocalDate().plusDays(2)))),
                        NOW,
                        new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                new StrategyData()
                                        .withBudget(BigDecimal.valueOf(12_000))
                                        .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                        .withStart(NOW.toLocalDate())
                                        .withFinish(NOW.toLocalDate().plusDays(2)))
                },
                {
                        "From autobudget_max_reach_custom_period to autobudget_max_impressions_custom_period. " +
                                "Counter is 4",
                        BigDecimal.valueOf(10_000),
                        CurrencyCode.RUB,
                        new CpmBannerCampaign()
                                .withEndDate(NOW.toLocalDate().plusDays(2))
                                .withStartDate(NOW.toLocalDate().minusDays(1))
                                .withId(RandomNumberUtils.nextPositiveLong())
                                .withStrategy((DbStrategy) new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                        new StrategyData()
                                                .withBudget(BigDecimal.valueOf(11_000))
                                                .withDailyChangeCount(4L)
                                                .withLastUpdateTime(NOW)
                                                .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                                .withStart(NOW.toLocalDate())
                                                .withFinish(NOW.toLocalDate().plusDays(2)))),
                        NOW,
                        new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                                .withStrategyData(
                                new StrategyData()
                                        .withBudget(BigDecimal.valueOf(12_000))
                                        .withName(CampaignsStrategyName.autobudget_max_impressions_custom_period.getLiteral())
                                        .withStart(NOW.toLocalDate())
                                        .withFinish(NOW.toLocalDate().plusDays(2)))
                },
                {
                        "From cpm_default strategy to autobudget_max_reach_custom_period. " +
                                "Strategy changed",
                        BigDecimal.valueOf(10_000),
                        CurrencyCode.RUB,
                        new CpmBannerCampaign()
                                .withEndDate(NOW.toLocalDate().plusDays(1))
                                .withStartDate(NOW.toLocalDate())
                                .withId(RandomNumberUtils.nextPositiveLong())
                                .withStrategy((DbStrategy) new DbStrategy()
                                .withStrategyName(StrategyName.CPM_DEFAULT)
                                .withStrategyData(new StrategyData().withName(CampaignsStrategyName.cpm_default.getLiteral()))),
                        NOW,
                        new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                new StrategyData()
                                        .withBudget(BigDecimal.valueOf(11_000))
                                        .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                        .withStart(NOW.toLocalDate())
                                        .withFinish(NOW.toLocalDate().plusDays(1)))
                },
                {
                        "From cpm_default strategy to autobudget_max_reach_custom_period. " +
                                "Max budget.",
                        BigDecimal.valueOf(10_000),
                        CurrencyCode.RUB,
                        new CpmBannerCampaign()
                                .withStartDate(NOW.toLocalDate())
                                .withEndDate(NOW.toLocalDate().plusDays(1))
                                .withId(RandomNumberUtils.nextPositiveLong())
                                .withStrategy((DbStrategy) new DbStrategy()
                                .withStrategyName(StrategyName.CPM_DEFAULT)
                                .withStrategyData(new StrategyData().withName(CampaignsStrategyName.cpm_default.getLiteral()))),
                        NOW,
                        new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                new StrategyData()
                                        .withBudget(BigDecimal.valueOf(200_000_000))
                                        .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                        .withStart(NOW.toLocalDate())
                                        .withFinish(NOW.toLocalDate().plusDays(1)))
                },
                {
                        "From cpm_default strategy to autobudget_max_reach_custom_period. " +
                                "Min budget with restarting",
                        BigDecimal.valueOf(99_000),
                        CurrencyCode.RUB,
                        new CpmBannerCampaign()
                                .withEndDate(NOW.toLocalDate().plusDays(1))
                                .withStartDate(NOW.toLocalDate())
                                .withId(RandomNumberUtils.nextPositiveLong())
                                .withStrategy((DbStrategy) new DbStrategy()
                                .withStrategyName(StrategyName.CPM_DEFAULT)
                                .withStrategyData(new StrategyData().withName(CampaignsStrategyName.cpm_default.getLiteral()))),
                        NOW,
                        new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                new StrategyData()
                                        .withBudget(BigDecimal.valueOf(600))
                                        .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                        .withStart(NOW.toLocalDate())
                                        .withFinish(NOW.toLocalDate().plusDays(1)))
                },
                {
                        "From cpm_default strategy to autobudget_max_reach_custom_period. " +
                                "Min budget without restarting",
                        BigDecimal.valueOf(10_000),
                        CurrencyCode.RUB,
                        new CpmBannerCampaign()
                                .withId(RandomNumberUtils.nextPositiveLong())
                                .withStartDate(NOW.toLocalDate())
                                .withStrategy((DbStrategy) new DbStrategy()
                                .withStrategyName(StrategyName.CPM_DEFAULT)
                                .withStrategyData(new StrategyData().withName(CampaignsStrategyName.cpm_default.getLiteral()))),
                        NOW,
                        new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                                .withStrategyData(
                                new StrategyData()
                                        .withBudget(BigDecimal.valueOf(10_000))
                                        .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                                        .withStart(NOW.toLocalDate().plusDays(1))
                                        .withFinish(NOW.toLocalDate().plusDays(10)))

                },
                {
                        "From cpm_default strategy to autobudget_max_impressions_custom_period",
                        BigDecimal.valueOf(10_000),
                        CurrencyCode.RUB,
                        new CpmBannerCampaign()
                                .withStartDate(NOW.toLocalDate())
                                .withId(RandomNumberUtils.nextPositiveLong())
                                .withStrategy((DbStrategy) new DbStrategy()
                                .withStrategyName(StrategyName.CPM_DEFAULT)
                                .withStrategyData(new StrategyData().withName(CampaignsStrategyName.cpm_default.getLiteral()))),
                        NOW,
                        new DbStrategy()
                                .withStrategyName(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                                .withStrategyData(
                                new StrategyData()
                                        .withBudget(BigDecimal.valueOf(11_000))
                                        .withName(CampaignsStrategyName.autobudget_max_impressions_custom_period.getLiteral())
                                        .withStart(NOW.toLocalDate().plusDays(1))
                                        .withFinish(NOW.toLocalDate().plusDays(10)))

                },
        };
    }

    @Test
    @Parameters(method = "validParametrizedTestData")
    @TestCaseName("description = {0}")
    public void checkStrategy_HasNoErrors(@SuppressWarnings("unused") String testDescription,
                                          BigDecimal minimalBudgetForCustomPeriod,
                                          CurrencyCode currencyCode,
                                          CpmCampaignWithCustomStrategy oldCampaign,
                                          LocalDateTime now,
                                          DbStrategy strategy) {
        ModelChanges<CpmCampaignWithCustomStrategy> mc =
                new ModelChanges<>(oldCampaign.getId(), CpmCampaignWithCustomStrategy.class);
        mc.process(strategy, CpmCampaignWithCustomStrategy.STRATEGY);

        var vr = CpmCampaignWithCustomStrategyBeforeApplyValidator.build(minimalBudgetForCustomPeriod, currencyCode,
                oldCampaign, now.toLocalDate())
                .apply(mc);
        assertThat(vr).
                is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    @Parameters(method = "invalidParametrizedTestData")
    @TestCaseName("description = {0}")
    public void checkStrategy_Error(@SuppressWarnings("unused") String testDescription,
                                    BigDecimal minimalBudgetForCustomPeriod,
                                    CurrencyCode currencyCode,
                                    CpmCampaignWithCustomStrategy oldCampaign,
                                    LocalDateTime now,
                                    DbStrategy strategy,
                                    Path expectedPath,
                                    Defect expectedDefectType
    ) {
        ModelChanges<CpmCampaignWithCustomStrategy> mc =
                new ModelChanges<>(oldCampaign.getId(), CpmCampaignWithCustomStrategy.class);
        mc.process(strategy, CpmCampaignWithCustomStrategy.STRATEGY);

        var vr = CpmCampaignWithCustomStrategyBeforeApplyValidator.build(minimalBudgetForCustomPeriod, currencyCode,
                oldCampaign, now.toLocalDate())
                .apply(mc);
        assertThat(vr).
                is(matchedBy(hasDefectWithDefinition(
                        validationError(expectedPath,
                                expectedDefectType))));
    }

    public static ModelChanges<CpmCampaignWithCustomStrategy> createMc(Long id, LocalDate startDate,
                                                                       LocalDate endDate) {
        ModelChanges<CpmCampaignWithCustomStrategy> mc =
                new ModelChanges<>(id, CpmCampaignWithCustomStrategy.class);

        mc.process(startDate, CpmCampaignWithCustomStrategy.START_DATE);
        mc.process(endDate, CpmCampaignWithCustomStrategy.END_DATE);

        return mc;
    }
}
