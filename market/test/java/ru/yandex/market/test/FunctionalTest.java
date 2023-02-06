package ru.yandex.market.test;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;


@SpringJUnitConfig(locations = "classpath:functional-test-config.xml")
public abstract class FunctionalTest {

    protected String urlBasePrefix;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    public void init() {

    }
}
