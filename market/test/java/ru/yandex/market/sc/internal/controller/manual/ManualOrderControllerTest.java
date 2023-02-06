package ru.yandex.market.sc.internal.controller.manual;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DirectFlowType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseRepository;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ExtendWith(DefaultScUserWarehouseExtension.class)
public class ManualOrderControllerTest {

    private static final long UID = 123L;

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final SortableTestFactory sortableTestFactory;
    private final Clock clock;
    private final XDocFlow flow;

    private final SortableQueryService sortableQueryService;
    private final ScOrderRepository scOrderRepository;
    private final WarehouseRepository warehouseRepository;

    SortingCenter sortingCenter;
    Warehouse warehouse;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        warehouse = testFactory.storedWarehouse();
    }

    @Test
    @SneakyThrows
    void getXdocPalletTest() {
        Inbound inbound = createInbound();
        Sortable sortable = sortableTestFactory
                .storeSortable(sortingCenter, SortableType.XDOC_PALLET, DirectFlowType.TRANSIT, "XDOC-p1",
                        inbound, null)
                .get();

        Cell cell = sortableTestFactory.getBufferCell(sortingCenter, CellSubType.BUFFER_XDOC);

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/manual/orders")
                                .param("externalId", sortable.getRequiredBarcodeOrThrow())
                                .param("scId", sortingCenter.getId().toString())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(expectedCreated(sortable, cell.getId())));
    }

    @Test
    @SneakyThrows
    void sortXdocPalletTest() {
        var cell = testFactory.storedCell(sortingCenter, "c-5", CellType.COURIER, CellSubType.SHIP_XDOC);
        flow.createInbound("IN-1")
                .linkPallets("XDOC-p1")
                .fixInbound()
                .createOutbound("OUT-1")
                .buildRegistry("XDOC-p1")
                .sortToAvailableCell("XDOC-p1");

        Sortable sortable = sortableQueryService.find(sortingCenter, "XDOC-p1").orElseThrow();

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/orders/acceptAndSort")
                                .param("externalOrderId", sortable.getRequiredBarcodeOrThrow())
                                .param("cellId", cell.getId().toString())
                                .param("scId", sortingCenter.getId().toString())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk());

        Sortable after = sortableQueryService.find(sortingCenter, "XDOC-p1").orElseThrow();

        assertThat(after.getCellIdOrNull()).isEqualTo(cell.getId());
    }

    private Inbound createInbound() {
        return testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundType(InboundType.XDOC_FINAL)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .build());
    }

    private static String expectedCreated(Sortable sortable, long bufferCellId) {
        return String.format("""
                {
                  "id": %1$s,
                  "externalId": "XDOC-p1",
                  "status": "KEEP",
                  "warehouse": {
                    "id": 0,
                    "name": ""
                  },
                  "availableCells": [
                    {
                      "id": %2$s,
                      "status": "NOT_ACTIVE",
                      "type": "BUFFER",
                      "subType": "BUFFER_XDOC"
                    }
                  ]
                }""", sortable.getId(), bufferCellId);
    }

    @Test
    @SneakyThrows
    void createDemoOrderIsLastMile() {
        var sc = testFactory.storedSortingCenter(84L);
        var ds = testFactory.storedDeliveryService("123456", sc.getId(), true);
        var courier = testFactory.storedCourier(72348723L, "test-courier");
        var wh = testFactory.storedWarehouse("demo-wh");

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/orders/createDemo")
                                .param("courierId", courier.getId().toString())
                                .param("placesCnt", "1")
                                .param("scId", sc.getId().toString())
                                .param("warehouseYandexId", wh.getYandexId())
                                .param("deliveryServiceYandexId", ds.getYandexId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var orders = scOrderRepository.findAll();
        assertThat(orders).hasSize(1);

        var order = orders.get(0);
        assertThat(order.isMiddleMile()).isFalse();
        assertThat(order.getCourier()).isEqualTo(courier);
        assertThat(order.getDeliveryService()).isEqualTo(ds);
    }

    @Test
    @SneakyThrows
    void createDemoOrderIsMiddleMile() {
        var sc = testFactory.storedSortingCenter(81L);
        var ds = testFactory.storedDeliveryService("98765", sc.getId(), false);
        var wh = testFactory.storedWarehouse("demo-wh");

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/orders/createDemo")
                                .param("placesCnt", "1")
                                .param("scId", sc.getId().toString())
                                .param("warehouseYandexId", wh.getYandexId())
                                .param("deliveryServiceYandexId", ds.getYandexId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var orders = scOrderRepository.findAll();
        assertThat(orders).hasSize(1);

        var order = orders.get(0);
        assertThat(order.isMiddleMile()).isTrue();
        assertThat(order.getDeliveryService()).isEqualTo(ds);
    }

    @Test
    @SneakyThrows
    void createDemoOrderWithExternalOrderId() {
        var sc = testFactory.storedSortingCenter(811111L);
        var ds = testFactory.storedDeliveryService("3451111", sc.getId(), false);
        var wh = testFactory.storedWarehouse("demo-wh");
        var orderExternalId = "test-order-external-id";

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/orders/createDemo")
                                .param("placesCnt", "1")
                                .param("externalOrderId", orderExternalId)
                                .param("scId", sc.getId().toString())
                                .param("warehouseYandexId", wh.getYandexId())
                                .param("deliveryServiceYandexId", ds.getYandexId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var orders = scOrderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getExternalId()).isEqualTo(orderExternalId);
    }

    @Test
    @SneakyThrows
    void shipPlaceForce() {
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2", "p3", "p4").build())
                .cancel().acceptPlaces("p1", "p2", "p3", "p4").sortPlaces("p3", "p4")
                .get();

        performPlaceShipment(order, "p1").andExpect(status().isOk());
        performPlaceShipment(order, "p3").andExpect(status().isOk());

        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        assertThat(testFactory.orderPlace(order, "p1").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "p2").getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(testFactory.orderPlace(order, "p3").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "p4").getStatus()).isEqualTo(PlaceStatus.SORTED);

        performPlaceShipment(order, "p2").andExpect(status().isOk());
        performPlaceShipment(order, "p4").andExpect(status().isOk());

        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
    }

    @Test
    @SneakyThrows
    void copyToUtilizationForOnePlace() {
        var externalId = "o1";
        var scTo = testFactory.storedSortingCenter(811111L);
        var scFrom = testFactory.storedSortingCenter(811112L);
        ScOrder scOrder = testFactory.createForToday(order(scFrom).externalId(externalId).build())
                .cancel().accept().get();
        var warehouse = TestFactory.warehouse("1", "2", WarehouseType.UTILIZATOR);
        warehouseRepository.save(warehouse);
        testFactory.storedCell(scTo, CellType.RETURN, warehouse, null);
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/orders/copyToUtilization")
                                .param("externalOrderId", externalId)
                                .param("scIdFrom", scFrom.getId().toString())
                                .param("scIdTo", scTo.getId().toString())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk());
        ScOrder copyOrder = testFactory.findOrder(externalId, scTo);
        assertThat(copyOrder.getFfStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(copyOrder.getWarehouseReturn().getId()).isEqualTo(warehouse.getId());
        assertThat(copyOrder.getWarehouseReturn().getType()).isEqualTo(warehouse.getType());
        List<Place> places = testFactory.orderPlaces(copyOrder);
        places.forEach(place -> {
            assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
            assertThat(place.getWarehouseReturn().getId()).isEqualTo(warehouse.getId());
            assertThat(place.getWarehouseReturn().getType()).isEqualTo(warehouse.getType());
        });
    }

    @Test
    @SneakyThrows
    void copyToUtilizationFromTwoPlaces() {
        var externalId = "o1";
        var scTo = testFactory.storedSortingCenter(811111L);
        var scFrom = testFactory.storedSortingCenter(811112L);
        testFactory.createForToday(order(scFrom).places("p1", "p2").externalId(externalId).build())
                .cancel().accept().get();
        var warehouse = TestFactory.warehouse("1", "2", WarehouseType.UTILIZATOR);
        warehouseRepository.save(warehouse);
        testFactory.storedCell(scTo, CellType.RETURN, warehouse, null);
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/orders/copyToUtilization")
                                .param("externalOrderId", externalId)
                                .param("scIdFrom", scFrom.getId().toString())
                                .param("scIdTo", scTo.getId().toString())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk());
        ScOrder copyOrder = testFactory.findOrder(externalId, scTo);
        assertThat(copyOrder.getFfStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(copyOrder.getWarehouseReturn().getId()).isEqualTo(warehouse.getId());
        assertThat(copyOrder.getWarehouseReturn().getType()).isEqualTo(warehouse.getType());
        List<Place> places = testFactory.orderPlaces(copyOrder);
        places.forEach(place -> {
            assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
            assertThat(place.getWarehouseReturn().getId()).isEqualTo(warehouse.getId());
            assertThat(place.getWarehouseReturn().getType()).isEqualTo(warehouse.getType());
        });
    }

    private ResultActions performPlaceShipment(ScOrder order, String placeMainPartnerCode) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.post("/manual/orders/shipPlaceForce")
                        .param("scId", String.valueOf(order.getSortingCenterId()))
                        .param("externalOrderId", order.getExternalId())
                        .param("placeMainPartnerCode", placeMainPartnerCode)
                        .header("Authorization", "OAuth uid-" + UID)
        );
    }
}
