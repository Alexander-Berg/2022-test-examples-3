package ru.yandex.market.psku.postprocessor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.commune.bazinga.impl.storage.BazingaStorage;

@Configuration
public class BazingaTestConfig {

    @Bean
    BazingaStorage bazingaStorage() {
        return null;
    }
}
