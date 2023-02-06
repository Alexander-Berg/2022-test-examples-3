package ru.yandex.chemodan.app.psbilling.worker;

import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.chemodan.app.psbilling.core.utils.ClassFinder;
import ru.yandex.commune.bazinga.scheduler.CronTask;
import ru.yandex.commune.bazinga.scheduler.OnetimeTask;
import ru.yandex.misc.test.Assert;

public class CommonTasksTest extends AbstractWorkerTest {
    private final String webPackageName = "ru.yandex.chemodan.app.psbilling.worker";
    private final String corePackageName = "ru.yandex.chemodan.app.psbilling.core";

    @Test
    @Ignore("not work on arc- check locally")
    // если они не будут бинами - они не будут в интерфейсе базинги
    public void allTasksShouldBeBeans() {
        List<Class<CronTask>> taskClasses = ClassFinder.findTaskClasses(webPackageName, CronTask.class);
        Assert.notEmpty(taskClasses);

        for (Class<CronTask> taskClass : taskClasses) {
            Map<String, CronTask> taskBeans = applicationContext.getBeansOfType(taskClass);
            Assert.notEmpty(taskBeans.keySet(), "task bean " + taskClass.getName() + " should exist");
        }
    }

    @Test
    @Ignore("not work on arc- check locally")
    public void allTasksShouldHaveRightQueue() {
        List<Class<OnetimeTask>> taskClasses = ClassFinder.findTaskClasses(corePackageName,
                OnetimeTask.class);
        Assert.notEmpty(taskClasses);

        for (Class<OnetimeTask> taskClass : taskClasses) {
            Map<String, OnetimeTask> taskBeans = applicationContext.getBeansOfType(taskClass);
            taskBeans.values().forEach(x ->
                    Assert.assertTrue(String.format("task %s should have queue for ps billing but have %s",
                            x.getClass().getName(), x.queueName()), x.queueName().getName().startsWith("ps-billing")));
        }
    }
}
