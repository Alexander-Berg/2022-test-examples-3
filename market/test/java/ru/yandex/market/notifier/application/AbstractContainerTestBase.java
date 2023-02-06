package ru.yandex.market.notifier.application;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.metrics.micrometer.PrometheusConfiguration;
import ru.yandex.market.notifier.config.ContainerConfiguration;
import ru.yandex.market.notifier.configuration.TestNotifierConfig;

@SpringBootTest(
        classes = AbstractContainerTestBase.TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@ContextConfiguration(classes = TestNotifierConfig.class)
@ExtendWith(SpringExtension.class)
public abstract class AbstractContainerTestBase {

    @Autowired
    protected TestRestTemplate testRestTemplate;

    @ImportAutoConfiguration({
            EmbeddedWebServerFactoryCustomizerAutoConfiguration.class,
            ServletWebServerFactoryAutoConfiguration.class,
            DispatcherServletAutoConfiguration.class,
            ContainerConfiguration.class,
            PrometheusConfiguration.class
    })
    public static class TestConfiguration {

    }
}
