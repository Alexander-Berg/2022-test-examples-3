package ru.yandex.market.jmf.ui.api.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.attributes.AttributesTestConfiguration;
import ru.yandex.market.jmf.mds.test.MdsSupportTestConfiguration;
import ru.yandex.market.jmf.ui.api.UiApiConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import({
        UiApiConfiguration.class,
        AttributesTestConfiguration.class,
        MdsSupportTestConfiguration.class,
})
public class UiApiTestConfiguration extends AbstractModuleConfiguration {
    protected UiApiTestConfiguration() {
        super("ui/api/test");
    }
}
