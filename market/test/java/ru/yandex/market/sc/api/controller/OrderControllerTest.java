package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.cell.model.ApiCellDto;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.cell.repository.CellMapper;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderDto;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderListStatus;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.route.RouteCommandService;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishByCellsRequest;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.route_so.RouteSoQueryService;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.AcceptReturnedOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogOperation;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogResult;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DestinationScType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseProperty;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.ScOrderWithPlaces;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.BooleanSource;
import ru.yandex.market.sc.core.util.Constants;
import ru.yandex.market.sc.internal.model.CourierDto;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.configuration.ConfigurationProperties.MISDELIVERY_RETURNS_MAPPINGS;
import static ru.yandex.market.sc.core.domain.order.OrderFlowService.MisdeliveryProcessingProperties;
import static ru.yandex.market.sc.core.domain.order.OrderFlowService.MisdeliveryReturnDirection;
import static ru.yandex.market.sc.core.domain.order.model.ApiOrderListStatus.ACCEPTED_AT_SORTING_CENTER;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderControllerTest extends BaseApiControllerTest {

    private final SortableQueryService sortableQueryService;
    private final SortableRepository sortableRepository;

    private final RouteSoQueryService routeSoQueryService;
    private final RouteCommandService routeCommandService;

    private final ScOrderRepository scOrderRepository;
    private final PlaceRepository placeRepository;

    private TestControllerCaller caller;

    @MockBean
    private Clock clock;

    private SortingCenter sortingCenter;
    private Cell cell;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        cell = testFactory.storedActiveCell(sortingCenter);
        testFactory.storedUser(sortingCenter, UID);
        TestFactory.setupMockClock(clock);
        testFactory.increaseScOrderId();
        caller = TestControllerCaller.createCaller(mockMvc, UID);
    }

    @Test
    @SneakyThrows
    void acceptReturnedPlace() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces().makeReturn().sortPlace("1").shipPlace("1").get();
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"externalId\":\"" + order.getExternalId() + "\",\"placeExternalId\":\"1\"}")
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                        .json("{\"status\":400,\"error\":\"PLACE_SHIPPED_ON_RETURN_STREAM\"}", false));

    }


    /**
     * прием возвратного заказа через приемку возвратов
     * (Поступила информация о возврате)
     */
    @Test
    @SneakyThrows
    void acceptReturnedOrderViaReturnEndpoint() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().get();
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), null))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"SORT_TO_WAREHOUSE\"}"));
    }


    /**
     * прием возвратного заказа через приемку возвратов
     * (Заказ просто отгружен)
     */
    @Test
    @SneakyThrows
    void acceptReturnedOrderViaReturnEndpoint2() {
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().get();
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), null))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"KEEP\"}"));
    }

    /**
     * Нельзя принимать прямой поток через приемку возвратов
     */
    @Test
    @SneakyThrows
    void cantAcceptStraightFlowViaReturnEndpoint() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), null))
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                        .json("{\"status\":400,\"error\":\"ORDER_FROM_STRAIGHT_STREAM\"}", false));
    }

    /**
     * прием возвратного заказа через приемку возвратов заказа, который уже был принят
     * (Поступила информация о возврате)
     */
    @Test
    @SneakyThrows
    void acceptReturnedOrderViaReturnEndpointTwice() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().get();
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), null))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"SORT_TO_WAREHOUSE\"}"));
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), null))
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                        .json("{\"status\":400,\"error\":\"ORDER_FROM_STRAIGHT_STREAM\"}", false));
    }

    /**
     * прием возвратного заказа через приемку возвратов дважды
     * (Поступила информация о возврате)
     */
    @Test
    @SneakyThrows
    void acceptReturnedOrderTwiceViaReturnEndpoint() {
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().get();
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), null))
                )
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), null))
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                        .json("{\"status\":400,\"error\":\"ORDER_FROM_STRAIGHT_STREAM\"}", false));
    }


    /**
     * принимаем и сортируем возвратный заказ через приемку возвратов,
     * а потом снова сканируем через приемку возвратов и получаем ответ, что заказ уже в своей ячейке
     * (Поступила информация о возврате)
     */
    @Test
    @SneakyThrows
    void acceptReturnedOrderTwiceViaReturnEndpointCheckFromCourierField4() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        var place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().getPlace();
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(place.getExternalId(), null))
                )
                .andExpect(status().is2xxSuccessful());
        var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();
        Cell cell = testFactory.determineRouteCell(route, place);

        caller.sortableBetaSort(new SortableSortRequestDto(place, cell))
                .andExpect(status().isOk());

        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(place.getExternalId(), null))
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content().
                        json("{\"status\":400,\"error\":\"ORDER_FROM_STRAIGHT_STREAM\"}", false));
    }

    /**
     * прием многоместного возвратного заказа через приемку возвратов
     * (Поступила информация о возврате)
     */
    @Test
    @SneakyThrows
    void acceptReturnedOrderViaReturnEndpointMultiPlace() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1", "2").sortPlaces("1", "2").ship().makeReturn().get();
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), "1"))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"SORT_TO_WAREHOUSE\"}"));
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), "2"))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"SORT_TO_WAREHOUSE\"}"));
    }

    /**
     * прием и сортировка одного места из многоместного возвратного заказа через приемку возвратов
     * (Поступила информация о возврате)
     */
    @Test
    @SneakyThrows
    void acceptAndSortOnePlaceReturnedOrderViaReturnEndpointMultiPlace() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1", "2").sortPlaces("1", "2").ship().makeReturn().get();
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), "1"))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"SORT_TO_WAREHOUSE\"}"));
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        long cellId = Objects.requireNonNull(testFactory.determineRouteCell(route, order)).getId();

        var request = new SortableSortRequestDto(order.getExternalId(), "1", String.valueOf(cellId));
        caller.sortableBetaSort(request)
                .andExpect(status().isOk());

        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), "2"))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"SORT_TO_WAREHOUSE\"}"));
    }

    /**
     * мы отгрузили многоместный из 2х посылок курьеру
     * делаем acceptReturn первой посылки (проходит успешно)
     * делаем acceptReturn второй посылки (проходит успешно)
     * ещё делаем acceptReturn первой посылки (падает)
     * ещё делаем acceptReturn второй посылки (падает)
     * проверяем что статус заказ теперь на хранении
     */
    @Test
    @SneakyThrows
    void acceptMultiPlaceOrderViaReturnEndpoint() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1", "2").sortPlaces("1", "2").ship().get();
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), "1"))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"KEEP\"}"));
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), "2"))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"KEEP\"}"));
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), "1"))
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content().
                        json("{\"status\":400,\"error\":\"ORDER_FROM_STRAIGHT_STREAM\"}", false));
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), "2"))
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content().
                        json("{\"status\":400,\"error\":\"ORDER_FROM_STRAIGHT_STREAM\"}", false));
        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
    }

    /**
     * прием и сортировка одного места из многоместного возвратного заказа через приемку возвратов
     * (заказ был просто отгружен и его вернул курьер)
     */
    @Test
    @SneakyThrows
    void acceptAndSortOnePlaceReturnedOrderViaReturnEndpointMultiPlace2() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1", "2").sortPlaces("1", "2").ship().get();
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), "1"))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"KEEP\"}"));
        var bufferCell = testFactory.storedCell(sortingCenter, CellType.BUFFER, null);
        var request = new SortableSortRequestDto(order.getExternalId(), "1", String.valueOf(bufferCell.getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().isOk());

        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), "2"))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"KEEP\"}"));
    }

    @Test
    @SneakyThrows
    void orderIdsEmptyRoute() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        testFactory.cancelOrder(order.getId());
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders/list?routeId="
                                        + testFactory.getRouteIdForSortableFlow(route))
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"orders\":[]}", true));
    }

    @Test
    @SneakyThrows
    void orderIdsOutgoingCourierRoute() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();

        var place = testFactory.orderPlace(order);
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders/list?routeId="
                                        + testFactory.getRouteIdForSortableFlow(route))
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"orders\":[" +
                        orderIdsJson(order, List.of(place),
                                List.of(ApiOrderListStatus.NOT_ACCEPTED_AT_SORTING_CENTER)) + "]}", true));
    }

    @Test
    @SneakyThrows
    void orderIdsOutgoingWarehouseRoute() {
        var place = testFactory.createOrder(sortingCenter)
                .accept()
                .keep()
                .makeReturn()
                .getPlace();
        var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders/list?routeId="
                                        + testFactory.getRouteIdForSortableFlow(route))
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"orders\":[" +
                        orderIdsJson(place.getOrder(), List.of(place),
                                List.of(ApiOrderListStatus.SHOULD_BE_RESORTED),
                                List.of(CellMapper.mapToApi(Objects.requireNonNull(place.getCell())))
                        )
                        + "]}", true));
    }

    @Test
    @SneakyThrows
    void multiPlaceOrderIdsOutgoingCourierRoute() {
        var order1 =
                testFactory.createForToday(order(sortingCenter).createTwoPlaces(true).externalId("1").build()).get();
        var order2 =
                testFactory.createForToday(order(sortingCenter).createTwoPlaces(true).externalId("2").build()).get();
        var route = testFactory.findOutgoingCourierRoute(order1).orElseThrow();

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders/list?routeId="
                                        + testFactory.getRouteIdForSortableFlow(route))
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"orders\":[" +
                        orderIdsJson(order1, testFactory.orderPlaces(order1),
                                List.of(ApiOrderListStatus.NOT_ACCEPTED_AT_SORTING_CENTER,
                                        ApiOrderListStatus.NOT_ACCEPTED_AT_SORTING_CENTER)) + "," +
                        orderIdsJson(order2, testFactory.orderPlaces(order2),
                                List.of(ApiOrderListStatus.NOT_ACCEPTED_AT_SORTING_CENTER,
                                        ApiOrderListStatus.NOT_ACCEPTED_AT_SORTING_CENTER)) +
                        "]}", true));
    }

    private String orderIdsJson(OrderLike order, List<Place> places,
                                List<ApiOrderListStatus> placeStatuses) {
        return orderIdsJson(order, places, placeStatuses,
                Stream.generate(() -> (ApiCellDto) null).limit(places.size()).toList());
    }

    @SuppressWarnings("SameParameterValue")
    private String orderIdsJson(OrderLike order, List<Place> places,
                                List<ApiOrderListStatus> placeStatuses,
                                List<ApiCellDto> placeCells) {
        return "{" +
                "\"id\":" + order.getId() +
                ",\"externalId\":\"" + order.getExternalId() + "\"" +
                (places.isEmpty()
                        ? ""
                        : IntStream.range(0, places.size())
                        .mapToObj(i -> "{" +
                                "\"orderId\":" + places.get(i).getOrderId() + "" +
                                ",\"orderExternalId\":\"" + places.get(i).getExternalId() + "\"" +
                                ",\"externalId\":\"" + places.get(i).getMainPartnerCode() + "\"" +
                                ",\"status\":\"" + placeStatuses.get(i) + "\"" +
                                (placeCells.get(i) == null ? "" : ",\"cell\":{" +
                                        "   \"id\":" + placeCells.get(i).getId() + "" +
                                        "   ,\"status\":\"" + placeCells.get(i).getStatus() + "\"" +
                                        "   ,\"type\":\"" + placeCells.get(i).getType() + "\"" +
                                        "   ,\"subType\":\"" + placeCells.get(i).getSubType() + "\"" +
                                        "   ,\"cargoType\":\"" + placeCells.get(i).getCargoType() + "\"" +
                                        "}") +
                                "}")
                        .collect(Collectors.joining(",", ",\"places\":[", "]"))) +
                "}";
    }

    @Test
    @SneakyThrows
    void getOrderContainsRouteId() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" +
                                        Objects.requireNonNull(order).getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json(String.format("""
                        {"routeTo":{"id":%d}}
                        """, testFactory.getRouteIdForSortableFlow(route)), false));
    }

    @DisplayName("Нельзя принимать посылки если есть открытая поставка на сегодня")
    @Test
    @SneakyThrows
    void cantAcceptPlaceOnDropoffWhenNonAcceptedInboundExists() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, true);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.ACCEPT_INBOUNDS, true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DROPOFF_ACCEPT_PLACE_ONLY_IF_INBOUND_ACCEPTED, true);
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build()).get();
        var warehouseFrom = order.getWarehouseFrom();
        Inbound inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .warehouseFromExternalId(warehouseFrom.getYandexId())
                .inboundType(InboundType.DS_SC)
                .sortingCenter(sortingCenter)
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .build());
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), "p1"))
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(String.format("""
                        {
                            "message": "Нельзя принять посылку если существует открытая поставка %s"
                        }
                        """, inbound.getExternalId()), false));
    }

    @DisplayName("Можно принимать посылки если нет открытой поставки на сегодня (к примеру, поставка принята)")
    @Test
    @SneakyThrows
    void acceptPlaceOnDropoffWhenNonAcceptedInboundExists() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, true);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.ACCEPT_INBOUNDS, true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DROPOFF_ACCEPT_PLACE_ONLY_IF_INBOUND_ACCEPTED, true);
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build()).get();
        var warehouseFrom = order.getWarehouseFrom();
        Inbound inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .warehouseFromExternalId(warehouseFrom.getYandexId())
                .inboundType(InboundType.DS_SC)
                .inboundExternalId("my_external_id")
                .sortingCenter(sortingCenter)
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .build());
        testFactory.acceptInbound(inbound.getId());
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), "p1"))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format("""
                        {"routeTo":{"id":%d}}
                        """, testFactory.getRouteIdForSortableFlow(route)), false));
    }

    @DisplayName("Нельзя отсортировать посылки если есть открытая поставка на сегодня")
    @Test
    @SneakyThrows
    void cantSortPlaceOnDropoffWhenNonAcceptedInboundExists() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, true);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.ACCEPT_INBOUNDS, true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DROPOFF_ACCEPT_PLACE_ONLY_IF_INBOUND_ACCEPTED, true);
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build()).get();
        var warehouseFrom = order.getWarehouseFrom();
        Inbound inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .warehouseFromExternalId(warehouseFrom.getYandexId())
                .inboundType(InboundType.DS_SC)
                .sortingCenter(sortingCenter)
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .build());
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();

        caller.acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), "p1"))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(String.format("""
                        {
                            "message": "Нельзя принять посылку если существует открытая поставка %s"
                        }
                        """, inbound.getExternalId()), false));
    }

    @Test
    @SneakyThrows
    void acceptOrderContainsRouteId() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), null))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(String.format("""
                        {"routeTo":{"id":%d}}
                        """, testFactory.getRouteIdForSortableFlow(route)), false));
    }

    @Test
    @SneakyThrows
    void shippedToCourierOrder() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().get();
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" +
                                        Objects.requireNonNull(order).getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json(shipped(order.getId()), false));
    }

    @Test
    @SneakyThrows
    void shippedToCourierXdocPallet() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().get();
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" +
                                        Objects.requireNonNull(order).getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json(shipped(order.getId()), false));
    }

    @Test
    @SneakyThrows
    void getOrderFromAnotherSortingCenter() {
        var anotherSortingCenter = testFactory.storedSortingCenter(777);
        var order = testFactory.createOrderForToday(anotherSortingCenter).get();
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" +
                                        Objects.requireNonNull(order).getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                        .json("{\"status\":403,\"error\":\"ORDER_FROM_ANOTHER_SC\"}", false));
    }

    @Test
    @SneakyThrows
    void getOrderFromAnotherSortingCenterClientReturn() {
        testFactory.setupWarehouseAndDeliveryServiceForClientReturns();
        var anotherSortingCenter = testFactory.storedSortingCenter(777);
        var order = testFactory.createClientReturnForToday(
                        anotherSortingCenter.getId(),
                        anotherSortingCenter.getToken(),
                        anotherSortingCenter.getYandexId(),
                        testFactory.defaultCourier(),
                        ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getAnyPrefix() + 123
                )
                .get();
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" +
                                        Objects.requireNonNull(order).getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void getDuplicatedOrderReturnsActualStatus() {
        String externalId = "order1";
        var sortingCenter1 = testFactory.storedSortingCenter(777);
        var sortingCenter2 = testFactory.storedSortingCenter(778);
        testFactory.createForToday(order(sortingCenter1, externalId).build()).cancel().get();
        testFactory.createForToday(order(sortingCenter2, externalId).build()).get();
        testFactory.setSortingCenterProperty(sortingCenter1, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED,
                "false");
        testFactory.setSortingCenterProperty(sortingCenter2, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED,
                "false");

        long uid1 = 7771L;
        testFactory.storedUser(sortingCenter1, uid1);
        long uid2 = 7781L;
        testFactory.storedUser(sortingCenter2, uid2);

        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + uid1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"externalId\":\"" + externalId + "\"}")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"SORT_TO_WAREHOUSE\"}"));
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + uid2)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"externalId\":\"" + externalId + "\"}")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"SORT_TO_COURIER\"}"));
    }

    @Test
    @SneakyThrows
    void sortOrderFromAnotherSortingCenter() {
        var anotherSortingCenter = testFactory.storedSortingCenter(777);
        var order = testFactory.createForToday(order(anotherSortingCenter).build()).get();
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"externalId\":\"" + order.getExternalId() + "\"}")
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"status\":403,\"error\":\"" +
                        ScErrorCode.ORDER_FROM_ANOTHER_SC + "\"}", false));
    }

    @Test
    @SneakyThrows
    void places() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1", "2")
                .sortPlaces("1")
                .get();
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" +
                                        Objects.requireNonNull(order).getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"places\":[" +
                        "{\"externalId\":\"1\"," +
                        "\"cell\":{\"id\":" + cell.getId() + ",\"number\":\"" + cell.getScNumber() + "\"," +
                        "\"status\": \"ACTIVE\",\"type\": \"COURIER\"}}," +
                        "{\"externalId\":\"2\"}]}", false));
    }

    @Test
    @SneakyThrows
    void sortToWarehouseIfNotAllPlacesSorted() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        var order = testFactory.create(order(sortingCenter).places("1", "2").build())
                .cancel().acceptPlace("1").sortPlace("1").acceptPlace("2").get();
        caller.getOrder(order.getExternalId(), "2", null)
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"SORT_TO_WAREHOUSE\"}", false));
    }

    @Test
    @SneakyThrows
    void zoneOnRouteTo() {
        var zone = testFactory.storedZone(sortingCenter, "zA");
        var courierCell = testFactory.storedCell(sortingCenter, CellType.COURIER, zone);
        var order = testFactory.createOrderForToday(sortingCenter).get();

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" +
                                        Objects.requireNonNull(order).getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"availableCells\":[{" +
                        "\"id\":" + courierCell.getId() + "," +
                        "\"zone\":{\"id\":" + zone.getId() + ",\"name\":\"zA\"}" +
                        "}]}", false));
    }

    @Test
    @SneakyThrows
    void sortToCourierIfNotAllPlacesSorted() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlace("1").sortPlace("1").acceptPlace("2").get();
        caller.getOrder(order.getExternalId(), "2", null)
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"SORT_TO_COURIER\"}", false));
    }

    @Test
    @SneakyThrows
    void okIfAllCourierPlacesSorted() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlace("1").sortPlace("1").acceptPlace("2").sortPlace("2").get();
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" +
                                        Objects.requireNonNull(order).getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"OK\"}", false));
    }

    @Test
    @SneakyThrows
    void okIfAllWarehousePlacesSorted() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        var order = testFactory.create(order(sortingCenter).places("1", "2").build())
                .cancel()
                .acceptPlace("1")
                .sortPlace("1")
                .acceptPlace("2")
                .sortPlace("2")
                .get();
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" +
                                        Objects.requireNonNull(order).getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"OK\"}", false));
    }

    @Test
    @SneakyThrows
    void sortMultiPlaceOrder() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .accept()
                .get();
        var route = testFactory.findOutgoingCourierRoute(order).orElseThrow();
        long cellId = Objects.requireNonNull(testFactory.determineRouteCell(route, order)).getId();

        var request = new SortableSortRequestDto(order.getExternalId(), "1", String.valueOf(cellId));
        caller.sortableBetaSort(request)
                .andExpect(status().isOk());

        assertThat(testFactory.getOrder(order.getId()).getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);

        request = new SortableSortRequestDto(order.getExternalId(), "2", String.valueOf(cellId));
        caller.sortableBetaSort(request)
                .andExpect(status().isOk());

        assertThat(testFactory.getOrder(order.getId()).getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @Test
    @SneakyThrows
    void sortMultiPlaceOrderToLotWithPalletizationAvailable() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var order =
                testFactory.create(order(sortingCenter).externalId("1")
                                .dsType(DeliveryServiceType.TRANSIT)
                                .places("p1", "p2")
                                .deliveryDate(LocalDate.now(clock))
                                .shipmentDate(LocalDate.now(clock))
                                .build())
                        .acceptPlaces(List.of("p1", "p2"))
                        .get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var courierCell = testFactory.determineRouteCell(route, order);
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);

        SortableSortRequestDto request1 = new SortableSortRequestDto(order.getExternalId(), "p1", lot.getBarcode());
        SortableSortRequestDto request2 = new SortableSortRequestDto(order.getExternalId(), "p2", lot.getBarcode());

        caller.sortableBetaSort(request1)
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.destination.id", is(lot.getBarcode())))
                .andExpect(jsonPath("$.destination.type", is(DestinationScType.LOT.toString())))
                .andExpect(jsonPath("$.parentRequired", is(false)));

        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);

        caller.sortableBetaSort(request2)
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.destination.id", is(lot.getBarcode())))
                .andExpect(jsonPath("$.destination.type", is(DestinationScType.LOT.toString())))
                .andExpect(jsonPath("$.parentRequired", is(false)));

        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @SneakyThrows
    @Test
    void sortMultiPlaceOrdersIntoDifferentPalletsWithPalletizationAvailable() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var order = testFactory.create(order(sortingCenter).externalId("1").places("p1", "p2")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .acceptPlaces(List.of("p1", "p2"))
                .get();

        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var courierCell = testFactory.determineRouteCell(route, order);
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        SortableLot lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        SortableSortRequestDto request1 = new SortableSortRequestDto(order.getExternalId(), "p1", lot.getBarcode());
        SortableSortRequestDto request2 = new SortableSortRequestDto(order.getExternalId(), "p2", lot2.getBarcode());

        caller.sortableBetaSort(request1)
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.destination.id", is(lot.getBarcode())))
                .andExpect(jsonPath("$.destination.type", is(DestinationScType.LOT.toString())))
                .andExpect(jsonPath("$.parentRequired", is(false)));

        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);

        caller.sortableBetaSort(request2)
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.destination.id", is(lot2.getBarcode())))
                .andExpect(jsonPath("$.destination.type", is(DestinationScType.LOT.toString())))
                .andExpect(jsonPath("$.parentRequired", is(false)));

        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @SneakyThrows
    @BooleanSource
    @ParameterizedTest
    void sortOrderMiddleMileFlag(boolean middleMile) {
        var ds = testFactory.storedDeliveryService("11");
        var order = testFactory.createForToday(
                order(sortingCenter)
                        .deliveryService(ds)
                        .dsType(middleMile ? DeliveryServiceType.TRANSIT : DeliveryServiceType.LAST_MILE_COURIER)
                        .build()
        ).get();

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" +
                                        Objects.requireNonNull(order).getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.middleMile", is(middleMile)));
    }

    @Test
    @SneakyThrows
    void sortOrder() {
        var place = testFactory.createOrderForToday(sortingCenter).getPlace();

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=not_existing")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(notExisting(), true));

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" +
                                        place.getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json(notSorted(place.getOrderId()), false));

        caller.acceptOrder(new AcceptOrderRequestDto(place.getExternalId(), place.getMainPartnerCode()))
                .andExpect(status().isOk());

        place = testFactory.getPlace(place.getId());
        assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);

        Route route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        Cell cell = testFactory.determineRouteCell(route, place);

        caller.sortableBetaSort(new SortableSortRequestDto(place, cell))
                .andExpect(status().isOk());

        place = testFactory.getPlace(place.getId());
        assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.SORTED_DIRECT);
        assertThat(place.getCell()).isEqualTo(cell);
    }

    @Test
    @SneakyThrows
    void sortOrderWhenShipBufferExists() {
        var zone = testFactory.storedZone(sortingCenter);
        var courier = testFactory.storedCourier(1);
        var bufferCell = testFactory.storedShipBufferCell(sortingCenter, courier.getId(), zone, 1, 1, 1, 1);
        var order = testFactory.createOrderForToday(sortingCenter).get();

        var route = testFactory.findOutgoingRoute(order).orElseThrow().allowReading();

        //две записи, и для bufferCell тоже
        assertThat(route.getRouteCells().size()).isEqualTo(2);

        //проверяем что bufferCell не пришло в availableCells
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" +
                                        Objects.requireNonNull(order).getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json(notSorted(order.getId()), false));
    }

    @Test
    @SneakyThrows
    void prepareToShipOrder() {
        var place = testFactory.createOrderForToday(sortingCenter).accept().sort().getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();

        prepareToShip(place, route);

        assertThat(testFactory.updated(place).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO);
    }

    @Test
    @SneakyThrows
    void prepareToShipOrderIdempotency() {
        var place = testFactory.createOrderForToday(sortingCenter).accept().sort().getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();

        prepareToShip(place, route);
        prepareToShip(place, route);

        assertThat(testFactory.updated(place).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO);
    }

    private void prepareToShip(Place place, Route route) throws Exception {
        mockMvc.perform(
                        put("/api/orders/" + place.getOrderId() + "/preship")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{" +
                                        "\"cellId\":" + Objects.requireNonNull(place.getCell()).getId() + "," +
                                        "\"routeId\":" + testFactory.getRouteIdForSortableFlow(route) + "," +
                                        "\"placeExternalId\":\"" + place.getMainPartnerCode() + "\"}")
                )
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void prepareToShipMultiplaceOrder() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("p1", "p2").build()
        ).acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        Place place1 = testFactory.orderPlace(order, "p1");
        Place place2 = testFactory.orderPlace(order, "p2");
        var route = testFactory.findOutgoingCourierRoute(place1).orElseThrow();

        prepareToShip(place1, route);

        assertThat(testFactory.updated(place1).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);

        prepareToShip(place2, route);

        assertThat(testFactory.updated(place1).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO);

    }

    @Test
    @SneakyThrows
    void prepareToShipMultiplaceOrderIdempotency() {
        var places = testFactory.createForToday(
                order(sortingCenter).places("p1", "p2").build()
        ).acceptPlaces("p1", "p2").sortPlaces("p1", "p2").getPlaces();
        Place place1 = places.get("p1");
        Place place2 = places.get("p2");
        var route = testFactory.findOutgoingCourierRoute(place1).orElseThrow();

        prepareToShip(place1, route);
        prepareToShip(place1, route);

        assertThat(testFactory.updated(place1).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);

        prepareToShip(place2, route);
        prepareToShip(place2, route);

        assertThat(testFactory.updated(place1).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO);

    }

    @Test
    @SneakyThrows
    void prepareToShipMultiplaceOrderWithoutPlace() {
        var places = testFactory.createForToday(
                order(sortingCenter).places("p1", "p2").build()
        ).acceptPlaces("p1", "p2").sortPlaces("p1", "p2").getPlaces();
        Place place1 = places.get("p1");
        var route = testFactory.findOutgoingCourierRoute(place1).orElseThrow();

        prepareToShip(place1, route);

        assertThat(testFactory.updated(place1).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    private String notExisting() {
        return "{" +
                "  \"externalId\":\"not_existing\"," +
                "  \"status\":\"ERROR\"," +
                "  \"canPartiallyShipped\":false," +
                "  \"places\":[" +
                "    {" +
                "      \"externalId\":\"not_existing\"," +
                "      \"status\":\"ERROR\"," +
                "      \"middleMile\":false," +
                "      \"lotSortAvailable\":false" +
                "  }]," +
                "  \"middleMile\":false," +
                "  \"lotSortAvailable\":false" +
                "}";
    }

    private String notSorted(long id) {
        return "{\"id\":" + id + ",\"externalId\":\"12775551-YD4897759\",\"status\":\"SORT_TO_COURIER\"," +
                "\"routeTo\":{\"courier\":{\"name\":\"Иван Пивовар Таранов\"}}," +
                "\"availableCells\":[{\"id\":" + cell.getId() + ",\"number\":\"123\",\"status\":\"ACTIVE\", " +
                "\"subType\":\"DEFAULT\"}]}";
    }

    private String cancelled(long id) {
        return "{\"id\":" + id + ",\"externalId\":\"12775551-YD4897759\",\"status\":\"SORT_TO_WAREHOUSE\"," +
                "\"routeTo\":{\"warehouse\":{}}," +
                "\"availableCells\":[{}]}";
    }

    private String notSortedMultiPlace(long id) {
        return "{\"id\":" + id + ",\"externalId\":\"12775551-YD4897759\",\"status\":\"SORT_TO_COURIER\"," +
                "\"availableCells\":[{\"id\":" + cell.getId() + ",\"number\":\"123\"," +
                "\"status\":\"ACTIVE\", \"subType\":\"DEFAULT\"}]," +
                "\"routeTo\":{}," +
                "\"canPartiallyShipped\":false," +
                "\"places\":[{\"externalId\":\"1\"},{\"externalId\":\"2\"}]}";
    }

    private String shipped(long id) {
        return "{\"id\":" + id + ",\"externalId\":\"12775551-YD4897759\",\"status\":\"KEEP\"}}";
    }


    @Test
    @SneakyThrows
    void acceptNotExisting() {
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody("not_existing", null))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(notExisting()));
    }

    private String acceptBody(String externalId, @Nullable String placeExternalId) {
        return "{\"externalId\":\"" + externalId + "\""
                + (placeExternalId == null ? "" : ",\"placeExternalId\":\"" + placeExternalId + "\"") + "}";
    }

    @Test
    @SneakyThrows
    void acceptMultiPlaceWithoutPlaceId() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build()).get();
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), null))
                )
                .andDo(handler -> {
                    System.out.println("Content: " + handler.getResponse().getContentAsString());
                })
                .andExpect(status().isOk())
                .andExpect(content().json(notSortedMultiPlace(order.getId())));
    }

    @Test
    @SneakyThrows
    void acceptMultiPlaceWithPlaceId() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build()).get();
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), "1"))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(notSortedMultiPlace(order.getId())));
    }

    @Test
    @SneakyThrows
    void acceptCreated() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), null))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(notSorted(order.getId())));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @SneakyThrows
    void acceptCreatedWithTicketId(boolean orderWithPlaceId) {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        var caller = new TestControllerCaller(mockMvc);

        var placeId = orderWithPlaceId ? null : order.getExternalId();
        var ticketId = "ticket-id";
        caller.acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), placeId, ticketId))
                .andExpect(status().isOk());
        for (var place : testFactory.orderPlaces(order)) {
            var rfp = testFactory.findRouteFinishPlaceByPlaceId(place.getId());
            assertThat(rfp.getTicketId()).isEqualTo(ticketId);
        }
    }

    @Test
    @SneakyThrows
    void acceptCancelled() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        var order = testFactory.createOrderForToday(sortingCenter).cancel().get();
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), null))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(cancelled(order.getId())));
    }


    @Test
    @SneakyThrows
    void resortingScanMultiplaceOrderFromCell() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1").sortPlaces("1").get();
        mockMvc.perform(scanOrderResorting(order.getExternalId()))
                .andExpect(status().isOk());
    }

    private RequestBuilder scanOrderResorting(String externalId) {
        return MockMvcRequestBuilders.get("/api/orders?externalId=" + externalId +
                        "&cellId=" + cell.getId())
                .header("Authorization", "OAuth uid-" + UID);
    }

    @Test
    @SneakyThrows
    void resortingScanMultiplaceOrderNotFromCell() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .cancel().acceptPlaces("1").sortPlaces("1").get();
        mockMvc.perform(scanOrderResorting(order.getExternalId()))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"error\":\"ORDER_NOT_FROM_CURRENT_CELL\"}"));
    }

    @Test
    @SneakyThrows
    void resortingScanOrderNotFromCell() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        mockMvc.perform(scanOrderResorting(order.getExternalId()))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"error\":\"ORDER_NOT_FROM_CURRENT_CELL\"}"));
    }

    @Test
    @SneakyThrows
    void resortingScanOrderFromCell() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().get();
        mockMvc.perform(scanOrderResorting(order.getExternalId()))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void cantAcceptReturn() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().get();
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), null))
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"error\":\"ORDER_FROM_RETURN_STREAM\"}"));
    }

    @Test
    @SneakyThrows
    void cantAcceptReturn2() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().makeReturn().get();
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), null))
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"error\":\"ORDER_FROM_RETURN_STREAM\"}"));
    }

    @Test
    @SneakyThrows
    void acceptAccepted() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().get();
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), null))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(notSorted(order.getId())));
    }

    @Test
    @SneakyThrows
    void getDroppedOrder() {
        var order = testFactory.create(order(sortingCenter)
                .places("1", "2")
                .shipmentDate(LocalDate.now(clock)).build()).get();
        cell = testFactory.storedCell(sortingCenter, CellType.BUFFER, CellSubType.DROPPED_ORDERS, null);
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" + order.getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"availableCells\":[{\"id\":" + cell.getId() + ",\"number\":\"1\"" +
                        ",\"status\":\"ACTIVE\",\"type\":\"BUFFER\", \"subType\":\"DROPPED_ORDERS\"}]}"));
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(order.getExternalId(), null))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"availableCells\":[{\"id\":"
                        + cell.getId() + ",\"number\":\"1\",\"status\"" +
                        ":\"ACTIVE\",\"type\":\"BUFFER\", \"subType\":\"DROPPED_ORDERS\"}]}"));
    }

    @Test
    @SneakyThrows
    void acceptAndSortDroppedOrder() {
        var place = testFactory.create(order(sortingCenter)
                        .shipmentDate(LocalDate.now(clock)).build())
                .getPlace();
        cell = testFactory.storedCell(sortingCenter, CellType.BUFFER, CellSubType.DROPPED_ORDERS, null);

        caller.acceptOrder(new AcceptOrderRequestDto(place))
                .andExpect(status().is2xxSuccessful());

        caller.sortableBetaSort(new SortableSortRequestDto(place, cell))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @SneakyThrows
    void acceptAndSortDroppedOrderInDefaultBufferCell() {
        var place1 = testFactory.create(order(sortingCenter)
                        .shipmentDate(LocalDate.now(clock)).build())
                .getPlace();
        testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept();
        var bufferCell = testFactory.storedCell(sortingCenter, CellType.BUFFER, CellSubType.DEFAULT, null);

        caller.acceptOrder(new AcceptOrderRequestDto(place1))
                .andExpect(status().is2xxSuccessful());

        caller.sortableBetaSort(new SortableSortRequestDto(place1, cell))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                        .json("{\"status\":400,\"error\":\"CANT_FIND_POSSIBLE_ROUTE\",\"message\"" +
                                ":\"Заказ должен быть помещен в другую ячейку\"}", false));
    }

    @Test
    @SneakyThrows
    void keepOrderIntoBufferCellDroppedSubtype() {
        var place = testFactory.createOrder(sortingCenter)
                .updateShipmentDate(LocalDate.now(clock).plus(2, ChronoUnit.DAYS))
                .accept()
                .getPlace();
        var droppedCell = testFactory.storedCell(sortingCenter, CellType.BUFFER, CellSubType.DROPPED_ORDERS, null);

        caller.sortableBetaSort(new SortableSortRequestDto(place, droppedCell))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                        .json("{\"status\":400,\"error\":\"NOT_SUPPORTED_CELL_SUBTYPE\",\"message\"" +
                                ":\"Cell Cell[" + droppedCell.getId()
                                + "] has wrong subtype DROPPED_ORDERS. Expected: DEFAULT\"}", false));
    }

    @Test
    @SneakyThrows
    void fieldCellToShouldBeNullWhenOrderKeep() {
        var order = testFactory.createForToday(
                        TestFactory.CreateOrderParams.builder().sortingCenter(sortingCenter).externalId("order1").build())
                .updateShipmentDate(LocalDate.now(clock).plus(2, ChronoUnit.DAYS))
                .get();
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" + Objects.requireNonNull(order)
                                        .getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.cellTo").doesNotExist());
    }

    @Test
    @SneakyThrows
    void multiOrderHasPlaceStatuses() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1", "2").get();
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" + order.getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andDo(handler -> {
                    System.out.println("Content: " + handler.getResponse().getContentAsString());
                })
                .andExpect(status().isOk())
                .andExpect(content().json("{" +
                        "\"places\":[" +
                        "{\"externalId\":\"1\",\"status\":\"SORT_TO_COURIER\"}," +
                        "{\"externalId\":\"2\",\"status\":\"SORT_TO_COURIER\"}" +
                        "]" +
                        "}"));
    }

    @Test
    @SneakyThrows
    void getUnknownClientReturnOrder() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        testFactory.setWarehouseProperty(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId(),
                WarehouseProperty.CAN_PROCESS_CLIENT_RETURNS, "true");
        testFactory.setWarehouseProperty(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId(),
                WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS, "false");
        testFactory.storedFakeReturnDeliveryService();
        String returnId = "VOZVRAT_SF_PS_1";
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"externalId\":\"" + returnId + "\"}")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{"
                        + "\"externalId\": \"" + returnId + "\","
                        + "\"status\": \"SORT_TO_WAREHOUSE\""
                        + "}"
                ));
        var response = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/orders?externalId={returnId}", returnId)
                        .header("Authorization", "OAuth uid-" + UID)
        ).andExpect(status().is2xxSuccessful());
        var dtoResp = readContentAsClass(response, ApiOrderDto.class);
        assertThat(CollectionUtils.isNonEmpty(dtoResp.getAvailableCells())).isTrue();
        for (var curCell : dtoResp.getAvailableCells()) {
            assertThat(curCell.getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);
            assertThat(curCell.getType()).isEqualTo(CellType.RETURN);
            assertThat(curCell.getSubType()).isEqualTo(CellSubType.CLIENT_RETURN);
        }
    }

    /**
     * Если создать клиентский возврат
     * Принять его через приемку возвратов, то потом можно принимать этот заказ через первичную приемку
     */
    @Test
    @SneakyThrows
    void getUnknownClientReturnOrderAndSortViaStraightFlow() {
        testFactory.setupWarehouseAndDeliveryServiceForClientReturns();
        String returnId = "VOZVRAT_SF_PS_1";
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"externalId\":\"" + returnId + "\"}")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{"
                        + "\"externalId\": \"" + returnId + "\","
                        + "\"status\": \"SORT_TO_WAREHOUSE\""
                        + "}"
                ));
        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/orders?externalId={returnId}", returnId)
                        .header("Authorization", "OAuth uid-" + UID)
        ).andExpect(status().is2xxSuccessful());
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(returnId, null))
                )
                .andExpect(status().is4xxClientError());
        mockMvc.perform(
                        put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(returnId, null))
                )
                .andExpect(status().is2xxSuccessful());

    }

    /**
     * Сортируем клиентский возвратный заказ в возвратную ячейку подтипа CLIENT_RETURN
     */
    @Test
    @SneakyThrows
    void sortUnknownClientReturnOrderToClientReturnCell() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        var clientReturnCell = testFactory.storedCell(
                sortingCenter,
                "default return",
                CellType.RETURN,
                CellSubType.CLIENT_RETURN,
                ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        var returnId = "VOZVRAT_SF_PS_1";
        mockMvc.perform(
                        put("/api/orders/acceptReturn")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"externalId\":\"" + returnId + "\"}"))
                .andExpect(status().isOk());

        var request = new SortableSortRequestDto(returnId, returnId, String.valueOf(clientReturnCell.getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().isOk());
    }

    /**
     * Сортируем поврежденные заказы только в возвратные ячейки подтипа "Поврежденные"
     */
    @Test
    @SneakyThrows
    void sortDamagedOrderOnlyInReturnDamagedCell() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        var damagedCell = testFactory.storedCell(sortingCenter,
                "damaged cell",
                CellType.RETURN,
                CellSubType.RETURN_DAMAGED,
                ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        var clientReturn = testFactory.storedCell(sortingCenter,
                "client return",
                CellType.RETURN,
                CellSubType.CLIENT_RETURN,
                ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        var defaultReturn = testFactory.storedCell(sortingCenter,
                "default return",
                CellType.RETURN,
                CellSubType.DEFAULT,
                ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        var courierCell = testFactory.storedCell(sortingCenter,
                "courier default",
                CellType.COURIER,
                CellSubType.DEFAULT,
                ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        var bufferCell = testFactory.storedCell(sortingCenter,
                "buffer default",
                CellType.BUFFER,
                CellSubType.DEFAULT,
                ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        var damagedOrder = testFactory.createForToday(
                        TestFactory.CreateOrderParams.builder()
                                .sortingCenter(sortingCenter)
                                .warehouseReturnId(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId())
                                .externalId("1")
                                .warehouseCanProcessDamagedOrders(true)
                                .build())
                .accept().markOrderAsDamaged().get();

        caller.sortableBetaSort(new SortableSortRequestDto(
                        damagedOrder.getExternalId(),
                        damagedOrder.getExternalId(),
                        String.valueOf(clientReturn.getId())))
                .andExpect(status().is4xxClientError());

        caller.sortableBetaSort(new SortableSortRequestDto(
                        damagedOrder.getExternalId(),
                        damagedOrder.getExternalId(),
                        String.valueOf(defaultReturn.getId())))
                .andExpect(status().is4xxClientError());

        caller.sortableBetaSort(new SortableSortRequestDto(
                        damagedOrder.getExternalId(),
                        damagedOrder.getExternalId(),
                        String.valueOf(courierCell.getId())))
                .andExpect(status().is4xxClientError());

        caller.sortableBetaSort(new SortableSortRequestDto(
                        damagedOrder.getExternalId(),
                        damagedOrder.getExternalId(),
                        String.valueOf(bufferCell.getId())))
                .andExpect(status().is4xxClientError());

        caller.sortableBetaSort(new SortableSortRequestDto(
                        damagedOrder.getExternalId(),
                        damagedOrder.getExternalId(),
                        String.valueOf(damagedCell.getId())))
                .andExpect(status().isOk());
    }

    /**
     * Когда выключена доработка с подтипом ячеек клиентских возвратов
     * Мы работаем с клиентским возвратом как с засылом
     */
    @Test
    @SneakyThrows
    void getUnknownClientReturnOrderWhenClientReturnSubtypeNotSupported() {
        testFactory.setupWarehouseAndDeliveryServiceForClientReturns();
        String returnId = "VOZVRAT_SF_PS_1";
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId={returnId}", returnId)
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{"
                        + "\"externalId\": \"" + returnId + "\","
                        + "\"status\": \"ERROR\""
                        + "}"
                ));
    }

    @Test
    void keepOrderIfRouteCellDistributionDisabled() throws Exception {
        var order1 = testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept().sort().get();
        testFactory.shipOrderRouteAndDisableCellDistribution(order1);
        testFactory.createForToday(order(sortingCenter).externalId("o2").build()).get();
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=o2")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{"
                        + "\"status\": \"KEEP\""
                        + "}"
                ));
    }

    @Test
    @SneakyThrows
    void regularReturnGoesToDefaultReturnCell() {
        var order = testFactory.createOrder(sortingCenter).accept().keep().makeReturn().get();
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId={returnId}", order.getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{"
                        + "\"externalId\": \"" + order.getExternalId() + "\","
                        + "\"status\": \"SORT_TO_WAREHOUSE\""
                        + "}"
                ));
        var response = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/orders?externalId={returnId}", order.getExternalId())
                        .header("Authorization", "OAuth uid-" + UID)
        ).andExpect(status().is2xxSuccessful());
        var dtoResp = readContentAsClass(response, ApiOrderDto.class);
        assertThat(CollectionUtils.isNonEmpty(dtoResp.getAvailableCells())).isTrue();
        for (var curCell : dtoResp.getAvailableCells()) {
            assertThat(curCell.getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);
            assertThat(curCell.getType()).isEqualTo(CellType.RETURN);
            assertThat(curCell.getSubType()).isEqualTo(CellSubType.DEFAULT);
        }
    }

    /**
     * не можем отсортировать обычный возвратный заказ в возвратную ячейку подтипа CLIENT_RETURN
     * и не можем отсортировать его в возвратную ячейку подтипа RETURN_DAMAGED
     */
    @Test
    @SneakyThrows
    void cantSortRegularReturnToClientReturnCell() {
        var order = testFactory.createOrder(sortingCenter).accept().keep().makeReturn().get();
        var clientReturnCell = testFactory.storedCell(sortingCenter,
                "client-return",
                CellType.RETURN,
                CellSubType.CLIENT_RETURN,
                order.getWarehouseReturn().getYandexId());
        var damageReturnCell = testFactory.storedCell(sortingCenter,
                "damaged-return",
                CellType.RETURN,
                CellSubType.RETURN_DAMAGED,
                order.getWarehouseReturn().getYandexId());
        var request = new SortableSortRequestDto(order.getExternalId(), order.getExternalId(),
                String.valueOf(clientReturnCell.getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"status\":400,\"error\":\"CELL_FROM_ANOTHER_ROUTE\"}",
                        false));

        request = new SortableSortRequestDto(order.getExternalId(), order.getExternalId(),
                String.valueOf(damageReturnCell.getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"status\":400,\"error\":\"CELL_FROM_ANOTHER_ROUTE\"}",
                        false));
    }

    /**
     * Обычный возвратный заказ (не клиентский) должен проситься в ячейку обычного типа
     * если на этом маршруте уже есть ячейки подтипа CLIENT_RETURN и RETURN_DAMAGED
     */
    @Test
    @SneakyThrows
    void cantSortRegularReturnToClientReturnCell2() {
        testFactory.setupWarehouseAndDeliveryServiceForClientReturns();
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        var order = testFactory.create(order(sortingCenter)
                        .externalId("new order")
                        .shipmentDate(LocalDate.now(clock))
                        .warehouseReturnId(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId())
                        .build())
                .updateCourier(testFactory.storedCourier())
                .accept().sort().ship().makeReturn()
                .accept().get();
        var clientReturnCell = testFactory.storedCell(sortingCenter,
                "client-return-1",
                CellType.RETURN,
                CellSubType.CLIENT_RETURN,
                order.getWarehouseReturn().getYandexId());
        var damagedCell = testFactory.storedCell(sortingCenter,
                "damaged-cell",
                CellType.RETURN,
                CellSubType.CLIENT_RETURN,
                order.getWarehouseReturn().getYandexId());
        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возвратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";
        testFactory.createClientReturnForToday(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker
        );
        var damagedOrder = testFactory.createForToday(
                        TestFactory.CreateOrderParams.builder()
                                .sortingCenter(sortingCenter)
                                .warehouseReturnId(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId())
                                .externalId("1")
                                .warehouseCanProcessDamagedOrders(true)
                                .build())
                .accept().markOrderAsDamaged().get();
        var regularReturnCell = testFactory.storedCell(sortingCenter,
                "default-return",
                CellType.RETURN,
                CellSubType.DEFAULT,
                order.getWarehouseReturn().getYandexId());

        var response = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/orders?externalId={returnId}", order.getExternalId())
                        .header("Authorization", "OAuth uid-" + UID)
        ).andExpect(status().is2xxSuccessful());
        var dtoResp = readContentAsClass(response, ApiOrderDto.class);
        assertThat(CollectionUtils.isNonEmpty(dtoResp.getAvailableCells())).isTrue();
        for (var curCell : dtoResp.getAvailableCells()) {
            assertThat(curCell.getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);
            assertThat(curCell.getType()).isEqualTo(CellType.RETURN);
            assertThat(curCell.getSubType()).isEqualTo(CellSubType.DEFAULT);
        }
    }

    @SneakyThrows
    @Test
    void sortOrderToCourierLot() {
        var courier = testFactory.storedCourier();
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER, courier.getId());
        var place = testFactory.createOrderForToday(sortingCenter).updateCourier(courier).accept().sort().getPlace();
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);

        caller.sortableBetaSort(new SortableSortRequestDto(place, lot))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.destination.id", is(lot.getBarcode())))
                .andExpect(jsonPath("$.destination.type", is(DestinationScType.LOT.toString())))
                .andExpect(jsonPath("$.parentRequired", is(false)));

        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @SneakyThrows
    @Test
    void resortOrderWithLot() {
        var user = testFactory.storedUser(sortingCenter, 700L);
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().get();
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);

        testFactory.sortOrderToLot(order, lot, user);

        mockMvc.perform(put("/api/orders/accept")
                        .header("Authorization", "OAuth uid-" + user.getUid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(
                                new AcceptOrderRequestDto(order.getExternalId(), null)))
                )
                .andExpect(jsonPath("$.places[0].currentLot['lotId']").value(lot.getLotId()))
                .andExpect(jsonPath("$.places[0].currentLot['lotStatus']").isNotEmpty());
    }

    @SneakyThrows
    @Test
    void resortPlaceWithLot() {
        var user = testFactory.storedUser(sortingCenter, 700L);
        var order = testFactory.create(order(sortingCenter).externalId("1").places("p1", "p2")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .acceptPlaces(List.of("p1", "p2"))
                .sortPlaces(List.of("p1", "p2"))
                .get();
        var courierCell = testFactory.anyOrderPlace(order).getCell();
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);

        testFactory.sortPlaceToLot(testFactory.orderPlace(order, "p1"), lot, user);
        testFactory.sortPlaceToLot(testFactory.orderPlace(order, "p2"), lot, user);

        mockMvc.perform(put("/api/orders/accept")
                        .header("Authorization", "OAuth uid-" + user.getUid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(
                                new AcceptOrderRequestDto(order.getExternalId(), "p1")))
                )
                .andExpect(jsonPath("$.places[0].currentLot['lotId']").value(lot.getLotId()))
                .andExpect(jsonPath("$.places[0].currentLot['lotStatus']").isNotEmpty())
                .andExpect(jsonPath("$.places[1].currentLot['lotId']").value(lot.getLotId()))
                .andExpect(jsonPath("$.places[1].currentLot['lotStatus']").isNotEmpty());
    }

    @SneakyThrows
    @Test
    void sortOrderToLotWithoutCell() {
        var deliveryService = testFactory.storedDeliveryService("345");
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var place = testFactory.createForToday(
                        order(sortingCenter)
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .accept().getPlace();
        var route0 = testFactory.findOutgoingCourierRoute(place).orElseThrow().allowReading();
        var cell = route0.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);

        caller.sortableBetaSort(new SortableSortRequestDto(place, lot))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.destination.id", is(lot.getBarcode())))
                .andExpect(jsonPath("$.destination.type", is(DestinationScType.LOT.toString())))
                .andExpect(jsonPath("$.parentRequired", is(false)));

        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @SneakyThrows
    @Test
    void sortOrderToLotWithoutCellWithSomeLots() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var deliveryService = testFactory.storedDeliveryService("345");
        var place = testFactory.createForToday(
                        order(sortingCenter)
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .accept().getPlace();
        var route = testFactory.findOutgoingRoute(place).orElseThrow().allowReading();
        var cell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        SortableLot lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        SortableLot lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);

        caller.sortableBetaSort(new SortableSortRequestDto(place, lot2))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.destination.id", is(lot2.getBarcode())))
                .andExpect(jsonPath("$.destination.type", is(DestinationScType.LOT.toString())))
                .andExpect(jsonPath("$.parentRequired", is(false)));

        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);

    }

    @SneakyThrows
    @Test
    void sortOrderToLotWithoutCellJustWithClosedLot() {
        var courier = testFactory.storedCourier();
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER, courier.getId());
        SortableLot lot = testFactory.storedLot(sortingCenter, cell, LotStatus.SHIPPED);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var place = testFactory.createOrderForToday(sortingCenter).updateCourier(courier).accept().getPlace();
        caller.sortableBetaSort(new SortableSortRequestDto(place, lot))
                .andExpect(status().is4xxClientError());
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    @SneakyThrows
    @Test
    void sortOrderToLotWithoutCellWithClosedLot() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var deliveryService = testFactory.storedDeliveryService("345");
        var place = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .deliveryService(deliveryService)
                                .build())
                .accept().getPlace();
        var route0 = testFactory.findOutgoingRoute(place).orElseThrow().allowReading();
        var cell = route0.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        SortableLot lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        SortableLot lot2 = testFactory.storedLot(sortingCenter, cell, LotStatus.SHIPPED);

        caller.sortableBetaSort(new SortableSortRequestDto(place, lot1))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.destination.id", is(lot1.getBarcode())))
                .andExpect(jsonPath("$.destination.type", is(DestinationScType.LOT.toString())))
                .andExpect(jsonPath("$.parentRequired", is(false)));
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);

        caller.sortableBetaSort(new SortableSortRequestDto(place, lot2))
                .andExpect(status().is4xxClientError());
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);

    }

    @SneakyThrows
    @Test
    void sortOrderWithNotPalletizationAvailable() {
        var courier = testFactory.storedCourier();
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER, courier.getId());
        testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);

        var place = testFactory.createForToday(order(sortingCenter).build())
                .accept()
                .getPlace();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(place)).orElseThrow();
        Cell courierCell = testFactory.determineRouteCell(route, place);

        caller.sortableBetaSort(new SortableSortRequestDto(place, courierCell))
                .andExpect(status().isOk());
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);

    }

    @SneakyThrows
    @Test
    void sortOrderInCellWithoutPalletizationAvailable() {
        var courier = testFactory.storedCourier();
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER, courier.getId());
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);

        var place = testFactory.createOrderForToday(sortingCenter).updateCourier(courier).accept().sort().getPlace();
        caller.sortableBetaSort(new SortableSortRequestDto(place, lot))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.destination.id", is(lot.getBarcode())))
                .andExpect(jsonPath("$.destination.type", is(DestinationScType.LOT.toString())))
                .andExpect(jsonPath("$.parentRequired", is(false)));

        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    /**
     * Нельзя отсортировать заказ прямого потока в лот для возвратов
     */
    @SneakyThrows
    @Test
    void cantSortOrderForCourierLotIntoReturnLot() {
        testFactory.increaseScOrderId();
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var deliveryService = testFactory.storedDeliveryService("345");
        var place = testFactory.createForToday(
                        order(sortingCenter)
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .accept().sort().getPlace();
        var route = testFactory.findOutgoingRoute(place).orElseThrow().allowReading();
        var courierCell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        SortableLot courierLot = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        var returnCell = testFactory.storedCell(sortingCenter, "r", CellType.RETURN, "w");
        var returnLot = testFactory.storedLot(sortingCenter, SortableType.PALLET, returnCell);

        caller.sortableBetaSort(new SortableSortRequestDto(place, returnLot))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"status\":400,\"error\":\"LOT_PARENT_CELL_FROM_ANOTHER_ROUTE\"}", false));
    }

    /**
     * Нельзя отсортировать заказ прямого потока в лот не с этого маршрута
     */
    @SneakyThrows
    @Test
    void cantSortOrderForCourierLotIntoLotFromAnotherRoute() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var deliveryService = testFactory.storedDeliveryService("345");
        var place = testFactory.createForToday(order(sortingCenter)
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .accept().sort().getPlace();
        var courier1 = place.getCourier();
        var courier2 = testFactory.storedCourier(2L);
        var route = testFactory.findOutgoingRoute(place).orElseThrow().allowReading();
        var courierCell1 = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var courierCell2 = testFactory.storedCell(sortingCenter, "2", CellType.COURIER, courier2.getId());
        SortableLot courierLot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell1);
        SortableLot courierLot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell2);
        caller.sortableBetaSort(new SortableSortRequestDto(place, courierLot2))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"status\":400,\"error\":\"LOT_PARENT_CELL_FROM_ANOTHER_ROUTE\"}", false));
    }

    /**
     * Заказ из прямого потока может быть отсортирован в любой лот привязанный к ячейке
     */
    @SneakyThrows
    @Test
    void canSortOrderToAnyLotInCell() {
        var courier = testFactory.storedCourier(1L);
        var courierCell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER, courier.getId());
        var place = testFactory.createOrderForToday(sortingCenter).updateCourier(courier).accept().sort().getPlace();
        SortableLot courierLot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        SortableLot courierLot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);

        caller.sortableBetaSort(new SortableSortRequestDto(place, courierLot2))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.destination.id", is(courierLot2.getBarcode())))
                .andExpect(jsonPath("$.destination.type", is(DestinationScType.LOT.toString())))
                .andExpect(jsonPath("$.parentRequired", is(false)));

        place = testFactory.getPlace(place.getId());
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.SORTED_DIRECT);
        assertThat(place.getParent()).isEqualTo(courierLot2.getSortable());
    }

    @SneakyThrows
    @Test
    void sortMultiPlaceOrderToCourierLot() {
        var courier = testFactory.storedCourier(1L);
        var courierCell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER, courier.getId());
        var order = testFactory.create(order(sortingCenter).externalId("1").places("p1", "p2")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .updateCourier(courier)
                .acceptPlaces(List.of("p1", "p2"))
                .sortPlaces(List.of("p1", "p2"))
                .get();
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        SortableSortRequestDto request1 = new SortableSortRequestDto(order.getExternalId(), "p1", lot.getBarcode());
        SortableSortRequestDto request2 = new SortableSortRequestDto(order.getExternalId(), "p2", lot.getBarcode());
        caller.sortableBetaSort(request1)
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.destination.id", is(lot.getBarcode())))
                .andExpect(jsonPath("$.destination.type", is(DestinationScType.LOT.toString())))
                .andExpect(jsonPath("$.parentRequired", is(false)));
        caller.sortableBetaSort(request2)
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.destination.id", is(lot.getBarcode())))
                .andExpect(jsonPath("$.destination.type", is(DestinationScType.LOT.toString())))
                .andExpect(jsonPath("$.parentRequired", is(false)));

        Place place1 = testFactory.orderPlace(order, "p1");
        Place place2 = testFactory.orderPlace(order, "p2");
        assertThat(place1.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(place1.getSortableStatus()).isEqualTo(SortableStatus.SORTED_DIRECT);
        assertThat(place1.getParent()).isEqualTo(lot.getSortable());
        assertThat(place2.getSortableStatus()).isEqualTo(SortableStatus.SORTED_DIRECT);
        assertThat(place2.getParent()).isEqualTo(lot.getSortable());
    }

    @SneakyThrows
    @Test
    void sortMultiPlaceOrdersIntoDifferentPallets() {
        var order = testFactory.create(order(sortingCenter).externalId("1").places("p1", "p2")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .acceptPlaces(List.of("p1", "p2"))
                .sortPlaces(List.of("p1", "p2"))
                .get();
        var courierCell = testFactory.anyOrderPlace(order).getCell();
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        SortableLot lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);

        SortableSortRequestDto request1 = new SortableSortRequestDto(order.getExternalId(), "p1", lot.getBarcode());
        SortableSortRequestDto request2 = new SortableSortRequestDto(order.getExternalId(), "p2", lot2.getBarcode());
        caller.sortableBetaSort(request1)
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.destination.id", is(lot.getBarcode())))
                .andExpect(jsonPath("$.destination.type", is(DestinationScType.LOT.toString())))
                .andExpect(jsonPath("$.parentRequired", is(false)));
        caller.sortableBetaSort(request2)
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.destination.id", is(lot2.getBarcode())))
                .andExpect(jsonPath("$.destination.type", is(DestinationScType.LOT.toString())))
                .andExpect(jsonPath("$.parentRequired", is(false)));

        Place place1 = testFactory.orderPlace(order, "p1");
        Place place2 = testFactory.orderPlace(order, "p2");
        assertThat(place1.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(place1.getSortableStatus()).isEqualTo(SortableStatus.SORTED_DIRECT);
        assertThat(place1.getParent()).isEqualTo(lot.getSortable());
        assertThat(place2.getSortableStatus()).isEqualTo(SortableStatus.SORTED_DIRECT);
        assertThat(place2.getParent()).isEqualTo(lot2.getSortable());
    }

    @SneakyThrows
    @Test
    void lotSortOrder() {
        Cell returnCell = testFactory.storedCell(sortingCenter, "return-1", CellType.RETURN);
        Place place = testFactory.createOrderForToday(sortingCenter).accept().cancel().sort().getPlace();
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, returnCell);
        caller.sortableBetaSort(new SortableSortRequestDto(place, lot))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.destination.id", is(lot.getBarcode())))
                .andExpect(jsonPath("$.destination.type", is(DestinationScType.LOT.toString())))
                .andExpect(jsonPath("$.parentRequired", is(false)));

        place = testFactory.getPlace(place.getId());
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
        assertThat(place.getParent()).isEqualTo(lot.getSortable());
    }

    @SneakyThrows
    @Test
    void getOrderWithLots() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        Cell returnCell = testFactory.storedCell(sortingCenter, "return-1", CellType.RETURN);
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().cancel().sort().get();
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, returnCell);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/orders/withLots?externalId={returnId}", order.getExternalId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "OAuth uid-" + UID)
                ).andExpect(status().isOk())
                .andExpect(content().json("{" +
                        "\"id\":" + order.getId() + "," +
                        "\"externalId\":" + order.getExternalId() + "," +
                        "\"lotsTo\":[{\"id\": " + lot.getLotId() + ",\"name\":\"" + lot.getNameForApi() + "\"}]" +
                        "}"));

    }

    @SneakyThrows
    @Test
    void getMultiplaceOrderWithLots() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        Cell returnCell = testFactory.storedCell(sortingCenter, "return-1", CellType.RETURN);
        var place1 = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces()
                .cancel()
                .sortPlaces()
                .getPlace("p1");
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, returnCell);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/orders/withLots?externalId={returnId}", place1.getExternalId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("{" +
                        "\"id\":" + place1.getOrderId() + "," +
                        "\"externalId\":" + place1.getExternalId() + "," +
                        "\"lotsTo\":[{\"id\": " + lot.getLotId() + ",\"name\":\"" + lot.getNameForApi() + "\"}]" +
                        "}"));

    }

    @SneakyThrows
    @Test
    void finishOutgoingRouteAfterLotSortOrderMultiPlace() {
        Cell returnCell = testFactory.storedCell(sortingCenter, "return-1", CellType.RETURN);
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, returnCell);

        var orderMultiPlace = testFactory.createForToday(order(sortingCenter, "5").places("1", "2").build()).cancel()
                .acceptPlaces().sortPlaces().getPlaces();
        Place place1 = orderMultiPlace.get("1");
        Place place2 = orderMultiPlace.get("2");

        Place place3 = testFactory.createForToday(order(sortingCenter, "3").build())
                .accept().cancel().sort().getPlace();
        Place place4 = testFactory.createForToday(order(sortingCenter, "4").build())
                .accept().sort().getPlace();

        caller.sortableBetaSort(new SortableSortRequestDto(place1, lot))
                .andExpect(status().isOk());

        caller.sortableBetaSort(new SortableSortRequestDto(place2, lot))
                .andExpect(status().isOk());

        caller.getOrderIdList(null, place1.getCell().getId(), null)
                .andExpect(jsonPath("$.orders", hasSize(2)))
                .andExpect(jsonPath("$.orders[1].places", hasSize(2)))
                .andExpect(jsonPath("$.orders[1].places[0].status", is(ACCEPTED_AT_SORTING_CENTER.name())))
                .andExpect(jsonPath("$.orders[1].places[0].cell").doesNotExist())
                .andExpect(jsonPath("$.orders[1].places[1].status", is(ACCEPTED_AT_SORTING_CENTER.name())))
                .andExpect(jsonPath("$.orders[1].places[1].cell").doesNotExist())
                .andExpect(status().isOk());

        var outgoingCourierRoute = testFactory.findOutgoingCourierRoute(place4).orElseThrow().allowReading();
        var cell1 = testFactory.determineRouteCell(outgoingCourierRoute, place4);

        Long courierRouteId = testFactory.getRouteIdForSortableFlow(outgoingCourierRoute);
        assertDoesNotThrow(() -> routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                courierRouteId,
                new ScContext(testFactory.getOrCreateStoredUser(sortingCenter)),
                List.of(cell1.getId()),
                null,
                false
        )));
    }

    @Test
    @SneakyThrows
    @DisplayName("Попытка отсортировать не запакованную паллету при сортировки")
    void sortUnpackedPallet() {
        Warehouse warehouse = testFactory.storedWarehouse("WHS-123");
        String barcode = "XDOC-PALLET123";
        Outbound outbound = testFactory.createOutbound(
                TestFactory.CreateOutboundParams.builder()
                        .externalId("OUT-123")
                        .sortingCenter(sortingCenter)
                        .partnerToExternalId(warehouse.getYandexId())
                        .logisticPointToExternalId(warehouse.getYandexId())
                        .toTime(clock.instant())
                        .fromTime(clock.instant())
                        .carNumber("A777MP77")
                        .type(OutboundType.XDOC)
                        .build()
        );
        Inbound inbound = testFactory.createInbound(
                TestFactory.CreateInboundParams.builder()
                        .fromDate(OffsetDateTime.now(clock))
                        .toDate(OffsetDateTime.now(clock))
                        .inboundType(InboundType.XDOC_TRANSIT)
                        .sortingCenter(sortingCenter)
                        .inboundExternalId("IN-123")
                        .nextLogisticPointId(warehouse.getYandexId())
                        .build()
        );

        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.XDOC_BASKET,
                testFactory.storedCell(sortingCenter, "123123", CellType.COURIER), LotStatus.CREATED, false,
                barcode
        );

        Sortable sortable = sortableQueryService.find(sortingCenter, barcode).orElseThrow();

        var request = new SortableSortRequestDto(
                sortable.getRequiredBarcodeOrThrow(),
                "123",
                "123123");
        caller.sortableBetaSort(request)
                .andExpect(status().is4xxClientError());
    }

    @Test
    @SneakyThrows
    @Transactional
    @DisplayName("Попытка отсортировать не запакованную паллету при получении ячейки")
    void getSortableWithUnpackedPallet() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, true);

        String barcode = "XDOC-PALLET123";
        Warehouse warehouse = testFactory.storedWarehouse("123");
        Inbound inbound = testFactory.createInbound(
                TestFactory.CreateInboundParams.builder()
                        .fromDate(OffsetDateTime.now(clock))
                        .toDate(OffsetDateTime.now(clock))
                        .inboundType(InboundType.XDOC_TRANSIT)
                        .sortingCenter(sortingCenter)
                        .inboundExternalId("IN-123")
                        .build()
        );

        Outbound outbound = testFactory.createOutbound(
                TestFactory.CreateOutboundParams.builder()
                        .sortingCenter(sortingCenter)
                        .externalId("OUT-123")
                        .carNumber("A777MP77")
                        .fromTime(Instant.now(clock))
                        .toTime(Instant.now(clock))
                        .type(OutboundType.XDOC)
                        .partnerToExternalId("123")
                        .logisticPointToExternalId("123")
                        .build()
        );
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.XDOC_BASKET,
                testFactory.storedCell(sortingCenter, "123123", CellType.COURIER), LotStatus.CREATED, false,
                barcode
        );

        Sortable sortable = sortableQueryService.find(sortingCenter, barcode).orElseThrow();

        sortable.setMutableState(
                sortable.getMutableState()
                        .withInbound(inbound)
                        .withOutRoute(routeSoQueryService.getRouteByOutbound(outbound))
        );
        sortableRepository.save(sortable);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders?externalId=" + barcode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "OAuth uid-" + UID))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @DisplayName("Приемка возвратного одноместного заказа (засыла) другого СЦ")
    void acceptReturnOrderAnotherSc() {
        String warehouseReturnYandexId = "10001";
        var targetSortingCenter = testFactory.storedSortingCenter(100L);
        var mistakeSortingCenter = testFactory.storedSortingCenter(200L);
        var user = testFactory.storedUser(mistakeSortingCenter, 600L);
        var warehouseReturn = testFactory.storedWarehouse(warehouseReturnYandexId);

        initDeliveryReturnsMappings();

        // Возвратный заказ
        var order = testFactory.createForToday(order(targetSortingCenter, "o1")
                .warehouseReturnId(warehouseReturnYandexId).build()
        ).accept().sort().ship().makeReturn().get();

        mockMvc.perform(put("/api/orders/acceptReturn")
                        .header("Authorization", "OAuth uid-" + user.getUid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\":\"" + order.getExternalId() + "\", \"placeExternalId\":\"" + order.getExternalId() + "\"}")
                )
                .andDo(print())
                .andExpect(status().isOk());

        List<ScOrder> orders = scOrderRepository.findAll();
        assertThat(orders).hasSize(2);
        assertThat(orders).filteredOn(o -> o.getFfStatus().equals(SO_GOT_INFO_ABOUT_PLANNED_RETURN)).hasSize(1)
                .extracting(OrderLike::getSortingCenter, OrderLike::getWarehouseReturn)
                .containsOnly(tuple(targetSortingCenter, warehouseReturn));
        assertThat(orders).filteredOn(o -> o.getFfStatus().equals(RETURNED_ORDER_AT_SO_WAREHOUSE)).hasSize(1)
                .extracting(OrderLike::getSortingCenter, OrderLike::getWarehouseReturn)
                .containsOnly(tuple(mistakeSortingCenter, warehouseReturn));

        List<Place> places = placeRepository.findAll();
        assertThat(places).filteredOn(p -> p.getSortableStatus().equals(SortableStatus.AWAITING_RETURN)).hasSize(1)
                .extracting(OrderLike::getSortingCenter, OrderLike::getWarehouseReturn)
                .containsOnly(tuple(targetSortingCenter, warehouseReturn));
        assertThat(places).filteredOn(p -> p.getSortableStatus().equals(SortableStatus.ACCEPTED_RETURN)).hasSize(1)
                .extracting(OrderLike::getSortingCenter, OrderLike::getWarehouseReturn)
                .containsOnly(tuple(mistakeSortingCenter, warehouseReturn));
    }

    @Test
    @SneakyThrows
    @DisplayName("Приемка многоместного заказ (засыла) другого СЦ")
    void acceptReturnPlacesAnotherSc() {
        String warehouseReturnYandexId = "10002";
        var targetSortingCenter = testFactory.storedSortingCenter(300L);
        var mistakeSortingCenter = testFactory.storedSortingCenter(400L);
        var user = testFactory.storedUser(mistakeSortingCenter, 700L);
        var warehouseReturn = testFactory.storedWarehouse(warehouseReturnYandexId);

        initDeliveryReturnsMappings();

        // Возвратный заказ
        var order = testFactory.createForToday(order(targetSortingCenter, "o2")
                        .places("o2-1", "o2-2", "o2-3")
                        .warehouseReturnId(warehouseReturnYandexId).build()
                ).acceptPlaces("o2-1", "o2-2", "o2-3")
                .sortPlaces("o2-1", "o2-2", "o2-3")
                .ship()
                .makeReturn()
                .acceptPlaces("o2-1", "o2-3")
                .get();

        mockMvc.perform(put("/api/orders/acceptReturn")
                        .header("Authorization", "OAuth uid-" + user.getUid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(
                                new AcceptOrderRequestDto(order.getExternalId(), "o2-2")))
                )
                .andDo(print())
                .andExpect(status().isOk());

        var orderFromTargetSc =
                scOrderRepository.findBySortingCenterAndExternalId(targetSortingCenter, "o2").orElseThrow();
        var placesFromTargetSc = placeRepository.findAllByOrderIdOrderById(orderFromTargetSc.getId());
        var orderFromMistakeSc =
                scOrderRepository.findBySortingCenterAndExternalId(mistakeSortingCenter, "o2").orElseThrow();
        var placesFromMistakeSc = placeRepository.findAllByOrderIdOrderById(orderFromMistakeSc.getId());

        assertThat(List.of(orderFromTargetSc))
                .extracting(OrderLike::getFfStatus, OrderLike::getSortingCenter, OrderLike::getWarehouseReturn)
                .containsOnly(tuple(RETURNED_ORDER_AT_SO_WAREHOUSE, targetSortingCenter, warehouseReturn));
        assertThat(placesFromTargetSc).hasSize(3)
                .filteredOn(p -> p.getSortableStatus() == SortableStatus.ACCEPTED_RETURN).hasSize(2);
        assertThat(placesFromTargetSc).hasSize(3)
                .filteredOn(p -> p.getSortableStatus() == SortableStatus.AWAITING_RETURN).hasSize(1);

        assertThat(List.of(orderFromMistakeSc))
                .extracting(OrderLike::getFfStatus, OrderLike::getSortingCenter, OrderLike::getWarehouseReturn)
                .containsOnly(tuple(RETURNED_ORDER_AT_SO_WAREHOUSE, mistakeSortingCenter, warehouseReturn));
        assertThat(placesFromMistakeSc).hasSize(3)
                .filteredOn(p -> p.getSortableStatus() == SortableStatus.AWAITING_RETURN).hasSize(2);
        assertThat(placesFromMistakeSc).hasSize(3)
                .filteredOn(p -> p.getSortableStatus() == SortableStatus.ACCEPTED_RETURN).hasSize(1);
    }

    private void initDeliveryReturnsMappings() throws JsonProcessingException {
        var misdeliveryProcessingScs = Map.of(
                100L,
                new MisdeliveryProcessingProperties("MSK", 1, MisdeliveryReturnDirection.SORTING_CENTER, "10001",
                        List.of()),
                200L,
                new MisdeliveryProcessingProperties("MSK", 2, MisdeliveryReturnDirection.SORTING_CENTER, "10001",
                        List.of()),
                300L,
                new MisdeliveryProcessingProperties("MSK", 1, MisdeliveryReturnDirection.SORTING_CENTER, "10002",
                        List.of()),
                400L,
                new MisdeliveryProcessingProperties("MSK", 2, MisdeliveryReturnDirection.SORTING_CENTER, "10002",
                        List.of())
        );

        testFactory.setConfiguration(
                MISDELIVERY_RETURNS_MAPPINGS, new ObjectMapper().writeValueAsString(misdeliveryProcessingScs));
    }

    @Test
    @SneakyThrows
    @DisplayName("Создание неизвестного возвратного заказа при обычной приемке")
    void exceptionWhenAcceptUnknownReturnOrderAnotherSc() {
        String warehouseReturnYandexId = "10002";
        var targetSortingCenter = testFactory.storedSortingCenter(300L);
        var mistakeSortingCenter = testFactory.storedSortingCenter(400L);
        var user = testFactory.storedUser(mistakeSortingCenter, 700L);
        testFactory.storedWarehouse(warehouseReturnYandexId);

        initDeliveryReturnsMappings();

        var order = testFactory.createForToday(order(targetSortingCenter, "o1")
                .warehouseReturnId(warehouseReturnYandexId).build()
        ).accept().sort().ship().makeReturn().get();

        mockMvc.perform(put("/api/orders/accept")
                        .header("Authorization", "OAuth uid-" + user.getUid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(
                                new AcceptOrderRequestDto(order.getExternalId(), "o1-2")))
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ORDER_FROM_RETURN_STREAM"))
                .andExpect(jsonPath("$.message").value(ScErrorCode.ORDER_FROM_RETURN_STREAM.getMessage()));

        List<ScOrder> orders = scOrderRepository.findAllBySortingCenterAndExternalIdInOrderByIdAsc(
                mistakeSortingCenter, List.of("o1"));
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getFfStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
    }

    @Test
    @SneakyThrows
    @DisplayName("Логирование зоны/рабочей станции в order_scan_log")
    void logZoneIdAndWorkstationId() {
        var order = testFactory.createForToday(order(sortingCenter, "o1").places("o1-1").build()).accept().get();
        var user = testFactory.storedUser(sortingCenter, 10L);
        var zone = testFactory.storedZone(sortingCenter, "z1");
        var ws = testFactory.storedWorkstation(sortingCenter, "ws1", zone.getId(), testFactory.storedProcess("p", "p"));
        mockMvc.perform(get("/api/orders")
                .header("Authorization", "OAuth uid-" + user.getUid())
                .header(Constants.Header.SC_ZONE, ws.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .param("externalId", order.getExternalId())
        ).andDo(print());

        List<Map<String, Object>> logs = jdbcTemplate.queryForList("select * from order_scan_log");
        assertThat(logs).hasSize(1);

        var orderScanLog = logs.get(0);
        assertThat(orderScanLog)
                .containsEntry("zone_id", zone.getId())
                .containsEntry("workstation_id", ws.getId())
                .containsEntry("result", ScanLogResult.OK.name())
                .containsEntry("operation", ScanLogOperation.SCAN.name())
                .containsEntry("dispatch_person_id", user.getId())
                .containsEntry("external_order_id", order.getExternalId())
                .containsEntry("external_place_id", "o1-1")
                .containsEntry("sorting_center_id", sortingCenter.getId());
    }

    @Test
    void acceptAndFinishMultiPlace() {
        var placeIds = List.of("1", "2", "3", "4");
        var order = testFactory.createForToday(order(sortingCenter).places(placeIds.toArray(String[]::new)).build())
                .cancel().get();
        for (var placeId : placeIds) {
            testFactory.acceptPlace(order, placeId);
        }
        var places = testFactory.orderPlaces(order);
        assertThat(places).hasSize(4);
        places.forEach(
                (p) -> assertThat(testFactory.findRouteFinishPlacesByPlaceId(p.getId())).isNotEmpty()
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("[Приемка возврата на дропоффе] Не выдавать ошибку при сканировании штрихкода многоместного заказа")
    void scanOrderExternalIdThroughAcceptReturn() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ACCEPT_AND_SORT_RETURN_ON_DROPOFF, true);
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces().sortPlaces().shipPlaces()
                .makeReturn()
                .get();

        caller.acceptReturn(new AcceptReturnedOrderRequestDto(order.getExternalId(), null, null))
                .andExpect(status().isOk());
        ScOrderWithPlaces orderWithPlaces = testFactory.getOrderWithPlaces(order.getId());
        assertThat(orderWithPlaces.order().getFfStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(orderWithPlaces.place("p1").getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
        assertThat(orderWithPlaces.place("p2").getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
    }
}
