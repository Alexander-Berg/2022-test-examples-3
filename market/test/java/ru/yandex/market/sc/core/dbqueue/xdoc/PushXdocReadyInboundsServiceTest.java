package ru.yandex.market.sc.core.dbqueue.xdoc;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatusHistoryRepository;
import ru.yandex.market.sc.core.domain.lot.LotQueryService;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableJdbcRepository;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.SortingCenterQueryService;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.tm.SortingCenterPushStateToTmService;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.external.tm.TmClient;
import ru.yandex.market.sc.core.external.tm.model.PutScStateRequest;
import ru.yandex.market.sc.core.external.tm.model.TmBox;
import ru.yandex.market.sc.core.external.tm.model.TmCenterType;
import ru.yandex.market.sc.core.external.tm.model.TmPallet;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.QueryCountAssertion;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@EmbeddedDbTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
class PushXdocReadyInboundsServiceTest {

    private static final long UID = 123L;

    @Autowired
    private TestFactory testFactory;
    @Autowired
    private SortableQueryService sortableQueryService;
    @Autowired
    private SortableLotService sortableLotService;
    @Autowired
    private SortableRepository sortableRepository;
    @Autowired
    private LotQueryService lotQueryService;
    @Autowired
    private SortingCenterQueryService sortingCenterQueryService;
    @SpyBean
    private TmClient tmClient;
    @Autowired
    private SortableJdbcRepository sortableJdbcRepository;
    @Autowired
    private InboundStatusHistoryRepository inboundStatusHistoryRepository;

    @Autowired
    private XDocFlow flow;

    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    User user;
    PushXdocReadyInboundsService pushXdocReadyInboundsService;
    SortingCenterPushStateToTmService sortingCenterPushStateToTmService;

    SortingCenter sortingCenter2;
    Warehouse nextWarehouse;
    AboutInbound aboutInbound1;
    AboutInbound aboutInbound2;
    AboutInbound aboutInbound3;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter(12);
        user = testFactory.storedUser(sortingCenter, UID);
        testFactory.setupMockClock(clock);
        this.sortingCenterPushStateToTmService = new SortingCenterPushStateToTmService(
                sortableJdbcRepository,
                inboundStatusHistoryRepository,
                lotQueryService,
                sortingCenterQueryService,
                tmClient
        );
        this.pushXdocReadyInboundsService = new PushXdocReadyInboundsService(sortingCenterPushStateToTmService);

        this.sortingCenter2 = flow.getSortingCenter();
        this.nextWarehouse = testFactory.storedWarehouse("101");
        OffsetDateTime now = OffsetDateTime.now(clock);
        this.aboutInbound1 = new AboutInbound("TMU101", this.nextWarehouse.getYandexId(), now);
        this.aboutInbound2 = new AboutInbound("TMU102", this.nextWarehouse.getYandexId(), now);
        this.aboutInbound3 = new AboutInbound("TMU103", this.nextWarehouse.getYandexId(), now);
    }

    @Test
    void pushScState() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId("12324")
                .inboundType(InboundType.XDOC_TRANSIT)
                .fromDate(now)
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(now)
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .nextLogisticPointId("34")
                .confirmed(true)
                .build();
        Inbound inbound = testFactory.createInbound(params);
        testFactory.linkSortableToInbound(inbound, "XDOC-1", SortableType.XDOC_PALLET, user);
        testFactory.finishInbound(inbound);

        clearInvocations(tmClient);
        pushXdocReadyInboundsService.processPayload(new PushXdocReadyInboundsPayload("", sortingCenter.getId()));
        var expectedRequest = PutScStateRequest.ofPallets(List.of(
                new TmPallet("XDOC-1", 34, "12324", now, List.of(), TmCenterType.DISTRIBUTION_CENTER)));
        verify(tmClient, times(1)).putScState(eq(sortingCenter), eq(expectedRequest));
    }

    @Test
    void pushStateWithSinglePallet() {
        flow.inboundBuilder(aboutInbound1.inboundExternalId())
                .nextLogisticPoint(aboutInbound1.nextLogisticPoint())
                .build()
                .transitionToReadyToReceive()
                .linkPallets("XDOC-PALLET-1")
                .fixInbound();

        var expectedRequest = ofPallets(pallet("XDOC-PALLET-1", aboutInbound1));

        Mockito.clearInvocations(tmClient);
        sortingCenterPushStateToTmService.pushXDocState(this.sortingCenter2.getId());

        verify(tmClient, times(1)).putScState(eq(sortingCenter2), eq(expectedRequest));
    }

    @Test
    void pushStateWithSeveralPallets() {
        flow.inboundBuilder(aboutInbound1.inboundExternalId())
                .nextLogisticPoint(aboutInbound1.nextLogisticPoint())
                .build()
                .transitionToReadyToReceive()
                .linkPallets("XDOC-PALLET-1", "XDOC-PALLET-2", "XDOC-PALLET-3")
                .fixInbound();

        var expectedRequest = ofPallets(
                pallet("XDOC-PALLET-1", aboutInbound1),
                pallet("XDOC-PALLET-2", aboutInbound1),
                pallet("XDOC-PALLET-3", aboutInbound1)
        );

        Mockito.clearInvocations(tmClient);
        sortingCenterPushStateToTmService.pushXDocState(this.sortingCenter2.getId());

        ArgumentCaptor<PutScStateRequest> stateCaptor = ArgumentCaptor.forClass(PutScStateRequest.class);
        Mockito.verify(tmClient).putScState(ArgumentCaptor.forClass(SortingCenter.class).capture(),
                stateCaptor.capture());
        assertState(expectedRequest, stateCaptor.getValue());
    }

    @Test
    void pushStateWithPalletsAndLot() {
        flow.inboundBuilder(aboutInbound1.inboundExternalId())
                .nextLogisticPoint(aboutInbound1.nextLogisticPoint())
                .build()
                .transitionToReadyToReceive()
                .linkPallets("XDOC-PALLET-1", "XDOC-PALLET-2")
                .fixInbound();

        flow.inboundBuilder(aboutInbound2.inboundExternalId())
                .nextLogisticPoint(aboutInbound2.nextLogisticPoint())
                .build()
                .transitionToReadyToReceive()
                .linkBoxes("XDOC-BOX-1", "XDOC-BOX-2")
                .fixInbound();

        Cell cell = flow.createBufferCellAndGet("cell-1", nextWarehouse.getYandexId());
        Sortable basket = flow.createBasketAndGet(cell);
        SortableLot lot = sortableLotService.findBySortableId(basket.getId()).orElseThrow();
        Sortable box1 = sortableQueryService.find(sortingCenter, "XDOC-BOX-1").orElseThrow();
        Sortable box2 = sortableQueryService.find(sortingCenter, "XDOC-BOX-2").orElseThrow();
        flow.sortBoxToLot(box1, lot);
        flow.sortBoxToLot(box2, lot);
        flow.packLot(basket.getRequiredBarcodeOrThrow());

        var expectedRequest = ofPallets(
                pallet("XDOC-PALLET-1", aboutInbound1),
                pallet("XDOC-PALLET-2", aboutInbound1),
                lot(basket.getRequiredBarcodeOrThrow(), new Boxes(
                        aboutInbound2, List.of("XDOC-BOX-1", "XDOC-BOX-2")
                ))
        );

        Mockito.clearInvocations(tmClient);
        sortingCenterPushStateToTmService.pushXDocState(this.sortingCenter2.getId());

        ArgumentCaptor<PutScStateRequest> stateCaptor = ArgumentCaptor.forClass(PutScStateRequest.class);
        Mockito.verify(tmClient).putScState(ArgumentCaptor.forClass(SortingCenter.class).capture(),
                stateCaptor.capture());
        assertState(expectedRequest, stateCaptor.getValue());
    }

    @DisplayName("количество запросов не должно расти в зависимости от количества sortable")
    @Test
    void queryCountDependingOnNumberOfSortables() {
        org.junit.jupiter.api.Assertions.assertAll(
                () -> {
                    flow.inboundBuilder(aboutInbound1.inboundExternalId())
                            .nextLogisticPoint(aboutInbound1.nextLogisticPoint())
                            .build()
                            .transitionToReadyToReceive()
                            .linkPallets("XDOC-PALLET-1")
                            .fixInbound();
                    pushStateAndAssertQueryCount(4);
                    cancel("XDOC-PALLET-1");  // отменяем что бы они не попали в выборку
                },
                () -> {
                    flow.inboundBuilder(aboutInbound2.inboundExternalId())
                            .nextLogisticPoint(aboutInbound2.nextLogisticPoint())
                            .build()
                            .transitionToReadyToReceive()
                            .linkPallets("XDOC-PALLET-2-1", "XDOC-PALLET-2-2")
                            .fixInbound();
                    pushStateAndAssertQueryCount(4);
                    cancel("XDOC-PALLET-2-1", "XDOC-PALLET-2-2");
                },
                () -> {
                    flow.inboundBuilder(aboutInbound3.inboundExternalId())
                            .nextLogisticPoint(aboutInbound3.nextLogisticPoint())
                            .build()
                            .transitionToReadyToReceive()
                            .linkPallets("XDOC-PALLET-3-1", "XDOC-PALLET-3-2", "XDOC-PALLET-3-3")
                            .fixInbound();
                    pushStateAndAssertQueryCount(4);
                    cancel("XDOC-PALLET-3-1", "XDOC-PALLET-3-2", "XDOC-PALLET-3-3");
                }
        );
    }

    @DisplayName("количество запросов не должно расти в зависимости от количества inbound")
    @Test
    void queryCountDependingOnNumberOfInbounds() {
        org.junit.jupiter.api.Assertions.assertAll(
                () -> {
                    flow.inboundBuilder(aboutInbound1.inboundExternalId())
                            .nextLogisticPoint(aboutInbound1.nextLogisticPoint())
                            .build()
                            .transitionToReadyToReceive()
                            .linkPallets("XDOC-PALLET-1")
                            .fixInbound();
                    pushStateAndAssertQueryCount(4);
                },
                () -> {
                    flow.inboundBuilder(aboutInbound2.inboundExternalId())
                            .nextLogisticPoint(aboutInbound2.nextLogisticPoint())
                            .build()
                            .transitionToReadyToReceive()
                            .linkPallets("XDOC-PALLET-2")
                            .fixInbound();
                    pushStateAndAssertQueryCount(4);
                },
                () -> {
                    flow.inboundBuilder(aboutInbound3.inboundExternalId())
                            .nextLogisticPoint(aboutInbound3.nextLogisticPoint())
                            .build()
                            .transitionToReadyToReceive()
                            .linkPallets("XDOC-PALLET-3")
                            .fixInbound();
                    pushStateAndAssertQueryCount(4);
                }
        );
    }

    @DisplayName("количество запросов не должно расти в зависимости от количества inbound и sortable в них")
    @Test
    void queryCountTest() {
        flow.inboundBuilder(aboutInbound1.inboundExternalId())
                .nextLogisticPoint(aboutInbound1.nextLogisticPoint())
                .build()
                .transitionToReadyToReceive()
                .linkPallets("XDOC-PALLET-1")
                .fixInbound();

        pushStateAndAssertQueryCount(4);  // query c 1 палетой

        flow.inboundBuilder(aboutInbound2.inboundExternalId())
                .nextLogisticPoint(aboutInbound2.nextLogisticPoint())
                .build()
                .transitionToReadyToReceive()
                .linkPallets("XDOC-PALLET-2-1", "XDOC-PALLET-2-2")
                .fixInbound();

        pushStateAndAssertQueryCount(4);  // query c 2 палетами

        flow.inboundBuilder(aboutInbound3.inboundExternalId())
                .nextLogisticPoint(aboutInbound3.nextLogisticPoint())
                .build()
                .transitionToReadyToReceive()
                .linkPallets("XDOC-PALLET-3-1", "XDOC-PALLET-3-2", "XDOC-PALLET-3-3")
                .fixInbound();

        pushStateAndAssertQueryCount(4);  // query c 3 палетами
    }

    private void pushStateAndAssertQueryCount(int expectedQueryCount) {
        QueryCountAssertion.assertQueryCountEqual(expectedQueryCount, () -> {
            sortingCenterPushStateToTmService.pushXDocState(this.sortingCenter2.getId());
            return null;
        });
    }

    private void cancel(String... barcodes) {
        for (String barcode : barcodes) {
            var maybeSortable = sortableRepository.findBySortingCenterAndBarcode(this.sortingCenter2, barcode);
            if (maybeSortable.isPresent()) {
                var sortable = maybeSortable.get();
                sortable.setMutableState(sortable.getMutableState().withStatus(SortableStatus.CANCELLED));
                sortableRepository.save(sortable);
            }
        }
    }

    private PutScStateRequest ofPallets(TmPallet... pallets) {
        return PutScStateRequest.ofPallets(List.of(pallets));
    }

    private TmPallet pallet(String barcode, AboutInbound aboutInbound) {
        return new TmPallet(
                barcode,
                Long.parseLong(aboutInbound.nextLogisticPoint()),
                aboutInbound.inboundExternalId(),
                OffsetDateTime.now(clock),
                List.of(),
                TmCenterType.DISTRIBUTION_CENTER
        );
    }

    private TmPallet lot(String barcode, Boxes... boxes) {
        List<TmBox> tmBoxes = new ArrayList<>();
        for (Boxes b : Arrays.stream(boxes).toList()) {
            var aboutInbound = b.aboutInbound();
            for (String boxBarcode : b.barcodes()) {
                tmBoxes.add(new TmBox(
                        boxBarcode,
                        aboutInbound.inboundExternalId(),
                        aboutInbound.inboundDateTime()
                ));
            }
        }
        return new TmPallet(
                barcode,
                Long.parseLong(Arrays.stream(boxes).findAny().orElseThrow().aboutInbound().nextLogisticPoint()),
                null,
                null,
                tmBoxes,
                TmCenterType.DISTRIBUTION_CENTER
        );
    }

    private record Boxes(
            AboutInbound aboutInbound,
            List<String> barcodes
    ) {
    }

    private record AboutInbound(
            String inboundExternalId,
            String nextLogisticPoint,
            OffsetDateTime inboundDateTime
    ) {
    }

    private static void assertState(PutScStateRequest expected, PutScStateRequest actual) {
        sortCollections(expected);
        sortCollections(actual);
        assertThat(actual).isEqualTo(expected);
    }

    private static void sortCollections(PutScStateRequest request) {
        if (CollectionUtils.isNotEmpty(request.getPallets())) {
            var pallets = new ArrayList<>(request.getPallets());
            pallets.sort((o1, o2) -> StringUtils.compare(o1.getId(), o2.getId()));
            request.setPallets(pallets);

            for (TmPallet pallet : request.getPallets()) {
                if (CollectionUtils.isNotEmpty(pallet.getBoxes())) {
                    var boxes = new ArrayList<>(pallet.getBoxes());
                    boxes.sort((o1, o2) -> StringUtils.compare(o1.getId(), o2.getId()));
                    pallet.setBoxes(boxes);
                }
            }
        }
    }

}
