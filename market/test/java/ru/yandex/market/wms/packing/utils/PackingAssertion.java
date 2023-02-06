package ru.yandex.market.wms.packing.utils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.packing.dao.PackingTaskDao;
import ru.yandex.market.wms.packing.enums.ItemStatus;
import ru.yandex.market.wms.packing.enums.TicketType;
import ru.yandex.market.wms.packing.pojo.OrderPackingTask;
import ru.yandex.market.wms.packing.pojo.PackingTable;
import ru.yandex.market.wms.packing.pojo.PackingTask;
import ru.yandex.market.wms.packing.pojo.PackingTaskAssignment;
import ru.yandex.market.wms.packing.pojo.PackingTaskItem;
import ru.yandex.market.wms.packing.pojo.TicketKey;
import ru.yandex.market.wms.shared.libs.label.printer.service.printer.PrintServiceMock;

import static org.assertj.core.api.Assertions.assertThat;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class PackingAssertion {
    private final PackingTaskDao packingTaskDao;
    private final PrintServiceMock printService;

    public void assertTaskHasUits(PackingTask task, Map<String, Set<String>> expectedUitsByOrderKey) {
        assertTaskHasUits(task, expectedUitsByOrderKey, Collections.emptySet());
    }

    public void assertTaskHasUits(PackingTask task,
                                  Map<String, Set<String>> expectedUitsByOrderKey,
                                  Set<String> expectedOrderlessUids) {
        Set<String> orderlessUids = task.getOrderTasks().stream()
                .filter(orderPackingTask -> orderPackingTask.getOrderKey() == null)
                .flatMap(t -> t.getItems().stream().map(PackingTaskItem::getUid))
                .collect(Collectors.toSet());
        Map<String, Set<String>> uitsByOrderKey = task.getOrderTasks().stream()
                .filter(orderPackingTask -> orderPackingTask.getOrderKey() != null)
                .collect(Collectors.toMap(
                OrderPackingTask::getOrderKey,
                ot -> ot.getItems().stream().map(PackingTaskItem::getUid).collect(Collectors.toSet())
        ));
        assertThat(uitsByOrderKey).isEqualTo(expectedUitsByOrderKey);
        assertThat(orderlessUids).isEqualTo(expectedOrderlessUids);
    }

    public void assertTaskHasCancelledUitsWithNullOrder(PackingTask task, Set<String> expectedUits) {
        assertThat(task.getOrderTasks().stream().allMatch(ot -> ot.getItems()
                .stream().allMatch(i -> i.getItemStatus() == ItemStatus.CANCELLED))).isTrue();
        Map<String, Set<String>> uitsByOrderKey = task.getOrderTasks().stream().collect(Collectors.toMap(
                OrderPackingTask::getOrderKey,
                ot -> ot.getItems().stream().map(PackingTaskItem::getUid).collect(Collectors.toSet())
        ));
        assertThat(uitsByOrderKey.keySet()).size().isEqualTo(1);
        assertThat(uitsByOrderKey.keySet()).containsNull();
        assertThat(uitsByOrderKey.get(null)).isEqualTo(expectedUits);
    }

    public void assertTaskContainerMayBeUsedAsParcelState(PackingTask task, boolean usable) {
        assertThat(task.isValidForScanItemsContainer()).isEqualTo(usable);
    }

    public void assertParcelLabel(Order order, String parcelId, int parcelNumber, boolean isLast) {
        String label = printService.getQueue().remove();

        // выглядит как "N/N" для последнего парсела, "N/+" для не последнего.
        String parcelNumberString = String.format("^FD%s/%s", parcelNumber, isLast ? parcelNumber : "+");
        String checkQRCodeString = String.format("^FDQA,%s^FS", order.getExternalOrderKey());
        OrderType orderType = OrderType.of(order.getType());
        assertThat(label)
                .contains(orderType.isWithdrawal() ? order.getOrderKey() : order.getExternalOrderKey())
                .contains(parcelId)
                .contains(parcelNumberString)
                .doesNotContain(checkQRCodeString);
    }

    public void assertParcelQRLabel(Order order, String parcelId, int parcelNumber, boolean isLast) {
        String label = printService.getQueue().remove();

        String parcelNumberString = String.format("^FD%s/%s^FS", parcelNumber, isLast ? parcelNumber : "+");
        String chechQRCodeString = String.format("^FDQA,%s^FS", order.getExternalOrderKey());
        OrderType orderType = OrderType.of(order.getType());
        assertThat(label)
                .contains(orderType.isWithdrawal() ? order.getOrderKey() : order.getExternalOrderKey())
                .contains(parcelId)
                .contains(parcelNumberString)
                .contains(chechQRCodeString);
    }

    public void assertUserIsIdle(String user, PackingTable table) {
        TicketKey ticketKey = TicketKey.builder().loc(table.getLoc()).build();
        assertHasTask(user, ticketKey, TicketType.IDLE);
    }

    public void assertUserHasSortableTask(String user, String loc, String orderKey) {
        TicketKey ticketKey = TicketKey.builder().loc(loc).orderKey(orderKey).build();
        assertHasTask(user, ticketKey, TicketType.SORTABLE);
    }

    public void assertUserHasStuckTask(String user, String loc) {
        TicketKey ticketKey = TicketKey.builder().loc(loc).build();
        assertHasTask(user, ticketKey, TicketType.STUCK);
    }

    public void assertUserHasNonsortTask(String user, String loc, String id, TicketType type) {
        TicketKey ticketKey = TicketKey.builder().loc(loc).id(id).build();
        assertHasTask(user, ticketKey, type);
    }

    public void assertUserHasPromoTask(String user, String orderKey) {
        TicketKey ticketKey = TicketKey.builder().loc("").orderKey(orderKey).build();
        assertHasTask(user, ticketKey, TicketType.PROMO);
    }

    private void assertHasTask(String user, TicketKey ticketKey, TicketType type) {
        Long ticketId = packingTaskDao.getTicketId(ticketKey);
        assertThat(ticketId).isPositive();
        PackingTaskAssignment ass = packingTaskDao.findPackingTaskAssignment(ticketId).get();
        assertThat(ass.getType()).isEqualTo(type);
        assertThat(ass.getAssignee()).isEqualToIgnoringCase(user);
    }

}
