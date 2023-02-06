package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.api.util.ScApiControllerCaller;
import ru.yandex.market.sc.core.domain.cell.CellCommandService;
import ru.yandex.market.sc.core.domain.cell.model.ApiCellForRouteBaseDto;
import ru.yandex.market.sc.core.domain.cell.model.CellRequestDto;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.cell.repository.CellJdbcRepository;
import ru.yandex.market.sc.core.domain.cell.repository.CellRepository;
import ru.yandex.market.sc.core.domain.courier.model.ApiCourierDto;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundRepository;
import ru.yandex.market.sc.core.domain.route.RouteFacade;
import ru.yandex.market.sc.core.domain.route.model.ApiRouteStatus;
import ru.yandex.market.sc.core.domain.route.model.FinishRouteRequestDto;
import ru.yandex.market.sc.core.domain.route.model.OutgoingCourierRouteType;
import ru.yandex.market.sc.core.domain.route.model.OutgoingRouteBaseDto;
import ru.yandex.market.sc.core.domain.route.repository.RouteMapper;
import ru.yandex.market.sc.core.domain.route_so.Routable;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.sortable.SortableCommandService;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.zone.repository.Zone;
import ru.yandex.market.sc.core.exception.ScWrongCellSortException;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@ScApiControllerTest
public class LotControllerMovingFlowTest {

    private static final long UID = 123L;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    SortableTestFactory sortableTestFactory;
    @Autowired
    ScOrderRepository scOrderRepository;
    @MockBean
    Clock clock;
    @Autowired
    SortableQueryService sortableQueryService;
    @Autowired
    SortableCommandService sortableCommandService;
    @Autowired
    SortableLotService sortableLotService;
    @Autowired
    ScanService scanService;
    @Autowired
    XDocFlow flow;
    @Autowired
    ScApiControllerCaller caller;
    @Autowired
    CellCommandService cellCommandService;
    @Autowired
    CellRepository cellRepository;
    @Autowired
    CellJdbcRepository cellJdbcRepository;
    @Autowired
    RouteFacade routeFacade;
    @Autowired
    RouteMapper routeMapper;
    @Autowired
    OutboundRepository outboundRepository;

    SortingCenter sortingCenter;
    User user;
    Courier courier1;
    Courier courier2;
    Cell cell1;
    Cell cell2;
    Zone zone;
    SortableLot lotForMoving1;
    SortableLot lotForMoving2;
    SortableLot lotForMoving3;
    SortableLot lotForMoving4;


    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.CREATE_ROUTE_SO_FOR_SORTABLE, true);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.MOVE_LOTS_FLOW_ENABLED, true);
        user = testFactory.storedUser(sortingCenter, UID);
        testFactory.setupMockClock(clock);
        courier1 = testFactory.storedCourier(1L, testFactory.defaultCourier().getName());
        courier2 = testFactory.storedCourier(2L, "Другое направление");
        zone = testFactory.storedZone(sortingCenter, "A");
        cell1 = testFactory.storedCell(sortingCenter, "cell1", CellType.COURIER, courier1.getId());
        cell2 = testFactory.storedCell(sortingCenter, "cell2", CellType.COURIER, courier2.getId());
        lotForMoving1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);
        lotForMoving2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);
        lotForMoving3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell1, LotStatus.CREATED, false);
        lotForMoving4 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell2, LotStatus.CREATED, false);
    }

    @DisplayName("Лот переместился и ячейка заполняется")
    @Test
    void lotIsBoundToCell() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort(cell1.getId()).sortToLot(lotForMoving1.getLotId()).get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").build());
        lotForMoving1 = testFactory.prepareToShipLot(lotForMoving1);

        var bufferCell = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c1", courier1.getId(), zone.getId(), 1L, 1L, 1L, 1L));
        cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c2", courier1.getId(), zone.getId(), 1L, 12L, 2L, 2L));
        cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c3", courier1.getId(), zone.getId(), 11L, 2L, 3L, 3L));

        caller.getLotForMoving(lotForMoving1.getBarcode())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "externalId": "%s",
                            "lotStatus": "%s",
                            "cellsAddressList": [
                                "  A-0001-0001-01",
                                "  A-0001-0012-02",
                                "  A-0011-0002-03"
                            ],
                            "destination": "%s",
                            "shipDate": "2020-04-16"
                        }""".formatted(lotForMoving1.getBarcode(), lotForMoving1.getLotStatus(),
                        courier1.getName()), true));

        //в ячейке ничего нет
        var lotsCountByCellId = cellJdbcRepository.getLotsCountInCellByIds(List.of(bufferCell.getId()));
        assertThat(lotsCountByCellId.getOrDefault(bufferCell.getId(), 0)).isZero();

        caller.moveLot(lotForMoving1.getBarcode(), String.valueOf(bufferCell.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cellAddress": "  A-0001-0001-01",
                            "zoneName": "A",
                            "freeLotsInZone": 5
                        }""", true));
        lotForMoving1 = testFactory.getLot(lotForMoving1.getLotId());
        assertThat(lotForMoving1.getSortable().getCellIdOrNull()).isEqualTo(bufferCell.getId());

        //ячейке заполнилась
        var lotsCountByCellIdAfter = cellJdbcRepository.getLotsCountInCellByIds(List.of(bufferCell.getId()));
        assertThat(lotsCountByCellIdAfter.getOrDefault(bufferCell.getId(), 0)).isEqualTo(1);

        var lotAfterSort = sortableLotService.findBySortableId(lotForMoving1.getSortableId()).get();
        //Лот отвязался от ячейки
        assertThat(lotAfterSort.getSortable().getCellIdOrNull()).isNotEqualTo(cell1.getId());

        //Лот привязался к буферной ячейке отгрузки
        assertThat(lotAfterSort.getSortable().getCellIdOrNull()).isEqualTo(bufferCell.getId());
    }

    @DisplayName("ErrorCodes для ручки /lots/forMoving")
    @Test
    void checkErrorCodeLotForMoving() {
        //выключаем флоу
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.MOVE_LOTS_FLOW_ENABLED, false);

        //не включен флоу
        caller.getLotForMoving(lotForMoving1.getBarcode())
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                        .json("{\"status\":400,\"message\":\"Перемещение лотов недоступно на данном СЦ\"}", false));

        //включаем флоу
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.MOVE_LOTS_FLOW_ENABLED, true);

        //created lot
        caller.getLotForMoving(lotForMoving1.getBarcode())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "externalId": "%s",
                            "lotStatus": "%s",
                            "errorCode": "WRONG_LOT_STATUS"
                        }""".formatted(lotForMoving1.getBarcode(), lotForMoving1.getLotStatus()), true));

        //сортируем в заказ в лот, лот в статусе "формируется"
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort(cell1.getId()).sortToLot(lotForMoving1.getLotId()).get();
        lotForMoving1 = testFactory.getLot(lotForMoving1.getLotId());

        caller.getLotForMoving(lotForMoving1.getBarcode())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "externalId": "%s",
                            "lotStatus": "%s",
                            "errorCode": "WRONG_LOT_STATUS"
                        }""".formatted(lotForMoving1.getBarcode(), lotForMoving1.getLotStatus()), true));

        //Лот уже отгружен
        lotForMoving1 = testFactory.prepareToShipLot(lotForMoving1);

        var route = testFactory.findOutgoingRoute(order1).orElseThrow();
        Routable routable = testFactory.getRoutable(route);
        var type = routeMapper.determineOutgoingCourierRouteShipType(routable);
        testFactory.shipLots(routable.getId(), sortingCenter);
        lotForMoving1 = testFactory.getLot(lotForMoving1.getLotId());

        caller.getLotForMoving(lotForMoving1.getBarcode())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "externalId": "%s",
                            "lotStatus": "%s",
                            "errorCode": "ALREADY_SHIPPED"
                        }""".formatted(lotForMoving1.getBarcode(), lotForMoving1.getLotStatus()), true));

        //already in last cell
        var order2 = testFactory.createForToday(order(sortingCenter, "2").build())
                // .updateCourier(courier2)
                .accept()
                .sort(cell1.getId())
                .sortToLot(lotForMoving2.getLotId()).get();
        lotForMoving2 = testFactory.prepareToShipLot(lotForMoving2);

        var bufferCell = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c1", courier1.getId(), zone.getId(), 1L, 1L, 1L, 1L));

        //move lot to test alreadyInCell
        caller.moveLot(lotForMoving2.getBarcode(), String.valueOf(bufferCell.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cellAddress": "  A-0001-0001-01",
                            "zoneName": "A",
                            "freeLotsInZone": 0
                        }""", true));

        caller.getLotForMoving(lotForMoving2.getBarcode())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                                {
                                    "externalId": "%s",
                                    "lotStatus": "%s",
                                    "errorCode": "ALREADY_IN_LAST_CELL",
                                    "cellAddress": "  A-0001-0001-01",
                                    "cellsAddressList": [],
                                    "destination": "%s",
                                    "shipDate": "2020-04-16"
                                }""".formatted(lotForMoving2.getBarcode(), lotForMoving2.getLotStatus(),
                                courier1.getName()),
                        true));

        //no cells for direction
        var order3 = testFactory.createForToday(order(sortingCenter, "3").build())
                .updateCourier(courier2)
                .accept()
                .sort(cell2.getId())
                .sortToLot(lotForMoving4.getLotId()).get();
        lotForMoving4 = testFactory.prepareToShipLot(lotForMoving4);
        caller.getLotForMoving(lotForMoving4.getBarcode())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "externalId": "%s",
                            "lotStatus": "%s",
                            "errorCode": "NO_CELLS_FOR_DESTINATION",
                            "cellsAddressList": [],
                            "destination": "%s",
                            "shipDate": "2020-04-16"

                        }""".formatted(lotForMoving4.getBarcode(),
                        lotForMoving4.getLotStatus(), courier2.getName()), true));

        //already in cell со списком доступных ячеек
        var bufferCell2 = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c2", courier1.getId(), zone.getId(), 1L, 2L, 1L, 1L));

        caller.getLotForMoving(lotForMoving2.getBarcode())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                                {
                                    "externalId": "%s",
                                    "lotStatus": "%s",
                                    "errorCode": "ALREADY_IN_CELL",
                                    "cellAddress": "  A-0001-0001-01",
                                    "cellsAddressList": [
                                        "  A-0001-0002-01"
                                    ],
                                    "destination": "%s",
                                    "shipDate": "2020-04-16"
                                }""".formatted(lotForMoving2.getBarcode(), lotForMoving2.getLotStatus(),
                                courier1.getName()),
                        true));
    }

    @DisplayName("ErrorCodes для ручки /lots/moveLot")
    @Test
    void checkErrorCodeMoveLot() {
        var bufferCell1 = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c1", courier1.getId(), zone.getId(), 1L, 1L, 1L, 2L));
        var bufferCell2 = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c2", courier1.getId(), zone.getId(), 1L, 2L, 0L, 2L));

        //ячейка не является буферной ячейкой отгрузки
        caller.moveLot(lotForMoving1.getBarcode(), String.valueOf(cell1.getId()))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                        .json("{\"status\":400,\"message\":\"Ячейка не является буферной ячейкой отгрузки\"}", false));

        //wrong destination
        caller.moveLot(lotForMoving4.getBarcode(), String.valueOf(bufferCell1.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cellAddress": "  A-0001-0001-01",
                            "errorCode": "WRONG_CELL_DESTINATION",
                            "cellDestination": "%s",
                            "zoneName": "A"
                        }""".formatted(courier1.getName()), true));

        //лот не вмещается в ячейку
        //заполним ячейку
        caller.moveLot(lotForMoving1.getBarcode(), String.valueOf(bufferCell1.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cellAddress": "  A-0001-0001-01",
                            "zoneName": "A",
                            "freeLotsInZone": 3
                        }""", true));

        caller.moveLot(lotForMoving2.getBarcode(), String.valueOf(bufferCell1.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cellAddress": "  A-0001-0001-01",
                            "zoneName": "A",
                            "freeLotsInZone": 2
                        }""", true));

        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort(cell1.getId()).sortToLot(lotForMoving3.getLotId()).get();
        lotForMoving3 = testFactory.prepareToShipLot(lotForMoving3);

        //полной ячейки не должно быть в списке доступных ячеек
        caller.getLotForMoving(lotForMoving3.getBarcode())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "externalId": "%s",
                            "lotStatus": "%s",
                            "cellsAddressList": [
                                "  A-0001-0002-00"
                            ],
                            "destination": "%s",
                            "shipDate": "2020-04-16"
                        }""".formatted(lotForMoving3.getBarcode(), lotForMoving3.getLotStatus(),
                        courier1.getName()), true));

        //пытаются переместить в полную ячейку
        //третий лот не должен вместиться
        caller.moveLot(lotForMoving3.getBarcode(), String.valueOf(bufferCell1.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cellAddress": "  A-0001-0001-01",
                            "zoneName": "A",
                            "errorCode": "ERROR_BY_CELL_CAPACITY",
                            "cellCapacity": 2,
                            "lotsInCell": 2
                        }""", true));

    }

    @DisplayName("Отгрузка лотов из буферной ячейки отгрузки")
    @Test
    void shipLotFromShipBufferCell() {
        var order11 = testFactory.createForToday(order(sortingCenter, "11").build())
                .accept().sort(cell1.getId()).sortToLot(lotForMoving1.getLotId()).get();
        var order12 = testFactory.createForToday(order(sortingCenter, "12").build())
                .accept().sort(cell1.getId()).sortToLot(lotForMoving1.getLotId()).get();
        var order21 = testFactory.createForToday(order(sortingCenter, "21").build())
                .accept().sort(cell1.getId()).sortToLot(lotForMoving2.getLotId()).get();
        var order22 = testFactory.createForToday(order(sortingCenter, "22").build())
                .accept().sort(cell1.getId()).sortToLot(lotForMoving2.getLotId()).get();
        var order31 = testFactory.createForToday(order(sortingCenter, "31").build())
                .accept().sort(cell1.getId()).sortToLot(lotForMoving3.getLotId()).get();
        var order32 = testFactory.createForToday(order(sortingCenter, "33").build())
                .accept().sort(cell1.getId()).sortToLot(lotForMoving3.getLotId()).get();

        var bufferCell = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c1", courier1.getId(), zone.getId(), 1L, 1L, 1L, 2L));

        var bufferCell2 = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c2", courier2.getId(), zone.getId(), 1L, 2L, 1L, 2L));

        var bufferCell3 = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c3", courier1.getId(), zone.getId(), 1L, 3L, 1L, 2L));

        lotForMoving1 = testFactory.getLot(lotForMoving1.getLotId());
        lotForMoving2 = testFactory.getLot(lotForMoving2.getLotId());
        lotForMoving3 = testFactory.getLot(lotForMoving3.getLotId());
        lotForMoving1 = testFactory.prepareToShipLot(lotForMoving1);
        lotForMoving2 = testFactory.prepareToShipLot(lotForMoving2);
        lotForMoving3 = testFactory.prepareToShipLot(lotForMoving3);

        //перемещаем два лота в одну буферную ячейку отгрузки
        caller.moveLot(lotForMoving1.getBarcode(), String.valueOf(bufferCell.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cellAddress": "  A-0001-0001-01",
                            "zoneName": "A",
                            "freeLotsInZone": 3
                        }""", true));

        caller.moveLot(lotForMoving2.getBarcode(), String.valueOf(bufferCell.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cellAddress": "  A-0001-0001-01",
                            "zoneName": "A",
                            "freeLotsInZone": 2
                        }""", true));

        //трейти лот в другую
        caller.moveLot(lotForMoving3.getBarcode(), String.valueOf(bufferCell3.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cellAddress": "  A-0001-0003-01",
                            "zoneName": "A",
                            "freeLotsInZone": 1
                        }""", true));

        //в первой ячейке два лота
        assertThat(cellJdbcRepository.getLotsCountInCellByIds(List.of(bufferCell.getId()))
                .getOrDefault(bufferCell.getId(), 0)).isEqualTo(2);

        //отгрузка
        var route = testFactory.findOutgoingRoute(order11).orElseThrow();
        FinishRouteRequestDto request = FinishRouteRequestDto.builder().cellId(bufferCell.getId()).build();
        routeFacade.finishOutgoingRoute(testFactory.getRouteIdForSortableFlow(route), request, new ScContext(user));

        lotForMoving1 = testFactory.getLot(lotForMoving1.getLotId());
        lotForMoving2 = testFactory.getLot(lotForMoving2.getLotId());
        lotForMoving3 = testFactory.getLot(lotForMoving3.getLotId());
        order11 = testFactory.getOrder(order11.getId());
        order12 = testFactory.getOrder(order12.getId());
        order21 = testFactory.getOrder(order21.getId());
        order22 = testFactory.getOrder(order22.getId());

        //проверка статусов первой ячейки
        assertThat(lotForMoving1.getLotStatus()).isEqualTo(LotStatus.SHIPPED);
        assertThat(lotForMoving2.getLotStatus()).isEqualTo(LotStatus.SHIPPED);
        assertThat(lotForMoving1.getSortable().getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        assertThat(lotForMoving2.getSortable().getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        assertThat(order11.getStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(order12.getStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(order21.getStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(order22.getStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);

        //проверка статусов второй ячейки
        assertThat(lotForMoving3.getLotStatus()).isEqualTo(LotStatus.READY);
        assertThat(lotForMoving3.getSortable().getStatus()).isEqualTo(SortableStatus.SORTED_DIRECT);
        assertThat(order31.getStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(order32.getStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);

        //первая ячейка освободилась после отгрузки
        assertThat(cellJdbcRepository.getLotsCountInCellByIds(List.of(bufferCell.getId()))
                .getOrDefault(bufferCell.getId(), 0)).isZero();

        //туда можно переместить другой лот
        caller.moveLot(lotForMoving3.getBarcode(), String.valueOf(bufferCell.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cellAddress": "  A-0001-0001-01",
                            "zoneName": "A",
                            "freeLotsInZone": 3
                        }""", true));

    }

    @DisplayName("Ячейка не создалась автоматически, если не было ячейки с таким направлением")
    @Test
    void cellHasNotBeenCreated() {
        var order3 = testFactory.createForToday(order(sortingCenter, "3").build())
                .updateCourier(courier2)
                .accept()
                .sort(cell2.getId())
                .sortToLot(lotForMoving4.getLotId()).get();
        lotForMoving4 = testFactory.prepareToShipLot(lotForMoving4);
        caller.getLotForMoving(lotForMoving4.getBarcode())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "externalId": "%s",
                            "lotStatus": "%s",
                            "errorCode": "NO_CELLS_FOR_DESTINATION",
                            "cellsAddressList": [],
                            "destination": "%s",
                            "shipDate": "2020-04-16"

                        }""".formatted(lotForMoving4.getBarcode(),
                        lotForMoving4.getLotStatus(), courier2.getName()), true));
        assertThat(cellRepository.findAll()).containsExactly(cell1, cell2);
    }

    @DisplayName("Нельзя разместить заказ в ячейку буферной отгрузки")
    @Test
    void orderCanNotBeSortToShipBufferCell() {
        var bufferCell = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c1", courier1.getId(), zone.getId(), 1L, 1L, 1L, 2L));
        assertThrows(ScWrongCellSortException.class,
                () -> testFactory.createForToday(order(sortingCenter, "1").build())
                        .accept().sort(bufferCell.getId()).get());
    }

    @DisplayName("Из одной буферной ячейки отгрузки в ту же или другую буферную ячейку отгрузки")
    @Test
    void fromShipBufferToShipBuffer() {
        var order2 = testFactory.createForToday(order(sortingCenter, "2").build())
                .accept()
                .sort(cell1.getId())
                .sortToLot(lotForMoving2.getLotId()).get();
        lotForMoving2 = testFactory.prepareToShipLot(lotForMoving2);

        var bufferCell = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c1", courier1.getId(), zone.getId(), 1L, 1L, 1L, 2L));
        var bufferCell2 = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c2", courier1.getId(), zone.getId(), 1L, 1L, 0L, 2L));

        //move lot to ship_buffer
        caller.moveLot(lotForMoving2.getBarcode(), String.valueOf(bufferCell.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cellAddress": "  A-0001-0001-01",
                            "zoneName": "A",
                            "freeLotsInZone": 3
                        }""", true));

        //already in ship_buffer cell
        caller.getLotForMoving(lotForMoving2.getBarcode())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                                {
                                    "externalId": "%s",
                                    "lotStatus": "%s",
                                    "errorCode": "ALREADY_IN_CELL",
                                    "cellAddress": "  A-0001-0001-01",
                                    "cellsAddressList": [
                                        "  A-0001-0001-00",
                                        "  A-0001-0001-01"
                                    ],
                                    "destination": "%s",
                                    "shipDate": "2020-04-16"
                                }""".formatted(lotForMoving2.getBarcode(), lotForMoving2.getLotStatus(),
                                courier1.getName()),
                        true));

        //нельзя переместить в ту же
        caller.moveLot(lotForMoving2.getBarcode(), String.valueOf(bufferCell.getId()))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"status\":400,\"message\":\"Объект нельзя сортировать в ячейку, " +
                        "в которой он находится\"}", false));

        //можно переместить в другую
        caller.moveLot(lotForMoving2.getBarcode(), String.valueOf(bufferCell2.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cellAddress": "  A-0001-0001-00",
                            "zoneName": "A",
                            "freeLotsInZone": 3
                        }""", true));
    }

    @DisplayName("Из одной буферной ячейки отгрузки отгружаем один из двух лотов")
    @Test
    void shipLotFromCellWithManyLots() {
        var order = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept()
                .sort(cell1.getId())
                .sortToLot(lotForMoving1.getLotId()).get();
        lotForMoving1 = testFactory.prepareToShipLot(lotForMoving1);

        var order2 = testFactory.createForToday(order(sortingCenter, "2").build())
                .accept()
                .sort(cell1.getId())
                .sortToLot(lotForMoving2.getLotId()).get();
        lotForMoving2 = testFactory.prepareToShipLot(lotForMoving2);

        var bufferCell = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c1", courier1.getId(), zone.getId(), 1L, 1L, 1L, 2L));
        var bufferCell2 = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c2", courier1.getId(), zone.getId(), 1L, 1L, 0L, 2L));

        //move lot to ship_buffer
        caller.moveLot(lotForMoving1.getBarcode(), String.valueOf(bufferCell.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cellAddress": "  A-0001-0001-01",
                            "zoneName": "A",
                            "freeLotsInZone": 3
                        }""", true));

        //move lot to ship_buffer
        caller.moveLot(lotForMoving2.getBarcode(), String.valueOf(bufferCell.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cellAddress": "  A-0001-0001-01",
                            "zoneName": "A",
                            "freeLotsInZone": 2
                        }""", true));

        testFactory.shipLotRouteByParentCell(lotForMoving1);
        lotForMoving1 = testFactory.getLot(lotForMoving1.getLotId());
        assertThat(lotForMoving1.getLotStatusOrNull()).isNull();
        assertThat(lotForMoving1.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        lotForMoving2 = testFactory.getLot(lotForMoving2.getLotId());
        assertThat(lotForMoving2.getLotStatusOrNull()).isEqualTo(LotStatus.READY);
        assertThat(lotForMoving2.getStatus()).isNotEqualTo(SortableStatus.SHIPPED_DIRECT);
    }

    @DisplayName("Проверка отгрузки когда в обычной ячейке есть лоты")
    @Test
    void shipLotsFromBufferCell() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                .orElseThrow().allowReading();
        var courier = route.getCourier().orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());

        var bufferCell = cellCommandService.createCellAndBindRoutes(sortingCenter,
                createShipBufferCell("c1", courier.getId(), zone.getId(), 1L, 1L, 1L, 2L));

        lot = testFactory.prepareToShipLot(lot);
        //move lot to ship_buffer
        caller.moveLot(lot.getBarcode(), String.valueOf(bufferCell.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cellAddress": "  A-0001-0001-01",
                            "zoneName": "A",
                            "freeLotsInZone": 1
                        }""", true));

        var order2 = testFactory.createForToday(
                order(flow.getSortingCenter(), "2").places("21", "22")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("21", "22").sortPlaces("21", "22").get();
        var lot2 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order2, "21", lot2, flow.getUser());
        testFactory.sortToLot(order2, "22", lot2, flow.getUser());

        //Обычная ячейка должна быть пустой
        OutgoingRouteBaseDto expectedResult = new OutgoingRouteBaseDto(
                testFactory.getRouteIdForSortableFlow(route),
                new ApiCourierDto(courier.getId(), courier.getId(), courier.getName(), courier.getDeliveryServiceId()),
                null,
                ApiRouteStatus.NOT_STARTED,
                List.of(new ApiCellForRouteBaseDto(
                                bufferCell.getId(), bufferCell.getNumber(), false, 1,
                                zone.getName(),
                                bufferCell.getSequenceNumber(),
                                bufferCell.getAlleyNumber(),
                                bufferCell.getSectionNumber(),
                                bufferCell.getLevelNumber(),
                                "  A-0001-0001-01"
                        ),
                        TestFactory.cellDto(cell, true, 0)),
                1,
                OutgoingCourierRouteType.MAGISTRAL
        );

        caller.getApiV2RoutesByID(testFactory.getRouteIdForSortableFlow(route))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expectedResult)));

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
