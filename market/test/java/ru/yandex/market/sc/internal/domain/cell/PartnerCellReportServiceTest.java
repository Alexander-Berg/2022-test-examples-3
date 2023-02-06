package ru.yandex.market.sc.internal.domain.cell;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.model.PartnerCellDto;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.cell.repository.CellJdbcRepository;
import ru.yandex.market.sc.core.domain.cell.repository.CellMapper;
import ru.yandex.market.sc.core.domain.cell.repository.CellRepository;
import ru.yandex.market.sc.core.domain.partner.cell.PartnerCellParamsDto;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.zone.repository.Zone;
import ru.yandex.market.sc.core.domain.zone.repository.ZoneMapper;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.CELL_ADDRESS_ENABLED;

/**
 * @author hardlight
 */
@EmbeddedDbIntTest
public class PartnerCellReportServiceTest {

    @Autowired
    PartnerCellReportService partnerCellReportService;
    @Autowired
    ZoneMapper zoneMapper;

    @Autowired
    TestFactory testFactory;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    CellRepository cellRepository;

    @Autowired
    CellJdbcRepository cellJdbcRepository;

    @Autowired
    CellMapper cellMapper;

    private SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void getCellWithSequenceNumber() {
        Long sequenceNumber = 2L;
        Cell cell = testFactory.storedCell(new Cell(sortingCenter, "cell-1", CellStatus.ACTIVE,
                CellType.COURIER, CellSubType.DEFAULT, false, null, null,
                null, sequenceNumber, null, null, null, null, null));

        assertCellsEqual(List.of(cell), getReportCells(
                PartnerCellParamsDto.builder().number("cell-1").build())
        );
    }

    @Test
    void getCellWithNumber() {
        Cell cell = testFactory.storedCell(sortingCenter, "cell-1");

        assertCellsEqual(List.of(cell), getReportCells(
                PartnerCellParamsDto.builder().number("cell-1").build())
        );
    }

    @Test
    void getCellWithPartialNumber() {
        Cell cell = testFactory.storedCell(sortingCenter, "cell-1");
        assertCellsEqual(List.of(cell), getReportCells(
                PartnerCellParamsDto.builder().number("ell-").build())
        );
    }

    @Test
    void getCellWithWrongNumber() {
        testFactory.storedCell(sortingCenter, "cell-1");
        assertCellsEqual(Collections.emptyList(), getReportCells(
                PartnerCellParamsDto.builder().number("wrong").build())
        );
    }

    @Test
    void getCellWithStatus() {
        Cell cell = testFactory.storedCell(sortingCenter, CellStatus.ACTIVE);

        assertCellsEqual(List.of(cell), getReportCells(
                PartnerCellParamsDto.builder().status(CellStatus.ACTIVE).build())
        );
    }

    @Test
    void getCellWithType() {
        Cell cell = testFactory.storedCell(sortingCenter, CellType.COURIER, CellSubType.DEFAULT, null);

        assertCellsEqual(List.of(cell), getReportCells(
                PartnerCellParamsDto.builder().type(CellType.COURIER).build())
        );
    }

    @Test
    void getCellWithSubType() {
        Cell cell = testFactory.storedCell(sortingCenter, CellType.BUFFER, CellSubType.DROPPED_ORDERS, null);

        assertCellsEqual(List.of(cell), getReportCells(
                PartnerCellParamsDto.builder().subType(CellSubType.DROPPED_ORDERS).build())
        );
    }

    @Test
    void getCellWithZoneId() {
        Zone zone = testFactory.storedZone(sortingCenter);
        Cell cell = testFactory.storedCell(sortingCenter, zone);

        assertCellsEqual(List.of(cell), getReportCells(
                PartnerCellParamsDto.builder().zoneId(zone.getId()).build())
        );
    }

    @Test
    void getCellWithSortOrdersCount() {
        var cell1 = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER);
        var cell2 = testFactory.storedCell(sortingCenter, "c-2", CellType.COURIER);

        testFactory.createOrderForToday(sortingCenter).accept().sort(cell1.getId()).get();

        assertCellsEqual(List.of(cell2, cell1), getReportCells(
                PartnerCellParamsDto.builder().type(CellType.COURIER).build(),
                PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "placeCount")))
        );
    }

    @Test
    void getCellWithSortByNumber() {
        var cell1 = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER);
        var cell2 = testFactory.storedCell(sortingCenter, "c-2", CellType.COURIER);
        var cell3 = testFactory.storedCell(sortingCenter, "d-2", CellType.COURIER);
        var cell4 = testFactory.storedCell(sortingCenter, "d-1", CellType.COURIER);

        testFactory.createOrderForToday(sortingCenter).accept().sort(cell1.getId()).get();

        assertCellsEqual(List.of(cell1, cell2, cell4, cell3), getReportCells(
                PartnerCellParamsDto.builder().type(CellType.COURIER).build(),
                PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "number")))
        );
    }

    @Test
    void getCellWithSortByNumberDesc() {
        var cell1 = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER);
        var cell2 = testFactory.storedCell(sortingCenter, "c-2", CellType.COURIER);
        var cell3 = testFactory.storedCell(sortingCenter, "d-2", CellType.COURIER);
        var cell4 = testFactory.storedCell(sortingCenter, "d-1", CellType.COURIER);

        testFactory.createOrderForToday(sortingCenter).accept().sort(cell1.getId()).get();

        assertCellsEqual(List.of(cell3, cell4, cell2, cell1), getReportCells(
                PartnerCellParamsDto.builder().type(CellType.COURIER).build(),
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "number")))
        );
    }

    /**
     * Проверка сортировки по cellAddress
     * сортировка по зоне A-Z
     * сортировка по аллее 1-9
     * сортировка по сегменту 1-9
     * сортировка по уровню 0-99
     *
     * A-0001-0001-01
     * A-0001-0004-02
     * A-0001-0004-03
     * A-0003-0004-05
     * c-2 (с зоной A)
     * I-0001-0002-01
     * c-1 (без адреса)
     */
    @Test
    void getCellWithSortByCellAddress() {
        var courier = testFactory.storedCourier(1);
        var zoneA = testFactory.storedZone(sortingCenter, "A");
        var zoneI = testFactory.storedZone(sortingCenter, "I");
        var cell1 = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER);
        var cell2 = testFactory.storedShipBufferCell(sortingCenter, 1, zoneA, 3, 4, 5, 1);
        var cell3 = testFactory.storedShipBufferCell(sortingCenter, 1, zoneI, 1, 2, 1, 1);
        var cell4 = testFactory.storedShipBufferCell(sortingCenter, 1, zoneA, 1, 4, 3, 1);
        var cell5 = testFactory.storedShipBufferCell(sortingCenter, 1, zoneA, 1, 1, 1, 1);
        var cell6 = testFactory.storedShipBufferCell(sortingCenter, 1, zoneA, 1, 4, 2, 1);
        var cell7 = testFactory.storedCell(sortingCenter, CellType.COURIER, CellSubType.SHIP_BUFFER, zoneA, "c-2");

        testFactory.createOrderForToday(sortingCenter).accept().sort(cell1.getId()).get();

        assertCellsEqual(List.of(cell5, cell6, cell4, cell2, cell7, cell3, cell1), getReportCells(
                PartnerCellParamsDto.builder().type(CellType.COURIER).build(),
                PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "cellAddress")))
        );
    }

    @Test
    void getCellWithSortByCellAddressDesc() {
        var courier = testFactory.storedCourier(1);
        var zoneA = testFactory.storedZone(sortingCenter, "A");
        var zoneI = testFactory.storedZone(sortingCenter, "I");
        var cell1 = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER);
        var cell2 = testFactory.storedShipBufferCell(sortingCenter, 1, zoneA, 3, 4, 5, 1);
        var cell3 = testFactory.storedShipBufferCell(sortingCenter, 1, zoneI, 1, 2, 1, 1);
        var cell4 = testFactory.storedShipBufferCell(sortingCenter, 1, zoneA, 1, 4, 3, 1);
        var cell5 = testFactory.storedShipBufferCell(sortingCenter, 1, zoneA, 1, 1, 1, 1);
        var cell6 = testFactory.storedShipBufferCell(sortingCenter, 1, zoneA, 1, 4, 2, 1);
        var cell7 = testFactory.storedCell(sortingCenter, CellType.COURIER, CellSubType.SHIP_BUFFER, zoneA, "c-2");

        testFactory.createOrderForToday(sortingCenter)
                .accept()
                .sort(cell1.getId())
                .get();

        assertCellsEqual(List.of(cell1, cell3, cell7, cell2, cell4, cell6, cell5), getReportCells(
                PartnerCellParamsDto.builder().type(CellType.COURIER).build(),
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "cellAddress")))
        );
    }

    @Test
    void getCellNotDeleted() {
        var cell = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER);
        var deletedCell = testFactory.storedCell(sortingCenter, CellType.COURIER, CellSubType.DEFAULT, null, true);

        testFactory.createOrderForToday(sortingCenter).accept().sort(cell.getId()).get();

        assertCellsEqual(List.of(cell), getReportCells(
                PartnerCellParamsDto.builder().type(CellType.COURIER).build(),
                PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "placeCount")))
        );
    }

    @Test
    @SneakyThrows
    void getPdfWithAllCellsInsideZone() {
        var zone = testFactory.storedZone(sortingCenter, "samara");
        testFactory.storedCell(sortingCenter, "samara-00.01", CellType.BUFFER, CellSubType.BUFFER_XDOC, null, zone);
        testFactory.storedCell(sortingCenter, "samara-00.02", CellType.BUFFER, CellSubType.BUFFER_XDOC, null, zone);
        testFactory.storedCell(sortingCenter, "samara-00.03", CellType.BUFFER, CellSubType.BUFFER_XDOC, null, zone);
        testFactory.storedCell(sortingCenter, "samara-00.04", CellType.BUFFER, CellSubType.BUFFER_XDOC, null, null);
        testFactory.storedCell(sortingCenter, "samara-01.03", CellType.BUFFER, CellSubType.BUFFER_XDOC, null, zone);
        testFactory.storedCell(sortingCenter, null, CellType.BUFFER, CellSubType.BUFFER_XDOC, null, zone);

        byte[] actualPDF = partnerCellReportService.getCellsQRCodesBy(sortingCenter, zone.getId(), "samara-00");

        byte[] expectedPDF = IOUtils.toByteArray(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(
                                "cells_qr_codes_by_zone.pdf"
                        )
                )
        );

//        Files.write(Paths.get("out.pdf"), actualPDF);

        assertThat(actualPDF).contains(expectedPDF);
    }

    @Test
    @SneakyThrows
    void getShipBufferCellLabelPdf() {
        testFactory.setSortingCenterProperty(sortingCenter, CELL_ADDRESS_ENABLED, true);

        var zone = testFactory.storedZone(sortingCenter, "ZONE-A");
        var courier = testFactory.storedCourier(1000L, "СЦ МК Тарный - служба доставки");
        var cell = testFactory.storedShipBufferCell(sortingCenter, courier.getId(), zone, 1, 2, 3, 4);

        byte[] actualPDF = partnerCellReportService.getCellLabelPdf(cell.getId());

        byte[] expectedPDF = IOUtils.toByteArray(Objects.requireNonNull(
                TestFactory.class.getClassLoader().getResourceAsStream("cell_label.pdf"))
        );
        assertThat(actualPDF).contains(expectedPDF);
    }

    @Test
    @SneakyThrows
    void getCellLabelPdfWithoutAddress() {
        testFactory.setSortingCenterProperty(sortingCenter, CELL_ADDRESS_ENABLED, true);
        var cell = testFactory.storedCell(sortingCenter, "Ячейка", CellType.COURIER, CellSubType.DEFAULT);

        byte[] actualPDF = partnerCellReportService.getCellLabelPdf(cell.getId());

        byte[] expectedPDF = IOUtils.toByteArray(Objects.requireNonNull(
                TestFactory.class.getClassLoader().getResourceAsStream("cell_label_wo_address.pdf"))
        );
        assertThat(actualPDF).contains(expectedPDF);
    }

    private List<PartnerCellDto> getReportCells(PartnerCellParamsDto params) {
        return getReportCells(params, Pageable.unpaged());
    }

    private List<PartnerCellDto> getReportCells(PartnerCellParamsDto params, Pageable pageable) {
        return partnerCellReportService.getNotDeletedCells(sortingCenter, params, pageable)
                .get().toList();
    }

    private void assertCellsEqual(List<Cell> cells, List<PartnerCellDto> reportedCells) {
        assertThat(reportedCells)
                .usingElementComparatorIgnoringFields("canBeDeleted", "canBeUpdated", "courierName", "warehouseName")
                .isEqualTo(cells
                        .stream()
                        .map(this::buildFromCell)
                        .toList()
                );
    }

    private PartnerCellDto buildFromCell(Cell cell) {
        int placeCount = testFactory.placesCount(cell);
        return cellMapper.mapToPartner(cell, true, placeCount, 0);
    }

}
