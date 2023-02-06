package ru.yandex.market.logistics.management.controller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.dropshipBySeller.UpdateDeliveryServiceCalendarRequest;
import ru.yandex.market.logistics.management.entity.request.dropshipBySeller.UpdateDropshipBySellerGraphRequest;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.management.util.TestUtil.hasResolvedExceptionContainingMessage;
import static ru.yandex.market.logistics.management.util.TestUtil.noContent;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/data/controller/dropshipBySellerGraph/before/service_codes.xml")
class DropshipBySellerGraphControllerTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;

    @JpaQueriesCount(0)
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME + " {1}")
    @MethodSource("validateRequestBodyArguments")
    void validateRequestBody(
        String fieldName,
        String errorMessage,
        UnaryOperator<UpdateDropshipBySellerGraphRequest.Builder> requestBuilderModifier
    ) throws Exception {
        UpdateDropshipBySellerGraphRequest request = requestBuilderModifier.apply(
                UpdateDropshipBySellerGraphRequest.newBuilder()
                    .partnerId(1L)
                    .updateDeliveryServiceCalendar(defaultRequestBuilder().build())
            )
            .build();
        updateDropshipBySellerGraph(objectMapper.writeValueAsString(List.of(request)))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(String.join(" ", fieldName, errorMessage)));
    }

    @Nonnull
    private UpdateDeliveryServiceCalendarRequest.Builder defaultRequestBuilder() {
        return UpdateDeliveryServiceCalendarRequest.newBuilder()
            .dateFrom(LocalDate.of(2021, 1, 1))
            .dateTo(LocalDate.of(2021, 1, 31))
            .holidayDates(Set.of(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 2)));
    }

    @Nonnull
    private static Stream<Arguments> validateRequestBodyArguments() {
        return Stream.<Triple<String, String, UnaryOperator<UpdateDropshipBySellerGraphRequest.Builder>>>of(
                Triple.of(
                    "partnerId",
                    "must not be null",
                    b -> b.partnerId(null)
                ),
                Triple.of(
                    "dateFrom",
                    "must not be null",
                    calendarModifier(b -> b.dateFrom(null))
                ),
                Triple.of(
                    "dateTo",
                    "must not be null",
                    calendarModifier(b -> b.dateTo(null))
                ),
                Triple.of(
                    "holidayDates.<iterable element>",
                    "must not be null",
                    calendarModifier(b -> b.holidayDates(Collections.singleton(null)))
                ),
                Triple.of(
                    "holidayDates",
                    "must not be null",
                    calendarModifier(b -> b.holidayDates(null))
                )
            )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @Nonnull
    private static UnaryOperator<UpdateDropshipBySellerGraphRequest.Builder> calendarModifier(
        UnaryOperator<UpdateDeliveryServiceCalendarRequest.Builder> modifier
    ) {
        return requestBuilder -> {
            UpdateDropshipBySellerGraphRequest request = requestBuilder.build();
            var calendarBuilder = request.getUpdateDeliveryServiceCalendar().toBuilder();
            modifier.apply(calendarBuilder);
            return request.toBuilder().updateDeliveryServiceCalendar(calendarBuilder.build());
        };
    }

    @Test
    @JpaQueriesCount(1)
    void partnerDoesNotExist() throws Exception {
        updateDropshipBySellerGraph()
            .andExpect(status().isNotFound())
            .andExpect(hasResolvedExceptionContainingMessage("Partners not found by ids [49850]"));
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("/data/controller/dropshipBySellerGraph/before/non_dbs_partner.xml")
    void partnerIsNotDropshipBySeller() throws Exception {
        updateDropshipBySellerGraph()
            .andExpect(status().isBadRequest())
            .andExpect(hasResolvedExceptionContainingMessage(
                "Expected partners of type DROPSHIP_BY_SELLER, but following partners are of different type: [49850]"
            ));
    }

    @Test
    @JpaQueriesCount(4)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner_without_platform.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
    })
    void partnerWithoutPlatform() {
        softly.assertThatThrownBy(this::updateDropshipBySellerGraph)
            .hasMessageContaining("Logistics segment was not created for warehouse id = 10000049850.");
    }

    @Test
    @JpaQueriesCount(2)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/inactive_dbs_warehouse.xml",
    })
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/no_graph.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void partnerHasNoActiveWarehouses() throws Exception {
        updateDropshipBySellerGraph();
    }

    @Test
    @JpaQueriesCount(2)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/multiple_dbs_warehouses.xml",
    })
    void partnerHasMultipleActiveWarehouses() {
        softly.assertThatThrownBy(this::updateDropshipBySellerGraph)
            .hasMessageContaining(
                "DBS partners expected to have exactly one active warehouses, " +
                    "following partners have more than one: [49850]"
            );
    }

    @Test
    @JpaQueriesCount(3)
    @DisplayName("У партнёра несколько WAREHOUSE сегментов по активному складу")
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/all_segments_exist.xml",
        "/data/controller/dropshipBySellerGraph/before/non_actual_calendar_days.xml",
        "/data/controller/dropshipBySellerGraph/before/extra_warehouse_segment.xml",
    })
    void partnerHasMultipleWarehouseSegments() {
        softly.assertThatThrownBy(this::updateDropshipBySellerGraph)
            .hasMessageContaining(
                "DBS partner 49850 expected to have exactly one active logistic segment of type WAREHOUSE"
            );
    }

    @Test
    @JpaQueriesCount(7)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/all_segments_exist.xml",
        "/data/controller/dropshipBySellerGraph/before/non_actual_calendar_days.xml",
        "/data/controller/dropshipBySellerGraph/before/extra_movement_segment.xml",
    })
    void partnerHasMultipleMovementSegments() {
        softly.assertThatThrownBy(this::updateDropshipBySellerGraph)
            .hasMessageContaining("DBS partner expected to have exactly one logistic segment of type MOVEMENT");
    }

    @Test
    @JpaQueriesCount(7)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/all_segments_exist.xml",
        "/data/controller/dropshipBySellerGraph/before/non_actual_calendar_days.xml",
        "/data/controller/dropshipBySellerGraph/before/extra_linehaul_segment.xml",
    })
    void partnerHasMultipleLinehaulSegments() {
        softly.assertThatThrownBy(this::updateDropshipBySellerGraph)
            .hasMessageContaining("DBS partner expected to have exactly one logistic segment of type LINEHAUL");
    }

    @Test
    @JpaQueriesCount(20)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/all_segments_exist.xml",
        "/data/controller/dropshipBySellerGraph/before/non_actual_calendar_days.xml",
        "/data/controller/dropshipBySellerGraph/before/extra_handing_segment.xml",
    })
    void partnerHasMultipleHandingSegments() {
        softly.assertThatThrownBy(this::updateDropshipBySellerGraph)
            .hasMessageContaining("DBS partner expected to have exactly one logistic segment of type HANDING");
    }

    // TODO DELIVERY-44883 Нестабильная работа интеграционных тестов из-за кеширования кодов логистических сервисов
    //  Тест не работает, если запускать его одного из-за расхождения счётчика в JpaQueriesCount,
    //  но работает, если запускать его вместе с соседним тестами
    @Test
    @JpaQueriesCount(56)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
    })
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/all_graph_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void allLogisticSegmentsWillBeCreated() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/no_warehouse_segment.xml",
    })
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/warehouse_segment_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void warehouseSegmentWillBeCreated() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    @Test
    @JpaQueriesCount(18)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/no_movement_segment.xml",
    })
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/movement_segment_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void movementSegmentWillBeCreated() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    @Test
    @JpaQueriesCount(29)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/no_linehaul_segment.xml",
    })
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/linehaul_segment_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void linehaulSegmentWillBeCreated() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    @Test
    @JpaQueriesCount(25)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/no_shop_last_mile_service.xml",
    })
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/shop_last_mile_service_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void shopLastMileServiceWillBeCreated() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    @Test
    @JpaQueriesCount(25)
    @DisplayName("Создание сервисов последней мили для флоу через дропофф")
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/no_shop_last_mile_service.xml",
        "/data/controller/dropshipBySellerGraph/before/through_dropoff.xml",
    })
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/shop_last_mile_service_created_through_dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void shopLastMileServiceWillBeCreatedThroughDropoff() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    @Test
    @JpaQueriesCount(20)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/no_handing_segment.xml",
    })
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/handing_segment_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void handingSegmentWillBeCreated() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    @Test
    @JpaQueriesCount(20)
    @DisplayName("Создание handing-сегмента для флоу через дропофф")
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/no_handing_segment.xml",
        "/data/controller/dropshipBySellerGraph/before/through_dropoff.xml",
    })
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/handing_segment_created_through_dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void handingSegmentWillBeCreatedThroughDropoff() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/all_segments_exist.xml",
        "/data/controller/dropshipBySellerGraph/before/non_actual_calendar_days.xml",
    })
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/only_calendar_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void shopLastMileSegmentWillBeUpdated() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    @Test
    @JpaQueriesCount(21)
    @DisplayName("Обновление сегмента последней мили для флоу через дропофф")
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/all_segments_exist.xml",
        "/data/controller/dropshipBySellerGraph/before/non_actual_calendar_days.xml",
        "/data/controller/dropshipBySellerGraph/before/through_dropoff.xml",
    })
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/only_calendar_updated_through_dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void shopLastMileSegmentWillBeUpdatedThroughDropoff() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    @Test
    @JpaQueriesCount(8)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/all_segments_exist.xml",
        "/data/controller/dropshipBySellerGraph/before/actual_calendar_days.xml",
    })
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/only_calendar_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void shopLastMileSegmentWillNotBeUpdatedBecauseThereIsNoChange() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    @Test
    @JpaQueriesCount(8)
    @DisplayName("Сегмент последней мили не обновляется тк нет изменений, флоу через дропофф")
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/all_segments_exist.xml",
        "/data/controller/dropshipBySellerGraph/before/actual_calendar_days.xml",
        "/data/controller/dropshipBySellerGraph/before/through_dropoff.xml",
    })
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/only_calendar_updated_through_dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void shopLastMileSegmentWillNotBeUpdatedBecauseThereIsNoChangeThroughDropoff() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    @Test
    @JpaQueriesCount(23)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/all_segments_exist.xml",
        "/data/controller/dropshipBySellerGraph/before/non_actual_calendar_days.xml",
    })
    @DatabaseSetup(
        value = "/data/controller/dropshipBySellerGraph/before/partner_inactive.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/services_inactive.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void servicesWillBeDeactivated() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    @Test
    @JpaQueriesCount(23)
    @DisplayName("Сервисы будут деактивированы для флоу через дропофф")
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/all_segments_exist.xml",
        "/data/controller/dropshipBySellerGraph/before/non_actual_calendar_days.xml",
        "/data/controller/dropshipBySellerGraph/before/through_dropoff.xml",
    })
    @DatabaseSetup(
        value = "/data/controller/dropshipBySellerGraph/before/partner_inactive.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/services_inactive_through_dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void servicesWillBeDeactivatedThroughDropoff() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    @Test
    @JpaQueriesCount(23)
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/all_segments_exist.xml",
        "/data/controller/dropshipBySellerGraph/before/non_actual_calendar_days.xml",
    })
    @DatabaseSetup(
        value = "/data/controller/dropshipBySellerGraph/after/services_inactive.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/services_active.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void servicesWillBeActivated() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    @Test
    @JpaQueriesCount(23)
    @DisplayName("Сервисы активируются для заказа через дропофф")
    @DatabaseSetup({
        "/data/controller/dropshipBySellerGraph/before/dbs_partner.xml",
        "/data/controller/dropshipBySellerGraph/before/dbs_warehouse.xml",
        "/data/controller/dropshipBySellerGraph/before/all_segments_exist.xml",
        "/data/controller/dropshipBySellerGraph/before/non_actual_calendar_days.xml",
        "/data/controller/dropshipBySellerGraph/before/through_dropoff.xml",
    })
    @DatabaseSetup(
        value = "/data/controller/dropshipBySellerGraph/after/services_inactive.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/services_active_through_dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void servicesWillBeActivatedThroughDropoff() throws Exception {
        updateDropshipBySellerGraphSuccessfully();
    }

    private void updateDropshipBySellerGraphSuccessfully() throws Exception {
        updateDropshipBySellerGraph()
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Nonnull
    private ResultActions updateDropshipBySellerGraph() throws Exception {
        return updateDropshipBySellerGraph(pathToJson("data/controller/dropshipBySellerGraph/request/update.json"));
    }

    @Nonnull
    private ResultActions updateDropshipBySellerGraph(String requestBody) throws Exception {
        return mockMvc.perform(
            put("/externalApi/dropship-by-seller/graphs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        );
    }
}
