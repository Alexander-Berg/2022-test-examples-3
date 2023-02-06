package ru.yandex.market.deepdive.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ru.yandex.market.deepdive.domain.client.PvzClientService;
import ru.yandex.market.deepdive.domain.order.OrderRepository;
import ru.yandex.market.deepdive.domain.order.OrderService;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;
import ru.yandex.market.deepdive.domain.specification.OrderSpecificationFactoryImpl;
import ru.yandex.market.deepdive.executor.UpdateDataExecutor;

@Configuration
@ComponentScan("ru.yandex.market.deepdive.domain")
@Import({
        DeepDiveExternalConfiguration.class,
})
@EnableWebMvc
@PropertySource("classpath:integration.properties")
public class IntegrationTestConfiguration {
    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private PickupPointRepository pickupPointRepository;

    @MockBean
    private OrderSpecificationFactoryImpl orderSpecificationFactory;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PvzClientService pvzClientService;

    @Autowired
    private PickupPointService pickupPointService;

    @Autowired
    private OrderService orderService;

    @Bean
    public MockRestServiceServer mockRestServiceServer() {
        return MockRestServiceServer.createServer(restTemplate);
    }

    @Bean
    public UpdateDataExecutor updateDataExecutor() {
        return new UpdateDataExecutor(
                pickupPointService,
                orderService,
                pvzClientService
        );
    }
}
