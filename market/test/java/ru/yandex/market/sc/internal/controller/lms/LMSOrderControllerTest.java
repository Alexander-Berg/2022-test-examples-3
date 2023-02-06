package ru.yandex.market.sc.internal.controller.lms;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.controller.mapper.LMSOrderDtoMapper;
import ru.yandex.market.sc.internal.model.CourierDto;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@SuppressWarnings("unused")
@Slf4j
@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LMSOrderControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    Clock clock;
    @Autowired
    JdbcTemplate jdbcTemplate;
    private final ScOrderRepository orderRepository;
    private final PlaceRepository placeRepository;

    @Test
    @Order(1)
    @SneakyThrows
    void getOrdersTest() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var order = testFactory.createForToday(order(sortingCenter).externalId("1").build()).get();
        mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/LMS/sortingCenter/orders")
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(
                                readResponse("lms_list_orders_response.json"),
                                LmsOrderService.MAXIMUM_RESULT_SET_SIZE, order.getId()
                        ), false));
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/orders/" + order.getId())
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(
                                readResponse("lms_detail_order_response.json"), order.getId()), false));
    }

    private static byte[] getCsvToCourier(List<OrderLike> ordersToCourier) {
        return (headerToCourier + ordersToCourier.stream()
                .map(order -> String.format("%s,%d,%d", order.getExternalId(),
                        order.getSortingCenter().getId(), order.getCourier().getId()))
                .collect(Collectors.joining("\n"))).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] getCsvToWarehouse(List<OrderLike> ordersToWarehouse) {
        return (headerToWarehouse + ordersToWarehouse.stream()
                .map(order -> String.format("%s,%d", order.getExternalId(), order.getSortingCenter().getId()))
                .collect(Collectors.joining("\n"))).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @SneakyThrows
    void returnToBuffer() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var order = testFactory.createForToday(order(sortingCenter)
                .externalId("1")
                .build()).accept().sort().get();

        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/LMS/sortingCenter/returnToBuffer")
                        .contentType("application/json;charset=UTF-8")
                        .content(getIds(order.getId()))
        )
                .andExpect(status().is2xxSuccessful());
        order = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    @SneakyThrows
    void returnToBufferMultiPlaceWhenOrderSorted() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var order = testFactory.createForToday(order(sortingCenter, "1").places("1", "2").build())
                .acceptPlaces("1").sortPlaces("1").get();
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/LMS/sortingCenter/returnToBuffer")
                        .contentType("application/json;charset=UTF-8")
                        .content(getIds(order.getId()))
        )
                .andExpect(status().is2xxSuccessful());
        order = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
        placeRepository.findAllByOrderIdOrderById(order.getId())
                .forEach(place -> {
                    if (place.getYandexId().equals("1")) {
                        assertThat(place.getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
                    } else {
                        assertThat(place.getStatus()).isEqualTo(PlaceStatus.CREATED);
                    }
                    assertThat(place.getCell()).isNull();
                });
    }

    @Test
    @SneakyThrows
    void returnToBufferMultiPlaceWhenSomePlacesNotOnSc() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var order = testFactory.createForToday(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT)
                .places("1", "2", "3", "4")
                .externalId("1")
                .build()).acceptPlaces("2", "3", "4")
                .sortPlaces("3", "4")
                .shipPlaces("4")
                .get();
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/LMS/sortingCenter/returnToBuffer")
                        .contentType("application/json;charset=UTF-8")
                        .content(getIds(order.getId()))
        )
                .andExpect(status().is2xxSuccessful());
        order = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        placeRepository.findAllByOrderIdOrderById(order.getId())
                .forEach(place -> {
                    var status = place.getStatus();
                    switch (place.getMainPartnerCode()) {
                        case "1" -> assertThat(status).isEqualTo(PlaceStatus.CREATED);
                        case "2", "3" -> assertThat(status).isEqualTo(PlaceStatus.ACCEPTED);
                        case "4" -> assertThat(status).isEqualTo(PlaceStatus.SHIPPED);
                        default -> throw new IllegalStateException();
                    }
                    assertThat(place.getCell()).isNull();
                    assertThat(place.getLot()).isNull();
                });
    }

    @Test
    @SneakyThrows
    void shipToCourier() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var order = testFactory.createForToday(order(sortingCenter)

                .externalId("1")
                .build()).get();
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/LMS/sortingCenter/shipToCourier")
                        .contentType("application/json;charset=UTF-8")
                        .content(getIds(order.getId()))
        )
                .andExpect(status().is2xxSuccessful());
        order = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
    }

    @Test
    @SneakyThrows
    void shipToCourierWhenCourierInHistory() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var order = testFactory.createForToday(order(sortingCenter)
                .externalId("1")
                .build()).get();
        var courier2 = testFactory.storedCourier(2L);
        var courier3 = testFactory.storedCourier(3L);
        var order2 = testFactory.createForToday(order(sortingCenter)
                .externalId("2")
                .build()).updateCourier(courier3).accept().get();
        jdbcTemplate.update("INSERT INTO order_ff_status_history (created_at, updated_at," +
                        " order_update_time, order_id, ff_status, courier)" +
                        "values (?::timestamp, ?::timestamp, ?::timestamp, ?, ?, ?)",
                "2020-11-11 16:23:22.528040", "2020-11-11 16:23:22.528040", "2020-11-11 16:23:22.528040",
                order.getId(), ScOrderFFStatus.ORDER_CREATED_FF.name(), courier2.getId());
        jdbcTemplate.update("INSERT INTO order_ff_status_history (created_at, updated_at," +
                        " order_update_time, order_id, ff_status, courier)" +
                        "values (?::timestamp, ?::timestamp, ?::timestamp, ?, ?, ?)",
                "2020-11-11 16:23:22.528040", "2020-11-11 16:23:22.528040", "2020-11-11 16:23:22.528040",
                order.getId(), ScOrderFFStatus.ORDER_CREATED_FF.name(), courier3.getId());
        jdbcTemplate.update("INSERT INTO order_ff_status_history (created_at, updated_at," +
                        " order_update_time, order_id, ff_status, courier)" +
                        "values (?::timestamp, ?::timestamp, ?::timestamp, ?, ?, ?)",
                "2020-11-11 16:23:22.528040", "2020-11-11 16:23:22.528040", "2020-11-11 16:23:22.528040",
                order.getId(), ScOrderFFStatus.ORDER_CREATED_FF.name(), null);
        jdbcTemplate.update("Update orders set courier = null where id = ?", order.getId());
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/LMS/sortingCenter/shipToCourier")
                        .contentType("application/json;charset=UTF-8")
                        .content(getIds(order.getId()))
        )
                .andExpect(status().is2xxSuccessful());
        order = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(order.getCourier()).isEqualTo(courier3);
    }

    @Test
    @SneakyThrows
    void shipWhenNoCourier() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var order = testFactory.createForToday(order(sortingCenter)
                .externalId("1")
                .build()).get();
        jdbcTemplate.update("Update orders set courier = null where id = ?", order.getId());
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/LMS/sortingCenter/shipToCourier")
                        .contentType("application/json;charset=UTF-8")
                        .content(getIds(order.getId()))
        )
                .andExpect(status().is4xxClientError());
        order = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_CREATED_FF);
    }

    @Test
    @SneakyThrows
    void shipWithExternalCourierWhenNoCourier() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var order = testFactory.createForToday(order(sortingCenter)
                .externalId("1")
                .build()).get();
        var courier = Objects.requireNonNull(order.getCourier());
        jdbcTemplate.update("Update orders set courier = null where id = ?", order.getId());
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/LMS/sortingCenter/shipToCourier")
                        .contentType("application/json;charset=UTF-8")
                        .content(
                                "{\"ids\": [" +
                                        order.getId() +
                                        "]," +
                                        "\"courierId\":" + courier.getId() +
                                        "}"
                        )
        )
                .andExpect(status().is2xxSuccessful());
        order = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(order.getCourier()).isEqualTo(courier);
    }

    @Test
    @SneakyThrows
    void shipToCourierMultiPlaceOrder() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var order = testFactory.createForToday(order(sortingCenter)
                .places("1", "2")
                .externalId("1")
                .build())
                .get();
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/LMS/sortingCenter/shipToCourier")
                        .contentType("application/json;charset=UTF-8")
                        .content(getIds(order.getId()))
        )
                .andExpect(status().is2xxSuccessful());
        order = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        placeRepository.findAllByOrderIdOrderById(order.getId()).forEach(place -> {
            assertThat(place.getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        });
    }

    @Test
    @SneakyThrows
    void returnToWarehouse() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var order = testFactory.createForToday(order(sortingCenter)
                .places("1", "2")
                .externalId("1")
                .build())
                .cancel()
                .get();
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/LMS/sortingCenter/returnToWarehouse")
                        .contentType("application/json;charset=UTF-8")
                        .content(getIds(order.getId()))
        )
                .andExpect(status().is2xxSuccessful());
        order = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
    }

    @Test
    @SneakyThrows
    void returnToWarehouseMultiPlaceOrder() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var order = testFactory.createForToday(order(sortingCenter)
                .externalId("1")
                .build()).get();
        testFactory.cancelOrder(order.getId());
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/LMS/sortingCenter/returnToWarehouse")
                        .contentType("application/json;charset=UTF-8")
                        .content(getIds(order.getId()))
        )
                .andExpect(status().is2xxSuccessful());
        order = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        placeRepository.findAllByOrderIdOrderById(order.getId()).forEach(place -> {
            assertThat(place.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        });
    }

    @Test
    @SneakyThrows
    void returnToWarehouseAndAddToRegistry() {
        testFactory.setConfiguration(ConfigurationProperties.USE_COUNTER_IN_REGISTRY_DOCUMENT_ID, true);
        testFactory.setConfiguration(ConfigurationProperties.CREATE_REGISTRY_FOR_LMS_WAREHOUSE_RETURN, true);

        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var user = testFactory.storedUser(sortingCenter, 100L);

        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.RETURN_OUTBOUND_ENABLED, true);

        var courierDto = testFactory.defaultCourier();
        var warehouse = testFactory.storedWarehouse("10001700279");
        testFactory.storedDeliveryService("ds_for_client_return", sortingCenter.getId(), true);
        var order1 = testFactory.createClientReturnForToday(sortingCenter, courierDto, "VOZVRAT_TAR_1")
                .accept().get();
        var cell = testFactory.findRouteCell(testFactory.findOutgoingRoute(order1).get(), order1).get();
        var lot = testFactory.storedLot(sortingCenter, cell, LotStatus.PROCESSING);

        var route = testFactory.findOutgoingRoute(order1).get();

        testFactory.sortOrderToLot(order1, lot, user);
        testFactory.prepareToShipLot(lot);

        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.ORDERS_RETURN)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(order1.getWarehouseReturnYandexId().get())
                .build()
        );
        mockMvc.perform(
                MockMvcRequestBuilders.put("/internal/partners/{scId}/lots/{routeId}",
                        sortingCenter.getPartnerId(),
                        testFactory.getRouteIdForSortableFlow(route))
        );
        order1 = orderRepository.findById(order1.getId()).orElseThrow();
        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);

        createReturnOrderAndShipByLms(sortingCenter, "VOZVRAT_TAR_2", courierDto);
        createReturnOrderAndShipByLms(sortingCenter, "VOZVRAT_TAR_3", courierDto);

        var registries = testFactory.getRegistryByOutboundId(outbound.getId());
        assertThat(registries).hasSize(3);

        assertThat(registries.stream()).allMatch(r -> r.getType() == RegistryType.FACTUAL_DELIVERED_ORDERS_RETURN);

        var registryDocumentIdList = registries.stream()
                .map(Registry::getDocumentId)
                .toList();
        var documentId = testFactory.getRouteIdForSortableFlow(route) + "-3"; // RouteDocumentType.ONLY_CLIENT_RETURNS
        assertThat(registryDocumentIdList).containsExactlyInAnyOrder(documentId, documentId + "-1", documentId + "-2");
    }

    private void createReturnOrderAndShipByLms(SortingCenter sortingCenter, String externalId, CourierDto courierDto)
            throws Exception {
        var order = testFactory.createClientReturnForToday(sortingCenter, courierDto, externalId)
                .accept().get();
        testFactory.cancelOrder(order.getId());
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/LMS/sortingCenter/returnToWarehouse")
                        .contentType("application/json;charset=UTF-8")
                        .content(getIds(order.getId()))
        ).andExpect(status().is2xxSuccessful());
        order = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        placeRepository.findAllByOrderIdOrderById(order.getId()).forEach(place ->
                assertThat(place.getStatus()).isEqualTo(PlaceStatus.RETURNED)
        );
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
    }

    @Test
    @SneakyThrows
    void singleShipToCourier() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var order = testFactory.createForToday(order(sortingCenter)
                .externalId("1")
                .build()).get();
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/LMS/sortingCenter/orders")
                        .contentType("application/json;charset=UTF-8")
                        .content(getJsonOrdersToShip(List.of(order)))
        )
                .andExpect(status().is2xxSuccessful());
        order = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
    }

    @Test
    @SneakyThrows
    void multipleShipToCourier() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var orders = generateListOrders(10, 0, sortingCenter);
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/LMS/sortingCenter/orders")
                        .contentType("application/json;charset=UTF-8")
                        .content(getJsonOrdersToShip(orders))
        )
                .andExpect(status().is2xxSuccessful());

        orders.forEach(order -> {
            order = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        });
    }

    @Test
    @SneakyThrows
    void singleReturnToWarehouse() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var order = testFactory.createForToday(order(sortingCenter)
                .externalId("1")
                .build()).cancel().get();
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/LMS/sortingCenter/orders")
                                .contentType("application/json;charset=UTF-8")
                                .content(getJsonOrdersToWarehouse(List.of(order)))
                )
                .andExpect(status().is2xxSuccessful());
        order = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
    }

    @Test
    @SneakyThrows
    void ordersPageExternalIdFilter() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var ordersToCouriers = generateListOrders(10, 0, sortingCenter);
        var ordersToWarehouse = generateListOrders(10, 10, sortingCenter);
        String contentAsString = mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/LMS/sortingCenter/orders")
                                .param("externalId", ordersToCouriers.get(0).getExternalId())
                                .param("sort", "externalId,desc")
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(
                        jsonPath("$.items[0]['values']['externalId']", is(ordersToCouriers.get(0).getExternalId()))
                )
                .andExpect(
                        jsonPath("$.items", hasSize(1))
                )
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @SneakyThrows
    void multipleReturnToWarehouse() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var orders = generateListOrders(10, 0, sortingCenter);
        orders.forEach(o -> testFactory.cancelOrder(o.getId()));
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/LMS/sortingCenter/orders")
                        .contentType("application/json;charset=UTF-8")
                                .content(getJsonOrdersToWarehouse(orders))
                )
                .andExpect(status().is2xxSuccessful());

        orders.forEach(order -> {
            order = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        });
    }

    @Test
    @SneakyThrows
    void noResultFindByIdTest() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var orders = generateListOrders(10, 0, sortingCenter);

        long ORDER_ID_NOT_IN_DB = 1_000_000_000;

        LMSOrderDtoMapper lmsOrderDtoMapper = new LMSOrderDtoMapper();
        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/LMS/sortingCenter/orders/" + ORDER_ID_NOT_IN_DB)
        ).andExpect(status().is4xxClientError());
    }

    @SneakyThrows
    private String readResponse(String file) {
        return IOUtils.toString(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                file
                        )
                ),
                StandardCharsets.UTF_8
        );
    }

    private String getIds(Long id) {
        return "{\"ids\": [" +
                id +
                "]}";
    }

    @Test
    @SneakyThrows
    void multipleShipAndReturn() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var ordersToCouriers = generateListOrders(10, 0, sortingCenter);
        var ordersToWarehouse = generateListOrders(10, 10, sortingCenter);
        ordersToWarehouse.forEach(o -> testFactory.cancelOrder(o.getId()));
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/LMS/sortingCenter/orders")
                        .contentType("application/json;charset=UTF-8")
                        .content(getJsonShipAndReturn(ordersToCouriers, ordersToWarehouse))
        )
                .andExpect(status().is2xxSuccessful());

        ordersToCouriers.forEach(order -> {
            order = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        });

        ordersToWarehouse.forEach(order -> {
            order = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        });
    }

    @Test
    @SneakyThrows
    void ordersPagePagination() {
        var sortingCenter = testFactory.storedSortingCenter(12L, "Новый СЦ");
        var ordersToCouriers = generateListOrders(10, 0, sortingCenter);
        var ordersToWarehouse = generateListOrders(10, 10, sortingCenter);

        final int PAGE_SIZE = 3;
        LMSOrderDtoMapper lmsOrderDtoMapper = new LMSOrderDtoMapper();
        List<OrderLike> scOrders = Stream.of(ordersToCouriers, ordersToWarehouse)
                .flatMap(List::stream)
                .sorted(Comparator.comparing(OrderLike::getExternalId).reversed())
                .skip(PAGE_SIZE)
                .limit(PAGE_SIZE)
                .toList();
        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/LMS/sortingCenter/orders")
                        .param("page", "1")
                        .param("size", String.valueOf(PAGE_SIZE))
                        .param("sort", "externalId,desc")
        ).andExpect(status().is2xxSuccessful());
        for (int i = 0; i < PAGE_SIZE; i++) {
            String contentAsString = resultActions.andReturn().getResponse().getContentAsString();
            resultActions.andExpect(
                    jsonPath("$.items[" + i + "]['values']['externalId']", is(scOrders.get(i).getExternalId()))
            );
        }
        resultActions.andExpect(jsonPath("$.items", hasSize(PAGE_SIZE)));
    }

    private String getJsonOrdersToShip(List<OrderLike> orders) {
        var shipToCourier = getCsvToCourier(orders);
        return "{\"ordersToCourierFile\": \"" +
                Base64.getEncoder().encodeToString(shipToCourier) +
                "\"}";
    }

    private final static String headerToCourier = "id,scId,courierId\n";

    private String getJsonOrdersToWarehouse(List<OrderLike> orders) {
        var returnToWarehouse = getCsvToWarehouse(orders);
        return "{\"ordersToWarehouseFile\": \"" +
                Base64.getEncoder().encodeToString(returnToWarehouse) +
                "\"}";
    }

    private final static String headerToWarehouse = "id,scId\n";

    private String getJsonShipAndReturn(List<OrderLike> ordersToCourier, List<OrderLike> ordersToWarehouse) {
        var shipToCourier = getCsvToCourier(ordersToCourier);
        var returnToWarehouse = getCsvToWarehouse(ordersToWarehouse);
        return "{\"ordersToCourierFile\": \"" +
                Base64.getEncoder().encodeToString(shipToCourier) +
                "\", " +
                "\"ordersToWarehouseFile\": \"" +
                Base64.getEncoder().encodeToString(returnToWarehouse) +
                "\"}";
    }

    private List<OrderLike> generateListOrders(int count, int start, SortingCenter sortingCenter) {
        var orders = new ArrayList<OrderLike>();
        for (int i = start; i < start + count; i++) {
            orders.add(testFactory.createForToday(order(sortingCenter)
                    .externalId(Integer.toString(i))
                    .build()).get());
        }

        return orders;
    }

}
