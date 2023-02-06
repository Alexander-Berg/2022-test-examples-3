package ru.yandex.market.deepdive.configuration;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ru.yandex.market.deepdive.client.PvzClient;
import ru.yandex.market.deepdive.repositories.OrderRepository;
import ru.yandex.market.deepdive.repositories.PickupPointRepository;

@Configuration
@ComponentScan("ru.yandex.market.deepdive.controller")
@ComponentScan("ru.yandex.market.deepdive.dto")
@ComponentScan("ru.yandex.market.deepdive.entities")
@ComponentScan("ru.yandex.market.deepdive.mapping")
@ComponentScan("ru.yandex.market.deepdive.services")
@ComponentScan("ru.yandex.market.deepdive.repositories")
@Import({
        DeepDiveExternalConfiguration.class,
})
@EnableWebMvc
public class IntegrationTestConfiguration {

    @MockBean
    private PickupPointRepository repository;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private PvzClient pvzClient;

}
