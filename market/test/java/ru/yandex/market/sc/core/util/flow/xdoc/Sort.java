package ru.yandex.market.sc.core.util.flow.xdoc;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.springframework.stereotype.Component;

import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundRepository;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.SneakyResultActions;

@Component
@RequiredArgsConstructor
public class Sort {

    private final SortableRepository sortableRepository;
    private final SortableQueryService sortableQueryService;
    private final XDocFlow xDocFlow;
    private final FlowEngine flowEngine;
    private final SortableTestFactory sortableTestFactory;
    private final OutboundRepository outboundRepository;
    private final TestFactory testFactory;

    public Sort sortToAvailableCell(String... barcodes) {
        xDocFlow.sortToAvailableCell(barcodes);
        return this;
    }

    public Sort sortToAvailableLot(String... boxes) {
        var user = testFactory.findUserByUid(TestFactory.USER_UID_LONG);
        for (String box : boxes) {
            var orderDto = sortableTestFactory.getOrder(box, user);

            var lotDto = Optional.ofNullable(orderDto.getAvailableLots())
                    .orElseGet(Collections::emptyList).stream()
                    .findFirst()
                    .orElseThrow();
            flowEngine.lotSort(orderDto.getId(), lotDto.getExternalId());
        }
        return this;
    }

    public Sort prepareToShip(String... barcodes) {

        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        for (String barcode : barcodes) {
            prepareToShip(SortableAPIAction.READY_FOR_SHIPMENT, sc, barcode);
            var after = sortableQueryService.findOrThrow(sc, barcode);
            Assertions.assertEquals(after.getStatus(), SortableStatus.PREPARED_DIRECT,
                    "После подготовки статус должен быть PREPARED_DIRECT");
        }
        return this;
    }

    public void prepareToShip(SortableAPIAction action, SortingCenter sc, String barcode) {
        var sortable = sortableQueryService.findOrThrow(sc, barcode);
        flowEngine.preship(sortable.getId(), sortable.getType(),
                action);
    }

    public Sort packLot(String... barcodes) {
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        for (String barcode : barcodes) {
            prepareToShip(SortableAPIAction.READY_FOR_PACKING, sc, barcode);
        }
        return this;
    }

    public XDocFlow and() {
        return xDocFlow;
    }

    public Outbound shipAndGet(String outboundExternalId) {
        flowEngine.shipOutbound(outboundExternalId);
        return outboundRepository.findByExternalId(outboundExternalId).orElseThrow();
    }

    public void callShip(String outboundExternalId, Consumer<SneakyResultActions> resultActions) {
        flowEngine.callShipOutbound(outboundExternalId, resultActions);
    }
}
