package ru.yandex.market.abo.shoppinger.generator.turbo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

/**
 * @author imelnikov
 * @since 17.02.2021
 */
public class TurboPingerUrlcheckerServiceTest extends EmptyTest {

    @Autowired
    TurboPingerUrlcheckerService service;

    @Test
    public void testQuery() {
        service.loadHostsWithTasks();
        service.loadHostForPremod();
        service.loadHostsForContentReping();
        service.loadHostsForPriceReping();
    }
}
