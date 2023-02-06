package ru.yandex.market.shop.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.ping.TmsAvailabilityChecker;
import ru.yandex.market.shop.FunctionalTest;

/**
 * Тест для проверки работы тестовой джобы HelloWorldExecutor и того, что контекст поднимается.
 *
 * @link ru.yandex.market.shop.config.TmsTasksConfig.
 */
class TmsTasksConfigTest extends FunctionalTest {

    @Autowired
    private TmsAvailabilityChecker tmsAvailabilityChecker;

    @Test
    void checkTms() {
        CheckResult expected = CheckResult.OK;
        CheckResult actual = tmsAvailabilityChecker.check();
        Assertions.assertEquals(expected, actual);
    }
}
