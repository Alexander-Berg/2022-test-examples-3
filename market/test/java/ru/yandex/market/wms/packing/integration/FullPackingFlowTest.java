package ru.yandex.market.wms.packing.integration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.OrderStatus;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.MockTaskConsumer;
import ru.yandex.market.wms.packing.dto.CloseParcelRequest;
import ru.yandex.market.wms.packing.dto.PackingTaskConverter;
import ru.yandex.market.wms.packing.dto.PackingTaskDto;
import ru.yandex.market.wms.packing.dto.ScanItemRequest;
import ru.yandex.market.wms.packing.dto.ScanItemResponse;
import ru.yandex.market.wms.packing.enums.TicketType;
import ru.yandex.market.wms.packing.pojo.OrderPackingTask;
import ru.yandex.market.wms.packing.pojo.PackingTable;
import ru.yandex.market.wms.packing.pojo.PackingTaskItem;
import ru.yandex.market.wms.packing.pojo.Sku;
import ru.yandex.market.wms.packing.pojo.SortingCell;
import ru.yandex.market.wms.packing.service.PackingService;
import ru.yandex.market.wms.packing.worker.Manager;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.wms.common.spring.utils.EntityCreationUtils.SCALED_ONE;
import static ru.yandex.market.wms.common.utils.CollectionUtils.asSet;
import static ru.yandex.market.wms.packing.BaseDbObjects.CARRIER_MP1;
import static ru.yandex.market.wms.packing.BaseDbObjects.CARTON_YMB;
import static ru.yandex.market.wms.packing.BaseDbObjects.SKU101;
import static ru.yandex.market.wms.packing.BaseDbObjects.SKU201;
import static ru.yandex.market.wms.packing.LocationsRov.SS1_CELL1;
import static ru.yandex.market.wms.packing.LocationsRov.SS1_CELL2;

public class FullPackingFlowTest extends PackingIntegrationTest {

    public static final String ORDER_KEY = "ORD0777";
    public static final String LOT1 = "LOT1";
    public static final String LOT2 = "LOT2";
    public static final String LOT3 = "LOT3";
    public static final String LOT4 = "LOT4";
    public static final String LOT5 = "LOT5";

    public static final String USER1 = "TEST";

    @Autowired
    private PackingService packingService;

    @Autowired
    private Manager manager;

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/full_packing_flow/normal_flow/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/full_packing_flow/normal_flow/expected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void normalFlow()  {
        PackingTable table = LocationsRov.TABLE_1;

        MockTaskConsumer consumer = new MockTaskConsumer(table, USER1);
        manager.register(consumer);
        manager.requestTask(USER1, false);
        PackingTaskDto packingTask = consumer.getTask();
        long ticketId = packingTask.getTicket().getTicketId();
        var expectedTask = PackingTaskConverter.convertOrderTask(
                OrderPackingTask.builder()
                        .orderKey(ORDER_KEY)
                        .orderStatus(OrderStatus.PICKED_COMPLETE)
                        .carrier(CARRIER_MP1)
                        .items(expectedItems(ORDER_KEY))
                        .packedParcelsCount(0)
                        .build(),
                TicketType.SORTABLE
        );

        assertThat(packingTask).isNotNull();
        assertThat(packingTask.getOrderTasks()).hasSize(1);
        PackingTaskDto.OrderTaskDto task = packingTask.getOrderTasks().get(0);

        assertThat(task.getOrderKey()).isEqualTo(expectedTask.getOrderKey());
        assertThat(task.getOrderStatus()).isEqualTo(expectedTask.getOrderStatus());
        assertThat(task.getCarrier()).isEqualTo(expectedTask.getCarrier());
        assertThat(packingTask.getTicket().getSortingCells()).isEqualTo(asSet(LocationsRov.SS1_CELL1,
                LocationsRov.SS1_CELL2));
        assertThat(task.getItems()).containsExactlyInAnyOrderElementsOf(expectedTask.getItems());

        List<String> uids = Stream.iterate(1, i -> i + 1)
                .limit(20)
                .map(i -> String.format("UID%04d", i))
                .collect(Collectors.toList());
        ScanItemRequest firstScanItemReq = new ScanItemRequest(ticketId, uids.get(0), false);
        ScanItemResponse scanItemResponse = packingService.scanItem(firstScanItemReq);
        uids.stream().skip(1).forEach(uid ->
                packingService.scanItem(new ScanItemRequest(ticketId, uid, true)));

        CloseParcelRequest closeParcelRequest = CloseParcelRequest.builder()
                .ticketId(packingTask.getTicket().getTicketId())
                .orderKey(ORDER_KEY)
                .parcelId(scanItemResponse.getParcelId())
                .recommendedCartonId(scanItemResponse.getCartonId())
                .selectedCartonId(CARTON_YMB.getType())
                .printer("P01")
                .uids(uids)
                .build();
        packingService.closeParcel(closeParcelRequest);

        manager.requestTask(USER1, false);
        packingTask = consumer.getTask();
        assertNull(packingTask);
    }

    private List<PackingTaskItem> expectedItems(String orderKey) {
        // разные SKU, лоты, ячейки, количества
        int id = 0;
        return Arrays.asList(
                newItem(orderKey, ++id, SKU101, LOT1, SS1_CELL1),
                newItem(orderKey, ++id, SKU101, LOT1, SS1_CELL1),
                newItem(orderKey, ++id, SKU101, LOT1, SS1_CELL1),
                newItem(orderKey, ++id, SKU101, LOT1, SS1_CELL1),

                newItem(orderKey, ++id, SKU101, LOT1, SS1_CELL2),
                newItem(orderKey, ++id, SKU101, LOT1, SS1_CELL2),
                newItem(orderKey, ++id, SKU101, LOT1, SS1_CELL2),

                newItem(orderKey, ++id, SKU101, LOT2, SS1_CELL1),
                newItem(orderKey, ++id, SKU101, LOT2, SS1_CELL1),

                newItem(orderKey, ++id, SKU101, LOT2, SS1_CELL2),

                newItem(orderKey, ++id, SKU201, LOT3, SS1_CELL1),

                newItem(orderKey, ++id, SKU201, LOT3, SS1_CELL2),
                newItem(orderKey, ++id, SKU201, LOT3, SS1_CELL2),

                newItem(orderKey, ++id, SKU201, LOT4, SS1_CELL1),
                newItem(orderKey, ++id, SKU201, LOT4, SS1_CELL1),
                newItem(orderKey, ++id, SKU201, LOT4, SS1_CELL1),

                newItem(orderKey, ++id, SKU201, LOT4, SS1_CELL2),
                newItem(orderKey, ++id, SKU201, LOT4, SS1_CELL2),
                newItem(orderKey, ++id, SKU201, LOT4, SS1_CELL2),

                newItem(orderKey, ++id, SKU201, LOT5, SS1_CELL2)
        );
    }

    private static PackingTaskItem newItem(String orderKey, int id, Sku sku, String lot, SortingCell cell) {
        return PackingTaskItem.builder()
                .orderKey(orderKey)
                .uid(String.format("UID%04d", id))
                .sku(sku)
                .lot(lot)
                .qty(SCALED_ONE)
                .pickDetailKey(String.format("PD%04d", id))
                .sortingCell(cell)
                .build();
    }
}
