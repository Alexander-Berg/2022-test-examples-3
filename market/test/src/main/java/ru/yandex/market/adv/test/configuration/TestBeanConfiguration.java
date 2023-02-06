package ru.yandex.market.adv.test.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.adv.generator.IdGenerator;
import ru.yandex.market.adv.service.random.RandomService;
import ru.yandex.market.adv.service.time.TimeService;
import ru.yandex.market.adv.test.service.random.TestRandomService;
import ru.yandex.market.adv.test.service.time.TestTimeService;

/**
 * Класс, содержащий базовые mock для сервисов из библиотеки adv-shop/common.
 * Конфигурация bean для тестов.
 * Date: 15.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
@Configuration
public class TestBeanConfiguration {

    @Bean
    public IdGenerator idGenerator() {
        return () -> "1";
    }

    @Bean
    public TimeService timeService() {
        return new TestTimeService();
    }

    @Bean
    public RandomService randomService() {
        return new TestRandomService();
    }
}
