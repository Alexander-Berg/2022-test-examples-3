package ru.yandex.chemodan.app.docviewer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;

/**
 * @author nshmakov
 */
@Configuration
public class TestContextConfiguration {

    @Bean
    public TestManager testManager() {
        return new TestManager();
    }

    @Bean
    public AppName appName() {
        return new SimpleAppName("docviewer", "web");
    }

    @Bean
    public EnvironmentType environmentType() {
        return EnvironmentType.TESTS;
    }
}
