package ru.yandex.market.sc.core.domain.courier;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.courier.repository.CourierMapper;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.model.CourierDto;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CourierCommandServiceTest {

    private final TestFactory testFactory;
    private final CourierCommandService courierCommandService;
    private final CourierMapper courierMapper;

    @Test
    void findCourier() {
        Courier courier = testFactory.storedCourier(444L, "Ivanov Ivan");
        CourierDto courierDto = courierMapper.map(courier);

        Courier courierFound = courierCommandService.findOrCreateCourier(courierDto, true);
        assertCourierEquals(courierFound, courierDto);
    }

    @Test
    void createCourier() {
        CourierDto courierDto = courierMapper.map(new Courier(
                444L, "Ivanov Ivan", "234234", "Лада седан, баклажан", "+7123456789", "Рога и копыта", null
        ));

        Courier courierFound = courierCommandService.findOrCreateCourier(courierDto, true);
        assertCourierEquals(courierFound, courierDto);
    }

    @Test
    void updateCourierIfExists() {
        Courier courier = testFactory.storedCourier(444L, "Ivanov Ivan");
        CourierDto courierDto = courierMapper.map(courier);
        courierDto.setName("Petrov Petr");
        String newCompanyName = "Ланбо";
        assertThat(courier.getCompanyName()).isNotEqualTo(newCompanyName);
        courierDto.setCompanyName(newCompanyName);

        Courier courierFound = courierCommandService.findOrCreateCourier(courierDto, true);
        assertCourierEquals(courierFound, courierDto);
    }

    private void assertCourierEquals(Courier courierFound, CourierDto courierDto) {
        assertThat(courierFound.getId()).isEqualTo(courierDto.getId());
        assertThat(courierFound.getName()).isEqualTo(courierDto.getName());
        assertThat(courierFound.getCompanyName()).isEqualTo(courierDto.getCompanyName());
        assertThat(courierFound.getCarNumber()).isEqualTo(courierDto.getCarNumber());
        assertThat(courierFound.getCarDescription()).isEqualTo(courierDto.getCarDescription());
        assertThat(courierFound.getDeliveryServiceId()).isEqualTo(courierDto.getDeliveryServiceId());
        assertThat(courierFound.getPhone()).isEqualTo(courierDto.getPhone());
    }
}
