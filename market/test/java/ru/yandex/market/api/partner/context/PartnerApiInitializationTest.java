package ru.yandex.market.api.partner.context;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Проверяем поднятие контекста приложения и инициализацию lazy бинов.
 */
public class PartnerApiInitializationTest extends FunctionalTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Test
    @DisplayName("Инициализация контекста. Тест принудительно инициализирует все lazy-init bean, описанные в .xml из папки app-ctx/")
    void test() {
        for (String name : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition definition = beanFactory.getBeanDefinition(name);

            if ((StringUtils.contains(definition.getResourceDescription(), "[app-ctx/"))
                    && !definition.isAbstract()
                    && definition.isLazyInit()) {
                beanFactory.getBean(name);
            }
        }
    }

}
