package ru.yandex.market.communication.proxy.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.communication.proxy.service.PersonalMarketService;

/**
 * @author i-shunkevich
 * @date 28.06.2022
 */
@Configuration
public class PersonalMarketServiceConfig {
    @Bean
    public PersonalMarketService personalMarketService() {
        return Mockito.mock(PersonalMarketService.class);
    }
}
