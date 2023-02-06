package ru.yandex.market.mboc.integration.test;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.mboc.common.config.AppPropertySourcesContextInitializer;

/**
 * Общая конфигурация для интеграционных тестов.
 * Тесты не подминают контект всего mbo-app.
 * Это сделано специально, так как эти интеграционные тесты должны тестировать только ручки.
 *
 * @author s-ermakov
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {CommonConfiguration.class}, initializers = AppPropertySourcesContextInitializer.class)
public abstract class BaseHttpIntegrationTestClass {
    protected static final Logger log = LoggerFactory.getLogger(BaseHttpIntegrationTestClass.class);

    @Autowired
    protected TestRestTemplate restTemplate;
}
