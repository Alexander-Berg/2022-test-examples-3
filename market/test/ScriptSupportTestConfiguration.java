package ru.yandex.market.jmf.script.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.script.ScriptSupportConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;
import ru.yandex.market.jmf.utils.UtilsTestConfiguration;

@Configuration
@Import({
        ScriptSupportConfiguration.class,
        UtilsTestConfiguration.class,
})
public class ScriptSupportTestConfiguration extends AbstractModuleConfiguration {
    protected ScriptSupportTestConfiguration() {
        super("script/support/test");
    }
}
