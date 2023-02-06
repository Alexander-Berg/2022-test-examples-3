package ru.yandex.market.logistics.nesu.controller.order;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.filter.ShipmentFilter;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.nesu.base.order.AbstractSearchOrdersTest;
import ru.yandex.market.logistics.nesu.dto.filter.AbstractOrderSearchFilter;
import ru.yandex.market.logistics.nesu.dto.filter.SendersOrderSearchFilter;
import ru.yandex.market.logistics.nesu.model.LmsFactory;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Поиск заказов")
@DatabaseSetup("/controller/order/get/data.xml")
class BackOfficeSearchOrdersTest extends AbstractSearchOrdersTest {

    private long shopId = 1;

    @Autowired
    private LMSClient lmsClient;

    @Test
    @DisplayName("Невалидный магазин")
    void searchOrdersInvalidShop() throws Exception {
        shopId = 2;

        search("controller/order/search/request/all.json")
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/order/search/response/no_access.json"));
    }

    @Test
    @DisplayName("Отсутствуют сендеры")
    void noSenderIds() throws Exception {
        mockSearchOrders(defaultFilter().build(), defaultResult(), PAGE_DEFAULTS);

        search("controller/order/search/request/empty.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/response/success.json"));
    }

    @Test
    @DisplayName("Поиск по отгрузке c невалидным партнером")
    void searchByShipmentPartnerNotFound() throws Exception {
        search("controller/order/search/request/by_shipment_all_fields.json")
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [42]"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поиск по отгрузке")
    @MethodSource("searchByShipmentArguments")
    void searchByShipment(@SuppressWarnings("unused") String displayName, String requestPath) throws Exception {
        mockSearchOrders(
            defaultFilter().shipment(
                ShipmentFilter.builder()
                    .type(ShipmentType.IMPORT)
                    .date(LocalDate.of(2019, 8, 5))
                    .warehousesFrom(Set.of(1L))
                    .warehouseTo(2L)
                    .marketIdTo(100L)
                    .build()
            ).build(),
            defaultResult(),
            PAGE_DEFAULTS
        );

        when(lmsClient.getPartner(42L)).thenReturn(Optional.of(LmsFactory.createPartner(42L, null)));

        search(requestPath)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/response/success.json"));
    }

    @Nonnull
    private static Stream<Arguments> searchByShipmentArguments() {
        return Stream.of(
            Arguments.of(
                "Указан склад отправления",
                "controller/order/search/request/by_shipment_all_fields.json"
            ),
            Arguments.of(
                "Указан список складов отправления",
                "controller/order/search/request/by_shipment_all_fields_warehouses_from.json"
            )
        );
    }

    @Test
    @DisplayName("Поиск по списку складов отправления отгрузки")
    void searchByShipmentWarehousesFrom() throws Exception {
        mockSearchOrders(
            defaultFilter().shipment(
                ShipmentFilter.builder()
                    .warehousesFrom(Set.of(1L))
                    .build()
            ).build(),
            defaultResult(),
            PAGE_DEFAULTS
        );

        search("controller/order/search/request/by_shipment_warehouses_from.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/response/success.json"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поля фильтра")
    @MethodSource("searchArguments")
    void searchOrdersBackOffice(
        @SuppressWarnings("unused") String displayName,
        String requestPath,
        OrderSearchFilter.OrderSearchFilterBuilder filter
    ) throws Exception {
        mockSearchOrders(filter.build(), defaultResult(), PAGE_DEFAULTS);

        search(requestPath)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/response/success.json"));
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.of(
            Arguments.of(
                "Нижняя граница даты отгрузки",
                "controller/order/search/request/by_shipment_from_date.json",
                defaultFilter().shipmentFromDate(LocalDate.of(2019, 7, 8))
            ),
            Arguments.of(
                "Верхняя граница даты отгрузки",
                "controller/order/search/request/by_shipment_to_date.json",
                defaultFilter().shipmentToDate(LocalDate.of(2019, 7, 8))
            ),
            Arguments.of(
                "Нижняя граница даты и времени создания заказа",
                "controller/order/search/request/by_from_date.json",
                defaultFilter().fromDate(Instant.parse("2019-07-03T17:00:00Z"))
            ),
            Arguments.of(
                "Верхняя граница даты и времени создания заказа",
                "controller/order/search/request/by_to_date.json",
                defaultFilter().toDate(Instant.parse("2019-07-03T17:00:00Z"))
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Валидация фильтра отгрузки")
    @MethodSource("validationArguments")
    void searchByShipmentValidation(
        @SuppressWarnings("unused") String displayName,
        String requestPath,
        String field,
        String message,
        String code
    ) throws Exception {
        search(requestPath)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(field, message, orderSearchObjectName(), code)));
    }

    @Nonnull
    private static Stream<Arguments> validationArguments() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр по отгрузке",
                "controller/order/search/request/by_invalid_shipment_empty.json",
                "shipment",
                "warehouseFrom or warehousesFrom must be presented",
                "ValidShipmentFilter"
            ),
            Arguments.of(
                "Не указан склад назначения для самопривоза",
                "controller/order/search/request/by_invalid_shipment_with_type_no_warehouse_to.json",
                "shipment",
                "when shipment type is IMPORT, warehouseTo must present",
                "ValidShipmentFilter"
            ),
            Arguments.of(
                "Null в списке складов отправления",
                "controller/order/search/request/by_invalid_shipment_warehouses_from_contains_null.json",
                "shipment.warehousesFrom",
                "must not contain nulls",
                "NotNullElements"
            )
        );
    }

    @Nonnull
    @Override
    protected String orderSearchObjectName() {
        return "sendersOrderSearchFilter";
    }

    @Override
    @Nonnull
    protected ResultActions search(String requestPath, MultiValueMap<String, String> params) throws Exception {
        return mockMvc.perform(
            put("/back-office/orders/search")
                .param("shopId", String.valueOf(shopId))
                .param("userId", "1")
                .params(params)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestPath))
        );
    }

    @Override
    @Nonnull
    protected ResultActions search(Consumer<AbstractOrderSearchFilter> filterAdjuster) throws Exception {
        SendersOrderSearchFilter filter = new SendersOrderSearchFilter();
        filterAdjuster.accept(filter);
        return mockMvc.perform(
            request(HttpMethod.PUT, "/back-office/orders/search", filter)
                .param("shopId", String.valueOf(shopId))
                .param("userId", "1")
        );
    }
}
