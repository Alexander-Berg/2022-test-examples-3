package ru.yandex.direct.oneshot.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.test.grut.GrutTestClientFactory;
import ru.yandex.grut.client.GrutClient;

@Configuration
@Import({AppConfiguration.class, CoreTestingConfiguration.class})
public class GrutOneshotTestingConfiguration {
    @Bean
    @Primary
    public GrutClient grutClient() { return GrutTestClientFactory.getGrutClient(); }
}
