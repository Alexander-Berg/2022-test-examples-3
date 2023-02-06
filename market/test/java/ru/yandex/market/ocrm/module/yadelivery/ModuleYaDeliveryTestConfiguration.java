package ru.yandex.market.ocrm.module.yadelivery;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.jmf.module.def.test.ModuleDefaultTestConfiguration;
import ru.yandex.market.jmf.module.ticket.OmniChannelSettingsService;
import ru.yandex.market.jmf.module.ticket.test.ModuleTicketTestConfiguration;
import ru.yandex.market.jmf.module.xiva.ModuleXivaTestConfiguration;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.ocrm.module.yadelivery.impl.LomScriptServiceApi;

@Configuration
@ComponentScan("ru.yandex.market.ocrm.module.yadelivery.test")
@Import({
        ModuleDefaultTestConfiguration.class,
        ModuleYaDeliveryConfiguration.class,
        ModuleTicketTestConfiguration.class,
        ModuleXivaTestConfiguration.class,
})
public class ModuleYaDeliveryTestConfiguration {

    @Bean
    public LMSClient lmsClient() {
        return Mockito.mock(LMSClient.class);
    }

    @Bean
    public LomClient lomClient() {
        return Mockito.mock(LomClient.class);
    }

    @Bean
    public NesuClient nesuClient() {
        return Mockito.mock(NesuClient.class);
    }

    @Bean
    public OmniChannelSettingsService omniChannelSettingsService() {
        return Mockito.mock(OmniChannelSettingsService.class);
    }

    @Primary
    @Bean
    public LomScriptServiceApi lomScriptServiceApi() {
        return Mockito.mock(LomScriptServiceApi.class);
    }
}
