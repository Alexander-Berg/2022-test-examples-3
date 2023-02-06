package ru.yandex.calendar.admin.universal;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.spring.ApplicationContextUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class CalendarReflectionToMultilineSerializerPluginTest extends AbstractConfTest {
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void isUsedInNonDaemonApplicationContext() {
        Assert.assertNotEmpty(
            ApplicationContextUtils.beansOfType(applicationContext, CalendarReflectionToMultilineSerializerPlugin.class)
        );
    }
}
