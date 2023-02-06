package ru.yandex.cs.billing.tms;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.tms.multi.MultiServiceExecutor;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitDataSets;

public class BillingExecutorCsBillingTmsExternalFunctionalTest extends AbstractCsBillingTmsExternalFunctionalTest {

    private final MultiServiceExecutor billingExecutor;
    private final MultiServiceExecutor financeCutoffExecutor;
    private final Clock clock;

    @Autowired
    public BillingExecutorCsBillingTmsExternalFunctionalTest(MultiServiceExecutor billingExecutor,
                                                             MultiServiceExecutor financeCutoffExecutor,
                                                             Clock clock) {
        this.billingExecutor = billingExecutor;
        this.financeCutoffExecutor = financeCutoffExecutor;
        this.clock = clock;
    }

    @Disabled
    @DisplayName("Биллим дневные рекмаги с 31 января по 31 марта")
    @DbUnitDataSets({
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedDailyWasActive/DATASOURCE.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedDailyWasActive/CAMPAIGN.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedDailyWasActive/CAMPAIGN_TARIFF.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedDailyWasActive/DS_CUTOFF_ARCHIVE.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedDailyWasActive/CLICKS_TRT.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedDailyWasActive/CAMPAIGN_PAYMENTS.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedDailyWasActive/CAMPAIGN_BALANCE.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedDailyWasActive/GET_CLICKS_LOG.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    after = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedDailyWasActive/CAMPAIGN_BILLING.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    after = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedDailyWasActive/CAMPAIGN_BALANCE_after.csv",
                    dataSource = "csBillingDataSource"
            )
    })
    @Test
    void testRecommendedDailyWasActive() {
        JobExecutionContext context = mockContext();
        for (LocalDateTime now = LocalDateTime.of(2020, Month.FEBRUARY, 1, 0, 0);
             now.isBefore(LocalDateTime.of(2020, Month.APRIL, 1, 10, 0));
             now = TimeUtil.nextDayStart(now)) {
            Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now.plusHours(1)));
            financeCutoffExecutor.doJob(context);
            billingExecutor.doJob(context);
            financeCutoffExecutor.doJob(context);
        }
    }

    @Disabled
    @DisplayName("Биллим месячные рекмаги с 31 января по 31 марта")
    @DbUnitDataSets({
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedMonthlyWasActive/DATASOURCE.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedMonthlyWasActive/CAMPAIGN.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedMonthlyWasActive/CAMPAIGN_TARIFF.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedMonthlyWasActive/DS_CUTOFF_ARCHIVE.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedMonthlyWasActive/CLICKS_TRT.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedMonthlyWasActive/CAMPAIGN_PAYMENTS.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedMonthlyWasActive/CAMPAIGN_BALANCE.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedMonthlyWasActive/GET_CLICKS_LOG.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedMonthlyWasActive/CAMPAIGN_BILLING_before.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedMonthlyWasActive/CAMPAIGN_CHARGES_before.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    after = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedMonthlyWasActive/CAMPAIGN_BILLING.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    after = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedMonthlyWasActive/DS_CUTOFF.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    after = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testRecommendedMonthlyWasActive/CAMPAIGN_BALANCE_after.csv",
                    dataSource = "csBillingDataSource"
            )
    })
    @Test
    void testRecommendedMonthlyWasActive() {
        JobExecutionContext context = mockContext();
        for (LocalDateTime now = LocalDateTime.of(2020, Month.FEBRUARY, 1, 0, 0);
             now.isBefore(LocalDateTime.of(2020, Month.APRIL, 1, 10, 0));
             now = TimeUtil.nextDayStart(now)) {
            Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now.plusHours(1)));
            financeCutoffExecutor.doJob(context);
            billingExecutor.doJob(context);
            financeCutoffExecutor.doJob(context);
        }
    }

    @Disabled
    @DisplayName("Биллим ставки на модели с 31 января по 31 марта")
    @DbUnitDataSets({
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testModelbids/DATASOURCE.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testModelbids/CAMPAIGN.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testModelbids/CAMPAIGN_TARIFF.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testModelbids/DS_CUTOFF_ARCHIVE.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testModelbids/CLICKS_TRT.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testModelbids/CAMPAIGN_PAYMENTS.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testModelbids/CAMPAIGN_BALANCE.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testModelbids/GET_CLICKS_LOG.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testModelbids/CAMPAIGN_BILLING_before.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testModelbids/CAMPAIGN_CHARGES_before.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    after = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testModelbids/CAMPAIGN_BILLING.csv",
                    dataSource = "csBillingDataSource"
            )
    })
    @Test
    void testModelbids() {
        JobExecutionContext context = mockContext();
        for (LocalDateTime now = LocalDateTime.of(2020, Month.FEBRUARY, 1, 0, 0);
             now.isBefore(LocalDateTime.of(2020, Month.APRIL, 1, 10, 0));
             now = TimeUtil.nextDayStart(now)) {
            Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now.plusHours(1)));
            financeCutoffExecutor.doJob(context);
            billingExecutor.doJob(context);
            financeCutoffExecutor.doJob(context);
        }
    }

    @DisplayName("Биллим брендзоны с 31 января по 31 марта")
    @DbUnitDataSets({
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testBrandzone/DATASOURCE.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testBrandzone/CAMPAIGN.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testBrandzone/CAMPAIGN_TARIFF.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testBrandzone/DS_CUTOFF_ARCHIVE.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testBrandzone/CAMPAIGN_PAYMENTS.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testBrandzone/CAMPAIGN_BALANCE.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    after = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testBrandzone/CAMPAIGN_BILLING.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    after = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testBrandzone/CAMPAIGN_CHARGES.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    after = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testBrandzone/DS_CUTOFF.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    after = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testBrandzone/CAMPAIGN_BALANCE_after.csv",
                    dataSource = "csBillingDataSource"
            )
    })
    @Test
    void testBrandzone() {
        JobExecutionContext context = mockContext();
        for (LocalDateTime now = LocalDateTime.of(2020, Month.FEBRUARY, 1, 0, 0);
             now.isBefore(LocalDateTime.of(2020, Month.APRIL, 1, 10, 0));
             now = TimeUtil.nextDayStart(now)) {
            Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now.plusHours(1)));
            billingExecutor.doJob(context);
            financeCutoffExecutor.doJob(context);
        }
    }

    @DisplayName("Биллим аналитику с 31 января по 31 марта")
    @DbUnitDataSets({
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testAnalytics/DATASOURCE.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testAnalytics/CAMPAIGN.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testAnalytics/CAMPAIGN_TARIFF.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testAnalytics/DS_CUTOFF_ARCHIVE.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testAnalytics/CAMPAIGN_PAYMENTS.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    before = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testAnalytics/CAMPAIGN_BALANCE.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    after = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testAnalytics/CAMPAIGN_BILLING.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    after = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testAnalytics/CAMPAIGN_CHARGES.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    after = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testAnalytics/DS_CUTOFF.csv",
                    dataSource = "csBillingDataSource"
            ),
            @DbUnitDataSet(
                    after = "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testAnalytics/CAMPAIGN_BALANCE_after.csv",
                    dataSource = "csBillingDataSource"
            )
    })
    @Test
    void testAnalytics() {
        JobExecutionContext context = mockContext();
        for (LocalDateTime now = LocalDateTime.of(2020, Month.FEBRUARY, 1, 0, 0);
             now.isBefore(LocalDateTime.of(2020, Month.APRIL, 1, 10, 0));
             now = TimeUtil.nextDayStart(now)) {
            Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now.plusHours(1)));
            billingExecutor.doJob(context);
            financeCutoffExecutor.doJob(context);
        }
    }

    @DbUnitDataSet(
            before = {
                    "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testOfferAnalytics/DATASOURCE.csv",
                    "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testOfferAnalytics/CAMPAIGN.csv",
                    "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testOfferAnalytics/CAMPAIGN_TARIFF.csv",
                    "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testOfferAnalytics/DS_CUTOFF_ARCHIVE.csv",
                    "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testOfferAnalytics/CAMPAIGN_PAYMENTS.csv",
                    "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testOfferAnalytics/CAMPAIGN_BALANCE.csv"
            },
            after = {
                    "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testOfferAnalytics/CAMPAIGN_BILLING.after.csv",
                    "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testOfferAnalytics/CAMPAIGN_CHARGES.after.csv",
                    "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testOfferAnalytics/DS_CUTOFF.after.csv",
                    "/ru/yandex/cs/billing/tms/BillingExecutorCsBillingTmsExternalFunctionalTest/testOfferAnalytics/CAMPAIGN_BALANCE.after.csv"
            },
            dataSource = "csBillingDataSource"
    )
    @Test
    @DisplayName("Биллим офертную аналитику с 31 января по 31 марта")
    void testOfferAnalytics() {
        JobExecutionContext context = mockContext();
        for (LocalDateTime now = LocalDateTime.of(2020, Month.FEBRUARY, 1, 0, 0);
             now.isBefore(LocalDateTime.of(2020, Month.APRIL, 1, 10, 0));
             now = TimeUtil.nextDayStart(now)
        ) {
            Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now.plusHours(1)));
            billingExecutor.doJob(context);
            financeCutoffExecutor.doJob(context);
        }
    }

    private JobExecutionContext mockContext() {
        final JobExecutionContext mockContext = Mockito.mock(JobExecutionContext.class);
        final JobDetail mockJobDetail = Mockito.mock(JobDetail.class);
        Mockito.when(mockContext.getJobDetail()).thenReturn(mockJobDetail);
        Mockito.when(mockJobDetail.getKey()).thenReturn(new JobKey("test"));
        return mockContext;
    }
}
