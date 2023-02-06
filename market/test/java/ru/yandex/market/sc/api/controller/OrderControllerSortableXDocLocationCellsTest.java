package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.api.util.ScApiControllerCaller;
import ru.yandex.market.sc.core.domain.cell.model.ApiCellDto;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.inbound.InboundCommandService;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderDto;
import ru.yandex.market.sc.core.domain.partner.lot.PartnerLotDto;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.domain.zone.repository.Zone;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Тест сортировки в ячейки подтипа BUFFER_XDOC_LOCATION
 * Ячейки должны размещаться в зоне
 * Ячейки предназначены для хранения одного грузоместа, что бы иметь информацию о его расположении на складе
 * возможные один из вариантов размещения:
 * 1. одна XDOC_PALLET
 * 2. один XDOC_BASKET
 * 3. множество XDOC_BOX
 */
@ScApiControllerTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderControllerSortableXDocLocationCellsTest {

    private final XDocFlow flow;
    private final ScApiControllerCaller caller;
    private final TestFactory testFactory;
    private final SortableTestFactory sortableTestFactory;
    private final SortableRepository sortableRepository;
    private final SortableQueryService sortableQueryService;
    private final Clock clock;
    private final InboundCommandService inboundCommandService;
    private final ObjectMapper objectMapper;
    private final SortableLotService sortableLotService;

    private SortingCenter sortingCenter;
    private Warehouse samaraWH;
    private Warehouse rostovWH;
    private Zone samaraZone;
    private Zone keepZone;


    @BeforeEach
    void init() {
        enableXDocLocation();
        sortingCenter = testFactory.storedSortingCenter();
        samaraWH = testFactory.storedWarehouse("samara-warehouse");
        rostovWH = testFactory.storedWarehouse("rostov-warehouse");
        samaraZone = testFactory.storedZone(sortingCenter, "samara-zone");
        keepZone = testFactory.storedZone(sortingCenter, "keepZone");
    }

    private void enableXDocLocation() {
        var scId = TestFactory.SC_ID;
        testFactory.setSortingCenterProperty(scId, SortingCenterPropertiesKey.XDOC_ENABLED, true);
        testFactory.setSortingCenterProperty(scId, SortingCenterPropertiesKey.SORT_ZONE_ENABLED, true);
        testFactory.setSortingCenterProperty(scId, SortingCenterPropertiesKey.ENABLE_BUFFER_XDOC_LOCATION, true);
    }

    @DisplayName("Есть ячейка в зоне, скан ШК ПАЛЕТЫ, ТСД попросит отнести палету в зону вместо ячейки")
    @Test
    void getOrdersShowsZoneNotCellForPallet() {
        testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-p-1").fixInbound();

        var orderDto = caller.getOrder("XDOC-p-1")
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);

        assertThat(orderDto.getAvailableCells()).hasSize(1);

        // can't use normal equals. DTO has equals by ID
        assertThat(orderDto.getAvailableCells().get(0))
                .matches(dto -> dto.getId() == samaraZone.getId())
                .matches(dto -> Objects.equals(dto.getNumber(), samaraZone.getName()))
                .matches(dto -> dto.getStatus() == CellStatus.ACTIVE)
                .matches(dto -> dto.getType() == CellType.BUFFER)
                .matches(dto -> dto.getSubType() == CellSubType.BUFFER_XDOC_LOCATION);
    }

    @DisplayName("""
            Множество зон одного направления,
            скан ШК ПАЛЕТЫ, ТСД попросит отнести в одну из зон вместо ячейки""")
    @Test
    void getOrdersShowsZonesNotCellForPallet() {
        var samaraZone2 = testFactory.storedZone(sortingCenter, "samara-zone-2");
        testFactory.storedCell(
                sortingCenter, "SAMARA-c1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);
        testFactory.storedCell(
                sortingCenter, "SAMARA-c2", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone2);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-p-1").fixInbound();

        var availableCells = caller.getOrder("XDOC-p-1")
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class)
                .getAvailableCells();

        assertThat(availableCells).hasSize(2)
                .anyMatch(dto ->
                        dto.getId() == samaraZone.getId() &&
                                Objects.equals(dto.getNumber(), samaraZone.getName()))
                .anyMatch(dto ->
                        dto.getId() == samaraZone2.getId() &&
                                Objects.equals(dto.getNumber(), samaraZone2.getName()));
    }

    @DisplayName("Есть ячейка в зоне, лоты XDOC_BASKET отсутствуют, скан ШК КОРОБКИ, ТСД попросит отнести палету в " +
            "зону вместо ячейки")
    @Test
    void getOrdersShowsZoneNotCellForBox() {
        testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkBoxes("XDOC-b-1").fixInbound();

        var orderDto = caller.getOrder("XDOC-b-1")
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);

        assertThat(orderDto.getAvailableCells()).hasSize(1);

        // can't use normal equals. DTO has equals by ID
        assertThat(orderDto.getAvailableCells().get(0))
                .matches(dto -> dto.getId() == samaraZone.getId())
                .matches(dto -> Objects.equals(dto.getNumber(), samaraZone.getName()))
                .matches(dto -> dto.getStatus() == CellStatus.ACTIVE)
                .matches(dto -> dto.getType() == CellType.BUFFER)
                .matches(dto -> dto.getSubType() == CellSubType.BUFFER_XDOC_LOCATION);
    }

    @DisplayName("Есть ячейка в зоне, лоты XDOC_BASKET есть в ячейке BUFFER_XDOC_LOCATION," +
            "скан ШК КОРОБКИ, ТСД попросит отнести коробку в лот")
    @Test
    void getOrdersShowsLot() {
        var cell = testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);
        sortableTestFactory.createEmptyLot(sortingCenter, cell);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkBoxes("XDOC-b-1").fixInbound();

        caller.getOrder("XDOC-b-1")
                .andExpect(status().isOk())
                .andExpect(content().json(
                        String.format("""
                                {
                                  "externalId": "XDOC-b-1",
                                  "availableCells": [],
                                  "availableLots": [
                                    {
                                      "lotName": "XDOC-100000 SAMARA-1",
                                      "lotType": "XDOC_BASKET",
                                      "parentCellId": %1$s,
                                      "externalId": "XDOC-100000"
                                    }
                                  ],
                                  "lotSortAvailable": true
                                }
                                """, cell.getId()), false
                ));
    }

    @DisplayName("ТСД не покажет пустой лот, лежащий в ячейке другого направления")
    @Test
    void getOrdersWillNotShowEmptyLotForDifferentDirection() {
        var cellSamara = testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);
        var samaraBasket = flow.createBasketAndGet(cellSamara);

        // существуют зона, ячейка и лот в ячейке другого направления и он пустой
        var zoneRostov = testFactory.storedZone(sortingCenter, "ROSTOV");
        var cellRostov = testFactory.storedCell(
                sortingCenter, "Rostov-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                rostovWH.getYandexId(), zoneRostov);
        flow.createBasketAndGet(cellRostov);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkBoxes("XDOC-b-1").fixInbound();

        // будет показан только один лот, того же направления, что и коробка
        caller.getOrder("XDOC-b-1")
                .andExpect(status().isOk())
                .andExpect(content().json(
                        String.format("""
                                        {
                                          "externalId": "XDOC-b-1",
                                          "availableCells": [],
                                          "availableLots": [
                                            {
                                              "lotName": "%1$s %2$s",
                                              "lotType": "XDOC_BASKET",
                                              "parentCellId": %3$s,
                                              "externalId": "%4$s"
                                            }
                                          ],
                                          "lotSortAvailable": true
                                        }
                                        """, samaraBasket.getRequiredBarcodeOrThrow(), cellSamara.getScNumber(),
                                cellSamara.getId(), samaraBasket.getRequiredBarcodeOrThrow()), false
                ));
    }

    @DisplayName("Можно отсортировать коробку в пустой лот, лежащий в ячейке этого направления")
    @Test
    void boxCanBeSortedToLot() {
        var cell = testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);
        var basket = flow.createBasketAndGet(cell);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkBoxes("XDOC-b-1").fixInbound();

        var box = sortableQueryService.find(sortingCenter, "XDOC-b-1").orElseThrow();
        var lot = sortableLotService.findBySortableId(basket.getId()).orElseThrow();

        caller.sort(
                box.getRequiredBarcodeOrThrow(),
                lot.getSortable()
        ).andExpect(status().isOk());

        var boxAfterSort = sortableQueryService.find(sortingCenter, "XDOC-b-1").orElseThrow();
        assertThat(boxAfterSort.getParent()).isEqualTo(basket);
    }

    @DisplayName("Нельзя отсортировать коробку в пустой лот, лежащий в ячейке другого направления")
    @Test
    void forbiddenToSortBoxIntoLotOfDifferentDirection() {
        var cellSamara = testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);
        flow.createBasketAndGet(cellSamara);

        // существуют зона, ячейка и лот в ячейке другого направления и он пустой
        var zoneRostov = testFactory.storedZone(sortingCenter, "ROSTOV");
        var cellRostov = testFactory.storedCell(
                sortingCenter, "Rostov-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                rostovWH.getYandexId(), zoneRostov);
        var basketRostov = flow.createBasketAndGet(cellRostov);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkBoxes("XDOC-b-1").fixInbound();

        var samaraBox = sortableQueryService.find(sortingCenter, "XDOC-b-1").orElseThrow();
        var rostovLot = sortableLotService.findBySortableId(basketRostov.getId()).orElseThrow();
        caller.sort(
                samaraBox.getRequiredBarcodeOrThrow(),
                rostovLot.getSortable()
        ).andExpect(status().isBadRequest());

        var boxAfterSort = sortableQueryService.find(sortingCenter, "XDOC-b-1").orElseThrow();
        assertThat(boxAfterSort.getParent()).isNull();
    }

    @DisplayName("""
            Зона хранения существует, но в ней нет ячеек.
            Скан ШК, ТСД попросит отнести палету дефолтную ячейку
            """)
    @Test
    void getOrdersButZoneWithoutCells() {
        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-p-1").fixInbound();

        var orderDto = caller.getOrder("XDOC-p-1")
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);

        assertThat(orderDto.getAvailableCells()).hasSize(1);
        assertThat(orderDto.getAvailableCells().get(0))
                .matches(dto -> dto.getNumber() == null)
                .matches(dto -> dto.getStatus() == CellStatus.NOT_ACTIVE)
                .matches(dto -> dto.getType() == CellType.BUFFER)
                .matches(dto -> dto.getSubType() == CellSubType.BUFFER_XDOC);
    }

    @DisplayName("""
            Зона хранения НЕ существует.
            Скан ШК, ТСД попросит отнести палету дефолтную ячейку
            """)
    @Test
    void getOrdersButZoneDoesNotExist() {
        flow.inboundBuilder("in-1").nextLogisticPoint(rostovWH.getYandexId()).build()
                .linkPallets("XDOC-p-1").fixInbound();

        var orderDto = caller.getOrder("XDOC-p-1")
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);

        assertThat(orderDto.getAvailableCells()).hasSize(1);
        assertThat(orderDto.getAvailableCells().get(0))
                .matches(dto -> dto.getNumber() == null)
                .matches(dto -> dto.getStatus() == CellStatus.NOT_ACTIVE)
                .matches(dto -> dto.getType() == CellType.BUFFER)
                .matches(dto -> dto.getSubType() == CellSubType.BUFFER_XDOC);
    }

    @DisplayName("""
            Есть ячейка в зоне, скан ШК ПАЛЕТЫ, ТСД попросит отнести палету в зону вместо ячейки.
            Кладовщик размещает в свободную ячейку внутри зоны.
            Палета будет размещена в ячейке. Палета перейдет в статус KEEPED_DIRECT.
            """)
    @Test
    void sortPalletToEmptyCell() {
        var cell = testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);

        // вторая ячейка, что бы адресное хранение не было заполнено полностью после сортировки в первую ячейку
        testFactory.storedCell(
                sortingCenter, "SAMARA-2", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-p-1").fixInbound();

        var orderDto = caller.getOrder("XDOC-p-1")
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);

        assertThat(orderDto).extracting(ApiOrderDto::getCell).isNull();

        // убедимся что в списке доступных только одна ячейка BUFFER_XDOC_LOCATION
        assertThat(orderDto.getAvailableCells())
                .hasSize(1);

        assertThat(orderDto.getAvailableCells().get(0))
                .extracting(ApiCellDto::getSubType).isEqualTo(CellSubType.BUFFER_XDOC_LOCATION);

        caller.sort(orderDto.getExternalId(), cell)
                .andExpect(status().isOk());

        var pallet = sortableQueryService.find(sortingCenter, "XDOC-p-1").orElseThrow();

        assertThat(pallet.getStatus())
                .isEqualTo(SortableStatus.KEEPED_DIRECT);

        assertThat(pallet.getCellIdOrNull()).isEqualTo(cell.getId());

        // повторный скан, после сортировки
        var orderDtoAfterSort = caller.getOrder("XDOC-p-1")
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);

        // после сортировки в списке доступных имеется ячейка, где лежит палета
        assertThat(orderDtoAfterSort.getAvailableCells())
                .hasSize(2);

        assertThat(orderDtoAfterSort.getAvailableCells().stream().map(ApiCellDto::getId).toList())
                .containsExactlyInAnyOrder(samaraZone.getId(), cell.getId());
    }

    @DisplayName("""
            Есть ячейка в зоне, скан ШК ПАЛЕТЫ, ТСД попросит отнести палету в зону вместо ячейки.
            Кладовщик пытается разместить в ячейку внутри зоны, в этой ячейке уже есть палета
            На ТСД будет ошибка
            """)
    @Test
    void sortPalletToCellButCellAlreadyHasPallet() {
        var cell = testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-p-1", "XDOC-p-2").fixInbound();

        sortToCell("XDOC-p-1", cell);

        var orderDtoAfterScanSecondPallet = caller.getOrder("XDOC-p-2")
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);

        // попытка отсортировать в ячейку хранения, в которой уже есть палета
        caller.sort(orderDtoAfterScanSecondPallet.getExternalId(), cell)
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                          "status": 400,
                          "error": "BAD_REQUEST"
                        }""", false));

        var firstPallet = sortableQueryService.find(sortingCenter, "XDOC-p-1").orElseThrow();
        assertThat(firstPallet.getCellIdOrNull()).isEqualTo(cell.getId());

        var secondPallet = sortableQueryService.find(sortingCenter, "XDOC-p-2").orElseThrow();
        assertThat(secondPallet.getCell()).isNull();
    }

    @DisplayName("""
            Есть ячейка в зоне, скан ШК КОРОБКИ, ТСД попросит отнести коробку в зону вместо ячейки.
            Кладовщик пытается разместить коробку в ячейку, в этой ячейке уже есть палета.
            На ТСД будет ошибка
            """)
    @Test
    void sortBoxToCellButCellAlreadyHasPallet() {
        var cell = testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-p-1")
                .fixInbound()
                .inboundBuilder("in-2").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkBoxes("XDOC-b-1")
                .fixInbound();

        sortToCell("XDOC-p-1", cell);

        var orderDtoAfterScanBox = caller.getOrder("XDOC-b-1")
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);

        // попытка отсортировать коробку в ячейку хранения, в которой уже есть палета
        caller.sort(orderDtoAfterScanBox.getExternalId(), cell)
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                          "status": 400,
                          "error": "BAD_REQUEST"
                        }""", false));

        var firstPallet = sortableQueryService.find(sortingCenter, "XDOC-p-1").orElseThrow();
        assertThat(firstPallet.getCellIdOrNull()).isEqualTo(cell.getId());

        var secondPallet = sortableQueryService.find(sortingCenter, "XDOC-b-1").orElseThrow();
        assertThat(secondPallet.getCell()).isNull();
    }

    @DisplayName("""
            Есть ячейка в зоне, скан ШК ПАЛЕТЫ, ТСД попросит отнести палету в зону вместо ячейки.
            Кладовщик пытается разместить палету в ячейку, в этой ячейке уже есть коробка.
            На ТСД будет ошибка
            """)
    @Test
    void sortPalletToCellButCellAlreadyHasBox() {
        var cell = testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-p-1")
                .fixInbound()
                .inboundBuilder("in-2").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkBoxes("XDOC-b-1")
                .fixInbound();

        sortToCell("XDOC-b-1", cell);

        var orderDtoAfterScanPallet = caller.getOrder("XDOC-p-1")
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);

        // попытка отсортировать палету в ячейку хранения, в которой уже есть коробка
        caller.sort(orderDtoAfterScanPallet.getExternalId(), cell)
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                        {
                          "status": 400,
                          "error": "BAD_REQUEST"
                        }""", false));

        var box = sortableQueryService.find(sortingCenter, "XDOC-b-1").orElseThrow();
        assertThat(box.getCellIdOrNull()).isEqualTo(cell.getId());

        var pallet = sortableQueryService.find(sortingCenter, "XDOC-p-1").orElseThrow();
        assertThat(pallet.getCell()).isNull();
    }

    @Disabled("Запрещена сортиров коробок в ячейки")
    @DisplayName("""
            Есть ячейка в зоне, скан ШК КОРОБКИ ТСД попросит отнести палету в зону вместо ячейки.
            Кладовщик пытается разместить коробку в ячейку, в этой ячейке уже есть коробки
            Коробка будет размещена в ячейке. Коробка перейдет в статус KEEPED_DIRECT.
            """)
    @Test
    void sortBoxToCellAndCellAlreadyHasBox() {
        var cell = testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkBoxes("XDOC-b-1", "XDOC-b-2")
                .fixInbound();

        sortToCell("XDOC-b-1", cell);
        // попытка отсортировать коробку в ячейку хранения, в которой уже есть коробки
        var orderDtoAfterScanCell = sortToCell("XDOC-b-2", cell);

        assertThat(orderDtoAfterScanCell.getCell())
                .matches(dto -> dto.getId() == cell.getId())
                .matches(dto -> Objects.equals(dto.getNumber(), cell.getScNumber()))
                .matches(dto -> dto.getStatus() == cell.getStatus())
                .matches(dto -> dto.getType() == cell.getType())
                .matches(dto -> dto.getSubType() == cell.getSubtype());

        var secondBox = sortableQueryService.find(sortingCenter, "XDOC-b-2").orElseThrow();

        assertThat(secondBox.getStatus())
                .isEqualTo(SortableStatus.KEEPED_DIRECT);
        assertThat(secondBox.getCellIdOrNull())
                .isEqualTo(cell.getId());
    }

    @DisplayName("Сортировка коробки/палеты из BUFFER_XDOC в BUFFER_XDOC_LOCATION")
    @Test
    void sortFromBufferToLocationAvailable() {
        var locationCell1 = testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);
        var locationCell2 = testFactory.storedCell(
                sortingCenter, "SAMARA-2", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);
        var bufferCell = testFactory.storedCell(
                sortingCenter, "SAMARA-BUFFER-1", CellType.BUFFER, CellSubType.BUFFER_XDOC,
                samaraWH.getYandexId(), null);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-p-1")
                .fixInbound()
                .inboundBuilder("in-2").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkBoxes("XDOC-b-1");

        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.ENABLE_BUFFER_XDOC_LOCATION,
                false);
        sortToCell("XDOC-p-1", bufferCell);
        sortToCell("XDOC-b-1", bufferCell);

        enableXDocLocation();
        sortToCell("XDOC-p-1", locationCell1);
        sortToCell("XDOC-b-1", locationCell2);

        var pallet = sortableQueryService.find(sortingCenter, "XDOC-p-1").orElseThrow();
        assertThat(pallet.getStatus())
                .isEqualTo(SortableStatus.KEEPED_DIRECT);
        assertThat(pallet.getCellIdOrNull())
                .isEqualTo(locationCell1.getId());

        var box = sortableQueryService.find(sortingCenter, "XDOC-b-1").orElseThrow();
        assertThat(box.getStatus())
                .isEqualTo(SortableStatus.KEEPED_DIRECT);
        assertThat(box.getCellIdOrNull())
                .isEqualTo(locationCell2.getId());
    }

    @DisplayName("Сортировка палеты из BUFFER_XDOC_LOCATION в ячейку отгрузки")
    @Test
    void sortFromLocationToCourierCell() {
        var locationCell = testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);
        var shipCell = testFactory.storedCell(sortingCenter, "SHIP_1", CellType.COURIER, CellSubType.SHIP_XDOC);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-p-1")
                .fixInbound();

        sortToCell("XDOC-p-1", locationCell);

        flow.outboundBuilder("out-1")
                .fromTime(Instant.now(clock).plus(2, ChronoUnit.HOURS))
                .toTime(Instant.now(clock).plus(2, ChronoUnit.HOURS))
                .logisticPointToExternalId(samaraWH.getYandexId())
                .courierExternalId("samaraCourier")
                .toRegistryBuilder()
                .externalId("out-reg-1")
                .addRegistryPallets("XDOC-p-1")
                .buildRegistry();

        sortToCell("XDOC-p-1", shipCell);

        var pallet = sortableQueryService.find(sortingCenter, "XDOC-p-1").orElseThrow();
        assertThat(pallet.getStatus())
                .isEqualTo(SortableStatus.SORTED_DIRECT);
        assertThat(pallet.getCellIdOrNull())
                .isEqualTo(shipCell.getId());
    }

    @DisplayName("После сортировки палеты из ячейки BUFFER_XDOC_LOCATION на отгрузку, " +
            "в ячейку BUFFER_XDOC_LOCATION можно будет разместить другую палету")
    @Test
    void bufferXDocLocationCellIsAvailableAfterOldPalletIsTakenFromIt() {
        var locationCell = testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);
        var shipCell = testFactory.storedCell(sortingCenter, "SHIP_1", CellType.COURIER, CellSubType.SHIP_XDOC);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-p-1")
                .fixInbound();

        sortToCell("XDOC-p-1", locationCell);

        flow.outboundBuilder("out-1")
                .fromTime(Instant.now(clock).plus(2, ChronoUnit.HOURS))
                .toTime(Instant.now(clock).plus(2, ChronoUnit.HOURS))
                .logisticPointToExternalId(samaraWH.getYandexId())
                .courierExternalId("samaraCourier")
                .toRegistryBuilder()
                .externalId("out-reg-1")
                .addRegistryPallets("XDOC-p-1")
                .buildRegistry();

        sortToCell("XDOC-p-1", shipCell);

        flow.inboundBuilder("in-2").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-p-2", "XDOC-p-3")
                .fixInbound();

        sortToCell("XDOC-p-2", locationCell);
    }

    @DisplayName("Если зона адресных ячеек по направлению заполнена, " +
            "то будет предложено сортировать в зону адресных ячеек keep, " +
            "если заполнены адресные ячейки keep, то попросится в зону BUFFER")
    @Test
    void zeroEmptyCellsInsideZoneWithDirection() {
        var samaraAddressCell = testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);

        var keepAddressCell = testFactory.storedCell(
                sortingCenter, "KEEP-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                null, keepZone);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-p-1", "XDOC-p-2");

        // заполняем единственную ячейку
        sortToCell("XDOC-p-1", samaraAddressCell);

        // зона самара заполнена, будет предложено сортировать в зону keep
        var orderDto = caller.getOrder("XDOC-p-2")
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);

        assertThat(orderDto.getAvailableCells()).hasSize(1);

        assertThat(orderDto.getAvailableCells().get(0))
                .extracting(
                        ApiCellDto::getId,
                        ApiCellDto::getNumber,
                        ApiCellDto::getStatus,
                        ApiCellDto::getType,
                        ApiCellDto::getSubType
                ).containsExactly(
                        keepZone.getId(),
                        keepZone.getName(),
                        CellStatus.ACTIVE,
                        CellType.BUFFER,
                        CellSubType.BUFFER_XDOC_LOCATION
                );

        // сортируем в ячейку keep
        caller.sort(orderDto.getExternalId(), keepAddressCell)
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);
    }

    @DisplayName("Если зона адресных ячеек по направлению заполнена, " +
            "то будет предложено сортировать в буферную ячейку, того же направления, если она есть")
    @Test
    void zeroEmptyCellsInsideZoneWithDirectionBufferCellExits() {
        var samaraAddressCell = testFactory.storedCell(
                sortingCenter, "SAMARA-1", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);

        var samaraKeepCell = testFactory.storedCell(
                sortingCenter, "SAMARA-KEEP-1", CellType.BUFFER, CellSubType.BUFFER_XDOC,
                samaraWH.getYandexId(), null);

        flow.inboundBuilder("in-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-p-1", "XDOC-p-2");

        // заполняем единственную ячейку
        sortToCell("XDOC-p-1", samaraAddressCell);

        // зона самара адресных ячеек заполнена, будет предложено сортировать в ячейку BUFFER_XDOC того же направления
        var orderDto = caller.getOrder("XDOC-p-2")
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);

        assertThat(orderDto.getAvailableCells()).hasSize(1);

        assertThat(orderDto.getAvailableCells().get(0))
                .extracting(
                        ApiCellDto::getId,
                        ApiCellDto::getNumber,
                        ApiCellDto::getStatus,
                        ApiCellDto::getType,
                        ApiCellDto::getSubType
                ).containsExactly(
                        samaraKeepCell.getId(),
                        samaraKeepCell.getScNumber(),
                        samaraKeepCell.getStatus(),
                        samaraKeepCell.getType(),
                        samaraKeepCell.getSubtype()
                );
    }

    @DisplayName("Сортировка лота из ячейки KEEP в адресную ячейку")
    @Test
    void sortNotEmptyLotFromKeepToAddressCell() {
        var samaraKeep = testFactory.storedCell(
                sortingCenter, "SAMARA_KEEP", CellType.BUFFER, CellSubType.BUFFER_XDOC,
                samaraWH.getYandexId(), null);

        var keep = testFactory.storedCell(
                sortingCenter, "XDOC_KEEP", CellType.BUFFER, CellSubType.BUFFER_XDOC,
                null, null);

        var samaraAddress = testFactory.storedCell(
                sortingCenter, "SAM_01_01", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);

        PartnerLotDto lotDto = sortableTestFactory.createEmptyLot(sortingCenter, samaraKeep);
        SortableLot basket = sortableLotService.findByLotIdOrThrow(lotDto.getId());

        // создадим коробки и отсортируем в лот
        flow.inboundBuilder("in-1")
                .nextLogisticPoint(samaraWH.getYandexId())
                .build()
                .linkBoxes("XDOC-b-1");


        Sortable box = sortableQueryService.find(sortingCenter, "XDOC-b-1").orElseThrow();
        caller.sort(
                box.getRequiredBarcodeOrThrow(),
                basket.getSortable()
        ).andExpect(status().isOk());

        var scanLotResponse = caller.getOrder(basket.getBarcode())
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);

        assertThat(scanLotResponse.getAvailableCells()).hasSize(1);

        assertThat(scanLotResponse.getAvailableCells().get(0))
                .extracting(
                        ApiCellDto::getId,
                        ApiCellDto::getNumber,
                        ApiCellDto::getStatus,
                        ApiCellDto::getType,
                        ApiCellDto::getSubType
                ).containsExactly(
                        samaraZone.getId(),
                        samaraZone.getName(),
                        CellStatus.ACTIVE,
                        CellType.BUFFER,
                        CellSubType.BUFFER_XDOC_LOCATION
                );

        caller.sort(basket.getBarcode(), samaraAddress)
                .andExpect(status().isOk());

        var basketAfterSort = sortableRepository.findByIdOrThrow(basket.getSortableId());

        assertThat(basketAfterSort.getCellIdOrNull())
                .isEqualTo(samaraAddress.getId());
    }

    @SneakyThrows
    @Test
    public void sortBoxToBufferCell() {
        var samaraAddress = testFactory.storedCell(
                sortingCenter, "SAM_01_01", CellType.BUFFER, CellSubType.BUFFER_XDOC_BOX,
                samaraWH.getYandexId(), samaraZone);

        var samaraAddress2 = testFactory.storedCell(
                sortingCenter, "SAM_01_02", CellType.BUFFER, CellSubType.BUFFER_XDOC_BOX,
                samaraWH.getYandexId(), samaraZone);

        setPropertyForEnableBufferBoxCell(true);
        flow.inboundBuilder("in-1")
                .nextLogisticPoint(samaraWH.getYandexId())
                .build()
                .linkBoxes("XDOC-1", "XDOC-2", "XDOC-3", "XDOC-4");

        ApiOrderDto orderDto = objectMapper.readValue(caller.getOrder("XDOC-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCells").isNotEmpty())
                .andExpect(jsonPath("$.availableCells[0].number").value(samaraZone.getName()))
                .andReturn().getResponse().getContentAsString(), ApiOrderDto.class);

        caller.sort(orderDto.getExternalId(), samaraAddress)
                .andExpect(status().isOk());

        ApiOrderDto orderDto2 = objectMapper.readValue(caller.getOrder("XDOC-2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCells").isNotEmpty())
                .andExpect(jsonPath("$.availableCells[0].number").value(samaraAddress.getScNumber()))
                .andReturn().getResponse().getContentAsString(), ApiOrderDto.class);

        caller.sort(orderDto2.getExternalId(), samaraAddress)
                .andExpect(status().isOk());

        ApiOrderDto orderDto3 = objectMapper.readValue(caller.getOrder("XDOC-3")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCells").isNotEmpty())
                .andExpect(jsonPath("$.availableCells[0].number").value(samaraAddress.getScNumber()))
                .andReturn().getResponse().getContentAsString(), ApiOrderDto.class);

        caller.sort(orderDto3.getExternalId(), samaraAddress2)
                .andExpect(status().isOk());

        ApiOrderDto orderDto4 = objectMapper.readValue(caller.getOrder("XDOC-4")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCells").isArray())
                .andExpect(jsonPath("$.availableCells").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.availableCells[0].number").value(samaraAddress.getScNumber()))
                .andExpect(jsonPath("$.availableCells[1].number").value(samaraAddress2.getScNumber()))
                .andReturn().getResponse().getContentAsString(), ApiOrderDto.class);

        caller.sort(orderDto4.getExternalId(), samaraAddress2)
                .andExpect(status().isOk());

        caller.fixInbound("in-1")
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    public void sortToLotWithAxaptaMovementRequest() {
        setPropertyForEnableBufferBoxCell(true);
        var samaraBoxCell = testFactory.storedCell(
                sortingCenter, "SAM_01_01", CellType.BUFFER, CellSubType.BUFFER_XDOC_BOX,
                samaraWH.getYandexId(), samaraZone);

        var samaraAddress = testFactory.storedCell(
                sortingCenter, "SAM_01_02", CellType.BUFFER, CellSubType.BUFFER_XDOC_LOCATION,
                samaraWH.getYandexId(), samaraZone);

        var lot = flow.createBasket(samaraAddress);

        Inbound inbound = flow.inboundBuilder("in-1")
                .nextLogisticPoint(samaraWH.getYandexId())
                .build()
                .linkBoxes("XDOC-1", "XDOC-2")
                .fixInbound()
                .getInbound("in-1");

        caller.sort("XDOC-1", samaraBoxCell)
                .andExpect(status().isOk());

        assertThat(sortableQueryService.findAllHavingAllBarcodes(sortingCenter, Set.of("XDOC-1")))
                .hasSize(1)
                .allMatch(box -> box.getCell() != null);

        inboundCommandService.setAxaptaMovementRequestId("Зпер123", "in-1");

        ApiOrderDto orderDto = objectMapper.readValue(caller.getOrder("XDOC-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableLots").isNotEmpty())
                .andExpect(jsonPath("$.lotSortAvailable").value(true))
                .andExpect(jsonPath("$.availableLots[0].lotName").value(lot.getNameForApi()))
                .andReturn().getResponse().getContentAsString(), ApiOrderDto.class);


        caller.sort(
                orderDto.getExternalId(),
                lot.getSortable()
        ).andExpect(status().isOk());

        assertThat(sortableQueryService.findAllHavingAllBarcodes(sortingCenter, Set.of("XDOC-1")))
                .hasSize(1)
                .allMatch(box -> box.getCell() == null);
    }

    private void setPropertyForEnableBufferBoxCell(boolean value) {
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_BUFFER_XDOC_BOX,
                value
        );
        if (value) {
            testFactory.setSortingCenterProperty(flow.getSortingCenter().getId(),
                    SortingCenterPropertiesKey.XDOC_ENABLED,
                    true);

            testFactory.setSortingCenterProperty(flow.getSortingCenter().getId(),
                    SortingCenterPropertiesKey.ENABLE_BUFFER_XDOC_LOCATION,
                    true);
        }
    }

    private ApiOrderDto sortToCell(String barcode, Cell cell) {
        var orderDto = caller.getOrder(barcode)
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);
        return caller.sort(orderDto.getExternalId(), cell)
                .andExpect(status().isOk())
                .getResponseAsClass(ApiOrderDto.class);
    }

}
