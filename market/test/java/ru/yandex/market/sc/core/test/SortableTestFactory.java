package ru.yandex.market.sc.core.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import lombok.Builder;
import lombok.Data;
import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.policy.DefaultCellDistributionPolicy;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.inbound.model.RegistryUnitType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortable;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortableRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.lot.LotCommandService;
import ru.yandex.market.sc.core.domain.lot.model.ApiSortableDto;
import ru.yandex.market.sc.core.domain.lot.model.PartnerLotRequestDto;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderDto;
import ru.yandex.market.sc.core.domain.outbound.OutboundCommandService;
import ru.yandex.market.sc.core.domain.outbound.model.CreateOutboundPlannedRegistryRequest;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.partner.lot.PartnerLotDto;
import ru.yandex.market.sc.core.domain.route_so.RouteSoQueryService;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSo;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoSite;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogContext;
import ru.yandex.market.sc.core.domain.sortable.SortableCommandService;
import ru.yandex.market.sc.core.domain.sortable.SortableId;
import ru.yandex.market.sc.core.domain.sortable.SortableLockService;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.ApiSortableSortDto;
import ru.yandex.market.sc.core.domain.sortable.model.SortableCreateRequest;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DirectFlowType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;

/**
 * @author ogonek
 */
public class SortableTestFactory {

    @Autowired
    SortableCommandService sortableCommandService;

    @Autowired
    OutboundCommandService outboundCommandService;

    @Autowired
    SortableQueryService sortableQueryService;

    @Autowired
    RouteSoQueryService routeSoQueryService;

    @Autowired
    SortableLockService sortableLockService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    DefaultCellDistributionPolicy defaultCellDistributionPolicy;

    @Autowired
    SortableRepository sortableRepository;

    @Autowired
    TestFactory testFactory;

    @Autowired
    RegistryRepository registryRepository;

    @Autowired
    RegistrySortableRepository registrySortableRepository;

    @Autowired
    ScanService scanService;

    @Autowired
    LotCommandService lotCommandService;

    public Sortable getLotAsSortable(SortableLot lot) {
        if (lot.getSortableId() == null) {
            throw new IllegalArgumentException("This lot has no sortable");
        }
        return sortableRepository.findById(lot.getSortableId())
                .orElseThrow(() -> new IllegalStateException("Can not find sortable for id " + lot.getSortableId()));
    }

    @Transactional
    public void createOutboundRegistry(CreateOutboundRegistryParams params) {
        List<String> pallets = new ArrayList<>();
        List<String> boxes = new ArrayList<>();

        if (params.getSortables() != null && !params.getSortables().isEmpty()) {
            for (var sortable : params.getSortables()) {
                var unitType = sortable.getType().getUnitType();
                if (unitType == RegistryUnitType.BOX) {
                    boxes.add(sortable.getRequiredBarcodeOrThrow());
                } else if (unitType == RegistryUnitType.PALLET) {
                    pallets.add(sortable.getRequiredBarcodeOrThrow());
                } else {
                    throw new AssertionError(
                            "Попытка положить в реестр sortable не являющийся ни палетой ни коробкой: " + sortable
                    );
                }
            }
        } else {
            pallets.addAll(params.getPallets());
            boxes.addAll(params.getBoxes());
        }
        outboundCommandService.putPlannedRegistry(CreateOutboundPlannedRegistryRequest.builder()
                .sortingCenter(params.getSortingCenter())
                .registryExternalId(params.registryExternalId)
                .outboundExternalId(params.outboundExternalId)
                .palletExternalIds(pallets)
                .boxExternalIds(boxes)
                .build());
    }

    @Transactional
    public List<Sortable> prepareSortablesPlan(Outbound outbound,
                                               Inbound inbound,
                                               String externalIdPrefix,
                                               int numberOfSortables,
                                               SortableType sortableType,
                                               RegistryUnitType registryUnitType) {

        Registry registry = testFactory.bindRegistry(outbound.getExternalId(), RegistryType.PLANNED);

        List<SortableId> sortableIds = new ArrayList<>(numberOfSortables);
        for (int i = 0; i < numberOfSortables; i++) {
            sortableIds.add(createSortable(
                    externalIdPrefix + i,
                    sortableType,
                    inbound,
                    inbound.getSortingCenter())
            );
        }

        List<Sortable> sortables =
                sortableQueryService.findAllHavingAllBarcodes(
                        outbound.getSortingCenter(),
                        sortableIds.stream()
                                .map(SortableId::getBarcode)
                                .toList()
                );

        List<RegistrySortable> registrySortables = StreamEx.of(sortables)
                .map(
                        sortableId -> new RegistrySortable(
                                registry,
                                sortableId.getRequiredBarcodeOrThrow(),
                                registryUnitType
                        ))
                .toList();


        sortableRepository.saveAll(sortables);
        registryRepository.save(registry);
        registrySortableRepository.saveAll(registrySortables);

        entityManager.flush();

        outbound.setRegistries(List.of(registry));
        sortableCommandService.fillRoutes(outbound, registrySortables);

        return new ArrayList<>(sortables);
    }

    @Transactional
    public Long getAnyShipCellId(Outbound outbound) {
        RouteSo routeSo = routeSoQueryService.getRouteByOutbound(outbound);
        return StreamEx.of(routeSo.getRouteSoSites())
                .map(RouteSoSite::getCell)
                .map(Cell::getId)
                .findAny().orElse(null);
    }

    @Transactional
    public void prepareSortablesPlanAndFact(Outbound outbound,
                                            Inbound inbound,
                                            String externalIdPrefix,
                                            int numberOfSortables,
                                            SortableType sortableType,
                                            RegistryUnitType registryUnitType) {

        List<Sortable> sortables = prepareSortablesPlan(
                outbound,
                inbound,
                externalIdPrefix,
                numberOfSortables,
                sortableType,
                registryUnitType
        );

        prepareSortablesFact(sortables);
    }

    @Transactional
    public void prepareSortablesFact(List<Sortable> sortables) {
        sortables.forEach(sortable ->
                sortable.setMutableState(sortable.getMutableState()
                        .withStatus(SortableStatus.PREPARED_DIRECT)
                )
        );

        sortableRepository.saveAll(sortables);
        entityManager.flush();
    }

    public SortableId createSortable(String barcode, SortableType sortableType, Inbound inbound,
                                     SortingCenter sortingCenter) {
        return sortableCommandService.create(SortableCreateRequest.builder()
                .barcode(barcode)
                .directFlow(DirectFlowType.TRANSIT)
                .type(sortableType)
                .sortingCenter(sortingCenter)
                .inbound(inbound)
                .build());
    }

    @Data
    @Builder
    public static class CreateOutboundRegistryParams {

        private SortingCenter sortingCenter;

        private String registryExternalId;

        private String outboundExternalId;

        private List<Sortable> sortables;

        @Builder.Default
        private Set<String> pallets = new HashSet<>();

        @Builder.Default
        private Set<String> boxes = new HashSet<>();

    }

    @Transactional
    public TestSortableBuilder create(CreateSortableParams params) {
        return new TestSortableBuilder().create(params);
    }

    @Transactional
    public TestSortableBuilder storeSimpleSortable(
            SortingCenter sortingCenter,
            SortableType sortableType,
            DirectFlowType directFlowType,
            String barcode
    ) {
        return storeSortable(sortingCenter, sortableType, directFlowType, barcode, null, null);
    }

    @Transactional
    public TestSortableBuilder storeSortable(
            SortingCenter sortingCenter,
            SortableType sortableType,
            DirectFlowType directFlowType,
            String barcode,
            @Nullable Inbound inbound,
            @Nullable User user
    ) {
        CreateSortableParams params = CreateSortableParams.builder()
                .barcode(barcode)
                .sortingCenter(sortingCenter)
                .sortableType(sortableType)
                .directFlowType(directFlowType)
                .inbound(inbound)
                .user(user)
                .build();
        return new TestSortableBuilder().create(params);
    }

    public RouteSo getRouteSo(Outbound outbound) {
        return routeSoQueryService.getRouteByOutbound(outbound);
    }

    @Transactional
    public Cell getAnyCell(Outbound outbound) {
        return routeSoQueryService.getRouteByOutbound(outbound).getRouteSoSites().stream().findFirst()
                .map(RouteSoSite::getCell)
                .orElseThrow(IllegalStateException::new);
    }

    @Transactional
    public Cell getBufferCell(SortingCenter sortingCenter, CellSubType type) {
        return defaultCellDistributionPolicy.findOrCreateFreeBufferCell(sortingCenter, type, false)
                .orElseThrow(IllegalStateException::new);
    }

    @Transactional
    public Cell createXdocBufferCell(SortingCenter sortingCenter) {
        Cell cell = defaultCellDistributionPolicy.findOrCreateFreeBufferCell(
                sortingCenter,
                CellSubType.BUFFER_XDOC,
                true
        ).orElseThrow(IllegalStateException::new);
        return cell;
    }

    public ApiOrderDto getOrder(String barcode) {
        var user = testFactory.findUserByUid(TestFactory.USER_UID_LONG);
        return getOrder(barcode, user);
    }

    public ApiOrderDto getOrder(String barcode, User user) {
        return scanService.getXdocSortableAsOrder(barcode, user);
    }

    public ApiSortableSortDto sortByBarcode(String barcode, long cellId) {
        var user = testFactory.findUserByUid(TestFactory.USER_UID_LONG);
        return scanService.sortSortable(
                new SortableSortRequestDto(barcode, barcode, String.valueOf(cellId)),
                new ScContext(user, ScanLogContext.SORT));
    }

    public ApiSortableSortDto sort(long sortableId, long cellId, User user) {
        Sortable sortable = sortableRepository.findByIdOrThrow(sortableId);
        String barcode = sortable.getRequiredBarcodeOrThrow();
        return scanService.sortSortable(
                new SortableSortRequestDto(barcode, barcode, String.valueOf(cellId)),
                new ScContext(user, ScanLogContext.SORT));
    }

    public ApiSortableSortDto lotSort(long sortableId, String lotBarcode, User user) {
        Sortable sortable = sortableRepository.findByIdOrThrow(sortableId);
        return lotSort(sortable.getRequiredBarcodeOrThrow(), lotBarcode, user);
    }

    public ApiSortableSortDto lotSort(String sortableBarcode, String lotBarcode, User user) {
        return scanService.sortSortable(
                new SortableSortRequestDto(sortableBarcode, sortableBarcode, lotBarcode),
                new ScContext(user, ScanLogContext.SORT_LOT));
    }

    public ApiSortableDto preship(long sortableId, SortableType type, SortableAPIAction action, User user) {
        return scanService.prepareToShipSortable(sortableId, type, action,
                new ScContext(user, ScanLogContext.PREPARE_SHIP));
    }

    public PartnerLotDto createEmptyLot(SortingCenter sortingCenter, Cell cell) {
        return createEmptyLot(sortingCenter, cell.getId());
    }

    public PartnerLotDto createEmptyLot(SortingCenter sortingCenter, long cellId) {
        return createEmptyLot(sortingCenter, new PartnerLotRequestDto(cellId, 1)).get(0);
    }

    public List<PartnerLotDto> createEmptyLot(SortingCenter sortingCenter, PartnerLotRequestDto partnerLotRequestDto) {
        return lotCommandService.createEmptyLots(sortingCenter, partnerLotRequestDto);
    }

    @Data
    @Builder
    public static class CreateSortableParams {

        private SortingCenter sortingCenter;

        @Builder.Default
        private SortableType sortableType = null;

        @Builder.Default
        private DirectFlowType directFlowType = null;

        @Builder.Default
        private String barcode = null;

        @Builder.Default
        private List<SortableCreateRequest.SortableBarcodeCreateRequest> additionalBarcodes = Collections.emptyList();

        @Builder.Default
        private Inbound inbound = null;

        @Builder.Default
        private User user = null;

    }

    public class TestSortableBuilder {

        private SortingCenter sortingCenter;

        private long sortableId;

        private String barcode;

        public Sortable get() {
            // for non-transactional tests
            return sortableQueryService.find(sortingCenter, barcode)
                    .orElseThrow(() -> new TplEntityNotFoundException(Sortable.class.getSimpleName(), barcode));
        }

        public TestSortableBuilder dummySortDirect() {
            return dummyChangeSortableStatus(SortableStatus.SORTED_DIRECT);
        }

        public TestSortableBuilder dummyPrepareDirect() {
            return dummyChangeSortableStatus(SortableStatus.PREPARED_DIRECT);
        }

        public TestSortableBuilder dummyChangeSortableStatus(SortableStatus target) {
            transactionTemplate.execute(ts -> {
                Sortable toAccept = sortableLockService.getRw(sortableId);
                toAccept.setStatus(target, null);
                entityManager.flush();
                return null;
            });
            return this;
        }

        private SortableCreateRequest sortableRequest(CreateSortableParams params) {
            return SortableCreateRequest.builder()
                    .barcode(params.getBarcode())
                    .additionalBarcodes(params.getAdditionalBarcodes())
                    .sortingCenter(params.getSortingCenter())
                    .type(params.getSortableType())
                    .directFlow(params.getDirectFlowType())
                    .barcode(params.getBarcode())
                    .inbound(params.getInbound())
                    .user(params.getUser())
                    .build();
        }

        private TestSortableBuilder create(CreateSortableParams params) {
            var sortable = sortableCommandService.create(sortableRequest(params));
            this.barcode = params.barcode;
            this.sortingCenter = params.sortingCenter;
            this.sortableId = sortable.getId();
            return this;
        }

    }

}
