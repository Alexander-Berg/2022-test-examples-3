package ru.yandex.market.tpl.carrier.lms.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.planner.lms.movement.LmsMovementUpdateDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
class LmsMovementControllerTest extends LmsControllerTest {

    private final MovementGenerator movementGenerator;
    private final MovementRepository movementRepository;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TestUserHelper testUserHelper;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    private Movement movement;
    private Movement movement2;

    @BeforeEach
    void setUp() {
        testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        movement = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                .deliveryIntervalFrom(Instant.now(clock))
                .deliveryIntervalTo(Instant.now(clock).plus(10, ChronoUnit.HOURS))
                .build());

        movement2 = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                .deliveryIntervalFrom(Instant.now(clock))
                .deliveryIntervalTo(Instant.now(clock).plus(10, ChronoUnit.HOURS))
                .build());
    }

    @SneakyThrows
    @Test
    void shouldReturnMovementsGrid() {
        mockMvc.perform(get("/LMS/carrier/movements")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(2)));
    }

    @SneakyThrows
    @Test
    void shouldReturnMovementDetails() {
        mockMvc.perform(get("/LMS/carrier/movements/" + movement2.getId())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item.id").value(movement2.getId()));
    }

    @SneakyThrows
    @Test
    void shouldUpdateMovement() {
        Instant newDeliveryIntervalFrom = movement2.getDeliveryIntervalFrom().plus(10, ChronoUnit.HOURS);
        Instant newDeliveryIntervalTo = movement2.getDeliveryIntervalTo().plus(10, ChronoUnit.HOURS);

        LmsMovementUpdateDto dto = new LmsMovementUpdateDto();
        dto.setId(movement2.getId());
        dto.setDeliveryIntervalFrom(newDeliveryIntervalFrom.toString());
        dto.setDeliveryIntervalTo(newDeliveryIntervalTo.toString());

        mockMvc.perform(put("/LMS/carrier/movements/" + movement2.getId())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(dto))
        )
                .andExpect(status().isOk());

        movement2 = movementRepository.findById(movement2.getId()).orElseThrow();

        Assertions.assertEquals(movement2.getDeliveryIntervalFrom(), newDeliveryIntervalFrom);
        Assertions.assertEquals(movement2.getDeliveryIntervalTo(), newDeliveryIntervalTo);
    }

}
