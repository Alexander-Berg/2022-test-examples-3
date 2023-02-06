package ru.yandex.market.mcrm.script.impl;

import java.util.Collections;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.mcrm.script.ScriptService;
import ru.yandex.market.mcrm.script.test.ScriptServiceTestConfig;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ScriptServiceTestConfig.class)
public class ScriptServiceImplTest {

    @Inject
    private ScriptService service;

    /**
     * В тесте проверяем тривиальный сценарий скрипта. Скрипт состоит из одной строки, содержащей число. Результатом
     * выполнения этого скрипта должно
     * быть это число.
     */
    @Test
    public void simpleReturn() {
        long value = Randoms.longValue();
        // вызов системы
        Object result = service.execute(String.valueOf(value), Collections.emptyMap());

        Assertions.assertEquals(value, result);
    }

    /**
     * В тесте проверяем тривиальный сценарий обращения к АПИ {@link EchoScriptServiceApi Echo}.
     */
    @Test
    public void echo() {
        long value = Randoms.longValue();
        // вызов системы
        Object result = service.execute("api.echo.echo(" + value + ")", Collections.emptyMap());

        Assertions.assertEquals(value, result);
    }

    /**
     * В тесте проверяем невозможность изменения списка АПИ
     */
    @Test
    public void apiModification() {
        // вызов системы
        Assertions.assertThrows(UnsupportedOperationException.class, () -> service.execute("api.echo = 1",
                Collections.emptyMap()));
    }


    /**
     * В тесте проверяем тривиальный сценарий работы с переменной, переданной в параметрах скрипта.
     */
    @Test
    public void variable() {
        long value = Randoms.longValue();
        // вызов системы
        Object result = service.execute("return bindingVariable;", ImmutableMap.of("bindingVariable", value));

        Assertions.assertEquals(value, result);
    }
}
