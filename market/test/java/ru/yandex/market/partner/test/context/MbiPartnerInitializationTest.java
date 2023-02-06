package ru.yandex.market.partner.test.context;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import ru.yandex.market.tags.Components;
import ru.yandex.market.tags.Tests;

/**
 * Тест на инициализацию приложения.
 * <p>
 * По умолчанию если spring не может инициализировать servlet во время работы приложения, то
 * <ul>
 * <li>приложение продолжает работать
 * <li>считается, что такого bean'а нет
 * <li>обработчик запросов не находит servlet, отвечающий на запрос и возвращает 404
 * </ul>
 * <p>
 * Исходя из вышенаписанного в этом тесте мы инжектим все сервантлеты, чтобы убедится,
 * что все они инициализируются без ошибок.
 * <p>
 * Spring при инициализации bean'ов всегда создаёт Proxy, вместо простого экземпляра конкретного класса
 * (Это нужно, например для того, чтобы bean можно было позже переопределить)
 * Из-за этого мы не можем inject'ить конкретные классы, а можем инжектить только интерфейсы.
 * Поэтому здесь мы везде пишем Servlet (интерфейс), вместо конкретного класса.
 */
@ParametersAreNonnullByDefault
@Tags({@Tag(Components.MBI_PARTNER), @Tag(Tests.COMPONENT)})
class MbiPartnerInitializationTest extends FunctionalTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Test
    @DisplayName("Инициализация контекста. Тест принудительно инициализирует все lazy-init bean, описанные в .xml из папки partner/")
    void test() {
        for (String name : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition definition = beanFactory.getBeanDefinition(name);
            if (StringUtils.contains(definition.getResourceDescription(), "[partner/")
                    && !definition.isAbstract()
                    && definition.isLazyInit()) {
                beanFactory.getBean(name);
            }
        }
    }
}
