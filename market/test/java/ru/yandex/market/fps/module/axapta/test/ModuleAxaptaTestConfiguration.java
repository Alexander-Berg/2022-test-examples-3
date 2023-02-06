package ru.yandex.market.fps.module.axapta.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fps.module.axapta.AxaptaClient;
import ru.yandex.market.fps.module.axapta.ModuleAxaptaConfiguration;
import ru.yandex.market.jmf.http.test.HttpTestConfiguration;

@Configuration
@Import({
        ModuleAxaptaConfiguration.class,
        HttpTestConfiguration.class,
})

public class ModuleAxaptaTestConfiguration {
    @Bean
    public AxaptaClient mockAxaptaClient() {
        return Mockito.mock(AxaptaClient.class);
    }
}
