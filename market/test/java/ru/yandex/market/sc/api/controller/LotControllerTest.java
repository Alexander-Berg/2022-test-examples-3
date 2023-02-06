package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.api.util.ScApiControllerCaller;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.lot.model.ApiLotSizeDto;
import ru.yandex.market.sc.core.domain.lot.model.ApiSortableDto;
import ru.yandex.market.sc.core.domain.lot.repository.LotRepository;
import ru.yandex.market.sc.core.domain.lot.repository.LotSize;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.sortable.SortableCommandService;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DirectFlowType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.stage.Stages.AWAITING_DIRECT;
import static ru.yandex.market.sc.core.domain.stage.Stages.FINAL_ACCEPT_DIRECT;
import static ru.yandex.market.sc.core.domain.stage.Stages.FIRST_ACCEPT_DIRECT;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@ScApiControllerTest
public class LotControllerTest {

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
    LotRepository lotRepository;
    @Autowired
    PlaceRepository placeRepository;
    @Autowired
    XDocFlow flow;
    @Autowired
    ScApiControllerCaller caller;
    @Autowired
    JdbcTemplate jdbcTemplate;

    SortingCenter sortingCenter;
    User user;

    private final String RESULT_WITH_ACTION = """
            {
              "id": %s,
              "type": "%s",
              "externalId": %s,
              "name": "XDOC-p1",
              "status": "%s",
              "sortableStatus": "%s",
              "actions": [%s],
              "labelCanBePrinted": %b,
              "transferable": %b,
              "size": "%s",
              "shape": {"id": "%s", "name": "%s", "description": "%s"}
            }
            """;

    private final String RESULT_WITHOUT_ACTIONS = """
            {
              "id": %s,
              "type": "%s",
              "externalId": %s,
              "name": "XDOC-p1",
              "status": "%s",
              "sortableStatus": "%s",
              "actions": [],
              "labelCanBePrinted": %b,
              "transferable": %b,
              "size": "%s",
              "shape": {"id": "%s", "name": "%s", "description": "%s"}
            }
            """;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, UID);
        testFactory.setupMockClock(clock);
    }

    @Test
    void getLotByIdBackwardCompatibility() throws Exception {
        Cell parentCell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/lots/external/" + lot.getLotId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json(lotJson(lot, Set.of()), true));
    }

    @Test
    void getLotByExternalId() throws Exception {
        Cell parentCell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/lots/external/" + lot.getBarcode())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json(lotJson(lot, Set.of()), true));
    }

    @Test
    void getLotByExternalIdNotReadyToShip() throws Exception {
        Cell parentCell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        SortableLot lot = testFactory.storedLot(sortingCenter,
                SortableType.PALLET,
                parentCell,
                LotStatus.PROCESSING,
                false
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/lots/external/" + lot.getBarcode())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json(lotJson(lot, Set.of(SortableAPIAction.READY_FOR_SHIPMENT)), true));
    }

    @Test
    void getLotByExternalIdReadyForShip() throws Exception {
        Cell parentCell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        SortableLot lot = testFactory.storedLot(sortingCenter,
                SortableType.PALLET,
                parentCell,
                LotStatus.READY,
                false
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/lots/external/" + lot.getBarcode())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json(lotJson(lot, Set.of(SortableAPIAction.NOT_READY_FOR_SHIPMENT)), true));
    }

    @Test
    void getXdocPalletByExternalId() throws Exception {
        Sortable sortable = prepareInboundWithXdocSortable(SortableType.XDOC_PALLET, SortableStatus.PREPARED_DIRECT);

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/lots/external/" + sortable.getRequiredBarcodeOrThrow())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json(
                        expectedResult(
                                RESULT_WITH_ACTION, sortable.getId(), sortable.getType(),
                                sortable.getRequiredBarcodeOrThrow(),
                                sortable.getStatus(), LotStatus.READY,
                                Set.of(SortableAPIAction.NOT_READY_FOR_SHIPMENT)),
                        false)
                );
    }

    @Test
    void getBoxByIdErr() throws Exception {
        Sortable sortable = sortableTestFactory
                .storeSimpleSortable(sortingCenter, SortableType.BOX, DirectFlowType.TRANSIT, "p1")
                .dummyPrepareDirect()
                .get();
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/lots/" + sortable.getId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().is4xxClientError());

    }

    @Test
    void getPalletByIdBadStatusErr() throws Exception {
        Sortable sortable = sortableTestFactory
                .storeSimpleSortable(sortingCenter, SortableType.PALLET, DirectFlowType.TRANSIT, "SC_LOT_p1")
                .get();
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/lots/" + sortable.getId())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().is4xxClientError());
    }

    private String lotJson(SortableLot lot, Set<SortableAPIAction> actions) {
        var parentCell = lot.getParentCell();
        Set<String> actionsString = actions.stream().map(SortableAPIAction::name).collect(Collectors.toSet());
        return "{\"id\": " + lot.getLotId() +
                ",\"type\":\"" + SortableType.PALLET + "\"" +
                ",\"externalId\":\"" + lot.getBarcode() + "\"" +
                ",\"name\":\"" + lot.getNameForApi() + "\"" +
                ",\"status\":\"" + lot.getOptLotStatus().orElse(LotStatus.SHIPPED) + "\"" +
                ",\"sortableStatus\":\"" + lot.getStatus() + "\"" +
                ",\"category\":\"" +  Cell.getSubTypeOrNull(parentCell) + "\"" +
                ",\"actions\":[" + String.join(",", actionsString) + "]" +
                ",\"labelCanBePrinted\":" + (lot.getLotStatusOrNull() == LotStatus.READY) +
                ",\"transferable\": false}";
    }

    @SneakyThrows
    @Test
    void prepareToShipLotBackwardCompatibility() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");

        Cell parentCell = testFactory.storedCell(sortingCenter, "cell-1", CellType.RETURN);
        SortableLot lot = testFactory.storedLot(sortingCenter, parentCell, LotStatus.PROCESSING);
        ScOrder scOrder = testFactory.createOrder(sortingCenter).cancel().accept().get();
        testFactory.sortOrderToLot(scOrder, lot, user);

        ApiSortableDto result = new ApiSortableDto(lot.getLotId(), SortableType.PALLET, lot.getBarcode(),
                lot.getNameForApi(), LotStatus.READY, null,
                CellSubType.DEFAULT, Set.of(SortableAPIAction.NOT_READY_FOR_SHIPMENT), true,
                lot.getLot().getSize());
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/{id}/preship", lot.getLotId())
                                .header("Authorization", "OAuth uid-" + UID)
                                .param("action", SortableAPIAction.READY_FOR_SHIPMENT.name())
                )
                .andExpect(status().isOk())
                .andExpect(content().json(JacksonUtil.toString(result)));
    }

    @SneakyThrows
    @Test
    void rejectPrepareToShipXdocPallet() {
        Sortable sortable = prepareInboundWithXdocSortable(SortableType.XDOC_PALLET, SortableStatus.PREPARED_DIRECT);

        ApiSortableDto result = new ApiSortableDto(sortable.getId(), SortableType.XDOC_PALLET,
                sortable.getRequiredBarcodeOrThrow(),
                sortable.getRequiredBarcodeOrThrow(), LotStatus.PROCESSING, SortableStatus.SORTED_DIRECT,
                null, Set.of(SortableAPIAction.READY_FOR_SHIPMENT),
                false, LotSize.NORMAL
        );
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/{id}/preship", sortable.getId())
                                .header("Authorization", "OAuth uid-" + UID)
                                .param("type", SortableType.XDOC_PALLET.name())
                                .param("action", SortableAPIAction.NOT_READY_FOR_SHIPMENT.name())
                )
                .andExpect(status().isOk())
                .andExpect(content().json(JacksonUtil.toString(result)));
    }

    @SneakyThrows
    @Test
    void rejectPrepareToShipXdocPalletError() {
        Sortable sortable = prepareInboundWithXdocSortable(SortableType.XDOC_PALLET, SortableStatus.PREPARED_DIRECT);

        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/{id}/preship", sortable.getId())
                                .header("Authorization", "OAuth uid-" + UID)
                                .param("type", SortableType.XDOC_PALLET.name())
                                .param("action", SortableAPIAction.READY_FOR_SHIPMENT.name())
                )
                .andExpect(status().is4xxClientError());
    }

    @SneakyThrows
    @Test
    void prepareToShipXdocPallet() {
        Inbound inbound = createInbound();
        Sortable sortable = sortableTestFactory
                .storeSortable(sortingCenter, SortableType.XDOC_PALLET, DirectFlowType.TRANSIT, "XDOC-p1",
                        inbound, null)
                .dummySortDirect()
                .get();
        var result = new ApiSortableDto(sortable.getId(), SortableType.XDOC_PALLET,
                sortable.getRequiredBarcodeOrThrow(), sortable.getRequiredBarcodeOrThrow(), LotStatus.READY,
                SortableStatus.PREPARED_DIRECT, null, Set.of(SortableAPIAction.NOT_READY_FOR_SHIPMENT), true,
                LotSize.NORMAL);
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/{id}/preship", sortable.getId())
                                .header("Authorization", "OAuth uid-" + UID)
                                .param("type", SortableType.XDOC_PALLET.name())
                                .param("action", SortableAPIAction.READY_FOR_SHIPMENT.name())
                )
                .andExpect(status().isOk())
                .andExpect(content().json(JacksonUtil.toString(result)));
    }

    @SneakyThrows
    @Test
    void prepareToShipXdocBasket() {
        testFactory.storedWarehouse();
        var cell = flow.createBufferCellAndGet("cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        var lot = flow.createBasket(cell);
        flow.createInbound("in-1")
                .linkBoxes("XDOC-1")
                .fixInbound();
        Sortable box = sortableQueryService.find(flow.getSortingCenter(), "XDOC-1").orElseThrow();
        flow.sortBoxToLot(box, lot);
        caller.preship(lot.getSortableId(), SortableType.XDOC_BASKET, SortableAPIAction.READY_FOR_PACKING);
        Sortable basket = sortableQueryService.find(lot.getSortableId()).orElseThrow();
        flow.createOutbound("out-1")
                .buildRegistry(basket.getRequiredBarcodeOrThrow())
                .sortToAvailableCell(basket.getRequiredBarcodeOrThrow());
        caller.preship(lot.getSortableId(), SortableType.XDOC_BASKET, SortableAPIAction.READY_FOR_SHIPMENT);
    }

    @Test
    void getXdocBasketByExternalId() throws Exception {
        Sortable sortable = prepareInboundWithXdocSortable(SortableType.XDOC_BASKET, SortableStatus.SORTED_DIRECT);

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/lots/external/" + sortable.getRequiredBarcodeOrThrow())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json(
                        expectedResult(
                                RESULT_WITH_ACTION, sortable.getId(), sortable.getType(), sortable.getRequiredBarcodeOrThrow(),
                                sortable.getStatus(), LotStatus.PROCESSING,
                                Set.of(SortableAPIAction.READY_FOR_SHIPMENT)),
                        false)
                );
    }

    @Test
    void getXdocBasketProcessingByExternalId() throws Exception {
        String barcode = "XDOC-p1";

        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.XDOC_BASKET,
                testFactory.storedCell(sortingCenter, CellType.BUFFER, CellSubType.BUFFER_XDOC, null),
                LotStatus.PROCESSING, false, barcode);

        Sortable sortable = sortableTestFactory.storeSortable(
                        sortingCenter,
                        SortableType.XDOC_BASKET,
                        DirectFlowType.TRANSIT,
                        barcode,
                        null,
                        null
                )
                .dummyChangeSortableStatus(SortableStatus.KEEPED_DIRECT)
                .get();

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/lots/external/" + sortable.getRequiredBarcodeOrThrow())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json(
                        expectedResult(
                                RESULT_WITH_ACTION, sortable.getId(), SortableType.XDOC_BASKET, sortable.getRequiredBarcodeOrThrow(),
                                SortableStatus.KEEPED_DIRECT, LotStatus.PROCESSING,
                                Set.of(SortableAPIAction.READY_FOR_PACKING)),
                        true)
                );
    }

    @Test
    void preshipSetPackedStatus() throws Exception {
        String barcode = "XDOC-p1";

        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.XDOC_BASKET,
                testFactory.storedCell(sortingCenter, CellType.BUFFER, CellSubType.BUFFER_XDOC, null),
                LotStatus.PROCESSING, false, barcode);

        Sortable sortable = sortableTestFactory.storeSortable(
                        sortingCenter,
                        SortableType.XDOC_BASKET,
                        DirectFlowType.TRANSIT,
                        barcode,
                        null,
                        null
                )
                .dummyChangeSortableStatus(SortableStatus.KEEPED_DIRECT)
                .get();

        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/lots/{id}/preship", sortable.getId())
                                .header("Authorization", "OAuth uid-" + UID)
                                .param("type", SortableType.XDOC_BASKET.name())
                                .param("action", SortableAPIAction.READY_FOR_PACKING.name())
                )
                .andExpect(status().isOk())
                .andExpect(content().json(
                        expectedResult(
                                RESULT_WITHOUT_ACTIONS, sortable.getId(), SortableType.XDOC_BASKET,
                                sortable.getRequiredBarcodeOrThrow(), SortableStatus.KEEPED_DIRECT,
                                LotStatus.PACKED, null),
                        true)
                );
    }

    @Test
    void packLotWithUnfixedInbounds() {
        testFactory.storedWarehouse();
        var cell = flow.createBufferCellAndGet("cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        var lot = flow.createBasket(cell);
        flow.createInbound("in-1")
                .linkBoxes("XDOC-1")
                .fixInbound()
                .createInbound("in-2")
                .linkBoxes("XDOC-2")
                .and();
        Sortable box1 = sortableQueryService.find(flow.getSortingCenter(), "XDOC-1").orElseThrow();
        Sortable box2 = sortableQueryService.find(flow.getSortingCenter(), "XDOC-2").orElseThrow();

        flow.sortBoxToLot(box1, lot);
        flow.sortBoxToLot(box2, lot);

        caller.preship(lot.getSortableId(), SortableType.XDOC_BASKET, SortableAPIAction.READY_FOR_PACKING)
                .andExpect(status().is4xxClientError());
    }

    @Test
    void packLotWithUsortedBoxes() {
        testFactory.storedWarehouse();
        var cell = flow.createBufferCellAndGet("cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        var lot = flow.createBasket(cell);
        flow.createInbound("in-1")
                .linkBoxes("XDOC-1")
                .linkBoxes("XDOC-2")
                .fixInbound();
        Sortable box1 = sortableQueryService.find(flow.getSortingCenter(), "XDOC-1").orElseThrow();
        Sortable box2 = sortableQueryService.find(flow.getSortingCenter(), "XDOC-2").orElseThrow();

        flow.sortBoxToLot(box1, lot);

        caller.preship(lot.getSortableId(), SortableType.XDOC_BASKET, SortableAPIAction.READY_FOR_PACKING)
                .andExpect(status().is4xxClientError());
    }

    @Test
    void inventoryLot() {
        testFactory.storedWarehouse();
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkBoxes("XDOC-1")
                .fixInbound()
                .inboundBuilder("in-2")
                .informationListBarcode("Зп-2")
                .build()
                .linkBoxes("XDOC-2")
                .fixInbound();
        Cell cell = flow.createBufferCellAndGet("cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        Sortable basket = flow.createBasketAndGet(cell);
        SortableLot lot = sortableLotService.findBySortableId(basket.getId()).orElseThrow();
        Sortable box1 = sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow();
        Sortable box2 = sortableQueryService.find(sortingCenter, "XDOC-2").orElseThrow();
        flow.sortBoxToLot(box1, lot);
        flow.sortBoxToLot(box2, lot);
        flow.packLot(basket.getRequiredBarcodeOrThrow());
        caller.getLotInfoXdoc(basket.getRequiredBarcodeOrThrow())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "externalId": "%s",
                            "inboundCount": 2,
                            "boxCount": 2,
                            "lotStatus": "PACKED",
                            "informationListCodes": [
                                "Зп-1",
                                "Зп-2"
                            ],
                            "name": "%s",
                            "cellName": "cell-1",
                            "destination": "%s"
                        }""".formatted(basket.getRequiredBarcodeOrThrow(), lot.getNameForApi(),
                        TestFactory.warehouse().getIncorporation()), true));
    }

    @Test
    void inventoryLotNotPacked() {
        testFactory.storedWarehouse();
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkBoxes("XDOC-1")
                .fixInbound()
                .inboundBuilder("in-2")
                .informationListBarcode("Зп-2")
                .build()
                .linkBoxes("XDOC-2")
                .fixInbound();
        Cell cell = flow.createBufferCellAndGet("cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        Sortable basket = flow.createBasketAndGet(cell);
        var lot = sortableLotService.findBySortableId(basket.getId()).orElseThrow();
        Sortable box1 = sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow();
        Sortable box2 = sortableQueryService.find(sortingCenter, "XDOC-2").orElseThrow();
        flow.sortBoxToLot(box1, lot);
        flow.sortBoxToLot(box2, lot);
        caller.getLotInfoXdoc(basket.getRequiredBarcodeOrThrow())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "externalId": "%s",
                            "inboundCount": 2,
                            "boxCount": 2,
                            "lotStatus": "PROCESSING",
                            "informationListCodes": [
                                "Зп-1",
                                "Зп-2"
                            ],
                            "name": "%s",
                            "cellName": "cell-1",
                            "destination": "%s"
                        }""".formatted(basket.getRequiredBarcodeOrThrow(), lot.getNameForApi(),
                        TestFactory.warehouse().getIncorporation()), true));
    }

    @Test
    void inventoryPallet() {
        Cell cell = flow.createBufferCellAndGet("cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        testFactory.storedWarehouse();
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkPallets("XDOC-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-1");

        Sortable pallet = sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow();

        caller.getLotInfoXdoc(pallet.getRequiredBarcodeOrThrow())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "externalId": "%s",
                            "inboundCount": 1,
                            "boxCount": 0,
                            "lotStatus": "READY",
                            "informationListCodes": [
                                "Зп-1"
                            ],
                            "name": "%s",
                            "cellName": "cell-1",
                            "destination": "%s"
                        }""".formatted(pallet.getRequiredBarcodeOrThrow(), pallet.getRequiredBarcodeOrThrow(),
                        TestFactory.warehouse().getIncorporation()), true));
    }

    @Test
    void inventoryBox() {
        var cell = flow.createBufferCellAndGet("cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        var lot = flow.createBasket(cell);
        testFactory.storedWarehouse();
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkBoxes("XDOC-1")
                .fixInbound();
        Sortable box = sortableQueryService.find(flow.getSortingCenter(), "XDOC-1").orElseThrow();
        flow.sortBoxToLot(box, lot);

        caller.getLotInfoXdoc(box.getRequiredBarcodeOrThrow())
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "externalId": "%s",
                            "inboundCount": 1,
                            "boxCount": 0,
                            "lotStatus": "READY",
                            "informationListCodes": [
                                "Зп-1"
                            ],
                            "name": "%s",
                            "cellName": "%s",
                            "destination": "%s"
                        }""".formatted(box.getRequiredBarcodeOrThrow(), box.getRequiredBarcodeOrThrow(), lot.getNameForApi(),
                        TestFactory.warehouse().getIncorporation()), true));
    }

    @ParameterizedTest
    @EnumSource(value = LotSize.class)
    void switchLotSize(LotSize lotSize) {
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER, CellSubType.DEFAULT);
        var lot = testFactory.storedLot(sortingCenter, cell, LotStatus.READY);
        caller.switchLotSize(lot.getLotId(), lotSize)
                .andExpect(jsonPath("$.size").value(lotSize.name()))
                .andExpect(jsonPath("$.shape.id").value(lotSize.name()))
                .andExpect(jsonPath("$.shape.name").value(lotSize.getReadableName()));
        caller.getLotForOutbound(lot.getBarcode())
                        .andExpect(jsonPath("$.size").value(lotSize.name()))
                .andExpect(jsonPath("$.shape.id").value(lotSize.name()))
                .andExpect(jsonPath("$.shape.name").value(lotSize.getReadableName()));
    }

    @Test
    void availableSizes() {
        caller.availableSizes()
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(LotSize.NORMAL.name()))
                .andExpect(jsonPath("$[1].id").value(LotSize.OVERSIZED.name()))
                .andExpect(jsonPath("$[2].id").value(LotSize.CARGOUNIT.name()));
    }

    private Sortable prepareInboundWithXdocSortable(SortableType sortableType, SortableStatus sortableStatus) {
        Inbound inbound = createInbound();
        return sortableTestFactory
                .storeSortable(sortingCenter, sortableType, DirectFlowType.TRANSIT, "XDOC-p1",
                        inbound, null)
                .dummyChangeSortableStatus(sortableStatus)
                .get();
    }

    private Inbound createInbound() {
        return testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundType(InboundType.XDOC_FINAL)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .nextLogisticPointId("11111111")
                .build());
    }

    private String expectedResult(String json, Long id, SortableType sortableType, String externalId,
                                  SortableStatus sortableStatus, LotStatus lotStatus,
                                  @Nullable Set<SortableAPIAction> actions) {
        return actions == null
            ? String.format(json, id, sortableType, externalId, lotStatus, sortableStatus,
                lotStatus == LotStatus.READY,
                sortableType == SortableType.ORPHAN_PALLET && lotStatus.isAllowedForTransferFrom(), LotSize.NORMAL,
                ApiLotSizeDto.mapLotSize(LotSize.NORMAL).getId(),
                ApiLotSizeDto.mapLotSize(LotSize.NORMAL).getName(),
                ApiLotSizeDto.mapLotSize(LotSize.NORMAL).getDescription()
                )
            : String.format(json, id, sortableType, externalId, lotStatus, sortableStatus,
                actions.stream()
                        .map(action -> "\'" + action.toString() + "\'")
                        .collect(Collectors.joining(",")),
                lotStatus == LotStatus.READY,
                sortableType == SortableType.ORPHAN_PALLET && lotStatus.isAllowedForTransferFrom(), LotSize.NORMAL,
                ApiLotSizeDto.mapLotSize(LotSize.NORMAL).getId(),
                ApiLotSizeDto.mapLotSize(LotSize.NORMAL).getName(),
                ApiLotSizeDto.mapLotSize(LotSize.NORMAL).getDescription()
        );
    }

    @Nested
    class AcceptLotWithPlaces {

        ScOrder order1;
        ScOrder order2;

        Place place1;
        Place place2;
        Place place3;

        SortingCenter sortingCenter;

        @BeforeEach
        void init() {
            sortingCenter = testFactory.storedSortingCenter();
            order1 = testFactory.create(order(sortingCenter).externalId("o-1")
                    .dsType(DeliveryServiceType.TRANSIT)
                    .places("p-1", "p-2")
                    .deliveryDate(LocalDate.now(clock))
                    .shipmentDate(LocalDate.now(clock))
                    .build()).get();

            order2 = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                    .sortingCenter(sortingCenter)
                    .deliveryDate(LocalDate.now(clock))
                    .shipmentDate(LocalDate.now(clock))
                    .externalId("o-2")
                    .places("p-3")
                    .build()
            ).get();

            place1 = testFactory.orderPlaces(order1.getId()).get(0);
            place2 = testFactory.orderPlaces(order1.getId()).get(1);
            place3 = testFactory.orderPlaces(order2.getId()).get(0);
        }

        @Test
        void acceptLotWithPlacesSuccess() {
            testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                    .inboundType(InboundType.DS_SC)
                    .sortingCenter(sortingCenter)
                    .inboundExternalId("in-1")
                    .registryMap(Map.of("registry_1", List.of(Pair.of("o-1", "p-1"), Pair.of("o-1", "p-2"),
                            Pair.of("o-2", "p-3"))))
                    .placeInPallets(Map.of("p-1", "SC_LOT_1", "p-2", "SC_LOT_2", "p-3", "SC_LOT_1"))
                    .palletToStamp(Map.of("SC_LOT_1", "stamp-1", "SC_LOT_2", "stamp-2"))
                    .fromDate(OffsetDateTime.now(clock))
                    .toDate(OffsetDateTime.now(clock))
                    .build()
            );

            var sortable = sortableQueryService.find(sortingCenter, "SC_LOT_1").orElseThrow();
            var lot = lotRepository.findBySortableId(sortable.getId()).orElseThrow();
            caller.acceptLot("""
                    {"stampId": "stamp-1"}
                    """).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(lot.getId()))
                    .andExpect(jsonPath("$.sortableStatus").value(SortableStatus.ARRIVED_DIRECT.name()));

            var expected1 = placeRepository.findByIdOrThrow(place1.getId());
            assertThat(expected1.getMutableState().getStageId()).isEqualTo(FIRST_ACCEPT_DIRECT.getId());
            var expected3 = placeRepository.findByIdOrThrow(place3.getId());
            assertThat(expected3.getMutableState().getStageId()).isEqualTo(FIRST_ACCEPT_DIRECT.getId());
            var expectedSortable = sortableQueryService.find(sortingCenter, "SC_LOT_1").orElseThrow();
            assertThat(expectedSortable.getStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);
            assertThat(expectedSortable.getMutableState().getStageId()).isEqualTo(FINAL_ACCEPT_DIRECT.getId());

        }

        @Test
        void acceptLotWithPlacesSuccessThenAcceptPlaces() {
            testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                    .inboundType(InboundType.DS_SC)
                    .sortingCenter(sortingCenter)
                    .inboundExternalId("in-1")
                    .registryMap(Map.of("registry_1", List.of(Pair.of("o-1", "p-1"), Pair.of("o-1", "p-2"),
                            Pair.of("o-2", "p-3"))))
                    .placeInPallets(Map.of("p-1", "SC_LOT_1", "p-2", "SC_LOT_2", "p-3", "SC_LOT_1"))
                    .palletToStamp(Map.of("SC_LOT_1", "stamp-1", "SC_LOT_2", "stamp-2"))
                    .fromDate(OffsetDateTime.now(clock))
                    .toDate(OffsetDateTime.now(clock))
                    .build()
            );

            var sortable = sortableQueryService.find(sortingCenter, "SC_LOT_1").orElseThrow();
            var lot = lotRepository.findBySortableId(sortable.getId()).orElseThrow();
            caller.acceptLot("""
                    {"stampId": "stamp-1"}
                    """).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(lot.getId()))
                    .andExpect(jsonPath("$.sortableStatus").value(SortableStatus.ARRIVED_DIRECT.name()));

            var expected1 = placeRepository.findByIdOrThrow(place1.getId());
            assertThat(expected1.getMutableState().getStageId()).isEqualTo(FIRST_ACCEPT_DIRECT.getId());
            var expected3 = placeRepository.findByIdOrThrow(place3.getId());
            assertThat(expected3.getMutableState().getStageId()).isEqualTo(FIRST_ACCEPT_DIRECT.getId());
            var expectedSortable = sortableQueryService.find(sortingCenter, "SC_LOT_1").orElseThrow();
            assertThat(expectedSortable.getStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);
            assertThat(expectedSortable.getMutableState().getStageId()).isEqualTo(FINAL_ACCEPT_DIRECT.getId());
            caller.accept(order1.getExternalId(), place1.getMainPartnerCode())
                    .andExpect(status().is2xxSuccessful());
            caller.accept(order1.getExternalId(), place2.getMainPartnerCode())
                    .andExpect(status().is2xxSuccessful());
            assertThat(placeRepository.findByIdOrThrow(place1.getId()).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            assertThat(placeRepository.findByIdOrThrow(place2.getId()).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
            var route = testFactory.findOutgoingCourierRoute(order1)
                    .orElseThrow();
            var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, order1));
        }

        @Test
        void acceptLotWithPlacesScanBarcodeFail() {
            testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                    .inboundType(InboundType.DS_SC)
                    .sortingCenter(sortingCenter)
                    .inboundExternalId("in-1")
                    .registryMap(Map.of("registry_1", List.of(Pair.of("o-1", "p-1"), Pair.of("o-1", "p-2"),
                            Pair.of("o-2", "p-3"))))
                    .placeInPallets(Map.of("p-1", "SC_LOT_1", "p-2", "SC_LOT_2", "p-3", "SC_LOT_1"))
                    .palletToStamp(Map.of("SC_LOT_1", "stamp-1", "SC_LOT_2", "stamp-2"))
                    .fromDate(OffsetDateTime.now(clock))
                    .toDate(OffsetDateTime.now(clock))
                    .build()
            );

            caller.acceptLot("""
                    {"stampId": "SC_LOT_1"}
                    """).andExpect(status().is4xxClientError());

            var expected1 = placeRepository.findByIdOrThrow(place1.getId());
            assertThat(expected1.getMutableState().getStageId()).isEqualTo(AWAITING_DIRECT.getId());
            var expected3 = placeRepository.findByIdOrThrow(place3.getId());
            assertThat(expected3.getMutableState().getStageId()).isEqualTo(AWAITING_DIRECT.getId());
            var expectedSortable = sortableQueryService.find(sortingCenter, "SC_LOT_1").orElseThrow();
            assertThat(expectedSortable.getStatus()).isEqualTo(SortableStatus.AWAITING_DIRECT);
            assertThat(expectedSortable.getMutableState().getStageId()).isEqualTo(AWAITING_DIRECT.getId());

        }
    }

}
