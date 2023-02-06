package ru.yandex.cs.billing.tms;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.cs.billing.CsBillingCoreConstants;
import ru.yandex.cs.billing.billing.BillingPeriod;
import ru.yandex.cs.billing.cutoff.model.DsCutoff;
import ru.yandex.cs.billing.cutoff.model.DsCutoffType;
import ru.yandex.cs.billing.cutoff.service.CutoffService;
import ru.yandex.cs.billing.tms.multi.MultiServiceExecutor;
import ru.yandex.cs.billing.util.PeriodType;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertNotNull;

@DbUnitDataSet(
        before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/before.csv",
        dataSource = "csBillingDataSource"
)
class FinanceCutoffExecutorExternalFunctionalTest extends AbstractCsBillingTmsExternalFunctionalTest {

    private final MultiServiceExecutor updateActualBalanceExecutor;
    private final FinanceCutoffExecutor financeCutoffExecutor;
    private final NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate;
    private final CutoffService cutoffService;
    private final Clock clock;

    @Autowired
    public FinanceCutoffExecutorExternalFunctionalTest(
            MultiServiceExecutor updateActualBalanceExecutor,
            FinanceCutoffExecutor financeCutoffExecutor,
            NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate,
            CutoffService cutoffService,
            Clock clock) {
        this.updateActualBalanceExecutor = updateActualBalanceExecutor;
        this.financeCutoffExecutor = financeCutoffExecutor;
        this.csBillingNamedParameterJdbcTemplate = csBillingNamedParameterJdbcTemplate;
        this.cutoffService = cutoffService;
        this.clock = clock;
    }

    @BeforeEach
    void beforeEach() {
        Mockito.doReturn(TimeUtil.toInstant(LocalDateTime.now()))
                .when(clock)
                .instant();
    }

    /**
     * Закрывает катофф:
     * <ul>
     *     <li>при положительном балансе</li>
     *     <li>при включённой кампании на текущий период</li>
     *     <li>датасоурс активен в текущем периоде</li>
     * </ul>
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testCloseFinanceCutoffForPositiveBalanceAtActiveDsInsidePeriod/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testCloseFinanceCutoffForPositiveBalanceAtActiveDsInsidePeriod() {
        Mockito.when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2019, Month.MAY, 3, 22, 33)));
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);

        DsCutoff dsCutoff = getVendorDsCutoff();
        assertNotNull(dsCutoff.getTo());
    }

    /**
     * Закрывает катофф:
     * <ul>
     *     <li>при положительном балансе</li>
     *     <li>при включённой кампании на текущий период</li>
     *     <li>датасоурс активен с предыдущего периода по текущий</li>
     * </ul>
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testCloseFinanceCutoffForPositiveBalanceAtActiveDsOutsidePeriod/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testCloseFinanceCutoffForPositiveBalanceAtActiveDsOutsidePeriod() {
        Mockito.when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2019, Month.MAY, 3, 22, 33)));

        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);

        DsCutoff dsCutoff = getVendorDsCutoff();
        assertNotNull(dsCutoff.getTo());
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseFinanceCutoffForPositiveBalanceAtNotActivePeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseFinanceCutoffForPositiveBalanceAtNotActivePeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не закрывает финансовый катофф (положительный баланс без включённой кампании на текущий период)")
    void testDontCloseFinanceCutoffForPositiveBalanceAtNotActivePeriod() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 5, 10, 11, 0, 0);
        Mockito.doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testCloseFinanceCutoffForTariffMinSum/before.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Закрывает финансовый катофф (баланса, с учётом пополнений, хватает на минимальную сумму тарифа)")
    void testCloseFinanceCutoffForTariffMinSum() {
        // assume that billing has been finished for the last day of previous month
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastDayOfPrevMonth = TimeUtil.truncateToDay(TimeUtil.oneMillisBefore(BillingPeriod.previousPeriod(PeriodType.MONTH, now).till()));
        csBillingNamedParameterJdbcTemplate.update("" +
                        "INSERT INTO CS_BILLING.CAMPAIGN_BILLING (ID, CS_ID, CAMPAIGN_ID, BILLED_DATE, SUM_SPENT, SUM_PAID, BALANCE) " +
                        "VALUES (CS_BILLING.S_CAMPAIGN_BILLING.NEXTVAL, 132, 1, :billedDate, 100500, 100500, 0)",
                singletonMap("billedDate", TimeUtil.toDate(lastDayOfPrevMonth))
        );

        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);

        final DsCutoff expectedDsCutoff = new DsCutoff(
                CsBillingCoreConstants.VENDOR_SERVICE_ID,
                1,
                1L,
                TimeUtil.toDate(LocalDateTime.of(2019, 5, 1, 0, 0)),
                TimeUtil.toDate(now),
                DsCutoffType.FINANCE,
                null
        );

        assertClosedDsCutoff(expectedDsCutoff, getVendorDsCutoff());
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testCloseFinanceCutoffForDynamicCost/before.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Закрывает финансовый катофф (баланса, с учётом пополнений, хватает на динамическую стоимость для кампании)")
    void testCloseFinanceCutoffForDynamicCost() {
        // assume that billing has been finished for the last day of previous month
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastDayOfPrevMonth =
                TimeUtil.truncateToDay(TimeUtil.oneMillisBefore(BillingPeriod.previousPeriod(PeriodType.MONTH, now).till()));
        csBillingNamedParameterJdbcTemplate.update("" +
                        "INSERT INTO CS_BILLING.CAMPAIGN_BILLING (ID, CS_ID, CAMPAIGN_ID, BILLED_DATE, SUM_SPENT, " +
                        "SUM_PAID, BALANCE) " +
                        "VALUES (CS_BILLING.S_CAMPAIGN_BILLING.NEXTVAL, 206, 4, :billedDate, 100500, 100500, 0)",
                singletonMap("billedDate", TimeUtil.toDate(lastDayOfPrevMonth))
        );

        financeCutoffExecutor.doJob(CsBillingCoreConstants.ANALYTICS_SERVICE_ID, null);

        final DsCutoff expectedDsCutoff = new DsCutoff(
                CsBillingCoreConstants.ANALYTICS_SERVICE_ID,
                1,
                4L,
                TimeUtil.toDate(LocalDateTime.of(2019, 5, 1, 0, 0)),
                TimeUtil.toDate(now),
                DsCutoffType.FINANCE,
                null
        );

        assertClosedDsCutoff(expectedDsCutoff, getAnalyticsDsCutoff());
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testCloseFinanceCutoffForTariffMinSumWithDelay/before.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Закрывает финансовый катофф (Баланса, с учётом пополнений, хватает на минимальную сумму тарифа. Но " +
            "пополнение произошло не сразу)")
    void testCloseFinanceCutoffForTariffMinSumWithDelay() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2020, 1, 15, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);

        final DsCutoff expectedDsCutoff = new DsCutoff(
                CsBillingCoreConstants.VENDOR_SERVICE_ID,
                1,
                3L,
                TimeUtil.toDate(LocalDateTime.of(2020, 1, 10, 0, 0)),
                TimeUtil.toDate(testCaseNow),
                DsCutoffType.FINANCE,
                null
        );

        DsCutoff dsCutoff = getVendorDsCutoff();

        assertClosedDsCutoff(expectedDsCutoff, dsCutoff);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseFinanceCutoffForTariffMinSumIfActualBalanceIsZero/before.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Закрывает финансовый катофф (баланса, с учётом пополнений, хватает на минимальную сумму тарифа, но " +
            "актуальный баланс нулевой)")
    void testDontCloseFinanceCutoffForTariffMinSumIfActualBalanceIsZero() {
        // assume that billing has been finished for the last day of previous month
        String pattern = "yyyy-MM-dd";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastDayOfPrevMonth = TimeUtil.oneMillisBefore(BillingPeriod.previousPeriod(PeriodType.MONTH, now).till());
        csBillingNamedParameterJdbcTemplate.update("" +
                        "INSERT INTO CS_BILLING.CAMPAIGN_BILLING (ID, CS_ID, CAMPAIGN_ID, BILLED_DATE, SUM_SPENT, SUM_PAID, BALANCE) " +
                        "VALUES (CS_BILLING.S_CAMPAIGN_BILLING.NEXTVAL, 132, 1, :billedDate, 100500, 100500, 0)",
                singletonMap("billedDate", TimeUtil.toDate(lastDayOfPrevMonth))
        );

        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);

        final DsCutoff expectedDsCutoff = new DsCutoff(
                CsBillingCoreConstants.VENDOR_SERVICE_ID,
                1,
                1L,
                TimeUtil.toDate(LocalDateTime.of(2019, 5, 1, 0, 0)),
                null,
                DsCutoffType.FINANCE,
                null
        );

        assertOpenDsCutoff(expectedDsCutoff, getVendorDsCutoff());
    }


    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseFinanceCutoffForTariffMinSum/before.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не закрывает финансовый катофф (с учётом пополнений, баланса не хватает на минимальную сумму тарифа)")
    void testDontCloseFinanceCutoffForTariffMinSum() {
        // assume that billing has been finished for the last day of previous month
        String pattern = "yyyy-MM-dd";
        LocalDateTime lastDayOfPrevMonth = TimeUtil.oneMillisBefore(BillingPeriod.previousPeriod(PeriodType.MONTH, LocalDateTime.now()).till());
        csBillingNamedParameterJdbcTemplate.update("" +
                        "INSERT INTO CS_BILLING.CAMPAIGN_BILLING (ID, CS_ID, CAMPAIGN_ID, BILLED_DATE, SUM_SPENT, SUM_PAID, BALANCE) " +
                        "VALUES (CS_BILLING.S_CAMPAIGN_BILLING.NEXTVAL, 132, 1, :billedDate, 100500, 100500, 0)",
                singletonMap("billedDate", TimeUtil.toDate(lastDayOfPrevMonth))
        );

        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);

        final DsCutoff expectedDsCutoff = new DsCutoff(
                CsBillingCoreConstants.VENDOR_SERVICE_ID,
                1,
                1L,
                TimeUtil.toDate(LocalDateTime.of(2019, 5, 1, 0, 0)),
                TimeUtil.toDate(LocalDateTime.now()),
                DsCutoffType.FINANCE,
                null
        );

        assertOpenDsCutoff(expectedDsCutoff, getVendorDsCutoff());
    }

    private DsCutoff getVendorDsCutoff() {
        return getDsCutoff(CsBillingCoreConstants.VENDOR_SERVICE_ID);
    }

    private DsCutoff getAnalyticsDsCutoff() {
        return getDsCutoff(CsBillingCoreConstants.ANALYTICS_SERVICE_ID);
    }

    private DsCutoff getDsCutoff(int serviceId) {
        return csBillingNamedParameterJdbcTemplate.query(
                "SELECT * FROM CS_BILLING.DS_CUTOFF WHERE CS_ID = :serviceId ",
                singletonMap("serviceId", serviceId),
                (rs, i) -> {
                    DsCutoff dsc = new DsCutoff();
                    dsc.setId(rs.getLong("id"));
                    dsc.setServiceId(rs.getInt("cs_id"));
                    dsc.setDatasourceId(rs.getInt("datasource_id"));
                    dsc.setFrom(rs.getTimestamp("from_time"));
                    dsc.setTo(rs.getTimestamp("to_time"));
                    dsc.setType(DsCutoffType.valueOf(rs.getInt("type_id")));
                    dsc.setCustomTag(rs.getString("CUSTOM_TAG"));
                    return dsc;
                }
        ).stream().findFirst().orElseThrow(IllegalStateException::new);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDoNotCloseFinanceCutoffForTariffMinSum/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDoNotCloseFinanceCutoffForTariffMinSum/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не закрывает финансовый катофф (не хватает баланса на минимальную сумму тарифа)")
    void testDoNotCloseFinanceCutoffForTariffMinSum() {
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testOpenFinanceCutoffForNotPositiveBalanceIfNotExists/before.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Открывает финансовый катофф (неположительный баланс кампании)")
    void testOpenFinanceCutoffForNotPositiveBalanceIfNotExists() {
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        cutoffService.findActive(CsBillingCoreConstants.VENDOR_SERVICE_ID)
                .forEach(this::assertActiveDsCutoff);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDoNotOpenFinanceCutoffForNotPositiveBalanceBecauseCutoffExists/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDoNotOpenFinanceCutoffForNotPositiveBalanceBecauseCutoffExists/after.csv",
            dataSource = "csBillingDataSource"

    )
    @DisplayName("Не открывает финансовый катофф (уже существует)")
    void testDoNotOpenFinanceCutoffForNotPositiveBalanceBecauseCutoffExists() {
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testCloseCutoffForNewlyCreatedCampaignWithDailyBilledTariff/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testCloseCutoffForNewlyCreatedCampaignWithDailyBilledTariff/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Закрыть катофф для созданой в этом периоде кампании с ежедневным обиливанием")
    void testCloseCutoffForNewlyCreatedCampaignWithDailyBilledTariff() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 12, 16, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseCutoffForNewlyCreatedCampaignWithDailyBilledTariffNotEnoughForMinTariff/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseCutoffForNewlyCreatedCampaignWithDailyBilledTariffNotEnoughForMinTariff/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не закрывать катофф для созданой в этом периоде кампании с ежедневным обиливанием")
    void testDontCloseCutoffForNewlyCreatedCampaignWithDailyBilledTariffNotEnoughForMinTariff() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 12, 16, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testCloseCutoffForNewlyCreatedCampaignWithMonthlyBilledTariff/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testCloseCutoffForNewlyCreatedCampaignWithMonthlyBilledTariff/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Закрыть катофф для созданой в этом периоде кампании с ежемесячным обиливанием")
    void testCloseCutoffForNewlyCreatedCampaignWithMonthlyBilledTariff() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 12, 16, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseCutoffForNewlyCreatedCampaignWithMonthlyBilledTariffNotEnoughForMinTariff/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseCutoffForNewlyCreatedCampaignWithMonthlyBilledTariffNotEnoughForMinTariff/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не закрывать катофф для созданой в этом периоде кампании с ежемесячным обиливанием")
    void testDontCloseCutoffForNewlyCreatedCampaignWithMonthlyBilledTariffNotEnoughForMinTariff() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 12, 16, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseCutoffCampaignWithMonthlyBilledTariffNotEnoughForMinTariffNoPaymentsCurrentPeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseCutoffCampaignWithMonthlyBilledTariffNotEnoughForMinTariffNoPaymentsCurrentPeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не закрывать катофф для кампании с ежемесячным обиливанием без оплат в текущем периоде")
    void testDontCloseCutoffCampaignWithMonthlyBilledTariffNotEnoughForMinTariffNoPaymentsCurrentPeriod() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 12, 16, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseExistingCutoffCampaignWithMonthlyBilledTariffNotEnoughForMinTariffChangedToCostlyTariffCurrentPeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseExistingCutoffCampaignWithMonthlyBilledTariffNotEnoughForMinTariffChangedToCostlyTariffCurrentPeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не закрывать существующий катофф для кампании с ежемесячным обиливанием при изменении тарифа в текущем периоде и нехватке средств до минимального тарифа")
    void testDontCloseExistingCutoffCampaignWithMonthlyBilledTariffNotEnoughForMinTariffChangedToCostlyTariffCurrentPeriod() {
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseExistingCutoffCampaignWithDailyBilledTariffNotEnoughForMinTariffChangedToCostlyTariffCurrentPeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseExistingCutoffCampaignWithDailyBilledTariffNotEnoughForMinTariffChangedToCostlyTariffCurrentPeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не открывать катофф для кампании с ежедневным обиливанием при изменении тарифа в текущем периоде и нехватке средств до минимального тарифа")
    void testDontCloseExistingCutoffCampaignWithDailyBilledTariffNotEnoughForMinTariffChangedToCostlyTariffCurrentPeriod() {
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseExistingCutoffCampaignWithMonthlyBilledTariffNotEnoughForMinTariffChangedToCheaperTariffCurrentPeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseExistingCutoffCampaignWithMonthlyBilledTariffNotEnoughForMinTariffChangedToCheaperTariffCurrentPeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не открывать катофф для кампании с ежемесячным обиливанием при изменении тарифа в текущем периоде и нехватке средств до минимального тарифа")
    void testDontCloseExistingCutoffCampaignWithMonthlyBilledTariffNotEnoughForMinTariffChangedToCheaperTariffCurrentPeriod() {
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseExistingCutoffCampaignWithDailyBilledTariffNotEnoughForMinTariffChangedToCheaperTariffCurrentPeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseExistingCutoffCampaignWithDailyBilledTariffNotEnoughForMinTariffChangedToCheaperTariffCurrentPeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не открывать катофф для кампании с ежедневным обиливанием при изменении тарифа в текущем периоде и нехватке средств до минимального тарифа")
    void testDontCloseExistingCutoffCampaignWithDailyBilledTariffNotEnoughForMinTariffChangedToCheaperTariffCurrentPeriod() {
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseExistingCutoffCampaignWithMonthlyBilledTariffEnoughForMinTariffMoreChargesThanChargeLimit/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseExistingCutoffCampaignWithMonthlyBilledTariffEnoughForMinTariffMoreChargesThanChargeLimit/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не закрывать катофф для кампании с лимитом расходов, т.к. расходы больше чем лимит")
    void testDontCloseExistingCutoffCampaignWithMonthlyBilledTariffEnoughForMinTariffMoreChargesThanChargeLimit() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 12, 16, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));

        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testCloseExistingCutoffCampaignWithMonthlyBilledTariffEnoughForMinTariffMoreChargesThanChargeLimit/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testCloseExistingCutoffCampaignWithMonthlyBilledTariffEnoughForMinTariffMoreChargesThanChargeLimit/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Закрывать катофф для кампании с лимитом расходов, т.к. расходы меньше чем лимита и баланс больше 0")
    void testCloseExistingCutoffCampaignWithMonthlyBilledTariffEnoughForMinTariffMoreChargesThanChargeLimit() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 12, 16, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));

        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testOpenCutoffCampaignWithMonthlyBilledChargesMoreThanChargeLimit/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testOpenCutoffCampaignWithMonthlyBilledChargesMoreThanChargeLimit/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Открыть катофф для кампании с лимитом расходов, т.к. расходы больше чем лимита и баланс больше 0")
    void testOpenCutoffCampaignWithMonthlyBilledChargesMoreThanChargeLimit() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 12, 16, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));

        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testOpenCutoffCampaignWithMonthlyBilledChargesMoreThanChargeLimit/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testOpenCutoffCampaignWithMonthlyBilledChargesMoreThanChargeLimit/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Открыть катофф для кампании с лимитом расходов, т.к. расходы больше чем лимита и баланс больше 0")
    void testDontOpenCutoffCampaignWithMonthlyBilledChargesLessThanChargeLimit() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 12, 16, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));

        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testOpenCutoffCampaignWithDailyBilledChargesMoreThanChargeLimit/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testOpenCutoffCampaignWithDailyBilledChargesMoreThanChargeLimit/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Открыть катофф для кампании с дневным лимитом расходов, т.к. расходы больше чем лимита и баланс больше 0")
    void testOpenCutoffCampaignWithDailyBilledChargesMoreThanChargeLimit() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 12, 16, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));

        updateActualBalanceExecutor.doJob(mockContext());
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontOpenCutoffCampaignWithDailyBilledChargesMoreThanChargeLimit/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontOpenCutoffCampaignWithDailyBilledChargesMoreThanChargeLimit/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не открывать катофф для кампании с дневным лимитом расходов, т.к. расходы меньше или равны чем лимит и баланс больше 0")
    void testDontOpenCutoffCampaignWithDailyBilledChargesMoreThanChargeLimit() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 12, 16, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));

        updateActualBalanceExecutor.doJob(mockContext());
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testCloseCutoffCampaignWithDailyBilledChargesMoreThanChargeLimit/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testCloseCutoffCampaignWithDailyBilledChargesMoreThanChargeLimit/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Закрыть катофф для кампании с дневным лимитом расходов, т.к. расходы меньше чем лимит и баланс больше 0")
    void testCloseCutoffCampaignWithDailyBilledChargesMoreThanChargeLimit() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 12, 16, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));

        updateActualBalanceExecutor.doJob(mockContext());
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseCutoffCampaignWithDailyBilledChargesMoreThanChargeLimit/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseCutoffCampaignWithDailyBilledChargesMoreThanChargeLimit/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не закрывать катофф для кампании с дневным лимитом расходов, т.к. расход больше или равен лимиту")
    void testDontCloseCutoffCampaignWithDailyBilledChargesMoreThanChargeLimit() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 12, 16, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));

        updateActualBalanceExecutor.doJob(mockContext());
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testCloseExistingCutoffCampaignMonthlyBilledEnoughForFlexibleTariffMin/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testCloseExistingCutoffCampaignMonthlyBilledEnoughForFlexibleTariffMin/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Закрывать катофф для кампании с гибкой минимальной суммой. Хватает на тариф")
    void testCloseExistingCutoffCampaignMonthlyBilledEnoughForFlexibleTariffMin() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 12, 16, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));

        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseExistingCutoffCampaignWithMonthlyBilledNotEnoughFlexibleTariff/before.csv",
            after = "/ru/yandex/cs/billing/tms/FinanceCutoffExecutorFunctionalTest/testDontCloseExistingCutoffCampaignWithMonthlyBilledNotEnoughFlexibleTariff/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не закрывать катофф для кампании с гибкой минимальной суммой. Не хватает на тариф")
    void testDontCloseExistingCutoffCampaignWithMonthlyBilledNotEnoughFlexibleTariff() {
        final LocalDateTime testCaseNow = LocalDateTime.of(2019, 12, 16, 11, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));

        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    private void assertActiveDsCutoff(DsCutoff dsCutoff) {
        Assertions.assertEquals(
                TimeUtil.truncateToDay(LocalDateTime.now()),
                TimeUtil.truncateToDay(TimeUtil.toLocalDateTime(dsCutoff.getFrom()))
        );
        Assertions.assertNull(dsCutoff.getTo());
    }

    private void assertClosedDsCutoff(DsCutoff expectedDsCutoff, DsCutoff closedDsCutoff) {
        Assertions.assertEquals(expectedDsCutoff.getId(), closedDsCutoff.getId());
        Assertions.assertEquals(expectedDsCutoff.getDatasourceId(), closedDsCutoff.getDatasourceId());
        Assertions.assertEquals(
                TimeUtil.truncateToDay(TimeUtil.toLocalDateTime(expectedDsCutoff.getFrom())),
                TimeUtil.truncateToDay(TimeUtil.toLocalDateTime(closedDsCutoff.getFrom()))
        );
        Assertions.assertEquals(
                TimeUtil.truncateToDay(TimeUtil.toLocalDateTime(expectedDsCutoff.getTo())),
                TimeUtil.truncateToDay(TimeUtil.toLocalDateTime(closedDsCutoff.getTo()))
        );
    }

    private void assertOpenDsCutoff(DsCutoff expectedDsCutoff, DsCutoff openDsCutoff) {
        Assertions.assertEquals(expectedDsCutoff.getId(), openDsCutoff.getId());
        Assertions.assertEquals(expectedDsCutoff.getDatasourceId(), openDsCutoff.getDatasourceId());
        Assertions.assertEquals(
                TimeUtil.truncateToDay(TimeUtil.toLocalDateTime(expectedDsCutoff.getFrom())),
                TimeUtil.truncateToDay(TimeUtil.toLocalDateTime(openDsCutoff.getFrom()))
        );
        Assertions.assertNull(openDsCutoff.getTo());
    }

    private JobExecutionContext mockContext() {
        final JobExecutionContext mockContext = Mockito.mock(JobExecutionContext.class);
        final JobDetail mockJobDetail = Mockito.mock(JobDetail.class);
        Mockito.when(mockContext.getJobDetail()).thenReturn(mockJobDetail);
        Mockito.when(mockJobDetail.getKey()).thenReturn(new JobKey("test"));
        return mockContext;
    }
}
