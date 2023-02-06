package ru.yandex.market.sc.api.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.api.util.ScApiControllerCaller;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.cell.CellCommandService;
import ru.yandex.market.sc.core.domain.cell.CellQueryService;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.model.PartnerCellDto;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.ClientReturnService;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.inbound.InboundCommandService;
import ru.yandex.market.sc.core.domain.inbound.InboundFacade;
import ru.yandex.market.sc.core.domain.inbound.model.CreateInboundRegistrySortableRequest;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.model.LinkToInboundRequestDto;
import ru.yandex.market.sc.core.domain.inbound.model.RegistryUnitType;
import ru.yandex.market.sc.core.domain.lot.LotCommandService;
import ru.yandex.market.sc.core.domain.lot.model.TransferFromLotToLotRequestDto;
import ru.yandex.market.sc.core.domain.lot.repository.Lot;
import ru.yandex.market.sc.core.domain.lot.repository.LotRepository;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.model.ApiCellLotDto;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderDto;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.order.model.CreateReturnRequest;
import ru.yandex.market.sc.core.domain.order.model.DeletedSegmentRequest;
import ru.yandex.market.sc.core.domain.order.model.OrderReturnType;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.scan.model.AcceptReturnedOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogContext;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.stage.StageLoader;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.model.WarehouseDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author ogonek
 */
@ScApiControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ExtendWith(DefaultScUserWarehouseExtension.class)
public class OrderControllerSortableTest {

    private static final long UID = TestFactory.USER_UID_LONG;
    private static final String ANOMALY_BARCODE = "AN-12345";

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final SortableTestFactory sortableTestFactory;
    private final SortableLotService sortableLotService;
    private final LotRepository lotRepository;
    private final PlaceRepository placeRepository;
    private final CellQueryService cellQueryService;
    private final CellCommandService cellCommandService;
    private final LotCommandService lotCommandService;
    private final SortableQueryService sortableQueryService;
    private final XDocFlow flow;
    private final ScApiControllerCaller caller;
    private final InboundCommandService inboundCommandService;
    private final InboundFacade inboundFacade;
    private final TransactionTemplate transactionTemplate;
    private final ScanService scanService;
    private final OrderCommandService orderCommandService;

    private SortingCenter sortingCenter;
    private Warehouse warehouse;


    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        warehouse = testFactory.storedWarehouse();
    }

    @Test
    void getXdocPalletTest() {
        Cell cell = testFactory.storedCell(sortingCenter, "BUF_1", CellType.BUFFER,
                CellSubType.BUFFER_XDOC);
        flow.createInbound("IN-1")
                .linkPallets("XDOC-p1")
                .fixInbound();

        Sortable sortable = sortableQueryService.find(sortingCenter, "XDOC-p1").orElseThrow();
        caller.getOrder("XDOC-p1")
                .andExpect(status().isOk())
                .andExpect(content().json(expectedCreated(sortable, cell.getId())));
    }

    @Test
    @SneakyThrows
    void acceptXdocPallet() {
        flow.createInbound("IN-1")
                .linkPallets("XDOC-p1")
                .fixInbound()
                .createOutbound("OUT-1")
                .buildRegistry("XDOC-p1")
                .sortToAvailableCell("XDOC-p1")
                .prepareToShip("XDOC-p1");

        Sortable sortable = sortableQueryService.find(sortingCenter, "XDOC-p1").orElseThrow();
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/orders/accept")
                                .header("Authorization", "OAuth uid-" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptBody(sortable.getRequiredBarcodeOrThrow()))
                )
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void acceptXdocAnomaly() {
        var inbound = flow.inboundBuilder("in-1")
            .nextLogisticPoint("0")
            .type(InboundType.XDOC_ANOMALY)
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

        Cell anomalyCell = testFactory.storedCell(
            sortingCenter, "ANOMALY-1", CellType.BUFFER, CellSubType.BUFFER_XDOC, "0", null
        );

        Sortable sortable = sortableQueryService.find(sortingCenter, ANOMALY_BARCODE).orElseThrow();
        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/orders/accept")
                    .header("Authorization", "OAuth uid-" + UID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(acceptBody(sortable.getRequiredBarcodeOrThrow()))
            )
            .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void sortXdocPalletWithoutCellTest() {
        flow.createInbound("IN-1")
                .linkPallets("XDOC-p1")
                .fixInbound()
                .createOutbound("OUT-1")
                .buildRegistry("XDOC-p1")
                .sortToAvailableCell("XDOC-p1");

        var cell = testFactory.storedCell(sortingCenter, "c-5", CellType.COURIER);
        Sortable sortable = sortableQueryService.find(sortingCenter, "XDOC-p1").orElseThrow();
        caller.sort(sortable.getRequiredBarcodeOrThrow(), cell)
                .andExpect(status().is4xxClientError());
    }

    @Test
    @SneakyThrows
    void sortXdocPalletWithApplicableCellTest() {
        var cell = testFactory.storedCell(sortingCenter, "c-5", CellType.COURIER, CellSubType.SHIP_XDOC);
        flow.createInbound("IN-1")
                .linkPallets("XDOC-p1")
                .fixInbound()
                .createOutbound("OUT-1")
                .buildRegistry("XDOC-p1")
                .sortToAvailableCell("XDOC-p1");

        Sortable sortable = sortableQueryService.find(sortingCenter, "XDOC-p1").orElseThrow();
        caller.sort(sortable.getRequiredBarcodeOrThrow(), cell)
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("{\"destination\":{\"id\":\"" + cell.getId() + "\",\"name\":\"c-5\",\"type\":\"CELL\"},\"parentRequired\":false}"));
    }

    @Test
    @SneakyThrows
    void sortXdocPalletWithApplicableBufferCellTest() {
        Cell cell = testFactory.storedCell(sortingCenter, "cell-1", CellType.BUFFER, CellSubType.BUFFER_XDOC,
                TestFactory.WAREHOUSE_YANDEX_ID);
        flow.createInbound("IN-1")
                .linkPallets("XDOC-p1")
                .fixInbound();

        Sortable sortable = sortableQueryService.find(sortingCenter, "XDOC-p1").orElseThrow();
        caller.sort(sortable.getRequiredBarcodeOrThrow(), cell)
                .andExpect(status().isOk());
    }

    private String acceptBody(String externalId) {
        return "{\"externalId\":\"" + externalId + "\"}";
    }

    private static String expectedCreated(Sortable sortable, long bufferCellId) {
        return String.format("""
                {
                  "id": %1$s,
                  "externalId": "XDOC-p1",
                  "status": "KEEP",
                  "warehouse": {
                    "id": 0,
                    "name": ""
                  },
                  "lotSortAvailable": false,
                  "availableCells": [
                    {
                      "id": %2$s,
                      "status": "ACTIVE",
                      "type": "BUFFER",
                      "subType": "BUFFER_XDOC"
                    }
                  ]
                }""", sortable.getId(), bufferCellId);
    }

    @Test
    @SneakyThrows
    void getCellToDestinationForXdocPallet_useGeneralBufferCellCreated() {
        flow
                .createInbound("IN-1")
                .linkPallets("XDOC-p1")
                .fixInbound();

        caller.getOrder("XDOC-p1")
                .andExpect(content().json("{\"availableCells\": [{\"status\": \"NOT_ACTIVE\", \"type\": \"BUFFER\", " +
                        "\"subType\": \"BUFFER_XDOC\"}]}"));

        List<PartnerCellDto> cells = StreamEx.of(cellQueryService.getPartnerCells(sortingCenter))
                .filterBy(PartnerCellDto::getType, CellType.BUFFER)
                .filterBy(PartnerCellDto::getSubType, CellSubType.BUFFER_XDOC)
                .filterBy(PartnerCellDto::getWarehouseYandexId, null)
                .toList();
        assertThat(cells).hasSize(1);
        assertThat(cells.get(0).getWarehouseYandexId()).isNull();
    }

    @Test
    @SneakyThrows
    void getCellToDestinationForXdocPallet_useExistedCell() {

        Cell cell = testFactory.storedCell(sortingCenter, "general", CellType.BUFFER, CellSubType.BUFFER_XDOC);
        Cell cellDest = testFactory.storedCell(sortingCenter, "destination", CellType.BUFFER, CellSubType.BUFFER_XDOC
                , TestFactory.WAREHOUSE_YANDEX_ID);

        flow.createInbound("IN-1")
                .linkPallets("XDOC-p1")
                .fixInbound();

        caller.getOrder("XDOC-p1")
                .andExpect(content().json("{\"availableCells\": [{\"number\": \"" + cellDest.getScNumber() + "\", " +
                        "\"status\": " +
                        "\"ACTIVE\", \"type\": \"BUFFER\", " +
                        "\"subType\": \"BUFFER_XDOC\"}]}"));

        List<PartnerCellDto> cells = StreamEx.of(cellQueryService.getPartnerCells(sortingCenter))
                .filterBy(PartnerCellDto::getType, CellType.BUFFER)
                .filterBy(PartnerCellDto::getSubType, CellSubType.BUFFER_XDOC)
                .filterBy(PartnerCellDto::getWarehouseYandexId, warehouse.getYandexId())
                .toList();
        assertThat(cells).hasSize(1);
        assertThat(cells).anyMatch(partnerCellDto -> partnerCellDto.getWarehouseYandexId().equals(warehouse.getYandexId()));
    }

    @Test
    @SneakyThrows
    void getCellToDestinationForXdocPallet_differentCellDestination() {
        Warehouse warehouse = testFactory.storedWarehouse("NEW_WHS");
        Cell cell = testFactory.storedCell(sortingCenter, "general", CellType.BUFFER, CellSubType.BUFFER_XDOC,
                warehouse.getYandexId());
        Cell cellDest = testFactory.storedCell(sortingCenter, "destination", CellType.BUFFER, CellSubType.BUFFER_XDOC
                , TestFactory.WAREHOUSE_YANDEX_ID);
        flow
                .createInbound("IN-1")
                .linkPallets("XDOC-p1")
                .fixInbound();
        flow
                .inboundBuilder("IN-2")
                .nextLogisticPoint(warehouse.getYandexId())
                .build()
                .linkPallets("XDOC-p2")
                .fixInbound();

        caller.getOrder("XDOC-p1")
                .andExpect(content().json("{\"availableCells\": [{\"number\": \"" + cellDest.getScNumber() + "\", " +
                        "\"status\": " +
                        "\"ACTIVE\", \"type\": \"BUFFER\", " +
                        "\"subType\": \"BUFFER_XDOC\"}]}"));

        caller.getOrder("XDOC-p2")
                .andExpect(content().json("{\"availableCells\": [{\"number\": \"" + cell.getScNumber() + "\", " +
                        "\"status\": " +
                        "\"ACTIVE\", \"type\": \"BUFFER\", " +
                        "\"subType\": \"BUFFER_XDOC\"}]}"));

        List<PartnerCellDto> cells = StreamEx.of(cellQueryService.getPartnerCells(sortingCenter))
                .filterBy(PartnerCellDto::getType, CellType.BUFFER)
                .filterBy(PartnerCellDto::getSubType, CellSubType.BUFFER_XDOC)
                .toList();
        assertThat(cells).hasSize(2);
        assertThat(cells)
                .anyMatch(partnerCellDto -> partnerCellDto.getWarehouseYandexId()
                        .equals(TestFactory.WAREHOUSE_YANDEX_ID));
        assertThat(cells)
                .anyMatch(partnerCellDto -> partnerCellDto.getWarehouseYandexId()
                        .equals(warehouse.getYandexId()));
    }

    @Test
    @SneakyThrows
    void getBufferCellForXdocPalletWithoutDestination() {
        flow.createInbound("IN-1")
                .linkPallets("XDOC-p1")
                .fixInbound();

        PartnerCellDto cell = StreamEx.of(cellQueryService.getPartnerCells(sortingCenter))
                .filterBy(PartnerCellDto::getType, CellType.BUFFER)
                .filterBy(PartnerCellDto::getSubType, CellSubType.BUFFER_XDOC)
                .findFirst()
                .orElseThrow();

        caller.getOrder("XDOC-p1")
                .andExpect(content().json("{\"availableCells\": [{\"id\":" + cell.getId() + ", \"status\": " +
                        "\"NOT_ACTIVE\", \"type\": \"BUFFER\", " +
                        "\"subType\": \"BUFFER_XDOC\"}]}"));

        List<PartnerCellDto> cells = StreamEx.of(cellQueryService.getPartnerCells(sortingCenter))
                .filterBy(PartnerCellDto::getType, CellType.BUFFER)
                .filterBy(PartnerCellDto::getSubType, CellSubType.BUFFER_XDOC)
                .toList();

        assertThat(cells).size().isEqualTo(1);
        assertThat(cells).anyMatch(partnerCellDto -> partnerCellDto.getWarehouseYandexId() == null);
    }

    /**
     * Проверяет, что будет создана буферная XDOC ячейка, если подходящей буферной ячейки нет
     * Этот функционал не нужен, так как буферная ячейка всегда создается с помощью
     * {@link ru.yandex.market.sc.core.domain.inbound.InboundManager#sortableAccepted}
     */
    @Disabled
    @Test
    @SneakyThrows
    void getBufferCellCreateBufferCellIfDestinationCellCreated() {
        flow.createInbound("IN-1")
                .linkPallets("XDOC-p1")
                .fixInbound();

        StreamEx.of(cellQueryService.getPartnerCells(sortingCenter))
                .forEach(partnerCellDto -> cellCommandService.deleteCell(sortingCenter, partnerCellDto.getId()));
        testFactory.storedCell(sortingCenter, "DIF_DIR", CellType.BUFFER, CellSubType.BUFFER_XDOC, "123");

        caller.getOrder("XDOC-p1")
                .andExpect(content().json("{\"availableCells\": [{\"status\": " +
                        "\"NOT_ACTIVE\", \"type\": \"BUFFER\", " +
                        "\"subType\": \"BUFFER_XDOC\"}]}"));

        List<PartnerCellDto> cells = StreamEx.of(cellQueryService.getPartnerCells(sortingCenter))
                .filterBy(PartnerCellDto::getType, CellType.BUFFER)
                .filterBy(PartnerCellDto::getSubType, CellSubType.BUFFER_XDOC)
                .toList();

        assertThat(cells).size().isEqualTo(2);
        assertThat(cells).anyMatch(partnerCellDto -> partnerCellDto.getWarehouseYandexId() == null);
    }

    @Test
    @SneakyThrows
    void getSortableBoxNoApplicableBasket() {
        Cell cell = testFactory.storedCell(sortingCenter, "1111", CellType.BUFFER, CellSubType.BUFFER_XDOC,
                TestFactory.WAREHOUSE_YANDEX_ID);
        flow.createInbound("IN-1")
                .linkPallets("XDOC-p1")
                .fixInbound();

        Sortable sortable = sortableQueryService.find(sortingCenter, "XDOC-p1").orElseThrow();

        caller.getOrder(sortable.getRequiredBarcodeOrThrow())
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(expectedCreated(sortable, cell.getId())));

    }

    @Test
    @SneakyThrows
    void getSortableBoxHasApplicableBasket() {
        Cell cell = testFactory.storedCell(sortingCenter, "test_cell", CellType.BUFFER, CellSubType.BUFFER_XDOC,
                TestFactory.WAREHOUSE_YANDEX_ID);
        Cell badCell = testFactory.storedCell(sortingCenter, "test_cell2", CellType.BUFFER, CellSubType.BUFFER_XDOC,
                "123");
        flow.createInbound("IN-1")
                .linkBoxes("XDOC-p1")
                .fixInbound()
                .createBasket(cell);
        flow.createBasket(cell);
        flow.createBasket(badCell);

        Sortable sortable = sortableQueryService.find(sortingCenter, "XDOC-p1").orElseThrow();

        caller.getOrder(sortable.getRequiredBarcodeOrThrow())
                .andExpect(status().isOk())
                .andExpect(content().json(String.format("""
                        {
                          "id": %1$s,
                          "externalId": "XDOC-p1",
                          "status": "KEEP",
                          "warehouse": {
                            "id": 0,
                            "name": ""
                          },
                          "availableCells": [],
                          "availableLots": [
                            {
                              "lotId": 1,
                              "lotType": "XDOC_BASKET",
                              "parentCellId": %2$s,
                              "externalId": "XDOC-100000"
                            },
                            {
                              "lotId": 2,
                              "lotType": "XDOC_BASKET",
                              "parentCellId": %2$s,
                              "externalId": "XDOC-100001"
                            }
                          ]
                        ,  "lotSortAvailable": true}""", sortable.getId(), cell.getId())));
    }

    @DisplayName("Нельзя отсортировать коробку, которая уже была размещена в лоте(Basket), лот запакован")
    @Test
    void forbiddenToSortBoxThatAlreadyPlacedInTheBasketAndBasketIsPacked() {
        Warehouse warehouse = testFactory.storedWarehouse("101010987");
        Cell cell = testFactory.storedCell(
                sortingCenter,
                "samara-keep",
                CellType.BUFFER,
                CellSubType.BUFFER_XDOC,
                warehouse.getYandexId()
        );

        flow.inboundBuilder("in-1")
                .nextLogisticPoint(warehouse.getYandexId())
                .build()
                .linkBoxes("XDOC-1")
                .fixInbound();

        var box = sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow();
        var lotDto = sortableTestFactory.createEmptyLot(sortingCenter, cell);
        var basket = sortableLotService.findByLotIdOrThrow(lotDto.getId());

        caller.sort(
                box.getRequiredBarcodeOrThrow(),
                basket.getSortable()
        ).andExpect(status().isOk());

        var boxAfterSortingToLot = sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow();
        assertThat(boxAfterSortingToLot)
                .extracting(Sortable::getParent).isEqualTo(basket.getSortable());

        caller.preship(basket.getSortableId(), basket.getType(), SortableAPIAction.READY_FOR_PACKING)
                .andExpect(status().isOk());

        var lotAfterPacking = lotRepository.findByIdOrThrow(lotDto.getId());
        assertThat(lotAfterPacking)
                .extracting(Lot::getStatus).isEqualTo(LotStatus.PACKED);

        // скан ШК коробки (get запрос)
        caller.getOrder("XDOC-1")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                            {
                              "error": "CANT_SORT_PACKED_XDOC_BOX"
                            }"""));

        var lotDto2 = sortableTestFactory.createEmptyLot(sortingCenter, cell);
        var basket2 = sortableLotService.findByLotIdOrThrow(lotDto2.getId());

        // put запрос сортировки в лот
        caller.sort(
                box.getRequiredBarcodeOrThrow(),
                basket2.getSortable()
        ).andExpect(status().isBadRequest())
                .andExpect(content().json("""
                            {
                              "error": "CANT_SORT_PACKED_XDOC_BOX"
                            }"""));
    }

    @DisplayName("Можно отсортировать коробку, которая уже была размещена в лоте(Basket), лот НЕ запакован")
    @Test
    void itsAllowedToSortBoxThatAlreadyPlacedInTheBasket() {
        Warehouse warehouse = testFactory.storedWarehouse("101010987");
        Cell cell = testFactory.storedCell(
                sortingCenter,
                "samara-keep",
                CellType.BUFFER,
                CellSubType.BUFFER_XDOC,
                warehouse.getYandexId()
        );

        flow.inboundBuilder("in-1")
                .nextLogisticPoint(warehouse.getYandexId())
                .build()
                .linkBoxes("XDOC-1");

        var box = sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow();

        var lotDto = sortableTestFactory.createEmptyLot(sortingCenter, cell);
        var basket = sortableLotService.findByLotIdOrThrow(lotDto.getId());

        // сортируем коробку в первый лот
        caller.sort(
                box.getRequiredBarcodeOrThrow(),
                basket.getSortable()
        ).andExpect(status().isOk());

        var boxAfterSortingToLot = sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow();
        assertThat(boxAfterSortingToLot)
                .extracting(Sortable::getParent).isEqualTo(basket.getSortable());

        // скан ШК коробки (get запрос)
        caller.getOrder("XDOC-1")
                .andExpect(status().isOk());

        var lotDto2 = sortableTestFactory.createEmptyLot(sortingCenter, cell);
        var basket2 = sortableLotService.findByLotIdOrThrow(lotDto2.getId());

        // сортируем во второй лот
        caller.sort(
                box.getRequiredBarcodeOrThrow(),
                basket2.getSortable()
        ).andExpect(status().isOk());

        var boxAfterSortingToSecondLot = sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow();
        assertThat(boxAfterSortingToSecondLot)
                .extracting(Sortable::getParent).isEqualTo(basket2.getSortable());

    }

    @DisplayName("""
            BUGFIX: при попытке сортировки коробов в лоты появляются дубли номеров возможных лотов.
            Причина в том, что предлагались лоты по трем условиям и складывались в один список,
            один и тот же лот мог удовлетворять нескольким условиям""")
    @Test
    void duplicatesInListOfCellsAvailableForSort() {
        Warehouse warehouse = testFactory.storedWarehouse("101010987");
        Cell cell = testFactory.storedCell(
                sortingCenter,
                "samara-keep",
                CellType.BUFFER,
                CellSubType.BUFFER_XDOC,
                warehouse.getYandexId()
        );

        flow.inboundBuilder("in-1")
                .nextLogisticPoint(warehouse.getYandexId())
                .build()
                .linkBoxes("XDOC-1");

        var box = sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow();

        var lotDto = sortableTestFactory.createEmptyLot(sortingCenter, cell);
        var basket = sortableLotService.findByLotIdOrThrow(lotDto.getId());

        // сортируем коробку в пустой лот
        caller.sort(
                box.getRequiredBarcodeOrThrow(),
                basket.getSortable()
        ).andExpect(status().isOk());

        // коробка размещена в лоте, теперь этот лот удовлетворяет двум условиям, то же направление и та же поставка
        var boxAfterSortingToLot = sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow();
        assertThat(boxAfterSortingToLot)
                .extracting(Sortable::getParent).isEqualTo(basket.getSortable());

        var apiOrderDto = caller.getOrder("XDOC-1")
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);

        assertThat(
                StreamEx.of(apiOrderDto.getAvailableLots())
                        .map(ApiCellLotDto::getLotId)
                        .toList()
        )
                .isEqualTo(List.of(basket.getLotId()));
    }

    @Nested
    class FixOnlyFullySortedInbounds {

        Warehouse samaraWH;
        Cell samaraCell;

        @BeforeEach
        void init() {
            testFactory.setSortingCenterProperty(
                    sortingCenter,
                    SortingCenterPropertiesKey.ENABLE_FIX_ONLY_FULLY_SORTED_XDOC_INBOUNDS,
                    true
            );
            samaraWH = testFactory.storedWarehouse("101010987");
            samaraCell = testFactory.storedCell(
                    sortingCenter,
                    "samara-keep",
                    CellType.BUFFER,
                    CellSubType.BUFFER_XDOC,
                    samaraWH.getYandexId()
            );
        }

        @DisplayName("При попытке сканить коробку, если подходящих лотов не существует," +
                " появится сообщение, что нужно создать лоты")
        @Test
        void applicableLotsDoesNotExist() {
            flow.inboundBuilder("in-1")
                    .nextLogisticPoint(samaraWH.getYandexId())
                    .build()
                    .linkBoxes("XDOC-1");
            caller.getOrder("XDOC-1")
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json("""
                            {
                              "error": "THERE_ARE_NO_APPLICABLE_LOTS",
                              "message": "Нет подходящих лотов, нужно создать новый"
                            }"""));
        }

        @DisplayName("Нельзя отсортировать коробку в ячейку")
        @Test
        void itsNotAllowedToSortBoxToCell() {
            flow.inboundBuilder("in-1")
                    .nextLogisticPoint(samaraWH.getYandexId())
                    .build()
                    .linkBoxes("XDOC-1");

            var box = sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow();

            // переход на этот экран на фронте не должен произойти,
            // но если все же произойдет, то при пике на ячейку будет ошибка
            caller.accept(box.getRequiredBarcodeOrThrow(), null)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json("""
                            {
                              "error": "THERE_ARE_NO_APPLICABLE_LOTS",
                              "message": "Нет подходящих лотов, нужно создать новый"
                            }"""));

            var sortedBox = sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow();
            assertThat(sortedBox.getParent())
                    .isNull();
        }

        @DisplayName("Сортировка в лот работает")
        @Test
        void sortToLotSuccessful() {
            flow.inboundBuilder("in-1")
                    .nextLogisticPoint(samaraWH.getYandexId())
                    .build()
                    .linkBoxes("XDOC-1");

            var box = sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow();
            var lotDto = sortableTestFactory.createEmptyLot(sortingCenter, samaraCell);
            var basket = sortableLotService.findByLotIdOrThrow(lotDto.getId());

            caller.getOrder("XDOC-1")
                    .andExpect(status().isOk())
                    .andExpect(content().json(String.format("""
                            {
                              "id": %1$s,
                              "externalId": "XDOC-1",
                              "status": "KEEP",
                              "warehouse": {
                                "id": 0,
                                "name": ""
                              },
                              "availableCells": [],
                              "availableLots": [
                                {
                                  "lotId": %2$s,
                                  "lotType": "XDOC_BASKET",
                                  "parentCellId": %3$s,
                                  "externalId": "XDOC-100000"
                                }
                              ],
                              "lotSortAvailable": true
                            }""", box.getId(), basket.getLotId(), samaraCell.getId())));

            caller.sort(
                    box.getRequiredBarcodeOrThrow(),
                    basket.getSortable()
            ).andExpect(status().isOk());

            var sortedBox = sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow();
            assertThat(sortedBox.getParent())
                    .isEqualTo(basket.getSortable());
        }

        @DisplayName("Перенос заказов (плейсов) из коробки (орфан лота) в палету (лот на отгрузку)")
        @Test
        void transferFromLotToLot() {
            var courierWithDs = testFactory.magistralCourier();
            var cell = testFactory.storedMagistralCell(
                    sortingCenter,
                    "cell1",
                    CellSubType.DEFAULT,
                    courierWithDs.courier().getId()
            );

            var lotFromExternalId = lotCommandService.createOrphanLots(sortingCenter, 1).get(0);

            var lotFrom = sortableLotService.findByExternalIdAndSortingCenterStrict(lotFromExternalId, sortingCenter);

            var lotTo = sortableTestFactory.createEmptyLot(sortingCenter, cell);

            var placeExternalIds = List.of("1", "2", "3");

            // создаём заказ
            var order = testFactory.createForToday(
                    order(sortingCenter, UUID.randomUUID().toString())
                            .places(placeExternalIds)
                            .dsType(DeliveryServiceType.TRANSIT)
                            .deliveryService(courierWithDs.deliveryService())
                            .build())
                .acceptPlaces()
                .sortPlaces()
                .get();

            // помещаем плейсы заказа в орфан лот (нашу коробочку на гравитационном стеллаже)
            for (var placeExternalId: placeExternalIds) {
                sortPlaceToOrphanLot(order.getExternalId(), placeExternalId, lotFromExternalId, cell.getId());
            }

            // переносим плейсы из орфан лота в палету для отгрузки
            caller.transferFromLotToLot(
                new TransferFromLotToLotRequestDto(lotFromExternalId, lotTo.getExternalId())
            ).andExpect(status().isOk());

            // убеждаемся, что орфан лот сбросил свой статус и ячейку и готов к переиспользованию
            assertThat(lotFrom.getLotStatusOrNull()).isEqualTo(LotStatus.CREATED);
            assertThat(lotFrom.getSortable().getCell()).isNull();

            transactionTemplate.execute(__ -> { //транзакция для того, чтобы getHistory() работал
                var places = placeRepository.findPlacesFromLots(List.of(
                        lotFrom.getLotId(),
                        lotTo.getId()));

                // убеждаемся, что все плейсы переехали в новый лот
                assertThat(places)
                        .allMatch(p ->
                                p.getLotId().isPresent() &&
                                        p.getLotId().get().equals(lotTo.getId()));

                // убеждаемся, что история плейсов записалась
                assertThat(places)
                        .allMatch(p ->
                                p.getHistory().stream().anyMatch(ph ->
                                        ph.getMutableState().getLot() != null &&
                                                ph.getMutableState().getLot().getId().equals(lotTo.getId())
                                )
                        );

                return null;
            });

            // пробуем отгрузить лот

            lotCommandService.prepareToShipLot(lotTo.getId(), SortableAPIAction.READY_FOR_SHIPMENT, null);

            var route = testFactory.findOutgoingRoute(order).orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

            order = testFactory.getOrder(order.getId());
            assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);

            var places = testFactory.orderPlaces(order);
            assertThat(places).allMatch(p -> p.getStatus() == PlaceStatus.SHIPPED)
                    .allMatch(p -> Objects.equals(p.getMutableState().getStageId(),
                            StageLoader.getBySortableStatus(SortableStatus.SHIPPED_DIRECT).getId()));

            var lot = testFactory.getLot(lotTo.getId());
            assertThat(lot.getOptLotStatus()).isEmpty();
            assertThat(lot.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);

            // пробуем переиспользовать орфан лот
            var newPlaceExternalId = "4";
            var newOrder = testFactory.createForToday(
                            order(sortingCenter, UUID.randomUUID().toString())
                                    .places(newPlaceExternalId)
                                    .dsType(DeliveryServiceType.TRANSIT)
                                    .deliveryService(courierWithDs.deliveryService())
                                    .build())
                    .acceptPlaces()
                    .sortPlaces()
                    .get();

            sortPlaceToOrphanLot(newOrder.getExternalId(), newPlaceExternalId, lotFromExternalId, cell.getId());
        }

        @Test
        @SneakyThrows
        @DisplayName("Проверка статуса заказа при отмененной коробке")
        void successApiOrderStatusIfPlaceStatusDeleted() {
            String externalId = "DELETED";
            var user = testFactory.storedUser(sortingCenter, 456L);
            setupDeleted(externalId, user);

            var apiOrderDto = scanService.acceptReturnedOrder(new AcceptReturnedOrderRequestDto(externalId, externalId, null),
                    new ScContext(user));
            ApiOrderDto response = caller.getOrder(apiOrderDto.getExternalId())
                    .andExpect(status().isOk())
                    .getResponseAsClass(ApiOrderDto.class);
            assertThat(response.getStatus()).isEqualTo(ApiOrderStatus.WRONG_STATUS);
        }

        private void setupDeleted(String barcode, User user) {
            testFactory.storedFakeReturnDeliveryService();
            testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);
            var whFrom = testFactory.storedWarehouse("wh0");
            var whTo = testFactory.storedWarehouse("wh1");
            var cargoUnitId = "1aaaa123";
            var segmentUuid = "1123-12asdf-sadf-3213";
            var fromWarehouse = WarehouseDto.builder()
                    .yandexId(whFrom.getYandexId())
                    .build();
            var returnWarehouse = WarehouseDto.builder()
                    .yandexId(whTo.getYandexId())
                    .build();

            orderCommandService.createReturn(CreateReturnRequest.builder()
                            .sortingCenter(sortingCenter)
                            .orderBarcode(barcode)
                            .returnDate(LocalDate.now())
                            .returnWarehouse(returnWarehouse)
                            .fromWarehouse(fromWarehouse)
                            .segmentUuid(segmentUuid)
                            .cargoUnitId(cargoUnitId)
                            .timeIn(Instant.now())
                            .timeOut(Instant.now())
                            .orderReturnType(OrderReturnType.CLIENT_RETURN)
                            .assessedCost(new BigDecimal(10_000))
                            .build()
                    , user);
            orderCommandService.deleteSegmentUUI(DeletedSegmentRequest.builder()
                    .segmentUuid(segmentUuid)
                    .cargoUnitId(cargoUnitId)
                    .build());
        }

        /**
         * Сортируем в орфан лот с привязкой к ячейке
         */
        @SneakyThrows
        private void sortPlaceToOrphanLot(
                String orderExternalId,
                String placeExternalId,
                String lotExternalId,
                long cellId) {
            TestControllerCaller.createCaller(mockMvc)
                    .sortableBetaSort(new SortableSortRequestDto(
                            orderExternalId,
                            placeExternalId,
                            lotExternalId,
                            String.valueOf(cellId),
                            false))
                    .andExpect(status().isOk());
        }
    }
}
