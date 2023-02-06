package ru.yandex.direct.core.entity.campaign.repository;

import java.lang.reflect.Method;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.MappingTestUtils;

@RunWith(Parameterized.class)
public class CampaignMappingTest {

    @Parameterized.Parameters(name = "метод: {1}, аргумент: {2}")
    public static Collection<Object[]> methodsAndArguments() {
        return MappingTestUtils.methodsAndArguments(CampaignMappings.class);
    }

    @Parameterized.Parameter(0)
    public Method method;

    @SuppressWarnings("unused")
    @Parameterized.Parameter(1)
    public String methodName;

    @Parameterized.Parameter(2)
    public Object argument;

    @Test
    public void testMapping() {
        try {
            method.invoke(null, argument);
        } catch (Exception e) {
            Assert.fail("вызов конвертирующего метода " +
                    CampaignMappings.class.getCanonicalName() + "." + method.getName() +
                    " с аргументом " + argument + " завершился с исключением " + e);
        }
    }
}
