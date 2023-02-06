package ru.yandex.market.tpl.core.domain.pickup;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class LocalTimeIntervalTest {

    @Test
    void test() {
        LocalTimeInterval normal = new LocalTimeInterval(LocalTime.of(7, 0), LocalTime.of(22, 0));
        assertThat(normal.asCoreInterval())
                .isEqualTo(ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval.valueOf("07:00-22:00"));

        LocalTimeInterval notNormal = new LocalTimeInterval(LocalTime.of(7, 0), LocalTime.of(2, 59));
        assertThat(notNormal.asCoreInterval())
                .isEqualTo(ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval.valueOf("07:00-23:59"));
    }

}
