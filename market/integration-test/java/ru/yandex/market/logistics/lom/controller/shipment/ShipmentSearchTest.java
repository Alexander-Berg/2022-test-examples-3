package ru.yandex.market.logistics.lom.controller.shipment;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.lom.model.search.Sort;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.model.search.Direction.DESC;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/shipment/search/search_shipments.xml")
class ShipmentSearchTest extends AbstractContextualTest {

    @Test
    @JpaQueriesCount(7)
    @DisplayName("Поиск всех отгрузок с подсчетом запросов")
    void searchShipmentAllCalculatingQueries() throws Exception {
        searchShipment(
            "controller/shipment/search/request/all.json",
            "controller/shipment/search/response/shipment_1234.json",
            sort("id,asc"),
            status().isOk()
        );
    }

    @ParameterizedTest
    @MethodSource("searchArgument")
    @DisplayName("Поиск отгрузок")
    void searchShipmentAll(String request, String response) throws Exception {
        searchShipment(
            request,
            response,
            sort("id,asc"),
            status().isOk()
        );
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "controller/shipment/search/request/by_shipment_type_withdraw.json",
                "controller/shipment/search/response/shipment_234.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_shipment_type_import.json",
                "controller/shipment/search/response/shipment_1.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_partner_type_delivery.json",
                "controller/shipment/search/response/shipment_124.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_partner_type_sc.json",
                "controller/shipment/search/response/shipment_3.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_date_interval.json",
                "controller/shipment/search/response/shipment_2.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_time_interval.json",
                "controller/shipment/search/response/shipment_134.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_new_application_status.json",
                "controller/shipment/search/response/shipment_13.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_market_ids_to.json",
                "controller/shipment/search/response/shipment_124.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_partner_ids_to.json",
                "controller/shipment/search/response/shipment_124.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_warehouse_to.json",
                "controller/shipment/search/response/shipment_234.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_warehouses_from.json",
                "controller/shipment/search/response/shipment_234.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_application_ids.json",
                "controller/shipment/search/response/shipment_23.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_order_statuses.json",
                "controller/shipment/search/response/shipment_12.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_segment_statuses.json",
                "controller/shipment/search/response/shipment_2.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_empty_market_ids_to.json",
                "controller/shipment/search/response/empty.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_empty_partner_ids_to.json",
                "controller/shipment/search/response/empty.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_empty_shipment_applications_ids.json",
                "controller/shipment/search/response/empty.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_empty_order_statuses.json",
                "controller/shipment/search/response/empty.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_two_application_statuses.json",
                "controller/shipment/search/response/shipment_123.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_empty_application_statuses.json",
                "controller/shipment/search/response/empty.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/with_application.json",
                "controller/shipment/search/response/shipment_12.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/without_application.json",
                "controller/shipment/search/response/shipment_3.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/with_orders_page_size.json",
                "controller/shipment/search/response/shipment_3_with_orders.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/with_orders_page_size_and_statuses_1.json",
                "controller/shipment/search/response/shipment_2_with_orders.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/with_orders_page_size_and_statuses_2.json",
                "controller/shipment/search/response/empty.json"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_empty_warehouses_from.json",
                "controller/shipment/search/response/empty.json"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("searchValidation")
    @DisplayName("Валидация фильтра поиска отгрузок")
    void searchShipmentValidation(String request, String message) throws Exception {
        mockMvc.perform(
            put("/shipments/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(request))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value("Following validation errors occurred:\n" + message));
    }

    @Nonnull
    private static Stream<Arguments> searchValidation() {
        return Stream.of(
            Arguments.of(
                "controller/shipment/search/request/by_null_value_in_market_ids_to.json",
                "Field: 'marketIdsTo[]', message: 'must not be null'"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_null_in_shipment_application_ids.json",
                "Field: 'shipmentApplicationIds[]', message: 'must not be null'"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_null_value_in_order_statuses.json",
                "Field: 'orderStatuses[]', message: 'must not be null'"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_null_values_in_segment_statuses.json",
                "Field: 'segmentStatuses[DELIVERY][]', message: 'must not be null'\n"
                    + "Field: 'segmentStatuses[SORTING_CENTER]', message: 'must not be null'",
                "Null-values в списке статусов сегментов"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_null_application_statuses.json",
                "Field: 'statuses[]', message: 'must not be null'"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_non_positive_orders_page_size.json",
                "Field: 'ordersPageSize', message: 'must be greater than or equal to 1'"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_null_in_sender_ids.json",
                "Field: 'senderIds[]', message: 'must not be null'"
            ),
            Arguments.of(
                "controller/shipment/search/request/by_null_in_warehouses_from.json",
                "Field: 'warehousesFrom[]', message: 'must not be null'"
            )
        );
    }

    @Test
    @DisplayName("Получение всех отгрузок c сортировкой")
    void searchShipmentTimeSorting() throws Exception {
        searchShipment(
            "controller/shipment/search/request/all.json",
            "controller/shipment/search/response/sorted.json",
            sort("shipmentDate,desc"),
            status().isOk()
        );
    }

    @Test
    @DisplayName("Получение всех отгрузок с параметрами сортировки из клиента LOM")
    void searchShipmentTimeLomClientSorting() throws Exception {
        Map<String, Set<String>> paramMap = new Pageable(
            0,
            5,
            new Sort(DESC, "shipmentDate")
        ).toUriParams();

        searchShipment(
            "controller/shipment/search/request/all.json",
            "controller/shipment/search/response/sort_size.json",
            paramMap,
            status().isOk()
        );
    }

    @Test
    @DisplayName("Получение всех отгрузок с заявками c ошибками в сортировке")
    void searchShipmentTimeErrorInSorting() throws Exception {
        searchShipment(
            "controller/shipment/search/request/all.json",
            "controller/shipment/search/response/sort_error.json",
            sort("unknownField,desc"),
            status().isBadRequest()
        );
    }

    @Test
    @DisplayName("Получение отгрузки с опциональной частью в заказе")
    @DatabaseSetup("/controller/shipment/search/cancellation_request.xml")
    void searchShipmentWithOrderOptionalParts() throws Exception {
        searchShipment(
            "controller/shipment/search/request/with_orders_page_size.json",
            "controller/shipment/search/response/shipment_3_with_orders_with_optional_parts.json",
            Map.of("optionalOrderParts", Set.of("CANCELLATION_REQUESTS")),
            status().isOk()
        );
    }

    @Test
    @DisplayName("Получение отгрузки без опциональной части в заказе")
    @DatabaseSetup("/controller/shipment/search/cancellation_request.xml")
    void searchShipmentWithoutOrderOptionalParts() throws Exception {
        searchShipment(
            "controller/shipment/search/request/with_orders_page_size.json",
            "controller/shipment/search/response/shipment_3_with_orders.json",
            null,
            status().isOk()
        );
    }

    @Nonnull
    private Map<String, Set<String>> sort(String value) {
        return Map.of("sort", Set.of(value));
    }

    private void searchShipment(
        String requestPath,
        String responsePath,
        @Nullable Map<String, Set<String>> params,
        ResultMatcher statusMatcher
    ) throws Exception {
        ShipmentTestUtil.searchShipmentByRawRequestResponse(
            mockMvc,
            extractFileContent(requestPath),
            extractFileContent(responsePath),
            true,
            params,
            statusMatcher
        );
    }
}
