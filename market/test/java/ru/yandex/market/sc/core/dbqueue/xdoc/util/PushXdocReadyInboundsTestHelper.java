package ru.yandex.market.sc.core.dbqueue.xdoc.util;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import one.util.streamex.StreamEx;

import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatus;
import ru.yandex.market.sc.core.domain.lot.model.CreateLotRequest;
import ru.yandex.market.sc.core.domain.lot.repository.Lot;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.dbo.PlainInbound;
import ru.yandex.market.sc.core.domain.sortable.repository.dbo.PlainSortable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PushXdocReadyInboundsTestHelper {

    private List<Sortable> all = new ArrayList<>();
    private OffsetDateTime now = OffsetDateTime.now();

    public List<PlainSortable> getAll() {
        return all.stream().map(this::plain).toList();
    }

    public Inbound fixedInbound(Integer externalId) {
        return fixedInbound(externalId, "45");
    }

    public Inbound fixedInbound(Integer externalId, String nextLogisticPointId) {
        return inbound(externalId, InboundStatus.FIXED, nextLogisticPointId);
    }

    public Inbound arrivedInbound(Integer externalId) {
        return inbound(externalId, InboundStatus.ARRIVED, "45");
    }

    private Inbound inbound(Integer id, InboundStatus status, String nextLogisticPointId) {
        var result = mock(Inbound.class);
        when(result.getId()).thenReturn((long) id);
        when(result.getExternalId()).thenReturn(Long.toString(id));
        when(result.getInboundStatus()).thenReturn(status);
        when(result.getNextLogisticPointId()).thenReturn(nextLogisticPointId);
        when(result.getFromDate()).thenReturn(now);
        when(result.getToDate()).thenReturn(now);
        return result;
    }

    public Sortable pallet(long id, @Nullable Inbound inbound, Sortable... children) {
        return sortable(id, inbound, SortableType.XDOC_PALLET, children);
    }

    public Sortable basket(long id, @Nullable Inbound inbound, Sortable... children) {
        return sortable(id, inbound, SortableType.XDOC_BASKET, children);
    }

    public Map<Long, Lot> getDummyLots(LotStatus status) {
        return StreamEx.of(all)
                .toMap(Sortable::getId, s -> new Lot(new CreateLotRequest(
                        SortableType.PALLET,
                        null,
                        s.getSortingCenter(),
                        false,
                        status,
                        s.getRequiredBarcodeOrThrow(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        false
                        )));
    }


    public Sortable box(long id, @Nullable Inbound inbound, Sortable... children) {
        return sortable(id, inbound, SortableType.XDOC_BOX, children);
    }

    private Sortable sortable(long id, @Nullable Inbound inbound, SortableType type, Sortable... children) {
        var result = mock(Sortable.class);
        when(result.getId()).thenReturn(id);
        when(result.getRequiredBarcodeOrThrow()).thenReturn("XDOC-" + id);
        when(result.getInbound()).thenReturn(inbound);
        when(result.getType()).thenReturn(type);
        StreamEx.of(children).forEach(child -> when(child.getParent()).thenReturn(result));
        all.add(result);
        return result;
    }

    public PlainSortable plain(Sortable sortable) {
        PlainSortable parent = null;
        if (sortable.getParent() != null) {
            var parentSortable = sortable.getParent();

            PlainInbound parentInbound = null;
            if (parentSortable.getInbound() != null) {
                parentInbound = plain(parentSortable.getInbound());
            }
            parent = new PlainSortable(
                    parentSortable.getId(),
                    parentSortable.getBarcode(),
                    parentSortable.getStatus(),
                    parentSortable.getType(),
                    parentInbound,
                    null
            );
        }

        return new PlainSortable(
                sortable.getId(),
                sortable.getBarcode(),
                sortable.getStatus(),
                sortable.getType(),
                plain(sortable.getInbound()),
                parent
        );
    }

    @Nullable
    private PlainInbound plain(@Nullable Inbound inbound) {
        if (inbound == null) {
            return null;
        }
        return new PlainInbound(
                inbound.getId(),
                inbound.getExternalId(),
                inbound.getNextLogisticPointId(),
                inbound.getFromDate(),
                inbound.getInboundStatus(),
                inbound.getType()
        );
    }

}
