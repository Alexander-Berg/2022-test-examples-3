package ru.yandex.market.sc.core.util.flow.xdoc;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import ru.yandex.market.sc.core.domain.cell.CellQueryService;
import ru.yandex.market.sc.core.domain.cell.model.ApiCellDto;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.lot.model.PartnerLotRequestDto;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableTypePrefix;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableBarcodeSeq;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterProperty;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertyRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.user.repository.UserRepository;
import ru.yandex.market.sc.core.test.InvalidTestParameters;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

@Component
@AllArgsConstructor
public class XDocFlow {

    public static final String PALLET_PREFIX = SortableTypePrefix.XDOC_BASKET.getValue() + "p-";
    public static final String BOX_PREFIX = SortableTypePrefix.XDOC_BASKET.getValue() + "b-";

    private final ApplicationContext appContext;
    private final SortableQueryService sortableQueryService;
    private final UserRepository userRepository;
    private final SortableBarcodeSeq sortableBarcodeSeq;
    private final TestFactory testFactory;
    private final SortableTestFactory sortableTestFactory;
    private final SortingCenterPropertyRepository sortingCenterPropertyRepository;
    private final CellQueryService cellQueryService;
    private final FlowEngine flowEngine;

    public SortingCenter getSortingCenter() {
        return testFactory.getSortingCenterById(TestFactory.SC_ID);
    }

    public User getUser() {
        return userRepository.findByUid(TestFactory.USER_UID_LONG).orElseThrow(
                () -> new TplIllegalStateException("No user is present by id " + TestFactory.USER_UID_LONG)
        );
    }

    public InboundCreation inboundBuilder(String externalId) {
        var inboundCreation = appContext.getBean(InboundCreation.class);
        inboundCreation.externalId(externalId);
        return inboundCreation;
    }

    public InboundArrival toArrival(String externalId) {
        var inboundCreation = appContext.getBean(InboundCreation.class);
        inboundCreation.externalId(externalId);
        return inboundCreation.toArrival();
    }

    public Inbound getInbound(String externalId) {
        return toArrival(externalId).getInbound();
    }

    public InboundArrival createInbound(String externalId) {
        var inboundCreation = appContext.getBean(InboundCreation.class);
        inboundCreation.externalId(externalId);
        return inboundCreation.build();
    }

    public Inbound createInboundAndGet(String externalId) {
        var inboundCreation = appContext.getBean(InboundCreation.class);
        inboundCreation.externalId(externalId);
        return inboundCreation.createAndGet();
    }

    public Inbound createInboundAndGet(String externalId, String informationListBarcode, String nextLogisticPoint) {
        var inboundCreation = appContext.getBean(InboundCreation.class);
        inboundCreation.externalId(externalId)
                .informationListBarcode(informationListBarcode)
                .nextLogisticPoint(nextLogisticPoint);
        return inboundCreation.createAndGet();
    }

    public Sortable createSortableAndGet(String barcode, SortableType sortableType, Inbound inbound) {
        var sortableCreation = appContext.getBean(SortableCreation.class);
        sortableCreation.barcode(barcode).sortableType(sortableType).inbound(inbound);
        return sortableCreation.createAndGet();
    }

    public SortableLot createBasket(Cell bufferCell) {
        var lotCreation = appContext.getBean(LotCreation.class);
        lotCreation.bufferCell(bufferCell);
        return lotCreation.createAndGet();
    }

    public void sortBoxToLot(Sortable box, SortableLot lot) {
        sortableTestFactory.lotSort(box.getId(), lot.getBarcode(), getUser());
    }

    public XDocFlow sortToAvailableCell(String... barcodes) {
        var user = testFactory.findUserByUid(TestFactory.USER_UID_LONG);
        for (String barcode : barcodes) {
            var orderDto = sortableTestFactory.getOrder(barcode, user);
            var cellDto = Optional.ofNullable(orderDto.getAvailableCells())
                    .orElseGet(Collections::emptyList).stream()
                    .findFirst()
                    .orElseThrow();
            long cellId = cellDto.getId();
            // Can't use here SortingCenterPropertySource because it has to be set explicitly
            Optional<SortingCenterProperty> sortingCenterProperty =
                    sortingCenterPropertyRepository.findBySortingCenterIdAndKey(user.getSortingCenter().getId(),
                            SortingCenterPropertiesKey.ENABLE_BUFFER_XDOC_LOCATION);
            if (sortingCenterProperty.isPresent() && sortingCenterProperty.get().getValue().equalsIgnoreCase("true")) {
                cellId = cellQueryService.getCellsByZone(user.getSortingCenter(),
                                orderDto.getAvailableCells().stream()
                                        .map(ApiCellDto::getId)
                                        .findAny()
                                        .orElseThrow()
                        )
                        .stream()
                        .findAny()
                        .orElseThrow()
                        .getId();
            }

            flowEngine.sort(orderDto.getExternalId(), cellId);
        }
        return this;
    }

    public Cell createBufferCellAndGet(String number, String warehouseYandexId) {
        var cellCreation = appContext.getBean(CellCreation.class);
        cellCreation.number(number)
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_XDOC)
                .warehouseYandexId(warehouseYandexId);
        return cellCreation.createAndGet();
    }

    public Cell createBufferCellBoxAndGet(String number, String warehouseYandexId) {
        var cellCreation = appContext.getBean(CellCreation.class);
        cellCreation.number(number)
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_XDOC_BOX)
                .warehouseYandexId(warehouseYandexId);
        return cellCreation.createAndGet();
    }

    public Cell createBufferCellLocationAndGet(String number, String warehouseYandexId) {
        var cellCreation = appContext.getBean(CellCreation.class);
        cellCreation.number(number)
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_XDOC_LOCATION)
                .warehouseYandexId(warehouseYandexId);
        return cellCreation.createAndGet();
    }


    public Cell createShipCellAndGet(String number) {
        var cellCreation = appContext.getBean(CellCreation.class);
        cellCreation.number(number)
                .type(CellType.COURIER)
                .subType(CellSubType.SHIP_XDOC);
        return cellCreation.createAndGet();
    }

    public OutboundCreation outboundBuilder(String externalId) {
        var outboundCreation = appContext.getBean(OutboundCreation.class);
        outboundCreation.externalId(externalId);
        return outboundCreation;
    }

    public RegistryCreation createOutbound(String externalId) {
        return outboundBuilder(externalId).toRegistryBuilder();
    }

    public RegistryCreation createOutbound(String externalId, Instant time) {
        return outboundBuilder(externalId).toRegistryBuilder(time, time);
    }

    public Outbound getOutbound(String externalId) {
        return outboundBuilder(externalId).get();
    }

    public Sortable createBasketAndGet() {
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        Cell cell = testFactory.storedCell(sc, CellType.BUFFER, CellSubType.BUFFER_XDOC, null);
        return createBasketAndGet(cell);
    }

    public Sortable createBasketAndGet(Cell cell) {
        long currentId = sortableBarcodeSeq.getId();
        flowEngine.createBasket(new PartnerLotRequestDto(cell.getId(), 1));

        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        return sortableQueryService.findOrThrow(sc, SortableTypePrefix.XDOC_BASKET.getValue() + (currentId + 1));
    }

    public static Set<String> generateBarcodes(int amount, String prefix) {
        validateAmount(amount);
        return IntStream.range(0, amount)
                .mapToObj(next -> prefix + next)
                .collect(Collectors.toSet());
    }

    public void prepareToShip(SortableAPIAction action, SortingCenter sc, String barcode) {
        var sortable = sortableQueryService.findOrThrow(sc, barcode);
        flowEngine.preship(sortable.getId(), sortable.getType(), action);
    }

    public void packLot(String... barcodes) {
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        for (String barcode : barcodes) {
            prepareToShip(SortableAPIAction.READY_FOR_PACKING, sc, barcode);
        }
    }

    public void prepareLot(String... barcodes) {
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        for (String barcode : barcodes) {
            prepareToShip(SortableAPIAction.READY_FOR_SHIPMENT, sc, barcode);
        }
    }

    private static void validateAmount(int amount) {
        if (amount <= 0) {
            throw new InvalidTestParameters("Положительное число коробок или палет можно прилинковать в поставке, а " +
                    "было " + amount);
        }
    }

}
