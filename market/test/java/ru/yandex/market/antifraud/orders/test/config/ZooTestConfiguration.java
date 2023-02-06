package ru.yandex.market.antifraud.orders.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import ru.yandex.market.volva.zookeeper.NamedSpaceZooClient;

import static org.mockito.Mockito.mock;

/**
 * @author dzvyagin
 */
@Configuration
public class ZooTestConfiguration {

    @Bean
    public NamedSpaceZooClient namedSpaceZooClient(){
        return mock(NamedSpaceZooClient.class);
    }
}
