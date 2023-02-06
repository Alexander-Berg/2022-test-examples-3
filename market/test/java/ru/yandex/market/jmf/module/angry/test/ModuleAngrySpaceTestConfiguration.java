package ru.yandex.market.jmf.module.angry.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.jmf.module.angry.AngrySpaceAttachmentsUploadService;
import ru.yandex.market.jmf.module.angry.AngrySpaceClient;
import ru.yandex.market.jmf.module.angry.ModuleAngrySpaceConfiguration;
import ru.yandex.market.jmf.module.ticket.test.ModuleTicketTestConfiguration;

@Configuration
@Import({
        ModuleAngrySpaceConfiguration.class,
        ModuleTicketTestConfiguration.class
})
@PropertySource(name = "testAngrySpaceProperties", value = "classpath:/ru/yandex/market/jmf/module/angry/test" +
        ".properties")
public class ModuleAngrySpaceTestConfiguration {
    @Bean
    @Primary
    public AngrySpaceClient testAngrySpaceClient() {
        return Mockito.mock(AngrySpaceClient.class);
    }

    @Bean
    @Primary
    public AngrySpaceAttachmentsUploadService testAngrySpaceAttachmentsUploadService() {
        return Mockito.mock(AngrySpaceAttachmentsUploadService.class);
    }
}
