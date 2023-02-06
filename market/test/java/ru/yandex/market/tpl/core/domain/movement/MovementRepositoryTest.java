package ru.yandex.market.tpl.core.domain.movement;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.lms.movement.AdminMovementStatus;
import ru.yandex.market.tpl.core.domain.lms.usershift.LmsNewMovementUserShiftDetailView;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseAddress;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.service.lms.usershift.LmsCrosswarehouseUsershiftService;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class MovementRepositoryTest {

    private final MovementRepository movementRepository;
    private final OrderWarehouseRepository orderWarehouseRepository;
    private final LmsCrosswarehouseUsershiftService lmsCrosswarehouseUsershiftService;

    private final Clock clock;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    @Test
    void findAllByDeliveryDate() {
        LocalDate today = LocalDate.now(clock);

        OrderWarehouse warehouse = createWarehouse("123");

        Movement movement = getMovement(
            warehouse,
            today.atStartOfDay().minusHours(1L).toInstant(ZoneOffset.UTC),
            today.atStartOfDay().plusHours(1L).toInstant(ZoneOffset.UTC),
            "1",
            MovementStatus.CREATED,
            1L
        );

        Movement movement2 = getMovement(
            warehouse,
            today.atStartOfDay().minusHours(1L).toInstant(ZoneOffset.UTC),
            today.atStartOfDay().plusHours(1L).toInstant(ZoneOffset.UTC),
            "2",
            MovementStatus.CREATED,
            2L
        );

        Movement movement3 = getMovement(
            warehouse,
            today.atStartOfDay().minusHours(1L).toInstant(ZoneOffset.UTC),
            today.atStartOfDay().plusHours(1L).toInstant(ZoneOffset.UTC),
            "3",
            MovementStatus.CANCELLED,
            1L
        );

        Movement movement4 = getMovement(
            warehouse,
            today.atStartOfDay().minusHours(3L).toInstant(ZoneOffset.UTC),
            today.atStartOfDay().minusHours(1L).toInstant(ZoneOffset.UTC),
            "4",
            MovementStatus.CREATED,
            1L
        );

        Movement movement5 = getMovement(
            warehouse,
            today.atStartOfDay().plusHours(25L).toInstant(ZoneOffset.UTC),
            today.atStartOfDay().plusHours(27L).toInstant(ZoneOffset.UTC),
            "5",
            MovementStatus.CREATED,
            1L
        );

        movementRepository.saveAll(List.of(movement, movement2, movement3, movement4, movement5));
        List<Movement> movements = movementRepository.findAllByDeliveryDate(
                today,
                ZoneOffset.UTC,
                List.of(1L),
                Optional.empty()
        );

        assertThat(movements).containsExactly(movement);
    }

    @Test
    void findAllToPickupOnDateWithTags() {
        LocalDate today = LocalDate.now(clock);

        OrderWarehouse warehouse = createWarehouse("123");
        OrderWarehouse warehouseTo = createWarehouse("1234");

        Movement movement = MovementTestBuilder.builder()
                .externalId("1")
                .warehouse(warehouse)
                .warehouseTo(warehouseTo)
                .deliveryIntervalFrom(today.atStartOfDay().minusHours(1L).toInstant(ZoneOffset.UTC))
                .deliveryIntervalTo(today.atStartOfDay().plusHours(1L).toInstant(ZoneOffset.UTC))
                .tags(null)
                .build().get();

        Movement movement2 = MovementTestBuilder.builder()
                .externalId("2")
                .warehouse(warehouse)
                .warehouseTo(warehouseTo)
                .deliveryIntervalFrom(today.atStartOfDay().minusHours(1L).toInstant(ZoneOffset.UTC))
                .deliveryIntervalTo(today.atStartOfDay().plusHours(1L).toInstant(ZoneOffset.UTC))
                .tags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN))
                .build().get();

        Movement movement3 = MovementTestBuilder.builder()
                .externalId("3")
                .warehouse(warehouse)
                .warehouseTo(warehouseTo)
                .deliveryIntervalFrom(today.atStartOfDay().minusHours(1L).toInstant(ZoneOffset.UTC))
                .deliveryIntervalTo(today.atStartOfDay().plusHours(1L).toInstant(ZoneOffset.UTC))
                .tags(List.of("some-tag"))
                .build().get();

        movementRepository.saveAll(List.of(movement, movement2, movement3));
        List<Movement> movements = movementRepository.findAllByDeliveryDate(
                today,
                ZoneOffset.UTC,
                List.of(1L),
                Optional.of(Movement.TAG_DROPOFF_CARGO_RETURN)
        );

        assertThat(movements).containsExactly(movement, movement3);
    }

    @Test
    void saveWithMovementApiFields() {
        Movement movement = createMovement(-1L, "10");
        movementRepository.save(movement);
        commitChanges();
        assertThat(movementRepository.findByExternalId("10")
            .orElseThrow(() -> new RuntimeException("Test failed, no movement with id=10 saved"))
        ).isEqualTo(movement);
    }

    @Test
    void saveTags() {
        var tagsExpected = List.of("1", "2", "other");

        Movement movement = createMovement(-1L, "10");
        movement.setTags(tagsExpected);

        movementRepository.save(movement);
        commitChanges();

        Movement result = movementRepository.findByExternalId("10").get();
        assertThat(result.getTags()).isEqualTo(tagsExpected);
    }

    @Test
    void findCrosswarehouseMovements() {
        Movement crosswarehouseMovement = createMovement(-1L, "10");
        Movement otherMovement = createMovement(2L, "11");
        movementRepository.save(otherMovement);
        movementRepository.save(crosswarehouseMovement);
        linkFakeDsToFakeCrosswarehouseSc();
        commitChanges();

        Page<Movement> openMovements = movementRepository.findCrosswarehouseMovements(
            SortingCenter.CROSSWAREHOUSE_FAKE_SC_ID,
            Pageable.unpaged()
        );
        assertThat(openMovements).hasSize(1);
    }

    @Test
    void findCrosswarehouseMovementsByDate() {
        Movement crosswarehouseMovement = createMovement(-1L, "10");
        Movement otherCrosswarehouseMovement = createMovement(-1L, "11");
        LocalDate otherDate = LocalDate.of(2021, 8, 1);
        otherCrosswarehouseMovement.setDeliveryIntervalFrom(otherDate.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC));
        movementRepository.save(otherCrosswarehouseMovement);
        movementRepository.save(crosswarehouseMovement);
        linkFakeDsToFakeCrosswarehouseSc();
        commitChanges();

        Page<Movement> openMovements = movementRepository.findCrosswarehouseMovementsForDate(
            otherDate,
            SortingCenter.CROSSWAREHOUSE_FAKE_SC_ID,
            Pageable.unpaged()
        );
        assertThat(openMovements).hasSize(1);
    }

    @Test
    void findCrosswarehouseMovementsByStatus() {
        Movement crosswarehouseMovement = createMovement(-1L, "10");
        Movement otherCrosswarehouseMovement = createMovement(-1L, "11");
        otherCrosswarehouseMovement.setStatus(MovementStatus.DELIVERED_TO_SC);
        movementRepository.save(otherCrosswarehouseMovement);
        movementRepository.save(crosswarehouseMovement);
        linkFakeDsToFakeCrosswarehouseSc();
        commitChanges();

        Page<Movement> openMovements = movementRepository.findCrosswarehouseMovementsForStatus(
            AdminMovementStatus.DELIVERED_TO_SC.name(),
            SortingCenter.CROSSWAREHOUSE_FAKE_SC_ID,
            Pageable.unpaged()
        );
        assertThat(openMovements).hasSize(1);
    }

    @Test
    void findCrosswarehouseMovementsByCourierFoundStatus() {
        Movement crosswarehouseMovement = createMovement(-1L, "10");
        Movement otherCrosswarehouseMovement = createMovement(-1L, "11");
        crosswarehouseMovement.setStatus(MovementStatus.INITIALLY_CONFIRMED);

        var savedMovement = movementRepository.save(crosswarehouseMovement);
        movementRepository.save(otherCrosswarehouseMovement);

        addFakeCompany();
        linkFakeDsToFakeCrosswarehouseSc();

        var newShiftDto = new LmsNewMovementUserShiftDetailView()
            .setLinkedMovement(savedMovement.getId())
            .setName("Курьер")
            .setSurname("Курьерыч")
            .setCompanyId(100L);

        lmsCrosswarehouseUsershiftService.createUserShiftForCrosswarehouseMovement(newShiftDto);

        commitChanges();

        Page<Movement> openMovements = movementRepository.findCrosswarehouseMovementsForStatus(
            AdminMovementStatus.COURIER_FOUND.name(),
            SortingCenter.CROSSWAREHOUSE_FAKE_SC_ID,
            Pageable.unpaged()
        );
        assertThat(openMovements).hasSize(1);
    }

    @Test
    void confirmMovement() {
        Movement crosswarehouseMovement = createMovement(-1L, "10");
        crosswarehouseMovement.confirm();
        assertThat(crosswarehouseMovement.getStatus()).isEqualTo(MovementStatus.INITIALLY_CONFIRMED);
    }

    private void linkFakeDsToFakeCrosswarehouseSc() {
        jdbcTemplate.execute(
            "INSERT INTO partner\n" +
                "(id, type, name, latitude, longitude, start_time, end_time)\n" +
                "VALUES\n" +
                "(-2, 'MARKET_SORTING_CENTER', 'Виртуальный СЦ', 0, 0, '06:00:00', '20:00:00');");
        jdbcTemplate.execute("INSERT INTO partner_mapping (delivery_service_id, sorting_center_id) VALUES (-1, -2)");
    }

    private void addFakeCompany() {
        jdbcTemplate.execute("INSERT INTO company " +
            "(id, created_at, updated_at, name, login, phone_number, taxpayer_number, juridical_address, natural_address, deactivated, campaign_id, ogrn, legal_form) " +
            "VALUES " +
            "(100, now(), now(), 'FAKE', 'fake', '79000009988', 123, 'Msk', 'Msk', false, 123, 456, 'OOO');");
    }

    private Movement createMovement(long deliveryServiceId, String externalId) {
        LocalDate today = LocalDate.now(clock);
        var movement = getMovement(
            createWarehouse("123"),
            today.atStartOfDay().plusHours(25L).toInstant(ZoneOffset.UTC),
            today.atStartOfDay().plusHours(27L).toInstant(ZoneOffset.UTC),
            externalId,
            MovementStatus.CREATED,
            deliveryServiceId
        );
        movement.setWarehouseTo(createWarehouse("234"));
        movement.setPallets(15);
        return movement;
    }

    private void commitChanges() {
        movementRepository.flush();
        entityManager.clear();
    }

    private OrderWarehouse createWarehouse(String yandexId) {
        return orderWarehouseRepository.saveAndFlush(
            new OrderWarehouse(yandexId, "corp", new OrderWarehouseAddress(
                "asdf",
                "asdf",
                "asdf",
                "asdf",
                "asdf",
                "asdf",
                "asdf",
                "asdf",
                1,
                BigDecimal.ZERO,
                BigDecimal.ZERO
            ), Map.of(), Collections.emptyList(), null, null));
    }

    private Movement getMovement(
        OrderWarehouse warehouse,
        Instant movementDeliveryIntervalFrom,
        Instant movementDeliveryIntervalTo,
        String externalId,
        MovementStatus created,
        long deliveryServiceId
    ) {
        Movement movement = new Movement();
        movement.setWarehouse(warehouse);
        movement.setDeliveryIntervalFrom(movementDeliveryIntervalFrom);
        movement.setDeliveryIntervalTo(movementDeliveryIntervalTo);
        movement.setExternalId(externalId);
        movement.setStatus(created);
        movement.setDeliveryServiceId(deliveryServiceId);
        return movement;
    }
}
