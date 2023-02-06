package ru.yandex.market.tpl.billing.service.tlog;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;

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

/**
 * Тесты для {@link CorrectionExportTransactionsService}
 */
public class CorrectionExportTransactionsServiceTest extends AbstractFunctionalTest {
    private final static LocalDate EXPORT_DATE = LocalDate.of(2022, Month.MARCH, 10);

    @Autowired
    private CorrectionExportTransactionsService correctionExportTransactionsService;

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
                    "/database/service/tlog/correctionExportTransactionsService/before/multiple_days.csv"},
            after = "/database/service/tlog/correctionExportTransactionsService/after/multiple_days.csv")
    void multipleDaysProcessing() {
        correctionExportTransactionsService.createTransactionsForPeriod(EXPORT_DATE.withDayOfMonth(1), EXPORT_DATE);
    }
}
