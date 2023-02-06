package ru.yandex.market.adv.test.service.time;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.AbstractAdvShopTest;
import ru.yandex.market.adv.service.time.TimeService;

/**
 * Date: 23.11.2021
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
class TestTimeServiceTest extends AbstractAdvShopTest {

    @Autowired
    private TimeService timeService;

    @DisplayName("Получение текущего заранее заданного OffsetDateTime")
    @Test
    void get_correctZoneId_offsetDateTime() {
        Assertions.assertThat(timeService.get(ZoneId.systemDefault()))
                .isEqualTo(OffsetDateTime.of(2021, 10, 21, 13, 42, 53, 0, ZoneOffset.of("+03:00")));
    }

    @DisplayName("Получение текущего заранее заданного Instant")
    @Test
    void get_correct_instant() {
        Assertions.assertThat(timeService.get())
                .isEqualTo(Instant.ofEpochSecond(1634812973L));
    }
}
