package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
public class LockingQueryServiceTest extends FunctionalTest {

    @Autowired
    private LockingQueryService lockingQueryService;

    @Test
    @DbUnitDataSet(before = "LockingQueryServiceTest.testDeleteExpiredQueries.before.csv",
            after = "LockingQueryServiceTest.testDeleteExpiredQueries.after.csv")
    public void testDeleteExpiredQueries() {
        setTestTime(LocalDateTime.of(2021, 9, 2, 0, 0, 0));
        lockingQueryService.deleteExpiredQueries();
    }
}
