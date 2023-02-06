package ru.yandex.market.jmf.mds.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.jmf.mds.MdsSupportConfiguration;
import ru.yandex.market.jmf.script.test.ScriptSupportTestConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import({
        MdsSupportConfiguration.class,
        ScriptSupportTestConfiguration.class,
})
@ComponentScan(basePackages = "ru.yandex.market.jmf.mds.test.impl")
@PropertySource(name = "testMdsSupportProperties", value = "classpath:mds-support/test/mds-test.properties")
public class MdsSupportTestConfiguration extends AbstractModuleConfiguration {
    protected MdsSupportTestConfiguration() {
        super("mds-support/test");
    }
}
