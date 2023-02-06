package ru.yandex.market.sc.tms.sortable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.CellCommandService;
import ru.yandex.market.sc.core.domain.cell.model.CellRequestDto;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.cell.repository.CellRepository;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.route.repository.RouteCellRepository;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoSite;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoSiteRepository;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sortable.SortableLockService;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.domain.route.RouteCellDistributor;
import ru.yandex.market.sc.tms.domain.sortable.SortableRescheduler;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.test.TestFactory.sortWithRouteSo;

@EmbeddedDbTmsTest
public class SortableReschedulerTest {

    @Autowired
    SortableRescheduler sortableRescheduler;
    @Autowired
    TestFactory testFactory;
    @Autowired
    SortableQueryService sortableQueryService;
    @Autowired
    RouteSoSiteRepository routeSoSiteRepository;
    @Autowired
    SortableLockService sortableLockService;
    @Autowired
    EntityManager entityManager;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    ScanService scanService;
    @Autowired
    CellCommandService cellCommandService;
    @Autowired
    CellRepository cellRepository;
    @Autowired
    RouteCellRepository routeCellRepository;
    @Autowired
    RouteCellDistributor routeCellDistributor;
    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    User user;
    Cell courierCell;
    Courier courier;
    DeliveryService deliveryService;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 123L);
        testFactory.setupMockClock(clock);
        courier = testFactory.storedCourier();
        courierCell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER, courier.getId());
        deliveryService = testFactory.storedDeliveryService();
    }

    @Test
    void rescheduleSortableOutRoutes() {
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.CREATE_ROUTE_SO_FOR_SORTABLE, true);

        var zone = testFactory.storedZone(sortingCenter);
        var bufferCell1 = testFactory.storedShipBufferCell(sortingCenter, courier.getId(), zone,
                1, 2, 3, 5);
        var bufferCell2 = testFactory.storedShipBufferCell(sortingCenter, courier.getId(), zone,
                1, 3, 1, 5);

        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        var routeSo = lot.getSortable().getOutRoute();
        assertThat(routeSo.getIntervalFrom()).isBefore(Instant.now(clock));
        assertThat(routeSo.getIntervalTo()).isAfter(Instant.now(clock));

        //проверка что route_so привязались к буферным ячейкам отгрузки
        assertThat(routeSoSiteRepository.findByCell(bufferCell1).get(0).getRoute()).isEqualTo(routeSo);
        assertThat(routeSoSiteRepository.findByCell(bufferCell2).get(0).getRoute()).isEqualTo(routeSo);

        //проверка что не упадет если не заполнен site
        transactionTemplate.execute(ts -> {
            var sortable3 = sortableLockService.getRw(lot3.getSortableId());
            sortable3.setCell(null, null);
            entityManager.flush();
            return null;
        });

        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));
        sortableRescheduler.rescheduleAll(LocalDate.now(clock));

        var sortable = sortableQueryService.find(lot.getSortableId()).get();
        var sortable2 = sortableQueryService.find(lot2.getSortableId()).get();

        routeSo = sortable.getOutRoute();

        assertThat(routeSo.getIntervalFrom()).isBefore(Instant.now(clock));
        assertThat(routeSo.getIntervalTo()).isAfter(Instant.now(clock));

        assertThat(sortable2.getOutRoute().getIntervalFrom())
                .isBefore(Instant.now(clock));
        assertThat(sortable2.getOutRoute().getIntervalTo())
                .isAfter(Instant.now(clock));

        //проверка что route_so привязались к буферным ячейкам отгрузки
        assertThat(routeSoSiteRepository.findByCell(bufferCell1).stream().map(RouteSoSite::getRoute).toList()
                .contains(routeSo));
        assertThat(routeSoSiteRepository.findByCell(bufferCell2).stream().map(RouteSoSite::getRoute).toList()
                .contains(routeSo));
    }


    @DisplayName("Отгрузка из той же ячейки отгрузки через несколько дней")
    @Test
    void shipLotFromCellAfterSeveralDays() {
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.CREATE_ROUTE_SO_FOR_SORTABLE, true);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.MOVE_LOTS_FLOW_ENABLED, true);
        var courier1 = testFactory.storedCourier(1L, deliveryService.getId());
        var zone = testFactory.storedZone(sortingCenter, "A");
        var cell1 = testFactory.storedCell(sortingCenter, "cell1", CellType.COURIER, courier1.getId());
        var lotForMoving1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);
        var order = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept()
                .sort(cell1.getId())
                .sortToLot(lotForMoving1.getLotId()).get();
        lotForMoving1 = testFactory.prepareToShipLot(lotForMoving1);

        var bufferCell = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("buf1", courier1.getId(), zone.getId(), 1L, 1L, 1L, 2L));
        var bufferCell2 = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("buf2", courier1.getId(), zone.getId(), 1L, 1L, 0L, 2L));

        List<RouteCell> routeCells =
                routeCellRepository.getRouteIdsInAdvanceForToday(List.of(courier1.getId()),
                        LocalDate.now(clock), sortingCenter.getId());
        List<RouteSoSite> routeSoSites =
                routeSoSiteRepository.getRouteIdsInAdvanceForToday(List.of(courier1.getId()),
                        LocalDateTime.now(clock), sortingCenter.getId());
        if (sortWithRouteSo()) {
            assertThat(routeSoSites.size()).isEqualTo(3);
        } else {
            //route_cell создались для новый ячеек по событию CellCreatedEvent
            assertThat(routeCells.size()).isEqualTo(3);
        }

        //move lot to ship_buffer
        scanService.moveLot(new SortableSortRequestDto(
                lotForMoving1.getBarcode(),
                lotForMoving1.getBarcode(),
                String.valueOf(bufferCell.getId())), new ScContext(user));

        //ship
        testFactory.shipLotRouteByParentCell(lotForMoving1);
        lotForMoving1 = testFactory.getLot(lotForMoving1.getLotId());
        assertThat(lotForMoving1.getLotStatusOrNull()).isNull();
        assertThat(lotForMoving1.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);

        routeCells =
                routeCellRepository.getRouteIdsInAdvanceForToday(List.of(courier1.getId()),
                        LocalDate.now(clock), sortingCenter.getId());
        routeSoSites =
                routeSoSiteRepository.getRouteIdsInAdvanceForToday(List.of(courier1.getId()),
                        LocalDateTime.now(clock), sortingCenter.getId());
        if (sortWithRouteSo()) {
            assertThat(routeSoSites.size()).isEqualTo(3);
        } else {
            //пока что количество не должно измениться
            assertThat(routeCells.size()).isEqualTo(3);
        }

        //проходит день
        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));
        var cell = cellRepository.findByIdOrThrow(bufferCell.getId());
        routeCellDistributor.distributeCells();

        routeCells =
                routeCellRepository.getRouteIdsInAdvanceForToday(List.of(courier1.getId()),
                        LocalDate.now(clock), sortingCenter.getId());
        //на следующий день route_cell должен удалится
        assertThat(routeCells.size()).isEqualTo(0);

        var lotForMovingNew = testFactory.storedLot(sortingCenter, SortableType.PALLET,
                cell1, LotStatus.CREATED, false);
        var order2 = testFactory.createForToday(order(sortingCenter, "2").build())
                .accept()
                .sort(cell1.getId())
                .sortToLot(lotForMovingNew.getLotId()).get();

        routeCells =
                routeCellRepository.getRouteIdsInAdvanceForToday(List.of(courier1.getId()),
                        LocalDate.now(clock), sortingCenter.getId());

        //route_cell создались для ячеек для нового заказа
        assertThat(routeCells.size()).isEqualTo(3);

        lotForMovingNew = testFactory.prepareToShipLot(lotForMovingNew);

        //move lot to ship_buffer
        scanService.moveLot(new SortableSortRequestDto(
                lotForMovingNew.getBarcode(),
                lotForMovingNew.getBarcode(),
                String.valueOf(bufferCell.getId())), new ScContext(user));

        //отгрузка
        testFactory.shipLotRouteByParentCell(lotForMovingNew);
        lotForMovingNew = testFactory.getLot(lotForMovingNew.getLotId());
        assertThat(lotForMovingNew.getLotStatusOrNull()).isNull();
        assertThat(lotForMovingNew.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);

        //проходит еще день
        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));
        cell = cellRepository.findByIdOrThrow(bufferCell.getId());
        //проверка что джобой почистится удаленая ячейка
        cellCommandService.deleteCell(sortingCenter, cell.getId());
        routeCellDistributor.distributeCells();

        routeCells =
                routeCellRepository.getRouteIdsInAdvanceForToday(List.of(courier1.getId()),
                        LocalDate.now(clock), sortingCenter.getId());
        //на следующий день  route_cell должен удалится
        assertThat(routeCells.size()).isEqualTo(0);
    }

    private CellRequestDto createShipBufferCell(String number, Long courierId, Long zoneId, Long alleyNumber,
                                                Long sectionNumber, Long levelNumber, Long lotsCapacity) {
        return CellRequestDto.builder()
                .number(number)
                .status(CellStatus.ACTIVE)
                .courierId(courierId)
                .type(CellType.COURIER)
                .subType(CellSubType.SHIP_BUFFER)
                .zoneId(zoneId)
                .alleyNumber(alleyNumber)
                .sectionNumber(sectionNumber)
                .levelNumber(levelNumber)
                .lotsCapacity(lotsCapacity)
                .build();
    }
}
