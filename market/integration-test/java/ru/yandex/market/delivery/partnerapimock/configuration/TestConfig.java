package ru.yandex.market.delivery.partnerapimock.configuration;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.delivery.partnerapimock.steps.MockSteps;

@Configuration
public class TestConfig {

    @Bean
    public MockSteps mockSteps() {
        MockSteps mockSteps = new MockSteps();
        mockSteps.setContextPath("/defaultContext");
        return mockSteps;
    }

    @Bean
    public MockSteps mockStepsForJsonContextPath() {
        MockSteps mockSteps = new MockSteps();
        mockSteps.setContextPath("/defaultContextJson");
        return mockSteps;
    }

    @Bean
    public MockSteps customContextMockSteps() {
        MockSteps mockSteps = new MockSteps();
        mockSteps.setContextPath("/customContext");
        return mockSteps;
    }

    @Bean
    public MockSteps contextWithSlashesInPathMockSteps() {
        MockSteps mockSteps = new MockSteps();
        mockSteps.setContextPath("/custom/context/with/slashes");
        return mockSteps;
    }

}
