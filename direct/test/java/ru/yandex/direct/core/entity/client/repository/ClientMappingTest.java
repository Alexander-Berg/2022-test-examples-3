package ru.yandex.direct.core.entity.client.repository;

import java.lang.reflect.Method;
import java.util.Collection;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.MappingTestUtils;

@RunWith(JUnitParamsRunner.class)
public class ClientMappingTest {

    public static Collection<Object[]> parametersForTestMapping() {
        return MappingTestUtils.methodsAndArguments(ClientMapping.class);
    }

    @Test
    @Parameters
    @TestCaseName("метод: {1}, параметр: {2}")
    public void testMapping(Method method, @SuppressWarnings("unused") String methodName, Object argument) {
        try {
            method.invoke(null, argument);
        } catch (Exception e) {
            Assert.fail("вызов конвертирующего метода " +
                    ClientMapping.class.getCanonicalName() + "." + method.getName() +
                    " с аргументом " + argument + " завершился с исключением " + e);
        }
    }

}
