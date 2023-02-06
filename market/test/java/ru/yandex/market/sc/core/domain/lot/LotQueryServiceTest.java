package ru.yandex.market.sc.core.domain.lot;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.lot.model.ApiSortableDto;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.partner.lot.PartnerLotDto;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DirectFlowType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LotQueryServiceTest {

    private final TestFactory testFactory;
    private final SortableTestFactory sortableTestFactory;
    private final LotQueryService lotQueryService;
    private final SortableLotService sortableLotService;

    private SortingCenter sortingCenter;
    private Cell parentCell;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void getApiLot() {
        parentCell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN, "warehouse");
        SortableLot lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.PROCESSING);
        ApiSortableDto apiSortableDto = lotQueryService.getApiLot(lot.getLotId());

        assertThat(apiSortableDto.getId()).isEqualTo(lot.getLotId());
    }

    @Test
    void getLotWithWarehouse() {
        Warehouse warehouse = testFactory.storedWarehouse("warehouse");
        parentCell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN, warehouse.getYandexId());
        SortableLot lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.PROCESSING);
        PartnerLotDto partnerLot = lotQueryService.getApiLot(sortingCenter, lot.getLotId());

        assertThat(partnerLot.getId()).isEqualTo(lot.getLotId());
        assertThat(partnerLot.getWarehouse()).isEqualTo(warehouse.getIncorporation());
    }


    @Test
    void loadOrdersAndPlacesInLot() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var c1 = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);

        var singlePlaceOrder = testFactory.createForToday(
                        order(sortingCenter).externalId("o1").build()
                )
                .accept()
                .sort(c1.getId())
                .get();

        var places = new String[]{"p1", "p2"};
        var multiPlaceOrder = testFactory.createForToday(
                        order(sortingCenter).places(places).externalId("o2").build()
                )
                .acceptPlaces(places).sortPlaces(places).get();


        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, c1);
        testFactory.sortPlaceToLot(testFactory.orderPlace(multiPlaceOrder, places[0]), lot, user);
        testFactory.sortPlaceToLot(testFactory.orderPlace(singlePlaceOrder), lot, user);
        testFactory.prepareToShipLot(lot);

        var actualPlaces = lotQueryService.loadPlaces(List.of(lot));

        Assertions.assertThat(actualPlaces).containsExactlyInAnyOrder(
                testFactory.orderPlace(singlePlaceOrder),
                testFactory.orderPlace(multiPlaceOrder, places[0]));
        Assertions.assertThat(actualPlaces.size()).isEqualTo(2);
        Assertions.assertThat(actualPlaces.stream()
                .map(Place::getMainPartnerCode)
                .filter(it -> it.equals(places[0]))
                .findFirst()
        ).isNotEmpty();
    }

    @Test
    void getLotWithoutWarehouse() {
        parentCell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        SortableLot lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.PROCESSING);
        PartnerLotDto partnerLot = lotQueryService.getApiLot(sortingCenter, lot.getLotId());

        assertThat(partnerLot.getId()).isEqualTo(lot.getLotId());
        assertThat(partnerLot.getWarehouse()).isNull();
    }

    @Test
    void getDeletedLotsFailed() {
        parentCell = testFactory.storedCell(sortingCenter, "1111", CellType.BUFFER, CellSubType.BUFFER_XDOC);
        testFactory.storedLot(sortingCenter, SortableType.XDOC_BASKET, parentCell,
                LotStatus.PROCESSING, true);
        Warehouse warehouse = testFactory.storedWarehouse("11111");
        Inbound inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .nextLogisticPointId(warehouse.getYandexId())
                .inboundType(InboundType.XDOC_TRANSIT)
                .sortingCenter(sortingCenter)
                .fromDate(OffsetDateTime.now())
                .toDate(OffsetDateTime.now())
                .build());
        Sortable sortable = sortableTestFactory.storeSortable(sortingCenter, SortableType.XDOC_BOX,
                DirectFlowType.TRANSIT, "XDOC-11111111", inbound, null).get();
        assertThat(
                sortableLotService.findApplicableXdocBasketByDirection(sortable, sortingCenter, warehouse.getYandexId())
                        .isEmpty()
        )
                .isTrue();
    }

}
