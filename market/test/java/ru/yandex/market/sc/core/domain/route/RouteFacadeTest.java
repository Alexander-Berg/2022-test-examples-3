package ru.yandex.market.sc.core.domain.route;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.application.monitoring.MonitoringUnit;
import ru.yandex.market.logistic.api.model.fulfillment.ReturnType;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.cell.model.ApiCellForRouteBaseDto;
import ru.yandex.market.sc.core.domain.cell.model.ApiCellForRouteDto;
import ru.yandex.market.sc.core.domain.cell.model.ApiPlaceInfoDto;
import ru.yandex.market.sc.core.domain.cell.model.CellDto;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.policy.OrderCellParams;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.courier.model.ApiCourierDto;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.location.repository.Location;
import ru.yandex.market.sc.core.domain.location.repository.LocationRepository;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.outbound.OutboundFacade;
import ru.yandex.market.sc.core.domain.outbound.OutboundQueryService;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundIdentifier;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundIdentifierType;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundRepository;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.route.model.ApiRouteListEntryDto;
import ru.yandex.market.sc.core.domain.route.model.ApiRouteStatus;
import ru.yandex.market.sc.core.domain.route.model.FinishRouteRequestDto;
import ru.yandex.market.sc.core.domain.route.model.OutgoingRouteBaseDto;
import ru.yandex.market.sc.core.domain.route.model.OutgoingRouteOrderStatus;
import ru.yandex.market.sc.core.domain.route.model.PartnerRouteParamsDto;
import ru.yandex.market.sc.core.domain.route.model.RouteAction;
import ru.yandex.market.sc.core.domain.route.model.RouteCategory;
import ru.yandex.market.sc.core.domain.route.model.RouteCreateRequest;
import ru.yandex.market.sc.core.domain.route.model.RouteDto;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishByCellsRequest;
import ru.yandex.market.sc.core.domain.route.model.RouteType;
import ru.yandex.market.sc.core.domain.route.model.TransferActIntervalDto;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.route_so.Routable;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.InMemorySortingCenterService;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.StockmanDto;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.model.ApiWarehouseDto;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.model.CourierDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty.NEED_TRANSPORT_BARCODE;
import static ru.yandex.market.sc.core.domain.route.model.OutgoingCourierRouteType.COURIER;
import static ru.yandex.market.sc.core.domain.route.model.OutgoingCourierRouteType.MAGISTRAL;
import static ru.yandex.market.sc.core.domain.route.model.RouteDocumentType.ALL;
import static ru.yandex.market.sc.core.domain.route.model.RouteDocumentType.NORMAL;
import static ru.yandex.market.sc.core.domain.route.model.RouteDocumentType.ONLY_CLIENT_RETURNS;
import static ru.yandex.market.sc.core.domain.route.model.RouteDocumentType.ONLY_DAMAGED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.HOURS_INTERVAL_FOR_TRANSFER_ACT_PRINT;
import static ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension.testNotMigrated;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.test.TestFactory.useNewSortableFlow;

/**
 * @author valter
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RouteFacadeTest {

    private final RouteFacade routeFacade;
    private final RouteCommandService routeCommandService;
    private final PlaceRepository placeRepository;
    private final LocationRepository locationRepository;
    private final TestFactory testFactory;
    private final ComplexMonitoring generalMonitoring;
    private final RouteMonitoringService routeMonitoringService;
    private final OutboundFacade outboundFacade;
    private final OutboundRepository outboundRepository;
    private final OutboundQueryService outboundQueryService;
    private final InMemorySortingCenterService inMemorySortingCenterService;
    private final TransactionTemplate transactionTemplate;

    @MockBean
    Clock clock;
    SortingCenter sortingCenter;
    SortingCenter scTarniy;
    Cell cell;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter(100L, "Новый СЦ");
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        scTarniy = testFactory.storedSortingCenter(75001L);
        cell = testFactory.storedCell(sortingCenter);
        user = testFactory.storedUser(sortingCenter, 123L);
        TestFactory.setupMockClockToSystemTime(clock);
    }

    @Test
    void getApiOutgoingRouteDtoMagistralCellWithOrdersIsEmpty() {
        var dsWithCourier = prepareScScOutboundReturnDsWithCourier(123);
        var cell = testFactory.storedCell(
                sortingCenter, "c1", CellType.COURIER, dsWithCourier.courier().getId());
        var order = testFactory.create(
                order(sortingCenter)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(dsWithCourier.deliveryService())
                        .externalId("o1")
                        .build()
        ).accept().sort(cell.getId()).get();
        var route = testFactory.findOutgoingCourierRoute(order).orElseThrow();
        route.revokeRouteReading();

        Routable routable = testFactory.getRoutable(route);
        assertThat(outboundQueryService.findMagistralRouteOutbounds(routable)).isNotEmpty();
        var dto = routeFacade.getApiOutgoingRouteDto(
                LocalDate.now(clock), testFactory.getRouteIdForSortableFlow(route), sortingCenter
        );
        assertThat(dto.getCells().get(0).empty()).isTrue();
    }

    @Test
    void getApiOutgoingRouteDtoMagistralCell() {
        var currentTime = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("UTC"));
        var fromTime = currentTime.minusHours(1);
        var toTime = currentTime.plusHours(24);

        TestFactory.setupMockClock(clock, currentTime.atZone(ZoneId.of("UTC")).toInstant());
        var dsWithCourier = prepareScScOutboundReturnDsWithCourier(123,
                ZonedDateTime.of(fromTime, ZoneId.of("UTC")).toInstant(),
                ZonedDateTime.of(toTime, ZoneId.of("UTC")).toInstant()
        );
        var order1 = testFactory.create(
                order(sortingCenter)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(dsWithCourier.deliveryService())
                        .externalId("o1")
                        .build()
        ).accept().get();
        var route1 = testFactory.findOutgoingCourierRoute(order1).orElseThrow().allowReading();
        assertThat(outboundQueryService.findMagistralRouteOutbounds(route1)).isNotEmpty();

        TestFactory.setupMockClock(clock, fromTime.plusHours(2).atZone(ZoneId.of("UTC")).toInstant());

        var order2 = testFactory.create(
                order(sortingCenter)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(dsWithCourier.deliveryService())
                        .externalId("o2")
                        .build()
        ).accept().get();
        var route2 = testFactory.findOutgoingCourierRoute(order2).orElseThrow();
        route2.revokeRouteReading();

        Routable routable2 = testFactory.getRoutable(route2);
        assertThat(outboundQueryService.findMagistralRouteOutbounds(routable2)).isNotEmpty();
    }

    record DsWithCourier(DeliveryService deliveryService, Courier courier) {

    }

    private DsWithCourier prepareScScOutboundReturnDsWithCourier(long scToId) {
        return prepareScScOutboundReturnDsWithCourier(scToId,
                clock.instant().minus(1, ChronoUnit.HOURS),
                clock.instant().plus(1, ChronoUnit.HOURS));
    }

    private DsWithCourier prepareScScOutboundReturnDsWithCourier(long scToId, Instant fromTime, Instant toTime) {
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES,
                true
        );
        testFactory.storedSortingCenterWithYandexId(scToId, String.valueOf(scToId));
        inMemorySortingCenterService.loadSync();

        testFactory.createOutbound("out1", OutboundStatus.CREATED, OutboundType.DS_SC, fromTime, toTime,
                String.valueOf(scToId), sortingCenter, null, String.valueOf(scToId));
        return new DsWithCourier(
                testFactory.storedDeliveryService(String.valueOf(scToId)),
                testFactory.storedCourierFromDs(scToId)
        );
    }

    @Test
    void getApiOutgoingRouteDtoMagistralCellWithNotReadyLotIsEmpty() {
        var dsWithCourier = prepareScScOutboundReturnDsWithCourier(124);
        var cell = testFactory.storedCell(
                sortingCenter, "c1", CellType.COURIER, dsWithCourier.courier().getId());
        var lot = testFactory.storedLot(sortingCenter, cell, LotStatus.CREATED);
        var order = testFactory.create(
                order(sortingCenter)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(dsWithCourier.deliveryService())
                        .externalId("o1")
                        .build()
        ).accept().sort(cell.getId()).sortToLot(lot.getLotId()).get();

        var route = testFactory.findOutgoingCourierRoute(order).orElseThrow();
        route.revokeRouteReading();

        Routable routable = testFactory.getRoutable(route);
        assertThat(outboundQueryService.findMagistralRouteOutbounds(routable)).isNotEmpty();
        var dto = routeFacade.getApiOutgoingRouteDto(
                LocalDate.now(clock), testFactory.getRouteIdForSortableFlow(route), sortingCenter
        );
        assertThat(dto.getCells().get(0).empty()).isTrue();
    }

    @Test
    void getApiOutgoingRouteDtoMagistralCellWithReadyLotIsNotEmpty() {
        var dsWithCourier = prepareScScOutboundReturnDsWithCourier(124);
        var cell = testFactory.storedCell(
                sortingCenter, "c1", CellType.COURIER, dsWithCourier.courier().getId());
        var lot = testFactory.storedLot(sortingCenter, cell, LotStatus.CREATED);
        var order = testFactory.create(
                order(sortingCenter)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(dsWithCourier.deliveryService())
                        .externalId("o1")
                        .build()
        ).accept().sort(cell.getId()).sortToLot(lot.getLotId()).prepareToShipLot(lot.getLotId()).get();

        var route = testFactory.findOutgoingCourierRoute(order).orElseThrow();
        route.revokeRouteReading();

        Routable routable = testFactory.getRoutable(route);
        assertThat(outboundQueryService.findMagistralRouteOutbounds(routable)).isNotEmpty();
        var dto = routeFacade.getApiOutgoingRouteDto(
                LocalDate.now(clock), testFactory.getRouteIdForSortableFlow(route), sortingCenter
        );
        assertThat(dto.getCells().get(0).empty()).isFalse();
    }

    @Test
    void getApiOutgoingRouteDtoMagistralCellsOrderedByLotNumber() {
        var dsWithCourier = prepareScScOutboundReturnDsWithCourier(124);
        var cell1 = testFactory.storedCell(sortingCenter, "c3", CellType.COURIER, dsWithCourier.courier().getId());
        var cell2 = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER, dsWithCourier.courier().getId());
        var cell3 = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER, dsWithCourier.courier().getId());
        var lot11 = testFactory.storedLot(sortingCenter, cell1, LotStatus.CREATED);
        var lot21 = testFactory.storedLot(sortingCenter, cell2, LotStatus.CREATED);
        var lot22 = testFactory.storedLot(sortingCenter, cell2, LotStatus.CREATED);
        var lot31 = testFactory.storedLot(sortingCenter, cell3, LotStatus.CREATED);
        var lot32 = testFactory.storedLot(sortingCenter, cell3, LotStatus.CREATED);

        var order = testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT)
                .deliveryService(dsWithCourier.deliveryService()).externalId("o11").build()
        ).accept().sortToLot(lot11.getLotId()).prepareToShipLot(lot11.getLotId()).get();
        testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT)
                .deliveryService(dsWithCourier.deliveryService()).externalId("o21").build()
        ).accept().sortToLot(lot21.getLotId()).prepareToShipLot(lot21.getLotId()).get();
        testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT)
                .deliveryService(dsWithCourier.deliveryService()).externalId("o22").build()
        ).accept().sortToLot(lot22.getLotId()).prepareToShipLot(lot22.getLotId()).get();
        testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT)
                .deliveryService(dsWithCourier.deliveryService()).externalId("o31").build()
        ).accept().sortToLot(lot31.getLotId()).prepareToShipLot(lot31.getLotId()).get();
        testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT)
                .deliveryService(dsWithCourier.deliveryService()).externalId("o32").build()
        ).accept().sortToLot(lot32.getLotId()).prepareToShipLot(lot32.getLotId()).get();

        var route = testFactory.findOutgoingCourierRoute(order).orElseThrow();
        route.revokeRouteReading();

        Routable routable = testFactory.getRoutable(route);
        assertThat(outboundQueryService.findMagistralRouteOutbounds(routable)).isNotEmpty();
        var dto = routeFacade.getApiOutgoingRouteDto(
                LocalDate.now(clock), testFactory.getRouteIdForSortableFlow(route), sortingCenter
        );

        assertThat(dto.getCells()).hasSize(3);

        assertThat(dto.getCells().get(0).lotCount()).isGreaterThanOrEqualTo(dto.getCells().get(1).lotCount());
        assertThat(dto.getCells().get(1).lotCount()).isGreaterThanOrEqualTo(dto.getCells().get(2).lotCount());

        assertThat(dto.getCells().get(0).number()).isLessThan(dto.getCells().get(1).number());
        assertThat(dto.getCells().get(1).number()).isLessThan(dto.getCells().get(2).number());
    }

    @Test
    void getOutgoingRouteWithMultiCellsReturnsLotInsideItsCell() {
        var deliveryService = testFactory.storedDeliveryService("123");
        var courier = testFactory.storedCourierFromDs(123);

        var cell1 = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER, courier.getId());
        var cell2 = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER, courier.getId());

        var lot1 = testFactory.storedLot(sortingCenter, cell1, LotStatus.CREATED);
        var lot2 = testFactory.storedLot(sortingCenter, cell2, LotStatus.CREATED);
        var lot3 = testFactory.storedLot(sortingCenter, cell2, LotStatus.CREATED);
        var lot4 = testFactory.storedLot(sortingCenter, cell2, LotStatus.CREATED);
        var lot5 = testFactory.storedLot(sortingCenter, cell2, LotStatus.CREATED);

        var order1 = testFactory.create(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).deliveryService(deliveryService)
                        .externalId("o1").build()
        ).accept().sort(cell1.getId()).sortToLot(lot1.getLotId()).get();
        var order2 = testFactory.create(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).deliveryService(deliveryService)
                        .externalId("o2").build()
        ).accept().sort(cell2.getId()).sortToLot(lot2.getLotId()).get();
        testFactory.create(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).deliveryService(deliveryService)
                        .externalId("o3").build()
        ).accept().sort(cell2.getId()).sortToLot(lot3.getLotId()).prepareToShipLot(lot3.getLotId()).get();
        testFactory.create(
                        order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).deliveryService(deliveryService)
                                .externalId("o4").build()
                ).accept().sort(cell2.getId()).sortToLot(lot4.getLotId())
                .prepareToShipLot(lot4.getLotId()).shipLot(lot4.getLotId()).get();
        var route = testFactory.findOutgoingCourierRoute(order1).orElseThrow().allowReading();
        assertThat(route).isEqualTo(testFactory.findOutgoingCourierRoute(order2).orElseThrow());

        var routeDto = routeFacade.getApiOutgoingRouteDto(
                LocalDate.ofInstant(clock.instant(), DateTimeUtil.DEFAULT_ZONE_ID),
                testFactory.getRouteIdForSortableFlow(route), sortingCenter);
        assertThat(routeDto.getCells()).hasSize(2);
        var cell1Dto = routeFacade.getCellForRoute(sortingCenter, routeDto.getCells().get(0).id(), routeDto.getId());
        assertThat(cell1Dto.getId()).isEqualTo(cell1.getId());
        assertThat(cell1Dto.getLotStatuses()).isEqualTo(Map.of(lot1.getBarcode(), LotStatus.PROCESSING));

        var cell2Dto = routeFacade.getCellForRoute(sortingCenter, routeDto.getCells().get(1).id(), routeDto.getId());
        assertThat(cell2Dto.getId()).isEqualTo(cell2.getId());
        assertThat(cell2Dto.getLotStatuses()).isEqualTo(
                Map.of(
                        lot2.getBarcode(), LotStatus.PROCESSING,
                        lot3.getBarcode(), LotStatus.READY,
                        lot5.getBarcode(), LotStatus.CREATED
                )
        );
    }

    @Test
    void finishRouteWithErrorFlushesMonitoring() {
        String monitoringName = routeMonitoringService.getRouteShipMonitoringName(
                RouteMonitoringService.RouteShipType.SHIP_ORDERS);
        // clear monitoring
        generalMonitoring.addTemporary(new MonitoringUnit(monitoringName), 1, TimeUnit.SECONDS);

        assertThat(generalMonitoring.getResult(monitoringName).getStatus()).isNotEqualTo(MonitoringStatus.CRITICAL);
        var place = testFactory.createOrderForToday(sortingCenter)
                .accept()
                .sort()
                .getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        TestFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));
        assertThatThrownBy(() -> routeFacade.finishOutgoingRoute(
                testFactory.getRouteIdForSortableFlow(route),
                new FinishRouteRequestDto(place.getExternalId(), place.getMainPartnerCode(),
                        place.getCellId().orElseThrow(), false, null, null), new ScContext(user))
        ).isInstanceOf(ScException.class);
        assertThat(generalMonitoring.getResult(monitoringName).getStatus()).isEqualTo(MonitoringStatus.CRITICAL);

        // clear monitoring
        generalMonitoring.addTemporary(new MonitoringUnit(monitoringName), 1, TimeUnit.SECONDS);
    }

    @Test
    void getApiRoutesOutgoingCourierMultiplace() {
        testFactory.createForToday(
                        order(sortingCenter).externalId("o1").places("o1p1", "o1p2").build()
                )
                .acceptPlaces("o1p1", "o1p2").sortPlaces("o1p1", "o1p2").ship().get();
        var order2 = testFactory.createForToday(
                        order(sortingCenter).externalId("o2").places("o2p1", "o2p2").build()
                )
                .acceptPlaces("o2p1", "o2p2").sortPlaces("o2p1", "o2p2").get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("o3").places("o3p1", "o3p2").build()
                )
                .acceptPlaces("o3p1").sortPlaces("o3p1").get();
        testFactory.createForToday(
                order(sortingCenter).externalId("o4").places("o4p1", "o4p2").build()
        ).get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("o5").places("o5p1", "o5p2").build()
                )
                .acceptPlaces("o5p1", "o5p2").sortPlaces("o5p1", "o5p2")
                .preparePlace("o5p1").preparePlace("o5p2").get();
        var route = testFactory.findOutgoingCourierRoute(order2).orElseThrow().allowReading();
        assertThat(routeFacade.getApiOutgoingRoutesList(LocalDate.now(clock), sortingCenter,
                RouteType.OUTGOING_COURIER)).isEqualTo(List.of(new ApiRouteListEntryDto(
                testFactory.getRouteIdForSortableFlow(route),
                Objects.requireNonNull(route.getCourierTo()).getName(),
                ApiRouteStatus.IN_PROGRESS,
                1,
                false
        )));
    }

    @Test
    void getApiRoutesListForCrossDockLot() {
        var cell = testFactory.storedCell(sortingCenter, CellType.COURIER, CellSubType.DEFAULT, null, "crossDockCell");
        var courier = testFactory.storedCourier(145123L, "Timur");
        var route = testFactory.storedOutgoingCourierRoute(LocalDate.now(clock), sortingCenter, courier, cell)
                                                                                                    .allowReading();
        var lot = testFactory.storedLot(sortingCenter, cell, LotStatus.READY, null, false, true);

        var apiOutgoingRoutesList = routeFacade.getApiOutgoingRoutesList(
                LocalDate.now(clock),
                sortingCenter,
                RouteType.OUTGOING_COURIER);
        assertThat(apiOutgoingRoutesList)
                .containsExactly(
                        new ApiRouteListEntryDto(
                                testFactory.getRouteIdForSortableFlow(route),
                                Objects.requireNonNull(route.getCourierTo()).getName(),
                                ApiRouteStatus.NOT_STARTED,
                                0,
                                false
                        )
                );
    }

    @Test
    void getApiRoutesListOutgoingCourier() {
        testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .accept().sort().ship().get();
        var order = testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("3").build()).get();
        testFactory.createForToday(order(sortingCenter).externalId("4").build()).cancel().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                    .orElseThrow().allowReading();
        assertThat(routeFacade.getApiOutgoingRoutesList(LocalDate.now(clock), sortingCenter,
                RouteType.OUTGOING_COURIER)).isEqualTo(List.of(new ApiRouteListEntryDto(
                testFactory.getRouteIdForSortableFlow(route),
                Objects.requireNonNull(route.getCourierTo()).getName(),
                ApiRouteStatus.IN_PROGRESS,
                1,
                false
        )));
    }

    @Test
    void needTransportBarcodeForDeliveryService() {
        var deliverService = testFactory.storedDeliveryService("1", false);
        var courier = testFactory.storedCourier(1L, 1L);
        var order = testFactory.createForToday(
                        order(sortingCenter).externalId("1")
                                .deliveryService(deliverService)
                                .build())
                .updateCourier(courier)
                .accept().sort().get();
        testFactory.setDeliveryServiceProperty(order.getDeliveryService(),
                NEED_TRANSPORT_BARCODE, String.valueOf(sortingCenter.getId()));
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                .orElseThrow().allowReading();
        assertThat(routeFacade.getApiOutgoingRoutesList(LocalDate.now(clock), sortingCenter,
                RouteType.OUTGOING_COURIER)).isEqualTo(List.of(new ApiRouteListEntryDto(
                testFactory.getRouteIdForSortableFlow(route),
                Objects.requireNonNull(route.getCourierTo()).getName(),
                ApiRouteStatus.FINISHED,
                1,
                true
        )));
    }

    @Test
    void getApiRoutesListOutgoingCourierMultiplace() {
        testFactory.createForToday(
                        order(sortingCenter).externalId("o1").places("o1p1", "o1p2").build()
                )
                .acceptPlaces("o1p1", "o1p2").sortPlaces("o1p1", "o1p2").ship().get();
        var order2 = testFactory.createForToday(
                        order(sortingCenter).externalId("o2").places("o2p1", "o2p2").build()
                )
                .acceptPlaces("o2p1", "o2p2").sortPlaces("o2p1", "o2p2").get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("o3").places("o3p1", "o3p2").build()
                )
                .acceptPlaces("o3p1").sortPlaces("o3p1").get();
        testFactory.createForToday(
                order(sortingCenter).externalId("o4").places("o4p1", "o4p2").build()
        ).get();
        var route = testFactory.findOutgoingCourierRoute(order2).orElseThrow().allowReading();
        assertThat(routeFacade.getApiOutgoingRoutesList(LocalDate.now(clock), sortingCenter,
                RouteType.OUTGOING_COURIER)).isEqualTo(List.of(new ApiRouteListEntryDto(
                testFactory.getRouteIdForSortableFlow(route),
                Objects.requireNonNull(route.getCourierTo()).getName(),
                ApiRouteStatus.IN_PROGRESS,
                1,
                false
        )));
    }

    // FIXME MARKETTPLSC-189 OK_PARTIAL
    @Test
    void getApiRoutesListOutgoingWarehouseMultiplace() {
        testFactory.createForToday(
                        order(sortingCenter).externalId("o1").places("o1p1", "o1p2").build()
                )
                .cancel().acceptPlaces("o1p1", "o1p2").sortPlaces("o1p1", "o1p2").ship().get();
        var order2 = testFactory.createForToday(
                        order(sortingCenter).externalId("o2").places("o2p1", "o2p2").build()
                )
                .cancel().acceptPlaces("o2p1", "o2p2").sortPlaces("o2p1", "o2p2").get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("o3").places("o3p1", "o3p2").build()
                )
                .cancel().acceptPlaces("o3p1").sortPlaces("o3p1").get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("o4").places("o4p1", "o4p2").build()
                )
                .cancel().acceptPlaces("o4p1").get();
        var route = testFactory.findOutgoingWarehouseRoute(order2).orElseThrow().allowReading();
        assertThat(routeFacade.getApiOutgoingRoutesList(LocalDate.now(clock), sortingCenter,
                RouteType.OUTGOING_WAREHOUSE)).isEqualTo(List.of(new ApiRouteListEntryDto(
                testFactory.getRouteIdForSortableFlow(route),
                Objects.requireNonNull(route.getWarehouseTo()).getIncorporation(),
                ApiRouteStatus.IN_PROGRESS,
                1,
                false
        )));
    }

    @Test
    void getApiRoutesListOutgoingWarehouseMultiplacePartial() {
        testFactory.createForToday(
                        order(sortingCenter).externalId("o1").places("o1p1", "o1p2").build()
                )
                .cancel().acceptPlaces("o1p1", "o1p2").sortPlaces("o1p1", "o1p2").ship().get();
        var order2 = testFactory.createForToday(
                        order(sortingCenter).externalId("o2").places("o2p1", "o2p2").build()
                )
                .cancel().acceptPlaces("o2p1", "o2p2").sortPlaces("o2p1", "o2p2").get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("o3").places("o3p1", "o3p2").build()
                )
                .cancel().acceptPlaces("o3p1").sortPlaces("o3p1").get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("o4").places("o4p1", "o4p2").build()
                )
                .cancel().acceptPlaces("o4p1").get();
        var route = testFactory.findOutgoingWarehouseRoute(order2).orElseThrow().allowReading();
        assertThat(routeFacade.getApiOutgoingRoutesList(LocalDate.now(clock), sortingCenter,
                RouteType.OUTGOING_WAREHOUSE)).isEqualTo(List.of(new ApiRouteListEntryDto(
                testFactory.getRouteIdForSortableFlow(route),
                Objects.requireNonNull(route.getWarehouseTo()).getIncorporation(),
                ApiRouteStatus.IN_PROGRESS,
                1,
                false
        )));
    }

    @Test
    void getApiRoutesListOutgoingWarehouseAllCellsEmpty() {
        var order = testFactory.createForToday(order(sortingCenter).externalId("2").build())
                .accept().sort().ship().makeReturn().accept().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                        .orElseThrow().allowReading();

        List<ApiRouteListEntryDto> apiOutgoingRoutesList = routeFacade.getApiOutgoingRoutesList(LocalDate.now(clock),
                sortingCenter,
                RouteType.OUTGOING_WAREHOUSE);

        ApiRouteListEntryDto expected = new ApiRouteListEntryDto(
                testFactory.getRouteIdForSortableFlow(route),
                Objects.requireNonNull(route.getWarehouseTo()).getIncorporation(),
                ApiRouteStatus.NOT_STARTED,
                1,
                false
        );

        assertThat(apiOutgoingRoutesList).isEqualTo(List.of(expected));
    }

    @Test
    void getApiRoutesListOutgoingWarehouseManyNonEmptyCells() {
        var cell1 = testFactory.storedCell(sortingCenter, "rc1", CellType.RETURN, "w1");
        var cell2 = testFactory.storedCell(sortingCenter, "rc2", CellType.RETURN, "w1");
        var order1 = testFactory.createForToday(order(sortingCenter).externalId("1").warehouseReturnId("w1").build())
                .accept().sort().ship().makeReturn().accept().sort(cell1.getId()).get();
        testFactory.createForToday(order(sortingCenter).externalId("2").warehouseReturnId("w1").build())
                .accept().sort().ship().makeReturn().accept().sort(cell2.getId()).get();
        var route = testFactory.findOutgoingWarehouseRoute(order1).orElseThrow().allowReading();
        assertThat(routeFacade.getApiOutgoingRoutesList(LocalDate.now(clock), sortingCenter,
                RouteType.OUTGOING_WAREHOUSE)).isEqualTo(List.of(new ApiRouteListEntryDto(
                testFactory.getRouteIdForSortableFlow(route),
                Objects.requireNonNull(route.getWarehouseTo()).getIncorporation(),
                ApiRouteStatus.FINISHED,
                2,
                false
        )));
    }

    @Test
    void searchApiOutgoingRoutesListCachedWarehouseCourierNotCached() {
        testFactory.setConfiguration(ConfigurationProperties.CACHED_API_ROUTE_LIST_WAREHOUSE, "true");
        var courier = testFactory.storedCourier(1L, "abc");

        assertThat(routeFacade.getApiOutgoingRoutesList(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_COURIER
        )).isEmpty();

        testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .updateCourier(courier).accept().sort().get();

        assertThat(routeFacade.getApiOutgoingRoutesList(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_COURIER
        )).hasSize(1);
    }

    @Test
    void searchApiOutgoingRoutesListCachedCourierWarehouseNotCached() {
        testFactory.setConfiguration(ConfigurationProperties.CACHED_API_ROUTE_LIST_COURIER, "true");
        var warehouse = testFactory.storedWarehouse("wh1");

        assertThat(routeFacade.getApiOutgoingRoutesList(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_WAREHOUSE
        )).isEmpty();

        testFactory.createForToday(
                order(sortingCenter).externalId("1").warehouseReturnId(warehouse.getYandexId()).build()
        ).cancel().accept().get();

        assertThat(routeFacade.getApiOutgoingRoutesList(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_WAREHOUSE
        )).hasSize(1);
    }

    @Test
    void searchApiOutgoingRoutesListCachedWarehouse() {
        testFactory.setConfiguration(ConfigurationProperties.CACHED_API_ROUTE_LIST_WAREHOUSE, "true");
        var warehouse = testFactory.storedWarehouse("wh1");

        assertThat(routeFacade.getApiOutgoingRoutesList(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_WAREHOUSE
        )).isEmpty();

        testFactory.createForToday(
                order(sortingCenter).externalId("1").warehouseReturnId(warehouse.getYandexId()).build()
        ).cancel().accept().get();

        assertThat(routeFacade.getApiOutgoingRoutesList(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_WAREHOUSE
        )).isEmpty();

        testFactory.invalidateMemcached();
        assertThat(routeFacade.getApiOutgoingRoutesList(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_WAREHOUSE
        )).hasSize(1);
    }

    @Test
    void searchApiOutgoingRoutesListCachedCourier() {
        testFactory.setConfiguration(ConfigurationProperties.CACHED_API_ROUTE_LIST_COURIER, "true");
        var courier = testFactory.storedCourier(1L, "abc");

        assertThat(routeFacade.getApiOutgoingRoutesList(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_COURIER
        )).isEmpty();

        testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .updateCourier(courier).accept().sort().get();

        assertThat(routeFacade.getApiOutgoingRoutesList(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_COURIER
        )).isEmpty();

        testFactory.invalidateMemcached();
        assertThat(routeFacade.getApiOutgoingRoutesList(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_COURIER
        )).hasSize(1);
    }

    @Test
    void getApiRoutesListOutgoingWarehouse() {
        testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .accept().sort().ship().accept().makeReturn().sort().ship().get();
        var order = testFactory.createForToday(order(sortingCenter).externalId("2").build())
                .accept().sort().ship().makeReturn().accept().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("3").build())
                .accept().sort().ship().makeReturn().get();
        testFactory.createForToday(order(sortingCenter).externalId("4").build())
                .accept().sort().ship().accept().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                .orElseThrow().allowReading();
        assertThat(routeFacade.getApiOutgoingRoutesList(LocalDate.now(clock), sortingCenter,
                RouteType.OUTGOING_WAREHOUSE)).isEqualTo(List.of(new ApiRouteListEntryDto(
                testFactory.getRouteIdForSortableFlow(route),
                Objects.requireNonNull(route.getWarehouseTo()).getIncorporation(),
                ApiRouteStatus.IN_PROGRESS,
                1,
                false
        )));
    }

    @Test
    void getApiRoutesListDoNotReturnEmptyOutgoingRoutes() {
        var courier1 = testFactory.storedCourier(1L);
        var courier2 = testFactory.storedCourier(2L);
        var order = testFactory.createOrder(sortingCenter)
                .updateCourier(courier1).updateShipmentDate(LocalDate.now(clock)).get();
        var route1 = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                            .orElseThrow().allowReading();

        order = testFactory.updateCourier(order, courier2);

        var route2 = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                            .orElseThrow().allowReading();
        assertThat(route1.getId()).isNotEqualTo(route2.getId());

        var routesList = routeFacade.getApiOutgoingRoutesList(LocalDate.now(clock), sortingCenter,
                RouteType.OUTGOING_COURIER);
        assertThat(routesList.size()).isEqualTo(1);
        Long routable2Id = testFactory.getRouteIdForSortableFlow(route2.getId());
        assertThat(routesList.get(0).getId()).isEqualTo(routable2Id);
    }

    /**
     * ApiRouteListEntryDto должен содержать shopId склада, если есть
     */
    @Test
    void getApiRoutesListOutgoingWarehouseHasShopId() {
        var order = testFactory.createForToday(
                order(scTarniy)
                        .externalId("o1")
                        .warehouseReturnId("10001699601")
                        .warehouseReturnType(ReturnType.SHOP)
                        .build()
        ).accept().cancel().get();
        testFactory.createForToday(
                order(scTarniy)
                        .externalId("o2")
                        .warehouseReturnId("10001699601")
                        .warehouseReturnType(ReturnType.SHOP)
                        .build()
        ).accept().get();
        var whReturn = order.getWarehouseReturn();
        String shopId = whReturn.getShopId();
        String incorporation = whReturn.getIncorporation();
        String expectedName = shopId + " " + incorporation;

        var routeListEntryDtos = routeFacade.getApiOutgoingRoutesList(
                LocalDate.now(clock),
                scTarniy,
                RouteType.OUTGOING_WAREHOUSE
        );
        assertThat(routeListEntryDtos.size()).isEqualTo(1);
        var routeListEntryDto = routeListEntryDtos.get(0);
        assertThat(routeListEntryDto.getOrdersCount()).isEqualTo(1L);
        assertThat(routeListEntryDto.getName()).isEqualTo(expectedName);
    }

    @Test
    void getRoutesOutgoingWarehouseWithClientReturn() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        var regularReturn1 = testFactory.create(order(sortingCenter)
                        .externalId("new order 1")
                        .shipmentDate(LocalDate.now(clock))
                        .warehouseReturnId(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId())
                        .build())
                .updateCourier(testFactory.storedCourier())
                .accept().sort().ship().makeReturn()
                .accept().sort().get();
        testFactory.create(order(sortingCenter)
                        .externalId("new order 2")
                        .shipmentDate(LocalDate.now(clock))
                        .warehouseReturnId(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId())
                        .build())
                .updateCourier(testFactory.storedCourier())
                .accept().sort().ship().makeReturn()
                .accept().sort().get();
        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";
        testFactory.createClientReturnForToday(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker
        ).accept().sort().get();
        var route = testFactory.findOutgoingWarehouseRoute(regularReturn1).orElseThrow();
        Long routeId;
        if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
            routeId = testFactory.getRouteSo(route).getId();
        } else {
            routeId = testFactory.getRouteIdForSortableFlow(route);
        }
        var routeDto = routeFacade.getApiOutgoingRouteDto(
                LocalDate.now(clock), routeId, sortingCenter);
        assertThat(routeDto.getCells().size()).isEqualTo(2);

        var cell1Dto = routeFacade.getCellForRoute(sortingCenter, routeDto.getCells().get(0).id(), routeDto.getId());
        assertThat(cell1Dto.getSubType()).isEqualTo(CellSubType.DEFAULT);
        assertThat(cell1Dto.getOrders().keySet()).isEqualTo(Set.of("new order 1", "new order 2"));

        var cell2Dto = routeFacade.getCellForRoute(sortingCenter, routeDto.getCells().get(1).id(), routeDto.getId());
        assertThat(cell2Dto.getSubType()).isEqualTo(CellSubType.CLIENT_RETURN);
        assertThat(cell2Dto.getOrders().keySet()).isEqualTo(Set.of(clientReturnLocker));

    }

    @Test
    public void getApiOutgoingRouteForCourierNotAllAccepted() {
        var courier = testFactory.storedCourier(11);
        testFactory.createForToday(
                        order(sortingCenter, "1").places("1", "2").build())
                .updateCourier(courier)
                .acceptPlaces("1")
                .sortPlaces("1")
                .get();
        var routable = routeFacade.getApiOutgoingRouteDtoForCourier(LocalDate.now(clock),
                sortingCenter,
                RouteType.OUTGOING_COURIER,
                courier.getId());
        long cellId = routable.getCells().get(0).id();
        ApiCellForRouteDto cellDto = routeFacade.getCellForRoute(sortingCenter, cellId,
                routable.getId());
        assertThat(cellDto.getAcceptedOrdersNotInCell()).isEqualTo(0L);
    }

    @Test
    public void getApiOutgoingRouteForCourierAllAcceptedNotAllSorted() {
        var courier = testFactory.storedCourier(11);
        testFactory.createForToday(
                        order(sortingCenter, "1").places("1", "2").build())
                .updateCourier(courier)
                .acceptPlaces("1", "2")
                .sortPlaces("1")
                .get();
        var route = routeFacade.getApiOutgoingRouteDtoForCourier(
                LocalDate.now(clock),
                sortingCenter,
                RouteType.OUTGOING_COURIER,
                courier.getId());
        ApiCellForRouteDto cell = routeFacade.getCellForRoute(
                sortingCenter,
                route.getCells().get(0).id(),
                testFactory.getRouteIdForSortableFlow(route.getId()));

        assertThat(cell.getAcceptedOrdersNotInCell()).isEqualTo(1L);
    }

    @Test
    public void getApiOutgoingRouteForCourierManyRoutesForOnlyOneCourier() {
        var anotherSortingCenter = testFactory.storedSortingCenter(777);
        sortingCenter = testFactory.storedSortingCenter(1L);
        var courier = testFactory.storedCourier(11);
        testFactory.create(order(sortingCenter).shipmentDate(LocalDate.now(clock))
                        .externalId("1").build())
                .updateCourier(courier).updateShipmentDate(LocalDate.now(clock));
        testFactory.create(order(anotherSortingCenter)
                        .shipmentDate(LocalDate.now(clock))
                        .externalId("2").build())
                .updateCourier(courier).updateShipmentDate(LocalDate.now(clock));

        var request1 = routeCreateRequest(RouteType.OUTGOING_COURIER, sortingCenter, courier);
        routeCommandService.createRouteIfNotExistsAndSetCell(request1);
        long routeId1 = routeCommandService.findRouteIdByRequest(request1).orElseThrow();
        var routableId1 = testFactory.getRouteIdForSortableFlow(routeId1);

        var request2 = routeCreateRequest(RouteType.OUTGOING_COURIER, anotherSortingCenter, courier);
        routeCommandService.createRouteIfNotExistsAndSetCell(request2);
        var routable = routeFacade.getApiOutgoingRouteDtoForCourier(
                LocalDate.now(clock),
                sortingCenter,
                RouteType.OUTGOING_COURIER,
                courier.getId());
        assertThat(routable.getId()).isEqualTo(routableId1);
    }

    @SuppressWarnings("SameParameterValue")
    private RouteCreateRequest routeCreateRequest(RouteType routeType, SortingCenter sortingCenter, Courier courier) {
        return new RouteCreateRequest(
                routeType,
                sortingCenter,
                LocalDate.now(clock),
                new LocalDateInterval(LocalDate.now(clock), LocalDate.now(clock)),
                LocalTime.now(clock),
                null,
                courier,
                cellParams(courier),
                null);
    }

    private OrderCellParams cellParams(Courier courier) {
        return testFactory.createForToday(order(sortingCenter).build())
                .updateCourier(courier).get();
    }

    @Test
    void getRoutesOutgoingWarehouseWithClientReturnMultiplace() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        var regularReturn1 = testFactory.create(order(sortingCenter)
                        .externalId("new order 1")
                        .places("1", "2")
                        .shipmentDate(LocalDate.now(clock))
                        .warehouseReturnId(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId())
                        .build())
                .updateCourier(testFactory.storedCourier())
                .acceptPlaces("1", "2").sortPlaces("1", "2").ship().makeReturn()
                .acceptPlaces("1", "2").sortPlaces("1", "2").get();
        testFactory.create(order(sortingCenter)
                        .externalId("new order 2")
                        .shipmentDate(LocalDate.now(clock))
                        .places("rg1", "rg2")
                        .warehouseReturnId(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId())
                        .build())
                .updateCourier(testFactory.storedCourier())
                .acceptPlaces("rg1", "rg2").sortPlaces("rg1", "rg2").ship().makeReturn()
                .acceptPlaces("rg1", "rg2").sortPlaces("rg1", "rg2").get();
        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";
        testFactory.createClientReturnForToday(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker
        ).accept().sort().get();
        var route = testFactory.findOutgoingWarehouseRoute(regularReturn1).orElseThrow();
        var routeDto = routeFacade.getApiOutgoingRouteDto(
                LocalDate.now(clock), testFactory.getRouteIdForSortableFlow(route), sortingCenter);
        assertThat(routeDto.getCells().size()).isEqualTo(2);

        var cell1Dto = routeFacade.getCellForRoute(sortingCenter, routeDto.getCells().get(0).id(), routeDto.getId());
        assertThat(cell1Dto.getSubType()).isEqualTo(CellSubType.DEFAULT);
        assertThat(cell1Dto.getOrders().keySet()).isEqualTo(Set.of("new order 1", "new order 2"));

        var cell2Dto = routeFacade.getCellForRoute(sortingCenter, routeDto.getCells().get(1).id(), routeDto.getId());
        assertThat(cell2Dto.getSubType()).isEqualTo(CellSubType.CLIENT_RETURN);
        assertThat(cell2Dto.getOrders().keySet()).isEqualTo(Set.of(clientReturnLocker));
    }

    @Test
    void placeInDifferentCellsOnReturnRoute() {
        var cell1 = testFactory.storedCell(sortingCenter, "rc1", CellType.RETURN, "w1");
        var cell2 = testFactory.storedCell(sortingCenter, "rc2", CellType.RETURN, "w1");
        var order = testFactory.createForToday(order(sortingCenter)
                        .externalId("1")
                        .places("p1", "p2")
                        .warehouseReturnId("w1")
                        .build())
                .acceptPlaces(List.of("p1", "p2"))
                .sortPlaces(List.of("p1", "p2"))
                .ship()
                .makeReturn()
                .acceptPlaces(List.of("p1", "p2"))
                .sortPlace("p1", cell1.getId())
                .sortPlace("p2", cell2.getId())
                .get();
        var route = testFactory.findPossibleOutgoingWarehouseRoute(order).orElseThrow();
        assertDoesNotThrow(() ->
                routeFacade.getApiOutgoingRouteDto(LocalDate.now(clock),
                        testFactory.getRouteIdForSortableFlow(route), sortingCenter));
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route), new ScContext(user),
                List.of(cell1.getId()),
                null,
                false
        ));
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route), new ScContext(user),
                List.of(cell2.getId()),
                null,
                false
        ));
        order = testFactory.getOrder(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        var places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places.stream().allMatch(place -> place.getStatus() == PlaceStatus.RETURNED)).isTrue();
    }

    @Disabled("MARKETTPLSC-528 Не работает привязка ячейки к маршруту только по привязке к складу")
    @Test
    void getApiRouteOutgoingWarehouseManyNonEmptyCells() {
        var cell1 = testFactory.storedCell(sortingCenter, "rc1", CellType.RETURN, "w1");
        var cell2 = testFactory.storedCell(sortingCenter, "rc2", CellType.RETURN, "w1");
        var order1 = testFactory.createForToday(order(sortingCenter).externalId("1").warehouseReturnId("w1").build())
                .accept().sort().ship().makeReturn().accept().sort(cell1.getId()).get();
        testFactory.createForToday(order(sortingCenter).externalId("2").warehouseReturnId("w1").build())
                .accept().sort().ship().makeReturn().accept().sort(cell2.getId()).get();
        var route = testFactory.findOutgoingWarehouseRoute(order1).orElseThrow();
        assertThat(routeFacade.getApiOutgoingRouteDto(LocalDate.now(clock), testFactory.getRouteIdForSortableFlow(route), sortingCenter))
                .isEqualTo(new OutgoingRouteBaseDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        null,
                        Objects.requireNonNull(route.getWarehouseTo()).getIncorporation(),
                        ApiRouteStatus.FINISHED,
                        List.of(
                                new ApiCellForRouteBaseDto(cell1, false, 0),
                                new ApiCellForRouteBaseDto(cell2, false, 0)
                        ),
                        0,
                        COURIER
                ));

        assertThat(routeFacade.getCellForRoute(sortingCenter, cell1.getId(),
                testFactory.getRouteIdForSortableFlow(route)))
                .isEqualTo(new ApiCellForRouteDto(
                        cell1,
                        Map.of("1", new ApiCellForRouteDto.OrderStatusWithReason(
                                OutgoingRouteOrderStatus.OK, ""
                        )), Collections.emptyMap(),
                        Collections.emptyMap(),
                        0L,
                        false
                ));

        assertThat(routeFacade.getCellForRoute(sortingCenter, cell2.getId(),
                testFactory.getRouteIdForSortableFlow(route)))
                .isEqualTo(new ApiCellForRouteDto(
                        cell2,
                        Map.of("2", new ApiCellForRouteDto.OrderStatusWithReason(
                                OutgoingRouteOrderStatus.OK, ""
                        )), Collections.emptyMap(),
                        Collections.emptyMap(),
                        0L,
                        false
                ));
    }

    @Test
    void doNotRequireReturnResortingCourierSimple() {
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        List<ApiCellForRouteBaseDto> routeCells = routeFacade.getApiOutgoingRouteDto(
                LocalDate.now(clock), testFactory.getRouteIdForSortableFlow(route), sortingCenter
        ).getCells();
        assertThat(routeCells).hasSize(1);
        var cell = routeFacade.getCellForRoute(sortingCenter, routeCells.get(0).id(),
                testFactory.getRouteIdForSortableFlow(route));
        assertThat(cell).matches(ApiCellForRouteDto::isCellPrepared);
    }

    @Test
    void doNotRequireReturnResortingWarehouseSimple() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ALWAYS_RESORT_RETURNS, "false");
        var order = testFactory.createOrderForToday(sortingCenter).cancel()
                .accept().sort().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        List<ApiCellForRouteBaseDto> routeCells = routeFacade.getApiOutgoingRouteDto(
                LocalDate.now(clock), testFactory.getRouteIdForSortableFlow(route), sortingCenter
        ).getCells();
        assertThat(routeCells).hasSize(1);
        var cell = routeFacade.getCellForRoute(sortingCenter, routeCells.get(0).id(),
                testFactory.getRouteIdForSortableFlow(route));
        assertThat(cell).matches(ApiCellForRouteDto::isCellPrepared);
    }

    @Test
    void requireReturnResortingWarehouseSimple() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ALWAYS_RESORT_RETURNS, "true");
        var order = testFactory.createOrderForToday(sortingCenter).cancel()
                .accept().sort().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        List<ApiCellForRouteBaseDto> routeCells = routeFacade.getApiOutgoingRouteDto(
                LocalDate.now(clock), testFactory.getRouteIdForSortableFlow(route), sortingCenter
        ).getCells();

        assertThat(routeCells).hasSize(1);
        var cell = routeFacade.getCellForRoute(sortingCenter, routeCells.get(0).id(),
                testFactory.getRouteIdForSortableFlow(route));
        assertThat(cell.isCellPrepared()).isFalse();
    }

    @Test
    void requireReturnResortingForReturnsWithProperty() {
        var order = testFactory.createOrderForToday(sortingCenter).cancel()
                .accept().sort().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ALWAYS_RESORT_RETURNS, "true");
        List<ApiCellForRouteBaseDto> routeCells = routeFacade.getApiOutgoingRouteDto(
                LocalDate.now(clock), testFactory.getRouteIdForSortableFlow(route), sortingCenter
        ).getCells();

        assertThat(routeCells).hasSize(1);
        var cell = routeFacade.getCellForRoute(sortingCenter, routeCells.get(0).id(),
                testFactory.getRouteIdForSortableFlow(route));
        assertThat(cell.isCellPrepared()).isFalse();
    }

    @Test
    void getOutgoingRouteWithMultiCellsOutgoingCourierAnotherScThrowsException() {
        var anotherSortingCenter = testFactory.storedSortingCenter(777);
        var order = testFactory.createForToday(order(anotherSortingCenter).externalId("1").build())
                .accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        assertThatThrownBy(() -> routeFacade.getApiOutgoingRouteDto(
                LocalDate.now(clock), testFactory.getRouteIdForSortableFlow(route), sortingCenter)
        ).isInstanceOf(ScException.class).hasMessage(ScErrorCode.ROUTE_FROM_ANOTHER_SC.getMessage());
    }

    @Test
    void getOutgoingRouteWithMultiCellsOutgoingCourier() {
        testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .accept().sort().ship().get();
        var order = testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("3").build()).accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("4").build()).cancel().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow()
                                                                                .allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        assertThat(testFactory.orderPlaces(order.getId())).hasSize(1);
        var place = testFactory.orderPlaces(order.getId()).stream().findFirst().orElseThrow();

        assertThat(routeFacade.getApiOutgoingRouteDto(LocalDate.now(clock),
                testFactory.getRouteIdForSortableFlow(route), sortingCenter))
                .isEqualTo(new OutgoingRouteBaseDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        new ApiCourierDto(
                                Objects.requireNonNull(route.getCourierTo()).getId(),
                                route.getCourierTo().getId(),
                                route.getCourierTo().getName(),
                                route.getCourierTo().getDeliveryServiceId()
                        ),
                        null,
                        ApiRouteStatus.IN_PROGRESS,
                        List.of(new ApiCellForRouteBaseDto(cell, false, 0)),
                        0,
                        COURIER
                ));

        assertThat(routeFacade.getCellForRoute(sortingCenter, cell.getId(),
                testFactory.getRouteIdForSortableFlow(route)))
                .isEqualTo(new ApiCellForRouteDto(cell, Map.of("2",
                        new ApiCellForRouteDto.OrderStatusWithReason(OutgoingRouteOrderStatus.OK, "")),
                        Map.of(
                                order.getExternalId(), Map.of(
                                        "2", new ApiPlaceInfoDto(place.getExternalId(), "2", true)
                                )
                        ),
                        Collections.emptyMap(), 1L, false));
    }

    @Test
    void getOutgoingRouteWithMultiCellsOutgoingCourierMultiplace() {
        testFactory.createForToday(
                        order(sortingCenter).externalId("o1").places("o1p1", "o1p2").build()
                )
                .acceptPlaces("o1p1", "o1p2").sortPlaces("o1p1", "o1p2").ship().get();
        var order2 = testFactory.createForToday(
                        order(sortingCenter).externalId("o2").places("o2p1", "o2p2").build()
                )
                .acceptPlaces("o2p1", "o2p2").sortPlaces("o2p1", "o2p2").get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("o3").places("o3p1", "o3p2").build()
                )
                .acceptPlaces("o3p1").sortPlaces("o3p1").get();
        testFactory.createForToday(
                order(sortingCenter).externalId("o4").places("o4p1", "o4p2").build()
        ).acceptPlaces("o4p1", "o4p2").get();
        var route = testFactory.findOutgoingCourierRoute(order2).orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order2);

        assertThat(routeFacade.getApiOutgoingRouteDto(LocalDate.now(clock),
                testFactory.getRouteIdForSortableFlow(route), sortingCenter))
                .isEqualTo(new OutgoingRouteBaseDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        new ApiCourierDto(
                                Objects.requireNonNull(route.getCourierTo()).getId(),
                                route.getCourierTo().getId(),
                                route.getCourierTo().getName(),
                                route.getCourierTo().getDeliveryServiceId()
                        ),
                        null,
                        ApiRouteStatus.IN_PROGRESS,
                        List.of(new ApiCellForRouteBaseDto(cell, false, 0)),
                        0,
                        COURIER
                ));

        assertThat(routeFacade.getCellForRoute(sortingCenter, cell.getId(),
                testFactory.getRouteIdForSortableFlow(route)))
                .isEqualTo(new ApiCellForRouteDto(cell, Map.of(
                        "o2", new ApiCellForRouteDto.OrderStatusWithReason(OutgoingRouteOrderStatus.OK, ""),
                        "o3", new ApiCellForRouteDto.OrderStatusWithReason(OutgoingRouteOrderStatus.DO_NOT_SHIP,
                                OutgoingRouteOrderStatus.REASON_CANT_BE_SHIPPED + " "
                                        + ScErrorCode.ORDER_IN_WRONG_STATUS)),
                        Map.of(
                                "o2", Map.of(
                                        "o2p1", new ApiPlaceInfoDto("o2p1", "o2", true),
                                        "o2p2", new ApiPlaceInfoDto("o2p2", "o2", true)
                                ),
                                "o3", Map.of(
                                        "o3p1", new ApiPlaceInfoDto("o3p1", "o3", true),
                                        "o3p2", new ApiPlaceInfoDto("o3p2", "o3", false)
                                )
                        ),
                        Collections.emptyMap(), 1L, true));
    }

    @Test
    void getOutgoingRouteWithMultiCellsOutgoingWarehouseMultiplacePartial() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ALWAYS_RESORT_RETURNS, "false");
        testFactory.createForToday(
                        order(sortingCenter).externalId("o1").places("o1p1", "o1p2").build()
                )
                .cancel().acceptPlaces("o1p1", "o1p2").sortPlaces("o1p1", "o1p2").ship().get();
        var order2 = testFactory.createForToday(
                        order(sortingCenter).externalId("o2").places("o2p1", "o2p2").build()
                )
                .cancel().acceptPlaces("o2p1", "o2p2").sortPlaces("o2p1", "o2p2").get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("o3").places("o3p1", "o3p2").build()
                )
                .cancel().acceptPlaces("o3p1").sortPlaces("o3p1").get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("o4").places("o4p1", "o4p2").build()
                )
                .cancel().acceptPlaces("o4p1").get();
        var route = testFactory.findOutgoingWarehouseRoute(order2).orElseThrow().allowReading();
        var cell2 = testFactory.determineRouteCell(route, order2);

        assertThat(routeFacade.getApiOutgoingRouteDto(LocalDate.now(clock),
                testFactory.getRouteIdForSortableFlow(route), sortingCenter))
                .isEqualTo(new OutgoingRouteBaseDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        null,
                        Objects.requireNonNull(route.getWarehouseTo()).getIncorporation(),
                        ApiRouteStatus.IN_PROGRESS,
                        List.of(new ApiCellForRouteBaseDto(cell2, false, 0)),
                        0,
                        COURIER
                ));

        assertThat(routeFacade.getCellForRoute(sortingCenter, cell2.getId(),
                testFactory.getRouteIdForSortableFlow(route)))
                .isEqualTo(new ApiCellForRouteDto(cell2,
                        Map.of("o2", new ApiCellForRouteDto.OrderStatusWithReason(OutgoingRouteOrderStatus.OK, ""),
                                "o3", new ApiCellForRouteDto.OrderStatusWithReason(OutgoingRouteOrderStatus.OK_PARTIAL, "")),
                        Map.of(
                                "o2", Map.of(
                                        "o2p1", new ApiPlaceInfoDto("o2p1", "o2", true),
                                        "o2p2", new ApiPlaceInfoDto("o2p2", "o2", true)
                                ),
                                "o3", Map.of(
                                        "o3p1", new ApiPlaceInfoDto("o3p1", "o3", true),
                                        "o3p2", new ApiPlaceInfoDto("o3p2", "o3", false)
                                )
                        ),
                        Collections.emptyMap(), 2L, false
                ));
    }

    @Test
    void getOutgoingRouteWithMultiCellsOutgoingWarehouseAllCellsEmpty() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ALWAYS_RESORT_RETURNS, "false");
        var place = testFactory.createForToday(order(sortingCenter).externalId("2").build())
                .accept().sort().ship().makeReturn().accept().getPlace();
        var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, place);

        assertThat(routeFacade.getApiOutgoingRouteDto(LocalDate.now(clock),
                testFactory.getRouteIdForSortableFlow(route), sortingCenter))
                .isEqualTo(new OutgoingRouteBaseDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        null,
                        Objects.requireNonNull(route.getWarehouseTo()).getIncorporation(),
                        ApiRouteStatus.NOT_STARTED,
                        List.of(new ApiCellForRouteBaseDto(cell, true, 0)),
                        0,
                        COURIER
                ));

        assertThat(routeFacade.getCellForRoute(sortingCenter, cell.getId(),
                testFactory.getRouteIdForSortableFlow(route)))
                .isEqualTo(new ApiCellForRouteDto(cell, Collections.emptyMap(), new HashMap<>(),
                        Collections.emptyMap(), 1L, false));

    }

    @Test
    void getOutgoingRouteWithMultiCellsRequireResortingForKeepedMultiplaceOrder() {
        var courier = testFactory.storedCourier();
        var order = testFactory.create(order(sortingCenter).externalId("o1").places("p1", "p2").build())
                .acceptPlaces("p1", "p2").keepPlaces("p1", "p2")
                .updateCourier(courier)
                .updateShipmentDate(LocalDate.now(clock).plusDays(1))
                .get();
        doReturn(Instant.now(clock).plus(1, ChronoUnit.DAYS)).when(clock).instant();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, order));
        List<Place> places = testFactory.orderPlaces(order);
        testFactory.acceptPlace(order, places.get(0).getYandexId());
        testFactory.sortPlace(order, places.get(0).getYandexId());
        String p1ExternalId = places.get(0).getMainPartnerCode();
        String p2ExternalId = places.get(1).getMainPartnerCode();
        assertThat(routeFacade.getApiOutgoingRouteDto(LocalDate.now(clock),
                testFactory.getRouteIdForSortableFlow(route), sortingCenter))
                .isEqualTo(new OutgoingRouteBaseDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        new ApiCourierDto(courier.getId(), courier.getId(), courier.getName(),
                                courier.getDeliveryServiceId()),
                        null,
                        ApiRouteStatus.NOT_STARTED,
                        List.of(new ApiCellForRouteBaseDto(cell, false, 0)),
                        0,
                        COURIER
                ));

        assertThat(routeFacade.getCellForRoute(sortingCenter, cell.getId(),
                testFactory.getRouteIdForSortableFlow(route)))
                .isEqualTo(new ApiCellForRouteDto(cell, Map.of("o1",
                        new ApiCellForRouteDto.OrderStatusWithReason(OutgoingRouteOrderStatus.DO_NOT_SHIP,
                                OutgoingRouteOrderStatus.REASON_CANT_BE_SHIPPED + " "
                                        + ScErrorCode.ORDER_IN_WRONG_STATUS)),
                        Map.of("o1", Map.of(p2ExternalId, new ApiPlaceInfoDto(p2ExternalId, "o1", false),
                                p1ExternalId, new ApiPlaceInfoDto(p1ExternalId, "o1", true))),
                        Collections.emptyMap(), 1L, true));
    }

    @Test
    void getOutgoingRouteWithMultiCellsOutgoingWarehouse() {
        testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .accept().sort().ship().accept().makeReturn().sort().ship().get();
        var order = testFactory.createForToday(order(sortingCenter).externalId("2").build())
                .accept().sort().ship().makeReturn().accept().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("3").build())
                .accept().sort().ship().makeReturn().accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("4").build())
                .accept().sort().ship().accept().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                    .orElseThrow().allowReading();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, order));
        assertThat(testFactory.orderPlaces(order.getId())).hasSize(1);
        var place = testFactory.orderPlaces(order.getId()).stream().findFirst().orElseThrow();

        assertThat(routeFacade.getApiOutgoingRouteDto(LocalDate.now(clock),
                testFactory.getRouteIdForSortableFlow(route), sortingCenter))
                .isEqualTo(new OutgoingRouteBaseDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        null,
                        Objects.requireNonNull(route.getWarehouseTo()).getIncorporation(),
                        ApiRouteStatus.IN_PROGRESS,
                        List.of(new ApiCellForRouteBaseDto(cell, false, 0)),
                        0,
                        COURIER
                ));

        assertThat(routeFacade.getCellForRoute(sortingCenter, cell.getId(),
                testFactory.getRouteIdForSortableFlow(route)))
                .isEqualTo(new ApiCellForRouteDto(cell, Map.of("2",
                        new ApiCellForRouteDto.OrderStatusWithReason(OutgoingRouteOrderStatus.OK, "")),
                        Map.of(
                                order.getExternalId(), Map.of(
                                        "2", new ApiPlaceInfoDto(place.getExternalId(), "2", true)
                                )
                        ),
                        Collections.emptyMap(), 1L, false));

    }

    @Test
    void ordersNotInCellDoesNotShowOrdersNotOnScDirectStream() {
        var order = testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, order));

        assertThat(routeFacade.getCellForRoute(sortingCenter, cell.getId(),
                testFactory.getRouteIdForSortableFlow(route)))
                .isEqualTo(new ApiCellForRouteDto(
                        cell,
                        Collections.emptyMap(),
                        new HashMap<>(),
                        Collections.emptyMap(),
                        0L,
                        false
                ));
    }

    @Test
    void ordersNotInCellShowsOrdersOnScDirectStream() {
        var order = testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .accept().get();
        testFactory.create(order(sortingCenter).externalId("2").build())
                .accept().keep()
                .updateCourier(Objects.requireNonNull(order.getCourier()))
                .updateShipmentDate(LocalDate.now(clock))
                .get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, order));

        assertThat(routeFacade.getCellForRoute(sortingCenter, cell.getId(),
                testFactory.getRouteIdForSortableFlow(route)))
                .isEqualTo(new ApiCellForRouteDto(
                        cell,
                        Collections.emptyMap(),
                        new HashMap<>(),
                        Collections.emptyMap(),
                        2L,
                        false
                ));
    }

    @Test
    void ordersNotInCellDoesNotShowOrdersNotOnScReturnStream() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ALWAYS_RESORT_RETURNS, "false");
        var order = testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .accept().sort().ship().makeReturn().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, order));

        assertThat(routeFacade.getCellForRoute(sortingCenter, cell.getId(),
                testFactory.getRouteIdForSortableFlow(route)))
                .isEqualTo(new ApiCellForRouteDto(
                        cell,
                        Collections.emptyMap(),
                        new HashMap<>(),
                        Collections.emptyMap(),
                        0L,
                        false
                ));
    }

    @Test
    void ordersNotInCellShowsOrdersOnScReturnStream() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ALWAYS_RESORT_RETURNS, "false");
        var place = testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .accept().makeReturn().getPlace();
        testFactory.create(order(sortingCenter).externalId("2").build())
                .accept().keep().makeReturn();
        var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, place));

        assertThat(routeFacade.getCellForRoute(sortingCenter, cell.getId(),
                testFactory.getRouteIdForSortableFlow(route)))
                .isEqualTo(new ApiCellForRouteDto(
                        cell,
                        Collections.emptyMap(),
                        new HashMap<>(),
                        Collections.emptyMap(),
                        2L,
                        false
                ));
    }

    @Test
    void getOutgoingRouteWithMultiCellsWhenFlagCalculateCellPreparedForLotEnabled() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.CALCULATE_CELL_PREPARED_FOR_LOT_ENABLED, Boolean.TRUE.toString());

        testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .accept().sort().ship();
        var place2 = testFactory.createForToday(order(sortingCenter).externalId("2").build())
                .accept().sort().getPlace();
        testFactory.createForToday(order(sortingCenter).externalId("3").build())
                .accept();
        testFactory.createForToday(order(sortingCenter).externalId("4").build())
                .cancel();
        var route = testFactory.findOutgoingCourierRoute(place2).orElseThrow().allowReading();
        var cl = testFactory.determineRouteCell(route, place2);
        SortableLot lot = testFactory.storedLot(sortingCenter, cl, LotStatus.CREATED);

        assertThat(routeFacade.getApiOutgoingRouteDto(LocalDate.now(clock),
                testFactory.getRouteIdForSortableFlow(route), sortingCenter))
                .isEqualTo(new OutgoingRouteBaseDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        new ApiCourierDto(
                                Objects.requireNonNull(route.getCourierTo()).getId(),
                                route.getCourierTo().getId(),
                                route.getCourierTo().getName(),
                                route.getCourierTo().getDeliveryServiceId()
                        ),
                        null,
                        ApiRouteStatus.IN_PROGRESS,
                        List.of(new ApiCellForRouteBaseDto(cl, false, 0)),
                        0,
                        COURIER
                ));

        assertThat(routeFacade.getCellForRoute(sortingCenter, cl.getId(),
                testFactory.getRouteIdForSortableFlow(route)))
                .isEqualTo(new ApiCellForRouteDto(
                        cl.getId(),
                        cl.getCellName().orElse(null),
                        CellStatus.NOT_ACTIVE,
                        CellType.COURIER,
                        CellSubType.DEFAULT,
                        null,
                        Map.of("2", OutgoingRouteOrderStatus.OK),
                        Map.of(
                                place2.getExternalId(), Map.of(
                                        "2", new ApiPlaceInfoDto(place2.getMainPartnerCode(), "2", true)
                                )
                        ),
                        Map.of("2", new ApiCellForRouteDto.OrderStatusWithReason(OutgoingRouteOrderStatus.OK, "")),
                        Map.of(Objects.requireNonNull(lot.getBarcode()), LotStatus.CREATED),
                        1L, false,
                        List.of(ApiCellForRouteDto.Action.SHIP_AND_RESORT)
                ));
    }

    @Test
    void getOutgoingRouteWithLots() {
        var place = testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .updateCourier(testFactory.storedCourier())
                .accept().sort().getPlace();
        var courier = place.getCourier();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        cell = testFactory.determineRouteCell(route, place);
        SortableLot lot1 = testFactory.storedLot(sortingCenter, cell, LotStatus.CREATED);
        SortableLot lot2 = testFactory.storedLot(sortingCenter, cell, LotStatus.PROCESSING);
        SortableLot lot3 = testFactory.storedLot(sortingCenter, cell, LotStatus.READY);
        SortableLot lot4 = testFactory.storedLot(sortingCenter, cell, LotStatus.SHIPPED);
        var lots = List.of(lot1, lot2, lot3, lot4);

        var orders = Map.of("1",
                new ApiCellForRouteDto.OrderStatusWithReason(OutgoingRouteOrderStatus.OK, ""));

        Map<String, LotStatus> lotStatuses = StreamEx.of(lots).toMap(SortableLot::getBarcode,
                sortableLot -> sortableLot.getOptLotStatus().orElse(LotStatus.SHIPPED));

        assertThat(routeFacade.getApiOutgoingRouteDto(LocalDate.now(clock),
                testFactory.getRouteIdForSortableFlow(route), sortingCenter))
                .isEqualTo(new OutgoingRouteBaseDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        new ApiCourierDto(
                                Objects.requireNonNull(courier).getId(),
                                courier.getId(),
                                courier.getName(),
                                courier.getDeliveryServiceId()),
                        null,
                        ApiRouteStatus.NOT_STARTED,
                        List.of(new ApiCellForRouteBaseDto(cell, false, 1)),
                        1,
                        COURIER
                ));

        assertThat(routeFacade.getCellForRoute(sortingCenter, cell.getId(),
                testFactory.getRouteIdForSortableFlow(route)))
                .isEqualTo(new ApiCellForRouteDto(
                        cell, orders,
                        Map.of(
                                place.getExternalId(), Map.of(
                                        "1", new ApiPlaceInfoDto(place.getMainPartnerCode(), "1", true)
                                )
                        ), lotStatuses, 0L, true));
    }

    @Test
    void getRoutesPageView() {
        getIncomingWarehouseRouteKeepOrder(() -> routeFacade.getRoutesPage(
                LocalDate.now(clock), sortingCenter, RouteType.INCOMING_WAREHOUSE,
                PageRequest.of(0, 100, Sort.DEFAULT_DIRECTION, "id"), null).getContent());
    }

    @Test
    void getRoutesPageViewByShopId() {
        getOutgoingWarehouseRouteSearchByShopId(shopId -> routeFacade.getRoutesPage(
                LocalDate.now(clock), scTarniy, RouteType.OUTGOING_WAREHOUSE,
                PageRequest.of(0, 100, Sort.DEFAULT_DIRECTION, "id"),
                PartnerRouteParamsDto.builder().recipientName(shopId).build()).getContent());
    }

    private void getOutgoingWarehouseRouteSearchByShopId(Function<String, List<RouteDto>> routesSupplier) {
        var order = testFactory.createForToday(
                order(scTarniy)
                        .externalId("o1")
                        .warehouseReturnId("10001699601")
                        .warehouseReturnType(ReturnType.SHOP)
                        .build()
        ).accept().cancel().get();
        testFactory.createForToday(
                order(scTarniy)
                        .externalId("o2")
                        .warehouseReturnId("10001699601")
                        .warehouseReturnType(ReturnType.SHOP)
                        .build()
        ).accept().get();
        var whReturn = order.getWarehouseReturn();
        String shopId = whReturn.getShopId();

        List<RouteDto> routes = routesSupplier.apply(shopId);
        assertThat(routes.size()).isEqualTo(1);
    }

    @Test
    void getOutgoingWarehouseRoutesPageViewByCategory() {
        getOutgoingWarehouseRouteFilterByCategory(category -> routeFacade.getRoutesPage(
                LocalDate.now(clock), scTarniy, RouteType.OUTGOING_WAREHOUSE,
                PageRequest.of(0, 100, Sort.DEFAULT_DIRECTION, "id"),
                PartnerRouteParamsDto.builder().category(category).build()).getContent());
    }

    private void getOutgoingWarehouseRouteFilterByCategory(Function<RouteCategory, List<RouteDto>> routesSupplier) {

        // order with shop id
        testFactory.createForToday(
                order(scTarniy)
                        .externalId("o1")
                        .warehouseReturnId("10001699601")
                        .warehouseReturnType(ReturnType.SHOP)
                        .build()
        ).accept().cancel().get();
        // order without shop id
        testFactory.createForToday(
                order(scTarniy)
                        .externalId("o2")
                        .build()
        ).accept().cancel().get();

        List<RouteDto> routes = routesSupplier.apply(null);
        assertThat(routes.size()).isEqualTo(2);

        List<RouteDto> warehouseRoutes = routesSupplier.apply(RouteCategory.WAREHOUSE);
        assertThat(warehouseRoutes.size()).isEqualTo(1);

        List<RouteDto> shopRoutes = routesSupplier.apply(RouteCategory.SHOP);
        assertThat(shopRoutes.size()).isEqualTo(1);
    }

    @Test
    void getIncomingWarehouseRoutesPageViewByCategory() {
        getIncomingWarehouseRouteFilterByCategory(category -> routeFacade.getRoutesPage(
                LocalDate.now(clock), scTarniy, RouteType.INCOMING_WAREHOUSE,
                PageRequest.of(0, 100, Sort.DEFAULT_DIRECTION, "id"),
                PartnerRouteParamsDto.builder().category(category).build()).getContent());
    }

    private void getIncomingWarehouseRouteFilterByCategory(Function<RouteCategory, List<RouteDto>> routesSupplier) {
        testFactory.setSortingCenterProperty(scTarniy, SortingCenterPropertiesKey.FILTER_INCOMING_ROUTES_ENABLED,
                "true");
        var warehouse = testFactory.storedWarehouse("warehouse-1", WarehouseType.SORTING_CENTER);
        var shop = testFactory.storedWarehouse("shop-1", WarehouseType.SHOP);

        // order not from dropoff
        testFactory.createForToday(
                order(scTarniy)
                        .externalId("o2")
                        .warehouseFromId(warehouse.getYandexId())
                        .build()
        ).accept().get();

        // order from dropoff
        testFactory.createForToday(
                order(scTarniy)
                        .externalId("o1")
                        .warehouseFromId(shop.getYandexId())
                        .build()
        ).accept().get();

        List<RouteDto> routes = routesSupplier.apply(null);
        assertThat(routes.size()).isEqualTo(2);

        List<RouteDto> warehouseRoutes = routesSupplier.apply(RouteCategory.WAREHOUSE);
        assertThat(warehouseRoutes.size()).isEqualTo(1);

        List<RouteDto> shopRoutes = routesSupplier.apply(RouteCategory.SHOP);
        assertThat(shopRoutes.size()).isEqualTo(1);
    }

    @Test
    void getOutgoingCourierRoutesPageViewByCategory() {
        getOutgoingCourierRouteFilterByCategory(category -> routeFacade.getRoutesPage(
                LocalDate.now(clock), scTarniy, RouteType.OUTGOING_COURIER,
                PageRequest.of(0, 100, Sort.DEFAULT_DIRECTION, "id"),
                PartnerRouteParamsDto.builder().category(category).build()).getContent());
    }

    private void getOutgoingCourierRouteFilterByCategory(Function<RouteCategory, List<RouteDto>> routesSupplier) {
        var courier = testFactory.storedCourier(1L);
        var deliverService = testFactory.storedDeliveryService("1", false);
        var middleMileCourier = testFactory.storedCourier(2L, 1L);

        // order for courier
        testFactory.createForToday(
                        order(scTarniy).externalId("o1")
                                .deliveryService(deliverService)
                                .build())
                .updateCourier(middleMileCourier)
                .accept().sort().get();
        // order for middle mile courier
        testFactory.createForToday(
                        order(scTarniy).externalId("o2").build())
                .updateCourier(courier)
                .accept().sort().get();

        List<RouteDto> routes = routesSupplier.apply(null);
        assertThat(routes.size()).isEqualTo(2);

        List<RouteDto> courierRoutes = routesSupplier.apply(RouteCategory.COURIER);
        assertThat(courierRoutes.size()).isEqualTo(1);

        List<RouteDto> middleMileRoutes = routesSupplier.apply(RouteCategory.MIDDLE_MILE_COURIER);
        assertThat(middleMileRoutes.size()).isEqualTo(1);
    }

    @Test
    void getIncomingWarehouseRouteKeepOrderView() {
        getIncomingWarehouseRouteKeepOrder(() -> routeFacade.getRoutes(
                LocalDate.now(clock), sortingCenter, RouteType.INCOMING_WAREHOUSE, Pageable.unpaged(), null));
    }

    private void getIncomingWarehouseRouteKeepOrder(Supplier<List<RouteDto>> routesSupplier) {
        testFactory.create(order(sortingCenter).externalId("3").build())
                .accept().keep()
                .updateCourier(Objects.requireNonNull(testFactory.courier()))
                .updateShipmentDate(LocalDate.now(clock).plusDays(1))
                .get();
        List<RouteDto> routes = routesSupplier.get();
        assertThat(routes.get(0).getOrdersInCell()).isEqualTo(1);
        assertThat(routes.get(0).getAcceptedButNotSorted()).isEqualTo(0);
    }

    @Test
    void sortToLotAssertOrderIsSortedInStatistics() {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }
        var deliveryService = testFactory.storedDeliveryService("345");
        var order = testFactory.createForToday(
                        order(sortingCenter)
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .accept().get();
        var courier = order.getCourier();
        var route0 = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = route0.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        testFactory.sortOrderToLot(order, lot, user);
        var routes = routeFacade.getRoutes(
                LocalDate.now(clock), sortingCenter, RouteType.INCOMING_WAREHOUSE,
                Pageable.unpaged(), null);
        assertThat(routes.get(0).getAcceptedButNotSorted()).isEqualTo(0);
    }

    @Test
    void getRoutesWithoutDeletedLots() {
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().accept().sort().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        Cell parentCell = testFactory.determineRouteCell(route, order);
        testFactory.storedLot(sortingCenter, parentCell, LotStatus.CREATED);
        testFactory.storedLot(sortingCenter, parentCell, LotStatus.PROCESSING);
        testFactory.storedLot(sortingCenter, parentCell, LotStatus.PROCESSING);
        var lotReady = testFactory.storedLot(sortingCenter, parentCell, LotStatus.READY);

        var routeDto = routeFacade.getRoutes(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_WAREHOUSE,
                Pageable.unpaged(), null).get(0);
        assertThat(Objects.requireNonNull(routeDto.getLotsSummaryStats()).getLotsProcessing()).isEqualTo(2L);
        assertThat(routeDto.getLotsSummaryStats().getLotsReady()).isEqualTo(1L);
        assertThat(routeDto.getActions()).contains(RouteAction.LOTS_SHIPMENT);

        testFactory.deleteLot(lotReady);
        routeDto = routeFacade.getRoutes(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_WAREHOUSE,
                Pageable.unpaged(), null).get(0);
        assertThat(Objects.requireNonNull(routeDto.getLotsSummaryStats()).getLotsProcessing()).isEqualTo(2L);
        assertThat(routeDto.getLotsSummaryStats().getLotsReady()).isEqualTo(0L);
        assertThat(routeDto.getActions()).isEmpty();
    }

    @Test
    void getIncomingWarehouseRouteReturnedView() {
        getIncomingWarehouseRouteReturned(() -> routeFacade.getRoutes(
                LocalDate.now(clock), sortingCenter, RouteType.INCOMING_WAREHOUSE, Pageable.unpaged(), null));
    }

    private void getIncomingWarehouseRouteReturned(Supplier<List<RouteDto>> routesSupplier) {
        testFactory.createForToday(order(sortingCenter).externalId("1").build()).accept().sort().ship()
                .accept().makeReturn().sort().ship().get();
        List<RouteDto> routes = routesSupplier.get();
        RouteDto routeDto = routes.get(0);
        assertThat(routeDto.getOrdersPlanned()).isEqualTo(1);
        assertThat(routeDto.getOrdersAccepted()).isEqualTo(1);
        assertThat(routeDto.getOrdersInCell()).isEqualTo(1);
        assertThat(routeDto.getAcceptedButNotSorted()).isEqualTo(0);
    }

    @Test
    void getIncomingWarehouseRouteCanceledView() {
        getIncomingWarehouseRouteCanceled(() -> routeFacade.getRoutes(
                LocalDate.now(clock), sortingCenter, RouteType.INCOMING_WAREHOUSE, Pageable.unpaged(), null));
    }

    private void getIncomingWarehouseRouteCanceled(Supplier<List<RouteDto>> routesSupplier) {
        testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .cancel().accept().makeReturn().sort().get();
        List<RouteDto> routes = routesSupplier.get();
        assertThat(routes.get(0).getOrdersPlanned()).isEqualTo(1);
        assertThat(routes.get(0).getOrdersAccepted()).isEqualTo(1);
        assertThat(routes.get(0).getOrdersInCell()).isEqualTo(1);
        assertThat(routes.get(0).getAcceptedButNotSorted()).isEqualTo(0);
    }

    @Test
    void getRoutesIncomingWarehouseView() {
        getRoutesIncomingWarehouse(() -> routeFacade.getRoutes(
                LocalDate.now(clock), sortingCenter, RouteType.INCOMING_WAREHOUSE, Pageable.unpaged(), null));
    }

    private void getRoutesIncomingWarehouse(Supplier<List<RouteDto>> routesSupplier) {
        getRoutesIncomingWarehouse(LocalDate.now(clock), routesSupplier);
    }

    @Test
    public void getTransferActIntervals() {
        testFactory.setSortingCenterProperty(sortingCenter, HOURS_INTERVAL_FOR_TRANSFER_ACT_PRINT, String.valueOf(6));
        var order = testFactory.createForToday(order(sortingCenter).externalId("o").build())
                .accept().sort().ship().makeReturn().get();
        testFactory.findPossibleIncomingCourierRoute(order).orElseThrow();
        var routes = routeFacade.getRoutes(LocalDate.now(clock), sortingCenter, RouteType.INCOMING_COURIER,
                Pageable.unpaged(), null);

        var transferActIntervals = routes.get(0).getTransferActIntervals();
        assertThat(transferActIntervals).isEqualTo(
                List.of(
                        new TransferActIntervalDto(LocalTime.of(0, 0), LocalTime.of(6, 0)),
                        new TransferActIntervalDto(LocalTime.of(6, 0), LocalTime.of(12, 0)),
                        new TransferActIntervalDto(LocalTime.of(12, 0), LocalTime.of(18, 0)),
                        new TransferActIntervalDto(LocalTime.of(18, 0), LocalTime.of(0, 0))
                )
        );
    }

    @Transactional
    public void getRoutesIncomingWarehouse(LocalDate date, Supplier<List<RouteDto>> routesSupplier) {
        TestFactory.setupMockClock(clock, LocalDateTime.of(date, LocalTime.of(12, 0))
                .toInstant(ZoneOffset.ofHours(+3)));
        var order1 = testFactory.createForToday(order(sortingCenter).externalId("1").build()).accept().sort().ship()
                .accept().makeReturn().sort().ship().get();
        var expectedStartedAt = Instant.now(clock);
        var expectedFinishedAt = expectedStartedAt.plus(1, ChronoUnit.HOURS);
        doReturn(expectedFinishedAt).when(clock).instant();
        testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().get();
        testFactory.create(order(sortingCenter).externalId("3").build())
                .accept().keep()
                .updateCourier(Objects.requireNonNull(order1.getCourier()))
                .updateShipmentDate(LocalDate.now(clock).plusDays(1))
                .get();
        testFactory.createForToday(order(sortingCenter).externalId("4").build()).get();
        testFactory.createForToday(order(sortingCenter).externalId("5").build()).cancel().get();
        testFactory.createForToday(order(sortingCenter).externalId("7").build())
                .accept().sort().prepare().get();

        var route = testFactory.findPossibleIncomingWarehouseRoute(order1).orElseThrow().allowReading();
        List<RouteDto> routes = routesSupplier.get();

        Warehouse warehouseFrom = route.getWarehouseFrom();
        Long locationId = Objects.requireNonNull(warehouseFrom).getLocation().getId();
        Location location = locationRepository.findById(locationId).orElseThrow();

        var address = Stream.of(location.getRegion(), location.getStreet(), location.getHouse())
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(", "));

        String incorporation = warehouseFrom.getIncorporation();
        if (StringUtils.isNotEmpty(address) && warehouseFrom.getType().equals(WarehouseType.DROPOFF)) {
            incorporation += " (" + address + ")";
        }

        assertThat(routes).usingElementComparatorIgnoringFields("dispatchPerson")
                .usingComparatorForType(
                        Comparator.comparing(instant -> instant.truncatedTo(ChronoUnit.MILLIS)), Instant.class
                )
                .isEqualTo(List.of(new RouteDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        date,
                        RouteType.INCOMING_WAREHOUSE,
                        ApiRouteStatus.IN_PROGRESS,
                        5L, 1L, 1L, 4L, 0L, 0L,
                        3L, 1L, 0L, 0L, false,
                        null,
                        Collections.emptyList(),
                        null,
                        new ApiWarehouseDto(Objects.requireNonNull(warehouseFrom).getId(),
                                incorporation,
                                warehouseFrom.getYandexId(),
                                warehouseFrom.getType()),
                        expectedStartedAt,
                        expectedFinishedAt,
                        null,
                        null,
                        Set.of(ALL),
                        null,
                        null,
                        null,
                        Collections.emptyList(),
                        new RouteDto.LotSummaryStatistics(),
                        null,
                        null
                )));
    }

    @Test
    void getOutgoingRouteMultiPlaceIncompleteInCellView() {
        getOutgoingRouteMultiPlaceIncompleteInCell(() -> routeFacade.getRoutes(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_COURIER, Pageable.unpaged(), null));
    }

    /**
     * Заказ считаем "неполным многоместным в ячейке" если он является многоместным и
     * есть посылки которые отсортированы в ячейку, но заказ не в статусе
     * ORDER_READY_TO_BE_SEND_TO_SO_FF/ORDER_PREPARED_TO_BE_SEND_TO_SO
     * т.е. часть посылок отсортирована, а другая часть нет
     */
    private void getOutgoingRouteMultiPlaceIncompleteInCell(Supplier<List<RouteDto>> routesSupplier) {
        testFactory.createForToday(order(sortingCenter).externalId("0").build()).cancel().get();
        testFactory.createForToday(order(sortingCenter).externalId("1").build()).get();
        testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("3").build()).accept().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("4").build()).accept().sort().ship().get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("5").places("o5p1", "o5p2").build())
                .acceptPlaces("o5p1").sortPlaces("o5p1")
                .get(); //этот
        testFactory.createForToday(
                        order(sortingCenter).externalId("6").places("o6p1", "o6p2").build())
                .acceptPlaces("o6p1", "o6p2").sortPlaces("o6p1", "o6p2")
                .get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("7").places("o7p1", "o7p2").build())
                .acceptPlaces("o7p2").sortPlaces("o7p2")
                .get(); //этот
        testFactory.createForToday(
                        order(sortingCenter).externalId("8").places("o8p1", "o8p2").build())
                .get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("9").places("o9p1", "o9p2").build())
                .acceptPlaces("o9p1", "o9p2")
                .get();
        List<RouteDto> routes = routesSupplier.get();
        RouteDto outgoingRoute = routes.get(0);
        assertThat(outgoingRoute.getMultiplaceIncompleteInCell()).isEqualTo(2);
        assertThat(outgoingRoute.getHasMultiplaceIncompleteInCell()).isEqualTo(true);
    }

    @Test
    void getOutgoingCourierRouteHasNoMultiPlaceIncompleteWhenOrderInBufferView() {
        getOutgoingCourierRouteHasNoMultiPlaceIncompleteWhenOrderInBuffer(() -> routeFacade.getRoutes(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_COURIER, Pageable.unpaged(), null));
    }

    /**
     * Многоместный заказ, который полностью попадает в хранение не должен учитываться как
     * неполный многоместный
     */
    private void getOutgoingCourierRouteHasNoMultiPlaceIncompleteWhenOrderInBuffer(
            Supplier<List<RouteDto>> routesSupplier) {
        testFactory.createForToday(order(sortingCenter).externalId("0").build()).cancel().get();
        testFactory.createForToday(order(sortingCenter).externalId("1").build()).get();
        testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("3").build()).accept().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("4").build()).accept().sort().ship().get();
        var baseOrder = testFactory.createForToday(
                        order(sortingCenter).externalId("5").places("o5p1", "o5p2").build())
                .acceptPlaces("o5p1").sortPlaces("o5p1")
                .get(); //этот
        testFactory.createForToday(
                        order(sortingCenter).externalId("6").places("o6p1", "o6p2").build())
                .acceptPlaces("o6p1", "o6p2").sortPlaces("o6p1", "o6p2")
                .get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("7").places("o7p1", "o7p2").build())
                .acceptPlaces("o7p2").sortPlaces("o7p2")
                .get(); //этот
        testFactory.createForToday(
                        order(sortingCenter).externalId("8").places("o8p1", "o8p2").build())
                .get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("9").places("o9p1", "o9p2").build())
                .acceptPlaces("o9p1", "o9p2")
                .get();

        var orderAwaiting = testFactory.createOrder(
                        order(sortingCenter).externalId("10").places("o10p1", "o10p2").build())
                .acceptPlaces("o10p1", "o10p2")
                .keepPlaces("o10p1", "o10p2")
                .get();
        testFactory.updateCourier(orderAwaiting, Objects.requireNonNull(baseOrder.getCourier()));
        testFactory.updateForTodayDelivery(orderAwaiting); //этот заказ не должен считаться как неполный многоместный
        List<RouteDto> routes = routesSupplier.get();
        RouteDto outgoingRoute = routes.get(0);
        assertThat(outgoingRoute.getMultiplaceIncompleteInCell()).isEqualTo(2);
        assertThat(outgoingRoute.getHasMultiplaceIncompleteInCell()).isEqualTo(true);
    }

    @Test
    void getRoutesOutgoingCourierView() {
        getRoutesOutgoingCourier(() -> routeFacade.getRoutes(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_COURIER, Pageable.unpaged(), null));
    }

    private void getRoutesOutgoingCourier(Supplier<List<RouteDto>> routesSupplier) {
        // если expectedStartedAt и expectedFinishedAt попадут на разные сутки, то результат будет другим
        TestFactory.setupMockClock(clock, Instant.now(clock).truncatedTo(ChronoUnit.DAYS));

        testFactory.createForToday(order(sortingCenter).externalId("1").build()).accept().sort().ship()
                .accept().makeReturn().sort().ship().get();
        var expectedStartedAt = Instant.now(clock);
        var expectedFinishedAt = expectedStartedAt.plus(1, ChronoUnit.HOURS);
        doReturn(expectedFinishedAt).when(clock).instant();
        testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().sort().ship().get();
        var order3 = testFactory.createForToday(order(sortingCenter).externalId("3").build()).accept().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("4").build()).accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("5").build()).get();
        testFactory.createForToday(order(sortingCenter).externalId("6").build()).cancel().get();
        testFactory.createForToday(order(sortingCenter).externalId("7").build())
                .accept().sort().prepare().get();

        var route = testFactory.findOutgoingCourierRoute(order3).orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order3);
        List<RouteDto> routes = routesSupplier.get();
        assertThat(routes).usingElementComparatorIgnoringFields("dispatchPerson")
                .usingComparatorForType(
                        Comparator.comparing(instant -> instant.truncatedTo(ChronoUnit.MILLIS)), Instant.class
                )
                .isEqualTo(List.of(new RouteDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        LocalDate.now(clock),
                        RouteType.OUTGOING_COURIER,
                        ApiRouteStatus.IN_PROGRESS,
                        6L, 4L, 1L, 1L, 2L, 2L,
                        0L, 1L, 3L, 0L, false,
                        new CellDto(
                                cell.getId(),
                                sortingCenter.getId(),
                                cell.getScNumber(),
                                CellStatus.NOT_ACTIVE,
                                CellType.COURIER,
                                false, false
                        ),
                        List.of(new CellDto(
                                cell.getId(),
                                sortingCenter.getId(),
                                cell.getScNumber(),
                                CellStatus.NOT_ACTIVE,
                                CellType.COURIER,
                                false, false
                        )),
                        testFactory.defaultCourier(),
                        null,
                        expectedStartedAt,
                        expectedFinishedAt,
                        null,
                        null,
                        Set.of(ALL),
                        null,
                        null,
                        null,
                        Collections.emptyList(),
                        new RouteDto.LotSummaryStatistics(),
                        null,
                        null
                )));
    }

    @Test
    void getRoutesOutgoingCourierAcceptedButNotShippedView() {
        getRoutesOutgoingCourierAcceptedButNotShipped(() -> routeFacade.getRoutes(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_COURIER, Pageable.unpaged(), null));
    }

    /**
     * В данном случае, принятыми заказами называется те, который сейчас
     * либо в статусе принято либо в статусе отсортировано
     */
    private void getRoutesOutgoingCourierAcceptedButNotShipped(Supplier<List<RouteDto>> routesSupplier) {
        testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().sort().shipPlace("2").get();
        testFactory.createForToday(order(sortingCenter).externalId("3").build()).accept().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("4").build()).accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("5").build()).get();
        testFactory.createForToday(order(sortingCenter).externalId("6").build()).cancel().get();
        testFactory.createForToday(order(sortingCenter).externalId("7").build()).accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("8").build()).accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("9").build()).accept().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("10").build()).accept().sort().shipPlace("10")
                .accept().makeReturn().sort().shipPlace("10").get();
        testFactory.createForToday(order(sortingCenter).externalId("11").build()).accept().sort().shipPlace("11")
                .accept().makeReturn().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("12").build()).accept().sort().shipPlace("12").get();
        testFactory.createForToday(order(sortingCenter).externalId("13").build()).accept().sort().shipPlace("13").get();
        List<RouteDto> routes = routesSupplier.get();
        assertThat(routes.get(0).getAcceptedButNotShipped()).isEqualTo(5);
    }

    @Test
    void getRoutesIncomingCourierView() {
        getRoutesIncomingCourier(() -> routeFacade.getRoutes(
                LocalDate.now(clock), sortingCenter, RouteType.INCOMING_COURIER, Pageable.unpaged(), null)
        );
    }

    private void getRoutesIncomingCourier(Supplier<List<RouteDto>> routesSupplier) {
        // если expectedStartedAt и expectedFinishedAt попадут на разные сутки, то результат будет другим
        TestFactory.setupMockClock(clock, Instant.now(clock).truncatedTo(ChronoUnit.DAYS));

        var order1 = testFactory.createForToday(order(sortingCenter).externalId("1").build()).accept().sort().ship()
                .accept().makeReturn().sort().ship().get();
        var expectedStartedAt = Instant.now(clock);
        var expectedFinishedAt = expectedStartedAt.plus(1, ChronoUnit.HOURS);
        doReturn(expectedFinishedAt).when(clock).instant();
        testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().sort().ship()
                .accept().makeReturn().get();
        testFactory.createForToday(order(sortingCenter).externalId("3").build()).accept().sort().ship()
                .accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("4").build()).accept().sort().ship()
                .makeReturn().accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("5").build()).accept().sort().ship()
                .makeReturn().get();
        testFactory.createForToday(order(sortingCenter).externalId("6").build()).cancel().get();
        testFactory.createForToday(order(sortingCenter).externalId("7").build())
                .accept().sort().prepare().get();

        var route = testFactory.findPossibleIncomingCourierRoute(order1).orElseThrow().allowReading();
        List<RouteDto> routes = routesSupplier.get();
        assertThat(routes).usingElementComparatorIgnoringFields("dispatchPerson",
                        "ordersInCell", "acceptedButNotSorted")
                .usingComparatorForType(
                        Comparator.comparing(instant -> instant.truncatedTo(ChronoUnit.MILLIS)), Instant.class
                )
                .isEqualTo(List.of(new RouteDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        LocalDate.now(clock),
                        RouteType.INCOMING_COURIER,
                        ApiRouteStatus.IN_PROGRESS,
                        5L, 1L, 0L, 4L, 0L, 1L,
                        1L, 2L, 0L, 0L, false,
                        null,
                        Collections.emptyList(),
                        testFactory.defaultCourier(),
                        null,
                        expectedStartedAt,
                        expectedFinishedAt,
                        null,
                        new StockmanDto(user.getId(), sortingCenter.getId(), user.getName(),
                                user.getEmail(), user.getRole(), false, null),
                        Set.of(ALL),
                        null,
                        null,
                        null,
                        Collections.emptyList(),
                        new RouteDto.LotSummaryStatistics(),
                        null,
                        null
                )));
    }

    @Test
    void getRoutesOutgoingWarehouseView() {
        getRoutesOutgoingWarehouse(() -> routeFacade.getRoutes(
                LocalDate.now(clock), sortingCenter, RouteType.OUTGOING_WAREHOUSE, Pageable.unpaged(), null));
    }

    private void getRoutesOutgoingWarehouse(Supplier<List<RouteDto>> routesSupplier) {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }

        testFactory.createForToday(order(sortingCenter).externalId("1").build()).accept().sort().ship()
                .accept().makeReturn().sort().ship().get();
        var expectedStartedAt = Instant.now(clock);
        var order2 = testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().sort().ship()
                .accept().makeReturn().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("4").build()).accept().sort().ship()
                .makeReturn().accept().sort().get();
        testFactory.createForToday(order(sortingCenter).externalId("3").build()).accept().sort().ship()
                .accept().makeReturn().get();
        testFactory.createForToday(order(sortingCenter).externalId("5").build()).accept().sort().ship()
                .makeReturn().get();
        testFactory.createForToday(order(sortingCenter).externalId("6").build()).accept().get();
        testFactory.createForToday(order(sortingCenter).externalId("7").build()).cancel().get();
        testFactory.createForToday(order(sortingCenter).externalId("8").build())
                .accept().sort().prepare().get();

        var route = testFactory.findOutgoingWarehouseRoute(order2).orElseThrow();
        var cell = Objects.requireNonNull(testFactory.determineRouteCell(route, order2));
        List<RouteDto> routes = routesSupplier.get();

        assertThat(routes).usingElementComparatorIgnoringFields("dispatchPerson", "acceptedButNotSorted")
                .usingComparatorForType(
                        Comparator.comparing(instant -> instant.truncatedTo(ChronoUnit.MILLIS)), Instant.class
                )
                .isEqualTo(List.of(new RouteDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        LocalDate.now(clock),
                        RouteType.OUTGOING_WAREHOUSE,
                        ApiRouteStatus.IN_PROGRESS,
                        5L, 4L, 0L, 1L, 2L, 1L,
                        0L, 1L, 0L, 2L, true,
                        new CellDto(
                                cell.getId(),
                                sortingCenter.getId(),
                                cell.getScNumber(),
                                CellStatus.NOT_ACTIVE,
                                CellType.RETURN,
                                false, false
                        ),
                        List.of(new CellDto(
                                cell.getId(),
                                sortingCenter.getId(),
                                cell.getScNumber(),
                                CellStatus.NOT_ACTIVE,
                                CellType.RETURN,
                                false, false
                        )),
                        null,
                        new ApiWarehouseDto(Objects.requireNonNull(route.getWarehouseTo()).getId(),
                                route.getWarehouseTo().getIncorporation(),
                                route.getWarehouseTo().getYandexId(),
                                route.getWarehouseTo().getType()),
                        expectedStartedAt,
                        expectedStartedAt,
                        null,
                        null,
                        Set.of(ALL, ONLY_CLIENT_RETURNS, NORMAL, ONLY_DAMAGED),
                        Set.of(ALL, ONLY_CLIENT_RETURNS, NORMAL, ONLY_DAMAGED),
                        Set.of(ALL, ONLY_CLIENT_RETURNS, NORMAL, ONLY_DAMAGED),
                        null,
                        Collections.emptyList(),
                        new RouteDto.LotSummaryStatistics(),
                        null,
                        null
                )));
    }


    @ParameterizedTest
    @DisplayName("Ячейка содержит только лоты любого типа кроме XDOC_BASKET")
    @EnumSource(value = SortableType.class, names = {"PALLET", "ORPHAN_PALLET"})
    void shipLotsWhenCellOnlyConsistsLots(SortableType lotType) {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        Cell c1 = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        Place p1 = testFactory.createForToday(order(sortingCenter, "o1").build())
                .accept().sort(c1.getId()).getPlace();
        Place p2 = testFactory.createForToday(order(sortingCenter, "o2").build())
                .accept().sort(c1.getId()).getPlace();
        SortableLot l1 = testFactory.storedLot(sortingCenter, lotType, c1);
        testFactory.sortPlaceToLot(p1, l1, user);
        testFactory.prepareToShipLot(l1);

        SortableLot l2 = testFactory.storedLot(sortingCenter, lotType, c1);
        testFactory.sortPlaceToLot(p2, l2, user);
        testFactory.prepareToShipLot(l2);

        var route = testFactory.findCellActiveRoute(c1.getId(), sortingCenter);

        routeFacade.finishOutgoingRoute(
                route.getId(),
                FinishRouteRequestDto.builder().cellId(c1.getId()).build(), new ScContext(user));
        p1 = testFactory.getPlace(p1.getId());
        p2 = testFactory.getPlace(p2.getId());
        l1 = testFactory.getLot(l1.getLotId());
        l2 = testFactory.getLot(l2.getLotId());
        var legacyRoute = testFactory.getRoute(route.route().getId());

        assertThat(p1.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(p1.getParent()).isNull();
        assertThat(l1.getParentCellId()).isNull();
        assertThat(l1.getOptLotStatus()).isEmpty();
        assertThat(l1.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);

        assertThat(p2.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(p2.getParent()).isNull();
        assertThat(l2.getParentCellId()).isNull();
        assertThat(l2.getOptLotStatus()).isEmpty();
        assertThat(l2.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);

        assertThat(legacyRoute.getRouteFinishes()).hasSize(1);
        assertThat(legacyRoute.getAllRouteFinishOrders()).hasSize(2);
        assertThat(legacyRoute.getAllRouteFinishOrders())
                .allMatch(o -> o.getStatus().equals(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF));
    }

    @Test
    @DisplayName("Ячейка содержит только заказы")
    void shipLotsWhenCellOnlyConsistsOrders() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        Cell c1 = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        Place p1 = testFactory.createForToday(order(sortingCenter, "o1").build())
                .accept().sort(c1.getId()).getPlace();
        Place p2 = testFactory.createForToday(order(sortingCenter, "o2").build())
                .accept().sort(c1.getId()).getPlace();

        var routable = testFactory.findCellActiveRoute(c1.getId(), sortingCenter);

        routeFacade.finishOutgoingRoute(
                routable.getId(),
                FinishRouteRequestDto.builder().cellId(c1.getId()).build(), new ScContext(user));
        p1 = testFactory.getPlace(p1.getId());
        p2 = testFactory.getPlace(p2.getId());

        assertThat(p1.getParent()).isNull();
        assertThat(p1.getCell()).isNull();
        assertThat(p1.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);

        assertThat(p2.getParent()).isNull();
        assertThat(p2.getCell()).isNull();
        assertThat(p2.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);

        transactionTemplate.execute( t -> {
            var routable2 = testFactory.findCellActiveRoute(c1.getId(), sortingCenter);

            assertThat(routable2.getRouteFinishes()).hasSize(1);
            assertThat(routable2.getAllRouteFinishOrders()).hasSize(2);
            assertThat(routable2.getAllRouteFinishOrders())
                    .allMatch(o -> o.getStatus().equals(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF));
            return null;
        });
    }

    @Test
    @DisplayName("Отгрузка маршрута для возвратного потока где заказы лежат в лоты и в ячейке")
    void shipLotsForReturnFlowWhenSomeOrdersInCell() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        Cell c1 = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        SortableLot l1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, c1);
        ScOrder o1 = testFactory.createForToday(order(sortingCenter, "o1").places("o1-1", "o1-2").build())
                .acceptPlaces().cancel()
                .acceptPlaces()
                .sortPlace("o1-1", c1.getId())
                .sortPlaceToLot(l1.getLotId(), "o1-2")
                .get();
        Place p1 = testFactory.orderPlace(o1, "o1-1");
        Place p2 = testFactory.orderPlace(o1, "o1-2");

        testFactory.prepareToShipLot(l1);
        var route = testFactory.findCellActiveRoute(c1.getId(), sortingCenter);

        routeFacade.finishOutgoingRoute(
                route.getId(),
                FinishRouteRequestDto.builder().cellId(c1.getId()).build(), new ScContext(user));

        p1 = testFactory.getPlace(p1.getId());
        l1 = testFactory.getLot(l1.getLotId());

        assertThat(p1.getCell()).isNull();
        assertThat(p1.getParent()).isNull();
        assertThat(p1.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);

        assertThat(l1.getParentCellId()).isNull();
        assertThat(l1.getOptLotStatus()).isEmpty();
        assertThat(l1.getStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);

        transactionTemplate.execute(ts -> {
            var r = testFactory.getRoute(route.getId());
            assertThat(r.getRouteFinishes()).hasSize(2);
            assertThat(r.getAllRouteFinishPlaces()).hasSize(2);
            assertThat(r.getAllRouteFinishPlaces())
                    .allMatch(p -> p.getSortableStatus() == SortableStatus.SHIPPED_RETURN);
            return null;
        });
    }

    @Test
    @DisplayName("Отгрузка лота через пересортировку по lot.id")
    void finishOutgoingRouteWhenShipLotResortByLotId() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, true);
        var deliveryService = testFactory.storedDeliveryService("123");
        Cell c1 = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        ScOrder o1 = testFactory.createForToday(order(sortingCenter, "o1")
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .places("o1-1", "o1-2").build())
                .acceptPlaces("o1-1", "o1-2")
                .sortPlaces("o1-1", "o1-2")
                .ship()
                .makeReturn()
                .acceptPlaces("o1-1", "o1-2")
                .sortPlaces("o1-1", "o1-2")
                .get();

        SortableLot l1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, c1);
        testFactory.sortPlaceToLot(testFactory.orderPlace(o1, "o1-1"), l1, user);
        testFactory.prepareToShipLot(l1);

        SortableLot l2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, c1);
        testFactory.sortPlaceToLot(testFactory.orderPlace(o1, "o1-2"), l2, user);
        testFactory.prepareToShipLot(l2);

        var route = testFactory.findCellActiveRoute(c1.getId(), sortingCenter);
        routeFacade.finishOutgoingRoute(
                route.getId(),
                FinishRouteRequestDto.builder().lotShippedExternalId(l1.getBarcode()).build(),
                new ScContext(user));

        l1 = testFactory.getLot(l1.getLotId());
        var p1 = testFactory.orderPlace(o1, "o1-1");

        assertThat(l1.getParentCellId()).isNull();
        assertLotAndPlaceState(l1, LotStatus.SHIPPED, p1, PlaceStatus.RETURNED);

        l2 = testFactory.getLot(l2.getLotId());
        var p2 = testFactory.orderPlace(o1, "o1-2");

        assertThat(l2.getParentCellId()).isEqualTo(c1.getId());
        assertLotAndPlaceState(l2, LotStatus.READY, p2, PlaceStatus.SORTED);
    }

    @Test
    @DisplayName("Отгрузка лота через пересортировку по barcode")
    void finishOutgoingRouteWhenShipLotResortByLotExternalId() {
        Cell c1 = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, true);
        var deliveryService = testFactory.storedDeliveryService("123");
        ScOrder o1 = testFactory.createForToday(order(sortingCenter, "o1")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(deliveryService)
                        .places("o1-1", "o1-2").build())
                .acceptPlaces("o1-1", "o1-2")
                .sortPlaces("o1-1", "o1-2")
                .ship()
                .makeReturn()
                .acceptPlaces("o1-1", "o1-2")
                .sortPlaces("o1-1", "o1-2")
                .get();

        SortableLot l1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, c1);
        testFactory.sortPlaceToLot(testFactory.orderPlace(o1, "o1-1"), l1, user);
        testFactory.prepareToShipLot(l1);

        SortableLot l2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, c1);
        testFactory.sortPlaceToLot(testFactory.orderPlace(o1, "o1-2"), l2, user);
        testFactory.prepareToShipLot(l2);

        var route = testFactory.findCellActiveRoute(c1.getId(), sortingCenter);
        routeFacade.finishOutgoingRoute(
                route.getId(),
                FinishRouteRequestDto.builder().lotShippedExternalId(l1.getBarcode()).build(),
                new ScContext(user));

        l1 = testFactory.getLot(l1.getLotId());
        var p1 = testFactory.orderPlace(o1, "o1-1");

        assertThat(l1.getParentCellId()).isNull();
        assertLotAndPlaceState(l1, LotStatus.SHIPPED, p1, PlaceStatus.RETURNED);

        l2 = testFactory.getLot(l2.getLotId());
        var p2 = testFactory.orderPlace(o1, "o1-2");

        assertThat(l2.getParentCellId()).isEqualTo(c1.getId());
        assertLotAndPlaceState(l2, LotStatus.READY, p2, PlaceStatus.SORTED);
    }

    @Test
    @DisplayName("Не отгружаем лоты других ячеек")
    void shipLotsOnlyFromSpecifiedCell() {
        Cell c1 = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        SortableLot l1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, c1);

        Place p1 = testFactory.createForToday(order(sortingCenter, "o1").build())
                .accept().sortToLot(l1.getLotId()).getPlace();

        Route route = testFactory.findOutgoingRoute(p1).orElseThrow();

        Cell c2 = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        SortableLot l2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, c2);
        testFactory.addRouteCell(route, c2, LocalDate.now(clock));

        Place p2 = testFactory.createForToday(order(sortingCenter, "o2").build())
                .accept().sortToLot(l2.getLotId()).getPlace();

        testFactory.prepareToShipLot(l1);
        testFactory.prepareToShipLot(l2);

        routeFacade.finishOutgoingRoute(
                testFactory.getRouteIdForSortableFlow(route),
                FinishRouteRequestDto.builder().cellId(c1.getId()).build(), new ScContext(user));
        p1 = testFactory.getPlace(p1.getId());
        p2 = testFactory.getPlace(p2.getId());

        assertThat(p1.getLot()).isNull();
        assertThat(p1.getCell()).isNull();
        assertThat(p1.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);

        assertThat(p2.getLot()).isNotNull();
        assertThat(p2.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);


        transactionTemplate.execute( t -> {
            Routable routable = testFactory.getRoutable(route);

            assertThat(routable.getRouteFinishes()).hasSize(1);
            assertThat(routable.getAllRouteFinishOrders()).hasSize(1);
            assertThat(routable.getAllRouteFinishOrders())
                    .allMatch(o -> o.getStatus().equals(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF));
            return null;
        });
    }

    @DisplayName("Тип печати АПП для маршрута")
    @Test
    void assertRouteTypeForPrint() {
        testFactory.setConfiguration("ENABLE_REDIRECT_TO_OUTBOUND_TRANSFER_ACT", true);
        var sortingCenter = testFactory.storedSortingCenter();
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(sortingCenter.getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order1 = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        testFactory.findOutgoingCourierRoute(order1);
        var route = testFactory.findOutgoingCourierRoute(order1).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order1);
        var lot = testFactory.storedLot(sortingCenter, cell, LotStatus.CREATED);
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        testFactory.sortToLot(order1, "11", lot, user);
        testFactory.sortToLot(order1, "12", lot, user);
        testFactory.prepareToShipLot(lot);

        // Нет заказов, отгруженных в рамках аутбаунда
        List<Long> routeIds = List.of(testFactory.getRouteIdForSortableFlow(route));
        var routeTransferActPrintInfoList =
                SortableFlowSwitcherExtension.useNewRouteSoStage2()
                        ? routeFacade.getTransferActPrintInfoListForRouteSo (routeIds)
                        : routeFacade.getTransferActPrintInfoList(routeIds);

        assertThat(routeTransferActPrintInfoList.size()).isEqualTo(1);
        assertThat(routeTransferActPrintInfoList.get(0).transferActPrintType()).isEqualTo(COURIER);

        var outboundIdentifier = new OutboundIdentifier(OutboundIdentifierType.EXTERNAL_ID, outbound.getExternalId());
        outboundFacade.bindLotToOutbound(outboundIdentifier, lot.getBarcode(),
                testFactory.getRouteIdForSortableFlow(route),
                new ScContext(user, sortingCenter));
        // Есть заказ, отгруженный в рамках аутбаунда, печатаем как магистральный маршрут
        routeTransferActPrintInfoList =
                SortableFlowSwitcherExtension.useNewRouteSoStage2()
                        ? routeFacade.getTransferActPrintInfoListForRouteSo (routeIds)
                        : routeFacade.getTransferActPrintInfoList(routeIds);
        assertThat(routeTransferActPrintInfoList.get(0).transferActPrintType()).isEqualTo(MAGISTRAL);

        // Есть шипнутый аутбаунд, но есть другой заказ, отгруженный не как магистральный (по старому флоу),
        // печатаем как курьерский маршрут
        var order2 = testFactory.createForToday(
                order(sortingCenter, "2")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sortPlaces().get();
        testFactory.shipOrderRoute(order2);
        routeTransferActPrintInfoList =
                SortableFlowSwitcherExtension.useNewRouteSoStage2()
                        ? routeFacade.getTransferActPrintInfoListForRouteSo (routeIds)
                        : routeFacade.getTransferActPrintInfoList(routeIds);

        assertThat(routeTransferActPrintInfoList.get(0).transferActPrintType()).isEqualTo(COURIER);
    }

    @Test
    @DisplayName("Отгружаем многоместный заказ попосылочно (с пересортировкой) на последней миле")
    void shipMultiplacePlaceByPlace() {
        ScOrder order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces()
                .sortPlaces()
                .get();

        Route route = testFactory.findOutgoingCourierRoute(order).orElseThrow();
        Cell cell = testFactory.findRouteCell(route, order).orElseThrow();

        assertDoesNotThrow(() -> routeFacade.finishOutgoingRoute(
                testFactory.getRouteIdForSortableFlow(route),
                new FinishRouteRequestDto(order.getExternalId(), "1", cell.getId(), false, null, null),
                new ScContext(user)
        ));
        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);

        assertDoesNotThrow(() -> routeFacade.finishOutgoingRoute(
                testFactory.getRouteIdForSortableFlow(route),
                new FinishRouteRequestDto(order.getExternalId(), "2", cell.getId(), false, null, null),
                new ScContext(user)
        ));
        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
    }

    @Test
    @DisplayName("Отгружаем многоместный заказ попосылочно (с пересортировкой) на последней миле [возвратный поток]")
    void shipMultiplacePlaceByPlace_ReturnFlow() {
        ScOrder order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces()
                .sortPlaces()
                .ship()
                .makeReturn()
                .acceptPlaces()
                .sortPlaces()
                .get();

        Route route = testFactory.findOutgoingWarehouseRoute(order).orElseThrow();
        Cell cell = testFactory.findRouteCell(route, order).orElseThrow();

        assertDoesNotThrow(() -> routeFacade.finishOutgoingRoute(
                testFactory.getRouteIdForSortableFlow(route),
                new FinishRouteRequestDto(order.getExternalId(), "1", cell.getId(), false, null, null),
                new ScContext(user)
        ));
        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        assertDoesNotThrow(() -> routeFacade.finishOutgoingRoute(
                testFactory.getRouteIdForSortableFlow(route),
                new FinishRouteRequestDto(order.getExternalId(), "2", cell.getId(), false, null, null),
                new ScContext(user)
        ));
        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
    }

    private void assertLotAndPlaceState(SortableLot lot, LotStatus ready, Place place, PlaceStatus sorted) {
        assertThat(lot.getLotStatus()).isEqualTo(ready);
        assertThat(place.getCell()).isNull();
        if (place.getStatus().getState().isOnSc()) {
            assertThat(place.getLot()).isNotNull();
        } else {
            assertThat(place.getLot()).isNull();
        }
        assertThat(place.getStatus()).isEqualTo(sorted);
    }

}
