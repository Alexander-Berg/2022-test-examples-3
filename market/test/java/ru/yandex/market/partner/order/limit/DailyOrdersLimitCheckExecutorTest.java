package ru.yandex.market.partner.order.limit;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;

import jdk.jfr.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.shop.FunctionalTest;

class DailyOrdersLimitCheckExecutorTest extends FunctionalTest {
    @Autowired
    private DailyOrdersLimitCheckExecutor executor;

    @Autowired
    private Clock clock;

    @BeforeEach
    public void setup() {
        Mockito.when(clock.instant())
                .thenReturn(
                        LocalDate.of(2021, Month.JANUARY, 6)
                                .atStartOfDay(DateTimes.MOSCOW_TIME_ZONE)
                                .toInstant()
                );
    }

    @Test
    @DbUnitDataSet(
            before = "DailyOrdersLimitCheckExecutorTest.before.csv",
            after = "DailyOrdersLimitCheckExecutorTest.after.csv"
    )
    @Description("Проверка, что накладывается катоф при достижении лимита")
    void testOpeningCutoff() {
        executor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "DailyOrdersLimitCheckExecutorTest.testCutoffAlreadyOpened.before.csv",
            after = "DailyOrdersLimitCheckExecutorTest.testCutoffAlreadyOpened.after.csv"
    )
    @Description("Проверка, что состояние магазина не меняется")
    void testCutoffAlreadyOpened() {
        executor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "DailyOrdersLimitCheckExecutorTest.testYesterdayNotAffectsToday.before.csv",
            after = "DailyOrdersLimitCheckExecutorTest.testYesterdayNotAffectsToday.after.csv"
    )
    @Description("Проверка, что вчерашняя статистика не влияет на сегодняшнюю")
    void testYesterdayNotAffectsToday() {
        executor.doJob(null);
    }
}
