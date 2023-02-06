package ru.yandex.market.sc.api.controller;

import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.api.util.ScApiControllerCaller;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.inbound.InboundCommandService;
import ru.yandex.market.sc.core.domain.inbound.InboundFacade;
import ru.yandex.market.sc.core.domain.inbound.model.CreateInboundRegistrySortableRequest;
import ru.yandex.market.sc.core.domain.inbound.model.LinkToInboundRequestDto;
import ru.yandex.market.sc.core.domain.inbound.model.RegistryUnitType;
import ru.yandex.market.sc.core.domain.lot.repository.Lot;
import ru.yandex.market.sc.core.domain.lot.repository.LotRepository;
import ru.yandex.market.sc.core.domain.partner.lot.PartnerLotDto;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogContext;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScApiControllerTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SortableControllerXDocTest {

    private final static String BARCODE_PALLET = "XDOC-PALLET-1";
    private final static String BARCODE_BOX = "XDOC-BOX-1";
    private static final String ANOMALY_BARCODE = "AN-12345";

    private final XDocFlow flow;
    private final ScApiControllerCaller caller;
    private final TestFactory testFactory;
    private final SortableRepository sortableRepository;
    private final SortableQueryService sortableQueryService;
    private final SortableTestFactory sortableTestFactory;
    private final LotRepository lotRepository;
    private final InboundCommandService inboundCommandService;
    private final InboundFacade inboundFacade;

    private SortingCenter sortingCenter;
    private Warehouse nextWarehouse;
    private Cell samaraCell;
    private Sortable lotInSamaraCellAsSortable;
    private Cell shipCell;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter.getId(), SortingCenterPropertiesKey.XDOC_ENABLED, true);
        nextWarehouse = testFactory.storedWarehouse("samara-warehouse");
        samaraCell = testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC, nextWarehouse.getYandexId(), null
        );
        shipCell = testFactory.storedCell(
                sortingCenter, "SHIP_1", CellType.COURIER, CellSubType.SHIP_XDOC, null, null
        );

        PartnerLotDto lotDto = sortableTestFactory.createEmptyLot(sortingCenter, samaraCell);
        Lot lotInSamaraCell = lotRepository.findByIdOrThrow(lotDto.getId());
        lotInSamaraCellAsSortable = sortableRepository.findByIdOrThrow(lotInSamaraCell.getSortableId());
    }

    @DisplayName("Сортировка палеты в ячейку хранения")
    @Test
    void sortPalletToCell() {
        flow.inboundBuilder("in-1")
                .nextLogisticPoint(nextWarehouse.getYandexId())
                .build()
                .linkPallets(BARCODE_PALLET);

        Sortable palletBeforeSort = find(BARCODE_PALLET);
        assertThat(palletBeforeSort).extracting(Sortable::getCell).isNull();

        caller.sort(BARCODE_PALLET, samaraCell)
                .andExpect(status().isOk());

        Sortable pallet = find(BARCODE_PALLET);
        assertThat(pallet).extracting(Sortable::getCell).isEqualTo(samaraCell);
    }

    @SneakyThrows
    @DisplayName("Сортировка аномалии в ячейку хранения")
    @Test
    void sortAnomalyToCell() {
        var inbound = flow.inboundBuilder("in-1")
                .nextLogisticPoint("0")
                .build().getInbound();

        inboundCommandService.createInboundRegistry(
                Collections.emptyList(),
                List.of(new CreateInboundRegistrySortableRequest(
                        inbound.getExternalId(),
                        ANOMALY_BARCODE,
                        RegistryUnitType.BOX
                )),
                inbound.getExternalId(),
                "TEST_REGESTRY",
                testFactory.getOrCreateAnyUser(inbound.getSortingCenter())
        );
        inboundFacade.linkToInbound(
                "in-1",
                new LinkToInboundRequestDto(ANOMALY_BARCODE, SortableType.XDOC_BOX),
                ScanLogContext.XDOC_ACCEPTANCE,
                flow.getUser()
        );
        inboundFacade.getXDocInboundForFix(
                ANOMALY_BARCODE,
                ScanLogContext.XDOC_ACCEPTANCE,
                flow.getUser()
        );

        Sortable palletBeforeSort = find(ANOMALY_BARCODE);
        assertThat(palletBeforeSort).extracting(Sortable::getCell).isNull();

        Cell anomalyCell = testFactory.storedCell(
                sortingCenter, "ANOMALY-1", CellType.BUFFER, CellSubType.BUFFER_XDOC, null, null
        );

        caller.sort(ANOMALY_BARCODE, anomalyCell)
                .andExpect(status().isOk());

        Sortable pallet = find(ANOMALY_BARCODE);
        assertThat(pallet).extracting(Sortable::getCell).isEqualTo(anomalyCell);
    }

    @DisplayName("Сортировка коробки в ячейку хранения")
    @Test
    void sortBoxToCell() {
        flow.inboundBuilder("in-1")
                .nextLogisticPoint(nextWarehouse.getYandexId())
                .build()
                .linkBoxes(BARCODE_BOX);

        Sortable boxBeforeSort = find(BARCODE_BOX);
        assertThat(boxBeforeSort.getCell()).isNull();

        caller.sort(BARCODE_BOX, samaraCell)
                .andExpect(status().isOk());

        Sortable box = find(BARCODE_BOX);
        assertThat(box).extracting(Sortable::getCell).isEqualTo(samaraCell);
    }

    @DisplayName("Сортировка коробки в лот")
    @Test
    void sortBoxToLot() {
        flow.inboundBuilder("in-1")
                .nextLogisticPoint(nextWarehouse.getYandexId())
                .build()
                .linkBoxes(BARCODE_BOX);

        Sortable boxBeforeSort = find(BARCODE_BOX);
        assertThat(boxBeforeSort)
                .extracting(Sortable::getCell, Sortable::getParent)
                .containsExactly(null, null);

        caller.sort(BARCODE_BOX, lotInSamaraCellAsSortable)
                .andExpect(status().isOk());

        Sortable boxAfterSortToLot = find(BARCODE_BOX);
        assertThat(boxAfterSortToLot)
                .extracting(Sortable::getCell, Sortable::getParent)
                .containsExactly(null, lotInSamaraCellAsSortable);

    }

    @DisplayName("Сортировка палеты ячейку отгрузки")
    @Test
    void sortPalletToShipCell() {
        flow.inboundBuilder("in-1")
                .nextLogisticPoint(nextWarehouse.getYandexId())
                .build()
                .linkPallets(BARCODE_PALLET);

        caller.sort(BARCODE_PALLET, samaraCell)
                .andExpect(status().isOk());

        // палета принята на РЦ и отсортирована на хранение
        Sortable palletBeforeOutbound = find(BARCODE_PALLET);
        assertThat(palletBeforeOutbound.getCell()).isEqualTo(samaraCell);

        flow.outboundBuilder("out-1")
                .toRegistryBuilder()
                .externalId("reg-1")
                .addRegistryPallets(BARCODE_PALLET)
                .buildRegistryAndGetOutbound();

        // сортировка палеты в ячейку отгрузки
        caller.sort(BARCODE_PALLET, shipCell)
                .andExpect(status().isOk());

        Sortable palletAfterSort = find(BARCODE_PALLET);
        assertThat(palletAfterSort.getCell()).isEqualTo(shipCell);
    }

    @DisplayName("Сортировка лота в ячейку отгрузки")
    @Test
    void sortLotToShipCell() {
        flow.inboundBuilder("in-1")
                .nextLogisticPoint(nextWarehouse.getYandexId())
                .build()
                .linkBoxes(BARCODE_BOX)
                .fixInbound();

        // коробка принята и размещена в лоте
        caller.sort(BARCODE_BOX, lotInSamaraCellAsSortable)
                .andExpect(status().isOk());

        // лот запакован
        caller.preship(lotInSamaraCellAsSortable.getId(),
                        lotInSamaraCellAsSortable.getType(),
                        SortableAPIAction.READY_FOR_PACKING)
                .andExpect(status().isOk());

        // создаем отгрузку
        flow.outboundBuilder("out-1")
                .toRegistryBuilder()
                .externalId("reg-1")
                .addRegistryPallets(lotInSamaraCellAsSortable.getRequiredBarcodeOrThrow())
                .addRegistryBoxes(BARCODE_BOX)
                .buildRegistryAndGetOutbound();

        // сортировка лота в ячейку отгрузки
        caller.sort(lotInSamaraCellAsSortable.getRequiredBarcodeOrThrow(), shipCell)
                .andExpect(status().isOk());

        Sortable lotAfterSort = find(lotInSamaraCellAsSortable.getRequiredBarcodeOrThrow());
        assertThat(lotAfterSort).extracting(Sortable::getCell).isEqualTo(shipCell);
    }


    @DisplayName("Сортировка не возможна, если первичная приемка не завершена")
    @Test
    void throwIfInboundAcceptanceNotFinished() {
        testFactory.setSortingCenterProperty(
                TestFactory.SC_ID, SortingCenterPropertiesKey.BLOCK_SORT_IF_ACCEPTANCE_NOT_FINISHED, true
        );

        flow.inboundBuilder("in-1")
                .nextLogisticPoint(nextWarehouse.getYandexId())
                .build()
                .carArrivedAndReadyToReceive()
                .linkBoxes(BARCODE_BOX);

        caller.accept(BARCODE_BOX, null)
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value("Чтобы сортировать, завершите приемку"));

        caller.sort(BARCODE_BOX, lotInSamaraCellAsSortable)
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value("Чтобы сортировать, завершите приемку"));
    }

    @DisplayName("После завершения первичной приемки сортировка и фиксация поставки продолжается без ошибок")
    @Test
    void sortingIsOkIfInboundAcceptanceFinished() {
        testFactory.setSortingCenterProperty(
                TestFactory.SC_ID, SortingCenterPropertiesKey.BLOCK_SORT_IF_ACCEPTANCE_NOT_FINISHED, true
        );

        flow.inboundBuilder("in-1")
                .nextLogisticPoint(nextWarehouse.getYandexId())
                .build()
                .carArrivedAndReadyToReceive()
                .linkBoxes(BARCODE_BOX)
                .finishAcceptance();

        caller.accept(BARCODE_BOX, null)
                .andExpect(status().isOk());

        // коробка принята и размещена в лоте
        caller.sort(BARCODE_BOX, lotInSamaraCellAsSortable)
                .andExpect(status().isOk());

        caller.fixInbound("in-1")
                .andExpect(status().isOk());
    }

    private Sortable find(String barcode) {
        return sortableQueryService.find(sortingCenter, barcode).orElseThrow();
    }

}
