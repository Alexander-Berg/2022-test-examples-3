package ru.yandex.direct.api.v5.testing.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.direct.test.grut.GrutTestClientFactory;
import ru.yandex.grut.client.GrutClient;

@Configuration
@Import({Api5TestingConfiguration.class})
public class GrutApi5TestingConfiguration {
    @Bean
    @Primary
    public GrutClient grutClient() {
        return GrutTestClientFactory.getGrutClient();
    }
}
