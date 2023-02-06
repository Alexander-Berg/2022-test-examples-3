package ru.yandex.market.delivery.transport_manager.service.movement.courier;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourier;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourierStatus;

class MovementCourierServiceTest extends AbstractContextualTest {
    private static final MovementCourier COURIER = new MovementCourier()
        .setId(1L)
        .setMovementId(1L)
        .setCarModel("старая добрая буханка")
        .setCarNumber("Р173НО199")
        .setName("Иван")
        .setSurname("Доставкин")
        .setPatronymic("Михайлович")
        .setPhone("+7(903) 012-11-10")
        .setPhoneAdditional("123")
        .setStatus(MovementCourierStatus.NEW)
        .setUnit(MovementCourier.Unit.ALL);

    @Autowired
    private MovementCourierService courierService;

    @BeforeEach
    void setUp() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 14, 20, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
    }

    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml",
        "/repository/movement_courier/multiple_couriers_partially_sent.xml"
    })
    @Test
    void getLatestCourier() {
        Optional<MovementCourier> latestCourier =
                courierService.getLatestCourier(1L, MovementCourier.Unit.ALL);
        softly.assertThat(latestCourier.get()).isEqualTo(COURIER);
    }
}
