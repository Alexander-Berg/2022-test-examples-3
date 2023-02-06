package ru.yandex.market.communication.proxy.tms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.communication.proxy.AbstractCommunicationProxyTest;

/**
 * @author zilzilok
 */
public class ClearCdrExecutorTest extends AbstractCommunicationProxyTest {
    @Autowired
    private ClearCdrExecutor clearCdrExecutor;

    @Test
    @DbUnitDataSet(
            before = "cdr/ClearCdrExecutorTest.clearOld.before.csv",
            after = "cdr/ClearCdrExecutorTest.clearOld.after.csv"
    )
    void clearOld() {
        clearCdrExecutor.doJob(mockContext());
    }

    @Test
    @DbUnitDataSet(
            before = "cdr/ClearCdrExecutorTest.clearToBeDeleted.before.csv",
            after = "cdr/ClearCdrExecutorTest.clearToBeDeleted.after.csv"
    )
    void clearToBeDeleted() {
        clearCdrExecutor.doJob(mockContext());
    }
}
