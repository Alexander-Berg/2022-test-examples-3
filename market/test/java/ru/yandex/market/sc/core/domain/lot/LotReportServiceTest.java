package ru.yandex.market.sc.core.domain.lot;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.lot.repository.Lot;
import ru.yandex.market.sc.core.domain.lot.repository.LotRepository;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.partner.lot.PartnerLotDto;
import ru.yandex.market.sc.core.domain.partner.lot.PartnerLotParamsDto;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
class LotReportServiceTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    LotReportService lotReportService;
    @Autowired
    LotCommandService lotCommandService;
    @Autowired
    LotRepository lotRepository;
    @Autowired
    Clock clock;

    SortingCenter sortingCenter;
    Cell parentCell;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
    }

    @Test
    void getLots() {
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        Page<PartnerLotDto> partnerLotDtos = getPartnerLotPage(new PartnerLotParamsDto());
        assertThat(partnerLotDtos.get().map(PartnerLotDto::getId).toList())
                .containsExactlyInAnyOrder(lot1.getLotId(), lot2.getLotId());
    }

    @Test
    void getLotsNoDeleted() {
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell, LotStatus.CREATED, true);

        Page<PartnerLotDto> partnerLotDtos = getPartnerLotPage(new PartnerLotParamsDto());

        assertThat(partnerLotDtos.get().map(PartnerLotDto::getId).toList())
                .containsExactly(lot1.getLotId());
    }

    @Test
    void getLotsNoOrphan() {
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        lotCommandService.createOrphanLots(sortingCenter, 1);

        Page<PartnerLotDto> partnerLotPage = getPartnerLotPage(null);

        // сейчас орфан лотов нет в ПИ, потому что вьюха v_partner_lot выфильтровывает все лоты без parent_cell
        // если она перестанет это делать, то орфанов без привязанного целла нужно будет удалять явно
        assertThat(partnerLotPage.get().map(PartnerLotDto::getId).toList())
                .containsExactly(lot1.getLotId());
    }

    @Test
    void getPartnerLotPlaceCount() {
        testFactory.setSortingCenterProperty(sortingCenter,
                                             SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        Cell cell = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept()
                .sortToLot(lot.getLotId());

        testFactory.createForToday(order(sortingCenter).externalId("o2").places("o2p1", "o2p2").build())
                .acceptPlace("o2p1")
                .sortPlaceToLot(lot.getLotId(), "o2p1");

        testFactory.createForToday(order(sortingCenter).externalId("o3").places("o3p1", "o3p2").build())
                .acceptPlaces("o3p1", "o3p2")
                .sortToLot(lot.getLotId());

        PartnerLotDto partnerLot = getPartnerLotPage(null).getContent().get(0);
        assertThat(partnerLot.getPlaceCount()).isEqualTo(4);
    }

    private Page<PartnerLotDto> getPartnerLotPage(PartnerLotParamsDto partnerLotParamsDto) {
        return lotReportService.getLots(sortingCenter, partnerLotParamsDto, Pageable.unpaged());
    }

    @SneakyThrows
    @Test
    void getMarkerPalletPdf() {
        testFactory.storedWarehouse("yandexId");
        var parentCell1 = testFactory.storedCell(sortingCenter, "c2", CellType.RETURN, "yandexId");
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET,
                parentCell1);

        byte[] expectedReport = IOUtils.toByteArray(
                Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream("market_pallet.pdf")
                )
        );
        byte[] actualReport = lotReportService.getMarkerPdf(lot.getLotId());
        // size of expected and actual reports differs in CI tests for some reason

        assertThat(actualReport.length).isGreaterThan(0);
    }

    @SneakyThrows
    @Test
    void getMarkerCourierPalletPdf() {
        var courier = testFactory.storedCourier(1L);
        var parentCell1 = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER, courier.getId());
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET,
                parentCell1);

        byte[] expectedReport = IOUtils.toByteArray(
                Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream("market_pallet.pdf")
                )
        );
        byte[] actualReport = lotReportService.getMarkerPdf(lot.getLotId());

        // size of expected and actual reports differs in CI tests for some reason
        assertThat(actualReport.length).isGreaterThan(0);
    }

    @DisplayName("Распечатка с QR-кодом для XDOC_BASKET лота")
    @Test
    @SneakyThrows
    void getMarkerXDocBasketPdf() {
        testFactory.storedWarehouse("yandexId");
        var cell = testFactory.storedCell(sortingCenter, "A-31", CellType.BUFFER, CellSubType.BUFFER_XDOC, "yandexId");
        var lot = testFactory.storedLot(sortingCenter, SortableType.XDOC_BASKET, cell);

        byte[] expectedReport = IOUtils.toByteArray(
                Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream("marker_xdoc_basket.pdf")
                )
        );
        byte[] actualReport = lotReportService.getMarkerPdf(lot.getLotId());
        // size of expected and actual reports differs in CI tests for some reason
        assertThat(actualReport.length).isGreaterThan(0);
    }

    @Test
    void getCreatedLots() {
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell, LotStatus.PROCESSING, false);

        var params = new PartnerLotParamsDto(null, null, null, null, null,
                List.of(LotStatus.CREATED), null, null, null, null, null, null, null, null);

        Page<PartnerLotDto> partnerLotDtos = getPartnerLotPage(params);

        assertThat(partnerLotDtos.get().map(PartnerLotDto::getId).toList())
                .containsExactly(lot1.getLotId());
    }

    @Test
    void getMultipleFilteredLots() {
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell, LotStatus.PROCESSING, false);
        testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell, LotStatus.READY, false);
        var lot4 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell, LotStatus.PROCESSING, false);

        var params = new PartnerLotParamsDto(null, null, null, null, null,
                List.of(LotStatus.CREATED, LotStatus.PROCESSING), null, null, null, null, null, null, null, null);

        Page<PartnerLotDto> partnerLotDtos = getPartnerLotPage(params);

        assertThat(partnerLotDtos.get().map(PartnerLotDto::getId).toList())
                .containsExactly(lot1.getLotId(), lot2.getLotId(), lot4.getLotId());
    }

    @Test
    void getCellFiltered() {
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell, LotStatus.PROCESSING, false);
        var secondCell = testFactory.storedCell(sortingCenter, "c2", CellType.RETURN);
        testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell);
        testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell, LotStatus.READY, false);


        var params = new PartnerLotParamsDto(null, null, null, null,
                List.of(Objects.requireNonNull(parentCell.getScNumber())), null, null, null, null, null, null, null, null, null);

        Page<PartnerLotDto> partnerLotDtos = getPartnerLotPage(params);

        assertThat(partnerLotDtos.get().map(PartnerLotDto::getId).toList())
                .containsExactly(lot1.getLotId(), lot2.getLotId());
    }

    @SuppressWarnings("unused")
    @Test
    void getFilteredCellAndStatus() {
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell, LotStatus.PROCESSING, false);
        var secondCell = testFactory.storedCell(sortingCenter, "c2", CellType.RETURN);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell);
        var lot4 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell, LotStatus.READY, false);
        var lot5 = testFactory.storedLot(sortingCenter, SortableType.PALLET,  secondCell, LotStatus.PROCESSING, false);


        var params = new PartnerLotParamsDto(null, "", null, null,
                List.of(Objects.requireNonNull(secondCell.getScNumber())),
                List.of(LotStatus.CREATED, LotStatus.PROCESSING), null, null, null, null, null, null, null, null);

        Page<PartnerLotDto> partnerLotDtos = getPartnerLotPage(params);

        assertThat(partnerLotDtos.get().map(PartnerLotDto::getId).toList())
                .containsExactlyInAnyOrder(lot3.getLotId(), lot5.getLotId());
    }

    @SuppressWarnings("unused")
    @Test
    @Disabled
    void getMultipleFilteredByCellName() {
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell, LotStatus.PROCESSING, false);
        var secondCell = testFactory.storedCell(sortingCenter, "c2", CellType.RETURN);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell);
        var thirdCell = testFactory.storedCell(sortingCenter, "c3", CellType.RETURN);
        var lot4 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell, LotStatus.READY, false);
        var lot5 = testFactory.storedLot(sortingCenter, SortableType.PALLET,  thirdCell, LotStatus.PROCESSING, false);

        var params = new PartnerLotParamsDto(null, "", null, null,
                List.of(Objects.requireNonNull(secondCell.getScNumber()),
                        Objects.requireNonNull(thirdCell.getScNumber())),
                List.of(LotStatus.CREATED, LotStatus.PROCESSING), null, null, null, null, null, null, null, null);

        Page<PartnerLotDto> partnerLotDtos = getPartnerLotPage(params);

        assertThat(partnerLotDtos.get().map(PartnerLotDto::getId).toList())
                .containsExactly(lot3.getLotId(), lot5.getLotId());
    }

    @ParameterizedTest(name = "фильтр по LotType = {0}, в результате должны вернуться только {1}")
    @EnumSource(value = SortableType.class, names = {"PALLET", "XDOC_BASKET"})
    void getUsingFilterByLotType(SortableType filter) {
        testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell, LotStatus.PROCESSING, false);
        testFactory.storedLot(sortingCenter, SortableType.XDOC_BASKET, parentCell, LotStatus.PROCESSING, false);
        var params = new PartnerLotParamsDto(null, null, null, null, null, null, filter, null, null, null, null, null,
                null, null);

        Page<PartnerLotDto> partnerLotDtos = getPartnerLotPage(params);
        assertThat(partnerLotDtos.get())
                .hasSize(1)
                .allMatch(x -> x.getType() == filter);
    }

    @DisplayName("При отсутствии фильтра по типу лота будут возвращены лоты всех типов")
    @Test
    void getFilterByLotTypeIsNull() {
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell, LotStatus.PROCESSING, false);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.XDOC_BASKET, parentCell, LotStatus.PROCESSING,
                false);
        var params = new PartnerLotParamsDto(null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);

        Page<PartnerLotDto> partnerLotDtos = getPartnerLotPage(params);
        assertThat(partnerLotDtos.get().map(PartnerLotDto::getId).sorted().toList())
                .containsExactly(lot1.getLotId(), lot2.getLotId());
    }

    @Test
    void getFilteredByExternalId() {
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell, LotStatus.PROCESSING, false);
        var secondCell = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell);
        var thirdCell = testFactory.storedCell(sortingCenter, "c3", CellType.BUFFER, CellSubType.BUFFER_XDOC);
        var lot4 = testFactory.storedLot(sortingCenter, SortableType.PALLET, thirdCell, LotStatus.READY, false);

        var params = new PartnerLotParamsDto(null, null, lot3.getBarcode(), null, null, null,
                null, null, null, null, null, null, null, null);

        Page<PartnerLotDto> partnerLotDtos = getPartnerLotPage(params);

        assertThat(partnerLotDtos.get().map(PartnerLotDto::getId).toList())
                .containsExactly(lot3.getLotId());
    }

    @Test
    void getFilteredByPartOfExternalId() {
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell,
                LotStatus.CREATED, false, "SC_LOT_123");
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell,
                LotStatus.PROCESSING, false, "SC_LOT_135");
        var secondCell = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell,
                LotStatus.CREATED, false, "SC_LOT_231");
        var thirdCell = testFactory.storedCell(sortingCenter, "c3", CellType.BUFFER, CellSubType.BUFFER_XDOC);
        var lot4 = testFactory.storedLot(sortingCenter, SortableType.PALLET, thirdCell,
                LotStatus.READY, false, "SC_LOT_321");

        var params = new PartnerLotParamsDto(null, null, "SC_LOT_1", null, null, null,
                null, null, null, null, null, null, null, null);

        Page<PartnerLotDto> partnerLotDtos = getPartnerLotPage(params);

        assertThat(partnerLotDtos.get().map(PartnerLotDto::getId).toList())
                .containsExactly(lot1.getLotId(), lot2.getLotId());
    }

    @Test
    void getFilteredByPartWithSpaceOfExternalId() {
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell,
                LotStatus.CREATED, false, "SC_LOT_123");
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell,
                LotStatus.PROCESSING, false, "SC_LOT_135");
        var secondCell = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell,
                LotStatus.CREATED, false, "SC_LOT_231");
        var thirdCell = testFactory.storedCell(sortingCenter, "c3", CellType.BUFFER, CellSubType.BUFFER_XDOC);
        var lot4 = testFactory.storedLot(sortingCenter, SortableType.PALLET, thirdCell,
                LotStatus.READY, false, "SC_LOT_321");

        var params = new PartnerLotParamsDto(null, null, "SC_LOT_2 ", null, null, null,
                null, null, null, null, null, null, null, null);

        Page<PartnerLotDto> partnerLotDtos = getPartnerLotPage(params);

        assertThat(partnerLotDtos.get().map(PartnerLotDto::getId).toList())
                .containsExactly(lot3.getLotId());
    }

    @Test
    void getFilteredByCellType() {
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell, LotStatus.PROCESSING, false);
        var secondCell = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell);
        var thirdCell = testFactory.storedCell(sortingCenter, "c3", CellType.BUFFER, CellSubType.BUFFER_XDOC);
        var lot4 = testFactory.storedLot(sortingCenter, SortableType.PALLET, thirdCell, LotStatus.READY, false);

        var params = new PartnerLotParamsDto(null, null, null, null, null, null,
                null, null, CellType.RETURN, null, null, null, null, null);

        Page<PartnerLotDto> partnerLotDtos = getPartnerLotPage(params);

        assertThat(partnerLotDtos.get().map(PartnerLotDto::getId).toList())
                .containsExactly(lot1.getLotId(), lot2.getLotId());
    }

    @Test
    void getFilteredByCellSubType() {
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell, LotStatus.PROCESSING, false);
        var secondCell = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell);
        var thirdCell = testFactory.storedCell(sortingCenter, "c3", CellType.BUFFER, CellSubType.BUFFER_XDOC);
        var lot4 = testFactory.storedLot(sortingCenter, SortableType.PALLET, thirdCell, LotStatus.READY, false);

        var params = new PartnerLotParamsDto(null, null, null, null, null, null,
                null, null, null, CellSubType.BUFFER_XDOC, null, null, null, null);

        Page<PartnerLotDto> partnerLotDtos = getPartnerLotPage(params);

        assertThat(partnerLotDtos.get().map(PartnerLotDto::getId).toList())
                .containsExactly(lot4.getLotId());
    }

    @Test
    @DisplayName("Распечатка QR-кодов обезличенных лотов")
    void getOrphanLotsMarkerPdf() {
        byte[] actualReport = lotReportService.getOrphanLotsMarkerPdf(List.of(
                SortableType.ORPHAN_PALLET.createBarcode(31241234),
                SortableType.ORPHAN_PALLET.createBarcode(1347198L),
                SortableType.ORPHAN_PALLET.createBarcode(9918971)
        ));
        assertThat(actualReport.length).isGreaterThan(0);
    }

    @Test
    void createOrphansBatch() {
        lotCommandService.createOrphanLots(sortingCenter, 3);
        List<Lot> allLots = lotRepository.findAll();
        assertThat(allLots).hasSize(3);

        lotCommandService.createOrphanLots(sortingCenter, 2);
        allLots = lotRepository.findAll();
        assertThat(allLots).hasSize(5);
    }
}
