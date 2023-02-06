package ru.yandex.market.tsum.exp3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.experiments3.client.Experiments3Client;

@Configuration
public class Exp3TestBeansConfig {
    @Bean
    public Experiments3Client experiments3Client() {
        String port = System.getenv("RECIPE_EXP3_MATCHER_PORT");
        return new Experiments3Client(Integer.parseInt(port));
    }
}
