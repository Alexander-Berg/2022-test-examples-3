package ru.yandex.market.jmf.module.startrek;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.jmf.module.comment.test.ModuleCommentTestConfiguration;
import ru.yandex.market.jmf.module.metric.test.MetricsModuleTestConfiguration;
import ru.yandex.market.jmf.startrek.support.test.StartrekSupportTestConfiguration;

@Configuration
@Import({
        ModuleStartrekConfiguration.class,
        ModuleCommentTestConfiguration.class,
        MetricsModuleTestConfiguration.class,
        StartrekSupportTestConfiguration.class,
})
@PropertySource(
        name = "testStartrekProperties",
        value = "classpath:/ru/yandex/market/jmf/module/startrek/test.properties"
)
@ComponentScan("ru.yandex.market.jmf.module.startrek.impl")
public class ModuleStartrekTestConfiguration {

}
