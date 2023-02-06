package ru.yandex.market.clab.test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;

/**
 * @author anmalysh
 * @since 12.11.2018
 */
public class IntegrationTestContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String INTEGRATION_TEST_PROPERTIES = "/integration-test.properties";
    private static final String INTEGRATION_TEST_LOCAL_PROPERTIES = "/integration-test.local.properties";

    @SuppressWarnings("NullableProblems")
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        MutablePropertySources sources = applicationContext.getEnvironment().getPropertySources();
        Resource properties = new ClassPathResource(INTEGRATION_TEST_PROPERTIES);
        Resource localProperties = new ClassPathResource(INTEGRATION_TEST_LOCAL_PROPERTIES);
        try {
            if (localProperties.exists()) {
                sources.addLast(new ResourcePropertySource(localProperties));
            }
            sources.addLast(new ResourcePropertySource(properties));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ((DefaultListableBeanFactory) applicationContext.getBeanFactory()).setAllowBeanDefinitionOverriding(true);
    }
}
