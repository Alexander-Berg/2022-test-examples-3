package ru.yandex.cs.billing.tms;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.cs.billing.AbstractCsBillingTmsFunctionalTest;
import ru.yandex.cs.billing.CsBillingCoreConstants;
import ru.yandex.cs.billing.billing.BillingServiceSql;
import ru.yandex.cs.billing.billing.Money;
import ru.yandex.cs.billing.history.impl.DbActionContext;
import ru.yandex.cs.billing.history.impl.HistoryServiceSql;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@DbUnitDataSet(
        before = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/before.csv",
        dataSource = "csBillingDataSource"
)
class BillingExecutorFunctionalTest extends AbstractCsBillingTmsFunctionalTest {

    @Autowired
    private BillingExecutor billingExecutor;
    @Autowired
    private BillingServiceSql billingServiceSql;
    @Autowired
    private HistoryServiceSql historySql;
    @Autowired
    private NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate;
    @Autowired
    private Clock clock;

    @BeforeEach
    void beforeEach() {
        doReturn(Money.valueOf(1L))
                .when(billingServiceSql)
                .getAvgSpending(anyInt(), anyLong(), any(LocalDateTime.class));
        doReturn(mock(DbActionContext.class))
                .when(historySql)
                .createActionContext(any());
        when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.now()));
    }

    @DisplayName("Открывает финансовый катоф при недостатке средств на счету кампании на следующий период")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testOpenCutoff/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testOpenCutoff() {
        doReturn(createMultimap(Map.of(1L, List.of(LocalDateTime.of(2019, 5, 31, 0, 0)))))
                .when(billingServiceSql).findCampaignsNeedingBilling(eq(CsBillingCoreConstants.VENDOR_SERVICE_ID));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @DisplayName("Закрывает финансовый катоф при достаточном количестве средств на следующий период")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testCloseCutoff/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCloseCutoff() {
        doReturn(createMultimap(Map.of(1L, List.of(LocalDateTime.of(2019, 4, 30, 0, 0)))))
                .when(billingServiceSql).findCampaignsNeedingBilling(eq(CsBillingCoreConstants.VENDOR_SERVICE_ID));
        LocalDateTime testCaseNow = LocalDateTime.of(2019, 5, 1, 0, 0, 0);
        when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        LocalDateTime to = csBillingNamedParameterJdbcTemplate.queryForObject(
                "SELECT TO_TIME FROM CS_BILLING.DS_CUTOFF WHERE CS_ID = :serviceId AND ID = :id",
                new MapSqlParameterSource()
                        .addValue("serviceId", CsBillingCoreConstants.VENDOR_SERVICE_ID)
                        .addValue("id", 1L),
                LocalDateTime.class
        );
        Assertions.assertEquals(testCaseNow, to);
    }

    @DisplayName("Не открывает финансовый катоф при достаточном кол-ве средств")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testDoNotOpenCutoffCampaignBalanceGreaterThenTariffMinCost/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testDoNotOpenCutoffCampaignBalanceGreaterThenTariffMinCost/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testDoNotOpenCutoffCampaignBalanceGreaterThenTariffMinCost() {
        doReturn(createMultimap(Map.of(1L, List.of(LocalDateTime.of(2019, 5, 31, 0, 0)))))
                .when(billingServiceSql).findCampaignsNeedingBilling(eq(CsBillingCoreConstants.VENDOR_SERVICE_ID));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @DisplayName("Не открывает финансовый катоф при достаточном кол-ве средств (кастомный тариф)")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testDoNotOpenCutoffCampaignBalanceGreaterThenDynamicCost/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testDoNotOpenCutoffCampaignBalanceGreaterThenDynamicCost/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testDoNotOpenCutoffCampaignBalanceGreaterThenDynamicCost() {
        doReturn(createMultimap(Map.of(1L, List.of(LocalDateTime.of(2019, 5, 31, 0, 0)))))
                .when(billingServiceSql).findCampaignsNeedingBilling(eq(CsBillingCoreConstants.ANALYTICS_SERVICE_ID));
        doReturn(Optional.of(3L))
                .when(billingServiceSql)
                .billCampaign(anyInt(), anyLong());
        billingExecutor.doJob(CsBillingCoreConstants.ANALYTICS_SERVICE_ID, null);
    }

    @DisplayName("Не открывать финансовый катоф при недостаточном кол-ве средств, т.к. он уже существует")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testDoNotOpenCutoffAlreadyExists/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testDoNotOpenCutoffAlreadyExists/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testDoNotOpenCutoffAlreadyExists() {
        doReturn(createMultimap(Map.of(1L, List.of(LocalDateTime.of(2019, 5, 31, 0, 0)))))
                .when(billingServiceSql).findCampaignsNeedingBilling(eq(CsBillingCoreConstants.VENDOR_SERVICE_ID));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testCutoffCampaignWithMonthlyBilledTariffNotEnoughForMinTariffChangedToCostlyTariffCurrentPeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testCutoffCampaignWithMonthlyBilledTariffNotEnoughForMinTariffChangedToCostlyTariffCurrentPeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Открыть катофф для кампании с ежемесячным обиливанием при изменении тарифа в текущем периоде и нехватке средств до минимального тарифа")
    void testCutoffCampaignWithMonthlyBilledTariffNotEnoughForMinTariffChangedToCostlyTariffCurrentPeriod() {
        doReturn(createMultimap(Map.of(1L, List.of(LocalDateTime.of(2019, 12, 1, 0, 0)))))
                .when(billingServiceSql).findCampaignsNeedingBilling(eq(CsBillingCoreConstants.VENDOR_SERVICE_ID));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testDontCutoffCampaignWithMonthlyBilledTariffEnoughForMinTariffChangedToCostlyTariffCurrentPeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testDontCutoffCampaignWithMonthlyBilledTariffEnoughForMinTariffChangedToCostlyTariffCurrentPeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не открывать катофф для кампании с ежемесячным обиливанием при изменении тарифа в текущем периоде и нехватке средств до минимального тарифа")
    void testDontCutoffCampaignWithMonthlyBilledTariffEnoughForMinTariffChangedToCostlyTariffCurrentPeriod() {
        doReturn(createMultimap(Map.of(1L, List.of(LocalDateTime.of(2019, 12, 1, 0, 0)))))
                .when(billingServiceSql).findCampaignsNeedingBilling(eq(CsBillingCoreConstants.VENDOR_SERVICE_ID));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testCutoffCampaignWithDailyBilledTariffNotEnoughForMinTariffChangedToCostlyTariffCurrentPeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testCutoffCampaignWithDailyBilledTariffNotEnoughForMinTariffChangedToCostlyTariffCurrentPeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Открыть катофф для кампании с ежедневным обиливанием при изменении тарифа в текущем периоде и нехватке средств до минимального тарифа")
    void testCutoffCampaignWithDailyBilledTariffNotEnoughForMinTariffChangedToCostlyTariffCurrentPeriod() {
        doReturn(createMultimap(Map.of(1L, List.of(LocalDateTime.of(2019, 11, 1, 0, 0)))))
                .when(billingServiceSql).findCampaignsNeedingBilling(eq(CsBillingCoreConstants.VENDOR_SERVICE_ID));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testDontCutoffCampaignWithDailyBilledTariffEnoughForMinTariffChangedToCostlyTariffCurrentPeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testDontCutoffCampaignWithDailyBilledTariffEnoughForMinTariffChangedToCostlyTariffCurrentPeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не открывать катофф для кампании с ежедневным обиливанием при изменении тарифа в текущем периоде и нехватке средств до минимального тарифа")
    void testDontCutoffCampaignWithDailyBilledTariffEnoughForMinTariffChangedToCostlyTariffCurrentPeriod() {
        doReturn(createMultimap(Map.of(1L, List.of(LocalDateTime.of(2019, 11, 2, 0, 0)))))
                .when(billingServiceSql).findCampaignsNeedingBilling(eq(CsBillingCoreConstants.VENDOR_SERVICE_ID));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testCutoffCampaignWithMonthlyBilledTariffNotEnoughForMinTariffChangedToCheaperTariffCurrentPeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testCutoffCampaignWithMonthlyBilledTariffNotEnoughForMinTariffChangedToCheaperTariffCurrentPeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Открыть катофф для кампании с ежемесячным обиливанием при изменении тарифа в текущем периоде и нехватке средств до минимального тарифа")
    void testCutoffCampaignWithMonthlyBilledTariffNotEnoughForMinTariffChangedToCheaperTariffCurrentPeriod() {
        doReturn(createMultimap(Map.of(1L, List.of(LocalDateTime.of(2019, 12, 1, 0, 0)))))
                .when(billingServiceSql).findCampaignsNeedingBilling(eq(CsBillingCoreConstants.VENDOR_SERVICE_ID));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testDontCutoffCampaignWithMonthlyBilledTariffEnoughForMinTariffChangedToCheaperTariffCurrentPeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testDontCutoffCampaignWithMonthlyBilledTariffEnoughForMinTariffChangedToCheaperTariffCurrentPeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не открывать катофф для кампании с ежемесячным обиливанием при изменении тарифа в текущем периоде и нехватке средств до минимального тарифа")
    void testDontCutoffCampaignWithMonthlyBilledTariffEnoughForMinTariffChangedToCheaperTariffCurrentPeriod() {
        doReturn(createMultimap(Map.of(1L, List.of(LocalDateTime.of(2019, 12, 1, 0, 0)))))
                .when(billingServiceSql).findCampaignsNeedingBilling(eq(CsBillingCoreConstants.VENDOR_SERVICE_ID));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testCutoffCampaignWithDailyBilledTariffNotEnoughForMinTariffChangedToCheaperTariffCurrentPeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testCutoffCampaignWithDailyBilledTariffNotEnoughForMinTariffChangedToCheaperTariffCurrentPeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Открыть катофф для кампании с ежедневным обиливанием при изменении тарифа в текущем периоде и нехватке средств до минимального тарифа")
    void testCutoffCampaignWithDailyBilledTariffNotEnoughForMinTariffChangedToCheaperTariffCurrentPeriod() {
        doReturn(createMultimap(Map.of(1L, List.of(LocalDateTime.of(2019, 11, 1, 0, 0)))))
                .when(billingServiceSql).findCampaignsNeedingBilling(eq(CsBillingCoreConstants.VENDOR_SERVICE_ID));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testDontCutoffCampaignWithDailyBilledTariffEnoughForMinTariffChangedToCheaperTariffCurrentPeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExecutorFunctionalTest/testDontCutoffCampaignWithDailyBilledTariffEnoughForMinTariffChangedToCheaperTariffCurrentPeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Не открывать катофф для кампании с ежедневным обиливанием при изменении тарифа в текущем периоде и нехватке средств до минимального тарифа")
    void testDontCutoffCampaignWithDailyBilledTariffEnoughForMinTariffChangedToCheaperTariffCurrentPeriod() {
        doReturn(createMultimap(Map.of(1L, List.of(LocalDateTime.of(2019, 11, 2, 0, 0)))))
                .when(billingServiceSql).findCampaignsNeedingBilling(eq(CsBillingCoreConstants.VENDOR_SERVICE_ID));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    private static MultiMap<Long, Date> createMultimap(Map<Long, List<LocalDateTime>> originalMap) {
        return originalMap.entrySet()
                .stream()
                .collect(
                        MultiMap::new,
                        (multiMap, entry) -> multiMap.put(entry.getKey(), mapLocalDateTime(entry.getValue())),
                        (map1, map2) -> {}
                );
    }

    private static List<Date> mapLocalDateTime(List<LocalDateTime> dateTimes) {
        return dateTimes.stream()
                .map(TimeUtil::toDate)
                .collect(Collectors.toList());
    }
}
