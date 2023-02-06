package ru.yandex.chemodan.app.dataapi.test;

import java.util.concurrent.ExecutorService;

import org.junit.Test;

import ru.yandex.chemodan.app.dataapi.core.DatabasesContextConfiguration;
import ru.yandex.chemodan.app.dataapi.core.dao.test.ActivateDataApiEmbeddedPg;
import ru.yandex.commune.bazinga.BazingaTaskManager;
import ru.yandex.commune.bazinga.test.BazingaTaskManagerStub;
import ru.yandex.misc.test.Assert;

/**
 * @author Denis Bakharev
 */
@ActivateDataApiEmbeddedPg
public class CorrectTestDependenciesTest extends DataApiTestSupport {

    @Test
    public void synchronousExecutorForDatabaseManager() {
        ThreadLocal<Integer> threadLocal = new ThreadLocal<>();
        threadLocal.set(0);
        ExecutorService executorService = (ExecutorService)
                applicationContext.getBean(DatabasesContextConfiguration.DATABASE_MANAGER_EXECUTOR_SERVICE);
        executorService.execute(() -> threadLocal.set(1));
        Assert.equals(1, threadLocal.get());
    }

    @Test
    public void mustUseBazingaStub() {
        Assert.isInstance(applicationContext.getBean(BazingaTaskManager.class),
                          BazingaTaskManagerStub.class);
    }
}
