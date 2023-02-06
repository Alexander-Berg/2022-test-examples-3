package ru.yandex.market.wms.packing.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao;
import ru.yandex.market.wms.packing.dto.CheckParcelResponse;
import ru.yandex.market.wms.packing.dto.CloseDropRequest;
import ru.yandex.market.wms.packing.dto.CloseParcelRequest;
import ru.yandex.market.wms.packing.dto.CloseParcelResponse;
import ru.yandex.market.wms.packing.dto.CloseParcelResponse.CreateSorterOrderState;
import ru.yandex.market.wms.packing.dto.DropParcelRequest;
import ru.yandex.market.wms.packing.dto.HotContainersResponse;
import ru.yandex.market.wms.packing.dto.ScanItemsContainerResponse;
import ru.yandex.market.wms.packing.enums.TicketType;
import ru.yandex.market.wms.packing.pojo.PackingTable;
import ru.yandex.market.wms.packing.pojo.PackingTask;
import ru.yandex.market.wms.packing.pojo.TicketKey;

import static ru.yandex.market.wms.packing.utils.TestCollectionUtils.head;
import static ru.yandex.market.wms.packing.utils.TestCollectionUtils.tail;

@Scope("prototype")
@Component
@RequiredArgsConstructor
public class PackingFlow {

    private final PackingAssertion assertion;
    private final OrderDao orderDao;
    private final PackingWebsocket socket;

    public PackingFlow connect(String user, PackingTable table) throws Exception {
        socket.connect(user, table);
        return this;
    }

    @SneakyThrows
    public void disconnect() {
        socket.disconnect();
    }

    public PackingFlow packSortable(PackingTaskDataset dataset) {
        Map<String, Set<String>> uitsByOrderKey = getUitsByOrderKey(dataset);
        if (uitsByOrderKey.size() != 1) {
            throw new IllegalArgumentException("Sortable task must contain single order");
        }

        String orderKey = uitsByOrderKey.keySet().iterator().next();
        PackingTask task = socket.getTask();
        assertion.assertUserHasSortableTask(socket.getUser(), task.getTicket().getSourceLoc(), orderKey);
        assertion.assertTaskHasUits(task, uitsByOrderKey);

        scanAndPack(dataset, task.getTicket().getTicketId(), true);
        return this;
    }

    public PackingFlow packNonsort(String containerId, PackingTaskDataset dataset) {
        Map<String, Set<String>> uitsByOrderKey = getUitsByOrderKey(dataset);

        PackingTask task;
        if (containerId != null) {
            socket.hotContainers(new HotContainersResponse(List.of()));
            task = socket.getTask(containerId);
        } else {
            task = socket.getTask();
        }

        TicketKey ticketKey = task.getTicket().getTicketKey();
        TicketType ticketType = task.getTicket().getType();
        assertion.assertTaskHasUits(task, uitsByOrderKey);
        assertion.assertUserHasNonsortTask(socket.getUser(), ticketKey.getLoc(), ticketKey.getId(), ticketType);

        scanAndPack(dataset, task.getTicket().getTicketId(), true);
        return this;
    }

    public void packNonsort(PackingTaskDataset dataset) {
        packNonsort(null, dataset);
    }

    public PackingFlow packBigWithdrawal(String containerId, PackingTaskDataset dataset,
                                         boolean expectContainerCanBeUsedAsParcel, boolean taskContainerWillBeEmpty) {
        Map<String, Set<String>> uitsByOrderKey = getUitsByOrderKey(dataset);

        PackingTask task = socket.getTask(containerId);

        TicketKey ticketKey = task.getTicket().getTicketKey();
        TicketType ticketType = task.getTicket().getType();
        assertion.assertTaskHasUits(task, uitsByOrderKey, dataset.getStuckUits());
        assertion.assertUserHasNonsortTask(socket.getUser(), ticketKey.getLoc(), ticketKey.getId(), ticketType);
        assertion.assertTaskContainerMayBeUsedAsParcelState(task, expectContainerCanBeUsedAsParcel);
        // big withdrawals have nonsort flow, but parcel is not closed automatically for each item
        scanAndPack(dataset, task.getTicket().getTicketId(), taskContainerWillBeEmpty);
        return this;
    }

    public PackingFlow packBigWithdrawalByContainer(String containerId, PackingTaskDataset dataset) {
        Map<String, Set<String>> uitsByOrderKey = getUitsByOrderKey(dataset);
        PackingTask task = socket.getTask(containerId);
        TicketKey ticketKey = task.getTicket().getTicketKey();
        TicketType ticketType = task.getTicket().getType();
        Long ticketId = task.getTicket().getTicketId();
        String orderKey = ticketKey.getOrderKey();
        assertion.assertTaskHasUits(task, uitsByOrderKey, dataset.getStuckUits());
        assertion.assertUserHasNonsortTask(socket.getUser(), ticketKey.getLoc(), ticketKey.getId(), ticketType);
        assertion.assertTaskContainerMayBeUsedAsParcelState(task, true);

        ScanItemsContainerResponse scanResponse = socket.scanContainerToUseItAsParcel(ticketId, containerId);

        Parcel newParcel = Parcel.builder()
                .parcelId(scanResponse.getParcelId())
                .orderKey(orderKey)
                .carton(scanResponse.getCartonId())
                .uits(new ArrayList<>(uitsByOrderKey.get(orderKey)))
                .parcelNumber(1)
                .isLast()
                .build();

        CloseParcelRequest parcelRequest = toCloseParcelRequest(ticketId, newParcel);
        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));

        Order order = orderDao.findOrderByOrderKey(newParcel.getOrderKey()).get();
        String externOrderKey = order.getExternalOrderKey();
        Assertions.assertThat(externOrderKey).isNotBlank();

        assertion.assertParcelLabel(order, newParcel.getParcelId(), newParcel.getParcelNumber(), newParcel.isLast());
        return this;
    }

    public PackingFlow closeDrop(String dropId) {
        socket.closeDrop(new CloseDropRequest(dropId));
        return this;
    }

    private void scanAndPack(PackingTaskDataset dataset, long ticketId, boolean expectFinishWithIdleState) {
        for (Parcel parcel : dataset.getParcels()) {
            List<String> uits = parcel.getUits();
            String parcelId = parcel.getParcelId();
            socket.scanFirstItemIntoParcel(
                    ticketId,
                    head(uits),
                    parcel.getCarton(),
                    parcelId,
                    parcel.isShouldCloseParcel());
            if (uits.size() > 1) {
                socket.scanItemsIntoOpenParcel(ticketId, tail(uits));
            }
            CloseParcelRequest parcelRequest = toCloseParcelRequest(ticketId, parcel);
            socket.checkParcel(parcelRequest, new CheckParcelResponse(0));

            if (parcel.isWithDropping()) {
                socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.DROP,
                        List.of(), List.of(), parcel.getScanDropInfo()));
                if (parcel.getScanDropInfo() != null) {
                    socket.dropParcel(new DropParcelRequest(parcelId, parcel.getScannedDropId()));
                }
            } else {
                socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
            }
            Order order = orderDao.findOrderByOrderKey(parcel.getOrderKey()).get();
            String externOrderKey = order.getExternalOrderKey();
            Assertions.assertThat(externOrderKey).isNotBlank();
            int parcelNumber = parcel.getParcelNumber();
            assertion.assertParcelLabel(order, parcelId, parcelNumber, parcel.isLast());
            if (parcel.getValidationAfterParcelClosed() != null) {
                parcel.getValidationAfterParcelClosed().run();
            }
        }
        if (expectFinishWithIdleState) {
            assertion.assertUserIsIdle(socket.getUser(), socket.getTable());
        } else {
            PackingTask task = socket.getTask();
            assertion.assertTaskHasUits(task, Collections.emptyMap(), dataset.getStuckUits());
        }
    }


    private static Map<String, Set<String>> getUitsByOrderKey(PackingTaskDataset dataset) {
        Map<String, Set<String>> uitsByOrderKey = new HashMap<>();
        for (Parcel parcel : dataset.getParcels()) {
            uitsByOrderKey.computeIfAbsent(parcel.getOrderKey(), k -> new HashSet<>()).addAll(parcel.getUits());
        }
        return uitsByOrderKey;
    }

    private static CloseParcelRequest toCloseParcelRequest(long ticketId, Parcel parcel) {
        return CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(parcel.getOrderKey())
                .parcelId(parcel.getParcelId())
                .recommendedCartonId(parcel.getCarton())
                .selectedCartonId(parcel.getCarton())
                .printer("P01")
                .uids(parcel.getUits())
                .build();
    }
}
