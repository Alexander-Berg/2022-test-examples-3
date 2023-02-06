package ru.yandex.market.sc.internal.controller.external;

import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.CellRepository;
import ru.yandex.market.sc.core.domain.courier.repository.CourierMapper;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterRepository;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.model.DropOffDto;
import ru.yandex.market.sc.internal.model.InventoryItemDto;
import ru.yandex.market.sc.internal.model.InventoryItemPlaceStatus;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@ScIntControllerTest
class LogisticsSortingCenterServiceTest {

    @Autowired
    LogisticsSortingCenterService logisticsSortingCenterService;
    @Autowired
    SortingCenterRepository sortingCenterRepository;
    @Autowired
    CellRepository cellRepository;
    @Autowired
    TestFactory testFactory;
    @Autowired
    SortableQueryService sortableQueryService;
    @MockBean
    Clock clock;

    SortingCenter sortingCenter;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setupMockClock(clock);
    }


    @Test
    void createSortingCenterAsDropOff() {
        logisticsSortingCenterService.createSortingCenterAsDropOff(new DropOffDto(
                1L, "my_address", "my_token", "my_campaign_id", "my_partner_name", "2",
                "my_dropoff_name", 3L
        ));
        var sc = sortingCenterRepository.findByIdOrThrow(2L);
        assertThat(sc).isEqualTo(new SortingCenter(2L, "my_address", "my_token", "my_campaign_id", "1",
                "my_partner_name", "my_dropoff_name", null, 1L));
        var outgoingCell = cellRepository.findBySortingCenterAndScNumber(sc, "O").get(0);
        assertThat(outgoingCell.getSortingCenter()).isEqualTo(sc);
        assertThat(outgoingCell.getScNumber()).isEqualTo("O");
        assertThat(outgoingCell.getCourierId()).isEqualTo(CourierMapper.mapDeliveryServiceIdToCourierId(3L));
        assertThat(outgoingCell.isDeleted()).isFalse();
        assertThat(outgoingCell.getType()).isEqualTo(CellType.COURIER);
        assertThat(outgoingCell.getSubtype()).isEqualTo(CellSubType.DEFAULT);
    }

    @Test
    void acceptAllPlacesByBarcode() {
        testFactory.createForToday(order(sortingCenter).places("p1", "p2").build());

        var before = logisticsSortingCenterService.findInventoryItemByBarcode(sortingCenter.getId(), List.of("p1"));
        assertThat(before.getPlaces()).hasSize(2);
        assertThat(before.getPlaces())
                .allMatch(p -> p.getStatus().equals(InventoryItemPlaceStatus.CAN_ACCEPT));

        logisticsSortingCenterService.acceptPlacesByBarcodes(sortingCenter.getId(), List.of("p1"));
        var afterFirstAccepted =
                List.of(logisticsSortingCenterService.findInventoryItemByBarcode(sortingCenter.getId(), List.of("p1")));
        assertThat(getPlaceStatusByBarcode(afterFirstAccepted, "p1")).isEqualTo(InventoryItemPlaceStatus.ACCEPTED);
        assertThat(getPlaceStatusByBarcode(afterFirstAccepted, "p2")).isEqualTo(InventoryItemPlaceStatus.CAN_ACCEPT);

        var afterAllAccepted = logisticsSortingCenterService.acceptPlacesByBarcodes(sortingCenter.getId(),
                List.of("p1", "p2"));
        var places = afterAllAccepted.stream().flatMap(e -> e.getPlaces().stream()).toList();
        assertThat(places).allMatch(p -> p.getStatus().equals(InventoryItemPlaceStatus.ACCEPTED));
    }

    @Test
    void acceptPlaceTwice() {
        testFactory.createForToday(order(sortingCenter).places("p1").build());

        var afterFirstAccept = logisticsSortingCenterService.acceptPlacesByBarcodes(sortingCenter.getId(),
                List.of("p1"));
        assertThat(getPlaceStatusByBarcode(afterFirstAccept, "p1")).isEqualTo(InventoryItemPlaceStatus.ACCEPTED);

        var afterSecondAccept = logisticsSortingCenterService.acceptPlacesByBarcodes(sortingCenter.getId(),
                List.of("p1"));
        assertThat(getPlaceStatusByBarcode(afterSecondAccept, "p1")).isEqualTo(InventoryItemPlaceStatus.ACCEPTED);
    }

    @Test
    void acceptPlaceWithBadBarcode() {
        testFactory.createForToday(order(sortingCenter).places("p1", "p2").build());

        var before = logisticsSortingCenterService.findInventoryItemByBarcode(sortingCenter.getId(), List.of("p1"));
        assertThat(before.getPlaces())
                .allMatch(p -> p.getStatus().equals(InventoryItemPlaceStatus.CAN_ACCEPT));

        try {
            logisticsSortingCenterService.acceptPlacesByBarcodes(sortingCenter.getId(), List.of(
                    "INVALID_BARCODE", "p1"));
        } catch (Exception ignored) {
        }

        var after = logisticsSortingCenterService.findInventoryItemByBarcode(sortingCenter.getId(), List.of("p1"));
        assertThat(after.getPlaces())
                .allMatch(p -> p.getStatus().equals(InventoryItemPlaceStatus.CAN_ACCEPT));
    }

    @Test
    void acceptPlacesFromDifferentOrders() {
        testFactory.create(order(sortingCenter).externalId("o1").places("p1", "p2").build()).get();
        testFactory.create(order(sortingCenter).externalId("o2").places("p3", "p4").build()).get();

        var result = logisticsSortingCenterService.acceptPlacesByBarcodes(sortingCenter.getId(),
                List.of("p1", "p3"));

        assertThat(result).hasSize(2);
        assertThat(getPlaceStatusByBarcode(result, "p1")).isEqualTo(InventoryItemPlaceStatus.ACCEPTED);
        assertThat(getPlaceStatusByBarcode(result, "p3")).isEqualTo(InventoryItemPlaceStatus.ACCEPTED);
    }

    private InventoryItemPlaceStatus getPlaceStatusByBarcode(List<InventoryItemDto> orders, String barcode) {
        for (var order : orders) {
            for (var place : order.getPlaces()) {
                if (place.getBarcode().equals(barcode)) {
                    return place.getStatus();
                }
            }
        }
        return null;
    }
}
