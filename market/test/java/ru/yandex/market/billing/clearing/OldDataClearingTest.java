package ru.yandex.market.billing.clearing;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;

@DisplayName("Тесты для PaymentsPayoutsTransactionLogDao")
public class OldDataClearingTest extends FunctionalTest {

    private static final LocalDateTime TEST_UPDATE_TIME = LocalDateTime.of(2020, 10, 1, 10, 0, 0);

    @Autowired
    private OldDataClearingService oldDataClearingService;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    private void setUp() {
        clock.setFixed(DateTimes.toInstantAtDefaultTz(TEST_UPDATE_TIME), ZoneId.systemDefault());
    }

    @AfterEach
    private void tearDown() {
        clock.clearFixed();
    }

    @Test
    @DisplayName("Проверка корректного удаления из всех таблиц")
    @DbUnitDataSet(
            before = "OldDataClearingTest.before.csv",
            after = "OldDataClearingTest.after.csv"
    )
    void clearingTest() {
        oldDataClearingService.clearOldData();
    }

    @Test
    @DisplayName("Проверка отсутствия некорректных параметров")
    void testCheckParams() {
        for (TableClearingOptions options: TableClearingOptions.values()) {
            Instant now = clock.instant();
            LocalDateTime deletingDate = options.getDate(now);
            Assert.assertTrue(deletingDate.compareTo(LocalDateTime.of(2020, 9, 1, 0, 0)) <= 0);
        }
    }
}
