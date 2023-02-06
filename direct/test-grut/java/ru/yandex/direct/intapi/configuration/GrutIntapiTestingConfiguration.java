package ru.yandex.direct.intapi.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.direct.test.grut.GrutTestClientFactory;
import ru.yandex.grut.client.GrutClient;

@Configuration
@Import({IntapiTestingConfiguration.class})
public class GrutIntapiTestingConfiguration {
    @Bean
    @Primary
    public GrutClient grutClient() {
        return GrutTestClientFactory.getGrutClient();
    }
}
