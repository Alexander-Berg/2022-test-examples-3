package ru.yandex.market.sc.core.domain.partner.order;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.ScOrderState;
import ru.yandex.market.sc.core.domain.partner.order.model.ApiPartnerOrder;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PartnerOrderJdbcRepositoryTest {

    private final PartnerOrderJdbcRepository partnerOrderJdbcRepository;
    private final TestFactory testFactory;
    private final Clock clock;

    private SortingCenter sortingCenter;
    private LocalDate currentDate;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        currentDate = LocalDate.now(clock);
    }

    @Test
    void findWithParams() {
        for (int i = 0; i < 5; i++) {
            testFactory.createForToday(order(sortingCenter, "externalId_" + i).build())
                    .accept().sort().get();
        }
        PartnerOrderParamsDto paramsDto = new PartnerOrderParamsDto();
        var allBySortingCenterId = partnerOrderJdbcRepository.findByParam(sortingCenter.getId(),
                paramsDto, pageable(100, 0), currentDate);
        assertThat(allBySortingCenterId.getTotalElements()).isEqualTo(5);

        paramsDto.setId("externalId_0");
        var allByExternalId = partnerOrderJdbcRepository.findByParam(sortingCenter.getId(),
                paramsDto, pageable(100, 0), currentDate);
        assertThat(allByExternalId.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findWithState() {
        OrderLike sortedOrder = testFactory.createForToday(order(sortingCenter).externalId("externalId_sorted").build())
                .accept().sort().get();
        OrderLike shippedOrder =
                testFactory.createForToday(order(sortingCenter).externalId("externalId_shipped").build())
                .accept().sort().shipPlace("externalId_shipped").get();

        PartnerOrderParamsDto paramsSorted = new PartnerOrderParamsDto();
        paramsSorted.setState(List.of(ScOrderState.SORTED));
        var sortedOrders = partnerOrderJdbcRepository
                .findByParam(sortingCenter.getId(), paramsSorted, pageable(100, 0), currentDate)
                .get().toList();
        assertThat(sortedOrders).extracting(ApiPartnerOrder::getId).containsOnly(sortedOrder.getExternalId());

        PartnerOrderParamsDto paramsShipped = new PartnerOrderParamsDto();
        paramsShipped.setState(List.of(ScOrderState.SHIPPED));
        var shippedOrders = partnerOrderJdbcRepository
                .findByParam(sortingCenter.getId(), paramsShipped, pageable(100, 0), currentDate)
                .get().toList();
        assertThat(shippedOrders).extracting(ApiPartnerOrder::getId).containsOnly(shippedOrder.getExternalId());
    }

    @Test
    //todo: нет функиональости поиска Коробок по фильтру на ПИ пока. Нужна ли она?
    void findOnlyUnsorted() {
        testFactory.createForToday(order(sortingCenter).externalId("externalId_sorted").build())
                .accept().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("externalId_keep").build())
                .updateShipmentDate(LocalDate.now(clock).plus(2, ChronoUnit.DAYS))
                .accept().keep()
                .get();
        OrderLike unsortedOrder =
                testFactory.createForToday(order(sortingCenter).externalId("externalId_unsorted").build())
                .updateShipmentDate(LocalDate.now(clock).plus(2, ChronoUnit.DAYS))
                .accept().keep()
                .updateShipmentDate(LocalDate.now(clock))
                .get();

        PartnerOrderParamsDto params = new PartnerOrderParamsDto();
        params.setOnlyUnsortedOrders(true);
        var orders = partnerOrderJdbcRepository
                .findByParam(sortingCenter.getId(), params, pageable(100, 0), currentDate)
                .get().toList();
        assertThat(orders).extracting(ApiPartnerOrder::getId).containsOnly(unsortedOrder.getExternalId());
    }

    @Test
    void findWithParamsFromPlaceView() {
        for (int i = 0; i < 5; i++) {
            testFactory.createForToday(order(sortingCenter, "externalId_" + i).build())
                    .accept().sort().get();
        }

        PartnerOrderParamsDto paramsDto = new PartnerOrderParamsDto();
        var allBySortingCenterId = partnerOrderJdbcRepository.findByParam(sortingCenter.getId(),
                paramsDto, pageable(100, 0), currentDate);
        assertThat(allBySortingCenterId.getTotalElements()).isEqualTo(5);

        paramsDto.setId("externalId_0");
        var allByExternalId = partnerOrderJdbcRepository.findByParam(sortingCenter.getId(),
                paramsDto, pageable(100, 0), currentDate);
        assertThat(allByExternalId.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findWithParamsFromPlaceViewByLotExternalId() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, true);
        Cell cell = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        SortableLot lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        SortableLot lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);

        testFactory.createForToday(order(sortingCenter).externalId("o1").places("o1p1", "o1p2").build())
                .acceptPlace("o1p1")
                .sortPlaceToLot(lot1.getLotId(), "o1p1");

        testFactory.createForToday(order(sortingCenter).externalId("o2").places("o2p1", "o2p2").build())
                .acceptPlaces("o2p1", "o2p2")
                .sortToLot(lot2.getLotId());

        testFactory.createForToday(order(sortingCenter).externalId("o3").places("o3p1").build())
                .acceptPlaces("o3p1")
                .sortToLot(lot2.getLotId());

        PartnerOrderParamsDto paramsDto = new PartnerOrderParamsDto();
        paramsDto.setLotExternalId(lot1.getBarcode());

        var count1 = partnerOrderJdbcRepository
                .countByParam(sortingCenter.getId(), paramsDto, currentDate);
        assertThat(count1).isEqualTo(1);

        paramsDto.setLotExternalId(lot2.getBarcode());
        var count2 = partnerOrderJdbcRepository
                .countByParam(sortingCenter.getId(), paramsDto, currentDate);
        assertThat(count2).isEqualTo(2);
    }

    @Test
    void findWithParamsByPartLotExternalId() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, true);
        Cell cell = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        SortableLot lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell,
                LotStatus.CREATED, false, "SC_LOT_1111");
        SortableLot lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell,
                LotStatus.CREATED, false, "SC_LOT_2222");

        testFactory.createForToday(order(sortingCenter).externalId("o1").places("o1p1", "o1p2").build())
                .acceptPlace("o1p1")
                .sortPlaceToLot(lot1.getLotId(), "o1p1");

        testFactory.createForToday(order(sortingCenter).externalId("o2").places("o2p1", "o2p2").build())
                .acceptPlaces("o2p1", "o2p2")
                .sortToLot(lot2.getLotId());

        testFactory.createForToday(order(sortingCenter).externalId("o3").places("o3p1").build())
                .acceptPlaces("o3p1")
                .sortToLot(lot2.getLotId());

        PartnerOrderParamsDto paramsDto = new PartnerOrderParamsDto();
        paramsDto.setLotExternalId("LOT_1");

        var count1 = partnerOrderJdbcRepository
                .countByParam(sortingCenter.getId(), paramsDto, currentDate);
        assertThat(count1).isEqualTo(1);

        paramsDto.setLotExternalId("_LOT_2 ");
        var count2 = partnerOrderJdbcRepository
                .countByParam(sortingCenter.getId(), paramsDto, currentDate);
        assertThat(count2).isEqualTo(2);
    }

    private Pageable pageable(int size, int page) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "arrivedToSoDate", "arrivedToSoTime"));
    }

}
