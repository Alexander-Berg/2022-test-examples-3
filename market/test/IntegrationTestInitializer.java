package ru.yandex.market.mbo.utils.test;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Инициализатор тестов.
 * Который умеет:
 * - форсировать ленивую инициализацию всех бинов в контейнере (чтобы не стартовать то, что не используется)
 * - инициализировать окружение, если оно вдруг оказалось пустым
 * - подмешивать заданные extraProperties PropertyPlaceholderConfigurer.locations, который найдёт в контексте
 * - подмешивать динамическое свойство (сейчас - test-seed - чиселка, которую можно использовать в разных местах для
 * суффиксов таблиц, скажем)
 * <p>
 * В совокупности с {@link IntegrationTestBase}:
 * - инциализирует путь к tnsnames.ora, если не задан
 * - позволяет писать интеграционные тесты поверх боевого контекста, см.
 * - ru.yandex.market.mbo.MboLiteIntegrationTestBase
 * - ru.yandex.market.mbo.gurulight.MbologsSaasServiceTest
 *
 * @author amaslak, yuramalinov
 */
public class IntegrationTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final int RANDOM_MIN = 10_000;
    private static final int RANDOM_MAX = 1_000_000;
    private static final Logger log = Logger.getLogger(IntegrationTestInitializer.class);
    protected String[] extraProperties = new String[]{};
    protected boolean forceLazyInit = false;

    protected List<String> getExtraProperties() {
        return Arrays.asList(extraProperties);
    }

    protected void setDynamicProperties(Properties properties) {
        properties.setProperty("test-seed",
            String.valueOf(ThreadLocalRandom.current().nextInt(RANDOM_MIN, RANDOM_MAX)));
    }

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        RegistryPostProcessor postProcessor = new RegistryPostProcessor();
        ctx.addBeanFactoryPostProcessor(postProcessor);
    }

    public class RegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            injectExtraProperties(registry);

            if (forceLazyInit) {
                markAllBeansLazy(registry);
            }
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            if (forceLazyInit) {
                markAllBeansLazy(beanFactory);
            }
        }

        private void injectExtraProperties(BeanDefinitionRegistry registry) {
            if (!registry.containsBeanDefinition("propertyConfigurer")) {
                if (extraProperties.length > 0) {
                    // Если будет какая-то реальная ситуация, можно решить подругому, а пока просто предупредим.
                    throw new IllegalStateException("Can't find propertyConfigurer bean in context, " +
                        "can't add extra properties");
                }
            } else {
                @SuppressWarnings("unchecked")
                List<Object> locations = (List<Object>) registry
                    .getBeanDefinition("propertyConfigurer")
                    .getPropertyValues()
                    .getPropertyValue("locations")
                    .getValue();
                List<String> extraProps = getExtraProperties();

                // Remove existing properties, so that relative order of extra properties is right
                locations.removeIf(location ->
                    location instanceof TypedStringValue
                        && extraProps.contains(((TypedStringValue) location).getValue()));

                // Add it to the end of list so that it takes precedence but
                locations.addAll(extraProps);

                // Add dynamic test properties to the end
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    Properties properties = new Properties();
                    setDynamicProperties(properties);
                    properties.store(outputStream, null);
                    locations.add(new ByteArrayResource(outputStream.toByteArray(), "dynamic"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void markAllBeansLazy(BeanDefinitionRegistry registry) {
            for (String beanName : registry.getBeanDefinitionNames()) {
                BeanDefinition definition = registry.getBeanDefinition(beanName);
                if (!definition.isLazyInit()) {
                    definition.setLazyInit(true);
                }
            }
        }

        private void markAllBeansLazy(ConfigurableListableBeanFactory beanFactory) {
            for (String beanName : beanFactory.getBeanDefinitionNames()) {
                BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
                if (!definition.isLazyInit()) {
                    definition.setLazyInit(true);
                }
            }
        }
    }
}
