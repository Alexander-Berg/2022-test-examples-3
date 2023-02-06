package ru.yandex.market.delivery.tracker.domain.entity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class DeliveryTrackTest {

    private static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2018, 1, 1, 0, 0)
            .atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    private DeliveryTrack deliveryTrack;

    @RegisterExtension
    JUnitJupiterSoftAssertions assertions = new JUnitJupiterSoftAssertions();

    @BeforeEach
    void setUp() {
        deliveryTrack = new DeliveryTrack(new DeliveryTrackMeta(), new ArrayList<>());
    }

    @Test
    void calculateStopTrackingDateZeroDuration() {
        int duration = 0;
        Date expected = Date.from(CLOCK.instant().plus(Period.ofDays(duration)));

        deliveryTrack.calculateStopTrackingDate(CLOCK, duration);

        assertions.assertThat(deliveryTrack.getDeliveryTrackMeta().getStopTrackingDate())
                .isEqualTo(expected);
    }

    @Test
    void calculateStopTrackingDate() {
        int duration = 100;
        Date expected = Date.from(CLOCK.instant().plus(Period.ofDays(duration)));

        deliveryTrack.calculateStopTrackingDate(CLOCK, duration);

        assertions.assertThat(deliveryTrack.getDeliveryTrackMeta().getStopTrackingDate())
                .isEqualTo(expected);
    }
}
