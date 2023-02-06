package ru.yandex.market.regulator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @author Anastasiya Emelianova / orphie@ / 10/11/21
 */
@Configuration
@EnableWebMvc
public class MockConfig {

    @Bean
    public MockMvcFactory mockMvcFactory() {
        return new MockMvcFactory();
    }

    @Bean
    public MockMvc mockMvc(MockMvcFactory mockMvcFactory) {
        return mockMvcFactory.getMockMvc();
    }
}
