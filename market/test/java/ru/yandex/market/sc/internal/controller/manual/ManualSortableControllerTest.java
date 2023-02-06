package ru.yandex.market.sc.internal.controller.manual;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.measurements_so.model.UnitMeasurementsDto;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.scan.SortableToSqsManager;
import ru.yandex.market.sc.core.domain.scan.event.SortableSortedOnDropoffEvent;
import ru.yandex.market.sc.core.domain.scan.model.DoCargoUnitStatusEventType;
import ru.yandex.market.sc.core.domain.scan.model.SaveVGHRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.UnlinkSortablesRequest;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DirectFlowType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.model.les_event.ResendSortableInfoToSqsRequest;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.external.sqs.SqsQueueProperties;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.controller.manual.xdoc.DeleteSortablesRequest;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ExtendWith(DefaultScUserWarehouseExtension.class)
class ManualSortableControllerTest {

    private static final String QUEUE_NAME = "sc_out";
    private static final String SOURCE = "sc";

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final SortableTestFactory sortableTestFactory;
    private final SortableQueryService sortableQueryService;
    private final SortableLotService sortableLotService;
    private final XDocFlow flow;
    private final ScIntControllerCaller caller;

    private DeliveryService deliveryServiceWithLotSortEnabled;
    private TestFactory.CourierWithDs courierWithDs;

    @MockBean
    Clock clock;

    @MockBean
    SortableToSqsManager sortableToSqsManager;

    @Autowired
    SqsQueueProperties sqsQueueProperties;

    SortingCenter sortingCenter;
    Warehouse warehouse;
    DeliveryService deliveryService;
    User user;


    @BeforeEach
    void init() {
        when(sqsQueueProperties.getOutQueue()).thenReturn(QUEUE_NAME);
        when(sqsQueueProperties.getSource()).thenReturn(SOURCE);
        sortingCenter = testFactory.storedSortingCenter();
        warehouse = testFactory.storedWarehouse();
        testFactory.setupMockClock(clock);
        courierWithDs = testFactory.magistralCourier("666");
        deliveryServiceWithLotSortEnabled = courierWithDs.deliveryService();
        deliveryService = testFactory.storedDeliveryService(sortingCenter.getId().toString());
        user = testFactory.storedUser(sortingCenter, 1L);
        testFactory.storedMagistralCell(sortingCenter, courierWithDs.courier().getId());
    }

    @Test
    @SneakyThrows
    @DisplayName("Демо сортировка в ячейку отгрузки для sortable")
    void sortDemoXdocSuccessful() {
        String barcode = "XDOC-123";
        Inbound inbound = createInbound(InboundType.XDOC_TRANSIT);
        Outbound outbound = createOutbound();

        Sortable sortable = sortableTestFactory.storeSortable(sortingCenter,
                SortableType.XDOC_PALLET, DirectFlowType.TRANSIT, barcode, inbound, null).get();

        createRegistry(outbound, List.of(sortable));
        assertThat(sortable.getStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);

        mockMvc.perform(
                        MockMvcRequestBuilders.put(
                                "/manual/orders/" + sortable.getRequiredBarcodeOrThrow() + "/demoSortXdoc"
                                ).param("scId", String.valueOf(sortingCenter.getId()))
                )
                .andExpect(status().isOk());

        assertThat(sortableQueryService.find(sortingCenter, barcode)
                .orElseThrow()
                .getStatus())
                .isEqualTo(SortableStatus.SORTED_DIRECT);
    }

    @Test
    @SneakyThrows
    @DisplayName("Демо создание сортаблов")
    void createDemoSortable() {
        Inbound inbound = createInbound(InboundType.XDOC_TRANSIT);
        String barcode = "XDOC-123";

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/orders/createDemoXdoc")
                                .param("scId", String.valueOf(sortingCenter.getId()))
                                .param("inboundId", inbound.getExternalId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.format("[{\"sortableId\": \"%s\", \"type\": \"%s\"}]", barcode,
                                        SortableType.XDOC_BOX))
                )
                .andExpect(status().isOk());

        Sortable sortable = sortableQueryService.find(sortingCenter, barcode).orElseThrow();

        assertThat(sortable.getType()).isEqualTo(SortableType.XDOC_BOX);
        assertThat(sortable.getStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);
    }

    @Test
    @SneakyThrows
    @DisplayName("Демо сортировка в лот сортаблов")
    void demoLotSortXdoc() {
        Inbound inbound = createInbound(InboundType.XDOC_TRANSIT);
        String barcode = "XDOC-b1";
        String barcodeBasket = "XDOC-10000";

        Sortable sortable = sortableTestFactory.storeSortable(sortingCenter,
                SortableType.XDOC_BOX, DirectFlowType.TRANSIT, barcode, inbound, null).get();

        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.XDOC_BASKET,
                testFactory.storedCell(sortingCenter, barcodeBasket, CellType.BUFFER, CellSubType.BUFFER_XDOC,
                        warehouse.getYandexId()));

        mockMvc.perform(
                        MockMvcRequestBuilders.put("/manual/orders/" + barcode + "/demoLotSortXdoc")
                                .param("scId", String.valueOf(sortingCenter.getId()))
                )
                .andExpect(status().isOk());

        Sortable sortableAfterSort = sortableQueryService.find(sortingCenter, barcode).orElseThrow();
        SortableLot lotAfterSort = sortableLotService.findByLotIdAndSortingCenter(lot.getLotId(), sortingCenter);
        Sortable sortableAfterSortLot = sortableQueryService.find(lotAfterSort.getSortableId()).orElseThrow();

        assertThat(sortableAfterSort.getStatus()).isEqualTo(SortableStatus.SORTED_DIRECT);
        assertThat(sortableAfterSort.getParent()).isNotNull();
        assertThat(lotAfterSort.getLotStatusOrNull()).isEqualTo(LotStatus.PROCESSING);
        assertThat(sortableAfterSortLot.getStatus()).isEqualTo(SortableStatus.KEEPED_DIRECT);

    }

    @DisplayName("success сортируем заказ в ячейку при возможности сортировки напрямую в лот")
    @Test
    void sortOrderToCellWithLotSortAvailable() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var place = testFactory.createForToday(
                        order(sortingCenter)
                                .deliveryService(deliveryServiceWithLotSortEnabled)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .accept()
                .getPlace();
        var route0 = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        var routeCell = route0.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, routeCell);
        var request = new SortableSortRequestDto(place, routeCell);
        caller.manualSortableBetaSort(request, String.valueOf(sortingCenter.getId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInCell(routeCell)));

        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @DisplayName("success сортируем заказ напрямую в лот без ячейки")
    @Test
    void sortOrderToLotWithoutCellWithLots() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var place = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .accept()
                .getPlace();
        var route = testFactory.findOutgoingRoute(place).orElseThrow();
        var routeCell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, routeCell);
        var request = new SortableSortRequestDto(place, lot);
        caller.manualSortableBetaSort(request, String.valueOf(sortingCenter.getId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @DisplayName("fail сортируем заказ из другого сц")
    @Test
    void sortOrderToLotFail() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var place = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .accept()
                .getPlace();
        var route = testFactory.findOutgoingRoute(place).orElseThrow();
        var routeCell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, routeCell);
        var sc = testFactory.storedSortingCenter(999L);
        var request = new SortableSortRequestDto(place, lot);
        caller.manualSortableBetaSort(request, String.valueOf(sc.getId()))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(error(400, "ORDER_FROM_ANOTHER_SC")));
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    void unlinkSortables() {
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkPallets("XDOC-1", "XDOC-2");

        UnlinkSortablesRequest request = UnlinkSortablesRequest.builder()
                .barcodes(List.of("XDOC-1", "XDOC-2"))
                .informationListCode("Зп-1")
                .sortingCenterId(flow.getSortingCenter().getId())
                .build();

        caller.unlinkSortables(request)
                .andExpect(status().isOk());

        assertThat(sortableQueryService.find(flow.getSortingCenter(), "XDOC-1")).isEmpty();
        assertThat(sortableQueryService.find(flow.getSortingCenter(), "XDOC-2")).isEmpty();
    }

    @Test
    void unlinkSortablesWithMeasurementsSo() {
        UnitMeasurementsDto unitMeasurementsDto1 = new UnitMeasurementsDto(
                new BigDecimal(120),
                new BigDecimal(150),
                new BigDecimal(80),
                new BigDecimal(200)
        );

        UnitMeasurementsDto unitMeasurementsDto2 = new UnitMeasurementsDto(
                new BigDecimal(120),
                new BigDecimal(60),
                new BigDecimal(80),
                new BigDecimal(100)
        );

        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkPallets("XDOC-1")
                .saveVgh(SaveVGHRequestDto.builder()
                                .sortableType(SortableType.XDOC_PALLET)
                                .vgh(unitMeasurementsDto1)
                                .build(),
                        "XDOC-1")
                .linkPallets("XDOC-2")
                .saveVgh(SaveVGHRequestDto.builder()
                                .sortableType(SortableType.XDOC_PALLET)
                                .vgh(unitMeasurementsDto2)
                                .build(),
                        "XDOC-2");

        UnlinkSortablesRequest request = UnlinkSortablesRequest.builder()
                .barcodes(List.of("XDOC-1", "XDOC-2"))
                .informationListCode("Зп-1")
                .sortingCenterId(flow.getSortingCenter().getId())
                .build();

        caller.unlinkSortables(request)
                .andExpect(status().isOk());

        assertThat(sortableQueryService.find(flow.getSortingCenter(), "XDOC-1")).isEmpty();
        assertThat(sortableQueryService.find(flow.getSortingCenter(), "XDOC-2")).isEmpty();
    }

    @Test
    void unlinkSortablesFailedWrongInfoListCode() {
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkPallets("XDOC-1");

        UnlinkSortablesRequest request = UnlinkSortablesRequest.builder()
                .barcodes(List.of("XDOC-1"))
                .informationListCode("Зп-2")
                .sortingCenterId(flow.getSortingCenter().getId())
                .build();

        caller.unlinkSortables(request)
                .andExpect(status().is4xxClientError());

        assertThat(sortableQueryService.find(flow.getSortingCenter(), "XDOC-1")).isNotEmpty();
    }

    @Test
    void unlinkSortablesFailedFixed() {
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkPallets("XDOC-1")
                .fixInbound();

        UnlinkSortablesRequest request = UnlinkSortablesRequest.builder()
                .barcodes(List.of("XDOC-1"))
                .informationListCode("Зп-1")
                .sortingCenterId(flow.getSortingCenter().getId())
                .build();

        caller.unlinkSortables(request)
                .andExpect(status().is4xxClientError());

        assertThat(sortableQueryService.find(flow.getSortingCenter(), "XDOC-1")).isNotEmpty();
    }

    @Test
    void unlinkSortablesSeveralInbounds() {
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-1")
                .build()
                .linkPallets("XDOC-1")
                .and()
                .inboundBuilder("in-2")
                .informationListBarcode("Зп-2")
                .build()
                .linkPallets("XDOC-2");

        UnlinkSortablesRequest request = UnlinkSortablesRequest.builder()
                .barcodes(List.of("XDOC-1", "XDOC-2"))
                .informationListCode("Зп-1")
                .sortingCenterId(flow.getSortingCenter().getId())
                .build();

        caller.unlinkSortables(request)
                .andExpect(status().is4xxClientError());

        assertThat(sortableQueryService.find(flow.getSortingCenter(), "XDOC-1")).isNotEmpty();
        assertThat(sortableQueryService.find(flow.getSortingCenter(), "XDOC-2")).isNotEmpty();
    }

    @Test
    @DisplayName("Ручка удаление sortable (используется для автотестов)")
    void deleteSortables() {
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-192929")
                .build()
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound();

        assertThat(
                sortableQueryService.findAllHavingAnyBarcode(sortingCenter, Set.of("XDOC-1", "XDOC-2"))
                        .stream().map(Sortable::getRequiredBarcodeOrThrow).toList()
        ).containsExactlyInAnyOrder("XDOC-1", "XDOC-2");

        caller.deleteSortables(new DeleteSortablesRequest(TestFactory.SC_ID, List.of("XDOC-1", "XDOC-2")))
                .andExpect(status().isOk());

        assertThat(
                sortableQueryService.findAllHavingAnyBarcode(sortingCenter, Set.of("XDOC-1", "XDOC-2"))
                        .stream().map(Sortable::getRequiredBarcodeOrThrow).toList()
        ).isEmpty();
    }

    @Test
    @DisplayName("Ручка отправки аномалий с РЦ - ошибка")
    void shipAnomaliesNotAnomalyBarcode() {
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-192929")
                .build()
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound();

        assertThat(
                sortableQueryService.findAllHavingAnyBarcode(sortingCenter, Set.of("XDOC-1", "XDOC-2"))
                        .stream().map(Sortable::getRequiredBarcodeOrThrow).toList()
        ).containsExactlyInAnyOrder("XDOC-1", "XDOC-2");

        caller.shipAnomalies(TestFactory.SC_ID, "XDOC-1", "XDOC-2")
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Ручка отправки аномалий с РЦ")
    void shipAnomalies() {
        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-192929")
                .build()
                .linkBoxes("AN-1", "AN-2")
                .fixInbound();

        assertThat(
                sortableQueryService.findAllHavingAnyBarcode(sortingCenter, Set.of("AN-1", "AN-2"))
                        .stream().map(Sortable::getRequiredBarcodeOrThrow).toList()
        ).containsExactlyInAnyOrder("AN-1", "AN-2");

        caller.shipAnomalies(TestFactory.SC_ID, "AN-1", "AN-2")
                .andExpect(status().isOk());
        assertThat(
                sortableQueryService.findAllHavingAnyBarcode(sortingCenter, Set.of("AN-1", "AN-2")).stream()
                        .filter(s -> s.getStatus() == SortableStatus.SHIPPED_DIRECT)
                        .map(Sortable::getRequiredBarcodeOrThrow)
                        .toList()
        ).containsExactlyInAnyOrder("AN-1", "AN-2");
    }

    @Test
    @DisplayName("Проверка отправки ивента для плейсов. type: DELETED_FROM_READY")
    void checkManualScEventPublishedForPlacesDelete() {
        testFactory.setConfiguration(ConfigurationProperties.DO_TPL_LES_ENABLED, true);
        SortingCenter dropoff = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(dropoff.getId(), SortingCenterPropertiesKey.IS_DROPOFF, true);

        var place = testFactory.createForToday(order(dropoff, "o3p")
                        .deliveryService(deliveryService).build()).accept().getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        var cell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var sortingCenterTo = testFactory.storedSortingCenter(place.getCourier().getDeliveryServiceId());
        var locationTo = Long.parseLong(Objects.requireNonNull(sortingCenterTo.getYandexId()));
        SortableLot lot = testFactory.storedLot(dropoff, cell, LotStatus.CREATED);
        var place1 = testFactory.createForToday(order(dropoff, "o1p")
                        .deliveryService(deliveryService).build())
                .accept().sort(cell.getId()).getPlace();
        testFactory.sortPlaceToLot(place1, lot, user);
        reset(sortableToSqsManager);

        ResendSortableInfoToSqsRequest request = new ResendSortableInfoToSqsRequest(
                List.of(place.getMainPartnerCode()), List.of()
        );
        caller.manualSendCargoEvent(request, place1.getSortingCenterId());

        verify(sortableToSqsManager).doCargoUnitStatus(argThat(arg -> {
            assertThat(arg)
                    .usingRecursiveComparison()
                    .ignoringFields("barcodes", "timestamp")
                    .isEqualTo(new SortableSortedOnDropoffEvent(
                            Long.parseLong(dropoff.getYandexId()),
                            locationTo,
                            List.of(),
                            SortableType.BOX,
                            DoCargoUnitStatusEventType.DELETED_FROM_READY));
            return true;
        }));
    }

    @Test
    @DisplayName("Проверка отправки ивента для плейсов. type: READY")
    void checkManualScEventPublishedForPlacesReady() {
        testFactory.setConfiguration(ConfigurationProperties.DO_TPL_LES_ENABLED, true);
        SortingCenter dropoff = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(dropoff.getId(), SortingCenterPropertiesKey.IS_DROPOFF, true);

        var place = testFactory.createForToday(order(dropoff, "o3pr")
                .deliveryService(deliveryService).build()).accept().getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        var cell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var sortingCenterTo = testFactory.storedSortingCenter(place.getCourier().getDeliveryServiceId());
        var locationTo = Long.parseLong(Objects.requireNonNull(sortingCenterTo.getYandexId()));
        SortableLot lot = testFactory.storedLot(dropoff, cell, LotStatus.CREATED);
        var placeReady1 = testFactory.createForToday(order(dropoff, "o1pr")
                        .deliveryService(deliveryService).build())
                .accept().sort(cell.getId()).getPlace();
        var placeReady2 = testFactory.createForToday(order(dropoff, "o2pr")
                        .deliveryService(deliveryService).build())
                .accept().sort(cell.getId()).getPlace();
        reset(sortableToSqsManager);

        ResendSortableInfoToSqsRequest request = new ResendSortableInfoToSqsRequest(
                List.of(placeReady1.getMainPartnerCode(), placeReady2.getMainPartnerCode()),
                List.of()
        );
        caller.manualSendCargoEvent(request, dropoff.getId());

        verify(sortableToSqsManager, times(2)).doCargoUnitStatus(argThat(arg -> {
            assertThat(arg)
                    .usingRecursiveComparison()
                    .ignoringFields("barcodes", "timestamp")
                    .isEqualTo(new SortableSortedOnDropoffEvent(
                            Long.parseLong(dropoff.getYandexId()),
                            locationTo,
                            List.of(),
                            SortableType.BOX,
                            DoCargoUnitStatusEventType.READY));
            return true;
        }));
    }

    private Outbound createOutbound() {
        return testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .logisticPointToExternalId(warehouse.getYandexId())
                .carNumber("A777MP77")
                .sortingCenter(sortingCenter)
                .toTime(Instant.now(clock))
                .fromTime(Instant.now(clock))
                .type(OutboundType.XDOC)
                .externalId("OUT-123")
                .build());
    }

    private Inbound createInbound(InboundType inboundType) {
        return testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundType(inboundType)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .nextLogisticPointId(warehouse.getYandexId())
                .confirmed(true)
                .build());
    }

    private void createRegistry(Outbound outbound, List<Sortable> sortables) {
        sortableTestFactory.createOutboundRegistry(SortableTestFactory.CreateOutboundRegistryParams.builder()
                .sortables(sortables)
                .sortingCenter(sortingCenter)
                .outboundExternalId(outbound.getExternalId())
                .registryExternalId("REG-123")
                .build());
    }

    private String sortedInCell(Cell cell) {
        return "{" +
                "\"destination\":{\"id\":\"" + cell.getId() + "\",\"name\":\"" + cell.getScNumber() + "\",\"type" +
                "\":\"CELL\"}," +
                "\"parentRequired\":false" +
                "}";
    }

    private String sortedInLot(SortableLot lot) {
        return "{" +
                "\"destination\":{\"id\":\"" + lot.getBarcode() + "\",\"name\":\"" + lot.getNameForApi() + "\",\"type" +
                "\":\"LOT\"}," +
                "\"parentRequired\":false" +
                "}";
    }

    private String error(int statusCode, String error) {
        return "{\"status\":" + statusCode + ",\"error\":\"" + error + "\"}";
    }

}
