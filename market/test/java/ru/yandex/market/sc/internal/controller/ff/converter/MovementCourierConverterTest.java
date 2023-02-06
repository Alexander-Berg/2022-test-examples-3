package ru.yandex.market.sc.internal.controller.ff.converter;

import java.text.MessageFormat;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.common.Courier;
import ru.yandex.market.logistic.api.model.common.Person;
import ru.yandex.market.sc.internal.ff.converter.MovementCourierConverter;

import static org.assertj.core.api.Assertions.assertThat;

public class MovementCourierConverterTest {

    @Test
    public void courierNameWithoutSurname() {
        String name = "test name";
        var person = Person.builder(name).build();
        Courier courier = Courier.builder().setPersons(List.of(person)).build();
        String courierName = MovementCourierConverter.getName(courier);
        assertThat(courierName).isEqualTo(name);
    }

    @Test
    public void courierNameWithSurname() {
        String name = "test name";
        String surname = "test surname";
        var person = Person.builder(name).setSurname(surname).build();

        Courier courier = Courier.builder().setPersons(List.of(person)).build();
        String courierName = MovementCourierConverter.getName(courier);
        assertThat(courierName).isEqualTo(MessageFormat.format("{0} {1}", surname, name));
    }
}
