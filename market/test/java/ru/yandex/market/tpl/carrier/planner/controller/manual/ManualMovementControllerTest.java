package ru.yandex.market.tpl.carrier.planner.controller.manual;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.carrier.planner.manual.movement.ManualCreateMovementDto;
import ru.yandex.market.tpl.carrier.planner.manual.movement.ManualMovementDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ManualMovementControllerTest extends BasePlannerWebTest {

    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final ObjectMapper tplObjectMapper;

    private OrderWarehouse orderWarehouse;
    private OrderWarehouse orderWarehouseTo;

    @BeforeEach
    void setUp() {
        orderWarehouse = orderWarehouseGenerator.generateWarehouse();
        orderWarehouseTo = orderWarehouseGenerator.generateWarehouse();
    }

    @SneakyThrows
    @Test
    void shouldCreateMovement() {

        Instant deliveryIntervalFrom = ZonedDateTime.of(
                2021, 6, 28, 15, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID
        ).toInstant();
        Instant deliveryIntervalTo = ZonedDateTime.of(
                2021, 6, 28, 18, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID
        ).toInstant();


        String responseContent = mockMvc.perform(
                post("/manual/movements/create")
                        .content(tplObjectMapper.writeValueAsString(
                                ManualCreateMovementDto.builder()
                                        .externalId("asdasd")
                                        .deliveryIntervalFrom(deliveryIntervalFrom)
                                        .deliveryIntervalTo(deliveryIntervalTo)
                                        .deliveryServiceId(2234562L)
                                        .pallets(123)
                                        .weight(new BigDecimal("34.56"))
                                        .volume(new BigDecimal("56.78"))
                                        .orderWarehouseId(orderWarehouse.getId())
                                        .orderWarehouseToId(orderWarehouseTo.getId())
                                        .build()
                        ))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var movementDto = tplObjectMapper.readValue(responseContent, ManualMovementDto.class);

        Assertions.assertThat(movementDto.getId()).isNotNull();
        Assertions.assertThat(movementDto.getDeliveryServiceId()).isEqualTo(2234562L);
        Assertions.assertThat(movementDto.getPallets()).isEqualTo(123);
        Assertions.assertThat(movementDto.getWeight()).isEqualTo(new BigDecimal("34.56"));
        Assertions.assertThat(movementDto.getVolume()).isEqualTo(new BigDecimal("56.78"));
        Assertions.assertThat(movementDto.getWarehouse().getYandexId()).isEqualTo(orderWarehouse.getYandexId());
        Assertions.assertThat(movementDto.getWarehouseTo().getYandexId()).isEqualTo(orderWarehouseTo.getYandexId());


    }
}
