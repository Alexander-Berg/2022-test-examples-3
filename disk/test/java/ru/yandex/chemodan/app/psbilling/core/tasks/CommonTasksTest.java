package ru.yandex.chemodan.app.psbilling.core.tasks;

import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.utils.ClassFinder;
import ru.yandex.commune.bazinga.scheduler.OnetimeTask;
import ru.yandex.misc.test.Assert;

public class CommonTasksTest extends AbstractPsBillingCoreTest {
    private final String packageName = "ru.yandex.chemodan.app.psbilling.core";

    @Test
    @Ignore("not work on arc- check locally")
    // если они не будут бинами - они не будут в интерфейсе базинги
    public void allTasksShouldBeBeans() {
        List<Class<OnetimeTask>> taskClasses = ClassFinder.findTaskClasses(packageName, OnetimeTask.class);
        Assert.notEmpty(taskClasses);

        for (Class<OnetimeTask> taskClass : taskClasses) {
            Map<String, OnetimeTask> taskBeans = applicationContext.getBeansOfType(taskClass);
            Assert.notEmpty(taskBeans.keySet(), "task bean " + taskClass.getName() + " should exist");
        }
    }
}
