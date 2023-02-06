package ru.yandex.market.logistics.management.controller.segment;

import java.time.LocalTime;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.UpdateDropshipLogisticMovementRequest;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.ActivityStatus;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DatabaseSetup("/data/service/combinator/db/before/service_codes.xml")
@DatabaseSetup("/data/controller/logisticSegment/before/prepare_data.xml")
@DatabaseSetup(
    value = "/data/controller/logisticSegment/before/dropship_logistic_movements_additional_data.xml",
    type = DatabaseOperation.REFRESH
)
@DisplayName("Обновление информации о логистических перемещениях (связях партнёров в новой модели) из дропшипов")
public class LogisticSegmentControllerUpdateDropshipLogisticMovementTest
    extends AbstractContextualAspectValidationTest {

    @Test
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/updated_dropship_logistic_movement.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Обновление информации о логистических перемещениях (связях партнёров в новой модели) из дропшипов")
    void testUpdateDropshipLogisticMovement() throws Exception {
        UpdateDropshipLogisticMovementRequest request = UpdateDropshipLogisticMovementRequest.builder()
            .fromPartnerId(1L)
            .toPartnerId(2L)
            .toLogisticsPointId(102L)
            .movementPartnerId(2L)
            .cutoffTime(LocalTime.of(20, 30))
            .shipmentSchedule(Set.of(
                new ScheduleDayResponse(null, 3, LocalTime.of(15, 0), LocalTime.of(22, 0), true),
                new ScheduleDayResponse(null, 5, LocalTime.of(15, 0), LocalTime.of(22, 0), true)
            ))
            .status(ActivityStatus.INACTIVE)
            .build();

        mockMvc.perform(
            put("/externalApi/logistic-segments/dropship-logistic-movement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/logisticSegment/response/dropship_logistic_movements/updated.json"));
    }

    @Test
    @DisplayName("Попытка обновления несуществующего логистического перемещения")
    void testUpdateDropshipLogisticMovementNotExists() throws Exception {
        UpdateDropshipLogisticMovementRequest request = UpdateDropshipLogisticMovementRequest.builder()
            .fromPartnerId(1L)
            .toPartnerId(1002L)
            .toLogisticsPointId(102L)
            .movementPartnerId(2L)
            .cutoffTime(LocalTime.of(20, 30))
            .shipmentSchedule(Set.of(
                new ScheduleDayResponse(null, 3, LocalTime.of(15, 0), LocalTime.of(22, 0), true),
                new ScheduleDayResponse(null, 5, LocalTime.of(15, 0), LocalTime.of(22, 0), true)
            ))
            .status(ActivityStatus.INACTIVE)
            .build();

        mockMvc.perform(
            put("/externalApi/logistic-segments/dropship-logistic-movement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(startsWith("No logistic movement segments found for updating by request ")));
    }
}
