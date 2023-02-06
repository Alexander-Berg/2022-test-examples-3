package ru.yandex.market.logistics.management.controller.segment;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.CreateMovementSegmentRequest;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.CreateMovementSegmentRequest.CreateMovementSegmentRequestBuilder;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.util.TestUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Создание логистического сегмента с типом 'Перемещение'")
@DatabaseSetup("/data/controller/logisticSegment/before/create_movement_segment_prepare_data.xml")
class LogisticSegmentControllerCreateDropoffMovementsTest extends AbstractContextualAspectValidationTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация тела запроса")
    void requestValidation(String field, CreateMovementSegmentRequestBuilder request) throws Exception {
        request(request)
            .andExpect(status().isBadRequest())
            .andExpect(TestUtil.validationErrorMatcher(
                "createMovementSegmentRequest",
                field,
                "NotNull",
                "must not be null"
            ));
    }

    @Nonnull
    private static Stream<Arguments> requestValidation() {
        return Stream.of(
            Arguments.of("logisticsPointFromId", defaultRequest().logisticsPointFromId(null)),
            Arguments.of("logisticsPointToId", defaultRequest().logisticsPointToId(null)),
            Arguments.of("deliveryServiceId", defaultRequest().deliveryServiceId(null)),
            Arguments.of("tmMovementSchedule[]", defaultRequest().tmMovementSchedule(Collections.singleton(null)))
        );
    }

    @Test
    @DisplayName("Не найдена логистическая точка отправления")
    void logisticsPointFromNotFound() throws Exception {
        request(defaultRequest().logisticsPointFromId(100L))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find logistics point with id=100"));
    }

    @Test
    @DisplayName("Не дропофф")
    void notDropoff() throws Exception {
        request(defaultRequest().logisticsPointFromId(11L))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Only dropoffs supported here"));
    }

    @Test
    @DisplayName("Не найдена логистическая точка назначения")
    void logisticsPointToNotFound() throws Exception {
        request(defaultRequest().logisticsPointToId(200L))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find logistics point with id=200"));
    }

    @Test
    @DisplayName("Не найден партнёр СД")
    void deliveryServiceNotFound() throws Exception {
        request(defaultRequest().deliveryServiceId(100L))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find Partner with id=100"));
    }

    @Test
    @DisplayName("Передан идентификатор партнёра, не являющегося службой доставки")
    void partnerIdNotDelivery() throws Exception {
        request(defaultRequest().deliveryServiceId(2L))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Partner with id = 2 is not delivery"));
    }

    @Test
    @DisplayName("Не найден логистический сегмент для точки назначение")
    void logisticsPointToSegmentNotFound() throws Exception {
        request(defaultRequest().logisticsPointToId(22L))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Warehouse segment for logistics point with id = 22 not found"));
    }

    @Test
    @DisplayName("Сегмент перемещения уже существует")
    @DatabaseSetup(
        value = "/data/controller/logisticSegment/before/movement_segment.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/backward_movement_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void segmentAlreadyExists() throws Exception {
        performMovementSegmentExist();
    }

    @Test
    @DisplayName("Дропофф, поддерживающий невыкупы: сегмент перемещения уже существует, возвратного сегмента нет")
    @DatabaseSetup(
        value = "/data/controller/logisticSegment/before/movement_segment.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/backward_movement_segment_dropoff_return_allowed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dropoffReturnAllowedMovementSegmentExistsBackwardNotExist() throws Exception {
        performMovementSegmentExist();
    }

    @Test
    @DisplayName("Дропофф, поддерживающий невыкупы: сегмент перемещения уже существует, возвратный сегмент есть")
    @DatabaseSetup(
        value = {
            "/data/controller/logisticSegment/before/movement_segment.xml",
            "/data/controller/logisticSegment/before/backward_segment.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/controller/logisticSegment/before/backward_segment_edges.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/backward_movement_segment_existed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dropoffReturnAllowedMovementSegmentExistsBackwardExist() throws Exception {
        performMovementSegmentExist();
    }

    @Test
    @DisplayName("Дропофф, не поддерживающий невыкупы: сегмент перемещения уже существует, возвратного сегмента нет")
    @DatabaseSetup(
        value = {
            "/data/controller/logisticSegment/before/movement_segment.xml",
            "/data/controller/logisticSegment/before/return_not_allowed.xml"
        },
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/segments_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dropoffReturnNotAllowedMovementSegmentExistsBackwardNotExist() throws Exception {
        performMovementSegmentExist();
    }

    @Test
    @DisplayName("Дропофф, не поддерживающий невыкупы: сегмент перемещения уже существует, возвратный сегмент есть")
    @DatabaseSetup(
        value = {
            "/data/controller/logisticSegment/before/movement_segment.xml",
            "/data/controller/logisticSegment/before/backward_segment.xml",
            "/data/controller/logisticSegment/before/return_not_allowed.xml"
        },
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/controller/logisticSegment/before/backward_segment_edges.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/backward_movement_segment_existed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dropoffReturnNotAllowedMovementSegmentExistsBackwardExistDelivery() throws Exception {
        performMovementSegmentExist();
    }

    @Test
    @DisplayName("Дропофф, поддерживаюший невыкупы: успешное создание сегмента, возвратного нет")
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/movement_segment_with_backward_dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dropoffReturnAllowedSuccessBackwardNotExist() throws Exception {
        request(scheduleRequest()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Дропофф, поддерживаюший невыкупы: успешное создание сегмента, возвратный есть")
    @DatabaseSetup(
        value = "/data/controller/logisticSegment/before/backward_segment.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/controller/logisticSegment/before/backward_segment_edges.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value =
            "/data/controller/logisticSegment/after/movement_segment_with_backward_existed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dropoffReturnAllowedSuccessBackwardExist() throws Exception {
        request(scheduleRequest()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Дропофф, не поддерживаюший невыкупы: успешное создание сегмента, возвратного нет")
    @DatabaseSetup(
        value = "/data/controller/logisticSegment/before/return_not_allowed.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/movement_segment_return_not_allowed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dropoffReturnNotAllowedSuccessBackwardNotExist() throws Exception {
        request(scheduleRequest()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Дропофф, не поддерживаюший невыкупы: успешное создание сегмента, возвратный есть")
    @DatabaseSetup(
        value = {
            "/data/controller/logisticSegment/before/backward_segment.xml",
            "/data/controller/logisticSegment/before/return_not_allowed.xml"
        },
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/controller/logisticSegment/before/backward_segment_edges.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/movement_segment_with_backward_existed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dropoffReturnNotAllowedSuccessBackwardExist() throws Exception {
        request(scheduleRequest()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешное создание сегмента с зафиксированным сервисом TRANSPORT_MANAGER_MOVEMENT")
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/movement_segment_frozen_tm_movement_service.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successFrozenTmMovementServices() throws Exception {
        request(scheduleRequest().freezeTmMovementService(true)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешное создание сегмента без указания расписания отгрузок и продолжительности сервисов")
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/movement_segment_without_tm_movement_schedule_and_duration.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successScheduleIsNull() throws Exception {
        request(defaultRequest()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешное создание сегмента с признаком START_AT_RIGHT_BORDER")
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/movement_segment_with_start_at_right_border.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successStartAtRightBorder() throws Exception {
        request(defaultRequest().startAtRightBorder(true))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешное создание сегмента при наличии дубля сегмента")
    @DatabaseSetup(
        value = "/data/controller/logisticSegment/before/add_duplicated_warehouse_segment.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/movement_segments_with_duplicate.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successWithDuplicatedSegment() throws Exception {
        request(scheduleRequest()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешное создание BMV сегментов между BWH")
    @DatabaseSetup(
        value = "/data/controller/logisticSegment/before/backward_warehouse.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/backward_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successBackwardWarehouses() throws Exception {
        request(defaultRequest()).andExpect(status().isOk());
    }

    @Nonnull
    private CreateMovementSegmentRequestBuilder scheduleRequest() {
        return defaultRequest()
            .tmMovementSchedule(Set.of(new ScheduleDayResponse(null, 1, LocalTime.of(15, 0), LocalTime.of(18, 0))))
            .shipmentDuration(60)
            .movementDuration(120);
    }

    @Nonnull
    private static CreateMovementSegmentRequestBuilder defaultRequest() {
        return CreateMovementSegmentRequest.builder()
            .logisticsPointFromId(10L)
            .logisticsPointToId(20L)
            .deliveryServiceId(1L);
    }

    @Nonnull
    private ResultActions request(CreateMovementSegmentRequestBuilder request) throws Exception {
        return mockMvc.perform(
            MockMvcRequestBuilders.request(HttpMethod.POST, "/externalApi/logistic-segments/dropoff-movements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request.build()))
        );
    }

    @Nonnull
    private ResultActions performMovementSegmentExist() throws Exception {
        return request(defaultRequest()).andExpect(status().isOk());
    }
}
