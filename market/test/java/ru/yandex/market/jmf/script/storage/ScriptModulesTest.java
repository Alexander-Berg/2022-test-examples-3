package ru.yandex.market.jmf.script.storage;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.script.ScriptService;

@SpringJUnitConfig(classes = ScriptModulesTest.Configuration.class)
public class ScriptModulesTest {

    @Inject
    private ScriptService scriptService;

    private void checkScript(Object expected, String script) {
        Object result = scriptService.execute(script);
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testSimpleSum() {
        checkScript(3, "1 + 2");
    }

    /**
     * попытка вызова обычного скрипта как модуля должна приводить к ошибке
     */
    @Test
    public void testScriptCall() {
        Assertions.assertThrows(RuntimeException.class, () -> checkScript(null, "modules.script0.one()"));
    }

    @Test
    public void testScriptModuleCall() {
        checkScript(3, "modules.module1.plus(1, 2)");
    }

    @Test
    public void testScriptModuleIndirectCall() {
        checkScript(6, "modules.module2.mult2(3)");
    }

    @Test
    public void testScriptModuleLoadedOnce() {
        checkScript(3, "modules.module3.inc() + modules.module3.inc()");
    }

    @Test
    public void testScriptModuleReloadedForNewScript() {
        checkScript(1, "modules.module3.inc()");
        checkScript(1, "modules.module3.inc()");
    }

    @Import({
            ScriptStorageTestConfiguration.class,
    })
    public static class Configuration {

        @Bean
        @Primary
        public ScriptStorageService scriptStorageService() {
            return InMemoryScriptStorageService.builder()
                    .withScript("script0", "def one() { 1 }")
                    .withModule("module1", "def plus(a, b) { a + b }")
                    .withModule("module2", "def mult2(a) { modules.module1.plus(a, a) }")
                    .withModule("module3", "@groovy.transform.Field def a = 0\n"
                            + "def inc() { ++a }")
                    .build();
        }
    }

}
