package ru.yandex.market.delivery.transport_manager.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.CompositeCourierId;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementAdditionalData;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.StatusHolder;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.dto.movement.MovementStatusDateTimeDto;
import ru.yandex.market.delivery.transport_manager.repository.mappers.MovementMapper;

public class MovementMapperTest extends AbstractContextualTest {
    @Autowired
    private MovementMapper movementMapper;

    private static final Movement XML_MOVEMENT;

    static {
        XML_MOVEMENT = new Movement()
            .setExternalId("movement1")
            .setStatus(MovementStatus.NEW)
            .setWeight(15)
            .setVolume(2)
            .setPartnerId(156L)
            .setMarketId(1567L)
            .setPlannedTransportId(1L)
            .setTransportId(2L)
            .setPrice(1_000L)
            .setArrivedAt(LocalDateTime.of(2021, 9, 1, 9, 0))
            .setAdditionalData(new MovementAdditionalData(new CompositeCourierId(13L, 14L)))
            .setPriceDate(Instant.parse("2021-09-01T09:00:00.00Z").atZone(ZoneId.systemDefault()));
    }

    @Test
    @DatabaseSetup("/repository/movement/movement_test.xml")
    void getMovementTest() {
        Movement movement = movementMapper.getById(1);
        assertThatModelEquals(XML_MOVEMENT, movement);
    }

    @Test
    void createMovementTest() {
        Long movementId = movementMapper.persist(XML_MOVEMENT);
        Movement movement = movementMapper.getById(movementId);
        assertThatModelEquals(XML_MOVEMENT, movement);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/single_new_transportation.xml"
    })
    void getByTransportationTest() {
        Movement movement = movementMapper.getMovement(1);
        softly.assertThat(movement.getId()).isEqualTo(4L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/single_new_transportation.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/movement_set_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void setStatusTest() {
        softly
            .assertThat(movementMapper.setStatus(4L, MovementStatus.CANCELLED))
            .containsExactly(new StatusHolder<>(4L, null, MovementStatus.NEW, MovementStatus.CANCELLED));
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/single_new_transportation.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/movement_set_external_id_and_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void setExternalIdAndStatusTest() {
        softly
            .assertThat(movementMapper.setExternalIdAndStatus(4L, MovementStatus.CANCELLED, "externalId"))
            .containsExactly(new StatusHolder<>(4L, null, MovementStatus.NEW, MovementStatus.CANCELLED));
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation/after/movement_created_not_null_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPersist() {
        movementMapper.persist(new Movement()
            .setStatus(MovementStatus.NEW)
            .setPartnerId(1L)
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation/after/movement_created_null_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPersistNullPartner() {
        movementMapper.persist(new Movement()
            .setStatus(MovementStatus.DRAFT)
            .setPartnerId(null)
            .setPlannedTransportId(1L)
            .setTransportId(2L)
            .setPrice(1_000L)
            .setPriceDate(ZonedDateTime.of(2021, 9, 1, 12, 0, 0, 0, ZoneId.of("Europe/Moscow")))
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation/after/no_movements.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPersistNullPartnerFail() {
        Assertions.assertThrows(
            Exception.class,
            () ->
                movementMapper.persist(new Movement()
                    .setStatus(MovementStatus.NEW)
                    .setPartnerId(null)
                )
        );
    }

    @Test
    @DatabaseSetup("/repository/movement/movement_test.xml")
    void getStatus() {
        softly.assertThat(movementMapper.getStatus(1L)).isEqualTo(MovementStatus.NEW);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/single_new_transportation.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/movement_set_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCount() {
        softly
            .assertThat(movementMapper.switchStatusReturningCount(4L, MovementStatus.NEW, MovementStatus.CANCELLED))
            .isEqualTo(1);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_dependencies.xml",
        "/repository/transportation/single_new_transportation.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/movement_set_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCountWithoutPreviousStatusCheck() {
        softly
            .assertThat(movementMapper.switchStatusReturningCount(4L, null, MovementStatus.CANCELLED))
            .isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/repository/movement/movement_test.xml")
    void saveDateTime() {
        ZonedDateTime priceDate = ZonedDateTime.of(2021, 9, 1, 12, 0, 0, 0, ZoneId.of("Europe/Moscow"));
        LocalDateTime arrivedAt = LocalDateTime.of(2021, 7, 29, 12, 0, 0);

        Movement movement = movementMapper.getById(1)
            .setPlannedTransportId(1L)
            .setPrice(1_000L)
            .setPriceDate(priceDate);
        movement.setArrivedAt(arrivedAt);
        movementMapper.updateByIdAndStatus(movement);
        movementMapper.flush();
        Assertions.assertEquals(movementMapper.getById(1).getArrivedAt(), arrivedAt);
        Assertions.assertEquals(movementMapper.getById(1).getPlannedTransportId(), 1L);
        Assertions.assertEquals(movementMapper.getById(1).getTransportId(), 2L);
        Assertions.assertEquals(movementMapper.getById(1).getPrice(), 1_000L);
        Assertions.assertEquals(
            movementMapper.getById(1).getPriceDate().withZoneSameInstant(ZoneId.systemDefault()),
            priceDate.withZoneSameInstant(ZoneId.systemDefault())
        );
    }

    @Test
    @DatabaseSetup("/repository/movement/movement_test.xml")
    @ExpectedDatabase(
        value = "/repository/movement/after/movement_planned_interval_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updatePlannedInterval() {
        movementMapper.updatePlannedInterval(
            1L,
            LocalDateTime.of(2021, 5, 1, 0, 0),
            null
        );
    }

    @Test
    @DatabaseSetup("/repository/movement/movement_confirmation.xml")
    void getAtStatus() {
        List<MovementStatusDateTimeDto> actual = movementMapper.getAtStatus(
            MovementStatus.PARTNER_CREATED,
            EntityType.MOVEMENT,
            TransportationStatus.CANCELLED,
            Instant.parse("2021-07-13T14:00:00.00Z"),
            Instant.parse("2021-07-13T23:00:00.00Z")
        );
        softly.assertThat(actual)
            .containsExactlyInAnyOrder(
                new MovementStatusDateTimeDto(3, Instant.parse("2021-07-13T14:46:40.515453Z")),
                new MovementStatusDateTimeDto(4, Instant.parse("2021-07-13T22:46:40.515453Z")),
                new MovementStatusDateTimeDto(5, Instant.parse("2021-07-13T22:47:40.515453Z"))
            );
    }

    @Test
    @DatabaseSetup("/repository/movement/movement_test.xml")
    @ExpectedDatabase(
        value = "/repository/movement/after/movement_arrived_date_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void saveArrivedAt() {
        movementMapper.saveArrivedAtIfNull(2L, LocalDateTime.of(2021, 9, 1, 6, 0));
    }

    @Test
    @DatabaseSetup("/repository/movement/movement_test.xml")
    @ExpectedDatabase(
        value = "/repository/movement/movement_test.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void saveArrivedAtNotChanged() {
        movementMapper.saveArrivedAtIfNull(1L, LocalDateTime.of(2021, 9, 1, 12, 0));
    }

    @Test
    @DatabaseSetup("/repository/movement/movement_test.xml")
    @ExpectedDatabase(
        value = "/repository/movement/after/partner_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void setPartnerId() {
        movementMapper.setPartnerId(2L, 1L);
    }
}
