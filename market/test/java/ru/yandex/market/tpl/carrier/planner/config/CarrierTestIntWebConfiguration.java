package ru.yandex.market.tpl.carrier.planner.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement.PutMovementHelper;

@Import(PutMovementHelper.class)
@Configuration
public class CarrierTestIntWebConfiguration {

    @Autowired
    private MockMvc mockMvc;

    @Bean
    public MockMvcClientHttpRequestFactory clientHttpRequestFactory() {
        return new MockMvcClientHttpRequestFactory(mockMvc);
    }
}
