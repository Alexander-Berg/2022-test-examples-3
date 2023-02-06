package ru.yandex.market.billing.tasks;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.fulfillment.WithdrawsImportService;
import ru.yandex.market.billing.fulfillment.supplies.dao.FulfillmentSupplyYtDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.fulfillment.WithdrawDao;
import ru.yandex.market.mbi.environment.EnvironmentService;

class DailyJobServiceTest extends FunctionalTest {

    private static final LocalDate DATE_2018_05_21 = LocalDate.of(2018, 5, 21);

    @Autowired
    private WithdrawDao withdrawDao;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private FulfillmentSupplyYtDao fulfillmentSupplyYtDao;

    @Test
    @DisplayName("Проверка, что импорт забирает данные только за указанный период")
    @DbUnitDataSet(
            before = "DailyJobServiceTest.withdrawDateRange.before.csv",
            after = "DailyJobServiceTest.withdrawDateRange.after.csv"
    )
    void shouldImportWithdrawsForCorrectDatesWhenGivenDateRange() {
        WithdrawsImportService withdrawsImportService = new WithdrawsImportService(
                withdrawDao, transactionTemplate, fulfillmentSupplyYtDao
        );
        Instant instant = DATE_2018_05_21.atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant();
        Clock clock = Clock.fixed(instant, ZoneOffset.systemDefault());
        DailyJobService dailyJobService = new DailyJobService(
                "market.billing.withdraws_import_executor.start_date",
                environmentService,
                transactionTemplate,
                clock
        );
        dailyJobService.forEachDayButToday(withdrawsImportService::importWithdraws);
    }

}