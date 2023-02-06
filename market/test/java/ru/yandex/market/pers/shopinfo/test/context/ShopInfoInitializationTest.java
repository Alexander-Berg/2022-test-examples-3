package ru.yandex.market.pers.shopinfo.test.context;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;

/**
 * Проверяем поднятие контекста приложения и инициализацию lazy бинов.
 */
public class ShopInfoInitializationTest extends FunctionalTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    /**
     * Тест принудительно инициализирует все lazy-init bean, описанные в .xml из директории context/.
     */
    @Test
    @DisplayName("Инициализация контекста")
    public void test() {
        for (String name : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition definition = beanFactory.getBeanDefinition(name);

            if ((StringUtils.contains(definition.getResourceDescription(), "[context/"))
                    && !definition.isAbstract()
                    && definition.isLazyInit()) {
                beanFactory.getBean(name);
            }
        }
    }
}
