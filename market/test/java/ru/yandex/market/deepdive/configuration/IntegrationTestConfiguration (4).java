package ru.yandex.market.deepdive.configuration;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ru.yandex.market.deepdive.domain.client.PvzClient;
import ru.yandex.market.deepdive.domain.order.OrderRepository;
import ru.yandex.market.deepdive.domain.order.OrderService;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;
import ru.yandex.market.deepdive.executor.UpdateDataExecutor;

@Configuration
@ComponentScan("ru.yandex.market.deepdive.domain")
@Import({
        DeepDiveExternalConfiguration.class,
        ObjectMapperConfiguration.class,
})
@EnableWebMvc
public class IntegrationTestConfiguration {
    @Value("${lom.api.url}")
    private String host;

    @Autowired
    private PvzClient pvzClient;

    @Autowired
    private PickupPointService pickupPointService;

    @Autowired
    private OrderService orderService;

    @Bean
    public RestTemplate clientRestTemplate(ObjectMapper objectMapper) {
        return new RestTemplateBuilder()
                .messageConverters(Arrays.asList(
                        new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        jsonConverter(objectMapper),
                        new ByteArrayHttpMessageConverter()
                ))
                .build();
    }

    @Bean
    @Primary
    public UpdateDataExecutor updateDataExecutor() {
        return new UpdateDataExecutor(pvzClient, pickupPointService, orderService);
    }

    private MappingJackson2HttpMessageConverter jsonConverter(ObjectMapper objectMapper) {
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    @Bean
    @Primary
    public OrderRepository orderRepository() {
        return Mockito.mock(OrderRepository.class);
    }

    @Bean
    @Primary
    public PickupPointRepository pickupPointRepository() {
        return Mockito.mock(PickupPointRepository.class);
    }

}
