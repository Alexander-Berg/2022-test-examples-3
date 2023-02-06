package ru.yandex.market.jmf.module.resizer.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.jmf.http.test.HttpTestConfiguration;
import ru.yandex.market.jmf.module.resizer.ModuleResizerConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import({
        ModuleResizerConfiguration.class,
        HttpTestConfiguration.class,
})
@PropertySource(name = "testResizerProperties", value = "classpath:resizer/test/resizer-test.properties")
public class ModuleResizerTestConfiguration extends AbstractModuleConfiguration {
    protected ModuleResizerTestConfiguration() {
        super("module/resizer/test");
    }
}
