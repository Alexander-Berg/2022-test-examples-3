package ru.yandex.market.sc.internal.controller.partner;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.cell.model.CellCargoType;
import ru.yandex.market.sc.core.domain.cell.model.CellRequestDto;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.policy.CellAddressPolicy;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.route.repository.RouteRepository;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertySource;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.domain.zone.model.ZoneStatisticDto;
import ru.yandex.market.sc.core.domain.zone.repository.Zone;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.test.TestFactory.CreateOrderParams;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.controller.dto.PartnerCellDtoWrapper;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static java.lang.String.format;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.CELL_ADDRESS_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.DROPPED_ORDERS_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.ENABLE_BUFFER_XDOC_LOCATION;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.IMPERMANENT_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.MOVE_LOTS_FLOW_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.SORT_ZONE_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.SUPPORTS_BUFFER_MULTI_PLACE_ORDERS;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.SUPPORTS_PACKAGE_REQUIRED_ORDERS;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.UTILIZATION_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.XDOC_ENABLED;
import static ru.yandex.market.sc.core.exception.ScErrorCode.CELL_ADDRESS_VALIDATION_FAILED;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.internal.controller.partner.PartnerZoneControllerTest.processesArrayNode;

/**
 * @author valter
 */
@ExtendWith(DefaultScUserWarehouseExtension.class)
@ScIntControllerTest
class PartnerCellControllerTest {

    @Autowired
    SortingCenterPropertySource sortingCenterPropertySource;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    Clock clock;
    @Autowired
    RouteRepository routeRepository;
    @Autowired
    ScIntControllerCaller caller;
    @Autowired
    XDocFlow flow;
    @Autowired
    SortableTestFactory sortableTestFactory;
    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void getCellTypes() {
        testFactory.setSortingCenterProperty(sortingCenter, XDOC_ENABLED, false);
        caller.getCellTypes()
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "cellTypes": [
                            "BUFFER",
                            "COURIER",
                            "RETURN"
                          ],
                          "warehouseAvailable": [
                            "BUFFER",
                            "RETURN"
                          ],
                          "midMilesCourierAvailable": [
                            "COURIER"
                          ],
                          "zoneAvailable": [
                            "BUFFER",
                            "COURIER"
                          ]
                        }
                        """, true));
    }

    @Test
    void getCellTypesWithZoneAvailable() {
        testFactory.setSortingCenterProperty(sortingCenter, XDOC_ENABLED, false);
        testFactory.setSortingCenterProperty(sortingCenter, SORT_ZONE_ENABLED, true);
        caller.getCellTypes()
                .andExpect(status().isOk())
                .andExpect(content().json("""
                         {
                           "cellTypes": [
                             "BUFFER",
                             "COURIER",
                             "RETURN"
                           ],
                           "warehouseAvailable": [
                             "BUFFER",
                             "RETURN"
                           ],
                           "midMilesCourierAvailable": [
                             "COURIER"
                           ],
                           "zoneAvailable": [
                             "BUFFER",
                             "COURIER"
                           ]
                         }
                        """, true));
    }

    @DisplayName("ячейки BUFFER_XDOC_LOCATION - зоны доступны на фронте при создании такой ячейки хранения")
    @Test
    void getCellTypeWithZoneAvailableXDoc() {
        enableXDocLocationCells();

        caller.getCellTypes()
                .andExpect(status().isOk())
                .andExpect(content().json("""
                         {
                           "cellTypes": [
                             "BUFFER",
                             "COURIER",
                             "RETURN"
                           ],
                           "warehouseAvailable": [
                             "BUFFER",
                             "RETURN"
                           ],
                           "midMilesCourierAvailable": [
                             "COURIER"
                           ],
                           "zoneAvailable": [
                             "BUFFER",
                             "COURIER"
                           ]
                         }
                        """, true));
    }

    @Test
    void getCellsEmpty() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/cells/page")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"content\":[],\"pageable\":{\"sort\":{\"sorted\":false,\"unsorted\":true,\"empty\":true}," +
                        "\"offset\":0,\"pageNumber\":0,\"pageSize\":20,\"paged\":true,\"unpaged\":false}," +
                        "\"last\":true,\"totalPages\":0,\"totalElements\":0,\"size\":20,\"number\":0," +
                        "\"sort\":{\"sorted\":false,\"unsorted\":true,\"empty\":true},\"numberOfElements\":0," +
                        "\"first\":true,\"empty\":true}", false));
    }

    @Test
    void getCellsPage() throws Exception {
        var cell1 = testFactory.storedCell(sortingCenter, "buffer 3", CellType.BUFFER);
        var cell2 = testFactory.storedCell(sortingCenter, "courier 1", CellType.COURIER);
        var cell4 = testFactory.storedCell(sortingCenter, "return 4", CellType.RETURN);

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/cells/page")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"content\":[" +
                        cellJson(cell1, 0, 0, true) + "," +
                        cellJson(cell2, 0, 0, true) + "," +
                        cellJson(cell4, 0, 0, true) +
                        "],\"empty\": false,\"first\": true,\"last\": true,\"number\": 0,\"numberOfElements\": 3," +
                        "\"pageable\": {\"offset\": 0,\"pageNumber\": 0,\"pageSize\": 20,\"paged\": true," +
                        "\"sort\": {\"empty\": true,\"sorted\": false,\"unsorted\": true},\"unpaged\": false},\n" +
                        "\"size\": 20,\"sort\": {\"empty\": true,\"sorted\": false,\"unsorted\": true}," +
                        "\"totalElements\": 3,\"totalPages\": 1}", true));
    }

    @Test
    void getCellsPageReturnsCourier() throws Exception {
        var courier = testFactory.storedCourier(1L);
        var cell = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER, courier.getId());

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/cells/page")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"content\":[" +
                        cellJson(cell, 0, 0, true) +
                        "],\"empty\": false,\"first\": true,\"last\": true,\"number\": 0,\"numberOfElements\": 1," +
                        "\"pageable\": {\"offset\": 0,\"pageNumber\": 0,\"pageSize\": 20,\"paged\": true," +
                        "\"sort\": {\"empty\": true,\"sorted\": false,\"unsorted\": true},\"unpaged\": false},\n" +
                        "\"size\": 20,\"sort\": {\"empty\": true,\"sorted\": false,\"unsorted\": true}," +
                        "\"totalElements\": 1,\"totalPages\": 1}", true));
    }

    @Test
    void getCellsPageSortedAsc() throws Exception {
        var cell1 = testFactory.storedCell(sortingCenter, "МК-15", CellType.BUFFER);
        var cell4 = testFactory.storedCell(sortingCenter, "МК-211", CellType.COURIER);
        var cell3 = testFactory.storedCell(sortingCenter, "МК - 16", CellType.RETURN);

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/cells/page?sort=number,asc")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"content\":[" +
                        cellJson(cell1, 0, 0, true) + "," +
                        cellJson(cell3, 0, 0, true) + "," +
                        cellJson(cell4, 0, 0, true) +
                        "],\"empty\": false,\"first\": true,\"last\": true,\"number\": 0,\"numberOfElements\": 3," +
                        "\"pageable\": {\"offset\": 0,\"pageNumber\": 0,\"pageSize\": 20,\"paged\": true," +
                        "\"sort\": {\"empty\": false,\"sorted\": true,\"unsorted\": false},\"unpaged\": false}," +
                        "\"size\": 20,\"sort\": {\"empty\": false,\"sorted\": true,\"unsorted\": false}," +
                        "\"totalElements\": 3,\"totalPages\": 1}", true));
    }

    @Test
    void getCellsPageSortedDesc() throws Exception {
        var cell1 = testFactory.storedCell(sortingCenter, "МК-15", CellType.BUFFER);
        var cell4 = testFactory.storedCell(sortingCenter, "МК-211", CellType.COURIER);
        var cell3 = testFactory.storedCell(sortingCenter, "МК - 16", CellType.RETURN);

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/cells/page?sort=number,desc")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"content\":[" +
                        cellJson(cell4, 0, 0, true) + "," +
                        cellJson(cell3, 0, 0, true) + "," +
                        cellJson(cell1, 0, 0, true) +
                        "],\"empty\": false,\"first\": true,\"last\": true,\"number\": 0,\"numberOfElements\": 3," +
                        "\"pageable\": {\"offset\": 0,\"pageNumber\": 0,\"pageSize\": 20,\"paged\": true," +
                        "\"sort\": {\"empty\": false,\"sorted\": true,\"unsorted\": false},\"unpaged\": false}," +
                        "\"size\": 20,\"sort\": {\"empty\": false,\"sorted\": true,\"unsorted\": false}," +
                        "\"totalElements\": 3,\"totalPages\": 1}", true));
    }

    @SneakyThrows
    @Test
    void getCurrentNumberLotsInCell() {
        var cell1 = testFactory.storedCell(sortingCenter, "buffer 3", CellType.BUFFER);
        var cell2 = testFactory.storedCell(sortingCenter, "courier 1", CellType.COURIER);
        var cell4 = testFactory.storedCell(sortingCenter, "return 4", CellType.RETURN);

        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell2);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell2);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell4);

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/cells/page")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"content\":[" +
                        cellJson(cell1, 0, 0, true) + "," +
                        cellJson(cell2, 0, 2, false) + "," +
                        cellJson(cell4, 0, 1, false) +
                        "],\"empty\": false,\"first\": true,\"last\": true,\"number\": 0,\"numberOfElements\": 3," +
                        "\"pageable\": {\"offset\": 0,\"pageNumber\": 0,\"pageSize\": 20,\"paged\": true," +
                        "\"sort\": {\"empty\": true,\"sorted\": false,\"unsorted\": true},\"unpaged\": false},\n" +
                        "\"size\": 20,\"sort\": {\"empty\": true,\"sorted\": false,\"unsorted\": true}," +
                        "\"totalElements\": 3,\"totalPages\": 1}", true));

    }

    private String cellJson(Cell cell, int placeCount, int lotsInCell, boolean canBeDeleted) {
        var cellAddressEntry = "";
        var extendedCellEntry = ",\"extendedCellMarkerAvailable\": " + (cell.getSubtype() == CellSubType.SHIP_BUFFER);

        if (cell.getSubtype() == CellSubType.SHIP_BUFFER) {
            cellAddressEntry = ",\"cellAddress\":" + CellAddressPolicy.getCellAddress(cell);
        }

        return "{\"sortingCenterId\":" + sortingCenter.getId() +
                ",\"id\":" + cell.getId() + "" +
                (cell.getScNumber() == null ? "" : (",\"number\":\"" + cell.getScNumber() + "\"")) +
                ",\"status\":\"" + cell.getStatus() + "\"" +
                ",\"type\":\"" + cell.getType() + "\"" +
                ",\"subType\":\"" + cell.getSubtype() + "\"" +
                ",\"deleted\":" + cell.isDeleted() +
                (cell.getCourierId() == null ? "" : (",\"courierId\":" + cell.getCourierId())) +
                (cell.getCourierId() == null ? "" : (",\"courierName\":\"Иван Иванов\"")) +
                (cell.getZone() == null ? "" : ",\"zone\":{" +
                        "\"id\":" + cell.getZone().getId() +
                        ",\"sortingCenterId\":" + cell.getZone().getSortingCenter().getId() +
                        ",\"name\":\"" + cell.getZone().getName() + "\"" +
                        ",\"deleted\":" + cell.getZone().isDeleted() +
                        ",\"processes\": " + processesArrayNode(cell.getZone().getProcesses()) +
                        ",\"type\": " + cell.getZone().getType().name() +
                        ",\"statistic\":" + JacksonUtil.toString(new ZoneStatisticDto(0, 0, 0, 0)) +
                        "}") +
                ",\"canBeDeleted\":" + canBeDeleted +
                ",\"canBeUpdated\":" + true +
                ",\"placeCount\":" + placeCount +
                ",\"lotsInCell\":" + lotsInCell +
                ",\"cargoType\":" + cell.getCargoType() +
                (cell.getZone() == null ? "" : ",\"address\":\"" + cell.getZone().getName() + "\"") +
                extendedCellEntry +
                cellAddressEntry + "}";
    }

    @Test
    void getCell() {
        var cell = testFactory.storedCell(sortingCenter, "courier", CellType.COURIER);
        var courier = testFactory.storedCourier();
        testFactory.create(order(sortingCenter, "1").build())
                .updateCourier(courier).updateShipmentDate(LocalDate.now(clock)).accept().sort(cell.getId()).get();
        assertGetCell(cell, 1, false);
    }

    @Test
    void getCellWithZone() {
        var zone = testFactory.storedZone(sortingCenter, "1");
        var cell = testFactory.storedCell(sortingCenter, "courier", CellStatus.ACTIVE, CellType.COURIER, zone);
        var courier = testFactory.storedCourier();
        testFactory.create(order(sortingCenter, "1").build())
                .updateCourier(courier).updateShipmentDate(LocalDate.now(clock)).accept().sort(cell.getId()).get();
        assertGetCell(cell, 1, false);
    }

    @SneakyThrows
    private void assertGetCell(Cell cell, int placeCount, boolean canBeDeleted) {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/cells/" + cell.getId())
                )
                .andExpect(status().isOk())
                .andExpect(content().json("" +
                        "{\"cell\":" + cellJson(cell, placeCount, 0, canBeDeleted) + "}", true));
    }

    @Test
    void createCell() {
        createCell(new CellRequestDto(null, CellStatus.NOT_ACTIVE, CellType.RETURN));
        List<Cell> cells = testFactory.findAllCells();
        assertThat(cells.size()).isEqualTo(1);
        var cell = cells.get(0);
        assertGetCell(cell, 0, true);
        assertThat(cell.getScNumber()).isNotNull();
        assertThat(cell.isDeleted()).isFalse();
        assertThat(cell.getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);
        assertThat(cell.getType()).isEqualTo(CellType.RETURN);
    }

    @Test
    void createCellAndDropUnneededBindingsForCourierRoute() {
        Courier courier = testFactory.storedCourier(1);

        Route route = createCourierRouteWithUnneededBindings(courier);

        Set<RouteCell> routeCellsBefore = routeRepository.findByIdOrThrow(route.getId())
                .allowReading()
                .getRouteCells();

        assertThat(routeCellsBefore).hasSize(3);

        createCell(new CellRequestDto(null, CellStatus.NOT_ACTIVE, CellType.COURIER, courier.getId()));

        List<Cell> cellsAfter = routeRepository.findByIdOrThrow(testFactory.getRouteIdForSortableFlow(route))
                .getRouteCells().stream()
                .map(RouteCell::getCell).toList();

        assertThat(cellsAfter).hasSize(2);
        cellsAfter.forEach(cell -> assertThat(cell.getCourierId()).isEqualTo(courier.getId()));
    }

    private Route createCourierRouteWithUnneededBindings(Courier courier) {
        Courier anotherCourier = testFactory.storedCourier(100);
        Warehouse anotherWarehouse = testFactory.storedWarehouse("333333333-100");

        Cell cellWithSameCourierBound = testFactory.storedCell(
                sortingCenter, "111 - MK", CellType.COURIER, courier.getId()
        );
        Cell cellWithAnotherCourierBound = testFactory.storedCell(
                sortingCenter, "222 - MK", CellType.COURIER, anotherCourier.getId()
        );
        Cell cellWithAnotherWarehouseBound = testFactory.storedCell(
                sortingCenter, "333 - VOZVRAT", CellType.COURIER, anotherWarehouse.getYandexId()
        );

        return testFactory.storedOutgoingCourierRoute(LocalDate.now(clock), sortingCenter, courier,
                cellWithSameCourierBound, cellWithAnotherCourierBound, cellWithAnotherWarehouseBound);
    }

    @Test
    void createCellAndDropUnneededBindingsForWarehouseRoute() {
        Warehouse warehouse = testFactory.storedWarehouse("111111111-100");

        Route route = createWarehouseRouteWithUnneededBindings(warehouse);

        Set<RouteCell> routeCellsBefore = routeRepository.findByIdOrThrow(route.getId())
                .allowReading()
                .getRouteCells();

        assertThat(routeCellsBefore).hasSize(3);

        createCell(new CellRequestDto(null, CellStatus.NOT_ACTIVE, CellType.RETURN, warehouse.getYandexId()));

        List<Cell> cellsAfter = routeRepository.findByIdOrThrow(testFactory.getRouteIdForSortableFlow(route))
                .getRouteCells().stream()
                .map(RouteCell::getCell).toList();

        assertThat(cellsAfter).hasSize(2);
        cellsAfter.forEach(cell -> assertThat(cell.getWarehouseYandexId()).isEqualTo(warehouse.getYandexId()));
    }

    private Route createWarehouseRouteWithUnneededBindings(Warehouse warehouse) {
        Cell cellWithSameWarehouseBound = testFactory.storedCell(
                sortingCenter, "111 - VOZVRAT", CellType.COURIER, warehouse.getYandexId()
        );
        Courier anotherCourier = testFactory.storedCourier(100);
        Warehouse anotherWarehouse = testFactory.storedWarehouse("333333333-100");

        Cell cellWithAnotherCourierBound = testFactory.storedCell(
                sortingCenter, "222 - MK", CellType.COURIER, anotherCourier.getId()
        );
        Cell cellWithAnotherWarehouseBound = testFactory.storedCell(
                sortingCenter, "333 - VOZVRAT", CellType.COURIER, anotherWarehouse.getYandexId()
        );

        return testFactory.storedOutgoingWarehouseRoute(LocalDate.now(clock), sortingCenter, warehouse,
                cellWithSameWarehouseBound, cellWithAnotherCourierBound, cellWithAnotherWarehouseBound);
    }

    @SneakyThrows
    private void createCell(CellRequestDto dto) {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/internal/partners/" + sortingCenter.getPartnerId()
                                        + "/cells")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JacksonUtil.toString(dto))
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Не заполнен cargoType при создании буферной ячейки")
    void cargoTypeFieldIsRequiredError() {
        testFactory.setSortingCenterProperty(sortingCenter, BUFFER_RETURNS_ENABLED, true);
        caller.createCell(
                        CellRequestDto.builder()
                                .number("cell-cargo-type")
                                .type(CellType.BUFFER)
                                .subType(CellSubType.BUFFER_RETURNS)
                                .status(CellStatus.ACTIVE)
                                .build()
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is(ScErrorCode.INVALID_CARGO_TYPE.name())));
    }

    @Test
    @DisplayName("Заполнен cargoType при создании буферной ячейки")
    void cargoTypeFieldIsRequiredOk() {
        testFactory.setSortingCenterProperty(sortingCenter, BUFFER_RETURNS_ENABLED, true);
        caller.createCell(
                        CellRequestDto.builder()
                                .number("cell-cargo-type")
                                .type(CellType.BUFFER)
                                .subType(CellSubType.BUFFER_RETURNS)
                                .status(CellStatus.ACTIVE)
                                .cargoType(CellCargoType.MGT)
                                .build()
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cell.cargoType", is(CellCargoType.MGT.name())));
    }

    @Test
    @DisplayName("Проверка значений CargoType доступные в фильтрах по умолчанию для CellSubtype")
    void cargoTypeDefaultForCellSubType() {
        caller.getCellCargoTypes()
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "cargoTypes":{
                                "DEFAULT":["NONE"],
                                "RETURN_DAMAGED":["NONE"],
                                "DROPPED_ORDERS":["NONE"],
                                "BUFFER_XDOC_LOCATION":["NONE"],
                                "BUFFER_RETURNS":["MGT","KGT"],
                                "CLIENT_RETURN":["NONE"],
                                "UTILIZATION":["NONE"],
                                "SHIP_XDOC":["NONE"],
                                "IN_ADVANCE_COURIER":["NONE"],
                                "BUFFER_XDOC_BOX":["NONE"],
                                "IMPERMANENT":["NONE"],
                                "BUFFER_XDOC":["NONE"],
                                "SHIP_BUFFER":["NONE"]
                            }
                        }
                        """, false));
    }

    @Test
    void updateCell() {
        createCell(new CellRequestDto("123", CellStatus.ACTIVE, CellType.COURIER));
        Cell cellBefore = testFactory.findCell(sortingCenter, "123");
        assertGetCell(cellBefore, 0, true);
        updateCell(new CellRequestDto("555", CellStatus.NOT_ACTIVE, CellType.RETURN), cellBefore);
        Cell cellAfter = testFactory.findCell(sortingCenter, "555");
        assertThat(cellAfter.getId()).isEqualTo(cellBefore.getId());
        assertGetCell(cellAfter, 0, true);
        assertThat(cellAfter.getScNumber()).isEqualTo("555");
        assertThat(cellAfter.isDeleted()).isFalse();
        assertThat(cellAfter.getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);
        assertThat(cellAfter.getType()).isEqualTo(CellType.RETURN);
    }

    @SneakyThrows
    private void updateCell(CellRequestDto dto, Cell cell) {
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/internal/partners/" + sortingCenter.getPartnerId()
                                        + "/cells/" + cell.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JacksonUtil.toString(dto))
                )
                .andExpect(status().isOk());
    }

    @Test
    void deleteCell() {
        createCell(new CellRequestDto("123", CellStatus.ACTIVE, CellType.BUFFER));
        Cell cellBefore = testFactory.findCell(sortingCenter, "123");
        assertGetCell(cellBefore, 0, true);
        deleteCell(cellBefore);
        Cell cellAfter = testFactory.findCell(cellBefore.getId());
        assertGetCell(cellAfter, 0, false);
        assertThat(cellAfter.getScNumber()).isEqualTo("123");
        assertThat(cellAfter.isDeleted()).isTrue();
        assertThat(cellAfter.getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);
        assertThat(cellAfter.getType()).isEqualTo(cellBefore.getType());
    }

    @SneakyThrows
    private void deleteCell(Cell cell) {
        mockMvc.perform(
                        MockMvcRequestBuilders.delete("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/cells/" + cell.getId())
                )
                .andExpect(status().isOk());
    }

    @Test
    void getBufferCell() {
        var cell = testFactory.storedActiveCell(sortingCenter, CellType.BUFFER, "buffer-cell");
        var order = testFactory.createOrder(CreateOrderParams.builder().externalId("321")
                        .sortingCenter(sortingCenter).build())
                .accept().keep().get();
        getBufferCell(sortingCenter, cell, 1, 0);
        testFactory.updateForTodayDelivery(order);
        getBufferCell(sortingCenter, cell, 1, 1);
    }

    @Test
    @SneakyThrows
    void getSubTypesOfCellTypeBuffer() {
        testFactory.setSortingCenterPropertyMap(sortingCenter, Map.ofEntries(
                        entry(DAMAGED_ORDERS_ENABLED, true),
                        entry(DROPPED_ORDERS_ENABLED, true),
                        entry(SUPPORTS_BUFFER_MULTI_PLACE_ORDERS, true),
                        entry(SUPPORTS_PACKAGE_REQUIRED_ORDERS, true),
                        entry(XDOC_ENABLED, true),
                        entry(ENABLE_BUFFER_XDOC_LOCATION, true),
                        entry(SORT_ZONE_ENABLED, true),
                        entry(BUFFER_RETURNS_ENABLED, true),
                        entry(IMPERMANENT_ENABLED, true),
                        entry(UTILIZATION_ENABLED, true),
                        entry(MOVE_LOTS_FLOW_ENABLED, true)
                )
        );

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/cellSubtypes")
                )
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("""
                                        {
                                          "subTypes": {
                                            "BUFFER": [
                                              "DROPPED_ORDERS",
                                              "BUFFER_RETURNS",
                                              "DEFAULT",
                                              "BUFFER_XDOC",
                                              "BUFFER_XDOC_LOCATION"
                                            ],
                                            "COURIER": [
                                              "SHIP_XDOC",
                                              "DEFAULT",
                                              "SHIP_BUFFER"
                                            ],
                                            "RETURN": [
                                              "RETURN_DAMAGED",
                                              "CLIENT_RETURN",
                                              "UTILIZATION",
                                              "DEFAULT",
                                              "IMPERMANENT"
                                            ]
                                          },
                                            "cellAddressRequired": [],
                                            "midMilesCourierRequired": []
                                        }""",
                                true));
    }

    @Test
    @SneakyThrows
    void getSubTypesOfCellTypeBufferWithoutXdoc() {
        testFactory.setSortingCenterPropertyMap(sortingCenter, Map.ofEntries(
                        entry(DAMAGED_ORDERS_ENABLED, true),
                        entry(DROPPED_ORDERS_ENABLED, true),
                        entry(SUPPORTS_BUFFER_MULTI_PLACE_ORDERS, true),
                        entry(SUPPORTS_PACKAGE_REQUIRED_ORDERS, true),
                        entry(XDOC_ENABLED, false),
                        entry(ENABLE_BUFFER_XDOC_LOCATION, true),
                        entry(SORT_ZONE_ENABLED, true),
                        entry(BUFFER_RETURNS_ENABLED, true),
                        entry(IMPERMANENT_ENABLED, true),
                        entry(UTILIZATION_ENABLED, true),
                        entry(MOVE_LOTS_FLOW_ENABLED, true)
                )
        );

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/cellSubtypes")
                )
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("""
                                        {
                                          "subTypes": {
                                            "BUFFER": [
                                              "DROPPED_ORDERS",
                                              "BUFFER_RETURNS",
                                              "DEFAULT"
                                            ],
                                            "COURIER": [
                                              "DEFAULT",
                                              "SHIP_BUFFER"
                                            ],
                                            "RETURN": [
                                              "RETURN_DAMAGED",
                                              "CLIENT_RETURN",
                                              "UTILIZATION",
                                              "DEFAULT",
                                              "IMPERMANENT"
                                            ]
                                          },
                                            "cellAddressRequired": [
                                                "SHIP_BUFFER"
                                            ],
                                            "midMilesCourierRequired": [
                                                "SHIP_BUFFER"
                                            ]
                                        }""",
                                true));
    }

    @Test
    @SneakyThrows
    void getSubTypesAllDisabled() {
        testFactory.setSortingCenterPropertyMap(sortingCenter, Map.of(
                DROPPED_ORDERS_ENABLED, false,
                DAMAGED_ORDERS_ENABLED, false,
                XDOC_ENABLED, false,
                BUFFER_RETURNS_ENABLED, false,
                UTILIZATION_ENABLED, false,
                ENABLE_BUFFER_XDOC_LOCATION, false,
                IMPERMANENT_ENABLED, false,
                SUPPORTS_BUFFER_MULTI_PLACE_ORDERS, false,
                SUPPORTS_PACKAGE_REQUIRED_ORDERS, false,
                MOVE_LOTS_FLOW_ENABLED, false)
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/cellSubtypes")
                )
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("""
                                {
                                    "subTypes": {
                                        "COURIER": [
                                            "DEFAULT"
                                        ],
                                        "RETURN": [
                                            "CLIENT_RETURN",
                                            "DEFAULT"
                                        ],
                                        "BUFFER": [
                                            "DEFAULT"
                                        ]
                                    },
                                    "midMilesCourierRequired": [],
                                    "cellAddressRequired": []
                                }
                                """, true));
    }

    @SuppressWarnings("SameParameterValue")
    @SneakyThrows
    private void getBufferCell(SortingCenter sortingCenter, Cell cell, int ordersCount, int ordersToSortCount) {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/cells/buffer")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(bufferCellJson(cell, ordersCount, ordersToSortCount), true));
    }

    private String bufferCellJson(Cell cell, int ordersCount, int ordersToSortCount) {
        return "{\"ordersTotal\":" + ordersCount +
                ",\"ordersToSortTotal\":" + ordersToSortCount +
                (ordersToSortCount == 0 ?
                        ",\"cells\":[]}" :
                        ",\"cells\":" +
                                "[{\"id\":" + cell.getId() +
                                ",\"sortingCenterId\":" + cell.getSortingCenter().getId() +
                                ",\"number\":" + cell.getScNumber() +
                                ",\"status\":" + cell.getStatus() +
                                ",\"type\":\"BUFFER\"," +
                                "\"deleted\":" + cell.isDeleted() +
                                ",\"ordersTotalCount\":" + ordersCount +
                                ",\"ordersToSortCount\":" + ordersToSortCount +
                                "}]}");

    }

    @Test
    void getCellsForLots() {
        testFactory.setSortingCenterProperty(sortingCenter, XDOC_ENABLED, false);

        Cell c1 = testFactory.storedCell(sortingCenter, "return-cell-1", CellType.RETURN);
        Cell c2 = testFactory.storedCell(sortingCenter, "return-cell-2", CellType.RETURN);
        testFactory.storedDeletedCell(sortingCenter, "return-cell-deleted-3", CellType.RETURN);
        testFactory.storedCell(sortingCenter, "buffer-cell", CellType.BUFFER);
        testFactory.storedNotActiveCell(sortingCenter, "not-active-cell", CellType.COURIER);
        Cell c3 = testFactory.storedCell(sortingCenter, "courier-cell-1", CellType.COURIER);
        testFactory.storedCell(sortingCenter, "xdoc-buffer-1", CellType.BUFFER, CellSubType.BUFFER_XDOC);
        testFactory.storedCell(sortingCenter, "buffer-address-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION);

        caller.getCellsForLots()
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().json(format("[" +
                        "{\"id\":%d,\"number\":\"return-cell-1\",\"type\":\"RETURN\"}," +
                        "{\"id\":%d,\"number\":\"return-cell-2\",\"type\":\"RETURN\"}," +
                        "{\"id\":%d,\"number\":\"courier-cell-1\",\"type\":\"COURIER\"}" +
                        "]", c1.getId(), c2.getId(), c3.getId())))
                .andReturn();
    }

    private void enableXDocLocationCells() {
        testFactory.setSortingCenterPropertyMap(sortingCenter, Map.of(
                XDOC_ENABLED, true,
                ENABLE_BUFFER_XDOC_LOCATION, true,
                SORT_ZONE_ENABLED, true)
        );
    }

    @Test
    void validateCellCreationWithAddress() {
        var goodZone = testFactory.storedZone(sortingCenter, "Z");
        checkCellCreationWithAddress("cell_no_address", null, null, null, null, status().isOk());
        checkCellCreationWithAddress("cell_full_address", goodZone.getId(), 1L, 1L, 0L, status().isOk());
        checkCellCreationWithAddress("cell_only_zone", goodZone.getId(), null, null, null, status().isOk());
        checkCellCreationWithAddress("cell_same_address", goodZone.getId(), 1L, 1L, 0L, (content()
                .json("{\"status\":400,\"message\":\"" + CELL_ADDRESS_VALIDATION_FAILED.getMessage() + "\"}", false)));
        checkCellCreationWithAddress("cell_z_section", goodZone.getId(), null, 1L, null, (content()
                .json("{\"status\":400,\"message\":\"" + CELL_ADDRESS_VALIDATION_FAILED.getMessage() + "\"}", false)));
        checkCellCreationWithAddress("cell_big_alley", goodZone.getId(), 1L, 10000L, 0L, (content()
                .json("{\"status\":400,\"message\":\"" + CELL_ADDRESS_VALIDATION_FAILED.getMessage() + "\"}", false)));
        checkCellCreationWithAddress("cell_big_level", goodZone.getId(), 1L, 1L, 100L, (content()
                .json("{\"status\":400,\"message\":\"" + CELL_ADDRESS_VALIDATION_FAILED.getMessage() + "\"}", false)));
        checkCellCreationWithAddress("cell_only_level", null, null, null, 0L, (content()
                .json("{\"status\":400,\"message\":\"" + CELL_ADDRESS_VALIDATION_FAILED.getMessage() + "\"}", false)));
        checkCellCreationWithAddress("cell_only_ally", null, 3L, null, null, (content()
                .json("{\"status\":400,\"message\":\"" + CELL_ADDRESS_VALIDATION_FAILED.getMessage() + "\"}", false)));
        checkCellCreationWithAddress("cell_no_zone", null, 2L, 3L, 0L, (content()
                .json("{\"status\":400,\"message\":\"" + CELL_ADDRESS_VALIDATION_FAILED.getMessage() + "\"}", false)));
    }

    @SneakyThrows
    private void checkCellCreationWithAddress(String cellName, Long zoneId, Long alleyId, Long sectionId, Long levelId,
                                              ResultMatcher matcher) {
        var dto = new CellRequestDto(cellName, CellStatus.ACTIVE, CellType.COURIER, CellSubType.DEFAULT,
                null, null, zoneId, null, alleyId, sectionId, levelId, null, null);
        mockMvc.perform(MockMvcRequestBuilders.post("/internal/partners/" + sortingCenter.getPartnerId()
                        + "/cells").contentType(MediaType.APPLICATION_JSON).content(JacksonUtil.toString(dto)))
                .andExpect(matcher);
    }

    @Nested
    @DisplayName("Отображение на фронте XDoc ячеек")
    class XDocCellsFrontEndView {

        @DisplayName("ячейки BUFFER_XDOC будут приходить на фронт для создания лотов BASKET")
        @Test
        void getCellsForLotsXDocEnabled() {
            testFactory.setSortingCenterProperty(sortingCenter, XDOC_ENABLED, true);
            testFactory.setSortingCenterProperty(sortingCenter, ENABLE_BUFFER_XDOC_LOCATION, false);

            Cell c1 = testFactory.storedCell(sortingCenter, "return-cell-1", CellType.RETURN);
            Cell c2 = testFactory.storedCell(sortingCenter, "return-cell-2", CellType.RETURN);
            testFactory.storedDeletedCell(sortingCenter, "return-cell-deleted-3", CellType.RETURN);
            testFactory.storedCell(sortingCenter, "buffer-cell", CellType.BUFFER);
            testFactory.storedNotActiveCell(sortingCenter, "not-active-cell", CellType.COURIER);
            Cell c3 = testFactory.storedCell(sortingCenter, "courier-cell-1", CellType.COURIER);
            Cell c4 = testFactory.storedCell(sortingCenter, "buffer-xdoc-1", CellType.BUFFER, CellSubType.BUFFER_XDOC);
            testFactory.storedCell(sortingCenter, "buffer-address-1", CellType.BUFFER,
                    CellSubType.BUFFER_XDOC_LOCATION);

            caller.getCellsForLots()
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(content().json(format("""
                            [
                              {
                                "id": %d,
                                "number": "return-cell-1",
                                "type": "RETURN"
                              },
                              {
                                "id": %d,
                                "number": "return-cell-2",
                                "type": "RETURN"
                              },
                              {
                                "id": %d,
                                "number": "courier-cell-1",
                                "type": "COURIER"
                              },
                              {
                                "id": %d,
                                "number": "buffer-xdoc-1",
                                "type": "BUFFER"
                              }
                            ]""", c1.getId(), c2.getId(), c3.getId(), c4.getId())))
                    .andReturn();
        }

        @DisplayName("ячейки BUFFER_XDOC и BUFFER_XDOC_LOCATION будут приходить на фронт для создания лотов BASKET")
        @Test
        void getCellsForLotsBufferXDocLocationCellsEnabled() {
            enableXDocLocationCells();

            Cell c1 = testFactory.storedCell(sortingCenter, "return-cell-1", CellType.RETURN);
            Cell c2 = testFactory.storedCell(sortingCenter, "return-cell-2", CellType.RETURN);
            testFactory.storedDeletedCell(sortingCenter, "return-cell-deleted-3", CellType.RETURN);
            testFactory.storedCell(sortingCenter, "buffer-cell", CellType.BUFFER);
            testFactory.storedNotActiveCell(sortingCenter, "not-active-cell", CellType.COURIER);
            Cell c3 = testFactory.storedCell(sortingCenter, "courier-cell-1", CellType.COURIER);
            Cell c4 = testFactory.storedCell(sortingCenter, "buffer-xdoc-1", CellType.BUFFER, CellSubType.BUFFER_XDOC);
            Cell c5 = testFactory.storedCell(sortingCenter, "buffer-address-1", CellType.BUFFER,
                    CellSubType.BUFFER_XDOC_LOCATION);

            caller.getCellsForLots()
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(content().json(format("""
                            [
                              {
                                "id": %d,
                                "number": "return-cell-1",
                                "type": "RETURN"
                              },
                              {
                                "id": %d,
                                "number": "return-cell-2",
                                "type": "RETURN"
                              },
                              {
                                "id": %d,
                                "number": "courier-cell-1",
                                "type": "COURIER"
                              },
                              {
                                "id": %d,
                                "number": "buffer-xdoc-1",
                                "type": "BUFFER"
                              },
                              {
                                "id": %d,
                                "number": "buffer-address-1",
                                "type": "BUFFER"
                              }
                            ]""", c1.getId(), c2.getId(), c3.getId(), c4.getId(), c5.getId())))
                    .andReturn();
        }

        @DisplayName("Не показывать на фронте занятые адресные xdoc ячейки")
        @Test
        void doNotShowOccupiedAddressCells() {
            enableXDocLocationCells();

            var samaraWH = testFactory.storedWarehouse("samara-warehouse");
            var samaraZone = testFactory.storedZone(sortingCenter, "XDOC_SAM");

            var c1 = testFactory.storedCell(
                    sortingCenter, "SAM_01_01", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                    samaraWH.getYandexId(), samaraZone);
            var c2 = testFactory.storedCell(
                    sortingCenter, "SAM_01_02", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                    samaraWH.getYandexId(), samaraZone);

            caller.getCellsForLots()
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(content().json(format("""
                            [
                              {
                                "id": %d,
                                "number": "SAM_01_01",
                                "type": "BUFFER"
                              },
                              {
                                "id": %d,
                                "number": "SAM_01_02",
                                "type": "BUFFER"
                              }
                            ]""", c1.getId(), c2.getId())));

            flow.inboundBuilder("in-1")
                    .nextLogisticPoint(samaraWH.getYandexId())
                    .build()
                    .linkPallets("XDOC-p-1");

            sortableTestFactory.sortByBarcode("XDOC-p-1", c1.getId());

            // в выдачу попала только свободная ячейка
            caller.getCellsForLots()
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(content().json(format("""
                            [
                              {
                                "id": %d,
                                "number": "SAM_01_02",
                                "type": "BUFFER"
                              }
                            ]""", c2.getId())));
        }
    }

    @Nested
    @DisplayName("CRUD cell's SubType BUFFER_XDOC_LOCATION")
    class SubTypeBufferXDocLocation {

        private Warehouse warehouse;
        private Zone zone;

        @BeforeEach
        void init() {
            warehouse = testFactory.storedWarehouse("spb-warehouse");
            zone = testFactory.storedZone(sortingCenter, "spb-zone-1");
        }

        @Test
        void creation() {
            enableXDocLocationCells();
            caller.createCell(
                            CellRequestDto.builder()
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.NOT_ACTIVE)
                                    .zoneId(zone.getId())
                                    .warehouseYandexId(warehouse.getYandexId())
                                    .build()
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().json(
                            String.format("""
                                    {
                                       "cell": {
                                         "sortingCenterId": %d,
                                         "status": "NOT_ACTIVE",
                                         "type": "BUFFER",
                                         "subType": "BUFFER_XDOC_LOCATION",
                                         "deleted": false,
                                         "canBeDeleted": true,
                                         "canBeUpdated": true,
                                         "placeCount": 0
                                       }
                                     }""", sortingCenter.getId()), false));
        }

        @DisplayName("Создание ячейки не разрешено без зоны")
        @Test
        void notAllowedToCreateWithoutZone() {
            enableXDocLocationCells();
            caller.createCell(
                            CellRequestDto.builder()
                                    .number("SPB-1")
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.ACTIVE)
                                    .warehouseYandexId(warehouse.getYandexId())
                                    .build()
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(ScErrorCode.CANT_CREATE_CELL_WITHOUT_ZONE.name()));

        }

        @DisplayName("Можно создавать адресные ячейки без направления")
        @Test
        void notAllowedToCreateWithoutWarehouse() {
            enableXDocLocationCells();
            caller.createCell(
                            CellRequestDto.builder()
                                    .number("keep-1")
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.ACTIVE)
                                    .zoneId(zone.getId())
                                    .warehouseYandexId(null)
                                    .build()
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().json(
                            String.format("""
                                    {
                                       "cell": {
                                         "sortingCenterId": %d,
                                         "status": "ACTIVE",
                                         "type": "BUFFER",
                                         "subType": "BUFFER_XDOC_LOCATION",
                                         "deleted": false,
                                         "canBeDeleted": true,
                                         "canBeUpdated": true,
                                         "placeCount": 0
                                       }
                                     }""", sortingCenter.getId()), false));
        }

        @DisplayName("Нельзя создать ячейку в зоне, где размещены ячейки для другого склада")
        @Test
        void notAllowedToCreateCellInsideZoneThatHasCellForDifferentWarehouse() {
            enableXDocLocationCells();
            caller.createCell(
                            CellRequestDto.builder()
                                    .number("spb-1")
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.NOT_ACTIVE)
                                    .zoneId(zone.getId())
                                    .warehouseYandexId(warehouse.getYandexId())
                                    .build()
                    )
                    .andExpect(status().isOk());

            var samaraWH = testFactory.storedWarehouse("samara-warehouse-1");
            caller.createCell(
                            CellRequestDto.builder()
                                    .number("samara-1")
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.NOT_ACTIVE)
                                    .zoneId(zone.getId())
                                    .warehouseYandexId(samaraWH.getYandexId())
                                    .build()
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error")
                            .value(ScErrorCode.CANT_CREATE_CELL_IN_ZONE_FOR_ANOTHER_WAREHOUSE.name()));
        }

        @DisplayName("Нельзя создать ячейку с направлением в зоне, где размещены ячейки без направления")
        @Test
        void notAllowedToCreateCellWithDirectionInsideZoneThatHasCellWithoutDirection() {
            enableXDocLocationCells();
            caller.createCell(
                            CellRequestDto.builder()
                                    .number("keep-1")
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.ACTIVE)
                                    .zoneId(zone.getId())
                                    .warehouseYandexId(null)
                                    .build()
                    )
                    .andExpect(status().isOk());

            var samaraWH = testFactory.storedWarehouse("samara-warehouse-1");
            caller.createCell(
                            CellRequestDto.builder()
                                    .number("samara-1")
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.ACTIVE)
                                    .zoneId(zone.getId())
                                    .warehouseYandexId(samaraWH.getYandexId())
                                    .build()
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error")
                            .value(ScErrorCode.CANT_CREATE_CELL_IN_ZONE_FOR_ANOTHER_WAREHOUSE.name()));
        }

        @DisplayName("Нельзя создать ячейку в зоне, где размещаются другие типы ячеек")
        @Test
        void notAllowedToCreateCellInsideZoneThatHasNonBufferXDocLocationCells() {
            enableXDocLocationCells();
            caller.createCell(
                            CellRequestDto.builder()
                                    .number("some-random-cell")
                                    .type(CellType.COURIER)
                                    .subType(CellSubType.DEFAULT)
                                    .status(CellStatus.NOT_ACTIVE)
                                    .zoneId(zone.getId())
                                    .build()
                    )
                    .andExpect(status().isOk());

            caller.createCell(
                            CellRequestDto.builder()
                                    .number("spb-1")
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.NOT_ACTIVE)
                                    .zoneId(zone.getId())
                                    .warehouseYandexId(warehouse.getYandexId())
                                    .build()
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(ScErrorCode.CANT_CREATE_CELL_IN_ZONE.name()));
        }

        @Test
        void update() {
            enableXDocLocationCells();
            var response = caller.createCell(
                            CellRequestDto.builder()
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .zoneId(zone.getId())
                                    .warehouseYandexId(warehouse.getYandexId())
                                    .build()
                    )
                    .andExpect(status().isOk())
                    .getResponseAsClass(PartnerCellDtoWrapper.class);

            caller.updateCell(
                            response.getCell().getId(),
                            CellRequestDto.builder()
                                    .number("SPB-1")
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.ACTIVE)
                                    .zoneId(zone.getId())
                                    .warehouseYandexId(warehouse.getYandexId())
                                    .build()
                    )
                    .andExpect(content().json(
                            String.format("""
                                    {
                                      "cell": {
                                        "sortingCenterId": %d,
                                        "status": "ACTIVE",
                                        "type": "BUFFER",
                                        "subType": "BUFFER_XDOC_LOCATION",
                                        "deleted": false,
                                        "canBeDeleted": true,
                                        "canBeUpdated": true,
                                        "placeCount": 0
                                      }
                                    }""", sortingCenter.getId()), false));
        }

        @DisplayName("update ячейки не разрешено без зоны")
        @Test
        void notAllowedToUpdateWithoutZone() {
            enableXDocLocationCells();
            var response = caller.createCell(
                            CellRequestDto.builder()
                                    .number("SPB-1")
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.ACTIVE)
                                    .zoneId(zone.getId())
                                    .warehouseYandexId(warehouse.getYandexId())
                                    .build()
                    )
                    .andExpect(status().isOk())
                    .getResponseAsClass(PartnerCellDtoWrapper.class);

            caller.updateCell(response.getCell().getId(),
                            CellRequestDto.builder()
                                    .number("SPB-1")
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.ACTIVE)
                                    .warehouseYandexId(warehouse.getYandexId())
                                    .build()
                    ).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(ScErrorCode.CANT_CREATE_CELL_WITHOUT_ZONE.name()));

        }

        @Test
        void creationXDocDisabled() {
            testFactory.setSortingCenterProperty(sortingCenter, XDOC_ENABLED, false);
            testFactory.setSortingCenterProperty(sortingCenter,
                    ENABLE_BUFFER_XDOC_LOCATION, true);
            caller.createCell(
                            CellRequestDto.builder()
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.NOT_ACTIVE)
                                    .warehouseYandexId(warehouse.getYandexId())
                                    .build()
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(ScErrorCode.NOT_SUPPORTED_CELL_SUBTYPE.name()));
        }

        @Test
        void creationSubTypeDisabled() {
            testFactory.setSortingCenterProperty(sortingCenter, XDOC_ENABLED, true);
            testFactory.setSortingCenterProperty(sortingCenter,
                    ENABLE_BUFFER_XDOC_LOCATION, false);
            caller.createCell(
                            CellRequestDto.builder()
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.NOT_ACTIVE)
                                    .warehouseYandexId(warehouse.getYandexId())
                                    .build()
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(ScErrorCode.NOT_SUPPORTED_CELL_SUBTYPE.name()));
        }


        @Test
        void updateXDocDisabled() {
            // перед update сначала создаем, это возможно только при включенных настройках
            enableXDocLocationCells();
            var response = caller.createCell(
                            CellRequestDto.builder()
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.NOT_ACTIVE)
                                    .zoneId(zone.getId())
                                    .warehouseYandexId(warehouse.getYandexId())
                                    .build()
                    )
                    .andExpect(status().isOk())
                    .getResponseAsClass(PartnerCellDtoWrapper.class);

            testFactory.setSortingCenterProperty(sortingCenter, XDOC_ENABLED, false);
            caller.updateCell(
                            response.getCell().getId(),
                            CellRequestDto.builder()
                                    .number("SPB-1")
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.ACTIVE)
                                    .zoneId(zone.getId())
                                    .warehouseYandexId(warehouse.getYandexId())
                                    .build()
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(ScErrorCode.NOT_SUPPORTED_CELL_SUBTYPE.name()));
        }

        @Test
        void updateSubTypeDisabled() {
            // перед update сначала создаем, это возможно только при включенных настройках
            enableXDocLocationCells();
            var response = caller.createCell(
                            CellRequestDto.builder()
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.NOT_ACTIVE)
                                    .zoneId(zone.getId())
                                    .warehouseYandexId(warehouse.getYandexId())
                                    .build()
                    )
                    .andExpect(status().isOk())
                    .getResponseAsClass(PartnerCellDtoWrapper.class);

            testFactory.setSortingCenterProperty(sortingCenter, ENABLE_BUFFER_XDOC_LOCATION, false);
            caller.updateCell(
                            response.getCell().getId(),
                            CellRequestDto.builder()
                                    .number("SPB-1")
                                    .type(CellType.BUFFER)
                                    .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                    .status(CellStatus.ACTIVE)
                                    .zoneId(zone.getId())
                                    .warehouseYandexId(warehouse.getYandexId())
                                    .build()
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(ScErrorCode.NOT_SUPPORTED_CELL_SUBTYPE.name()));
        }
    }

    @Nested
    @DisplayName("Работа с буферными ячейками с составными адресами")
    class BufferCells {

        @BeforeEach
        void init() {
            testFactory.setSortingCenterProperty(sortingCenter, CELL_ADDRESS_ENABLED, true);
        }

        @SneakyThrows
        @Test
        void generateShipBufferCellLabelPdf() {
            testFactory.setSortingCenterProperty(sortingCenter, CELL_ADDRESS_ENABLED, true);

            var zone = testFactory.storedZone(sortingCenter, "ZONE-A");
            var courier = testFactory.storedCourier(1000L, "СЦ МК Тарный - служба доставки");
            var cell = testFactory.storedShipBufferCell(sortingCenter, courier.getId(), zone, 1, 2, 3, 5);

            mockMvc.perform(MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                    + "/cells/" + cell.getId() + "/cellAddressLabel")).andExpect(status().isOk());
        }

        @Test
        void creationValidation() {
            var goodZone = testFactory.storedZone(sortingCenter, "Z");

            checkCellCreation(goodZone.getId(), 1, 1, 0, status().isOk());
            checkCellCreation(goodZone.getId(), 1, 1, 0, status().is4xxClientError());
            checkCellCreation(goodZone.getId(), 10000, 1, 0, status().is4xxClientError());
            checkCellCreation(goodZone.getId(), 1, 10000, 0, status().is4xxClientError());
            checkCellCreation(goodZone.getId(), 1, 1, 100, status().is4xxClientError());
            checkCellCreation(null, 1, 1, 0, status().is4xxClientError());
        }

        @Test
        void updateValidation() {
            var goodZone = testFactory.storedZone(sortingCenter, "Z");
            // создаем ячейку без адреса
            var cell = testFactory.storedCell(sortingCenter, CellType.COURIER, CellSubType.SHIP_BUFFER, goodZone);
            // создаем еще одну ячейку уже с адресом
            checkCellCreation(goodZone.getId(), 1, 1, 0, status().isOk());
            // пытаемся назначить первой ячейке тот же адрес, что и второй
            checkCellUpdate(cell.getId(), goodZone.getId(), 1, 1, 0, status().is4xxClientError());
        }

        @Test
        void getCellsPage() throws Exception {
            var goodZone = testFactory.storedZone(sortingCenter, "Z");
            checkCellCreation(goodZone.getId(), 1, 1, 0, status().isOk());
            checkCellCreation(goodZone.getId(), 1, 2, 0, status().isOk());
            checkCellCreation(goodZone.getId(), 2, 1, 1, status().isOk());
            checkCellCreation(goodZone.getId(), 2, 1, 0, status().isOk());

            //select by alley number
            mockMvc.perform(MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                            + "/cells/page").param("alleyNumber", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                    .andExpect(jsonPath("$.content[*].alleyNumber").value(Matchers.everyItem(Matchers.equalTo(1))));

            //select by alley number and section number
            mockMvc.perform(MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                            + "/cells/page").param("alleyNumber", "1").param("sectionNumber", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.content[*].alleyNumber").value(Matchers.everyItem(Matchers.equalTo(1))))
                    .andExpect(jsonPath("$.content[*].sectionNumber").value(Matchers.everyItem(Matchers.equalTo(1))))
                    .andExpect(jsonPath("$.content[*].extendedCellMarkerAvailable")
                            .value(Matchers.everyItem(Matchers.equalTo(true))))
                    .andExpect(jsonPath("$.content[*].getCellAddress")
                            .value(Matchers.everyItem(Matchers.equalTo("Z-0001-0001-00"))));

            //select by alley number and section number and levelId
            mockMvc.perform(MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                    + "/cells/page").param("alleyNumber", "2").param("sectionNumber", "1")
                            .param("levelNumber", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.content[*].alleyNumber").value(Matchers.everyItem(Matchers.equalTo(2))))
                    .andExpect(jsonPath("$.content[*].sectionNumber").value(Matchers.everyItem(Matchers.equalTo(1))))
                    .andExpect(jsonPath("$.content[*].levelNumber").value(Matchers.everyItem(Matchers.equalTo(0))));
        }

        @SneakyThrows
        private void checkCellUpdate(long cellId, long zoneId, long alleyId, long sectionId, long levelId,
                                     ResultMatcher matcher) {
            var dto = new CellRequestDto("goodCell1", CellStatus.ACTIVE, CellType.COURIER, CellSubType.SHIP_BUFFER,
                    null, null, zoneId, null, alleyId, sectionId, levelId, null, null);
            mockMvc.perform(MockMvcRequestBuilders.post("/internal/partners/" + sortingCenter.getPartnerId()
                            + "/cells").contentType(MediaType.APPLICATION_JSON).content(JacksonUtil.toString(dto)))
                    .andExpect(matcher);
        }

        @SneakyThrows
        private void checkCellCreation(Long zoneId, long alleyId, long sectionId, long levelId, ResultMatcher matcher) {
            var cellName = "cell" + zoneId + alleyId + sectionId + levelId;
            var dto = new CellRequestDto(cellName, CellStatus.ACTIVE, CellType.COURIER, CellSubType.SHIP_BUFFER,
                    null, null, zoneId, null, alleyId, sectionId, levelId, null, null);
            mockMvc.perform(MockMvcRequestBuilders.post("/internal/partners/" + sortingCenter.getPartnerId()
                            + "/cells").contentType(MediaType.APPLICATION_JSON).content(JacksonUtil.toString(dto)))
                    .andExpect(matcher);
        }
    }

}
