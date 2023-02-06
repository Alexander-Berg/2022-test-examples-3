package ru.yandex.market.checkout.checkouter.delivery;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.pushapi.client.validator.DeliveryTimeValidator;

public class DeliveryTimeValidatorTest {

    private DeliveryTimeValidator validator;
    private final int intervalStartHour = 1;

    @BeforeEach
    public void setUp() {
        validator = new DeliveryTimeValidator();
    }

    @Test
    public void testPositiveCases() {
        // min valid interval
        validator.validate(
                LocalTime.of(intervalStartHour, 0),
                LocalTime.of(intervalStartHour + DeliveryTimeValidator.TIME_INTERVAL_HOURS_MIN, 0)
        );
        // max valid interval
        validator.validate(
                LocalTime.of(intervalStartHour, 0),
                LocalTime.of(intervalStartHour + DeliveryTimeValidator.TIME_INTERVAL_HOURS_MAX, 0)
        );
        // last valid interval
        validator.validate(
                LocalTime.of(24 - DeliveryTimeValidator.TIME_INTERVAL_HOURS_MIN, 0),
                LocalTime.MAX.truncatedTo(ChronoUnit.MINUTES)
        );
        // last max valid interval
        validator.validate(
                LocalTime.of(24 - DeliveryTimeValidator.TIME_INTERVAL_HOURS_MAX, 0),
                LocalTime.MAX.truncatedTo(ChronoUnit.MINUTES)
        );

        // not round interval
        validator.validate(
                LocalTime.of(intervalStartHour, 15),
                LocalTime.of(intervalStartHour + DeliveryTimeValidator.TIME_INTERVAL_HOURS_MIN, 15)
        );
    }

    @Test
    public void testIntervalTooLate() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(
                    LocalTime.of(23, 0),
                    LocalTime.of(intervalStartHour + DeliveryTimeValidator.TIME_INTERVAL_HOURS_MIN, 0)
            );
        });
    }

    @Test
    public void testIntervalMissingParameters() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(
                    LocalTime.of(intervalStartHour, 0),
                    null
            );
        });
    }

    @Test
    public void testIntervalSwappedParameters() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(
                    LocalTime.of(intervalStartHour + DeliveryTimeValidator.TIME_INTERVAL_HOURS_MIN, 0),
                    LocalTime.of(intervalStartHour, 0)
            );
        });
    }
}
