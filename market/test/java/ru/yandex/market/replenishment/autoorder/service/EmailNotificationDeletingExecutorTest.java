package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.executor.DeleteExecutor;
@ActiveProfiles("unittest")
public class EmailNotificationDeletingExecutorTest extends FunctionalTest {

    @Qualifier("emailNotificationDeletingExecutor")
    @Autowired
    private DeleteExecutor emailNotificationDeletingExecutor;

    @DbUnitDataSet(
            before = "EmailNotificationDeletingExecutorTest.Delete.before.csv",
            after = "EmailNotificationDeletingExecutorTest.Delete.after.csv")
    @Test
    public void deleteTest() {
        emailNotificationDeletingExecutor.doJob(null);
    }
}
