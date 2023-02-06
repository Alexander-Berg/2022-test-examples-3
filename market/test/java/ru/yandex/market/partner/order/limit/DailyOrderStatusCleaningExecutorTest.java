package ru.yandex.market.partner.order.limit;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static ru.yandex.market.mbi.common.Mbi.DEFAULT_TIME_ZONE;

class DailyOrderStatusCleaningExecutorTest extends FunctionalTest {
    @Autowired
    private DailyOrderStatusCleaningExecutor executor;

    @Autowired
    private Clock clock;

    @Test
    @DbUnitDataSet(
            before = "DailyOrderStatusCleaningExecutorTest.before.csv",
            after = "DailyOrderStatusCleaningExecutorTest.after.csv"
    )
    void testCleaning() {
        Mockito.when(clock.instant())
                .thenReturn(
                        LocalDate.of(2021, Month.JANUARY, 14)
                                .atStartOfDay(DEFAULT_TIME_ZONE)
                                .toInstant()
                );

        executor.doJob(null);
    }

}
