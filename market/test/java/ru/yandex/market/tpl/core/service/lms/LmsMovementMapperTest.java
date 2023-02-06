package ru.yandex.market.tpl.core.service.lms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.front.library.dto.ReferenceObject;
import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.core.domain.lms.movement.AdminMovementStatus;
import ru.yandex.market.tpl.core.domain.lms.movement.LmsMovementDetailView;
import ru.yandex.market.tpl.core.domain.lms.movement.LmsMovementGridView;
import ru.yandex.market.tpl.core.domain.lms.movement.LmsMovementMapper;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseAddress;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LmsMovementMapperTest {

    LmsMovementMapper movementMapper = Mappers.getMapper(LmsMovementMapper.class);

    @Test
    void detailTest() {
        var converted = movementMapper.map(movement(),
            AdminMovementStatus.INITIALLY_CONFIRMED,
            null,
            null
        );
        Assertions.assertThat(converted).isEqualTo(converted());
    }

    @Test
    void gridTest() {
        var converted = movementMapper.mapGrid(movement(), AdminMovementStatus.INITIALLY_CONFIRMED);
        Assertions.assertThat(converted).isEqualToComparingFieldByField(convertedGrid());
    }

    private Movement movement() {
        var movement = new Movement();
        movement.setId(1L);
        movement.setStatus(MovementStatus.INITIALLY_CONFIRMED);
        movement.setExternalId("TMM100");
        movement.setWarehouse(
            getWarehouse("Достаточно длинное описание чтобы не влезать в 64 символа вот оно и заканчивается")
        );
        movement.setWarehouseTo(
            getWarehouse(null)
        );
        movement.setDeliveryIntervalFrom(LocalDateTime.of(LocalDate.of(2021, 7, 5), LocalTime.of(9, 0, 0))
            .toInstant(ZoneOffset.UTC));
        movement.setDeliveryIntervalTo(LocalDateTime.of(LocalDate.of(2021, 7, 6), LocalTime.of(9, 0, 0))
            .toInstant(ZoneOffset.UTC));
        movement.setPallets(2);
        movement.setVolume(BigDecimal.valueOf(50L));
        movement.setWeight(BigDecimal.valueOf(100L));
        return movement;
    }

    private OrderWarehouse getWarehouse(String description) {
        return new OrderWarehouse(
            "yid",
            "corp",
            new OrderWarehouseAddress(
                "Россия, МО, склад Софьино",
                "Россия",
                "Московская область",
                "Московская область",
                "Софьино",
                "Логистическая",
                "2",
                "",
                1,
                BigDecimal.ZERO,
                BigDecimal.ZERO
            ),
            Map.of(),
            Collections.emptyList(),
            description,
            null
        );
    }

    private LmsMovementDetailView converted() {
        return new LmsMovementDetailView(
            1L,
            "Перевозка #1",
            100L,
            new ReferenceObject(
                "yid",
                "Достаточно длинное описание чтобы не влезать в 64 символа вот...",
                "lms/logistics-point"
            ),
            new ReferenceObject(
                "yid",
                "Точка по адресу: г. Московская область, Логистическая, 2, стр. , к. ",
                "lms/logistics-point"
            ),
            2L,
            100L,
            50L,
            AdminMovementStatus.INITIALLY_CONFIRMED,
            LocalDateTime.of(LocalDate.of(2021, 7, 5), LocalTime.NOON),
            LocalDateTime.of(LocalDate.of(2021, 7, 6), LocalTime.NOON),
            null,
            null
        );
    }

    private LmsMovementGridView convertedGrid() {
        return new LmsMovementGridView(
            1L,
            LocalDate.of(2021, 7, 5),
            new ReferenceObject(
                "yid",
                "Достаточно длинное описание чтобы не влезать в 64 символа вот...",
                "lms/logistics-point"
            ),
            new ReferenceObject(
                "yid",
                "Точка по адресу: г. Московская область, Логистическая, 2, стр. , к. ",
                "lms/logistics-point"
            ),
            LocalDateTime.of(LocalDate.of(2021, 7, 5), LocalTime.NOON),
            LocalDateTime.of(LocalDate.of(2021, 7, 6), LocalTime.NOON),
            AdminMovementStatus.INITIALLY_CONFIRMED
        );
    }
}
