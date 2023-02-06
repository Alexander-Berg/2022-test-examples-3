package ru.yandex.market.jmf.module.startrek;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.jmf.module.def.test.ModuleDefaultTestConfiguration;
import ru.yandex.market.jmf.module.ou.ModuleOuConfiguration;

@Configuration
@Import({
        ModuleStartrekConfiguration.class,
        ModuleDefaultTestConfiguration.class,
        ModuleOuConfiguration.class,
})
@PropertySource(
        name = "testStartrekProperties",
        value = "classpath:/ru/yandex/market/jmf/module/startrek/test.properties"
)
@ComponentScan("ru.yandex.market.jmf.module.startrek.test")
public class StartrekServiceTestConfig {

}
