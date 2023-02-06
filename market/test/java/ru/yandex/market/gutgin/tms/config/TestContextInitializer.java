package ru.yandex.market.gutgin.tms.config;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;

/**
 * @author danfertev
 * @since 27.06.2019
 */
public class TestContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String TEST_PROPERTIES = "/02_datasources-test.properties";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        MutablePropertySources sources = applicationContext.getEnvironment().getPropertySources();
        Resource testProperties = new ClassPathResource(TEST_PROPERTIES);
        try {
            sources.addLast(new ResourcePropertySource(testProperties));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ((DefaultListableBeanFactory) applicationContext.getBeanFactory()).setAllowBeanDefinitionOverriding(true);
    }
}