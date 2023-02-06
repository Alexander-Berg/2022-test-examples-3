package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.cell.model.ApiCellDto;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.model.BeforeShipOrderDto;
import ru.yandex.market.sc.core.domain.order.model.ResortReasonCode;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.exception.ScException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ResortOnShipTest extends BaseApiControllerTest {

    private final ObjectMapper objectMapper;

    @MockBean
    private Clock clock;

    private SortingCenter sortingCenter;
    private TestControllerCaller caller;
    @Autowired
    private PlaceRepository placeRepository;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.storedUser(sortingCenter, UID);
        testFactory.setupMockClock(clock);
        testFactory.increaseScOrderId();
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "true");
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DROPOFF_CAN_PROCESS_CANCEL_ON_SORTED_ORDER, "true");
        caller = TestControllerCaller.createCaller(mockMvc, UID);
    }

    @DisplayName("СРЕДНЯЯ МИЛЯ. Нельзя отменить отсортированный заказ")
    @Test
    @SneakyThrows
    void cantCancelOrderSortedOrderOnMiddleMile() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "false");
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DROPOFF_CAN_PROCESS_CANCEL_ON_SORTED_ORDER, "true");
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2", "p3").get();
        assertThrows(ScException.class, () -> testFactory.cancelOrder(order.getId()));
    }

    @DisplayName("ДРОПОФФ. Нельзя отменить отсортированный заказ, когда свойство выключено.")
    @Test
    @SneakyThrows
    void cantCancelOrderSortedOrderOnDropoffWhenPropertyISOff() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DROPOFF_CAN_PROCESS_CANCEL_ON_SORTED_ORDER, "false");
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2", "p3").get();
        assertThrows(ScException.class, () -> testFactory.cancelOrder(order.getId()));
    }

    @DisplayName("ДРОПОФФ. Не нужно пересортировывать в возратную ячейку на возвратом потоке.")
    @Test
    @SneakyThrows
    void noNeedToResortToReturnCellOnReturnFlow() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .cancel()
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2", "p3").get();
        var returnRoute = testFactory.findOutgoingWarehouseRoute(order.getId()).orElseThrow().allowReading();
        var returnCell = returnRoute.getCells(LocalDate.now(clock)).get(0);
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .multiPlace(true)
                .placeExternalId("p1")
                .needResort(false)
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .availableCells(List.of())
                .build();
        Long returnRouteId = testFactory.getRouteIdForSortableFlow(returnRoute.getId());
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + returnRouteId +
                                        "&externalId=" + order.getExternalId() + "&placeExternalId=" + "p1")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
        assertDoesNotThrow(() -> testFactory.shipPlace(order, "p1"));
        var place1 = testFactory.orderPlaces(order.getId()).stream()
                .filter(p -> p.getMainPartnerCode().equals("p1")).findFirst().orElseThrow();
        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .multiPlace(true)
                .placeExternalId("p2")
                .needResort(false)
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .availableCells(List.of())
                .build();
        response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + returnRouteId +
                                        "&externalId=" + order.getExternalId() + "&placeExternalId=" + "p2")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("СРЕДНЯЯ МИЛЯ. Нельзя отменить заказ")
    @Test
    @SneakyThrows
    void cantCancelOrderWhenPropertyIsOff() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "false");
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2", "p3").get();
        assertThrows(ScException.class, () -> testFactory.cancelOrder(order.getId()));
    }

    @DisplayName("Дропофф. Для одноместного, отмененного заказа с ДО требуется пересортировка на возврат")
    @Test
    @SneakyThrows
    void onOrderResortNeedToResortWhenOrderCanceledOnDO() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .accept().sort().get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        testFactory.cancelOrder(order.getId());
        var returnRoute = testFactory.getRoutable(testFactory.findOutgoingWarehouseRoute(order.getId())
                                                                                            .orElseThrow());
        var returnCell = routeSoMigrationHelper.getCells(returnRoute, LocalDate.now(clock)).get(0);
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .multiPlace(false)
                .needResort(true)
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .resortReasonCode(ResortReasonCode.RESORT_CANCELED)
                .availableCells(List.of(new ApiCellDto(returnCell)))
                .build();
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=" + order.getExternalId()
                                )
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("Дропофф. Для многоместного, отмененного заказа. На ДО требуется пересортировка на возврат")
    @Test
    @SneakyThrows
    void onOrderResortNeedToResortWhenMultiPlaceOrderCanceledOnDO() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2", "p3").get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        testFactory.cancelOrder(order.getId());
        var returnRoute = testFactory.findOutgoingWarehouseRoute(order.getId()).orElseThrow().allowReading();
        var returnCell = returnRoute.getCells(LocalDate.now(clock)).get(0);
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .placeExternalId("p1")
                .multiPlace(true)
                .needResort(true)
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .resortReasonCode(ResortReasonCode.RESORT_CANCELED)
                .availableCells(List.of(new ApiCellDto(returnCell)))
                .build();
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=" + order.getExternalId() + "&placeExternalId=" + "p1")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("Дропофф. Для многоместного, отмененного заказа. На ДО требуется пересортировка на возврат. " +
            "Проверяем, что заказ автоматически разместился в ячейке")
    @Test
    @SneakyThrows
    void onOrderResortNeedToResortWhenMultiPlaceOrderCanceledOnDOCheckAutoSorted() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ACCEPT_AND_SORT_RETURN_ON_DROPOFF, true);
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2", "p3").get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        testFactory.cancelOrder(order.getId());
        var returnRoute = testFactory.findOutgoingWarehouseRoute(order.getId()).orElseThrow().allowReading();
        var returnCell = returnRoute.getCells(LocalDate.now(clock)).get(0);
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .placeExternalId("p1")
                .multiPlace(true)
                .needResort(false)
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .availableCells(List.of())
                .build();
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=" + order.getExternalId() + "&placeExternalId=" + "p1")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
        var place1 = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "p1").get();
        assertThat(place1.getCell()).isNotNull();
        assertThat(place1.getCell().getId()).isEqualTo(returnCell.getId());
    }

    @DisplayName("Дропофф. Для многоместного, отмененного заказа. На ДО НЕ требуется пересортировка на возврат. " +
            "Сканируют неотсортированную посылку.")
    @Test
    @SneakyThrows
    void onOrderResortNeedToResortWhenMultiPlaceOrderCanceleScanNotSortedPlacedOnDO() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2").get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        testFactory.cancelOrder(order.getId());
        var returnRoute = testFactory.findOutgoingWarehouseRoute(order.getId()).orElseThrow().allowReading();
        var returnCell = returnRoute.getCells(LocalDate.now(clock)).get(0);
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .placeExternalId("p3")
                .multiPlace(true)
                .needResort(false)
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .availableCells(List.of())
                .build();
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=" + order.getExternalId() + "&placeExternalId=" + "p3")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("Дропофф. Для многоместного, отмененного заказа. На ДО НЕ требуется пересортировка на возврат. " +
            "Сканируют посылку с другого маршрута (хотя на другом маршруте ее нужно пересортировать на возрвта).")
    @Test
    @SneakyThrows
    void onOrderResortNeedToResortWhenMultiPlaceOrderCanceleScanOrderFromAnotherRouteOnDO() {
        var anotherDeliveryService =
                testFactory.storedDeliveryService("777", sortingCenter.getId(), false);
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2").get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        testFactory.cancelOrder(order.getId());

        var order2 = testFactory
                .create(order(sortingCenter).externalId("2").places("p21", "p22", "p23")
                        .deliveryDate(LocalDate.now(clock))
                        .deliveryService(anotherDeliveryService)
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .acceptPlaces("p21", "p22", "p23").sortPlaces("p21", "p22").get();
        testFactory.cancelOrder(order2.getId());

        var returnRoute = testFactory.getRoutable(testFactory.findOutgoingWarehouseRoute(order.getId())
                                                                                                .orElseThrow());
        var returnCell = routeSoMigrationHelper.getCells(returnRoute, LocalDate.now(clock)).get(0);
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order2.getExternalId())
                .placeExternalId("p22")
                .multiPlace(true)
                .needResort(false)
                .availableCells(List.of())
                .build();
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=" + order2.getExternalId() + "&placeExternalId=" + "p22")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("Дропофф. Для многоместного заказа, отсортированного. На ДО НЕ требуется пересортировка на возврат." +
            "Заказ не отменен. Все посылки отсортированны.")
    @Test
    @SneakyThrows
    void onOrderResortNoNeedToResortMultiPlaceWhenOrderCanceledOnDo() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2", "p3").get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .multiPlace(true)
                .placeExternalId("p1")
                .deliveryServiceName(order.getDeliveryService().getName())
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .needResort(false)
                .availableCells(List.of())
                .build();
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=" + order.getExternalId() + "&placeExternalId=" + "p1")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("Дропофф. Для многоместного заказа, частичного отгруженного. " +
            "На ДО НЕ требуется пересортировка на возврат. Заказ не отменен")
    @Test
    @SneakyThrows
    void onOrderResortNoNeedToResortPartiallyShipedNoCanceled() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2", "p3").shipPlaces("p1", "p2").get();
        var route = testFactory.findOutgoingRoute(testFactory.orderPlace(order, "p3"))
                .orElseThrow();
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .multiPlace(true)
                .placeExternalId("p3")
                .deliveryServiceName(order.getDeliveryService().getName())
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .needResort(false)
                .availableCells(List.of())
                .build();
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=" + order.getExternalId() + "&placeExternalId=" + "p3")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("Дропофф. Для многоместного заказа, частичного отгруженного. " +
            "На ДО требуется пересортировка на возврат. Заказ отменен после частичной отгрузки")
    @Test
    @SneakyThrows
    void onOrderResortNoNeedToResortPartiallyShippedCanceledAfterPartialShip() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2", "p3").shipPlaces("p1", "p2").get();
        var route = testFactory.findOutgoingRoute(testFactory.orderPlace(order, "p3"))
                .orElseThrow();
        testFactory.cancelOrder(order.getId());
        var returnRoute = testFactory.findOutgoingWarehouseRoute(order.getId()).orElseThrow().allowReading();
        var returnCell = returnRoute.getCells(LocalDate.now(clock)).get(0);
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .multiPlace(true)
                .placeExternalId("p3")
                .needResort(true)
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .resortReasonCode(ResortReasonCode.RESORT_CANCELED)
                .availableCells(List.of(new ApiCellDto(returnCell)))
                .build();
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=" + order.getExternalId() + "&placeExternalId=" + "p3")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("Дропофф. Для одноместного заказа, отсортированного на ДО НЕ требуется пересортировка на возврат")
    @Test
    @SneakyThrows
    void onOrderResortNoNeedToResortWhenOrderCanceledOnDo() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .accept().sort().get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .multiPlace(false)
                .needResort(false)
                .deliveryServiceName(order.getDeliveryService().getName())
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .availableCells(List.of())
                .build();
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=" + order.getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("ПОСЛЕДНЯЯ МИЛЯ. Для многоместного заказа на последней миле необходимо пересортировать " +
            "в хранение неполный многоместный. Обычный СЦ.")
    @Test
    @SneakyThrows
    void onOrderResortMultiPlaceKeepPartialSorted() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "false");
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DROPOFF_CAN_PROCESS_CANCEL_ON_SORTED_ORDER, "false");
        var courier = testFactory.courier();
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .updateCourier(courier)
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2").get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .multiPlace(true)
                .placeExternalId("p1")
                .needResort(true)
                .deliveryServiceName(order.getDeliveryService().getName())
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .availableCells(List.of())
                .resortReasonCode(ResortReasonCode.RESORT_TO_BUFFER)
                .build();
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=" + order.getExternalId() + "&placeExternalId=" + "p1")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("ПОСЛЕДНЯЯ МИЛЯ. Для многоместного заказа на последней миле НЕ нужно пересортировывать " +
            "в хранение многоместный заказ, полностью отсортированный. Обычный СЦ.")
    @Test
    @SneakyThrows
    void onOrderResortMultiPlaceKeepFullSortedSorted() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "false");
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DROPOFF_CAN_PROCESS_CANCEL_ON_SORTED_ORDER, "false");
        var courier = testFactory.courier();
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .updateCourier(courier)
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2", "p3").get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .multiPlace(true)
                .placeExternalId("p1")
                .needResort(false)
                .deliveryServiceName(order.getDeliveryService().getName())
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .availableCells(List.of())
                .build();
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=" + order.getExternalId() + "&placeExternalId=" + "p1")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("ПОСЛЕДНЯЯ МИЛЯ. Одноместный, отсортированный заказ не нужно пересортировывать.")
    @Test
    @SneakyThrows
    void onOrderResortFullSortedSortedLastMile() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "false");
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DROPOFF_CAN_PROCESS_CANCEL_ON_SORTED_ORDER, "false");
        var courier = testFactory.courier();
        var order = testFactory
                .create(order(sortingCenter).externalId("1")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .updateCourier(courier)
                .accept().sort().get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .multiPlace(false)
                .needResort(false)
                .deliveryServiceName(order.getDeliveryService().getName())
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .availableCells(List.of())
                .build();
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=" + order.getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("ПОСЛЕДНЯЯ МИЛЯ. Одноместный, неотсортированный заказ не нужно пересортировывать.")
    @Test
    @SneakyThrows
    void onOrderResortNotSortedLastMile() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "false");
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DROPOFF_CAN_PROCESS_CANCEL_ON_SORTED_ORDER, "false");
        var courier = testFactory.courier();
        var order = testFactory
                .create(order(sortingCenter).externalId("1")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .updateCourier(courier)
                .accept().get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .multiPlace(false)
                .needResort(false)
                .deliveryServiceName(order.getDeliveryService().getName())
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .availableCells(List.of())
                .build();
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + testFactory.getRouteIdForSortableFlow(route) +
                                "&externalId=" + order.getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("ПОСЛЕДНЯЯ МИЛЯ. Запрашивают инфо на несуществующий заказ")
    @Test
    @SneakyThrows
    void onOrderResortNonExistingOrderLastMile() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "false");
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DROPOFF_CAN_PROCESS_CANCEL_ON_SORTED_ORDER, "false");
        var courier = testFactory.courier();
        var order = testFactory
                .create(order(sortingCenter).externalId("1")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .updateCourier(courier)
                .accept().sort().get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId=" + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=non_existing_order")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(String.format("""
                        {
                          "status": 400,
                          "error": "ORDER_NOT_FOUND",
                          "message": "Order with externalId non_existing_order not found on route %s"
                        }""", testFactory.getRouteIdForSortableFlow(route)), false));
    }

    @DisplayName("ПОСЛЕДНЯЯ МИЛЯ. Запрашивают инфо на несуществующую посылку. Не просим пересортировать " +
            "даже если такой посылки нет.")
    @Test
    @SneakyThrows
    void onOrderResortNonExistingPlaceLastMile() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "false");
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DROPOFF_CAN_PROCESS_CANCEL_ON_SORTED_ORDER, "false");
        var courier = testFactory.courier();
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .updateCourier(courier)
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2", "p3").get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .multiPlace(true)
                .placeExternalId("p777")
                .deliveryServiceName(order.getDeliveryService().getName())
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .needResort(false)
                .availableCells(List.of())
                .build();
        // В новом api мы ищем ApiOrderDto через placeExternalId, поэтому если его не найдено, выдаем ошибку
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId="
                                        + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=1&placeExternalId=" + "p777")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is4xxClientError());
    }

    @DisplayName("ПОСЛЕДНЯЯ МИЛЯ. Запрашивают инфо на неотсортированную посылку. Не просим пересортировать.")
    @Test
    @SneakyThrows
    void onOrderResortNonSortedPlaceLastMile() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "false");
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DROPOFF_CAN_PROCESS_CANCEL_ON_SORTED_ORDER, "false");
        var courier = testFactory.courier();
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .updateCourier(courier)
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2").get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order.getExternalId())
                .multiPlace(true)
                .placeExternalId("p3")
                .needResort(false)
                .availableCells(List.of())
                .build();
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId="
                                        + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=1&placeExternalId=" + "p3")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("ПОСЛЕДНЯЯ МИЛЯ. Запрашивают инфо на заказ не с этого маршрута. " +
            "Отвечаем что пересортировка не требуется (даже не смотря на то, что заказ не с данного маршрута).")
    @Test
    @SneakyThrows
    void onOrderResortOrderNotFromThisRouteLastMile() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, "false");
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.DROPOFF_CAN_PROCESS_CANCEL_ON_SORTED_ORDER, "false");
        var courier = testFactory.courier();
        var courier2 = testFactory.storedCourier(courier.getId() + 17L); //random courier
        var order = testFactory
                .create(order(sortingCenter).externalId("1")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .updateCourier(courier)
                .accept().sort().get();
        var order2 = testFactory
                .create(order(sortingCenter).externalId("2")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .updateCourier(courier2)
                .accept().sort().get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        var route2 = testFactory.findOutgoingRoute(order2).orElseThrow();
        var expected = BeforeShipOrderDto.builder()
                .orderExternalId(order2.getExternalId())
                .multiPlace(false)
                .deliveryServiceName(order.getDeliveryService().getName())
                .possibleOutgoingRouteDate(order.getOutgoingRouteDate())
                .needResort(false)
                .availableCells(List.of())
                .build();
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/resortInfo?routeId="
                                        + testFactory.getRouteIdForSortableFlow(route) +
                                        "&externalId=" + order2.getExternalId())
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var actual = objectMapper.readValue(response, BeforeShipOrderDto.class);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("ДРОПОФФ. Нельзя отсортировать отмененный заказ на дропоффе в лот.")
    @Test
    @SneakyThrows
    void cantSortCanceledOrderToLotOnDropoff() {
        var order =
                testFactory.create(order(sortingCenter).externalId("1")
                                .places("p1", "p2")
                                .deliveryDate(LocalDate.now(clock))
                                .shipmentDate(LocalDate.now(clock))
                                .build())
                        .acceptPlaces(List.of("p1", "p2"))
                        .sortPlaces(List.of("p1", "p2"))
                        .get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var courierCell = testFactory.determineRouteCell(route, order);
        testFactory.cancelOrder(order.getId());
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        SortableSortRequestDto request = new SortableSortRequestDto(order.getExternalId(), "p1", lot.getBarcode());

        caller.sortableBetaSort(request)
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                          "status": 400,
                          "error": "CANT_SORT_CANCELED_ORDER_TO_LOT",
                          "message": "Этот заказ отменен. Не нужно его помещать в лот"
                        }""", false));
    }

}
