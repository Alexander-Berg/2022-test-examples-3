package ru.yandex.market.logistic.gateway.service.converter;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.delivery.transport_manager.model.dto.MovementCourierDto;
import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.common.model.common.Car;
import ru.yandex.market.logistic.gateway.common.model.common.Courier;
import ru.yandex.market.logistic.gateway.common.model.common.Person;
import ru.yandex.market.logistic.gateway.common.model.common.Phone;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;

public class TmConverterTest extends BaseTest {

    @Test
    public void getMovement() {
        Courier courier = Courier.builder()
                .setPartnerId(ResourceId.builder().build())
                .setPersons(List.of(
                    Person.builder("Иван")
                        .setSurname("Доставкин")
                        .setPatronymic("Михайлович")
                        .setId(1L)
                        .build()
                ))
                .setPhone(Phone.builder("+79991112233").build())
                .setCar(Car.builder("Р173НО199")
                        .setDescription("-21000")
                        .setBrand("Some")
                        .setModel("Model")
                        .setTrailerNumber("123")
                        .build())
                .build();
        var converted = TmConverter.convert(courier);
        var expected = MovementCourierDto.builder()
            .carModel("Model")
            .carNumber("Р173НО199")
            .carBrand("Some")
            .carTrailerNumber("123")
            .courierUid("-21000")
            .name("Иван")
            .surname("Доставкин")
            .patronymic("Михайлович")
            .phone("+79991112233")
            .build();
        assertions.assertThat(converted).isEqualTo(expected);
    }
}
