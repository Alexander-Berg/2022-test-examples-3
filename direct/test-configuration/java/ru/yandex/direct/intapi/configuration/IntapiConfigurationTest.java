package ru.yandex.direct.intapi.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class IntapiConfigurationTest {
    AnnotationConfigApplicationContext applicationContext;

    @BeforeEach
    public void setUp() {
        applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(IntapiConfiguration.class);
    }

    @Test
    public void configIsCreated() {
        applicationContext.refresh();
    }

    @AfterEach
    public void tearDown() {
        applicationContext.close();
    }
}
