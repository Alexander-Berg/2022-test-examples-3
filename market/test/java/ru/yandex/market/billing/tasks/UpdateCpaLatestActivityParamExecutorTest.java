package ru.yandex.market.billing.tasks;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class UpdateCpaLatestActivityParamExecutorTest extends FunctionalTest {

    @Autowired
    private UpdateCpaLatestActivityParamExecutor updateCpaLatestActivityParamExecutor;

    @Test
    @DbUnitDataSet(before = "UpdateCpaLatestActivityParamExecutorTest.before.csv",
            after = "UpdateCpaLatestActivityParamExecutorTest.after.csv")
    void testExecuteJob() {
        updateCpaLatestActivityParamExecutor.doJob(null);
    }
}
