package ru.yandex.market.sc.core.domain.sortable;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.sortable.cell.SortableCellQueryService;
import ru.yandex.market.sc.core.domain.sortable.model.SortableCreateRequest;
import ru.yandex.market.sc.core.domain.sortable.model.enums.BarcodeType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DirectFlowType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableBarcodePlain;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SortableQueryServiceTest {

    private final SortableQueryService sortableQueryService;
    private final SortableCellQueryService sortableCellQueryService;
    private final TestFactory testFactory;
    private final SortableTestFactory sortableTestFactory;
    private final RegistryRepository registryRepository;
    private final XDocFlow flow;

    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    User user;
    Warehouse warehouse;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
        warehouse = testFactory.storedWarehouse();
        user = testFactory.storedUser(sortingCenter, TestFactory.USER_UID_LONG);
        testFactory.setupMockClock(clock);
    }

    @Test
    void getBarcodesPlaceSingle() {
        var order = testFactory.create(order(sortingCenter).externalId("o1").places("p1", "p2").build()).get();
        var place2 = testFactory.orderPlace(order, "p2");

        var sortableBarcodesList1 =
                sortableQueryService.getPlaceIdsHavingAllBarcodes(List.of("o1", "p2"), sortingCenter);
        assertThat(sortableBarcodesList1).hasSize(1);
        assertThat(sortableBarcodesList1.get(0).placeId()).isEqualTo(place2.getId());
        assertThat(sortableBarcodesList1.get(0).barcodes()).hasSize(2);
        assertThat(sortableBarcodesList1.get(0).barcodes())
                .usingElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .containsExactlyInAnyOrder(
                        new SortableBarcodePlain(sortingCenter.getId(), "o1", place2.getId(), null, BarcodeType.ORDER),
                        new SortableBarcodePlain(sortingCenter.getId(), "p2", place2.getId(), null, BarcodeType.PLACE)
                );

        var sortableBarcodesList2 =
                sortableQueryService.getPlaceIdsHavingAllBarcodes(List.of("p2"), sortingCenter);
        assertThat(sortableBarcodesList1).isEqualTo(sortableBarcodesList2);
    }

    @Test
    void getBarcodesPlaceMultiple() {
        var order = testFactory.create(order(sortingCenter).externalId("o1").places("p1", "p2").build()).get();
        var place1 = testFactory.orderPlace(order, "p1");
        var place2 = testFactory.orderPlace(order, "p2");
        var sortableBarcodesList = sortableQueryService.getPlaceIdsHavingAllBarcodes(List.of("o1"), sortingCenter);

        assertThat(sortableBarcodesList).hasSize(2);
        assertThat(sortableBarcodesList.get(0).placeId()).isEqualTo(place1.getId());
        assertThat(sortableBarcodesList.get(0).barcodes()).hasSize(2);
        assertThat(sortableBarcodesList.get(0).barcodes())
                .usingElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .containsExactlyInAnyOrder(
                        new SortableBarcodePlain(sortingCenter.getId(), "o1", place1.getId(), null, BarcodeType.ORDER),
                        new SortableBarcodePlain(sortingCenter.getId(), "p1", place1.getId(), null, BarcodeType.PLACE)
                );

        assertThat(sortableBarcodesList.get(1).placeId()).isEqualTo(place2.getId());
        assertThat(sortableBarcodesList.get(1).barcodes()).hasSize(2);
        assertThat(sortableBarcodesList.get(1).barcodes())
                .usingElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .containsExactlyInAnyOrder(
                        new SortableBarcodePlain(sortingCenter.getId(), "o1", place2.getId(), null, BarcodeType.ORDER),
                        new SortableBarcodePlain(sortingCenter.getId(), "p2", place2.getId(), null, BarcodeType.PLACE)
                );
    }

    @Test
    void getBarcodesPlaceEmpty() {
        testFactory.create(order(sortingCenter).externalId("o1").places("p1", "p2").build()).get();
        var sortableBarcodesList = sortableQueryService.getPlaceIdsHavingAllBarcodes(List.of("p3"), sortingCenter);
        assertThat(sortableBarcodesList).isEmpty();
    }

    @Test
    void getBarcodesSingle() {
        createLot("SC_LOT_l1", "o1");
        var lot2 = createLot("SC_LOT_l2", "o1");

        var sortableBarcodesList1 =
                sortableQueryService.findAllForSortablesHavingAllBarcodes(List.of("o1", "SC_LOT_l2"), sortingCenter);
        assertThat(sortableBarcodesList1).hasSize(1);
        assertThat(sortableBarcodesList1.get(0).sortableId()).isEqualTo(lot2.getId());
        assertThat(sortableBarcodesList1.get(0).barcodes()).hasSize(2);
        assertThat(sortableBarcodesList1.get(0).barcodes())
                .usingElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .containsExactlyInAnyOrder(
                        new SortableBarcodePlain(sortingCenter.getId(), "o1", null, lot2.getId(), BarcodeType.ORDER),
                        new SortableBarcodePlain(sortingCenter.getId(), "SC_LOT_l2", null, lot2.getId(), BarcodeType.LOT)
                );

        var sortableBarcodesList2 =
                sortableQueryService.findAllForSortablesHavingAllBarcodes(List.of("SC_LOT_l2"), sortingCenter);
        assertThat(sortableBarcodesList1).isEqualTo(sortableBarcodesList2);
    }

    private Sortable createLot(String mainBarcode, String additionalBarcode) {
        return sortableTestFactory.create(
                SortableTestFactory.CreateSortableParams.builder()
                        .barcode(mainBarcode)
                        .additionalBarcodes(List.of(
                                new SortableCreateRequest.SortableBarcodeCreateRequest(
                                        additionalBarcode, BarcodeType.ORDER)
                        ))
                        .sortingCenter(sortingCenter)
                        .sortableType(SortableType.PALLET)
                        .directFlowType(DirectFlowType.TRANSIT)
                        .build()
        ).get();
    }

    @Test
    void getBarcodesMultiple() {
        var lot1 = createLot("SC_LOT_l1", "s1");
        var lot2 = createLot("SC_LOT_l2", "s1");
        var sortableBarcodesList = sortableQueryService.findAllForSortablesHavingAllBarcodes(List.of("s1"), sortingCenter);

        assertThat(sortableBarcodesList).hasSize(2);
        assertThat(sortableBarcodesList.get(0).sortableId()).isEqualTo(lot1.getId());
        assertThat(sortableBarcodesList.get(0).barcodes()).hasSize(2);
        assertThat(sortableBarcodesList.get(0).barcodes())
                .usingElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .containsExactlyInAnyOrder(
                        new SortableBarcodePlain(sortingCenter.getId(), "s1", null, lot1.getId(), BarcodeType.ORDER),
                        new SortableBarcodePlain(sortingCenter.getId(), "SC_LOT_l1", null, lot1.getId(), BarcodeType.LOT)
                );

        assertThat(sortableBarcodesList.get(1).sortableId()).isEqualTo(lot2.getId());
        assertThat(sortableBarcodesList.get(1).barcodes()).hasSize(2);
        assertThat(sortableBarcodesList.get(1).barcodes())
                .usingElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .containsExactlyInAnyOrder(
                        new SortableBarcodePlain(sortingCenter.getId(), "s1", null, lot2.getId(), BarcodeType.ORDER),
                        new SortableBarcodePlain(sortingCenter.getId(), "SC_LOT_l2", null, lot2.getId(), BarcodeType.LOT)
                );
    }

    @Test
    void getBarcodesEmpty() {
        createLot("SC_LOT_l1", "o1");
        createLot("SC_LOT_l2", "o1");
        var sortableBarcodesList = sortableQueryService.findAllForSortablesHavingAllBarcodes(List.of("SC_LOT_l3"), sortingCenter);
        assertThat(sortableBarcodesList).isEmpty();
    }

    @Test
    @DisplayName("Загрузка списка sortable для нескольких поставок")
    void loadByInbounds() {
        flow.createInbound("IN1")
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createInbound("IN-2")
                .linkPallets("XDOC-3", "XDOC-4")
                .fixInbound();

        List<Sortable> loaded = sortableQueryService.findByTypesAndStatuses(
                sortingCenter,
                Set.of(SortableType.XDOC_PALLET),
                Set.of(SortableStatus.ARRIVED_DIRECT, SortableStatus.SORTED_DIRECT)
        );
        assertThat(loaded).hasSize(4);
    }

    @Test
    @Transactional
    @DisplayName("Загрузка множества sortable для заданной поставки")
    void loadByInbound() {
        Inbound inbound = flow.createInbound("IN1")
                .linkPallets("XDOC-1")
                .getInbound();

        var sortables = sortableQueryService.find(inbound);

        assertThat(sortables)
                .hasSize(1)
                .anyMatch(sortable -> sortable.getRequiredBarcodeOrThrow().equalsIgnoreCase("XDOC-1"));

    }

    @Test
    @Transactional
    @DisplayName("Загрузка множества sortable для заданной отгрузки")
    void loadByOutbound() {
        Outbound outbound = flow.createInbound("IN1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound("OUT1")
                .addRegistryPallets("XDOC-1")
                .buildRegistry()
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1")
                .and()
                .getOutbound("OUT1");

        var sortables = sortableQueryService.find(outbound).stream()
                .filter(sortable -> Set.of(SortableStatus.SHIPPED_DIRECT, SortableStatus.PREPARED_DIRECT)
                        .contains(sortable.getStatus()))
                .collect(Collectors.toSet());

        assertThat(sortables)
                .hasSize(1)
                .anyMatch(sortable -> sortable.getRequiredBarcodeOrThrow().equals("XDOC-1"));

    }

    @Test
    @DisplayName("Загрузка списка sortable для реестра")
    void loadSortableByRegistry() {

        flow.createInbound("IN1")
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound("OUT1")
                .externalId("REG1")
                .addRegistryPallets("XDOC-1", "XDOC-2")
                .buildRegistry();

        Registry registry = registryRepository.findByExternalId("REG1").orElseThrow();

        Set<Sortable> sortables = sortableQueryService.findSortableByRegistriesIn(sortingCenter, Set.of(registry));

        assertThat(sortables)
                .hasSize(2)
                .anyMatch(sortable -> sortable.getRequiredBarcodeOrThrow().equalsIgnoreCase("XDOC-1"))
                .anyMatch(sortable -> sortable.getRequiredBarcodeOrThrow().equalsIgnoreCase("XDOC-2"));

    }

    @Test
    @DisplayName("Паллета не запакована")
    void testPalletNotPacked() {
        Sortable sortable = flow.createInbound("IN1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound("OUT1")
                .buildRegistry("XDOC-1")
                .and()
                .createBasketAndGet();

        assertThat(sortableCellQueryService.isReadyToSortOut(sortable)).isFalse();
    }

    @Test
    @DisplayName("Паллета запакована")
    @Transactional
    void testPalletPacked() {
        Sortable sortable = flow.createBasketAndGet();
        flow.createInbound("IN1")
                .linkBoxes("XDOC-1")
                .fixInbound()
                .createOutbound("OUT1")
                .addRegistryBoxes("XDOC-1")
                .buildRegistry()
                .sortToAvailableLot("XDOC-1")
                .packLot(sortable.getRequiredBarcodeOrThrow());
        assertThat(sortableCellQueryService.isReadyToSortOut(sortable)).isTrue();
    }

}
