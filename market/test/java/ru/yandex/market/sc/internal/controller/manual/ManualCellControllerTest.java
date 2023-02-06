package ru.yandex.market.sc.internal.controller.manual;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.cell.model.CellRequestDto;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.cell.repository.CellRepository;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.DistributionCenterWarehouse;
import ru.yandex.market.sc.core.domain.sorting_center.repository.DistributionCenterWarehouseRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.domain.zone.repository.Zone;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.controller.manual.xdoc.CreateTopologyRequest;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.cell.model.CellSubType.BUFFER_XDOC_BOX;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.ENABLE_BUFFER_XDOC_BOX;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.ENABLE_BUFFER_XDOC_LOCATION;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.SORT_ZONE_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.XDOC_ENABLED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ScIntControllerTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
public class ManualCellControllerTest {

    private final TestFactory testFactory;
    private final ScIntControllerCaller caller;
    private final DistributionCenterWarehouseRepository distributionCenterWarehouseRepository;
    private final CellRepository cellRepository;
    private final XDocFlow flow;
    private final SortableQueryService sortableQueryService;

    private SortingCenter sortingCenter;
    private Warehouse samaraWH;
    private Zone samaraZone;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();

        samaraWH = testFactory.storedWarehouse("samara-warehouse-1");
        distributionCenterWarehouseRepository.save(
                new DistributionCenterWarehouse(sortingCenter.getId(), samaraWH.getId())
        );

        samaraZone = testFactory.storedZone(sortingCenter, "SAMARA");
    }

    @Test
    @SneakyThrows
    void anySortingCenterTopology() {
        testFactory.setSortingCenterPropertyMap(sortingCenter, Map.of(
                        ENABLE_BUFFER_XDOC_LOCATION, true,
                        XDOC_ENABLED, true,
                        SORT_ZONE_ENABLED, true
                )
        );
        caller.createCellsManual(
                sortingCenter.getId(),
                new CreateTopologyRequest(
                        List.of(
                                CellRequestDto.builder()
                                        .type(CellType.BUFFER)
                                        .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                        .status(CellStatus.ACTIVE)
                                        .warehouseYandexId(samaraWH.getYandexId())
                                        .number("SAMARA-01_01")
                                        .zoneId(samaraZone.getId())
                                        .build(),
                                CellRequestDto.builder()
                                        .type(CellType.BUFFER)
                                        .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                        .status(CellStatus.ACTIVE)
                                        .warehouseYandexId(samaraWH.getYandexId())
                                        .number("SAMARA-01_02")
                                        .zoneId(samaraZone.getId())
                                        .build()
                        )
                )
        ).andExpect(status().isOk());

        var cells = cellRepository.findAll();
        assertThat(cells)
                .hasSize(2)
                .allMatch(cell ->
                        cell.getType() == CellType.BUFFER &&
                                cell.getSubtype() == CellSubType.BUFFER_XDOC_LOCATION &&
                                cell.getStatus() == CellStatus.ACTIVE &&
                                Objects.equals(cell.getWarehouseYandexId(), samaraWH.getYandexId()) &&
                                Objects.equals(cell.getZone(), samaraZone)
                );

        assertThat(cells.stream().map(Cell::getScNumber))
                .containsExactlyInAnyOrder("SAMARA-01_01", "SAMARA-01_02");
    }

    @Test
    void getCellsForZone() {
        testFactory.setSortingCenterPropertyMap(sortingCenter, Map.of(
                        ENABLE_BUFFER_XDOC_LOCATION, true,
                        ENABLE_BUFFER_XDOC_BOX, true,
                        XDOC_ENABLED, true,
                        SORT_ZONE_ENABLED, true
                )
        );
        caller.createCellsManual(
                        sortingCenter.getId(),
                        new CreateTopologyRequest(List.of(
                                CellRequestDto.builder()
                                        .type(CellType.BUFFER)
                                        .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                        .status(CellStatus.ACTIVE)
                                        .warehouseYandexId(samaraWH.getYandexId())
                                        .number("SAMARA-01_01")
                                        .zoneId(samaraZone.getId())
                                        .build()
                        )))
                .andExpect(status().isOk());

        List<Cell> cells = cellRepository.findAllBySortingCenterAndZoneOrderById(sortingCenter, samaraZone);

        caller.getCellsForZone(sortingCenter.getId(), samaraZone.getId(), null)
                .andExpect(status().isOk())
                .andExpect(content().json(getExpectedResponseForCellsForZone(samaraZone.getId(), samaraZone.getName(),
                        cells, List.of())));

        // diff subtype, same zone
        caller.createCellsManual(
                        sortingCenter.getId(),
                        new CreateTopologyRequest(List.of(
                                CellRequestDto.builder()
                                        .type(CellType.BUFFER)
                                        .subType(BUFFER_XDOC_BOX)
                                        .status(CellStatus.ACTIVE)
                                        .warehouseYandexId(samaraWH.getYandexId())
                                        .number("SAMARA-01_01b")
                                        .zoneId(samaraZone.getId())
                                        .build()
                        )))
                .andExpect(status().isOk());

        List<Cell> withNewCell = cellRepository.findAllBySortingCenterAndZoneOrderById(sortingCenter, samaraZone);
        assertThat(withNewCell).hasSize(2);

        List<Cell> onlyNewCells = StreamEx.of(withNewCell).remove(cells::contains).toList();
        assertThat(onlyNewCells).hasSize(1);

        caller.getCellsForZone(sortingCenter.getId(), samaraZone.getId(), Set.of(BUFFER_XDOC_BOX))
                .andExpect(status().isOk())
                .andExpect(content().json(getExpectedResponseForCellsForZone(samaraZone.getId(), samaraZone.getName(),
                        onlyNewCells, List.of())));
    }

    @Test
    void getCellsForZoneWithSortable() {
        testFactory.setSortingCenterPropertyMap(sortingCenter, Map.of(
                        ENABLE_BUFFER_XDOC_LOCATION, true,
                        XDOC_ENABLED, true,
                        SORT_ZONE_ENABLED, true
                )
        );
        caller.createCellsManual(
                        sortingCenter.getId(),
                        new CreateTopologyRequest(List.of(
                                CellRequestDto.builder()
                                        .type(CellType.BUFFER)
                                        .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                        .status(CellStatus.ACTIVE)
                                        .warehouseYandexId(samaraWH.getYandexId())
                                        .number("SAMARA-01_01")
                                        .zoneId(samaraZone.getId())
                                        .build(),
                                CellRequestDto.builder()
                                        .type(CellType.BUFFER)
                                        .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                        .status(CellStatus.ACTIVE)
                                        .warehouseYandexId(samaraWH.getYandexId())
                                        .number("SAMARA-02_01")
                                        .zoneId(samaraZone.getId())
                                        .build()
                        )))
                .andExpect(status().isOk());

        flow.inboundBuilder("in-1")
                .nextLogisticPoint(samaraWH.getYandexId())
                .build()
                .linkPallets("XDOC-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-1");

        List<Cell> cells = cellRepository.findAllBySortingCenterAndZoneOrderById(sortingCenter, samaraZone);
        Sortable sortable = sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow();

        Cell occupiedCell = cells.stream()
                .filter(cell -> Objects.requireNonNull(sortable.getCell()).getId().equals(cell.getId()))
                .findAny()
                .orElseThrow();
        cells = StreamEx.of(cells).removeBy(Cell::getId, occupiedCell.getId()).toList();

        caller.getCellsForZone(sortingCenter.getId(), samaraZone.getId(), null)
                .andExpect(status().isOk())
                .andExpect(content().json(getExpectedResponseForCellsForZone(samaraZone.getId(), samaraZone.getName(),
                        cells, List.of(occupiedCell))));
    }

    @Test
    public void clearCell() {
        Cell cell = flow.createBufferCellAndGet("cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        flow.createInbound("in-1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-1");

        caller.clearCell(cell.getId())
                .andExpect(status().isOk());
        assertThat(sortableQueryService.find(sortingCenter, "XDOC-1").map(Sortable::getCell)).isEmpty();
    }

    @Test
    public void deleteCell() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().get();
        var route  = testFactory.findOutgoingRoute(order).orElseThrow();
        var cell = testFactory.findRouteCell(route, order).orElseThrow();
        testFactory.sortOrder(order, cell.getId());

        caller.deleteCell(cell.getId(), cell.getSortingCenter().getId())
                .andExpect(status().isOk());
        assertThat(testFactory.findRouteCell(route, order)).isEmpty();
        assertThat(testFactory.getCell(cell.getId()).isDeleted()).isTrue();
    }

    private String getExpectedResponseForCellsForZone(long zoneId, String zoneName, List<Cell> cellsFree,
                                                      List<Cell> cellsFull) {
        String cellsFreeStr = cellsFree.stream()
                .map(cell -> getExpectedResponse(cell, true))
                .collect(Collectors.joining(","));
        String cellsFullStr = cellsFull.stream()
                .map(cell -> getExpectedResponse(cell, false))
                .collect(Collectors.joining(","));
        String cells;
        if (!cellsFreeStr.isEmpty() && !cellsFullStr.isEmpty()) {
            cells = cellsFreeStr + ", " + cellsFullStr;
        } else if (cellsFullStr.isEmpty()) {
            cells = cellsFreeStr;
        } else {
            cells = cellsFullStr;
        }
        return """
                {
                    "id": %s,
                    "name": "%s",
                    "cells": [
                        %s
                    ]
                }
                """.formatted(zoneId, zoneName, cells);
    }

    private String getExpectedResponse(Cell cell, boolean empty) {
        return """
                {
                    "id": %s,
                    "scNumber": "%s",
                    "empty": %s
                }""".formatted(cell.getId(), cell.getScNumber(), empty);
    }
}
