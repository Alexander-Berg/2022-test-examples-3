package ru.yandex.market.vendor;

import java.time.Clock;
import java.time.LocalDateTime;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.cs.placement.tms.notification.NotifyBrandzonePrepaidFinanceLowExecutor;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest extends AbstractCsPlacementTmsFunctionalTest {
    @Autowired
    private NotifyBrandzonePrepaidFinanceLowExecutor executor;
    @Autowired
    private Clock clock;
    @Autowired
    private WireMockServer csBillingApiMock;

    @DisplayName("Сохранено событие уведомления (менее 7 дней до отключения).")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNotificationPersistenceLessThan7Days/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNotificationPersistenceLessThan7Days/before.vendors.csv",
            after = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNotificationPersistenceLessThan7Days/after.csv",
            dataSource = "vendorDataSource"
    )
    void testNotificationPersistenceLessThan7Days() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testNotificationPersistenceLessThan7Days/retrofit2_response.json"))));

        LocalDateTime testCaseNow = LocalDateTime.of(2019, 9, 26, 0, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();
        executor.doJob(null);
    }

    @DisplayName("Не сохранено событие уведомления (менее 3 дней до отключения). Достаточно денег на следующий месяц, нет катоффов.")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testBalanceOkNoCutoffsLessThen3Days/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testBalanceOkNoCutoffsLessThen3Days/after.csv",
            dataSource = "vendorDataSource"
    )
    void testBalanceOkNoCutoffsLessThen3Days() {
        LocalDateTime testCaseNow = LocalDateTime.of(2019, 9, 29, 0, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();
        executor.doJob(null);
    }

    @DisplayName("Не сохранено событие уведомления (менее 7 дней до отключения). Достаточно денег на следующий месяц, нет катоффов.")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testBalanceOkNoCutoffsLessThen7Days/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testBalanceOkNoCutoffsLessThen7Days/after.csv",
            dataSource = "vendorDataSource"
    )
    void testBalanceOkNoCutoffsLessThen7Days() {
        LocalDateTime testCaseNow = LocalDateTime.of(2019, 9, 26, 0, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();
        executor.doJob(null);
    }

    @DisplayName("Сохранено событие уведомления (менее 3 дней до отключения). Нехватает денег до следующего тарифа, нет катоффов.")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNotificationPersistenceLessThan3Days/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNotificationPersistenceLessThan3Days/before.vendors.csv",
            after = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNotificationPersistenceLessThan3Days/after.csv",
            dataSource = "vendorDataSource"
    )
    void testNotificationPersistenceLessThan3Days() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testNotificationPersistenceLessThan3Days/retrofit2_response.json"))));

        LocalDateTime testCaseNow = LocalDateTime.of(2019, 9, 29, 0, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();
        executor.doJob(null);
    }

    @DisplayName("Не было сохранено событие уведомления (менее 3 дней до отключения, средств достаточно)")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNoNotificationWhenBalanceOk/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNoNotificationWhenBalanceOk/after.csv",
            dataSource = "vendorDataSource"
    )
    void testNoNotificationWhenBalanceOk() {
        LocalDateTime testCaseNow = LocalDateTime.of(2019, 9, 29, 0, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();
        executor.doJob(null);
    }

    @DisplayName(
            "Сохранено событие уведомления для выключенной БЗ (менее 3 дней до отключения). " +
            "Не хватает до тарифа, отложенное включение в следующем месяце."
    )
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testSavedWithNotEnoughBalanceAndPostponedActivationAtNextMonth/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testSavedWithNotEnoughBalanceAndPostponedActivationAtNextMonth/before.vendors.csv",
            after = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testSavedWithNotEnoughBalanceAndPostponedActivationAtNextMonth/after.csv",
            dataSource = "vendorDataSource"
    )
    void testSavedWithNotEnoughBalanceAndPostponedActivationAtNextMonth() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testSavedWithNotEnoughBalanceAndPostponedActivationAtNextMonth/retrofit2_response.json"))));

        LocalDateTime testCaseNow = LocalDateTime.of(2019, 9, 29, 0, 0, 0);
        when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        executor.doJob(null);
    }

    @DisplayName(
            "Сохранено событие уведомления для выключенной БЗ (менее 3 дней до отключения). " +
            "Не хватает до тарифа, была активна в этот период."
    )
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testSaveBrandzoneWasActiveInThisPeriodLessThan3Days/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testSaveBrandzoneWasActiveInThisPeriodLessThan3Days/before.vendors.csv",
            after = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testSaveBrandzoneWasActiveInThisPeriodLessThan3Days/after.csv",
            dataSource = "vendorDataSource"
    )
    void testSaveBrandzoneWasActiveInThisPeriodLessThan3Days() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testSaveBrandzoneWasActiveInThisPeriodLessThan3Days/retrofit2_response.json"))));

        LocalDateTime testCaseNow = LocalDateTime.of(2019, 9, 29, 0, 0, 0);
        when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        executor.doJob(null);
    }

    @DisplayName(
            "Не сохранено событие уведомления для выключенной БЗ (менее 3 дней до отключения). " +
            "Активируется в следующем месяце, не хватает до тарифа."
    )
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNoSaveBrandzoneScheduledNextPeriodHasFinanceAndAdminCutoff/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNoSaveBrandzoneScheduledNextPeriodHasFinanceAndAdminCutoff/before.vendors.csv",
            after = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNoSaveBrandzoneScheduledNextPeriodHasFinanceAndAdminCutoff/after.csv",
            dataSource = "vendorDataSource"
    )
    void testNoSaveBrandzoneScheduledNextPeriodHasFinanceAndAdminCutoff() {
        LocalDateTime testCaseNow = LocalDateTime.of(2019, 9, 29, 0, 0, 0);
        when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        executor.doJob(null);
    }

    @DisplayName(
            "Не сохранено событие уведомления для выключенной БЗ (менее 3 дней до отключения). " +
            "Не было периодов активности."
    )
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNoSaveIfNoActivityPeriod/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNoSaveIfNoActivityPeriod/before.vendors.csv",
            after = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNoSaveIfNoActivityPeriod/after.csv",
            dataSource = "vendorDataSource"
    )
    void testNoSaveIfNoActivityPeriod() {
        LocalDateTime testCaseNow = LocalDateTime.of(2019, 9, 29, 0, 0, 0);
        when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        executor.doJob(null);
    }

    @DisplayName(
            "Сохранено событие уведомления для выключенной БЗ (менее 3 дней до отключения). " +
            "Активна. Поменяли тариф на более дорогой. Не хватает денег."
    )
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testSaveActiveChangeToExpensiveTariff/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testSaveActiveChangeToExpensiveTariff/before.vendors.csv",
            after = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testSaveActiveChangeToExpensiveTariff/after.csv",
            dataSource = "vendorDataSource"
    )
    void testSaveActiveChangeToExpensiveTariff() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testSaveActiveChangeToExpensiveTariff/retrofit2_response.json"))));

        LocalDateTime testCaseNow = LocalDateTime.of(2019, 9, 29, 0, 0, 0);
        when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        executor.doJob(null);
    }

    @DisplayName(
            "Сохранено событие уведомления для выключенной БЗ (менее 3 дней до отключения). " +
            "Активна. Поменяли тариф на более дешевый. Не хватает денег."
    )
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testSaveActiveChangeToCheaperTariff/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testSaveActiveChangeToCheaperTariff/before.vendors.csv",
            after = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testSaveActiveChangeToCheaperTariff/after.csv",
            dataSource = "vendorDataSource"
    )
    void testSaveActiveChangeToCheaperTariff() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/service/132/tariffs/search?tariffTypeId=63&tariffTypeId=75")
                .willReturn(aResponse().withBody(getStringResource("/testSaveActiveChangeToCheaperTariff/retrofit2_response.json"))));

        LocalDateTime testCaseNow = LocalDateTime.of(2019, 9, 29, 0, 0, 0);
        when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        executor.doJob(null);
    }

    @DisplayName(
            "Не сохранено событие уведомления для включённой БЗ (менее 3 дней до отключения). " +
            "Активна. Сменили на меньший тариф. Хватает денег."
    )
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNoSaveActiveChangeToCheaperTariffEnoughBalance/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNoSaveActiveChangeToCheaperTariffEnoughBalance/before.vendors.csv",
            after = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNoSaveActiveChangeToCheaperTariffEnoughBalance/after.csv",
            dataSource = "vendorDataSource"
    )
    void testNoSaveActiveChangeToCheaperTariffEnoughBalance() {
        LocalDateTime testCaseNow = LocalDateTime.of(2019, 9, 29, 2, 0, 0);
        when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        executor.doJob(null);
    }

    @DisplayName(
            "Не сохранено событие уведомления для включённой БЗ (менее 3 дней до отключения). " +
            "Активна. Сменили на больший тариф. Хватает денег."
    )
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNoSaveActiveChangeToExpensiveTariffEnoughBalance/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNoSaveActiveChangeToExpensiveTariffEnoughBalance/before.vendors.csv",
            after = "/ru/yandex/market/vendor/NotifyBrandzonePrepaidFinanceLowExecutorFunctionalTest/testNoSaveActiveChangeToExpensiveTariffEnoughBalance/after.csv",
            dataSource = "vendorDataSource"
    )
    void testNoSaveActiveChangeToExpensiveTariffEnoughBalance() {
        LocalDateTime testCaseNow = LocalDateTime.of(2019, 9, 29, 2, 0, 0);
        when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        executor.doJob(null);
    }
}
