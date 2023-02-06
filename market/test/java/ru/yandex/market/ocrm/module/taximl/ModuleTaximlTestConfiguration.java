package ru.yandex.market.ocrm.module.taximl;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.jmf.module.ticket.test.ModuleTicketTestConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;
import ru.yandex.market.ocrm.module.common.ModuleOcrmCommonConfiguration;

@Configuration
@ActiveProfiles(profiles = "test")
@Import({
        ModuleTaximlConfiguration.class,
        ModuleTicketTestConfiguration.class,
        ModuleOcrmCommonConfiguration.class
})
public class ModuleTaximlTestConfiguration extends AbstractModuleConfiguration {

    public ModuleTaximlTestConfiguration() {
        super("ocrm/module/taximl/test");
    }

    @Bean
    @Primary
    public TaximlClient taximlTestClient() {
        return Mockito.mock(TaximlClient.class);
    }
}
