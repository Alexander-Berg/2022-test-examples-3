package ru.yandex.market.tpl.core.domain.movement;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.geobase.HttpGeobase;
import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseAddress;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.core.domain.warehouse.UpdateOrderWarehousePayload;
import ru.yandex.market.tpl.core.domain.warehouse.UpdateOrderWarehouseProducer;
import ru.yandex.market.tpl.core.domain.warehouse.UpdateOrderWarehouseService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class UpdateOrderWarehouseTest extends TplAbstractTest {
    private final UpdateOrderWarehouseProducer producer;
    private final UpdateOrderWarehouseService service;

    private final JdbcTemplate jdbcTemplate;
    private final OrderWarehouseRepository orderWarehouseRepository;
    @MockBean
    private HttpGeobase geobase;
    private final Clock clock;
    private final MovementRepository movementRepository;

    @BeforeEach
    void init() {
        LocalDate today = LocalDate.now(clock);

        OrderWarehouse warehouse = new OrderWarehouse("123", "corp", new OrderWarehouseAddress(
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
        ), Map.of(), Collections.emptyList(), null, null);
        orderWarehouseRepository.saveAndFlush(warehouse);

        Movement movement = getMovement(
                warehouse,
                today.atStartOfDay().toInstant(ZoneOffset.UTC),
                today.atStartOfDay().plusHours(1L).toInstant(ZoneOffset.UTC),
                "1",
                MovementStatus.CREATED,
                1L
        );


        Movement movement2 = getMovement(warehouse,
                today.atStartOfDay().minusHours(1L).toInstant(ZoneOffset.UTC),
                today.atStartOfDay().plusHours(1L).toInstant(ZoneOffset.UTC),
                "3",
                MovementStatus.CANCELLED,
                1L
        );

        movementRepository.saveAll(List.of(movement, movement2));
        movementRepository.flush();
    }

    @Test
    void produce() {
        producer.produce(123L);
        jdbcTemplate.query("select queue_name, task from queue_task", rs -> {
                    assertThat(rs.getString(1)).isEqualTo("UPDATE_ORDER_WAREHOUSE");
                    assertThat(rs.getString(2)).isEqualTo("{\"orderWarehouseId\":123,\"entityId\":\"123\"}");
                }
        );
    }

    @Test
    void processPayload() {
        OrderWarehouseAddress address = new OrderWarehouseAddress("abc", "abc", "abc", "abc", "abc", "abc", "abc",
                "abc", 1, new BigDecimal(55.751138), new BigDecimal(37.590003));
        OrderWarehouse orderWarehouse = new OrderWarehouse("yaId", "incorporation",
                address, new HashMap<>(), new ArrayList<>(), "descr", "cont");
        var owId = orderWarehouseRepository.saveAndFlush(orderWarehouse).getId();
        when(geobase.getRegionId(anyDouble(), anyDouble())).thenReturn(117065);
        service.processPayload(new UpdateOrderWarehousePayload("123", owId));
        assertThat(orderWarehouseRepository.findById(owId).orElseThrow().getRegionId()).isEqualTo(117065);
    }

    private Movement getMovement(OrderWarehouse warehouse, Instant movementDeliveryIntervalFrom,
                                 Instant movementDeliveryIntervalTo, String s, MovementStatus created, long l) {
        Movement movement = new Movement();
        movement.setWarehouse(warehouse);
        movement.setDeliveryIntervalFrom(movementDeliveryIntervalFrom);
        movement.setDeliveryIntervalTo(movementDeliveryIntervalTo);
        movement.setExternalId(s);
        movement.setStatus(created);
        movement.setDeliveryServiceId(l);
        return movement;
    }

}
