package ru.yandex.market.mbo.tms.conf;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.internal.util.reflection.FieldSetter;
import org.quartz.CronExpression;

import ru.yandex.market.tms.quartz2.model.CronTriggerJobBean;

import static org.junit.Assert.fail;

public class MboTmsTriggerConfigTest {

    @Test
    public void testCronExpression() throws NoSuchFieldException {
        MboTmsTriggerConfig mboTmsTriggerConfig = new MboTmsTriggerConfig();
        FieldSetter.setField(mboTmsTriggerConfig,
            MboTmsTriggerConfig.class.getDeclaredField("environment"),
            "production");
        FieldSetter.setField(mboTmsTriggerConfig,
            MboTmsTriggerConfig.class.getDeclaredField("environmentExtension"),
            "");

        final List<Method> methods = Arrays.asList(MboTmsTriggerConfig.class.getDeclaredMethods());
        methods.stream()
            .filter(method -> method.getReturnType().equals(CronTriggerJobBean.class))
            .map(method -> {
                try {
                    return (CronTriggerJobBean) method.invoke(mboTmsTriggerConfig);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            })
            .map(CronTriggerJobBean::getCronExpression)
            .forEach(expression -> {
                try {
                    new CronExpression(expression);
                } catch (ParseException e) {
                    fail("Non valid expression: " + expression + ". Exception: " + e);
                }
            });
    }

}
