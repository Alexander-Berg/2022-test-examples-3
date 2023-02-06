package ru.yandex.market.tpl.core.domain.ds;

import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.domain.interval.localtime.PickupPointDeliveryLocalTimeIntervalCalculator;

import static org.assertj.core.api.Assertions.assertThat;

class PickupPointDeliveryLocalTimeIntervalCalculatorTest {

    private PickupPointDeliveryLocalTimeIntervalCalculator pickupPointDeliveryLocalTimeIntervalCalculator;

    @BeforeEach
    void init() {
        pickupPointDeliveryLocalTimeIntervalCalculator = new PickupPointDeliveryLocalTimeIntervalCalculator();
    }

    @Test
    void calculateForPickupPointShiftedLeftFromDefault() {
        var calculated = pickupPointDeliveryLocalTimeIntervalCalculator.calculateForPickupPoint(
                createLocalTime(7, 14),
                createLocalTime(6, 15)
        );
        assertThat(calculated).isEqualTo(createLocalTime(7, 14));
    }

    @Test
    void calculateForPickupPointShiftedRightFromDefault() {
        var calculated = pickupPointDeliveryLocalTimeIntervalCalculator.calculateForPickupPoint(
                createLocalTime(12, 20),
                createLocalTime(6, 15)
        );
        assertThat(calculated).isEqualTo(createLocalTime(12, 15));
    }

    @Test
    void calculateForPickupPointDoesNotIntersectWithDefault() {
        var calculated = pickupPointDeliveryLocalTimeIntervalCalculator.calculateForPickupPoint(
                createLocalTime(16, 21),
                createLocalTime(6, 15)
        );
        assertThat(calculated).isEqualTo(createLocalTime(16, 21));
    }

    @Test
    void calculateForPickupPointTooNarrow() {
        var calculated = pickupPointDeliveryLocalTimeIntervalCalculator.calculateForPickupPoint(
                createLocalTime(10, 13),
                createLocalTime(6, 15)
        );
        assertThat(calculated).isEqualTo(createLocalTime(10, 13));
    }

    private LocalTimeInterval createLocalTime(int left, int right) {
        return new LocalTimeInterval(LocalTime.of(left, 0), LocalTime.of(right, 0));
    }
}
