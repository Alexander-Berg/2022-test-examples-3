package ru.yandex.market.crm.operatorwindow.http.controller.util.validators;

import java.time.LocalTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.operatorwindow.http.controller.api.view.TimeIntervalData;

public class TimeIntervalValidatorTest {

    private static final TimeIntervalValidator VALIDATOR = new TimeIntervalValidator();

    @Test
    public void validValue() {
        Assertions.assertTrue(isValid(new TimeIntervalData(
                LocalTime.of(1, 42),
                LocalTime.of(16, 3)
        )));
    }

    @Test
    public void invalidValue() {
        Assertions.assertFalse(isValid(new TimeIntervalData(
                LocalTime.of(17, 42),
                LocalTime.of(16, 3)
        )));
    }

    @Test
    public void fromValueNull() {
        Assertions.assertFalse(isValid(new TimeIntervalData(
                null,
                LocalTime.of(16, 3)
        )));
    }

    @Test
    public void toValueNull() {
        Assertions.assertFalse(isValid(new TimeIntervalData(
                LocalTime.of(1, 42),
                null
        )));
    }

    @Test
    public void fromAndToValueNull() {
        Assertions.assertTrue(isValid(new TimeIntervalData(
                null,
                null
        )));
    }

    private boolean isValid(TimeIntervalData interval) {
        return VALIDATOR.isValid(interval, null);
    }
}
