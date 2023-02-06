package ru.yandex.chemodan.app.telemost.bazinga;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.bazinga.task.TelemostCronTask;
import ru.yandex.chemodan.app.telemost.bazinga.task.TelemostOnetimeTask;
import ru.yandex.commune.bazinga.scheduler.CronTask;
import ru.yandex.commune.bazinga.scheduler.OnetimeTask;
import ru.yandex.misc.spring.ApplicationContextUtils;
import ru.yandex.misc.test.Assert;

public class TelemostTaskContextConfigurationTest extends TelemostBaseContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testTasksAreTelemostBased() {
        for (OnetimeTask task : beansOfType(OnetimeTask.class)) {
            Assert.isInstance(task, TelemostOnetimeTask.class, task.getClass().getName());
        }
        for (CronTask task : beansOfType(CronTask.class)) {
            Assert.isInstance(task, TelemostCronTask.class, task.getClass().getName());
        }
    }

    private <T> ListF<T> beansOfType(Class<T> type) {
        return ApplicationContextUtils.beansOfType(applicationContext, type);
    }
}
