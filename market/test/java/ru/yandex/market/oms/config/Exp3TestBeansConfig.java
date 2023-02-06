package ru.yandex.market.oms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.experiments3.client.Experiments3Client;
import ru.yandex.market.experiments3.client.Experiments3ClientConfig;

@Configuration
@Profile("functionalTest")
public class Exp3TestBeansConfig {
    // todo: Нужно придумать мок для тестирования, рецептом проверять - геморно
    @Bean
    public Experiments3Client experiments3Client() {
        String port = System.getenv("RECIPE_EXP3_MATCHER_PORT");
        if (port == null) {
            port = "11920";
        }
        var config = new Experiments3ClientConfig.Builder()
                .setPort(Integer.parseInt(port))
                .setConsumer("oms")
                .build();
        return new Experiments3Client(config);
    }
}
