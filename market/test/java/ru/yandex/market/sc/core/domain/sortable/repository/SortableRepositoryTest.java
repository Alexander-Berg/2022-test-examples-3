package ru.yandex.market.sc.core.domain.sortable.repository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.route_so.model.RouteType;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSo;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoRepository;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.BarcodeType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DirectFlowType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SortableRepositoryTest {

    private final RouteSoRepository routeSoRepository;
    private final SortableRepository sortableRepository;
    private final TestFactory testFactory;
    private final SortableTestFactory sortableTestFactory;
    private final SortableQueryService sortableQueryService;
    private final Clock clock;

    SortingCenter sortingCenter;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void findBySortingCenterAndBarcode() {
        String barcode = "test";
        sortableTestFactory.create(
                SortableTestFactory.CreateSortableParams.builder()
                        .sortingCenter(sortingCenter)
                        .sortableType(SortableType.PLACE)
                        .barcode(barcode)
                        .build()
        );
        var place = sortableQueryService.findOrThrow(sortingCenter, barcode);
        assertThat(place.getType()).isEqualTo(SortableType.PLACE);
        assertThat(place.getRequiredBarcodeOrThrow()).isEqualTo(barcode);
    }

    @Test
    void getBarcodes() {
        var place = sortableTestFactory.create(
                SortableTestFactory.CreateSortableParams.builder()
                        .sortingCenter(sortingCenter)
                        .sortableType(SortableType.PLACE)
                        .barcode("test")
                        .build()
        ).get();
        assertThat(Hibernate.isInitialized(place.getBarcodes())).isTrue();
        assertThat(Hibernate.isInitialized(place.getHistory())).isFalse();
    }

    @Test
    void getBarcodeTypes() {
        var place = sortableTestFactory.create(
                SortableTestFactory.CreateSortableParams.builder()
                        .sortingCenter(sortingCenter)
                        .sortableType(SortableType.PLACE)
                        .barcode("test1")
                        .build()
        ).get();
        assertThat(place.getBarcodes().get(0).getBarcodeType()).isEqualTo(BarcodeType.PLACE);
        var lot = sortableTestFactory.create(
                SortableTestFactory.CreateSortableParams.builder()
                        .sortingCenter(sortingCenter)
                        .sortableType(SortableType.PALLET)
                        .barcode("SC_LOT_test2")
                        .build()
        ).get();
        assertThat(lot.getBarcodes().get(0).getBarcodeType()).isEqualTo(BarcodeType.LOT);
    }

    @Test
    void save() {
        var cell = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER);
        var courier = testFactory.storedCourier();
        var routeSo = routeSoRepository.save(new RouteSo(sortingCenter, RouteType.OUT_DIRECT,
                courier, null, null,
                Instant.now(clock).minus(2, ChronoUnit.HOURS),
                Instant.now(clock).plus(2, ChronoUnit.HOURS), "О868АС198"));
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1", "p2").sort(cell.getId()).get();
        var sortable = new Sortable(
                sortingCenter,
                "p1",
                SortableType.PLACE,
                SortableStatus.SORTED_DIRECT,
                LocalDate.now(clock),
                null,
                routeSo,
                DirectFlowType.CONSOLIDATE,
                cell,
                order,
                null,
                null,
                null,
                null,
                null
        );
        var storedSortable = sortableRepository.save(sortable);
        assertThat(storedSortable.getId()).isNotNull();
        assertThat(storedSortable.getHistory().size()).isEqualTo(1);
    }

}
