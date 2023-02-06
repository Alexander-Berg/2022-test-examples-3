package ru.yandex.market.wms.packing.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.OrderStatus;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.packing.BaseDbObjects;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.LocationsSof;
import ru.yandex.market.wms.packing.enums.TicketType;
import ru.yandex.market.wms.packing.pojo.Carrier;
import ru.yandex.market.wms.packing.pojo.OrderPackingTask;
import ru.yandex.market.wms.packing.pojo.PackingTask;
import ru.yandex.market.wms.packing.pojo.PackingTaskItem;
import ru.yandex.market.wms.packing.pojo.Ticket;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.wms.common.spring.utils.EntityCreationUtils.SCALED_ONE;
import static ru.yandex.market.wms.common.utils.CollectionUtils.asSet;

public class PackingTaskServiceTest extends IntegrationTest {

    @Autowired
    private PackingTaskService packingTaskService;

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/dao/packing_task/get_packing_task/setup.xml", type = INSERT)
    void getPackingTask() {

        Ticket ticket = Ticket.builder()
                .orderKey("ORD0777")
                .type(TicketType.SORTABLE)
                .editDate(Instant.parse("2020-01-01T06:50:03Z"))
                .sourceLoc("SS1")
                .sortingCells(asSet(LocationsSof.SS1_CELL1, LocationsSof.SS1_CELL2))
                .build();

        Ticket ticketWithId = packingTaskService.assignTask(ticket, "TEST", "PACKTBL1");
        PackingTask packingTask = packingTaskService.getPackingTask(ticketWithId);

        List<PackingTaskItem> expectedItems = Arrays.asList(
                PackingTaskItem.builder()
                        .orderKey(ticket.getOrderKey())
                        .orderLineNumber("01")
                        .uid("UID0001")
                        .sku(BaseDbObjects.SKU101)
                        .lot("LOT1")
                        .qty(SCALED_ONE)
                        .pickDetailKey("PD0001")
                        .sortingCell(LocationsRov.SS1_CELL1)
                        .build(),
                PackingTaskItem.builder()
                        .orderKey(ticket.getOrderKey())
                        .orderLineNumber("02")
                        .uid("UID0002")
                        .sku(BaseDbObjects.SKU201)
                        .lot("LOT2")
                        .qty(SCALED_ONE)
                        .pickDetailKey("PD0002")
                        .sortingCell(LocationsRov.SS1_CELL1)
                        .build(),
                PackingTaskItem.builder()
                        .orderKey(ticket.getOrderKey())
                        .orderLineNumber("02")
                        .uid("UID0003")
                        .sku(BaseDbObjects.SKU201)
                        .lot("LOT2")
                        .qty(SCALED_ONE)
                        .pickDetailKey("PD0003")
                        .sortingCell(LocationsRov.SS1_CELL2)
                        .build()
        );
        OrderPackingTask expectedTask = OrderPackingTask.builder()
                .orderKey(ticket.getOrderKey())
                .orderStatus(OrderStatus.PICKED_COMPLETE)
                .carrier(BaseDbObjects.CARRIER_SP1)
                .items(expectedItems)
                .packedParcelsCount(0)
                .build();

        assertThat(packingTask.getOrderTasks()).hasSize(1);
        OrderPackingTask task = packingTask.getOrderTasks().get(0);

        assertThat(task.getOrderKey()).isEqualTo(expectedTask.getOrderKey());
        assertThat(task.getOrderStatus()).isEqualTo(expectedTask.getOrderStatus());
        assertThat(task.getCarrier()).isEqualTo(expectedTask.getCarrier());
        assertThat(task.getItems()).containsExactlyInAnyOrderElementsOf(expectedTask.getItems());
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/dao/packing_task/get_packing_task_withdrawal/setup.xml", type = INSERT)
    void getPackingTaskWithdrawal() {

        Ticket ticket = Ticket.builder()
                .orderKey("ORD0777")
                .type(TicketType.SORTABLE)
                .editDate(Instant.parse("2020-01-01T06:50:03Z"))
                .sourceLoc("SS1")
                .sortingCells(asSet(LocationsSof.SS1_CELL1, LocationsSof.SS1_CELL2))
                .build();

        Ticket ticketWithId = packingTaskService.assignTask(ticket, "TEST", "PACKTBL1");
        PackingTask packingTask = packingTaskService.getPackingTask(ticketWithId);

        List<PackingTaskItem> expectedItems = Arrays.asList(
                PackingTaskItem.builder()
                        .orderKey(ticket.getOrderKey())
                        .orderLineNumber("01")
                        .uid("UID0001")
                        .sku(BaseDbObjects.SKU101)
                        .lot("LOT1")
                        .qty(SCALED_ONE)
                        .pickDetailKey("PD0001")
                        .sortingCell(LocationsRov.SS1_CELL1)
                        .build(),
                PackingTaskItem.builder()
                        .orderKey(ticket.getOrderKey())
                        .orderLineNumber("02")
                        .uid("UID0002")
                        .sku(BaseDbObjects.SKU201)
                        .lot("LOT2")
                        .qty(SCALED_ONE)
                        .pickDetailKey("PD0002")
                        .sortingCell(LocationsRov.SS1_CELL1)
                        .build(),
                PackingTaskItem.builder()
                        .orderKey(ticket.getOrderKey())
                        .orderLineNumber("02")
                        .uid("UID0003")
                        .sku(BaseDbObjects.SKU201)
                        .lot("LOT2")
                        .qty(SCALED_ONE)
                        .pickDetailKey("PD0003")
                        .sortingCell(LocationsRov.SS1_CELL2)
                        .build()
        );
        OrderPackingTask expectedTask = OrderPackingTask.builder()
                .orderKey(ticket.getOrderKey())
                .orderStatus(OrderStatus.PICKED_COMPLETE)
                .carrier(Carrier.builder().cartonGroup("PK").supportsMultiPackaging(true).build())
                .items(expectedItems)
                .packedParcelsCount(0)
                .build();

        assertThat(packingTask.getOrderTasks()).hasSize(1);
        OrderPackingTask task = packingTask.getOrderTasks().get(0);

        assertThat(task.getOrderKey()).isEqualTo(expectedTask.getOrderKey());
        assertThat(task.getOrderStatus()).isEqualTo(expectedTask.getOrderStatus());
        assertThat(task.getCarrier()).isEqualTo(expectedTask.getCarrier());
        assertThat(task.getItems()).containsExactlyInAnyOrderElementsOf(expectedTask.getItems());
    }
}
