package ru.yandex.market.sc.internal.controller.partner;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.route.RouteDtoMapper;
import ru.yandex.market.sc.core.domain.route.model.RouteDocumentType;
import ru.yandex.market.sc.core.domain.route.model.RouteType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.model.CourierDto;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PartnerRouteControllerTest {

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    Cell cell;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        cell = testFactory.storedCell(sortingCenter);
        user = testFactory.storedUser(sortingCenter, 123L);
        testFactory.setupMockClockToSystemTime(clock);
    }

    @Test
    @SneakyThrows
    void getMainInfoRoutesCreated() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).get();
        checkOneRoute(order, order.getCourier());
    }

    @Test
    @SneakyThrows
    void getMainInfoRoutesAccepted() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().get();
        checkOneRoute(order, order.getCourier());
    }

    @Test
    @SneakyThrows
    void getMainInfoRoutesSorted() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort().get();
        checkOneRoute(order, order.getCourier());
    }

    @Test
    @SneakyThrows
    void getMainInfoRoutesShipped() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().get();
        checkOneRoute(order, order.getCourier());
    }

    @Test
    @SneakyThrows
    void getMainInfoRoutesKeep() {
        OrderLike order = testFactory.
                create(order(sortingCenter).externalId("o0").build())
                .accept()
                .keep()
                .get();
        String date = "date=" + order.getIncomingRouteDate() + "&";
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes/mainInfo?" + date + "&type=INCOMING_WAREHOUSE")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"content\":[{\"date\":\"" + order.getIncomingRouteDate() + "\",\"type\":\"INCOMING_WAREHOUSE\"," +
                        "\"transferActTypes\":[\"ALL\"]," +
                        "\"warehouse\":{\"id\":" + order.getWarehouseFrom().getId() + "," +
                        "\"name\":\"ООО Ромашка-Склад\"}}" +
                        "],\"totalPages\":1,\"totalElements\":1}", false));
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes/mainInfo?" + date + "&type=OUTGOING_COURIER")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"content\":[],\"totalPages\":0,\"totalElements\":0}", false));
    }

    @SuppressWarnings("unused")
    @Test
    @SneakyThrows
    void getMainInfoRoutesAllStatuses() {
        LocalDate date = LocalDate.now(clock);
        var courier = testFactory.storedCourier();
        OrderLike order0 = testFactory.
                create(order(sortingCenter).externalId("o0").build())
                .updateShipmentDate(date)
                .cancel()
                .get();
        OrderLike order1 = testFactory.
                create(order(sortingCenter).externalId("o1").build())
                .updateShipmentDate(date)
                .updateCourier(courier)
                .get();
        OrderLike order2 = testFactory.
                create(order(sortingCenter).externalId("o2").build())
                .updateShipmentDate(date)
                .updateCourier(courier)
                .accept()
                .get();
        OrderLike order3 = testFactory.
                create(order(sortingCenter).externalId("o3").build())
                .updateShipmentDate(date)
                .updateCourier(courier)
                .accept()
                .sort()
                .get();
        OrderLike order4 = testFactory.
                create(order(sortingCenter).externalId("o4").build())
                .updateShipmentDate(date)
                .updateCourier(courier)
                .accept()
                .sort()
                .ship()
                .get();
        checkOneRoute(order1, courier);
    }

    @Test
    @SneakyThrows
    void getMainInfoRoutesWhenOrderCanceled() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).cancel().get();
        String date = "date=" + order.getShipmentDate() + "&";
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes/mainInfo?" + date + "&type=INCOMING_WAREHOUSE")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"content\":[],\"totalPages\":0,\"totalElements\":0}", false));
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes/mainInfo?" + date + "&type=OUTGOING_COURIER")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"content\":[],\"totalPages\":0,\"totalElements\":0}", false));
    }

    @Test
    @SneakyThrows
    void getRoutes() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).get();

        String date = "date=" + order.getShipmentDate() + "&";

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?" + date + "&type=INCOMING_WAREHOUSE")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"routes\":[{\"date\":\"" + order.getShipmentDate() + "\",\"type\":\"INCOMING_WAREHOUSE\"," +
                        "\"status\":\"NOT_STARTED\",\"ordersPlanned\":1}]}", false));

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?" + date + "&type=OUTGOING_COURIER")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"routes\":[{\"date\":\"" + order.getShipmentDate() + "\",\"type\":\"OUTGOING_COURIER\"," +
                        "\"status\":\"NOT_STARTED\"," +
                        "\"ordersPlanned\":1,\"courier\":{\"id\":1," +
                        "\"name\":\"Иван Пивовар Таранов\"}}]}", false));
    }

    @Test
    @SneakyThrows
    void getRoutesPage() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).get();
        OrderLike anotherOrder = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .externalId("1243214")
                        .build())
                .updateShipmentDate(LocalDate.now(clock))
                .updateCourier(new CourierDto(2L, "Иван Таран Пивоваров", null, null, null, null, null, false))
                .get();

        assertThat(order.getShipmentDate()).isNotNull();

        var courierShiftStart = LocalTime.of(9, 0);
        testFactory.createCourierShiftForScOrder(order, order.getShipmentDate(), courierShiftStart);
        var courierArrivalTimeFormatted = LocalDateTime.of(order.getShipmentDate(), courierShiftStart)
                .plusMinutes(RouteDtoMapper.COURIER_ARRIVAL_ADVANCE_MINUTES)
                .format(DateTimeFormatter.ISO_DATE_TIME);

        String date = "date=" + order.getShipmentDate() + "&";

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes/page?" + date + "&type=INCOMING_WAREHOUSE&size=1&page=1")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"content\":[]},\"totalPages\":1,\"totalElements\":1}", false));

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes/page?" + date + "&type=OUTGOING_COURIER&size=2&page=0")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"content\":[{\"date\":\"" + order.getShipmentDate() + "\",\"type\":\"OUTGOING_COURIER\"," +
                        "\"status\":\"NOT_STARTED\"," +
                        "\"courierArrivesAt\":\"" + courierArrivalTimeFormatted + "\"," +
                        "\"ordersPlanned\":1,\"courier\":{\"id\":1," +
                        "\"name\":\"Иван Пивовар Таранов\"}}," +
                        "{\"date\":\"" + anotherOrder.getShipmentDate() + "\",\"type\":\"OUTGOING_COURIER\"," +
                        "\"status\":\"NOT_STARTED\"," +
                        "\"ordersPlanned\":1,\"courier\":{\"id\":2," +
                        "\"name\":\"Иван Таран Пивоваров\"}}" +
                        "],\"totalPages\":1,\"totalElements\":2}", false));

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes/page?" + date + "&type=OUTGOING_COURIER&size=1&page=0")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"content\":[{\"date\":\"" + order.getShipmentDate() + "\",\"type\":\"OUTGOING_COURIER\"," +
                        "\"status\":\"NOT_STARTED\"," +
                        "\"ordersPlanned\":1,\"courier\":{\"id\":1," +
                        "\"name\":\"Иван Пивовар Таранов\"}}" +
                        "],\"totalPages\":2,\"totalElements\":2}", false));
    }

    @Test
    @SneakyThrows
    void getRoutesSummary() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).get();

        String date = "date=" + order.getShipmentDate() + "&";

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes/summary?" + date + "&type=INCOMING_WAREHOUSE")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{" +
                        "\"status\":\"NOT_STARTED\",\"ordersPlanned\":1,\"acceptedButNotShipped\":0," +
                        "\"acceptedButNotSorted\":0,\"ordersAccepted\":0,\"ordersInCell\":0,\"ordersLeft\":1," +
                        "\"ordersShipped\":0}]" +
                        "}", true));

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes/summary?" + date + "&type=OUTGOING_COURIER")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{" +
                        "\"status\":\"NOT_STARTED\",\"ordersPlanned\":1,\"acceptedButNotShipped\":0," +
                        "\"acceptedButNotSorted\":0,\"ordersAccepted\":0,\"ordersInCell\":0,\"ordersLeft\":1," +
                        "\"ordersShipped\":0}]" +
                        "}", true));
    }

    @Test
    @SneakyThrows
    void getRoutesSummaryEmptyRoutes() {
        LocalDate now = clock.instant().atZone(clock.getZone()).toLocalDate();
        String date = "date=" + now.toString() + "&";

        for (RouteType type : RouteType.values()) {
            mockMvc.perform(
                            MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                    + "/routes/summary?" + date + "&type=" + type)
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().json("{" +
                            "\"status\":\"NOT_STARTED\",\"ordersPlanned\":0,\"acceptedButNotShipped\":0," +
                            "\"acceptedButNotSorted\":0,\"ordersAccepted\":0,\"ordersInCell\":0,\"ordersLeft\":0," +
                            "\"ordersShipped\":0}]" +
                            "}", true));
        }
    }

    @Test
    @SneakyThrows
    void getIncomingRoutesAfterAcceptReturn() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship()
                .accept().get();
        String date = "date=" + order.getShipmentDate() + "&";
        String types = "type=INCOMING_COURIER";
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?" + date + types)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                                "{\"routes\":[" +
                                "{\"date\":\"" + order.getShipmentDate() + "\",\"type\":\"INCOMING_COURIER\"," +
                                "\"status\":\"FINISHED\"," +
                                "\"ordersPlanned\":1,\"ordersSorted\":0, \"ordersAccepted\":1," +
                                "\"courier\":{\"id\":1," +
                                "\"name\":\"Иван Пивовар Таранов\"}}]}",
                        false));

        types = "type=INCOMING_WAREHOUSE";
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?" + date + types)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                                "{\"routes\":[" +
                                "{\"date\":\"" + order.getShipmentDate() + "\",\"type\":\"INCOMING_WAREHOUSE\"," +
                                "\"status\":\"FINISHED\",\"ordersPlanned\":1," +
                                "\"ordersSorted\": 0, \"ordersAccepted\": 1}]}",
                        false));
    }

    @Test
    @SneakyThrows
    void getReturnRoutes() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().makeReturn().get();
        String date = "date=" + order.getShipmentDate() + "&";

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?" + date + "&type=INCOMING_COURIER")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"routes\":[{\"date\":\"" + order.getShipmentDate() + "\",\"type\":\"INCOMING_COURIER\"," +
                        "\"status\":\"NOT_STARTED\"," +
                        "\"ordersPlanned\":1,\"courier\":{\"id\":1," +
                        "\"name\":\"Иван Пивовар Таранов\"}}]}", false));

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?" + date + "&type=OUTGOING_WAREHOUSE")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"routes\":[{\"date\":\"" + order.getShipmentDate() + "\",\"type\":\"OUTGOING_WAREHOUSE\"," +
                        "\"status\":\"NOT_STARTED\",\"ordersPlanned\":1," +
                        "\"warehouse\":{\"name\":\"ООО Ромашка-Склад\"}}]}", false));
    }

    @Test
    @SneakyThrows
    void tarniyWarehouseNamesDoContainShopIds() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        SortingCenter scTarniy = testFactory.storedSortingCenter(75001L);
        OrderLike order = testFactory.createForToday(
                order(scTarniy)
                        .isClientReturn(true)
                        .build()
        ).get();

        String shopId = order.getWarehouseReturn().getShopId();
        String incorporation = order.getWarehouseReturn().getIncorporation();
        String expectedName = shopId + " " + incorporation;

        String jsonString = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + scTarniy.getPartnerId() +
                                "/routes?date=" + order.getShipmentDate() + "&type=OUTGOING_WAREHOUSE")
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonObject().getAsJsonArray("routes");
        JsonObject route = jsonArray.get(0).getAsJsonObject();
        JsonObject warehouse = route.getAsJsonObject("warehouse");
        String name = warehouse.getAsJsonPrimitive("name").getAsString();
        assertThat(name).isEqualTo(expectedName);
    }

    @Test
    @SneakyThrows
    void getRoutesAfterSort() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort().get();

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?date=" + order.getShipmentDate() + "&type=INCOMING_WAREHOUSE")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"routes\":[{\"date\":\"" + order.getShipmentDate() + "\"," +
                        "\"type\":\"INCOMING_WAREHOUSE\"," +
                        "\"status\":\"FINISHED\",\"ordersPlanned\":1," +
                        "\"ordersSorted\": 0, \"ordersAccepted\": 1}]}", false));

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?date=" + order.getShipmentDate() + "&type=OUTGOING_COURIER")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"routes\":[{\"date\":\"" + order.getShipmentDate() + "\"," +
                        "\"type\":\"OUTGOING_COURIER\"," +
                        "\"status\":\"FINISHED\"," +
                        "\"ordersPlanned\":1,\"ordersSorted\": 1,\"courier\":{\"id\":1," +
                        "\"name\":\"Иван Пивовар Таранов\"}}]}", false));
    }

    @Test
    @SneakyThrows
    public void getApiRoutesOutgoingCourierSortedByFields() {
        var courier1 = testFactory.storedCourier(5, "Абрикосов");
        var order = testFactory.createForToday(order(sortingCenter).externalId("n0").build())
                .updateCourier(courier1).get();
        testFactory.createForToday(order(sortingCenter).externalId("n1").build())
                .updateCourier(courier1).accept().get(); //dispatched 0, acceptedButNotShipped 1, sorted 0

        var courier2 = testFactory.storedCourier(2, "Яблоков");
        testFactory.createForToday(order(sortingCenter).externalId("n2").build())
                .updateCourier(courier2).accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("n3").build())
                .updateCourier(courier2).accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("n4").build())
                .updateCourier(courier2).accept().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("n5").build())
                .updateCourier(courier2).accept().sort().shipPlace("n5").get(); //dispatched 1, acceptedButNotShipped 3, sorted 1

        var courier3 = testFactory.storedCourier(3, "Булочкин");
        testFactory.createForToday(order(sortingCenter).externalId("n6").build())
                .updateCourier(courier3).accept().sort().shipPlace("n6").get(); //dispatched 1, acceptedButNotShipped 0, sorted 0

        var courier4 = testFactory.storedCourier(4, "Кругликов");
        testFactory.createForToday(order(sortingCenter).externalId("n7").build())
                .updateCourier(courier4).accept().sort().shipPlace("n7").get(); //dispatched 1, acceptedButNotShipped 0, sorted 0

        String jsonString = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?date=" + order.getShipmentDate() + "&type=OUTGOING_COURIER&sort=ordersShipped,desc" +
                                "&sort=acceptedButNotShipped,desc&sort=recipientName,asc")
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonArray routesArray = jsonObject.getAsJsonArray("routes");
        assertThat(routesArray.size()).isEqualTo(4);

        var route1 = routesArray.get(0).getAsJsonObject();
        assertThat(route1.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Яблоков");
        assertThat(route1.getAsJsonObject().get("ordersShipped").getAsString()).isEqualTo("1");
        assertThat(route1.getAsJsonObject().get("acceptedButNotShipped").getAsString()).isEqualTo("3");
        var route2 = routesArray.get(1).getAsJsonObject();
        assertThat(route2.getAsJsonObject().get("ordersShipped").getAsString()).isEqualTo("1");
        assertThat(route2.getAsJsonObject().get("acceptedButNotShipped").getAsString()).isEqualTo("0");
        assertThat(route2.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Булочкин");
        var route3 = routesArray.get(2).getAsJsonObject();
        assertThat(route3.getAsJsonObject().get("ordersShipped").getAsString()).isEqualTo("1");
        assertThat(route3.getAsJsonObject().get("acceptedButNotShipped").getAsString()).isEqualTo("0");
        assertThat(route3.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Кругликов");
        var route4 = routesArray.get(3).getAsJsonObject();
        assertThat(route4.getAsJsonObject().get("ordersShipped").getAsString()).isEqualTo("0");
        assertThat(route4.getAsJsonObject().get("acceptedButNotShipped").getAsString()).isEqualTo("1");
        assertThat(route4.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Абрикосов");

        //sort by planned and sorted fields
        jsonString = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?date=" + order.getShipmentDate() + "&type=OUTGOING_COURIER&sort=ordersPlanned,desc" +
                                "&sort=ordersSorted,asc&sort=recipientName,desc")
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        routesArray = jsonObject.getAsJsonArray("routes");
        assertThat(routesArray.size()).isEqualTo(4);
        route1 = routesArray.get(0).getAsJsonObject();
        assertThat(route1.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Яблоков");
        assertThat(route1.getAsJsonObject().get("ordersPlanned").getAsString()).isEqualTo("4");
        assertThat(route1.getAsJsonObject().get("ordersSorted").getAsString()).isEqualTo("1");

        route2 = routesArray.get(1).getAsJsonObject();
        assertThat(route2.getAsJsonObject().get("ordersPlanned").getAsString()).isEqualTo("2");
        assertThat(route2.getAsJsonObject().get("ordersSorted").getAsString()).isEqualTo("0");
        assertThat(route2.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Абрикосов");

        route3 = routesArray.get(2).getAsJsonObject();
        assertThat(route3.getAsJsonObject().get("ordersPlanned").getAsString()).isEqualTo("1");
        assertThat(route3.getAsJsonObject().get("ordersSorted").getAsString()).isEqualTo("0");
        assertThat(route3.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Кругликов");

        route4 = routesArray.get(3).getAsJsonObject();
        assertThat(route4.getAsJsonObject().get("ordersPlanned").getAsString()).isEqualTo("1");
        assertThat(route4.getAsJsonObject().get("ordersSorted").getAsString()).isEqualTo("0");
        assertThat(route4.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Булочкин");

    }

    @Test
    @SneakyThrows
    void getApiRoutesOutgoingCourierSortedByStatus() {
        var newCourier = testFactory.storedCourier(777, "Абрикосов");
        var order = testFactory.createForToday(order(sortingCenter).externalId("n0").build())
                .updateCourier(newCourier).get();
        testFactory.createForToday(order(sortingCenter).externalId("n1").build())
                .updateCourier(newCourier).accept().get();
        var newCourier2 = testFactory.storedCourier(555, "Яблоков");
        testFactory.createForToday(order(sortingCenter).externalId("n2").build())
                .updateCourier(newCourier2).accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("n3").build())
                .updateCourier(newCourier2).accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("n4").build())
                .updateCourier(newCourier2).accept().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("n5").build())
                .updateCourier(newCourier2).accept().sort().shipPlace("n5").get();
        var newCourier3 = testFactory.storedCourier(888, "Булочкин");
        testFactory.createForToday(order(sortingCenter).externalId("n6").build())
                .updateCourier(newCourier3).accept().sort().shipPlace("n6").get();
        var newCourier4 = testFactory.storedCourier(999, "Кругликов");
        testFactory.createForToday(order(sortingCenter).externalId("n7").build())
                .updateCourier(newCourier4).accept().sort().shipPlace("n7").get();
        String jsonString = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?date=" + order.getShipmentDate() + "" +
                                "&type=OUTGOING_COURIER&sort=status,asc&sort=status,desc")
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonArray routesArray = jsonObject.getAsJsonArray("routes");
        assertThat(routesArray.size()).isEqualTo(4);
        var route1 = routesArray.get(0).getAsJsonObject();
        assertThat(route1.getAsJsonObject().get("status").getAsString()).isEqualTo("NOT_STARTED");
        var route2 = routesArray.get(1).getAsJsonObject();
        assertThat(route2.getAsJsonObject().get("status").getAsString()).isEqualTo("IN_PROGRESS");
        var route3 = routesArray.get(2).getAsJsonObject();
        assertThat(route3.getAsJsonObject().get("status").getAsString()).isEqualTo("SHIPPED");
        var route4 = routesArray.get(2).getAsJsonObject();
        assertThat(route4.getAsJsonObject().get("status").getAsString()).isEqualTo("SHIPPED");
        jsonString = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?date=" + order.getShipmentDate() + "&type=OUTGOING_COURIER&status=SHIPPED")
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        routesArray = jsonObject.getAsJsonArray("routes");
        assertThat(routesArray.size()).isEqualTo(2);
        route1 = routesArray.get(0).getAsJsonObject();
        assertThat(route1.getAsJsonObject().get("status").getAsString()).isEqualTo("SHIPPED");
        route2 = routesArray.get(1).getAsJsonObject();
        assertThat(route2.getAsJsonObject().get("status").getAsString()).isEqualTo("SHIPPED");
    }

    @Test
    @SneakyThrows
    void getApiRoutesOutgoingCourierSortedByRecipientName() {
        var order = testFactory.createForToday(order(sortingCenter).externalId("1").build()).get();
        var newCourier = testFactory.storedCourier(777, "Абрикосов");
        testFactory.createForToday(order(sortingCenter).externalId("n1").build())
                .updateCourier(newCourier).accept().get();
        var newCourier2 = testFactory.storedCourier(555, "АбАрикосов");
        testFactory.createForToday(order(sortingCenter).externalId("n2").build())
                .updateCourier(newCourier2).accept().get();
        String date = "date=" + order.getShipmentDate() + "&";
        String jsonString = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?date=" + order.getShipmentDate() + "&type=OUTGOING_COURIER&sort=recipientName,asc")
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonArray routesArray = jsonObject.getAsJsonArray("routes");
        var route1 = routesArray.get(0).getAsJsonObject();
        assertThat(route1.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("АбАрикосов");
        var route2 = routesArray.get(1).getAsJsonObject();
        assertThat(route2.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Абрикосов");
        var route3 = routesArray.get(2).getAsJsonObject();
        assertThat(route3.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Иван Пивовар Таранов");
        //now filter
        jsonString = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?date=" + order.getShipmentDate() + "&type=OUTGOING_COURIER&recipientName=АбариКоСов")
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        routesArray = jsonObject.getAsJsonArray("routes");
        assertThat(routesArray.size()).isEqualTo(1);
        route1 = routesArray.get(0).getAsJsonObject();
        assertThat(route1.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("АбАрикосов");
        jsonString = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?date=" + order.getShipmentDate() + "&type=OUTGOING_COURIER" +
                                "&recipientName=Иван Пивовар Таранов" +
                                "&hasMultiplaceIncompleteInCell=false")
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        routesArray = jsonObject.getAsJsonArray("routes");
        assertThat(routesArray.size()).isEqualTo(1);
        route1 = routesArray.get(0).getAsJsonObject();
        assertThat(route1.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Иван Пивовар Таранов");
        //now filter by recipientName and hasMultiplaceInocomplete
        jsonString = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?" +
                                date + "&type=OUTGOING_COURIER&recipientName=Иван Пивовар Таранов" +
                                "&hasMultiplaceIncompleteInCell=true")
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        routesArray = jsonObject.getAsJsonArray("routes");
        assertThat(routesArray.size()).isEqualTo(0);
    }

    @Test
    @SneakyThrows
    void getApiRoutesOutgoingCourierSortedByHasIncompleteMultiplaceInCell() {
        var firstCourier = testFactory.storedCourier(111, "Абрикосов Борис");
        testFactory.createForToday(order(sortingCenter).externalId("0").build()).updateCourier(firstCourier)
                .cancel().get();
        var order = testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .updateCourier(firstCourier).get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("2").places("o2p1", "o2p2").build())
                .updateCourier(firstCourier)
                .acceptPlaces("o2p1").sortPlaces("o2p1")
                .get(); //этот

        //now orders for another courier without multiplace incomplete in cell
        var newCourier = testFactory.storedCourier(777, "Яблочкин");
        ScOrder scOrder = testFactory.createForToday(order(sortingCenter).externalId("n1").build())
                .updateCourier(newCourier).accept().get();

        var newCourier2 = testFactory.storedCourier(555, "Южин");
        testFactory.createForToday(order(sortingCenter).externalId("k1").build())
                .updateCourier(newCourier2).accept().get();

        //now another courier with multiplace incomplete in cell
        var newCourier3 = testFactory.storedCourier(2, "Булочкин Анатолий");
        testFactory.createForToday(
                        order(sortingCenter).externalId("l1").places("olp1", "olp2").build())
                .updateCourier(newCourier3)
                .acceptPlaces("olp1").sortPlaces("olp1")
                .get(); //этот

        String jsonString = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?date=" + order.getShipmentDate() + "&type=OUTGOING_COURIER" +
                                "&sort=hasMultiplaceIncompleteInCell,desc")
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonArray routesArray = jsonObject.getAsJsonArray("routes");
        var route1 = routesArray.get(0).getAsJsonObject();
        assertThat(route1.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("true");
        assertThat(route1.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("1");
        var route2 = routesArray.get(1).getAsJsonObject();
        assertThat(route2.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("true");
        assertThat(route2.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("1");
        var route3 = routesArray.get(2).getAsJsonObject();
        var route4 = routesArray.get(3).getAsJsonObject();
        assertThat(route3.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("false");
        assertThat(route3.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("0");
        assertThat(route4.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("false");
        assertThat(route4.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("0");

        //sort by asc
        jsonString = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?date=" + order.getShipmentDate() + "&type=OUTGOING_COURIER" +
                                "&sort=hasMultiplaceIncompleteInCell,asc")
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        routesArray = jsonObject.getAsJsonArray("routes");
        route1 = routesArray.get(0).getAsJsonObject();
        assertThat(route1.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("false");
        assertThat(route1.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("0");
        route2 = routesArray.get(1).getAsJsonObject();
        assertThat(route2.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("false");
        assertThat(route2.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("0");
        route3 = routesArray.get(2).getAsJsonObject();
        route4 = routesArray.get(3).getAsJsonObject();
        assertThat(route3.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("true");
        assertThat(route3.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("1");
        assertThat(route4.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("true");
        assertThat(route4.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("1");
        //filter only with multiplaceIncompoleteInCell true
        jsonString = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?date=" + order.getShipmentDate() + "&type=OUTGOING_COURIER" +
                                "&hasMultiplaceIncompleteInCell=true")
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        routesArray = jsonObject.getAsJsonArray("routes");
        assertThat(routesArray.size()).isEqualTo(2);
        route1 = routesArray.get(0).getAsJsonObject();
        assertThat(route1.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("true");
        assertThat(route1.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("1");
        route2 = routesArray.get(1).getAsJsonObject();
        assertThat(route2.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("true");
        assertThat(route2.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("1");
        //filter only with multiplaceIncompoleteInCell true and order by recipientName
        jsonString = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?date=" + order.getShipmentDate() + "&type=OUTGOING_COURIER" +
                                "&hasMultiplaceIncompleteInCell=true" +
                                "&sort=recipientName,asc")
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        routesArray = jsonObject.getAsJsonArray("routes");
        assertThat(routesArray.size()).isEqualTo(2);
        route1 = routesArray.get(0).getAsJsonObject();
        assertThat(route1.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("true");
        assertThat(route1.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("1");
        assertThat(route1.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Абрикосов Борис");

        route2 = routesArray.get(1).getAsJsonObject();
        assertThat(route2.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("true");
        assertThat(route2.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("1");
        assertThat(route2.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Булочкин Анатолий");
        //ordery by multiplaceIncompoleteInCell and order by recipientName
        jsonString = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?date=" + order.getShipmentDate() + "&type=OUTGOING_COURIER" +
                                "&sort=hasMultiplaceIncompleteInCell,desc" +
                                "&sort=recipientName,asc")
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        routesArray = jsonObject.getAsJsonArray("routes");
        assertThat(routesArray.size()).isEqualTo(4);
        route1 = routesArray.get(0).getAsJsonObject();
        assertThat(route1.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("true");
        assertThat(route1.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("1");
        assertThat(route1.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Абрикосов Борис");
        route2 = routesArray.get(1).getAsJsonObject();
        assertThat(route2.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("true");
        assertThat(route2.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("1");
        assertThat(route2.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Булочкин Анатолий");
        route3 = routesArray.get(2).getAsJsonObject();
        route4 = routesArray.get(3).getAsJsonObject();
        assertThat(route3.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("false");
        assertThat(route3.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("0");
        assertThat(route3.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Южин");
        assertThat(route4.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("false");
        assertThat(route4.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("0");
        assertThat(route4.getAsJsonObject().get("courier").getAsJsonObject().get("name")
                .getAsString()).isEqualTo("Яблочкин");
    }

    @Test
    @SneakyThrows
    void getApiRoutesOutgoingCourierSortedByHasIncompleteMultiplaceInCell_2() {
        var firstCourier = testFactory.storedCourier(111, "Абрикосов Борис");
        ScOrder scOrder = testFactory.createForToday(
                        order(sortingCenter).externalId("2").places("o2p1", "o2p2").build())
                .updateCourier(firstCourier)
                .acceptPlaces("o2p1").sortPlaces("o2p1")
                .get();//этот

        //now another courier with multiplace incomplete in cell
        var newCourier3 = testFactory.storedCourier(2, "Булочкин Анатолий");
        testFactory.createForToday(
                        order(sortingCenter).externalId("l1").places("olp1", "olp2").build())
                .updateCourier(newCourier3)
                .acceptPlaces("olp1").sortPlaces("olp1")
                .get(); //этот

        String jsonString = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes?date=" + scOrder.getShipmentDate() + "&type=OUTGOING_COURIER" +
                                "&sort=hasMultiplaceIncompleteInCell,desc")
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonArray routesArray = jsonObject.getAsJsonArray("routes");

        List<JsonElement> routesArray2 = Lists.newArrayList(routesArray);
        routesArray2.sort( (element1, element2) ->
                -Long.compare(element1.getAsJsonObject().get("ordersPlanned").getAsLong(),
                        element2.getAsJsonObject().get("ordersPlanned").getAsLong()));

        var route1 = routesArray.get(0).getAsJsonObject();
        assertThat(route1.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("true");
        assertThat(route1.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("1");
        var route2 = routesArray.get(1).getAsJsonObject();
        assertThat(route2.getAsJsonObject().get("hasMultiplaceIncompleteInCell")
                .getAsString()).isEqualTo("true");
        assertThat(route2.getAsJsonObject().get("multiplaceIncompleteInCell")
                .getAsString()).isEqualTo("1");
    }

    @Test
    @SneakyThrows
    void getTransferAct() {
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("1", "2").build()
        ).cancel().acceptPlaces("1").sortPlaces("1").ship().get();
        testFactory.createForToday(order(sortingCenter, "2").build())
                .cancel().accept().sort().ship().get();
        var route = testFactory.findPossibleOutgoingWarehouseRoute(order1).orElseThrow();
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partners/{scId}/routes/{id}/transferAct/{type}",
                        sortingCenter.getPartnerId(),
                        testFactory.getRouteIdForSortableFlow(route),
                        RouteDocumentType.NORMAL
                )
        ).andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void getReturnRegistryOld() {
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("1", "2").build()
        ).cancel().acceptPlaces("1").sortPlaces("1").ship().get();
        testFactory.createForToday(order(sortingCenter, "2").build())
                .cancel().accept().sort().ship().get();
        var route = testFactory.findPossibleOutgoingWarehouseRoute(order1).orElseThrow();
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partners/{scId}/routes/{id}/returnRegistry" +
                                "?processOnlyDamagedOrders=true",
                        sortingCenter.getPartnerId(),
                        testFactory.getRouteIdForSortableFlow(route)
                )
        ).andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void getReturnRegistry() {
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("1", "2").build()
        ).cancel().acceptPlaces("1").sortPlaces("1").ship().get();
        testFactory.createForToday(order(sortingCenter, "2").build())
                .cancel().accept().sort().ship().get();
        var route = testFactory.findPossibleOutgoingWarehouseRoute(order1).orElseThrow();
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partners/{scId}/routes/{id}/returnRegistry/{type}",
                        sortingCenter.getPartnerId(),
                        testFactory.getRouteIdForSortableFlow(route),
                        RouteDocumentType.NORMAL
                )
        ).andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void getReturnRegistryAll() {
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("1", "2").build()
        ).cancel().acceptPlaces("1").sortPlaces("1").ship().get();
        testFactory.createForToday(order(sortingCenter, "2").build())
                .cancel().accept().sort().ship().get();
        var route = testFactory.findPossibleOutgoingWarehouseRoute(order1).orElseThrow();
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partners/{scId}/routes/{id}/returnRegistry/{type}",
                        sortingCenter.getPartnerId(),
                        testFactory.getRouteIdForSortableFlow(route),
                        RouteDocumentType.ALL
                )
        ).andExpect(status().isOk());
    }

    @Test
    void getReturnPalletLabel() throws Exception {
        var order = testFactory.createForToday(
                        order(sortingCenter).warehouseCanProcessDamagedOrders(true).build()
                )
                .cancel().accept().markOrderAsDamaged().sort().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partners/{scId}/routes/{id}/returnPalletLabel/{type}",
                        sortingCenter.getPartnerId(),
                        testFactory.getRouteIdForSortableFlow(route),
                        RouteDocumentType.ONLY_DAMAGED
                )
        ).andExpect(status().isOk());
    }

    @Test
    void getReturnPalletLabelAll() throws Exception {
        var order = testFactory.createForToday(
                        order(sortingCenter).warehouseCanProcessDamagedOrders(true).build()
                )
                .cancel().accept().markOrderAsDamaged().sort().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partners/{scId}/routes/{id}/returnPalletLabel/{type}",
                        sortingCenter.getPartnerId(),
                        testFactory.getRouteIdForSortableFlow(route),
                        RouteDocumentType.ALL
                )
        ).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Заполнение информации о лотах маршрута")
    @SneakyThrows
    void getApiRoutesFillLotSummaryStatistics() {
        var order = testFactory.createForToday(order(sortingCenter).externalId("1").build()).get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        Cell parentCell = testFactory.determineRouteCell(route, order);
        testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        testFactory.storedLot(sortingCenter, parentCell, LotStatus.PROCESSING);
        testFactory.storedLot(sortingCenter, parentCell, LotStatus.PROCESSING);
        testFactory.storedLot(sortingCenter, parentCell, LotStatus.READY);
        testFactory.storedLot(sortingCenter, parentCell, LotStatus.READY);
        testFactory.storedLot(sortingCenter, parentCell, LotStatus.READY);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(
                "/internal/partners/{partnerId}/routes?date={shipmentDate}&type={routeType}",
                sortingCenter.getPartnerId(),
                order.getShipmentDate(),
                RouteType.OUTGOING_COURIER
        );
        //noinspection deprecation
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.routes", hasSize(1)))
                .andExpect(jsonPath("$.routes.[0].lotsSummaryStats.lotsProcessing", is(2)))
                .andExpect(jsonPath("$.routes.[0].lotsSummaryStats.lotsReady", is(3)));
    }

    @Test
    @SneakyThrows
    void getRouteCategories() {
        for (RouteType type : RouteType.values()) {
            MockHttpServletRequestBuilder request =
                    MockMvcRequestBuilders.get("/internal/partners/{sortingCenterPartnerId}/routes/categories" +
                            "?type={routeType}", sortingCenter.getPartnerId(), type);
            String categories = Stream.of(type.getCategories())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            //noinspection deprecation
            mockMvc.perform(request)
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andExpect(content().json("{categories:" + categories + "}"));
        }
    }

    @SneakyThrows
    private void checkOneRoute(OrderLike order, @Nullable Courier courier) {
        assertThat(courier).isNotNull();
        String date = "date=" + order.getShipmentDate() + "&";
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes/mainInfo?" + date + "&type=INCOMING_WAREHOUSE")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"content\":[{\"date\":\"" + order.getShipmentDate() + "\",\"type\":\"INCOMING_WAREHOUSE\"," +
                        "\"transferActTypes\":[\"ALL\"]," +
                        "\"warehouse\":{\"id\":" + order.getWarehouseFrom().getId() + "," +
                        "\"name\":\"ООО Ромашка-Склад\"}}" +
                        "],\"totalPages\":1,\"totalElements\":1}", false));
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/routes/mainInfo?" + date + "&type=OUTGOING_COURIER")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"content\":[{\"date\":\"" + order.getShipmentDate() + "\",\"type\":\"OUTGOING_COURIER\"," +
                        "\"transferActTypes\":[\"ALL\"]," +
                        "\"courier\":{\"id\": " + courier.getId() + "," +
                        "\"name\":\"" + courier.getName() + "\"}}" +
                        "],\"totalPages\":1,\"totalElements\":1}", false));
    }

}
