package ru.yandex.market.sc.core.domain.cell;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.cell.model.CellRequestDto;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.model.PartnerBufferCellsDto;
import ru.yandex.market.sc.core.domain.cell.model.PartnerBufferCellsDto.PartnerBufferCellDto;
import ru.yandex.market.sc.core.domain.cell.model.PartnerCellDto;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.model.ApiCellWithPlacesDto;
import ru.yandex.market.sc.core.domain.order.model.ApiCellsToSortDto;
import ru.yandex.market.sc.core.domain.order.model.ApiCellsToSortDto.CellToSortDto;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.test.TestFactory.CreateOrderParams;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CellQueryServiceTest {

    private final TestFactory testFactory;
    private final CellQueryService cellQueryService;
    private final CellCommandService cellCommandService;
    private final Clock clock;

    private SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void getCellWithLots() {
        var deliveryService = testFactory.storedDeliveryService("123");
        var courier = testFactory.storedCourierFromDs(123);
        var cell1 = testFactory.storedCell(
                sortingCenter, "c1", CellType.COURIER, courier.getId());
        var cell2 = testFactory.storedCell(
                sortingCenter, "c2", CellType.COURIER, courier.getId());
        var lot = testFactory.storedLot(sortingCenter, cell1, LotStatus.CREATED);
        var order1 = testFactory.create(
                order(sortingCenter)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(deliveryService)
                        .externalId("o1")
                        .build()
        ).accept().sort(cell2.getId()).get();
        testFactory.create(
                order(sortingCenter)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(deliveryService)
                        .externalId("o2")
                        .build()
        ).accept().sort(cell1.getId()).sortToLot(lot.getLotId()).prepareToShipLot().get();
        var route = testFactory.findOutgoingCourierRoute(order1).orElseThrow();
        var dto1 = cellQueryService.getCellWithLots(cell1.getId(), testFactory.getRouteIdForSortableFlow(route));
        assertThat(dto1.getProcessingLots()).isEqualTo(0);
        assertThat(dto1.getShippedLots()).isEqualTo(0);
        assertThat(dto1.getReadyToShipLots()).isEqualTo(1);

        var dto2 = cellQueryService.getCellWithLots(cell2.getId(), testFactory.getRouteIdForSortableFlow(route));
        assertThat(dto2.getProcessingLots()).isEqualTo(0);
        assertThat(dto2.getShippedLots()).isEqualTo(0);
        assertThat(dto2.getReadyToShipLots()).isEqualTo(0);
    }

    @Test
    void getCellWithLotsPacked() {
        var deliveryService = testFactory.storedDeliveryService("123");
        var courier = testFactory.storedCourierFromDs(123);
        var cell = testFactory.storedCell(
                sortingCenter, "c1", CellType.COURIER, courier.getId());
        var lot = testFactory.storedLot(sortingCenter, cell, LotStatus.CREATED);
        var order = testFactory.create(
                order(sortingCenter)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(deliveryService)
                        .externalId("o1")
                        .build()
        ).accept().sort(cell.getId()).sortToLot(lot.getLotId()).prepareToShipLot().get();
        var route = testFactory.findOutgoingCourierRoute(order).orElseThrow();
        testFactory.setLotStatus(lot.getLotId(), LotStatus.PACKED);
        var dto = cellQueryService.getCellWithLots(cell.getId(), testFactory.getRouteIdForSortableFlow(route));
        assertThat(dto.getProcessingLots()).isEqualTo(0);
        assertThat(dto.getShippedLots()).isEqualTo(0);
        assertThat(dto.getReadyToShipLots()).isEqualTo(1);
    }

    @Test
    void getCellWithOrdersDeletedCellThrowException() {
        var cell = cellCommandService.createCellAndBindRoutes(sortingCenter,
                new CellRequestDto("1", null, null));
        cellCommandService.deleteCell(sortingCenter, cell.getId());

        assertThrows(TplInvalidParameterException.class,
                () -> cellQueryService.getCellWithPlaces(cell.getId(), sortingCenter));
    }

    @Test
    void getCellWithOrdersBufferCell() {
        var place = testFactory.create(TestFactory.order(sortingCenter, "1").build())
                .accept()
                .keep()
                .getPlace();

        ApiCellWithPlacesDto cellWithOrders = cellQueryService.getCellWithPlaces(
                Objects.requireNonNull(place.getCell()).getId(), sortingCenter);
        assertThat(cellWithOrders.getOrdersAssignedToCell()).isNull();
        assertThat(cellWithOrders.getAcceptedButNotSortedPlaceCount()).isNull();
    }

    @Test
    void getCellWithOrder() {
        var place = testFactory.createOrderForToday(sortingCenter)
                .accept()
                .sort()
                .getPlace();
        ApiCellWithPlacesDto cellWithOrders = cellQueryService.getCellWithPlaces(
                Objects.requireNonNull(place.getCell()).getId(), sortingCenter);
        assertThat(cellWithOrders.getOrdersAssignedToCell()).isEqualTo(1);
        assertThat(cellWithOrders.getOrdersInCell()).isEqualTo(1);
        assertThat(cellWithOrders.getAcceptedButNotSortedPlaceCount()).isEqualTo(0);
        assertThat(cellWithOrders.getCellPrepared()).isTrue();
    }

    @Test
    void getCellWithOrderNotPrepared() {
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1", "p2").sortPlaces("p1")
                .get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        ApiCellWithPlacesDto cellWithOrders = cellQueryService.getCellWithPlaces(cell.getId(), sortingCenter);
        assertThat(cellWithOrders.getOrdersAssignedToCell()).isEqualTo(1);
        assertThat(cellWithOrders.getOrdersInCell()).isEqualTo(1);
        assertThat(cellWithOrders.getAcceptedButNotSortedPlaceCount()).isEqualTo(1);
        assertThat(cellWithOrders.getCellPrepared()).isFalse();
    }

    @Test
    void getCellWithOrderPrepared() {
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1", "p2").keepPlacesIgnoreTodayRoute("p1")
                .get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        ApiCellWithPlacesDto cellWithOrders = cellQueryService.getCellWithPlaces(cell.getId(), sortingCenter);
        assertThat(cellWithOrders.getOrdersAssignedToCell()).isEqualTo(1);
        assertThat(cellWithOrders.getOrdersInCell()).isEqualTo(0);
        assertThat(cellWithOrders.getAcceptedButNotSortedPlaceCount()).isEqualTo(2);
        assertThat(cellWithOrders.getCellPrepared()).isTrue();
    }

    @Test
    void getCellsToSort() {
        var place1 = testFactory.createForToday(
                        CreateOrderParams.builder().sortingCenter(sortingCenter).externalId("order1").build())
                .updateShipmentDate(LocalDate.now(clock).plus(2, ChronoUnit.DAYS))
                .accept().keep()
                .updateShipmentDate(LocalDate.now(clock))
                .getPlace();
        var place2 = testFactory.createForToday(
                        CreateOrderParams.builder().sortingCenter(sortingCenter).externalId("order2").build())
                .updateShipmentDate(LocalDate.now(clock).plus(2, ChronoUnit.DAYS))
                .accept().keep().getPlace();
        var cellsToSort = cellQueryService.getCellsToSort(sortingCenter);
        assertThat(cellsToSort.getCells()).hasSize(1);
        assertThat(place1.getCell()).isEqualTo(place2.getCell());
        var cell = place1.getCell();
        assertThat(cellsToSort.getCells())
                .containsOnly(new CellToSortDto(cell.getId(), cell.getScNumber(), 2L, 1L));
    }


    @Test
    void getCellsToSortMultiPlaceOrderNotFullNeedToBeSortred() {
        var order = testFactory.createForToday(
                        CreateOrderParams.builder().sortingCenter(sortingCenter).externalId("order1")
                                .places("o1p1", "o1p2").build()).acceptPlace("o1p1")
                .keepPlaces(true, "o1p1").get();
        Place place1 = testFactory.orderPlace(order, "o1p1");
        assertThat(cellQueryService.getCellsToSort(sortingCenter)).isEqualTo(
                new ApiCellsToSortDto(1L, 1L, 1L,
                        List.of(new CellToSortDto(place1.getCell().getId(), place1.getCell().getScNumber(), 1L, 1L)))
        );
    }

    @Test
    void getCellsToSortOnlyCellsNeedToBeSorted() {
        testFactory.createForToday(
                        CreateOrderParams.builder().sortingCenter(sortingCenter).externalId("order1").build())
                .updateShipmentDate(LocalDate.now(clock).plus(2, ChronoUnit.DAYS))
                .accept().keep().get();
        var cellsToSort = cellQueryService.getCellsToSort(sortingCenter);
        assertThat(cellsToSort).usingRecursiveComparison()
                .isEqualTo(new ApiCellsToSortDto(1, 0, 1, Collections.emptyList()));
    }

    @Test
    void getPartnerCellsToSort() {
        var place1 = testFactory.createForToday(
                        CreateOrderParams.builder().sortingCenter(sortingCenter).externalId("order1").build())
                .updateShipmentDate(LocalDate.now(clock).plus(2, ChronoUnit.DAYS))
                .accept().keep()
                .updateShipmentDate(LocalDate.now(clock))
                .getPlace();
        var place2 = testFactory.createForToday(
                        CreateOrderParams.builder().sortingCenter(sortingCenter).externalId("order2").build())
                .updateShipmentDate(LocalDate.now(clock).plus(2, ChronoUnit.DAYS))
                .accept().keep()
                .getPlace();
        var cellsToSort = cellQueryService.getPartnerCellsToSort(sortingCenter);
        assertThat(cellsToSort.getCells()).hasSize(1);
        assertThat(place1.getCell()).isEqualTo(place2.getCell());
        var cell = place1.getCell();
        assertThat(cellsToSort.getCells().iterator().next())
                .isEqualTo(new PartnerBufferCellDto(
                        cell.getId(),
                        cell.getSortingCenter().getId(),
                        cell.getScNumber(),
                        cell.getStatus(),
                        cell.getType(),
                        cell.getWarehouseYandexId(),
                        cell.isDeleted(),
                        2L,
                        1L
                ));
    }

    @Test
    void getPartnerCellsToSortNoOrdersToSort() {
        testFactory.createOrderForToday(sortingCenter)
                .updateShipmentDate(LocalDate.now(clock).plus(2, ChronoUnit.DAYS))
                .accept().keep().get();
        var cellsToSort = cellQueryService.getPartnerCellsToSort(sortingCenter);
        assertThat(cellsToSort.getCells()).isEmpty();
        assertThat(cellsToSort).usingRecursiveComparison()
                .isEqualTo(new PartnerBufferCellsDto(
                        1L,
                        0L,
                        Collections.emptyList()
                ));
    }

    @Test
    void getPartnerCellToSortMultiPlaceOrderNotFullNeedToBeSortred() {
        var order = testFactory.createForToday(
                        CreateOrderParams.builder().sortingCenter(sortingCenter).externalId("order1")
                                .places("o1p1", "o1p2").build()).acceptPlace("o1p1")
                .keepPlaces(true, "o1p1").get();
        Place place1 = testFactory.orderPlace(order, "o1p1");
        assertThat(cellQueryService.getPartnerCellsToSort(sortingCenter)).isEqualTo(
                new PartnerBufferCellsDto(
                        1L, 1L,
                        List.of(new PartnerBufferCellDto(place1.getCell().getId(), sortingCenter.getId(),
                                place1.getCell().getScNumber(), place1.getCell().getStatus(),
                                place1.getCell().getType(),
                                place1.getCell().getWarehouseYandexId(), place1.getCell().isDeleted(),
                                1L, 1L))
                ));
    }

    @Test
    void getPartnerCellPlaceCount() {
        testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept()
                .sort();

        testFactory.createForToday(order(sortingCenter).externalId("o2").places("o2p1", "o2p2").build())
                .acceptPlace("o2p1")
                .sortPlace("o2p1");

        testFactory.createForToday(order(sortingCenter).externalId("o3").places("o3p1", "o3p2").build())
                .acceptPlaces("o3p1", "o3p2")
                .sortPlaces("o3p1", "o3p2");

        PartnerCellDto partnerCell = cellQueryService.getPartnerCells(sortingCenter).get(0);
        assertThat(partnerCell.getPlaceCount()).isEqualTo(4);
    }

    @Test
    void getCellFromAnotherSc() {
        var anotherSc = testFactory.storedSortingCenter(2L);
        var cell = cellCommandService.createCellAndBindRoutes(sortingCenter,
                new CellRequestDto("1", null, null));
        assertThatThrownBy(() -> cellQueryService.getCellWithPlaces(cell.getId(), anotherSc))
                .isInstanceOf(ScException.class)
                .is(new Condition<>(
                        e -> Objects.equals(((ScException) e).getCode(), ScErrorCode.CELL_FROM_ANOTHER_SC.name()),
                        null));
    }
}
