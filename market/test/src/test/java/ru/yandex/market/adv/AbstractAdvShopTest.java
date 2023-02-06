package ru.yandex.market.adv;

import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.adv.config.CommonBeanAutoconfiguration;
import ru.yandex.market.adv.test.AbstractTest;

/**
 * Общий наследник для всех тестовых классов. Нужен, чтобы честно инициализировать контекст приложения в тестах.
 * Date: 23.09.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
@SpringBootTest(
        classes = {
                CommonBeanAutoconfiguration.class
        }
)
public abstract class AbstractAdvShopTest extends AbstractTest {
}
