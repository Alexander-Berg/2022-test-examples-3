package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.courier.model.ApiCourierDto;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.route.model.ApiRouteStatus;
import ru.yandex.market.sc.core.domain.route.model.OutgoingCourierRouteType;
import ru.yandex.market.sc.core.domain.route.model.OutgoingRouteBaseDto;
import ru.yandex.market.sc.core.domain.route.model.RouteType;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;
import ru.yandex.market.tpl.common.util.JacksonUtil;
import ru.yandex.market.tpl.common.util.security.QRCodeUtil;
import ru.yandex.market.tpl.common.util.security.QrCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@ScApiControllerTest
@Slf4j
class RouteControllerTest {

    private static final long UID = 123L;

    @Autowired
    ConfigurationService configurationService;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    OrderCommandService orderCommandService;
    @Autowired
    ScOrderRepository scOrderRepository;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    TestFactory testFactory;
    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    User user;
    TestControllerCaller caller;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.storedCell(sortingCenter);
        user = testFactory.storedUser(sortingCenter, UID);
        doReturn(Instant.ofEpochMilli(0L)).when(clock).instant();
        doReturn(ZoneId.systemDefault()).when(clock).getZone();
        caller = TestControllerCaller.createCaller(mockMvc);
    }

    @Test
    void getRoutesList() throws Exception {
        var order = testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/routes/list?routeType=OUTGOING_COURIER")
                        .header("Authorization", "OAuth uid-" + UID)
        )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"routes\":[" +
                        "{\"name\":\"Иван Пивовар Таранов\",\"id\":" + testFactory.getRouteIdForSortableFlow(route) + "," +
                        "\"status\":\"FINISHED\",\"ordersCount\":1}" +
                        "]}"));
    }

    @Test
    @SneakyThrows
    void shipWithMultiPlaceOrder() {
        var order = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12").build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();

        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);

        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/routes/" + testFactory.getRouteIdForSortableFlow(route))
                        .header("Authorization", "OAuth uid-" + UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordersShipped\":[\"" + order.getExternalId() + "\"],\"ordersNotShipped\":[], " +
                                "\"cellId\":" + Objects.requireNonNull(cell).getId() + " }")
        )
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void shipWithMultiPlaceOrderByTwoLots() {
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.increaseScOrderId();
        var courier1 = testFactory.storedCourier(1L);
        var courier2 = testFactory.storedCourier(2L);
        var order = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12").build()
        ).updateCourier(courier1).acceptPlaces("11", "12").sortPlaces("11", "12").get();

        var order2 = testFactory.createForToday(
                order(sortingCenter, "2").places("21", "22").build()
        ).updateCourier(courier2).acceptPlaces("21", "22").sortPlaces("21", "22").get();

        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(sortingCenter, cell, LotStatus.CREATED);
        var route2 = testFactory.findOutgoingCourierRoute(order2).orElseThrow();
        var cell2 = testFactory.determineRouteCell(route2, order2);
        var lot2 = testFactory.storedLot(sortingCenter, cell2, LotStatus.CREATED);
        testFactory.sortToLot(order,  "11", lot, user);
        testFactory.sortToLot(order,  "12", lot, user);
        testFactory.prepareToShipLot(lot);
        testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);
        testFactory.sortToLot(order2,  "21", lot2, user);
        testFactory.sortToLot(order2, "22", lot2, user);
        testFactory.prepareToShipLot(lot2);
        Long route2Id = testFactory.getRouteIdForSortableFlow(route2);
        testFactory.shipLots(route2Id, sortingCenter);
    }

    @Test
    @SneakyThrows
    void actionsWithResorting() {
        var courierToShip = testFactory.storedCourier(1L);
        var courierToResorting = testFactory.storedCourier(2L);

        var orderToShip = testFactory.create(
                order(sortingCenter, "1").places("11", "12").build()
        ).updateShipmentDate(LocalDate.now(clock)).updateCourier(courierToShip)
                .acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var orderToResorting = testFactory.create(
                order(sortingCenter, "2").places("21", "22").build()
        ).updateShipmentDate(LocalDate.now(clock)).updateCourier(courierToResorting)
                .acceptPlaces("21").sortPlaces("21").get();

        var routeToShip = testFactory.findOutgoingCourierRoute(orderToShip).orElseThrow();
        var routeToResorting = testFactory.findOutgoingCourierRoute(orderToResorting).orElseThrow();

        Long routeToShipId = testFactory.getRouteIdForSortableFlow(routeToShip.getId());
        MvcResult toShipResult = caller.getOutgoingRoute(routeToShipId)
                .andExpect(status().isOk())
                .andReturn();
        var routeToShipDto = objectMapper.readValue(toShipResult.getResponse().getContentAsString(), OutgoingRouteBaseDto.class);
        caller.getCellForRoute(routeToShipDto.getCells().get(0).id(), routeToShipDto.getId())
                .andExpect(status().isOk())
                .andExpect(content().json("{\"actions\":[\"SHIP_AND_RESORT\", \"SHIP_ALL\"]}", false));

        Long routeToResortingId = testFactory.getRouteIdForSortableFlow(routeToResorting.getId());
        MvcResult toResortingResult = caller.getOutgoingRoute(routeToResortingId)
                .andExpect(status().isOk())
                .andReturn();
        var routeToResortingDto = objectMapper.readValue(toResortingResult.getResponse().getContentAsString(), OutgoingRouteBaseDto.class);
        caller.getCellForRoute(routeToResortingDto.getCells().get(0).id(), routeToResortingDto.getId())
                .andExpect(status().isOk())
                .andExpect(content().json("{\"actions\":[\"SHIP_AND_RESORT\"]}", false));
    }

    @Test
    @SneakyThrows
    void shipOrderByResortingWithLotInSameCell() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL,
                true);

        var courier = testFactory.storedCourier(1L);

        var place = testFactory.create(order(sortingCenter, "1").build())
                .updateShipmentDate(LocalDate.now(clock))
                .updateCourier(courier)
                .accept()
                .sort()
                .getPlace();

        assertThat(place.getCell()).isNotNull();

        var placeForLot = testFactory.create(order(sortingCenter, "2").build())
                .updateShipmentDate(LocalDate.now(clock))
                .updateCourier(courier)
                .accept()
                .sortToLot("SC_LOT_100000", SortableType.PALLET)
                .getPlace();

        Sortable parentLot = placeForLot.getParent();
        assertThat(parentLot).isNotNull();
        SortableLot sortableLot = testFactory.getLot(parentLot);
        testFactory.prepareToShipLot(sortableLot);

        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        assertThat(route).isNotNull();

        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/routes/" + testFactory.getRouteIdForSortableFlow(route))
                        .header("Authorization", "OAuth uid-" + UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                "orderShipped":"%s",
                                "placeShipped":"%s",
                                "cellId": %d
                                }""", place.getExternalId(), place.getMainPartnerCode(), place.getCell().getId()))
        ).andExpect(status().isOk());

        sortableLot = testFactory.getLot(sortableLot.getLotId());
        assertThat(sortableLot).isNotNull();
        assertThat(sortableLot.getLotStatusOrNull()).isEqualTo(LotStatus.READY);
    }

    @Test
    @SneakyThrows
    void dontLoseDispatchPersonDuringShipment() {
        var lotExternalId = "SC_LOT_100000";

        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL,
                true);
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().sortToLot(lotExternalId, SortableType.PALLET).get();

        var place = testFactory.orderPlace(order);
        testFactory.prepareToShipLot(testFactory.getLot(place.getLotId().get()));

        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/routes/" + testFactory.getRouteIdForSortableFlow(route))
                        .header("Authorization", "OAuth uid-" + UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"lotShippedExternalId\":\"%s\"}", lotExternalId))
        ).andExpect(status().isOk());

        transactionTemplate.execute((ignored) -> {
            assertThat(testFactory.getOrder(order.getId()).getFfStatus()).isEqualTo(
                    ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF
            );
            var orderStatusHistoryItems = testFactory.findOrderStatusHistoryItems(order.getId()).stream()
                    .filter(ofsh -> ofsh.getFfStatus() == ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF)
                    .toList();
            assertThat(orderStatusHistoryItems).hasSize(1);
            assertThat(orderStatusHistoryItems.get(0).getDispatchPerson().getRole()).isEqualTo(UserRole.STOCKMAN);
            return null;
        });
    }

    @Test
    @SneakyThrows
    @DisplayName("Тест на соответствие контракту")
    void getRoutesV2() {
        var orderWithMultiplaceIncomplete = testFactory.createForToday(
                order(sortingCenter).externalId("o1").places("p1", "p2").build()
        ).acceptPlaces("p1").sortPlaces("p1").get();

        var simpleOrder = testFactory.createForToday(
                order(sortingCenter).externalId("o2").build()
        ).accept().sort().prepare().get();

        var route = testFactory.findOutgoingCourierRoute(
                testFactory.anyOrderPlace(orderWithMultiplaceIncomplete))
            .orElseThrow().allowReading();

        assertThat(testFactory.orderPlace(simpleOrder).getCell()).isNotNull();
        assertThat(route.getCourierTo()).isNotNull();

        String expectedJson = String.format(
                "{\"id\": %d, \"cells\":[{\"id\":%d, \"empty\": false}]}",
                testFactory.getRouteIdForSortableFlow(route),
                testFactory.orderPlace(simpleOrder).getCell().getId()
        );

        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/v2/routes/" + testFactory.getRouteIdForSortableFlow(route))
                        .header("Authorization", "OAuth uid-" + UID))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson,false));

    }

    @Test
    @SneakyThrows
    void getOldRoute() {
        var order = testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept().sort().prepare().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));

        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/routes/" + testFactory.getRouteIdForSortableFlow(route))
                        .header("Authorization", "OAuth uid-" + UID)
        )
                .andExpect(status().is4xxClientError());
    }

    @Test
    @SneakyThrows
    void cellPreparedPartialMultiplaceInInCell() {
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("o1").places("p1", "p2").build()
        ).acceptPlaces("p1").sortPlaces("p1").get();

        testFactory.createForToday(
                order(sortingCenter).externalId("o2").build()
        ).accept().sort().prepare();

        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();

        MvcResult result = caller.getOutgoingRoute(testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().isOk())
                .andReturn();
        var routeDto = objectMapper.readValue(result.getResponse().getContentAsString(), OutgoingRouteBaseDto.class);

        caller.getCellForRoute(routeDto.getCells().get(0).id(), routeDto.getId())
                .andExpect(status().isOk())
                .andExpect(content().json("{\"cellPrepared\":false}", false));
    }

    @Test
    @Disabled
    @SneakyThrows
    void accept() {
        var order1 = testFactory.create(order(sortingCenter, "1").build()).cancel().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").build()).get();
        doReturn(clock.instant().plus(1, ChronoUnit.DAYS)).when(clock).instant();
        var order3 = testFactory.create(order(sortingCenter, "3").build()).cancel().get();
        var order4 = testFactory.createForToday(order(sortingCenter, "4").build()).get();

        getOrder(order1, orderKeep("1"), "orderKeep ext 1");
        acceptAndSortOrder(order1, "{}");
        var route1 = testFactory.findOutgoingWarehouseRoute(order1).orElseThrow();
        var cellWarehouse = testFactory.determineRouteCell(route1, order1);
        getOrder(order1, orderSortToWarehouse("1", ApiOrderStatus.SORT_TO_WAREHOUSE,
                Objects.requireNonNull(cellWarehouse).getId()), "orderSortToWarehouse ext 1");

        var route3 = testFactory.findPossibleRouteForCancelledOrder(order3).orElseThrow();
        var cellWarehouse3 = testFactory.determineRouteCell(route3, order3);
        getOrder(order3, orderSortToWarehouse("3", ApiOrderStatus.SORT_TO_WAREHOUSE,
                Objects.requireNonNull(cellWarehouse3).getId(), false), "orderSortToWarehouse ext 3");
        acceptAndSortOrder(order3, "{\"cellId\":" + cellWarehouse3.getId() + "}");

        getOrder(order2, orderKeep("2"), "orderKeep ext 2");
        acceptAndSortOrder(order2, "{}");
        getOrder(order2, orderKeep("2"), "orderKeep et 2 p 2");

        var route4 = testFactory.findOutgoingCourierRoute(order4).orElseThrow();
        var cellCourier = testFactory.determineRouteCell(route4, order4);
        getOrder(order4, orderSortToCourier("4", ApiOrderStatus.SORT_TO_COURIER,
                Objects.requireNonNull(cellCourier).getId()), "orderSortToCourier ext 3");
        acceptAndSortOrder(order4, "{\"cellId\":" + cellCourier.getId() + "}");
        getOrder(order4, orderSortToCourier("4", ApiOrderStatus.OK,
                cellCourier.getId()), "orderSortToCourier ext 4");
    }

    @SneakyThrows
    private void getOrder(OrderLike order, String expectedBody) {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/orders?externalId=" + order.getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json(expectedBody, false));
    }

    @SneakyThrows
    private void getOrder(OrderLike order, String expectedBody, @Nullable String message) {
        log.info(message);
        getOrder(order, expectedBody);
    }

    @SneakyThrows
    private void acceptAndSortOrder(OrderLike order, String requestBody) {
        log.info("rqBody {}", requestBody);
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/" + order.getId())
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk());
    }

    private String orderKeep(String externalId) {
        return "{\"externalId\":\"" + externalId + "\",\"status\":\"KEEP\"}";
    }

    @SuppressWarnings("SameParameterValue")
    private String orderSortToCourier(String externalId, ApiOrderStatus orderStatus, Long designatedCellId) {
        return "{\"externalId\":\"" + externalId + "\",\"status\":\"" + orderStatus + "\"," +
                "\"routeTo\":{" +
                "\"courier\":{\"id\":1,\"name\":\"Иван Пивовар Таранов\"}}," +
                "\"availableCells\":[{\"id\":" + designatedCellId + ",\"status\":\"NOT_ACTIVE\"}]}";
    }

    @SuppressWarnings("SameParameterValue")
    private String orderSortToWarehouse(String externalId, ApiOrderStatus orderStatus, Long designatedCellId) {
        return orderSortToWarehouse(externalId, orderStatus, designatedCellId, true);
    }

    @SuppressWarnings("SameParameterValue")
    private String orderSortToWarehouse(String externalId, ApiOrderStatus orderStatus, Long designatedCellId,
                                        boolean inCell) {
        return "{\"externalId\":\"" + externalId + "\",\"status\":\"" + orderStatus + "\"," +
                (inCell ? "\"cell\":{\"id\":1,\"status\":\"ACTIVE\"}," : "") +
                "\"routeTo\":{" +
                "\"cell\":{\"id\":" + designatedCellId + ",\"status\":\"NOT_ACTIVE\"}," +
                "\"warehouse\":{\"name\":\"ООО Ромашка-Склад\"}}}";
    }

    @Test
    @SneakyThrows
    void shipWithOrderShipped() {

        @Value
        class TestData {

            Cell cell;
            OrderLike order1;
            OrderLike order2;
            long routeId;

        }

        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12").build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var order2 = testFactory.createForToday(
                order(sortingCenter, "2").places("21", "22").build()
        ).acceptPlaces("21", "22").sortPlaces("21", "22").get();
        var data = new TestData(
                testFactory.anyOrderPlace(order1).getCell(),
                order1, order2,
                testFactory.findOutgoingCourierRoute(order1).orElseThrow().getId()
        );

        long routableId = testFactory.getRouteIdForSortableFlow(Objects.requireNonNull(data).getRouteId());
        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/routes/" + routableId)
                        .header("Authorization", "OAuth uid-" + UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"один не взял\"," +
                                "\"orderShipped\":\"1\",\"placeShipped\":\"11\",\"cellId\":" + data.getCell().getId() + "}")
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/routes/" + routableId)
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"comment\":\"один не взял\"," +
                                        "\"orderShipped\":\"1\",\"placeShipped\":\"12\",\"cellId\":" + data.getCell().getId() + "}")
                )
                .andExpect(status().isOk());
        getOrder(data.getOrder1(), orderKeep("1"));
        getOrder(data.getOrder2(), orderSortToCourier("2", ApiOrderStatus.OK,
                data.getCell().getId()));
    }

    @Test
    @SneakyThrows
    void shipRouteFromAnotherSortingCenter() {
        var anotherSortingCenter = testFactory.storedSortingCenter(777);
        var order1 = testFactory.createForToday(order(anotherSortingCenter, "1").build())
                .accept().sort().get();

        var courierRoute = testFactory.findOutgoingCourierRoute(order1).orElseThrow();
        var cell = testFactory.determineRouteCell(courierRoute, order1);
        Long courierRoutableId = testFactory.getRouteIdForSortableFlow(courierRoute.getId());
        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/routes/" + courierRoutableId)
                        .header("Authorization", "OAuth uid-" + UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordersShipped\":[\"1\"], " +
                                "\"cellId\":" + Objects.requireNonNull(cell).getId() + " }")
        )
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                        .json("{\"status\":400,\"error\":\""
                                + ScErrorCode.ROUTE_FROM_ANOTHER_SC + "\"}", false));
    }

    @Test
    @SneakyThrows
    void getApiRouteDtoFromAnotherSortingCenter() {
        var anotherSortingCenter = testFactory.storedSortingCenter(777);
        var order1 = testFactory.createForToday(order(anotherSortingCenter, "1").build()).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(order1).orElseThrow();
        caller.getOutgoingRoute(testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                        .json("{\"status\":403,\"error\":\""
                                + ScErrorCode.ROUTE_FROM_ANOTHER_SC + "\"}", false));
    }

    @Test
    @SneakyThrows
    void shipSinglePlace() {
        var order = testFactory.create(
                order(sortingCenter).externalId("o1").places("p1", "p2").build()
        ).acceptPlaces("p1", "p2").keepPlaces("p1", "p2").makeReturn().sortPlaces("p1", "p2").get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.anyOrderPlace(order))
                .orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/routes/" + testFactory.getRouteIdForSortableFlow(route))
                        .header("Authorization", "OAuth uid-" + UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"один не взял\"," +
                                "\"orderShipped\":\"o1\",\"placeShipped\":\"p1\", " +
                                "\"cellId\":" + Objects.requireNonNull(cell).getId() + " }")
        )
                .andExpect(status().isOk());
        var actualOrder = scOrderRepository.findBySortingCenterAndExternalId(sortingCenter, "o1")
                .orElseThrow();
        var place1 = testFactory.orderPlace(order, "p1");
        var place2 = testFactory.orderPlace(order, "p2");
        assertThat(actualOrder.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.SORTED);
    }

    @Test
    @SneakyThrows
    void ship() {
        var place1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept()
                .getPlace();
        var route1 = testFactory.findOutgoingCourierRoute(place1).orElseThrow();
        var designatedCellCourier = testFactory.determineRouteCell(route1, place1);
        var place2 = testFactory.createForToday(order(sortingCenter, "2").build())
                .accept()
                .getPlace();
        var courierRoute = testFactory.findOutgoingCourierRoute(place1).orElseThrow();
        var cell1 = testFactory.determineRouteCell(courierRoute, place1);

        caller.sortableBetaSort(new SortableSortRequestDto(place1, designatedCellCourier))
                .andExpect(status().isOk());

        caller.sortableBetaSort(new SortableSortRequestDto(place2, designatedCellCourier))
                .andExpect(status().isOk());

        Long courierRouteId = testFactory.getRouteIdForSortableFlow(courierRoute.getId());
        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/routes/" + courierRouteId)
                        .header("Authorization", "OAuth uid-" + UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"один не взял\"," +
                                "\"orderShipped\":\"1\", " +
                                "\"placeShipped\":\"1\", " +
                                "\"cellId\":" + Objects.requireNonNull(cell1).getId() + " }")
        )
                .andExpect(status().isOk());

        var place3 = testFactory.createForToday(order(sortingCenter, "3").build())
                .accept()
                .getPlace();

        caller.sortableBetaSort(new SortableSortRequestDto(place3, designatedCellCourier))
                .andExpect(status().isOk());

        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/routes/" + courierRouteId)
                        .header("Authorization", "OAuth uid-" + UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"один не взял\"," +
                                "\"cellId\":" + Objects.requireNonNull(cell1).getId() + " }")
        )
                .andExpect(status().isOk());

        orderCommandService.returnOrdersByIds(List.of(place1.getOrderId()), user, false);

        caller.acceptReturn(new AcceptOrderRequestDto(place1.getExternalId(), place1.getMainPartnerCode()))
                .andExpect(status().isOk());

        place1 = testFactory.getPlace(place1.getId());
        var outgoingWarehouseRoute = testFactory.findOutgoingWarehouseRoute(place1)
                .orElseThrow();
        var designatedCellWarehouse = testFactory.determineRouteCell(outgoingWarehouseRoute, place1);

        caller.sortableBetaSort(new SortableSortRequestDto(place1, designatedCellWarehouse))
                .andExpect(status().isOk());

        var warehouseRoute = testFactory.findOutgoingWarehouseRoute(place1)
                .orElseThrow();
        var warehouseCell = testFactory.determineRouteCell(warehouseRoute, place1);

        Long warehouseRouteId = testFactory.getRouteIdForSortableFlow(warehouseRoute);
        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/routes/" + warehouseRouteId)
                        .header("Authorization", "OAuth uid-" + UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"orderShipped\":\"1\", " +
                                "\"placeShipped\":\"1\", " +
                                "\"cellId\":" + Objects.requireNonNull(warehouseCell).getId() + " }")
        )
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void getOutgoingRouteBaseDtoForCourierCipher() {
        var courier = testFactory.storedCourier(23L);
        var shipmentDate = LocalDate.now(clock);
        var randomNumber = ThreadLocalRandom.current().nextLong();

        var place = testFactory.createForToday(order(sortingCenter, "1").build())
                .updateCourier(courier)
                .accept().sort()
                .getPlace();

        var qrCode = QrCode.of(randomNumber, shipmentDate, courier.getId());
        var cipherText = QRCodeUtil.encryptQrCode(qrCode, "none", "none");
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();

        OutgoingRouteBaseDto expectedResult = new OutgoingRouteBaseDto(
                testFactory.getRouteIdForSortableFlow(route),
                new ApiCourierDto(courier.getId(), courier.getId(), courier.getName(), courier.getDeliveryServiceId()),
                null,
                ApiRouteStatus.NOT_STARTED,
                List.of(TestFactory.cellDto(Objects.requireNonNull(place.getCell()), false, 0)),
                0,
                OutgoingCourierRouteType.COURIER
        );

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/v2/routes/forCourier/cipher")
                                .header("Authorization", "OAuth uid-" + UID)
                                .param("routeType", RouteType.OUTGOING_COURIER.name())
                                .param("cipherId", cipherText)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(expectedResult)));
    }
}
