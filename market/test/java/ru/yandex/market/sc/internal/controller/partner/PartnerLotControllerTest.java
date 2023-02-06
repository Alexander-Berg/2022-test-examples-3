package ru.yandex.market.sc.internal.controller.partner;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellRequestDto;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.inbound.model.InboundRegistryDto;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.lot.model.PartnerLotRequestDto;
import ru.yandex.market.sc.core.domain.lot.repository.Lot;
import ru.yandex.market.sc.core.domain.lot.repository.LotRepository;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.outbound.OutboundQueryService;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceHistory;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.route.RouteCommandService;
import ru.yandex.market.sc.core.domain.route.model.RouteDocumentType;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DirectFlowType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableBarcodeSeq;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertySource;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.stage.StageLoader;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.controller.dto.PartnerCellDtoWrapper;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.ENABLE_BUFFER_XDOC_LOCATION;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.SORT_ZONE_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.XDOC_ENABLED;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@ScIntControllerTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
class PartnerLotControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    LotRepository lotRepository;
    @Autowired
    SortableLotService sortableLotService;
    @Autowired
    PlaceRepository placeRepository;
    @Autowired
    SortingCenterPropertySource propertySource;
    @Autowired
    OutboundQueryService outboundQueryService;
    @Autowired
    Clock clock;
    @Autowired
    RouteCommandService routeCommandService;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    ComplexMonitoring complexMonitoring;
    @Autowired
    SortableRepository sortableRepository;
    @Autowired
    SortableBarcodeSeq sortableBarcodeSeq;
    @Autowired
    ScIntControllerCaller caller;
    @Autowired
    XDocFlow flow;
    @Autowired
    SortableTestFactory sortableTestFactory;
    @Autowired
    SortingCenterPropertySource sortingCenterPropertySource;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
    }

    @Test
    @SneakyThrows
    void getLot() {
        var warehouse = testFactory.storedWarehouse("warehouse");
        var parentCell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN, warehouse.getYandexId());
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/lots/" + lot.getLotId())
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"lots\": [" + lotJson(lot, warehouse, null) + "]}", true));

    }

    @Test
    @SneakyThrows
    void getLots() {
        var parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/lots")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"content\": [" + lotJson(lot) + "]}", false));
    }

    @Test
    @SneakyThrows
    void getLotsFilterByName() {
        var parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/lots" +
                                "?name=p1")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"content\": [" + lotJson(lot) + "]}", false));
    }

    @Test
    @SneakyThrows
    void getLotsFilterByNameSubstring() {
        var parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/lots" +
                                "?name=p")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"content\": [" + lotJson(lot) + "]}", false));
    }

    @Test
    @SneakyThrows
    void getLotsFilterByCellId() {
        var parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/lots" +
                                "?cellId=" + parentCell.getId())
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"content\": [" + lotJson(lot) + "]}", false));
    }

    @Test
    void getCellsPage() throws Exception {
        var zoneA = testFactory.storedZone(sortingCenter, "ZA");
        var zoneB = testFactory.storedZone(sortingCenter, "ZB");

        var courier = testFactory.storedCourier();
        var c110 = testFactory.storedShipBufferCell(sortingCenter, courier.getId(), zoneA, 1, 1, 0, 10);
        testFactory.storedLot(sortingCenter, c110, LotStatus.CREATED);
        var c120 = testFactory.storedShipBufferCell(sortingCenter, courier.getId(), zoneA, 1, 2, 0, 10);
        testFactory.storedLot(sortingCenter, c120, LotStatus.CREATED);
        var c211 = testFactory.storedShipBufferCell(sortingCenter, courier.getId(), zoneA, 2, 1, 1, 10);
        testFactory.storedLot(sortingCenter, c211, LotStatus.CREATED);
        var c210 = testFactory.storedShipBufferCell(sortingCenter, courier.getId(), zoneB, 2, 1, 0, 10);
        testFactory.storedLot(sortingCenter, c210, LotStatus.CREATED);

        //select by alley zone
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                        + "/lots").param("cellZoneId", String.valueOf(zoneA.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(3)))
                .andExpect(jsonPath("$.content[*].cellZoneId")
                        .value(Matchers.everyItem(Matchers.equalTo(zoneA.getId().intValue()))));

        //select by alley number
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                        + "/lots").param("cellAlleyNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[*].cellAlleyNumber").value(Matchers.everyItem(Matchers.equalTo(1))));

        //select by alley number and section number
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                        + "/lots").param("cellAlleyNumber", "1").param("cellSectionNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[*].cellAlleyNumber").value(Matchers.everyItem(Matchers.equalTo(1))))
                .andExpect(jsonPath("$.content[*].cellSectionNumber").value(Matchers.everyItem(Matchers.equalTo(1))));

        //select by alley number and section number and levelId
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/lots").param("cellAlleyNumber", "2").param("cellSectionNumber", "1")
                        .param("cellLevelNumber", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[*].cellAlleyNumber").value(Matchers.everyItem(Matchers.equalTo(2))))
                .andExpect(jsonPath("$.content[*].cellSectionNumber").value(Matchers.everyItem(Matchers.equalTo(1))))
                .andExpect(jsonPath("$.content[*].cellLevelNumber").value(Matchers.everyItem(Matchers.equalTo(0))));
    }

    @Test
    @SneakyThrows
    void getLotsFilterByWrongCellId() {
        var wrongCell = testFactory.storedCell(sortingCenter, "wrong", CellType.RETURN);
        var parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/lots" +
                                "?cellId=" + wrongCell.getId())
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"content\": []}", false));
    }

    @Test
    @SneakyThrows
    void getLotsFilterByCellName() {
        var parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/lots" +
                                "?cellName=" + parentCell.getScNumber())
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"content\": [" + lotJson(lot) + "]}", false));
    }

    @Test
    @SneakyThrows
    void getLotsFilterByWrongCellName() {
        var wrongCell = testFactory.storedCell(sortingCenter, "wrong", CellType.RETURN);
        var parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/lots" +
                                "?cellName=" + wrongCell.getScNumber())
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"content\": []}", false));
    }

    @Test
    @SneakyThrows
    void getLotsCanBeDeletedFalse() {
        var parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        var place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().accept().sort().getPlace();
        assertThat(place.getCell()).isEqualTo(parentCell);
        testFactory.sortPlaceToLot(place, lot, testFactory.storedUser(sortingCenter, 1L));
        testFactory.prepareToShipLot(lot);
        var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();
        transactionTemplate.execute(t -> {
            routeCommandService.finishRouteWithLots(testFactory.getRouteIdForSortableFlow(route), null, sortingCenter);
            return null;
        });
        lot = testFactory.getLot(lot.getLotId());
        assertThat(lot.getOptLotStatus()).isEmpty();
        assertThat(lot.getStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/lots")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].externalId", Matchers.equalTo(lot.getBarcode())));
    }

    @Test
    @SneakyThrows
    void lotAddedToOrderHistoryOnLotShipment() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).get();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        testFactory.sortOrderToLot(order, lot, user);
        testFactory.prepareToShipLot(lot);
        transactionTemplate.execute(t -> {
            routeCommandService.finishRouteWithLots(testFactory.getRouteIdForSortableFlow(route), null, sortingCenter);
            Optional<PlaceHistory> history = testFactory.orderPlace(order)
                    .getHistory()
                    .stream()
                    .filter(h -> h.getMutableState().getLot() != null)
                    .findFirst();
            assertThat(history).isNotEmpty();

            return null;
        });
    }

    @Test
    @SneakyThrows
    void getMarkerPallet() {
        testFactory.storedWarehouse("yandexId");
        var parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN, "yandexId");
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);

        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/{scId}/lots/{id}/markerPallet",
                                sortingCenter.getPartnerId(),
                                lot.getLotId(),
                                RouteDocumentType.NORMAL
                        )
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    private String lotJson(SortableLot lot) {
        return lotJson(lot, null, null);
    }

    /**
     * Не можем отгрузить лоты у маршрута, когда нет ни одного готового к отгрузке лота
     */
    @Test
    @SneakyThrows
    void shipLotWhenNoLotReady() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().accept().sort().getPlace();
        var cell = place.getCell();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        place = testFactory.sortPlaceToLot(place, lot, user);
        var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();

        shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter.getPartnerId())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.error", is(ScErrorCode.LOT_CANT_FIND.name())))
                .andExpect(jsonPath("$.message", not(isEmptyString())));
    }

    /**
     * Отгружаем лот в котором есть часть многоместного заказа.
     * В лоте: 2 обычный заказа. Одна посылка многоместного заказа.
     */
    @Test
    @SneakyThrows
    void shipLotWithMultiPlaceOrders() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var regularOrder1 = testFactory.create(
                        order(sortingCenter).externalId("o1").shipmentDate(LocalDate.now(clock)).build())
                .updateCourier(testFactory.storedCourier())
                .accept().sort().ship().makeReturn().accept().sort().get();
        var regularOrder2 = testFactory.create(
                        order(sortingCenter).externalId("o2").shipmentDate(LocalDate.now(clock)).build())
                .updateCourier(testFactory.storedCourier())
                .accept().sort().ship().makeReturn().accept().sort().get();
        var multiPlaceOrder = testFactory.create(
                        order(sortingCenter).externalId("o3")
                                .places("p1", "p2").shipmentDate(LocalDate.now(clock)).build())
                .updateCourier(testFactory.storedCourier())
                .acceptPlaces(List.of("p1", "p2"))
                .sortPlaces(List.of("p1", "p2"))
                .ship()
                .makeReturn()
                .acceptPlaces(List.of("p1", "p2"))
                .sortPlaces("p1")
                .get();
        var placeToLot = placeRepository.findByOrderIdAndMainPartnerCode(multiPlaceOrder.getId(), "p1")
                .orElseThrow();
        var cell = testFactory.orderPlace(regularOrder1).getCell();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        regularOrder1 = testFactory.sortOrderToLot(regularOrder1, lot, user);
        regularOrder2 = testFactory.sortOrderToLot(regularOrder2, lot, user);
        placeToLot = testFactory.sortPlaceToLot(placeToLot, lot, user);
        testFactory.prepareToShipLot(lot);
        var route = testFactory.findOutgoingWarehouseRoute(regularOrder1).orElseThrow();
        shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter.getPartnerId())
                .andExpect(status().is2xxSuccessful());
        assertThat(lotRepository.findByIdOrThrow(lot.getLotId()).getStatus()).isEqualTo(LotStatus.SHIPPED);
        assertThat(testFactory.getOrder(regularOrder1.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(testFactory.getOrder(regularOrder2.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(testFactory.getOrder(multiPlaceOrder.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(placeRepository.findByOrderIdAndMainPartnerCode(multiPlaceOrder.getId(), "p1")
                .orElseThrow().getStatus())
                .isEqualTo(PlaceStatus.RETURNED);
        assertThat(placeRepository.findByOrderIdAndMainPartnerCode(multiPlaceOrder.getId(), "p2")
                .orElseThrow().getStatus())
                .isEqualTo(PlaceStatus.ACCEPTED);
    }

    /**
     * Отгружаем маршрут с одним готовым к отгрузке лотом
     */
    @Test
    @SneakyThrows
    void shipLotReady() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().accept().sort().getPlace();
        var cell = place.getCell();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        place = testFactory.sortPlaceToLot(place, lot, user);
        testFactory.prepareToShipLot(lot);
        var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();

        shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter.getPartnerId())
                .andExpect(status().is2xxSuccessful());
        assertThat(lotRepository.findByIdOrThrow(lot.getLotId()).getStatus()).isEqualTo(LotStatus.SHIPPED);
        assertThat(testFactory.updated(place).getFfStatus())
                .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
    }

    /**
     * Отгружаем маршрут, у которого есть два лота на одной ячейке. Один лот готов к отгрузке, другой нет
     */
    @Test
    @SneakyThrows
    void shipRouteWith2Lots() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var place1 = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().accept().sort().getPlace();
        var place2 = testFactory.create(
                        order(sortingCenter).externalId("o2").shipmentDate(LocalDate.now(clock)).build())
                .updateCourier(testFactory.storedCourier())
                .accept().sort().ship().makeReturn().accept().sort().getPlace();
        var cell = place1.getCell();
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        place1 = testFactory.sortPlaceToLot(place1, lot1, user);
        place2 = testFactory.sortPlaceToLot(place2, lot2, user);
        testFactory.prepareToShipLot(lot1);
        var route = testFactory.findOutgoingWarehouseRoute(place1).orElseThrow();

        shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter.getPartnerId())
                .andExpect(status().is2xxSuccessful());
        assertThat(lotRepository.findByIdOrThrow(lot1.getLotId()).getStatus()).isEqualTo(LotStatus.SHIPPED);
        assertThat(lotRepository.findByIdOrThrow(lot2.getLotId()).getStatus()).isEqualTo(LotStatus.PROCESSING);
        assertThat(testFactory.updated(place1).getFfStatus())
                .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(testFactory.updated(place2).getFfStatus())
                .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    /**
     * Отгружаем маршрут на котором два заказа. один заказ находится в лоте, который готов к отгрузке
     */
    @Test
    @SneakyThrows
    void shipLotReadyWith2OrdersOnRoute() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var place1 = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().accept().sort().getPlace();
        var place2 = testFactory.create(
                        order(sortingCenter).externalId("o2").shipmentDate(LocalDate.now(clock)).build())
                .updateCourier(testFactory.storedCourier())
                .accept().sort().ship().makeReturn().accept().sort().getPlace();
        var cell = place1.getCell();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        place1 = testFactory.sortPlaceToLot(place1, lot, user);
        testFactory.prepareToShipLot(lot);
        var route = testFactory.findOutgoingWarehouseRoute(place1).orElseThrow();

        shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter.getPartnerId())
                .andExpect(status().is2xxSuccessful());
        assertThat(lotRepository.findByIdOrThrow(lot.getLotId()).getStatus()).isEqualTo(LotStatus.SHIPPED);
        assertThat(testFactory.updated(place1).getFfStatus())
                .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(testFactory.updated(place2).getFfStatus())
                .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    /**
     * Не можем отгрузить маршрут, если СЦ поддерживает возвратные отправки, но отправка не была создана.
     */
    @Test
    @Disabled("Пока не кидаем ошибку, если не смогли отгрузить outbound")
    @SneakyThrows
    void cantShipRouteWithoutOutboundIfSupported() {
        // Включаем поддержку отправок
        enableReturnOutbounds();

        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().accept().sort().getPlace();
        var cell = place.getCell();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        place = testFactory.sortPlaceToLot(place, lot, user);
        testFactory.prepareToShipLot(lot);
        var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();

        shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter.getPartnerId())
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                        .json("{\"status\":400,\"error\":\"INVALID_ACTION\"," +
                                " \"message\": \"Appropriate outbound is not found for warehouse " +
                                route.getWarehouseTo().getYandexId() + "\"}", false));
    }

    /**
     * Можем отгрузить маршрут, если СЦ поддерживает возвратные отправки, но отправка не была создана.
     */
    @Test
    @SneakyThrows
    void canShipRouteWithoutOutboundIfSupported() {
        // Включаем поддержку отправок
        enableReturnOutbounds();

        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().accept().sort().getPlace();
        var cell = place.getCell();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        place = testFactory.sortPlaceToLot(place, lot, user);
        testFactory.prepareToShipLot(lot);
        var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();

        shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter.getPartnerId())
                .andExpect(status().is2xxSuccessful());
        assertThat(lotRepository.findByIdOrThrow(lot.getLotId()).getStatus()).isEqualTo(LotStatus.SHIPPED);
        assertThat(testFactory.updated(place).getFfStatus())
                .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);

        // Проверяем что зажгли мониторинг
        var monitoringResult = complexMonitoring.getResult("outboundShippingFailed");
        assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.CRITICAL);
        assertThat(monitoringResult.getMessage())
                .isEqualTo("outboundShippingFailed: Entity not found: Outbound for warehouse[9865]");
    }

    @Test
    @SneakyThrows
    void shipRouteWithOutbound() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        // Включаем поддержку отправок и создаем отправку
        enableReturnOutbounds();
        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("222")
                .fromTime(Instant.parse("2021-04-22T02:00:00Z"))
                .toTime(Instant.parse("2021-04-22T12:00:00Z"))
                .locationCreateRequest(TestFactory.locationCreateRequest())
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId())
                .build()
        );

        var user = testFactory.getOrCreateStoredUser(sortingCenter);

        var returned = sortAndPrepare(testFactory.createForToday(order(sortingCenter)
                        .externalId("returnOrder")
                        .warehouseReturnId(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId())
                        .build())
                .accept().sort().ship().makeReturn().accept().sort().getPlace(), user);

        var clientReturn = sortAndPrepare(testFactory.createForToday(order(sortingCenter)
                        .externalId("clientReturnOrder")
                        .isClientReturn(true)
                        .warehouseReturnId(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId())
                        .build())
                .accept().sort().getPlace(), user);

        var damaged = sortAndPrepare(testFactory.createForToday(order(sortingCenter)
                        .externalId("damagedOrder")
                        .warehouseReturnId(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId())
                        .warehouseCanProcessDamagedOrders(true)
                        .build())
                .accept().markOrderAsDamaged().sort().getPlace(), user
        );

        // Ожидаем, что все заказы попали в один Route
        var route = testFactory.findOutgoingWarehouseRoute(returned).orElseThrow();
        Lot returnedLot = returned.getLot();
        Lot clientReturnLot = clientReturn.getLot();
        Lot damagedLot = damaged.getLot();
        shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter.getPartnerId())
                .andExpect(status().is2xxSuccessful());

        checkStatus(returned, returnedLot, ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM, LotStatus.SHIPPED);
        checkStatus(clientReturn, clientReturnLot, ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM,
                LotStatus.SHIPPED);
        checkStatus(damaged, damagedLot, ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM, LotStatus.SHIPPED);

        // Проверяем, что заказы попали в реестры отправки
        var outboundInfo = outboundQueryService.getOutboundInfoBy(outbound.getExternalId());
        // шипнется потом dbqueue таской
        assertThat(outboundInfo.getOutbound().getStatus()).isEqualTo(OutboundStatus.CREATED);
        var registries = outboundInfo.getOrderRegistries();
        assertThat(registries).hasSize(3);
        var orderToRegistryType = registries.stream()
                .peek(registry -> assertThat(registry.getOrders()).hasSize(1))
                .collect(Collectors.toMap(
                        registry -> registry.getOrders().get(0).getOrderExternalId(),
                        InboundRegistryDto::getType
                ));
        assertThat(orderToRegistryType.get(returned.getExternalId()))
                .isEqualTo(RegistryType.FACTUAL_UNDELIVERED_ORDERS_RETURN);
        assertThat(orderToRegistryType.get(clientReturn.getExternalId()))
                .isEqualTo(RegistryType.FACTUAL_DELIVERED_ORDERS_RETURN);
        assertThat(orderToRegistryType.get(damaged.getExternalId()))
                .isEqualTo(RegistryType.FACTUAL_UNDELIVERED_ORDERS_RETURN);
    }

    @Test
    @SneakyThrows
    void getAllFilteredLots() {
        var parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        mockMvc.perform(MockMvcRequestBuilders.get(getFilteredUrl(null, List.of("c1"), null)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"content\": [" + lotJson(lot) + "]}", false));
    }

    @Test
    @SneakyThrows
    void getFilteredByCellLots() {
        var parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        var secondCell = testFactory.storedCell(sortingCenter, "c2", CellType.RETURN);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell);
        mockMvc.perform(MockMvcRequestBuilders.get(getFilteredUrl(null, List.of("c2"), null)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"content\": [" + lotJson(lot2) + "]}", false));
    }

    @Test
    @SneakyThrows
    void getFilteredByStatusLots() {
        var parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        var secondCell = testFactory.storedCell(sortingCenter, "c2", CellType.RETURN);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell, LotStatus.PROCESSING,
                false);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET,  secondCell, LotStatus.READY, false);
        mockMvc.perform(MockMvcRequestBuilders.get(getFilteredUrl(null, null, List.of(LotStatus.CREATED,
                        LotStatus.PROCESSING))))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"content\": [" + Stream.of(lot1, lot2)
                        .map(this::lotJson)
                        .collect(Collectors.joining(",\n")) + "]}", false));
    }

    @Test
    @SneakyThrows
    void getFilteredByCellAndStatusLots() {
        var parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        var secondCell = testFactory.storedCell(sortingCenter, "c2", CellType.RETURN);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell, LotStatus.PROCESSING,
                false);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell, LotStatus.READY, false);
        mockMvc.perform(MockMvcRequestBuilders.get(getFilteredUrl(null, List.of("c2"), List.of(LotStatus.CREATED,
                        LotStatus.PROCESSING))))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"content\": [" + lotJson(lot2) + "]}", false));
    }

    @Test
    @SneakyThrows
    void getFilteredByCellsLots() {
        var parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        var secondCell = testFactory.storedCell(sortingCenter, "c2", CellType.RETURN);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell);
        var thirdCell = testFactory.storedCell(sortingCenter, "c3", CellType.RETURN);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, thirdCell);
        mockMvc.perform(MockMvcRequestBuilders.get(getFilteredUrl(null, List.of("c2, c3"), null)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"content\": [" + Stream.of(lot2, lot3)
                        .map(this::lotJson)
                        .collect(Collectors.joining(",\n")) + "]}", false));
    }

    @Test
    @SneakyThrows
    void getFullFilteredLots() {
        var parentCell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, parentCell);
        var secondCell = testFactory.storedCell(sortingCenter, "c2", CellType.RETURN);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell, LotStatus.PROCESSING,
                false);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, secondCell, LotStatus.READY, false);
        mockMvc.perform(MockMvcRequestBuilders.get(getFilteredUrl(lot2.getBarcode(), List.of("c2"),
                        List.of(LotStatus.PROCESSING))))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"content\": [" + lotJson(lot2) + "]}", false));
    }

    /**
     * различные комбинации фильтров покрыты тестами класса
     * этот тест на корректность сериализации при запросах с фильтром по типу
     */
    @DisplayName("фильтрация по LotType")
    @Test
    @SneakyThrows
    void filterByLotType() {
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.BUFFER, CellSubType.BUFFER_XDOC);
        testFactory.storedLot(sortingCenter, SortableType.PALLET, cell, LotStatus.CREATED, false);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell, LotStatus.READY, false);
        testFactory.storedLot(sortingCenter, SortableType.PALLET, cell, LotStatus.PROCESSING, false);

        mockMvc.perform(MockMvcRequestBuilders.get(getFilteredUrl(null, null, List.of(LotStatus.READY),
                        SortableType.PALLET)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"content\": [" + lotJson(lot2) + "]}", false));
    }

    @DisplayName("Мапа доступных статусов по типам лотов (тест добавлен для защиты контракта с фронтом)")
    @Test
    @SneakyThrows
    void getStatusesByLotType() {
        String url = format("/internal/partners/%s/lots/statusesByType", sortingCenter.getPartnerId());
        mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(status().isOk())
                .andExpect(content().json("{\n" +
                        "  \"PALLET\": [\n" +
                        "    \"CREATED\",\n" +
                        "    \"PROCESSING\",\n" +
                        "    \"READY\",\n" +
                        "    \"SHIPPED\"\n" +
                        "  ],\n" +
                        "  \"XDOC_BASKET\": [\n" +
                        "    \"CREATED\",\n" +
                        "    \"PROCESSING\",\n" +
                        "    \"PACKED\",\n" +
                        "    \"READY\",\n" +
                        "    \"SHIPPED\"\n" +
                        "  ]\n" +
                        "}"));
    }

    @DisplayName("Создание XDoc лота (XDOC_BASKET)")
    @Test
    @SneakyThrows
    void createXDocLot() {
        var xDocCell = testFactory.storedCell(sortingCenter, "c1", CellType.BUFFER, CellSubType.BUFFER_XDOC);
        var expectedBarcode = "XDOC-" + (sortableBarcodeSeq.getId() + 1);

        createLot(xDocCell.getId())
                .andExpect(status().isOk());

        var createdLot = sortableLotService.findByExternalIdAndSortingCenter(expectedBarcode, sortingCenter)
                .orElseThrow();
        assertThat(createdLot.getLotStatusOrNull()).isEqualTo(LotStatus.CREATED);
        assertThat(createdLot.getType()).isEqualTo(SortableType.XDOC_BASKET);
        assertThat(createdLot.getParentCellId()).isEqualTo(xDocCell.getId());
        assertThat(createdLot.getSortableId()).isNotNull();
        assertThat(createdLot.getBarcode()).isEqualTo(expectedBarcode);

        var sortableId = createdLot.getSortableId();
        var sortable = sortableRepository.findById(sortableId)
                .orElseThrow(() -> new AssertionError("Отсутствует Sortable с id=" + sortableId));

        assertThat(sortable.getType()).isEqualTo(SortableType.XDOC_BASKET);
        assertThat(sortable.getDirectFlow()).isEqualTo(DirectFlowType.TRANSIT);
        assertThat(sortable.getRequiredBarcodeOrThrow()).isEqualTo(expectedBarcode);
        assertThat(sortable.getMutableState().getInbound()).isNull();
        assertThat(sortable.getMutableState().getOutbound()).isNull();
        assertThat(sortable.getMutableState().getInRoute()).isNull();
        assertThat(sortable.getMutableState().getOutRoute()).isNull();
        assertThat(sortable.getMutableState().getStatus()).isEqualTo(SortableStatus.KEEPED_DIRECT);
        assertThat(sortable.getMutableState().getParent()).isNull();
        assertThat(sortable.getMutableState().getCell()).isEqualTo(xDocCell);
    }

    @DisplayName("Создание XDoc лота (XDOC_BASKET) в ячейке хранения BUFFER_XDOC_LOCATION")
    @Test
    void createXDocLotInsideBufferXDocLocationCell() {
        enableXDocLocationCells();
        var zone = testFactory.storedZone(sortingCenter, "spb-zone-1");
        var warehouse = testFactory.storedWarehouse("sbp-warehouse-1");
        var response = caller.createCell(
                        CellRequestDto.builder()
                                .number("sbp_keep_1")
                                .type(CellType.BUFFER)
                                .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                .status(CellStatus.ACTIVE)
                                .zoneId(zone.getId())
                                .warehouseYandexId(warehouse.getYandexId())
                                .build()
                )
                .andExpect(status().isOk())
                .getResponseAsClass(PartnerCellDtoWrapper.class);

        var cellId = response.getCell().getId();
        var expectedBarcode = "XDOC-" + (sortableBarcodeSeq.getId() + 1);

        caller.createLot(new PartnerLotRequestDto(cellId, 1))
                .andExpect(status().isOk());

        var createdLot = sortableLotService.findByExternalIdAndSortingCenter(expectedBarcode, sortingCenter)
                .orElseThrow();
        assertThat(createdLot.getLotStatusOrNull()).isEqualTo(LotStatus.CREATED);
        assertThat(createdLot.getType()).isEqualTo(SortableType.XDOC_BASKET);
        assertThat(createdLot.getParentCellId()).isEqualTo(cellId);
        assertThat(createdLot.getSortableId()).isNotNull();
        assertThat(createdLot.getBarcode()).isEqualTo(expectedBarcode);

        var sortableId = createdLot.getSortableId();
        var sortable = sortableRepository.findById(sortableId)
                .orElseThrow(() -> new AssertionError("Отсутствует Sortable с id=" + sortableId));

        assertThat(sortable.getType()).isEqualTo(SortableType.XDOC_BASKET);
        assertThat(sortable.getDirectFlow()).isEqualTo(DirectFlowType.TRANSIT);
        assertThat(sortable.getRequiredBarcodeOrThrow()).isEqualTo(expectedBarcode);
        assertThat(sortable.getMutableState().getInbound()).isNull();
        assertThat(sortable.getMutableState().getOutbound()).isNull();
        assertThat(sortable.getMutableState().getInRoute()).isNull();
        assertThat(sortable.getMutableState().getOutRoute()).isNull();
        assertThat(sortable.getMutableState().getStatus()).isEqualTo(SortableStatus.KEEPED_DIRECT);
        assertThat(sortable.getMutableState().getParent()).isNull();
        assertThat(sortable.getMutableState().getCell().getId()).isEqualTo(cellId);
    }

    @DisplayName("""
            Невозможно создать лот XDOC_BASKET в ячейке BUFFER_XDOC_LOCATION
            Если совершена попытка создать более одного лота""")
    @Test
    void unableToCreateMoreThanOneLotForBufferXDocLocationCell() {
        enableXDocLocationCells();
        var zone = testFactory.storedZone(sortingCenter, "spb-zone-1");
        var warehouse = testFactory.storedWarehouse("sbp-warehouse-1");
        var response = caller.createCell(
                        CellRequestDto.builder()
                                .number("sbp_keep_1")
                                .type(CellType.BUFFER)
                                .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                .status(CellStatus.ACTIVE)
                                .zoneId(zone.getId())
                                .warehouseYandexId(warehouse.getYandexId())
                                .build()
                )
                .andExpect(status().isOk())
                .getResponseAsClass(PartnerCellDtoWrapper.class);

        caller.createLot(new PartnerLotRequestDto(response.getCell().getId(), 2))
                .andExpect(status().isBadRequest());

        assertThat(sortableRepository.findAll()).isEmpty();
    }


    @DisplayName("""
            Невозможно создать лот XDOC_BASKET в ячейке BUFFER_XDOC_LOCATION
            Если в ячейке уже лежит XDOC_PALLET или XDOC_BOX""")
    @Test
    void unableToCreateLotIfBufferXDocLocationCellContainsXDocPalletOrBox() {
        enableXDocLocationCells();
        var zone = testFactory.storedZone(sortingCenter, "spb-zone-1");
        var warehouse = testFactory.storedWarehouse("sbp-warehouse-1");
        var cellWithBox = caller.createCell(
                        CellRequestDto.builder()
                                .number("sbp_keep_1")
                                .type(CellType.BUFFER)
                                .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                .status(CellStatus.ACTIVE)
                                .zoneId(zone.getId())
                                .warehouseYandexId(warehouse.getYandexId())
                                .build()
                )
                .andExpect(status().isOk())
                .getResponseAsClass(PartnerCellDtoWrapper.class);

        var cellWithPallet = caller.createCell(
                        CellRequestDto.builder()
                                .number("sbp_keep_2")
                                .type(CellType.BUFFER)
                                .subType(CellSubType.BUFFER_XDOC_LOCATION)
                                .status(CellStatus.ACTIVE)
                                .zoneId(zone.getId())
                                .warehouseYandexId(warehouse.getYandexId())
                                .build()
                )
                .andExpect(status().isOk())
                .getResponseAsClass(PartnerCellDtoWrapper.class);

        flow.inboundBuilder("in-1")
                .nextLogisticPoint(warehouse.getYandexId())
                .build()
                .linkBoxes("XDOC-b-1")
                .fixInbound()
                .inboundBuilder("in-2")
                .nextLogisticPoint(warehouse.getYandexId())
                .build()
                .linkPallets("XDOC-p-1")
                .fixInbound();
        sortableTestFactory.sortByBarcode("XDOC-b-1", cellWithBox.getCell().getId());
        sortableTestFactory.sortByBarcode("XDOC-p-1", cellWithPallet.getCell().getId());

        caller.createLot(new PartnerLotRequestDto(cellWithBox.getCell().getId(), 1))
                .andExpect(status().isBadRequest());
        caller.createLot(new PartnerLotRequestDto(cellWithPallet.getCell().getId(), 1))
                .andExpect(status().isBadRequest());

        assertThat(sortableRepository.findAll().stream().map(Sortable::getRequiredBarcodeOrThrow))
                .containsExactlyInAnyOrder("XDOC-b-1", "XDOC-p-1");
    }

    @DisplayName("Создание обычного лота (PALLET)")
    @Test
    @SneakyThrows
    void createLot() {
        var courierCell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var expectedBarcode = "SC_LOT_" + (sortableBarcodeSeq.getId() + 1);

        createLot(courierCell.getId())
                .andExpect(status().isOk());

        var createdLot = sortableLotService.findByExternalIdAndSortingCenter(expectedBarcode, sortingCenter)
                .orElseThrow();
        assertThat(createdLot.getLotStatusOrNull()).isEqualTo(LotStatus.CREATED);
        assertThat(createdLot.getType()).isEqualTo(SortableType.PALLET);
        assertThat(createdLot.getParentCellId()).isEqualTo(courierCell.getId());
        assertThat(createdLot.getSortableId()).isNotNull();
        assertThat(createdLot.getBarcode()).isEqualTo(expectedBarcode);

        var sortableId = createdLot.getSortableId();
        assertThat(sortableRepository.findById(sortableId)).isPresent();
        // на данному этапе нет никаких требований о том что должен содержать Sortable для обычного лота нет
    }

    @Test
    @DisplayName("Не бросать исключение при повторном нажатии на кнопку 'Отгрузить лоты'")
    @SneakyThrows
    void notThrowExceptionWhenRouteHaveLotAlreadyShipped() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        List<Place> places = new ArrayList<>();
        IntStream.range(1, 5)
                .forEach(i -> {
                    var place = testFactory.createForToday(order(sortingCenter, String.valueOf(i)).build())
                            .accept().sort().ship()
                            .makeReturn()
                            .accept().sort()
                            .getPlace();
                    places.add(place);
                });

        var p1 = places.get(0);
        var cell = p1.getCell();
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        testFactory.sortPlaceToLot(places.get(0), lot1, user);
        testFactory.sortPlaceToLot(places.get(1), lot1, user);
        testFactory.prepareToShipLot(lot1);

        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        testFactory.sortPlaceToLot(places.get(2), lot2, user);
        testFactory.sortPlaceToLot(places.get(3), lot2, user);

        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);

        var route = testFactory.findOutgoingWarehouseRoute(p1).orElseThrow();
        shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter.getPartnerId());

        assertThatCode(() -> shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter.getPartnerId()))
                .doesNotThrowAnyException();
        lot1 = testFactory.getLot(lot1.getLotId());
        lot2 = testFactory.getLot(lot2.getLotId());
        lot3 = testFactory.getLot(lot3.getLotId());
        assertThat(lot1.getOptLotStatus()).isEmpty();
        assertThat(lot1.getStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
        assertThat(lot2.getLotStatusOrNull()).isEqualTo(LotStatus.PROCESSING);
        assertThat(lot3.getLotStatusOrNull()).isEqualTo(LotStatus.CREATED);
    }

    private String getFilteredUrl(String barcode, List<String> cellName, List<LotStatus> statuses) {
        return getFilteredUrl(barcode, cellName, statuses, null);
    }

    private String getFilteredUrl(String barcode, List<String> cellName, List<LotStatus> statuses,
                                  @Nullable SortableType lotType) {
        String reqName = barcode == null
                ? null
                : "externalId=" + barcode;
        String lotStatus = statuses == null
                ? null
                : "lotStatus=" +
                statuses.stream()
                        .map(Enum::toString)
                        .collect(Collectors.joining(", "));
        String cell = cellName == null
                ? null
                : "cellName=" + String.join(", ", cellName);
        String reqType = lotType == null
                ? null
                : "lotType=" + lotType.name();
        String endUrl = Stream.of(reqName, cell, lotStatus, reqType)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("&", "?", ""));
        return format("/internal/partners/%s/lots%s", sortingCenter.getPartnerId(), endUrl);
    }

    private void checkStatus(Place place, Lot lot, ScOrderFFStatus expectedOrderStatus,
                             LotStatus expectedLotStatus) {
        assertThat(lotRepository.findByIdOrThrow(lot.getId()).getStatus()).isEqualTo(expectedLotStatus);
        assertThat(testFactory.updated(place).getFfStatus()).isEqualTo(expectedOrderStatus);
    }

    private Place sortAndPrepare(Place place, User user) {
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, place.getCell());
        place = testFactory.sortPlaceToLot(place, lot, user);
        testFactory.prepareToShipLot(lot);
        return place;
    }

    private String lotJson(SortableLot lot, @Nullable Warehouse warehouse, @Nullable Boolean canBeDeleted) {
        var lotCreatedAt = LocalDate.ofInstant(lot.getCreatedAt(), DateTimeUtil.DEFAULT_ZONE_ID);
        var parentCell = lot.getParentCell();
        var stage = StageLoader.getById(lot.getStageId());

        return "{\"cellName\":\"" + Cell.getNameOrNull(parentCell) + "\"" +
                ",\"deleted\":" + lot.isDeleted() +
                ", \"canBeDeleted\":" + (canBeDeleted == null ? "true" : canBeDeleted) +
                ",\"id\":" + lot.getLotId() +
                ",\"externalId\":" + lot.getBarcode() +
                ",\"cellType\":\"" +
                (Cell.getTypeOrNull(parentCell) == null ? "" : Cell.getTypeOrNull(parentCell)) + "\"" +
                ",\"cellSubType\":\"" + Cell.getSubTypeOrNull(parentCell) + "\"" +
                ",\"createdAt\":\"" + lotCreatedAt + "\"" +
                (warehouse == null ? "" : ",\"warehouse\":\"" + warehouse.getIncorporation() + "\"") +
                ",\"status\":\"" + lot.getOptLotStatus().orElse(LotStatus.SHIPPED) + "\"" +
                ",\"type\":\"" + lot.getType().name() + "\"" +
                ",\"placeCount\":0" +
                ",\"stageName\":" + "\"" + stage.getSystemName() + "\"" +
                "}";
    }

    private void enableReturnOutbounds() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.RETURN_OUTBOUND_ENABLED, "true");
    }

    @SneakyThrows
    private ResultActions shipLots(long routeId, String scPartnerId) {
        return mockMvc.perform(
                MockMvcRequestBuilders.put("/internal/partners/{scId}/lots/{routeId}", scPartnerId, routeId)
        );
    }

    private ResultActions createLot(long cellId) {
        return createLot(cellId, 1);
    }

    @SneakyThrows
    private ResultActions createLot(long cellId, int count) {
        return mockMvc.perform(
                MockMvcRequestBuilders.post("/internal/partners/{scId}/lots", sortingCenter.getPartnerId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "  \"cellId\": " + cellId + "," +
                                "  \"count\": " + count +
                                "}")
        );
    }

    private void enableXDocLocationCells() {
        testFactory.setSortingCenterPropertyMap(sortingCenter, Map.of(
                XDOC_ENABLED, true,
                ENABLE_BUFFER_XDOC_LOCATION, true,
                SORT_ZONE_ENABLED, true)
        );
    }

}
