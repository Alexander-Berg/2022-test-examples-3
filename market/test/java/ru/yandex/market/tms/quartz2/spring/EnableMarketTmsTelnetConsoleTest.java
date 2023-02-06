package ru.yandex.market.tms.quartz2.spring;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.tms.quartz2.config.FunctionalTest;
import ru.yandex.market.tms.quartz2.config.FunctionalTestConfig;
import ru.yandex.market.tms.quartz2.spring.config.TmsCommandsConfig;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration
class EnableMarketTmsTelnetConsoleTest extends FunctionalTest {

    @Autowired
    private TestExecutionStateHolder testExecutionStateHolder;

    @Test
    void testEnableMarketTmsContextInit() throws InterruptedException {
        assertTrue(testExecutionStateHolder.getLatch().await(10, TimeUnit.SECONDS));
    }

    @Configuration
    @Import({
            FunctionalTestConfig.class,
            TmsCommandsConfig.class,
            EnableMarketTmsTasksConfig.class
    })
    static class Config {
    }

}
