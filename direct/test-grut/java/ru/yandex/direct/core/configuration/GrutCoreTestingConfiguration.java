package ru.yandex.direct.core.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.core.testing.configuration.UacYdbTestingConfiguration;
import ru.yandex.direct.test.grut.GrutTestClientFactory;
import ru.yandex.grut.client.GrutClient;

@Configuration
@Import({CoreTestingConfiguration.class, UacYdbTestingConfiguration.class})
public class GrutCoreTestingConfiguration {
    @Bean
    @Primary
    public GrutClient grutClient() {
        return GrutTestClientFactory.getGrutClient();
    }
}
