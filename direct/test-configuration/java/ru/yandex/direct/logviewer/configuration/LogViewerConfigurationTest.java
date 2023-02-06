package ru.yandex.direct.logviewer.configuration;

import java.util.Map;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static ru.yandex.direct.config.EssentialConfiguration.OVERRIDING_CONFIG_BEAN_NAME;

public class LogViewerConfigurationTest {

    ConfigurableApplicationContext applicationContext;

    @BeforeEach
    public void setUp() {
        applicationContext = new AnnotationConfigApplicationContext();
    }

    @Test
    public void configIsCreated() {
        ((AnnotationConfigRegistry) applicationContext).register(LogViewerConfigurationForTest.class);
        applicationContext.refresh();
    }

    @Configuration
    @Import({LogViewerConfiguration.class})
    public static class LogViewerConfigurationForTest {
        @Bean(OVERRIDING_CONFIG_BEAN_NAME)
        public Config overridingConfig() {
            return getOverridingConfig();
        }
    }

    @Test
    public void authorizationConfigIsCreated() {
        ((AnnotationConfigRegistry) applicationContext).register(AuthorizationConfiguration.class);
        applicationContext.refresh();
    }

    @Test
    public void webAppConfigIsCreated() {
        applicationContext = new AnnotationConfigWebApplicationContext();
        ((ConfigurableWebApplicationContext) applicationContext).setServletContext(new MockServletContext());
        ((AnnotationConfigRegistry) applicationContext).register(LogViewerWebAppConfigurationForTest.class);
        applicationContext.refresh();
    }

    @Configuration
    @Import({WebAppConfiguration.class})
    public static class LogViewerWebAppConfigurationForTest {
        @Bean(OVERRIDING_CONFIG_BEAN_NAME)
        public Config overridingConfig() {
            return getOverridingConfig();
        }
    }

    private static Config getOverridingConfig() {
        return ConfigFactory.parseMap(
                Map.ofEntries(
                        Map.entry("db_config_overrides.grut_logs.pass", "memory://fake")
                )
        );
    }

    @AfterEach
    public void tearDown() {
        applicationContext.close();
    }
}
