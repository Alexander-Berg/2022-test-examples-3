package ru.yandex.market.mbo.utils.test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author amaslak, yuramalinov
 */
public class MockContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    protected List<String> getExtraProperties() {
        return Collections.emptyList();
    }

    public void addTestPropertySources(MutablePropertySources propertySources) {
        try {
            for (String path : getExtraProperties()) {
                ClassPathResource resource = new ClassPathResource(path);
                propertySources.addFirst(new ResourcePropertySource(resource));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerMocks(MockRegistryPostProcessor postProcessor) {

    }

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        addTestPropertySources(ctx.getEnvironment().getPropertySources());

        MockRegistryPostProcessor postProcessor = new MockRegistryPostProcessor();
        ctx.getBeanFactory().registerSingleton("mbo.mockRegistryPostProcessor", postProcessor);
        registerMocks(postProcessor);
    }

    public static class MockRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {

        private final Map<String, Object> mockBeans = new HashMap<>();

        public void addMockBean(String beanName, Object mockInstance) {
            mockBeans.put(beanName, mockInstance);
        }

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            mockBeans.keySet().forEach(registry::removeBeanDefinition);
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            mockBeans.forEach(beanFactory::registerSingleton);
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }
    }
}
