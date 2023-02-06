package ru.yandex.market.sc.internal.domain.order_history;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.ClientReturnService;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.model.CreateReturnRequest;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.model.ScOrderState;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.order_history.model.HistoryEvent;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.test.TestFactory.CreateOrderParams;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.controller.dto.PartnerOrderWithItemsDto;
import ru.yandex.market.sc.internal.model.LocationDto;
import ru.yandex.market.sc.internal.model.WarehouseDto;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.internal.controller.dto.PartnerOrderWithItemsDto.Action.MARK_AS_DAMAGED;
import static ru.yandex.market.sc.internal.controller.dto.PartnerOrderWithItemsDto.CancelReason.IS_DAMAGED;

@EmbeddedDbIntTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
class PartnerOrderStatusEventServiceTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    PartnerOrderStatusEventService partnerOrderStatusEventService;
    @Autowired
    ScOrderRepository scOrderRepository;
    @Autowired
    PlaceRepository placeRepository;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    XDocFlow flow;
    @Autowired
    SortableLotService sortableLotService;
    @Autowired
    SortableQueryService sortableQueryService;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    OrderCommandService orderCommandService;

    @Autowired
    Clock clock;

    private SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "false");
    }

    @Test
    void getPartnerOrderEventOrThrow() {
        String externalOrderId = "1";
        var scOrder = testFactory.createForToday(order(sortingCenter).externalId(externalOrderId).build())
                .accept().get();
        transactionTemplate.execute(ts -> {
            var partnerEvents = partnerOrderStatusEventService.getPartnerOrderEventOrThrow(
                    sortingCenter, externalOrderId);
            var actualOrder = scOrderRepository.findByIdOrThrow(scOrder.getId());

            int placeEventsCount = 0;
            List<Place> places = placeRepository.findAllByOrderIdOrderById(scOrder.getId());
            assertThat(places).hasSize(1);
            placeEventsCount = places.get(0).getHistory().size();

            assertEquals(actualOrder.getFfStatusHistory().size() + actualOrder.getUpdateHistoryItems().size()
                    + placeEventsCount, partnerEvents.size());
            return null;
        });
    }

    @Test
    void getPartnerOrderEventMultiOrder() {
        String externalOrderId = "1";
        var scOrder = testFactory.createForToday(
                order(sortingCenter).externalId(externalOrderId).places("p1", "p2").build()
        ).acceptPlaces().get();
        transactionTemplate.execute(ts -> {
            var orderEvents = partnerOrderStatusEventService.getPartnerOrderEventOrThrow(
                    sortingCenter, externalOrderId);
            var actualOrder = scOrderRepository.findByIdOrThrow(scOrder.getId());
            var places = placeRepository.findAllByOrderIdOrderById(actualOrder.getId());
            var expectedSize = actualOrder.getFfStatusHistory().size() +
                    places.stream().map(Place::getHistory).mapToLong(List::size).sum() +
                    actualOrder.getUpdateHistoryItems().size();
            assertEquals(expectedSize, orderEvents.size());
            return null;
        });
    }

    @Test
    void getOrderItemsOrThrow() {
        String canceledExternalId = "0";
        var canceledScOrder = testFactory.createForToday(order(sortingCenter).externalId(canceledExternalId).build())
                .cancel().get();
        var canceledPartnerOrder = partnerOrderStatusEventService.getOrderItemsOrThrow(
                sortingCenter, canceledExternalId);
        assertPartnerOrder(canceledScOrder, canceledPartnerOrder, false, false);

        String acceptedExternalId = "1";
        var acceptedScOrder = testFactory.createForToday(
                CreateOrderParams.builder().sortingCenter(sortingCenter)
                        .externalId(acceptedExternalId)
                        .warehouseCanProcessDamagedOrders(true)
                        .build()
        ).accept().get();
        var acceptedPartnerOrder = partnerOrderStatusEventService.getOrderItemsOrThrow(
                sortingCenter, acceptedExternalId);
        assertPartnerOrder(acceptedScOrder, acceptedPartnerOrder, true, false);

        String wrongExternalId = "2";
        assertThrows(TplEntityNotFoundException.class,
                () -> partnerOrderStatusEventService.getOrderItemsOrThrow(sortingCenter, wrongExternalId));

        String multiOrderId = "3";
        var multiOrder = testFactory.createForToday(
                CreateOrderParams.builder().sortingCenter(sortingCenter)
                        .externalId(multiOrderId)
                        .places("mp1", "mp2")
                        .warehouseCanProcessDamagedOrders(true)
                        .build()
        ).acceptPlaces().get();
        var partnerMultiOrder = partnerOrderStatusEventService.getOrderItemsOrThrow(
                sortingCenter, multiOrderId);
        assertPartnerOrder(multiOrder, partnerMultiOrder, true, false);

        String alreadyMarkedAsDamaged = "4";
        var alreadyMarkedAsDamagedOrder = testFactory.createForToday(
                CreateOrderParams.builder().sortingCenter(sortingCenter)
                        .externalId(alreadyMarkedAsDamaged)
                        .warehouseCanProcessDamagedOrders(true)
                        .build()
        ).accept().markOrderAsDamaged().get();
        var partnerAlreadyMarkedAsDamagedOrder = partnerOrderStatusEventService
                .getOrderItemsOrThrow(sortingCenter, alreadyMarkedAsDamaged);
        assertPartnerOrder(alreadyMarkedAsDamagedOrder, partnerAlreadyMarkedAsDamagedOrder, true, false);
    }

    @Test
    void getPalletSorted() {
        Cell shipCell = flow.createShipCellAndGet("cell-2");
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "true");
        flow.createInbound("in1")
                .linkPallets("XDOC-p-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-p-1")
                .createOutbound("out-1")
                .buildRegistry("XDOC-p-1")
                .sortToAvailableCell("XDOC-p-1");

        PartnerOrderWithItemsDto dto = partnerOrderStatusEventService.getOrderItemsOrThrow(sortingCenter, "XDOC-p-1");
        assertThat(dto.getStatus()).isEqualTo(ScOrderState.SORTED);
        assertThat(dto.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(dto.getExternalOrderId()).isEqualTo("XDOC-p-1");
        assertThat(dto.getCellName()).isEqualTo(shipCell.getScNumber());
    }

    @Test
    void getPalletPrepared() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "true");
        flow.createInbound("in1")
                .linkPallets("XDOC-p-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-p-1")
                .createOutbound("out-1")
                .buildRegistry("XDOC-p-1")
                .sortToAvailableCell("XDOC-p-1")
                .prepareToShip("XDOC-p-1");

        PartnerOrderWithItemsDto dto = partnerOrderStatusEventService.getOrderItemsOrThrow(sortingCenter, "XDOC-p-1");
        assertThat(dto.getStatus()).isEqualTo(ScOrderState.SORTED);
        assertThat(dto.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO);
        assertThat(dto.getExternalOrderId()).isEqualTo("XDOC-p-1");
        assertThat(dto.getCellName()).isNull();
    }

    @Test
    void getPalletShipped() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "true");
        flow.createInbound("in1")
                .linkPallets("XDOC-p-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-p-1")
                .createOutbound("out-1")
                .buildRegistry("XDOC-p-1")
                .sortToAvailableCell("XDOC-p-1")
                .prepareToShip("XDOC-p-1")
                .shipAndGet("out-1");

        PartnerOrderWithItemsDto dto = partnerOrderStatusEventService.getOrderItemsOrThrow(sortingCenter, "XDOC-p-1");
        assertThat(dto.getStatus()).isEqualTo(ScOrderState.SHIPPED);
        assertThat(dto.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(dto.getExternalOrderId()).isEqualTo("XDOC-p-1");
        assertThat(dto.getCellName()).isNull();
    }

    @Test
    void getPalletEvents() {
        Cell bufCell = flow.createBufferCellAndGet("cell-1 ", TestFactory.WAREHOUSE_YANDEX_ID);
        Cell shipCell = flow.createShipCellAndGet("cell-2");
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "true");
        flow.createInbound("in1")
                .linkPallets("XDOC-p-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-p-1")
                .createOutbound("out-1")
                .buildRegistry("XDOC-p-1")
                .sortToAvailableCell("XDOC-p-1")
                .prepareToShip("XDOC-p-1")
                .shipAndGet("out-1");

        assertThat(partnerOrderStatusEventService.getPartnerOrderEventOrThrow(flow.getSortingCenter(), "XDOC-p-1"))
                .hasSize(7)
                .anyMatch(dto -> dto.getEvent() == HistoryEvent.AWAITING_DIRECT)
                .anyMatch(dto -> dto.getEvent() == HistoryEvent.ARRIVED_DIRECT)
                .anyMatch(dto -> dto.getEvent() == HistoryEvent.KEEPED_DIRECT
                        && bufCell.getId().equals(dto.getCellId()) && bufCell.getScNumber().equals(dto.getCellName()))
                .anyMatch(dto -> dto.getEvent() == HistoryEvent.SORTED_DIRECT
                        && shipCell.getId().equals(dto.getCellId()) && shipCell.getScNumber().equals(dto.getCellName()))
                .anyMatch(dto -> dto.getEvent() == HistoryEvent.PREPARED_DIRECT)
                .anyMatch(dto -> dto.getEvent() == HistoryEvent.SHIPPED_DIRECT);
    }

    @Test
    void getBasketSorted() {
        Cell bufCell = flow.createBufferCellLocationAndGet("cell-1 ", TestFactory.WAREHOUSE_YANDEX_ID);
        Cell shipCell = flow.createShipCellAndGet("cell-2");
        Sortable basket = flow.createBasketAndGet(bufCell);
        var lot = sortableLotService.findBySortableId(basket.getId()).orElseThrow();

        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "true");

        flow.createInbound("in1")
                .linkBoxes("XDOC-b-1", "XDOC-b-2")
                .fixInbound();

        Sortable box1 = sortableQueryService.find(sortingCenter, "XDOC-b-1").orElseThrow();
        Sortable box2 = sortableQueryService.find(sortingCenter, "XDOC-b-2").orElseThrow();
        flow.sortBoxToLot(box1, lot);
        flow.sortBoxToLot(box2, lot);

        flow.createOutbound("out-1")
                .buildRegistry(basket.getRequiredBarcodeOrThrow())
                .packLot(basket.getRequiredBarcodeOrThrow())
                .sortToAvailableCell(basket.getRequiredBarcodeOrThrow());

        PartnerOrderWithItemsDto dto = partnerOrderStatusEventService.getOrderItemsOrThrow(sortingCenter,
                basket.getRequiredBarcodeOrThrow());
        assertThat(dto.getStatus()).isEqualTo(ScOrderState.SORTED);
        assertThat(dto.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(dto.getExternalOrderId()).isEqualTo(basket.getRequiredBarcodeOrThrow());
        assertThat(dto.getCellName()).isEqualTo(shipCell.getScNumber());
    }

    @Test
    void getBasketShipped() {
        Cell bufCell = flow.createBufferCellLocationAndGet("cell-1 ", TestFactory.WAREHOUSE_YANDEX_ID);
        Cell shipCell = flow.createShipCellAndGet("cell-2");
        Sortable basket = flow.createBasketAndGet(bufCell);
        var lot = sortableLotService.findBySortableId(basket.getId()).orElseThrow();

        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "true");

        flow.createInbound("in1")
                .linkBoxes("XDOC-b-1", "XDOC-b-2")
                .fixInbound();

        Sortable box1 = sortableQueryService.find(sortingCenter, "XDOC-b-1").orElseThrow();
        Sortable box2 = sortableQueryService.find(sortingCenter, "XDOC-b-2").orElseThrow();
        flow.sortBoxToLot(box1, lot);
        flow.sortBoxToLot(box2, lot);

        flow.createOutbound("out-1")
                .buildRegistry(basket.getRequiredBarcodeOrThrow())
                .packLot(basket.getRequiredBarcodeOrThrow())
                .sortToAvailableCell(basket.getRequiredBarcodeOrThrow())
                .prepareToShip(basket.getRequiredBarcodeOrThrow())
                .shipAndGet("out-1");

        PartnerOrderWithItemsDto dto = partnerOrderStatusEventService.getOrderItemsOrThrow(sortingCenter,
                basket.getRequiredBarcodeOrThrow());
        assertThat(dto.getStatus()).isEqualTo(ScOrderState.SHIPPED);
        assertThat(dto.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(dto.getExternalOrderId()).isEqualTo(basket.getRequiredBarcodeOrThrow());
        assertThat(dto.getCellName()).isNull();
    }

    @Test
    void getBasketEvents() {
        Cell bufCell = flow.createBufferCellLocationAndGet("cell-1 ", TestFactory.WAREHOUSE_YANDEX_ID);
        Cell shipCell = flow.createShipCellAndGet("cell-2");
        Sortable basket = flow.createBasketAndGet(bufCell);
        var lot = sortableLotService.findBySortableId(basket.getId()).orElseThrow();

        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "true");
        flow.createInbound("in1")
                .linkBoxes("XDOC-b-1", "XDOC-b-2")
                .fixInbound();

        Sortable box1 = sortableQueryService.find(sortingCenter, "XDOC-b-1").orElseThrow();
        Sortable box2 = sortableQueryService.find(sortingCenter, "XDOC-b-2").orElseThrow();
        flow.sortBoxToLot(box1, lot);
        flow.sortBoxToLot(box2, lot);

        flow.createOutbound("out-1")
                .buildRegistry(basket.getRequiredBarcodeOrThrow())
                .packLot(basket.getRequiredBarcodeOrThrow())
                .sortToAvailableCell(basket.getRequiredBarcodeOrThrow())
                .prepareToShip(basket.getRequiredBarcodeOrThrow())
                .shipAndGet("out-1");

        // to mess with a result a little
        flow.createInbound("in-2")
                .linkPallets("XDOC-p-1")
                .fixInbound();

        assertThat(partnerOrderStatusEventService.getPartnerOrderEventOrThrow(flow.getSortingCenter(),
                basket.getRequiredBarcodeOrThrow()))
                .hasSize(5)
                .anyMatch(dto -> dto.getEvent() == HistoryEvent.KEEPED_DIRECT
                        && bufCell.getId().equals(dto.getCellId()) && bufCell.getScNumber().equals(dto.getCellName()))
                .anyMatch(dto -> dto.getEvent() == HistoryEvent.SORTED_DIRECT
                        && shipCell.getId().equals(dto.getCellId()) && shipCell.getScNumber().equals(dto.getCellName()))
                .anyMatch(dto -> dto.getEvent() == HistoryEvent.PREPARED_DIRECT)
                .anyMatch(dto -> dto.getEvent() == HistoryEvent.SHIPPED_DIRECT);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertPartnerOrder(
            OrderLike scOrder,
            PartnerOrderWithItemsDto partnerOrder,
            boolean wasAccept,
            boolean canBeProcessAsDamaged
    ) {
        transactionTemplate.execute(ts -> {
            var actualOrder = scOrderRepository.findByIdOrThrow(scOrder.getId());
            assertEquals(actualOrder.getExternalId(), partnerOrder.getExternalOrderId());
            assertNotNull(partnerOrder.getCreateOrderDateTime());
            if (wasAccept) {
                assertNotNull(partnerOrder.getArrivedToSoDate());
            } else {
                assertNull(partnerOrder.getArrivedToSoDate());
            }
            assertEquals(actualOrder.getFfStatus(), partnerOrder.getFfStatus());
            assertEquals(0, partnerOrder.getItems().size());

            var places = placeRepository.findAllByOrderIdOrderById(actualOrder.getId());
            assertEquals(places.size(), partnerOrder.getPlaces().size());

            if (scOrder.isDamaged()) {
                assertEquals(IS_DAMAGED, partnerOrder.getCancelReason());
            } else {
                assertNull(partnerOrder.getCancelReason());
            }
            if (canBeProcessAsDamaged) {
                assertEquals(1, partnerOrder.getActions().size());
                assertEquals(MARK_AS_DAMAGED, partnerOrder.getActions().iterator().next());
            } else {
                assertNull(partnerOrder.getActions());
            }
            return null;
        });
    }

    private final LocationDto MOCK_WAREHOUSE_LOCATION = LocationDto.builder()
            .country("Россия")
            .region("Москва и Московская область")
            .locality("Котельники")
            .build();

    @Test
    void getPartnerOrderEventWithoutCreatedStatusInHistory() {
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);
        testFactory.storedFakeReturnDeliveryService();
        var fromWarehouse = WarehouseDto.builder()
                .type(WarehouseType.SORTING_CENTER.name())
                .yandexId("123123")
                .logisticPointId("123123")
                .incorporation("ООО фром мерчант")
                .location(MOCK_WAREHOUSE_LOCATION)
                .shopId("123")
                .build();
        var returnWarehouse = WarehouseDto.builder()
                .type(WarehouseType.SHOP.name())
                .yandexId("222222")
                .logisticPointId("222222")
                .incorporation("ООО ретурн мерчант")
                .location(MOCK_WAREHOUSE_LOCATION)
                .shopId("333")
                .build();
        var orderId = orderCommandService.createReturn(CreateReturnRequest.builder()
                .sortingCenter(sortingCenter)
                .orderBarcode("externalId")
                .returnDate(LocalDate.now())
                .returnWarehouse(returnWarehouse)
                .fromWarehouse(fromWarehouse)
                .timeIn(Instant.now(clock))
                .timeOut(Instant.now(clock))
                .build()
        , testFactory.getOrCreateAnyUser(sortingCenter));

        transactionTemplate.execute(ts -> {
            var orderEvents = partnerOrderStatusEventService.getOrderItemsOrThrow(
                    sortingCenter, orderId.getExternalId());
            var actualOrder = scOrderRepository.findByIdOrThrow(orderId.getId());
            assertEquals(DateTimeUtil.toLocalDateTime(actualOrder.getCreatedAt()),
                    orderEvents.getCreateOrderDateTime());
            return null;
        });
    }


}
