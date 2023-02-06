package ru.yandex.market.jmf.module.properties.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.entity.test.EntityApiTestConfiguration;
import ru.yandex.market.jmf.module.properties.ModuleContextPropertiesConfiguration;

@Configuration
@Import({
        ModuleContextPropertiesConfiguration.class,
        EntityApiTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.jmf.module.properties.test.impl")
public class ModuleContextPropertiesTestConfiguration {
}
