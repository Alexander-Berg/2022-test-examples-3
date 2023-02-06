package ru.yandex.market.adv.service.time;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Date: 23.11.2021
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
class TimeServiceImplTest {

    private final TimeService timeService = new TimeServiceImpl();

    @DisplayName("Получение текущего OffsetDateTime")
    @Test
    void get_correctZoneId_offsetDateTime() {
        Assertions.assertThat(timeService.get(ZoneId.systemDefault()))
                .isBefore(OffsetDateTime.now(ZoneId.systemDefault()));
    }

    @DisplayName("Получение текущего Instant")
    @Test
    void get_correct_instant() {
        Assertions.assertThat(timeService.get())
                .isBefore(Instant.now());
    }
}
