package ru.yandex.market.jmf.module.toloka;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.jmf.module.def.test.ModuleDefaultTestConfiguration;
import ru.yandex.market.jmf.module.ou.test.ModuleOuTestConfiguration;
import ru.yandex.market.jmf.module.ticket.test.ModuleTicketTestConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import({
        ModuleTolokaConfiguration.class,
        ModuleDefaultTestConfiguration.class,
        ModuleTicketTestConfiguration.class,
        ModuleOuTestConfiguration.class
})
@ComponentScan("ru.yandex.market.jmf.module.toloka.utils")
public class ModuleTolokaTestConfiguration extends AbstractModuleConfiguration {

    public ModuleTolokaTestConfiguration() {
        super("tests/toloka");
    }

    @Bean
    @Primary
    public TolokaClient tolokaClient() {
        return Mockito.mock(TolokaClient.class);
    }

    @Bean
    @Primary
    public TolokaClients tolokaClients() {
        return Mockito.mock(TolokaClients.class);
    }

}
