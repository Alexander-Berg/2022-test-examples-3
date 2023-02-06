package ru.yandex.market.abo.tms.dynamic;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.dynamic.model.MatViewTask;
import ru.yandex.EmptyTest;

/**
 * @author imelnikov
 */
public class MatViewTaskExecutorTest extends EmptyTest {

    @Autowired
    private MatViewTaskExecutor mviewRefreshExecutor;

    @Test
    public void concurrentyRefresh() {
        MatViewTask task = mviewRefreshExecutor.getTask("mv_uc_shop_report");
        mviewRefreshExecutor.doJob(null, task);
    }
}
