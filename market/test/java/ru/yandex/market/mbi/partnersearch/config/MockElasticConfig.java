package ru.yandex.market.mbi.partnersearch.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.mbi.partnersearch.data.elastic.ElasticService;

@Configuration
public class MockElasticConfig {

    @Bean
    @Primary
    public ElasticService mockElasticService() {
        return Mockito.mock(ElasticService.class);
    }
}
