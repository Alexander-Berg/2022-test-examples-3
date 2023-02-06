package ru.yandex.market.mcrm.script.impl;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ScriptValueServiceImplTest {

    @Test
    public void getAllInterfacesRecursive() {
        var scriptValueService = new ScriptValueServiceImpl(new ArrayList<>());

        var actualInterfaces = scriptValueService.getAllInterfacesForType(TestClass.class);

        List<Class<?>> expectedInterfaces =
                List.of(TestInterface.class, TestInterfaceTwo.class);

        Assert.assertEquals(expectedInterfaces.size(), actualInterfaces.size());

        Assert.assertEquals(expectedInterfaces, actualInterfaces);
    }

    interface TestInterface extends TestInterfaceTwo {

    }

    interface TestInterfaceTwo {

    }

    class TestClass implements TestInterface {

    }
}
