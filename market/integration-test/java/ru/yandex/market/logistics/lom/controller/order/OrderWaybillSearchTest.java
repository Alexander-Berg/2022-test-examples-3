package ru.yandex.market.logistics.lom.controller.order;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.filter.ShipmentFilter;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;
import ru.yandex.market.logistics.test.integration.jpa.MultiplyThreadsQueriesInspector;
import ru.yandex.market.logistics.test.integration.jpa.QueriesContentInspector;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Поиск сегментов найденных по фильтру заказов")
@DatabaseSetup("/controller/order/search/orders_with_waybill.xml")
class OrderWaybillSearchTest extends AbstractContextualTest {
    private final Predicate<String> isCountOrdersQuery = sql -> sql.startsWith("select count(order0_.id)");

    @BeforeEach
    void setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск всех заказов")
    void searchOrders(
        @SuppressWarnings("unused") String displayName,
        OrderSearchFilter.OrderSearchFilterBuilder request,
        String response
    ) throws Exception {
        search(request).andExpect(status().isOk()).andExpect(jsonContent(response));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "по senderIds",
                OrderSearchFilter.builder().senderIds(Set.of(1L, 4L)),
                "controller/order/search/segmentsResponse/draft.json"
            ),
            Arguments.of(
                "по marketIdFrom",
                OrderSearchFilter.builder().marketIdFrom(1L),
                "controller/order/search/segmentsResponse/third_order.json"
            ),
            Arguments.of(
                "по orderIds",
                OrderSearchFilter.builder().orderIds(Set.of(1L, 4L, 5L)),
                "controller/order/search/segmentsResponse/draft.json"
            ),
            Arguments.of(
                "по externalIds",
                OrderSearchFilter.builder().externalIds(Set.of("1003")),
                "controller/order/search/segmentsResponse/third_order.json"
            ),
            Arguments.of(
                "по partnerIds",
                OrderSearchFilter.builder().partnerIds(Set.of(2L, 3L)),
                "controller/order/search/segmentsResponse/processing.json"
            ),
            Arguments.of(
                "по нижней границе даты отгрузки и по partnerId",
                OrderSearchFilter.builder()
                    .shipmentFromDate(LocalDate.parse("2019-06-01"))
                    .partnerIds(Set.of(6L)),
                "controller/order/search/segmentsResponse/third_order.json"
            ),
            Arguments.of(
                "по верхней границе даты отгрузки",
                OrderSearchFilter.builder().shipmentToDate(LocalDate.parse("2019-06-20")),
                "controller/order/search/segmentsResponse/third_order.json"
            ),
            Arguments.of(
                "по нижней границе даты создания",
                OrderSearchFilter.builder().fromDate(Instant.parse("2019-05-20T17:00:00Z")),
                "controller/order/search/segmentsResponse/all.json"
            ),
            Arguments.of(
                "по верхней границе даты создания",
                OrderSearchFilter.builder().toDate(Instant.parse("2019-06-20T17:00:00Z")),
                "controller/order/search/segmentsResponse/all.json"
            ),
            Arguments.of(
                "по статусам",
                OrderSearchFilter.builder().statuses(Set.of(OrderStatus.DRAFT, OrderStatus.VALIDATING)),
                "controller/order/search/segmentsResponse/draft.json"
            ),
            Arguments.of(
                "по lastCancellationOrder-статусам",
                OrderSearchFilter.builder()
                    .lastCancellationOrderStatuses(Set.of(
                        CancellationOrderStatus.PROCESSING,
                        CancellationOrderStatus.MANUALLY_CONFIRMED
                    )),
                "controller/order/search/segmentsResponse/third_order.json"
            ),
            Arguments.of(
                "по пустой коллекции",
                OrderSearchFilter.builder().senderIds(Set.of()),
                "controller/order/search/segmentsResponse/empty.json"
            ),
            Arguments.of(
                "по отгрузкам: полный фильтр",
                OrderSearchFilter.builder()
                    .shipment(
                        ShipmentFilter.builder()
                            .type(ShipmentType.IMPORT)
                            .date(LocalDate.parse("2019-06-12"))
                            .warehousesFrom(Set.of(1L, 2L))
                            .warehouseTo(2L)
                            .marketIdTo(2L)
                            .build()
                    ),
                "controller/order/search/segmentsResponse/third_order.json"
            ),
            Arguments.of(
                "по отгрузкам: заполнен список складов отправления",
                OrderSearchFilter.builder()
                    .shipment(
                        ShipmentFilter.builder()
                            .warehousesFrom(Set.of(1L))
                            .build()
                    ),
                "controller/order/search/segmentsResponse/orders_with_waybill_segments.json"
            ),
            Arguments.of(
                "по отгрузкам: заполнен тип отгрузки и не заполнен склад назначения",
                OrderSearchFilter.builder()
                    .shipment(
                        ShipmentFilter.builder()
                            .type(ShipmentType.WITHDRAW)
                            .warehousesFrom(Set.of(1L))
                            .build()
                    ),
                "controller/order/search/segmentsResponse/empty.json"
            ),
            Arguments.of(
                "по segmentStatuses",
                OrderSearchFilter.builder()
                    .segmentStatuses(Map.of(
                        PartnerType.SORTING_CENTER, Set.of(SegmentStatus.IN),
                        PartnerType.DELIVERY, Set.of(SegmentStatus.PENDING)
                    )),
                "controller/order/search/segmentsResponse/third_order.json"
            ),
            Arguments.of(
                "по статусам заказа и статусам сегмента",
                OrderSearchFilter.builder()
                    .segmentStatuses(Map.of(
                        PartnerType.SORTING_CENTER, Set.of(SegmentStatus.IN, SegmentStatus.INFO_RECEIVED),
                        PartnerType.OWN_DELIVERY, Set.of(SegmentStatus.INFO_RECEIVED)
                    ))
                    .statuses(Set.of(OrderStatus.DRAFT, OrderStatus.VALIDATING)),
                "controller/order/search/segmentsResponse/all.json"
            ),
            Arguments.of(
                "по пустым значениям мапы segmentStatuses",
                OrderSearchFilter.builder()
                    .segmentStatuses(Map.of(
                        PartnerType.DELIVERY, Set.of(),
                        PartnerType.SORTING_CENTER, Set.of(SegmentStatus.IN)
                    )),
                "controller/order/search/segmentsResponse/empty.json"
            ),
            Arguments.of(
                "по пустой коллекции segmentStatuses",
                OrderSearchFilter.builder().segmentStatuses(Map.of()),
                "controller/order/search/segmentsResponse/empty.json"
            ),
            Arguments.of(
                "по идентификатору платформы",
                OrderSearchFilter.builder().platformClientId(1L),
                "controller/order/search/segmentsResponse/fifth_order.json"
            )
        );
    }

    @Test
    @JpaQueriesCount(6)
    @DisplayName("Поиск всех заказов с подсчетом количества запросов")
    void searchOrdersAllWithQueryCount() throws Exception {
        search(OrderSearchFilter.builder())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/segmentsResponse/all.json"));

        softly.assertThat(QueriesContentInspector.getQueries()).noneMatch(isCountOrdersQuery);
    }

    @Test
    @JpaQueriesCount(6)
    @DisplayName("Поиск всех заказов без параметров страниц")
    void searchOrdersUnpaged() throws Exception {
        mockMvc.perform(
                searchOrderRequestBuilder(OrderSearchFilter.builder())
                    .param("unpaged", "true")
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/segmentsResponse/all_unpaged.json"));

        softly.assertThat(QueriesContentInspector.getQueries()).noneMatch(isCountOrdersQuery);
    }

    @Test
    @JpaQueriesCount(7)
    @DisplayName("С подсчетом общего числа")
    void searchOrdersWithCount() throws Exception {
        mockMvc.perform(
                searchOrderRequestBuilder(OrderSearchFilter.builder())
                    .param("page", "0")
                    .param("size", "2")
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/segmentsResponse/first_two_paged.json"));

        softly.assertThat(MultiplyThreadsQueriesInspector.getQueries()).anyMatch(isCountOrdersQuery);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchValidation")
    @DisplayName("Валидация фильтра поиска заказов")
    void searchOrdersValidation(
        @SuppressWarnings("unused") String displayName,
        OrderSearchFilter.OrderSearchFilterBuilder request,
        String message
    ) throws Exception {
        search(request)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Following validation errors occurred:\n" + message));
    }

    @Nonnull
    private static Stream<Arguments> searchValidation() {
        return Stream.of(
            Arguments.of(
                "Null-values в списке идентификаторов сендеров",
                OrderSearchFilter.builder().senderIds(Collections.singleton(null)),
                "Field: 'senderIds[]', message: 'must not be null'"
            ),
            Arguments.of(
                "Null-values в списке идентификаторов заказов",
                OrderSearchFilter.builder().orderIds(Collections.singleton(null)),
                "Field: 'orderIds[]', message: 'must not be null'"
            ),
            Arguments.of(
                "Null-values в списке идентификаторов заказов",
                OrderSearchFilter.builder().externalIds(Collections.singleton(null)),
                "Field: 'externalIds[]', message: 'must not be null'"
            ),
            Arguments.of(
                "Null-values в списке идентификаторов партнёров",
                OrderSearchFilter.builder().partnerIds(Collections.singleton(null)),
                "Field: 'partnerIds[]', message: 'must not be null'"
            ),
            Arguments.of(
                "Null-values в списке статусов",
                OrderSearchFilter.builder().statuses(Collections.singleton(null)),
                "Field: 'statuses[]', message: 'must not be null'"
            ),
            Arguments.of(
                "Null-values в списке lastCancellationOrder-статусов",
                OrderSearchFilter.builder().lastCancellationOrderStatuses(Collections.singleton(null)),
                "Field: 'lastCancellationOrderStatuses[]', message: 'must not be null'"
            ),
            Arguments.of(
                "Null-values в списке статусов сегментов",
                OrderSearchFilter.builder()
                    .segmentStatuses(Map.of(
                        PartnerType.DELIVERY,
                        Collections.singleton(null)
                    )),
                "Field: 'segmentStatuses[DELIVERY][]', message: 'must not be null'"
            ),
            Arguments.of(
                "Null-values в списке статусов сегментов",
                OrderSearchFilter.builder()
                    .segmentStatuses(Collections.singletonMap(
                        PartnerType.SORTING_CENTER,
                        null
                    )),
                "Field: 'segmentStatuses[SORTING_CENTER]', message: 'must not be null'"
            ),
            Arguments.of(
                "UNKNOWN в списке статусов сегментов",
                OrderSearchFilter.builder().statuses(Set.of(OrderStatus.UNKNOWN)),
                "Field: 'statuses[]', message: 'UNKNOWN is not allowed value'"
            ),
            Arguments.of(
                "Незаполненный ключ отгрузки",
                OrderSearchFilter.builder().shipment(ShipmentFilter.builder().build()),
                "Field: 'shipment.warehousesFrom', message: 'must not be empty'"
            ),
            Arguments.of(
                "Null-values в списке складов отправления в отгрузке заказов",
                OrderSearchFilter.builder()
                    .shipment(
                        ShipmentFilter.builder()
                            .warehousesFrom(Collections.singleton(null))
                            .build()
                    ),
                "Field: 'shipment.warehousesFrom[]', message: 'must not be null'"
            ),
            Arguments.of(
                "Не заполнен склад получателя для самопривоза",
                OrderSearchFilter.builder()
                    .shipment(
                        ShipmentFilter.builder()
                            .type(ShipmentType.IMPORT)
                            .warehousesFrom(Set.of(1L))
                            .warehouseTo(null)
                            .build()
                    ),
                "Field: 'shipment', message: 'when shipment type is IMPORT, warehouseTo must present'"
            )
        );
    }

    @Test
    @DisplayName("Получение всех заказов с сортировкой по дате создания")
    void searchOrdersCreatedSorting() throws Exception {
        mockMvc.perform(
                searchOrderRequestBuilder(OrderSearchFilter.builder())
                    .param("sort", "created,asc", "id,asc")
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/search/segmentsResponse/all.json"));
    }

    @Test
    @DisplayName("Получение всех заказов с невалидным параметром сортировки")
    void searchOrdersWithInvalidSortingParam() throws Exception {
        mockMvc.perform(
                searchOrderRequestBuilder(OrderSearchFilter.builder())
                    .param("sort", "unknownField,desc")
            )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Invalid sorting properties: unknownField: DESC, "
                + "available properties: [created, id]"));
    }

    @Nonnull
    private ResultActions search(OrderSearchFilter.OrderSearchFilterBuilder request) throws Exception {
        return mockMvc.perform(searchOrderRequestBuilder(request).param("sort", "id,asc"));
    }

    @Nonnull
    private MockHttpServletRequestBuilder searchOrderRequestBuilder(OrderSearchFilter.OrderSearchFilterBuilder filter)
        throws Exception {
        return request(HttpMethod.PUT, "/orders/searchOrderSegments", filter.build());
    }
}
