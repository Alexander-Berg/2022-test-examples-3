package ru.yandex.market.sc.core.domain.lot.tm;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.lot.jdbc.LotJdbcRepository;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.jdbc.OrderJdbcRepository;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.place.jdbc.PlaceJdbcRepository;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.scan_log.repository.OrderScanLogEntryRepository;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertySource;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.external.tm.TmClient;
import ru.yandex.market.sc.core.external.tm.model.PutScStateRequest;
import ru.yandex.market.sc.core.external.tm.model.TmBag;
import ru.yandex.market.sc.core.external.tm.model.TmCenterType;
import ru.yandex.market.sc.core.external.tm.model.TmReturnBox;
import ru.yandex.market.sc.core.external.tm.model.TmSortable;
import ru.yandex.market.sc.core.external.tm.model.TmSortableLinkType;
import ru.yandex.market.sc.core.external.tm.model.TmSortableType;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
class PushReturnDropoffLotsToTmServiceTest {

    @Autowired
    private TestFactory testFactory;
    @Autowired
    private SortingCenterRepository sortingCenterRepository;
    @Autowired
    private SortingCenterPropertySource sortingCenterPropertySource;
    @Autowired
    private LotJdbcRepository lotJdbcRepository;
    @Autowired
    private OrderJdbcRepository orderJdbcRepository;
    @Autowired
    private PlaceJdbcRepository placeJdbcRepository;
    @Autowired
    private OrderScanLogEntryRepository orderScanLogEntryRepository;
    @Autowired
    private ScanService scanService;
    @Autowired
    private ConfigurationProvider configurationProvider;
    @MockBean
    private TmClient tmClient;
    private static final long UID = 124L;

    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    User user;

    PushStateToTmService pushReturnDropoffLotsToTmService;

    PushDoSortableToTmService pushDoSortableToTmService;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter(12);
        user = testFactory.getOrCreateStoredUser(sortingCenter);
        testFactory.setupMockClock(clock);
        pushReturnDropoffLotsToTmService = new PushStateToTmService(
                sortingCenterRepository,
                sortingCenterPropertySource,
                lotJdbcRepository,
                orderJdbcRepository,
                orderScanLogEntryRepository,
                tmClient
        );
        pushDoSortableToTmService = new PushDoSortableToTmService(
                sortingCenterRepository,
                sortingCenterPropertySource,
                configurationProvider,
                lotJdbcRepository,
                placeJdbcRepository,
                tmClient,
                clock
        );
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.PUSH_LOT_STATE_TO_TM, true);
        clearInvocations(tmClient);
    }

    @Test
    void pushScState() {
        Warehouse warehouse = testFactory.storedWarehouse("736819871", WarehouseType.DROPOFF);

        Cell cell = testFactory.storedCell(sortingCenter, CellType.RETURN, warehouse, "Nevikupi");

        OrderLike order1 = testFactory.createForToday(order(sortingCenter)
                        .externalId("o1")
                        .warehouseReturnId(warehouse.getYandexId())
                        .build())
                .accept().sort().ship().makeReturn().accept().sort(cell.getId()).get();
        OffsetDateTime order1Time = getAndIncreaseTime(60);


        OrderLike order2 = testFactory.createForToday(order(sortingCenter)
                        .externalId("o2")
                        .places("p1", "p2")
                        .warehouseReturnId(warehouse.getYandexId())
                        .build())
                .acceptPlaces().sortPlaces().ship().makeReturn().acceptPlaces().sortPlaces(cell.getId(), "p1").get();
        List<Place> places = testFactory.orderPlaces(order2.getId());
        OffsetDateTime order2Time = getAndIncreaseTime(120);

        SortableLot lot = testFactory.storedLot(
                sortingCenter,
                SortableType.PALLET,
                cell,
                LotStatus.PROCESSING,
                false);
        testFactory.sortOrderToLot(order1, lot, user);
        testFactory.sortPlaceToLot(places.get(0), lot, user);

        scanService.prepareToShipSortable(
                lot.getLotId(),
                SortableType.PALLET,
                SortableAPIAction.READY_FOR_SHIPMENT,
                new ScContext(user));
        OffsetDateTime lotTime = getAndIncreaseTime(120);

        PutScStateRequest actualRequest = pushReturnDropoffLotsToTmService.preparePushReturnRequest(sortingCenter.getId());
        PutScStateRequest expectedRequest = PutScStateRequest.ofBags(List.of(new TmBag(
                "SC_LOT_100000",
                736819871,
                lotTime,
                List.of(
                        new TmReturnBox("o1", "o1", order1Time),
                        new TmReturnBox("o2", "p1", order2Time)
                ),
                TmCenterType.SORTING_CENTER,
                "Nevikupi"
        )));
        sortBoxes(actualRequest);
        assertThat(actualRequest).isEqualTo(expectedRequest);
    }

    @Test
    @Transactional
    void pushDoState() {
        SortingCenter dropoff = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(dropoff.getId(), SortingCenterPropertiesKey.IS_DROPOFF, true);
        var order = testFactory.createForToday(order(dropoff, "o3").build())
                .accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(order).orElseThrow().allowReading();
        var cell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var sortingCenterTo = testFactory.storedSortingCenter(order.getCourier().getDeliveryServiceId());
        SortableLot lot = testFactory.storedLot(dropoff, cell, LotStatus.CREATED);
        SortableLot lot2 = testFactory.storedLot(dropoff, cell, LotStatus.CREATED);


        var order1 = testFactory.createForToday(order(dropoff, "o1").build())
                .accept().sort(cell.getId()).sortToLot(lot.getLotId()).get();
        testFactory.addStampToSortableLotAndPrepare(lot.getBarcode(), "simpleStamp1", user);
        var order2 = testFactory.createForToday(order(dropoff, "o2").build())
                .accept().sort(cell.getId()).sortToLot(lot2.getLotId()).get();
        testFactory.addStampToSortableLotAndPrepare(lot2.getBarcode(), "simpleStamp2", user);

        var now = LocalDateTime.now(clock);
        var locationTo = Long.parseLong(Objects.requireNonNull(sortingCenterTo.getYandexId()));
        PutScStateRequest actualRequest = pushDoSortableToTmService.prepareDropoffRequest(dropoff.getId(), now);
        PutScStateRequest expectedRequest = PutScStateRequest.ofSortables(List.of(
                new TmSortable(
                        "simpleStamp1",
                null,
                        Map.of(TmSortableLinkType.REFERENCE_ID.getValue(), "SC_LOT_100000"),
                        TmSortableType.LOT,
                        locationTo,
                        now
                ),
                new TmSortable(
                        "simpleStamp2",
                        null,
                        Map.of(TmSortableLinkType.REFERENCE_ID.getValue(), "SC_LOT_100001"),
                        TmSortableType.LOT,
                        locationTo,
                        now
                ),
                new TmSortable(
                        "o3",
                        null,
                        Map.of(TmSortableLinkType.REFERENCE_ID.getValue(), "o3"),
                        TmSortableType.BOX,
                        locationTo,
                        now
                )
        ));
        assertThat(actualRequest).isEqualTo(expectedRequest);
    }

    @Test
    @Transactional
    void pushDoStateWithShippedAlready() {
        SortingCenter dropoff = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(dropoff.getId(), SortingCenterPropertiesKey.IS_DROPOFF, true);
        var order = testFactory.createForToday(order(dropoff, "o3").build())
                .accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(order).orElseThrow().allowReading();
        var cell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var sortingCenterTo = testFactory.storedSortingCenter(order.getCourier().getDeliveryServiceId());
        SortableLot lot = testFactory.storedLot(dropoff, cell, LotStatus.CREATED);
        SortableLot lot2 = testFactory.storedLot(dropoff, cell, LotStatus.CREATED);


        var order1 = testFactory.createForToday(order(dropoff, "o1").build())
                .accept().sort(cell.getId()).sortToLot(lot.getLotId()).get();
        testFactory.addStampToSortableLotAndPrepare(lot.getBarcode(), "simpleStamp1", user);
        var order2 = testFactory.createForToday(order(dropoff, "o2").build())
                .accept().sort(cell.getId()).sortToLot(lot2.getLotId()).get();
        testFactory.addStampToSortableLotAndPrepare(lot2.getBarcode(), "simpleStamp2", user);
        testFactory.shipOrderRoute(order1);
        var now = LocalDateTime.now(clock);
        var locationTo = Long.parseLong(Objects.requireNonNull(sortingCenterTo.getYandexId()));
        PutScStateRequest actualRequest = pushDoSortableToTmService.prepareDropoffRequest(dropoff.getId(), now);
        PutScStateRequest expectedRequest = PutScStateRequest.ofSortables(List.of(
                new TmSortable(
                        "simpleStamp1",
                        null,
                        Map.of(TmSortableLinkType.REFERENCE_ID.getValue(), "SC_LOT_100000"),
                        TmSortableType.LOT,
                        locationTo,
                        now
                ),
                new TmSortable(
                        "simpleStamp2",
                        null,
                        Map.of(TmSortableLinkType.REFERENCE_ID.getValue(), "SC_LOT_100001"),
                        TmSortableType.LOT,
                        locationTo,
                        now
                ),
                new TmSortable(
                        "o3",
                        null,
                        Map.of(TmSortableLinkType.REFERENCE_ID.getValue(), "o3"),
                        TmSortableType.BOX,
                        locationTo,
                        now
                )
        ));
        assertThat(actualRequest).isEqualTo(expectedRequest);
    }

    // в реальности порядок не имеет значения - сортируем ради воспроизводимости теста
    private void sortBoxes(PutScStateRequest actualRequest) {
        for (TmBag bag : actualRequest.getBags()) {
            bag.getBoxes().sort(Comparator.comparing(TmReturnBox::getOrderId));
        }
    }

    @Test
    void shouldNotPushStateFromXdocSc() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, true);

        pushReturnDropoffLotsToTmService.pushState(sortingCenter);

        Mockito.verifyNoInteractions(tmClient);
    }

    private OffsetDateTime getAndIncreaseTime(int secondsToAdd) {
        Instant time = clock.instant();
        testFactory.setupMockClock(clock, clock.instant().plusSeconds(secondsToAdd));
        return OffsetDateTime.ofInstant(time, ZoneId.systemDefault());

    }
}
