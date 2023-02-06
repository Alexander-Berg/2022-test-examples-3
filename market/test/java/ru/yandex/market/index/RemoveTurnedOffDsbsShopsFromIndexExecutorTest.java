package ru.yandex.market.index;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

public class RemoveTurnedOffDsbsShopsFromIndexExecutorTest extends FunctionalTest {

    @Autowired
    private RemoveTurnedOffDsbsShopsFromIndexExecutor tested;

    @Test
    @DbUnitDataSet(before = "RemoveTurnedOffDsbsShopsFromIndexExecutorTest.before.csv",
    after = "RemoveTurnedOffDsbsShopsFromIndexExecutorTest.after.csv")
    void testSuccessfulJobExecution() {
        tested.doJob(null);
    }
}
