package ru.yandex.market.jmf.module.youscan;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.module.mail.test.ModuleMailTestConfiguration;
import ru.yandex.market.jmf.module.ou.test.ModuleOuTestConfiguration;
import ru.yandex.market.jmf.module.ticket.test.ModuleTicketTestConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import({
        ModuleYouScanConfiguration.class,
        ModuleTicketTestConfiguration.class,
        ModuleOuTestConfiguration.class,
        ModuleMailTestConfiguration.class,
})
public class ModuleYouScanTestConfiguration extends AbstractModuleConfiguration {
    protected ModuleYouScanTestConfiguration() {
        super("ru/yandex/market/jmf/module/youscan");
    }

}
