package ru.yandex.market.jmf.module.notification;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.metadata.MetadataProvider;
import ru.yandex.market.jmf.metadata.MetadataProviders;

@Configuration
@Import(ModuleNotificationTestConfiguration.class)
public class InternalModuleNotificationTestConfiguration {
    @Bean
    public MetadataProvider moduleNotificationTestMetadataProvider(MetadataProviders providers) {
        return providers.of("classpath:notification_metadata.xml");
    }
}
