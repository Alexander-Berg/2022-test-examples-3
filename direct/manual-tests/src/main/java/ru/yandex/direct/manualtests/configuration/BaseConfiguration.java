package ru.yandex.direct.manualtests.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.core.configuration.CoreConfiguration;
import ru.yandex.direct.core.entity.uac.grut.EmptyGrutContext;

@Configuration
@Import({
        CoreConfiguration.class,
})
public class BaseConfiguration {
    @Bean
    public EmptyGrutContext grutContext() {
        return new EmptyGrutContext();
    }
}
