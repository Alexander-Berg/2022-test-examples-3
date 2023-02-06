package ru.yandex.market.crm.operatorwindow.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.operatorwindow.domain.delivery.DeliveryInterval;

public class DeliveryIntervalTest {

    @Test
    public void interval() {
        DeliveryInterval deliveryInterval = new DeliveryInterval(
                LocalDate.of(2018, 1, 1),
                LocalDate.of(2018, 2, 1),
                LocalTime.of(10, 0),
                LocalTime.of(18, 0)
        );
        Assertions.assertEquals("01.01 - 01.02 10:00 - 18:00", deliveryInterval.toShortFormat());
        Assertions.assertEquals("с 01.01 по 01.02 с 10:00 до 18:00", deliveryInterval.toRuShortFormat());
    }

    @Test
    public void intervalWithoutTime() {
        DeliveryInterval deliveryInterval = new DeliveryInterval(
                LocalDate.of(2018, 1, 1),
                LocalDate.of(2018, 2, 1),
                null,
                null
        );
        Assertions.assertEquals("01.01 - 01.02", deliveryInterval.toShortFormat());
        Assertions.assertEquals("с 01.01 по 01.02", deliveryInterval.toRuShortFormat());
    }

    @Test
    public void intervalWithEqualsDates() {
        DeliveryInterval deliveryInterval = new DeliveryInterval(
                LocalDate.of(2018, 1, 1),
                LocalDate.of(2018, 1, 1),
                LocalTime.of(10, 0),
                LocalTime.of(18, 0)
        );
        Assertions.assertEquals("01.01 10:00 - 18:00", deliveryInterval.toShortFormat());
        Assertions.assertEquals("01.01 с 10:00 до 18:00", deliveryInterval.toRuShortFormat());
    }

    @Test
    public void intervalWithoutDatesAndTimes() {
        DeliveryInterval deliveryInterval = new DeliveryInterval(
                null, null, null, null);
        Assertions.assertEquals("", deliveryInterval.toShortFormat());
        Assertions.assertEquals("", deliveryInterval.toRuShortFormat());
    }

    @Test
    public void formatInterval_empty() {
        Optional<String> actual = DeliveryInterval.formatInterval(null, null, Object::toString);
        Assertions.assertTrue(actual.isEmpty());
    }

    @Test
    public void formatInterval_onlyFrom() {
        Optional<String> actual = DeliveryInterval.formatInterval(1, null, Object::toString);
        Assertions.assertTrue(actual.isPresent());
        Assertions.assertEquals("1", actual.get());
    }

    @Test
    public void formatInterval_onlyTo() {
        Optional<String> actual = DeliveryInterval.formatInterval(null, 2, Object::toString);
        Assertions.assertTrue(actual.isPresent());
        Assertions.assertEquals("2", actual.get());
    }

    @Test
    public void formatInterval_fromTo() {
        Optional<String> actual = DeliveryInterval.formatInterval(1, 2, Object::toString);
        Assertions.assertTrue(actual.isPresent());
        Assertions.assertEquals("1 - 2", actual.get());
    }

}
