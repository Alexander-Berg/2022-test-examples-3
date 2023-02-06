package ru.yandex.direct.jobs.configuration;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@ParametersAreNonnullByDefault
class DebugJobRunnerConfigurationTest {
    private AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

    @Test
    void configIsCreated() {
        applicationContext.register(DebugJobRunnerConfiguration.class);
        applicationContext.refresh();
    }

    @AfterEach
    void after() {
        applicationContext.close();
    }
}
