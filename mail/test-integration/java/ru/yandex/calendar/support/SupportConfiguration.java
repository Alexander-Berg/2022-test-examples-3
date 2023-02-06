package ru.yandex.calendar.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import static io.cucumber.spring.CucumberTestContext.SCOPE_CUCUMBER_GLUE;

@Configuration
public class SupportConfiguration {
    @Bean
    @Scope(SCOPE_CUCUMBER_GLUE)
    public ServiceStorage storage() {
        return new ServiceStorage();
    }
}
