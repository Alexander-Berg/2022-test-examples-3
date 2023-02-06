package ru.yandex.market.tpl.billing.service.tlog;

import java.time.LocalDate;
import java.time.LocalTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.util.DateTimeUtil;

public class OrderExportTransactionsServiceTest extends AbstractFunctionalTest {
    private final static LocalDate EXPORT_DATE = LocalDate.of(2022, 2, 10);

    @Autowired
    private OrderExportTransactionsService rewardTransactionsService;
    @Autowired
    private OrderExportTransactionsService dbsTransactionsService;
    @Autowired
    private OrderExportTransactionsService dropoffTransactionsService;
    @Autowired
    private OrderExportTransactionsService compensationsTransactionsService;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setClock() {
        clock.setFixed(
                EXPORT_DATE.atTime(LocalTime.of(10, 10)).toInstant(DateTimeUtil.DEFAULT_ZONE_ID),
                DateTimeUtil.DEFAULT_ZONE_ID
        );
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/tlog/base.csv",
                    "/database/service/tlog/orderExportTransactionsService/before/init.csv"},
            after = "/database/service/tlog/orderExportTransactionsService/after/result.csv")
void oneDayProcessing() {
        rewardTransactionsService.createTransactionsForPeriod(EXPORT_DATE, EXPORT_DATE);
        dbsTransactionsService.createTransactionsForPeriod(EXPORT_DATE, EXPORT_DATE);
        dropoffTransactionsService.createTransactionsForPeriod(EXPORT_DATE, EXPORT_DATE);
        compensationsTransactionsService.createTransactionsForPeriod(EXPORT_DATE, EXPORT_DATE);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/tlog/base.csv",
                    "/database/service/tlog/orderExportTransactionsService/before/multipleDays.csv"},
            after = "/database/service/tlog/orderExportTransactionsService/after/multipleDaysResult.csv")
    void multipleDaysProcessing() {
        rewardTransactionsService.createTransactionsForPeriod(EXPORT_DATE.withDayOfMonth(1), EXPORT_DATE);
        dbsTransactionsService.createTransactionsForPeriod(EXPORT_DATE.withDayOfMonth(1), EXPORT_DATE);
        dropoffTransactionsService.createTransactionsForPeriod(EXPORT_DATE.withDayOfMonth(1), EXPORT_DATE);
        compensationsTransactionsService.createTransactionsForPeriod(EXPORT_DATE.withDayOfMonth(1), EXPORT_DATE);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/tlog/base.csv",
                    "/database/service/tlog/orderExportTransactionsService/before/specifiedServices.csv"},
            after = "/database/service/tlog/orderExportTransactionsService/after/specifiedServices.csv")
    void onlySpecifiedServiceTypeProcessed() {
        compensationsTransactionsService.createTransactionsForPeriod(EXPORT_DATE, EXPORT_DATE);
    }
}
