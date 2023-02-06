package ru.yandex.market.ocrm.module.quality.management;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.b2bcrm.module.account.ModuleAccountConfiguration;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTestConfig;
import ru.yandex.market.jmf.module.chat.ChatClientService;
import ru.yandex.market.jmf.module.chat.ModuleChatConfiguration;
import ru.yandex.market.jmf.module.def.test.ModuleDefaultTestConfiguration;
import ru.yandex.market.jmf.module.ou.test.ModuleOuTestConfiguration;
import ru.yandex.market.jmf.module.ticket.test.ModuleTicketTestConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;
import ru.yandex.market.ocrm.module.csat.ModuleCsatConfiguration;
import ru.yandex.market.ocrm.module.order.ModuleOrderTestConfiguration;

@Configuration
@Import({
        B2bTicketTestConfig.class,
        ModuleChatConfiguration.class,
        ModuleAccountConfiguration.class,
        ModuleCsatConfiguration.class,
        ModuleDefaultTestConfiguration.class,
        ModuleOrderTestConfiguration.class,
        ModuleOuTestConfiguration.class,
        ModuleQualityManagementConfiguration.class,
        ModuleTicketTestConfiguration.class
})
public class ModuleQualityManagementTestConfiguration extends AbstractModuleConfiguration {

    protected ModuleQualityManagementTestConfiguration() {
        super("ocrm/module/quality/management/test");
    }

    @Bean
    public ChatClientService chatClientService() {
        return Mockito.mock(ChatClientService.class);
    }
}
