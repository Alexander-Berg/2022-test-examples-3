package ru.yandex.market.delivery.transport_manager.repository;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourier;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourierStatus;
import ru.yandex.market.delivery.transport_manager.model.enums.OwnershipType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.movement_courier.MovementCourierMapper;

public class MovementCourierMapperTest extends AbstractContextualTest {

    private static final MovementCourier COURIER = new MovementCourier()
        .setId(1L)
        .setMovementId(1L)
        .setCarModel("старая добрая буханка")
        .setCarBrand("2104")
        .setCarNumber("Р173НО199")
        .setCarTrailerNumber(null)
        .setOwnershipType(OwnershipType.RENT)
        .setExternalId("ext-1")
        .setName("Иван")
        .setSurname("Доставкин")
        .setPatronymic("Михайлович")
        .setPhone("+7(903) 012-11-10")
        .setPhoneAdditional("123")
        .setCourierUid("1L")
        .setStatus(MovementCourierStatus.NEW)
        .setUnit(MovementCourier.Unit.ALL);

    private static final MovementCourier COURIER2 = new MovementCourier()
        .setId(6L)
        .setMovementId(2L)
        .setCarModel("старая добрая буханка")
        .setCarBrand("2104")
        .setCarNumber("Р173НО199")
        .setCarTrailerNumber(null)
        .setOwnershipType(OwnershipType.RENT)
        .setName("Дим")
        .setSurname("Галкин")
        .setPatronymic("Димыч")
        .setPhone("+7(903) 012-11-10")
        .setPhoneAdditional("123")
        .setStatus(MovementCourierStatus.NEW)
        .setUnit(MovementCourier.Unit.ALL);

    @Autowired
    private MovementCourierMapper movementCourierMapper;

    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml",
        "/repository/movement_courier/multiple_couriers.xml"
    })
    @Test
    void getById() {
        MovementCourier courier = movementCourierMapper.getById(1L);
        softly.assertThat(courier).isEqualTo(COURIER);
    }

    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml",
        "/repository/movement_courier/multiple_couriers.xml"
    })
    @Test
    void findLatest() {
        MovementCourier courier = movementCourierMapper.findLatest(1L);
        softly.assertThat(courier).isEqualTo(COURIER);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/single_new_transportation.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/movement_courier_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void saveFirstCourier() {
        movementCourierMapper.insert(new MovementCourier()
            .setMovementId(4L)
            .setCarModel("старая добрая буханка")
            .setCarTrailerNumber("1234")
            .setCarBrand("2104")
            .setOwnershipType(OwnershipType.RENT)
            .setCarNumber("Р173НО199")
            .setName("Иван")
            .setSurname("Доставкин")
            .setPatronymic("Михайлович")
            .setPhone("+7(903) 012-11-10")
            .setPhoneAdditional("123")
            .setYandexUid(-1L)
            .setCourierUid("-1L")
            .setExternalId("ext-4")
            .setStatus(MovementCourierStatus.SENT)
        );
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/single_new_transportation.xml",
        "/repository/transportation/after/movement_courier_saved.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/second_courier_inserted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void saveSecondCourier() {
        movementCourierMapper.insert(new MovementCourier()
            .setMovementId(4L)
            .setCarModel("нестарая недобрая небуханка")
            .setCarNumber("О173ОО199")
            .setName("Неиван")
            .setSurname("Недоставкин")
            .setPatronymic("Немихайлович")
            .setPhone("+7(910) 111-11-11")
            .setPhoneAdditional("123")
            .setYandexUid(-2L)
            .setCourierUid("-2L")
            .setStatus(MovementCourierStatus.SENT)
        );
    }

    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml",
        "/repository/movement_courier/multiple_couriers.xml"
    })
    @Test
    void getCouriersByMovementIds() {
        softly.assertThat(movementCourierMapper.getCouriersByMovementIds(List.of(1L))).containsExactly(COURIER);
        softly.assertThat(movementCourierMapper.getCouriersByMovementIds(List.of(2L))).containsExactly(COURIER2);
    }
}
