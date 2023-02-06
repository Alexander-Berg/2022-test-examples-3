package ru.yandex.market.sc.internal.controller.manual;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ManualLotControllerTest {

    private static final long UID = 123L;

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final ScOrderRepository scOrderRepository;
    private final XDocFlow flow;
    private final SortableQueryService sortableQueryService;
    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.storedUser(sortingCenter, UID);
    }

    @Test
    void getLotByExternalId() throws Exception {
        Cell parentCell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        mockMvc.perform(
                get("/manual/lots/external")
                        .param("externalId", lot.getBarcode())
                        .param("scId", sortingCenter.getId().toString())
        )
                .andExpect(status().isOk())
                .andExpect(content().json(String.format("""
                                {
                                "id": %s,
                                "type": "PALLET",
                                "externalId": "%s",
                                "name": "SC_LOT_100000 r1",
                                "status": "CREATED",
                                "sortableStatus": "KEEPED_RETURN",
                                "category": "DEFAULT",
                                "actions": [],
                                "labelCanBePrinted": false,
                                "transferable": false
                                }""", lot.getLotId(), lot.getBarcode()),
                        true)
                );
    }

    @Test
    void shipLot() throws Exception {
        Cell parentCell = testFactory.storedCell(sortingCenter, "r1", CellType.COURIER);
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET,  parentCell);
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort(parentCell.getId()).get();
        testFactory.sortOrderToLot(order, lot, testFactory.storedUser(sortingCenter, 1L));
        testFactory.prepareToShipLot(lot);

        mockMvc.perform(
                        put("/manual/lots/ship")
                                .queryParam("externalLotId", lot.getBarcode())
                                .queryParam("scId", String.valueOf(sortingCenter.getId()))
                )
                .andExpect(status().isOk());

        lot = testFactory.getLot(lot.getLotId());
        order = testFactory.getOrder(order.getId());

        assertThat(lot.getOptLotStatus()).isEmpty();
        assertThat(lot.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        assertThat(order.getOrderStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
    }

    OrderLike initOrderEnv(Long id, String orderExternalId) {
        var courier = testFactory.storedCourier(id);
        testFactory.storedCell(sortingCenter, "c-" + id, CellType.COURIER, courier.getId());
        var order = testFactory.createForToday(order(sortingCenter, orderExternalId).build())
                .updateCourier(courier)
                .accept()
                .get();
        var logisticPoint = testFactory.storedTargetLogisticPoint(id, "test-pvz", "Street");
        order.setTargetLogisticPointId(logisticPoint.getId());
        return scOrderRepository.save(order);
    }

    @SneakyThrows
    @Test
    void getExternalIdById() {
        Cell cell = flow.createBufferCellAndGet("cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        var lot = flow.createBasket(cell);
        String barcode = sortableQueryService.find(lot.getSortableId()).map(Sortable::getRequiredBarcodeOrThrow).orElseThrow();
        mockMvc.perform(get("/manual/lots/externalId/" + lot.getLotId() + "?scId=" + flow.getSortingCenter().getId()))
                .andExpect(status().isOk())
                .andExpect(content().string("\"" + barcode + "\""));
    }
}
