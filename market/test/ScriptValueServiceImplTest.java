package ru.yandex.market.jmf.script.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.script.impl.ScriptValueServiceImpl;

public class ScriptValueServiceImplTest {

    @Test
    public void getAllInterfacesRecursive() {
        var scriptValueService = new ScriptValueServiceImpl(new ArrayList<>());

        var actualInterfaces = scriptValueService.getAllInterfacesForType(TestClass.class);

        List<Class<?>> expectedInterfaces =
                List.of(TestInterface.class, TestInterfaceTwo.class);

        Assertions.assertEquals(expectedInterfaces, actualInterfaces);
    }

    interface TestInterface extends TestInterfaceTwo {

    }

    interface TestInterfaceTwo {

    }

    class TestClass implements TestInterface {

    }
}
