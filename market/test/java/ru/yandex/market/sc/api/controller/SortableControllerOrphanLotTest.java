package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.lot.LotCommandService;
import ru.yandex.market.sc.core.domain.lot.repository.Lot;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author mors741
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SortableControllerOrphanLotTest extends BaseApiControllerTest {

    private final LotCommandService lotCommandService;
    private final SortableLotService sortableLotService;

    @MockBean
    private Clock clock;

    private SortingCenter sortingCenter;
    private Cell cell;
    private TestControllerCaller controllerCaller;
    private TestFactory.CourierWithDs courierWithDs;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter,
                SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        testFactory.setSortingCenterProperty(
                sortingCenter,
                SortingCenterPropertiesKey.ENABLE_ORPHAN_LOTS, "true");
        courierWithDs = testFactory.magistralCourier();
        cell = testFactory.storedMagistralCell(sortingCenter, "123", CellSubType.DEFAULT,
                courierWithDs.courier().getId());
        testFactory.storedUser(sortingCenter, UID);
        TestFactory.setupMockClock(clock);
        testFactory.increaseScOrderId();
        controllerCaller = TestControllerCaller.createCaller(mockMvc);
    }

    @Test
    @SneakyThrows
    void parentRequired() {
        Place place = testFactory.createOrderForToday(sortingCenter).accept().getPlace();
        String lotExtId = lotCommandService.createOrphanLots(sortingCenter, 1).get(0);

        var request = new SortableSortRequestDto(place.getExternalId(), place.getMainPartnerCode(), lotExtId);
        controllerCaller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(parentRequired(lotExtId, lotExtId), true));

        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    @SneakyThrows
    @DisplayName("Основной сценарий орфан лотов")
    void mainFlow() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");

        Place place = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(courierWithDs.deliveryService())
                        .build()
        ).accept().getPlace();
        Route route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        Cell courierCell = testFactory.determineRouteCell(route, place);
        String lotExtId = lotCommandService.createOrphanLots(sortingCenter, 1).get(0);

        // Пробуем сортировать в орфан лот без привязанной ячейки
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(
                        place.getExternalId(),
                        place.getMainPartnerCode(),
                        lotExtId))
                .andExpect(status().isOk())
                .andExpect(content().json(parentRequired(lotExtId, lotExtId), true));

        // Получили parentRequired, заказ не отсортирован
        place = testFactory.getPlace(place.getId());
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
        assertThat(place.getParent()).isNull();

        // Сортируем в орфан лот с привязкой к ячейке
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(
                        place.getExternalId(),
                        place.getMainPartnerCode(),
                        lotExtId,
                        String.valueOf(courierCell.getId()),
                        false))
                .andExpect(status().isOk())
                .andExpect(content().json(sortedInLot(lotExtId, lotExtId + " " + cell.getScNumber()), true));

        // Заказ отсортирован, лот привязан к ячейке
        place = testFactory.getPlace(place.getId());
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(place.getParent()).isNotNull();
        assertThat(place.getParent().getRequiredBarcodeOrThrow()).isEqualTo(lotExtId);
    }

    @Test
    @SneakyThrows
    @DisplayName("Основной сценарий без сортировки напрямую в лот (Палетизация)")
    void mainFlowWithoutDirecLotSort() {
        Place place = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .deliveryService(courierWithDs.deliveryService())
                                .build()
                )
                .accept()
                .sort()
                .getPlace();
        Route route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        Cell courierCell = testFactory.determineRouteCell(route, place);
        String lotExtId = lotCommandService.createOrphanLots(sortingCenter, 1).get(0);

        // Пробуем сортировать в орфан лот без привязанной ячейки
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(
                        place.getExternalId(),
                        place.getMainPartnerCode(),
                        lotExtId))
                .andExpect(status().isOk())
                .andExpect(content().json(parentRequired(lotExtId, lotExtId), true));

        // Получили parentRequired, заказ не отсортирован в лот
        assertThat(place.getParent()).isNull();

        // Сортируем в орфан лот с привязкой к ячейке
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(
                        place.getExternalId(),
                        place.getMainPartnerCode(),
                        lotExtId,
                        String.valueOf(courierCell.getId()),
                        false))
                .andExpect(status().isOk())
                .andExpect(content().json(sortedInLot(lotExtId, lotExtId + " " + cell.getScNumber()), true));

        // Заказ отсортирован, лот привязан к ячейке
        Sortable parent = testFactory.getPlace(place.getId()).getParent();
        assertThat(parent).isNotNull();
        assertThat(parent.getRequiredBarcodeOrThrow()).isEqualTo(lotExtId);
    }

    @Test
    @SneakyThrows
    @DisplayName("Требуем привязывать орфана к целлу только первый раз")
    void cellBindingOnlyFirstTime() {
        Place place1 = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .deliveryService(courierWithDs.deliveryService())
                                .build()
                )
                .accept()
                .sort()
                .getPlace();
        Route route = testFactory.findOutgoingCourierRoute(place1).orElseThrow();
        Cell courierCell = testFactory.determineRouteCell(route, place1);
        String lotExtId = lotCommandService.createOrphanLots(sortingCenter, 1).get(0);

        // Сортируем в орфан лот с привязкой к ячейке
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(
                        place1.getExternalId(),
                        place1.getMainPartnerCode(),
                        lotExtId,
                        String.valueOf(courierCell.getId()),
                        false))
                .andExpect(status().isOk())
                .andExpect(content().json(sortedInLot(lotExtId, lotExtId + " " + cell.getScNumber()), true));
        // Заказ отсортирован, лот привязан к ячейке
        Sortable parent1 = testFactory.updated(place1).getParent();
        assertThat(parent1).isNotNull();
        assertThat(parent1.getRequiredBarcodeOrThrow()).isEqualTo(lotExtId);

        Place place2 = testFactory.createForToday(
                        order(sortingCenter)
                                .externalId("o2")
                                .dsType(DeliveryServiceType.TRANSIT)
                                .deliveryService(courierWithDs.deliveryService())
                                .build()
                )
                .accept()
                .sort(courierCell.getId())
                .getPlace();

        // Сортируем в привязанный орфан лот - id целла больше не нужен
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(
                        place2.getExternalId(),
                        place2.getMainPartnerCode(),
                        lotExtId))
                .andExpect(status().isOk())
                .andExpect(content().json(sortedInLot(lotExtId, lotExtId + " " + cell.getScNumber()), true));

        // Заказ отсортирован в тот же лот
        Sortable parent2 = testFactory.updated(place2).getParent();
        assertThat(parent2).isNotNull();
        assertThat(parent2.getRequiredBarcodeOrThrow()).isEqualTo(lotExtId);
    }


    @Test
    @SneakyThrows
    @DisplayName("Привязываем второго орфана к тому же целлу")
    void bindSecondOrphanToSameCell() {
        Place place1 = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .deliveryService(courierWithDs.deliveryService())
                                .build()
                )
                .accept()
                .sort()
                .getPlace();
        Route route = testFactory.findOutgoingCourierRoute(place1).orElseThrow();
        Cell courierCell = testFactory.determineRouteCell(route, place1);
        List<String> orphanLots = lotCommandService.createOrphanLots(sortingCenter, 2);
        String lot1ExtId = orphanLots.get(0);
        String lot2ExtId = orphanLots.get(1);

        // Сортируем в орфан лот с привязкой к ячейке
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(
                        place1.getExternalId(),
                        place1.getMainPartnerCode(),
                        lot1ExtId,
                        String.valueOf(courierCell.getId()),
                        false))
                .andExpect(status().isOk())
                .andExpect(content().json(sortedInLot(lot1ExtId, lot1ExtId + " " + cell.getScNumber()), true));

        // Заказ отсортирован, лот привязан к ячейке
        Sortable parent1 = testFactory.updated(place1).getParent();
        assertThat(parent1).isNotNull();
        assertThat(parent1.getRequiredBarcodeOrThrow()).isEqualTo(lot1ExtId);


        Place place2 = testFactory.createForToday(
                        order(sortingCenter)
                                .externalId("o2")
                                .dsType(DeliveryServiceType.TRANSIT)
                                .deliveryService(courierWithDs.deliveryService())
                                .build()
                )
                .accept()
                .sort(courierCell.getId())
                .getPlace();

        // Сортируем в новый лот, привязанный к этой же ячейке
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(
                        place2.getExternalId(),
                        place2.getMainPartnerCode(),
                        lot2ExtId,
                        String.valueOf(courierCell.getId()),
                        false))
                .andExpect(status().isOk())
                .andExpect(content().json(sortedInLot(lot2ExtId, lot2ExtId + " " + cell.getScNumber()), true));

        // Заказ отсортирован в новый лот
        Sortable parent2 = testFactory.updated(place2).getParent();
        assertThat(parent2).isNotNull();
        assertThat(parent2.getRequiredBarcodeOrThrow()).isEqualTo(lot2ExtId);
    }

    @Test
    @SneakyThrows
    @DisplayName("Можно привязать лот только к целлу, в который просится заказ")
    void tryToBindWrongCell() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        Place place = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(courierWithDs.deliveryService())
                        .build()
        ).accept().getPlace();
        Cell wrongCell = testFactory.storedCell(sortingCenter, "WRONG CELL", CellType.COURIER);
        String lotExtId = lotCommandService.createOrphanLots(sortingCenter, 1).get(0);

        // Пытаемся сортировать в орфан лот с привязкой к неправильной ячейке (не ток, в которую просился заказ)
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(
                        place.getExternalId(),
                        place.getMainPartnerCode(),
                        lotExtId,
                        String.valueOf(wrongCell.getId()),
                        false))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"status\":400,\"error\":\"LOT_PARENT_CELL_FROM_ANOTHER_ROUTE\"}", false));

        // Заказ НЕ отсортирован в лот
        assertThat(place.getParent()).isNull();

        // Лот НЕ привязался к ячейке
        List<SortableLot> lotsOfCell = sortableLotService.findAllAvailableByParentCellIdInOrderById(
                List.of(wrongCell.getId())
        );
        assertThat(lotsOfCell).isEmpty();
    }

    @Test
    @SneakyThrows
    @DisplayName("Нельзя складывать в привязанный лот заказы с другого маршрута")
    void orderFromOtherRoute() {
        Place place1 = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .deliveryService(courierWithDs.deliveryService())
                                .build()
                )
                .accept()
                .sort()
                .getPlace();
        Route route = testFactory.findOutgoingCourierRoute(place1).orElseThrow();
        Cell courierCell = testFactory.determineRouteCell(route, place1);
        String lotExtId = lotCommandService.createOrphanLots(sortingCenter, 1).get(0);

        // Сортируем в орфан лот с привязкой к ячейке
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(
                        place1.getExternalId(),
                        place1.getMainPartnerCode(),
                        lotExtId,
                        String.valueOf(courierCell.getId()),
                        false))
                .andExpect(status().isOk())
                .andExpect(content().json(sortedInLot(lotExtId, lotExtId + " " + cell.getScNumber()), true));

        // Заказ отсортирован, лот привязан к ячейке
        Sortable parent = testFactory.getPlace(place1.getId()).getParent();
        assertThat(parent).isNotNull();
        assertThat(parent.getRequiredBarcodeOrThrow()).isEqualTo(lotExtId);

        // Создаём заказ с другого маршрута
        var courier = testFactory.storedCourier(7L, "abc7");
        Place place2 = testFactory.createForToday(order(sortingCenter).externalId("o2").build())
                .updateShipmentDate(LocalDate.now(clock)).updateCourier(courier)
                .accept()
                .sort()
                .getPlace();

        // Пытаемся отсортировать новый заказ в лот, привязанные к неправильной ячейке
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(
                        place2.getExternalId(),
                        place2.getMainPartnerCode(),
                        lotExtId))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"status\":400,\"error\":\"LOT_PARENT_CELL_FROM_ANOTHER_ROUTE\"}",
                        false));

        // Заказ НЕ отсортирован в лот
        assertThat(testFactory.getPlace(place2.getId()).getParent()).isNull();
    }

    @Test
    @SneakyThrows
    @DisplayName("Нельзя привязывать целл к орфану, у которого уже привязан целл")
    void tryToRebindCell_ignoring_notRebinding() {
        Place place1 = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .deliveryService(courierWithDs.deliveryService())
                                .build()
                )
                .accept()
                .sort()
                .getPlace();
        Route route = testFactory.findOutgoingCourierRoute(place1).orElseThrow();
        Cell courierCell = testFactory.determineRouteCell(route, place1);
        String lotExtId = lotCommandService.createOrphanLots(sortingCenter, 1).get(0);

        // Сортируем в орфан лот с привязкой к ячейке
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(
                        place1.getExternalId(),
                        place1.getMainPartnerCode(),
                        lotExtId,
                        String.valueOf(courierCell.getId()),
                        false))
                .andExpect(status().isOk())
                .andExpect(content().json(sortedInLot(lotExtId, lotExtId + " " + cell.getScNumber()), true));

        // Заказ отсортирован, лот привязан к ячейке
        Sortable parent = testFactory.getPlace(place1.getId()).getParent();
        assertThat(parent).isNotNull();
        assertThat(parent.getRequiredBarcodeOrThrow()).isEqualTo(lotExtId);

        // Создаём заказ с другого маршрута
        var courier = testFactory.storedCourier(7L, "abc7");
        Place place2 = testFactory.createForToday(order(sortingCenter).externalId("o2").build())
                .updateShipmentDate(LocalDate.now(clock)).updateCourier(courier)
                .accept()
                .sort()
                .getPlace();

        // Пытаемся отсортировать новый заказ в тот же лот, привязывая другую ячейку
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(
                        place2.getExternalId(),
                        place2.getMainPartnerCode(),
                        lotExtId,
                        String.valueOf(courierCell.getId()),
                        false))
                .andExpect(status().is4xxClientError());

        // Заказ НЕ отсортирован в лот
        assertThat(testFactory.getPlace(place2.getId()).getParent()).isNull();

        // Лот всё так же привязан к первой ячейке
        List<SortableLot> lotsOfCell =
                sortableLotService.findAllAvailableByParentCellIdInOrderById(List.of(courierCell.getId()));
        assertThat(lotsOfCell).hasSize(1);
        assertThat(lotsOfCell.get(0).getBarcode()).isEqualTo(lotExtId);
    }

    @Test
    @SneakyThrows
    @DisplayName("Сортируем в орфан многоместный заказ")
    void multiplace() {
        OrderLike order = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .deliveryService(courierWithDs.deliveryService())
                                .places("p1", "p2")
                                .build()
                )
                .acceptPlaces()
                .sortPlaces()
                .get();
        Route route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        Cell courierCell = testFactory.determineRouteCell(route, order);
        String lotExtId = lotCommandService.createOrphanLots(sortingCenter, 1).get(0);

        // Сортируем в орфан лот с привязкой к ячейке
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(
                        order.getExternalId(),
                        "p1",
                        lotExtId,
                        String.valueOf(courierCell.getId()),
                        false))
                .andExpect(status().isOk())
                .andExpect(content().json(sortedInLot(lotExtId, lotExtId + " " + cell.getScNumber()), true));

        // Сортируем в орфан лот, не привязывая ячейку, потому что она уже есть
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(
                        order.getExternalId(),
                        "p2",
                        lotExtId))
                .andExpect(status().isOk())
                .andExpect(content().json(sortedInLot(lotExtId, lotExtId + " " + cell.getScNumber()), true));

        // Оба плейса отсортированы в орфан лот, который теперь привязан к маршруту
        List<Place> places = testFactory.orderPlaces(order.getId());
        for (Place place : places) {
            Lot lot = place.getLot();
            assertThat(sortableLotService.findByLotIdOrThrow(lot.getId()).getBarcode()).isEqualTo(lotExtId);

        }
    }

    @Test
    @SneakyThrows
    @DisplayName("isLotSortAvailable=true даже если у ячейки, в которую просится заказ, нет привязанных лотов")
        // потому что орфаны можно привязать на лету к любой ячейке
    void lotSortAvailableWithoutLots() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");

        OrderLike order = testFactory.createOrderForToday(sortingCenter).get();
        lotCommandService.createOrphanLots(sortingCenter, 1);

        controllerCaller.acceptOrder(new AcceptOrderRequestDto(
                        order.getExternalId(),
                        null))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.lotSortAvailable").value(true));
    }

    @Test
    @SneakyThrows
    @DisplayName("isLotSortAvailable=false, если заказ просится на хранение")
        // потому что на хранении не бывает лотов
    void lotSortNotAvailableForBufferCells() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.storedCell(sortingCenter, "buffer-1", CellType.BUFFER);

        OrderLike order = testFactory.createOrder(sortingCenter).get();
        lotCommandService.createOrphanLots(sortingCenter, 1);

        controllerCaller.acceptOrder(new AcceptOrderRequestDto(
                        order.getExternalId(),
                        null))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.lotSortAvailable").value(false));
    }

    private String sortedInLot(String lotId, String lotName) {
        return "{" +
                "\"destination\":{\"id\":\"" + lotId + "\",\"name\":\"" + lotName + "\",\"type" +
                "\":\"LOT\"}," +
                "\"parentRequired\":false" +
                "}";
    }

    private String parentRequired(String lotId, String lotName) {
        return "{" +
                "\"destination\":{\"id\":\"" + lotId + "\",\"name\":\"" + lotName + "\",\"type\":\"LOT\"}," +
                "\"parentRequired\":true" +
                "}";
    }
}
