package ru.yandex.market.jmf.module.def.test;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@SpringJUnitConfig(classes = {
        AbstractModuleConfigurationTest.Configuration1.class,
        AbstractModuleConfigurationTest.Configuration2.class
})
public class AbstractModuleConfigurationTest {

    @Inject
    MetadataService metadataService;

    @Test
    public void checkConfigurations() {
        Assertions.assertNotNull(
                metadataService.getMetaclass(Fqn.of("simple1")),
                "Должны получить метакласс  зарегистрированный первой конфигурацией");
        Assertions.assertNotNull(
                metadataService.getMetaclass(Fqn.of("simple2")), "Должны получить метакласс  зарегистрированный " +
                        "второй конфигурацией");
    }


    @Import(ModuleDefaultTestConfiguration.class)
    public static class Configuration1 extends AbstractModuleConfiguration {
        public Configuration1() {
            super("module/default/test/1");
        }
    }

    @Import(ModuleDefaultTestConfiguration.class)
    public static class Configuration2 extends AbstractModuleConfiguration {
        public Configuration2() {
            super("module/default/test/2");
        }
    }
}
