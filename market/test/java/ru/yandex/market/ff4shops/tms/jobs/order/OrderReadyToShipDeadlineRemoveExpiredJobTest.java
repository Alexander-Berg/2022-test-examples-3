package ru.yandex.market.ff4shops.tms.jobs.order;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.ff4shops.api.json.AbstractJsonControllerFunctionalTest;

@DisplayName("Удаление лишних записей из таблицы дедлайнов сборки заказов")
class OrderReadyToShipDeadlineRemoveExpiredJobTest extends AbstractJsonControllerFunctionalTest {

    @Autowired
    private OrderReadyToShipDeadlineRemoveExpiredJob removeExpiredJob;

    @Autowired
    private TestableClock testableClock;

    @BeforeEach
    void init() {
        testableClock.setFixed(Instant.parse("2021-08-17T18:15:30Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Успешное удаление записей")
    @DbUnitDataSet(
        before = "OrderReadyToShipDeadlineRemoveExpiredJobTest.before.csv",
        after = "OrderReadyToShipDeadlineRemoveExpiredJobTest.after.csv"
    )
    void deleteExpiredDeadlines() {
        removeExpiredJob.doJob(null);
    }
}
