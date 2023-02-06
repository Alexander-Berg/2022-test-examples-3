package ru.yandex.direct.api.v5.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ApiConfigurationTest {
    AnnotationConfigApplicationContext applicationContext;

    @BeforeEach
    public void setUp() {
        applicationContext = new AnnotationConfigApplicationContext();
    }

    @Test
    public void webConfigIsCreated() {
        applicationContext.register(WebServiceConfiguration.class);
        applicationContext.refresh();
    }

    @AfterEach
    public void tearDown() {
        applicationContext.close();
    }
}
