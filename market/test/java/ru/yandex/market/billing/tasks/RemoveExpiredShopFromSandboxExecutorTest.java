package ru.yandex.market.billing.tasks;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class RemoveExpiredShopFromSandboxExecutorTest extends FunctionalTest {

    @Autowired
    private RemoveExpiredShopFromSandboxExecutor tested;

    @Test
    @DbUnitDataSet(before = "RemoveExpiredShopFromSandboxExecutorTest.before.csv",
    after = "RemoveExpiredShopFromSandboxExecutorTest.after.csv")
    void testSuccessfulJobExecution() {
        tested.doJob(null);
    }
}
