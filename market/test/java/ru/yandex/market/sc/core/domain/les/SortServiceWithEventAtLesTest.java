package ru.yandex.market.sc.core.domain.les;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.scan.CargoUnitEventService;
import ru.yandex.market.sc.core.domain.scan.SortableToSqsManager;
import ru.yandex.market.sc.core.domain.scan.event.SortableSortedOnDropoffEvent;
import ru.yandex.market.sc.core.domain.scan.model.DoCargoUnitStatusEventType;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.external.sqs.SqsQueueProperties;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
public class SortServiceWithEventAtLesTest {

    private static final String QUEUE_NAME = "sc_out";
    private static final String SOURCE = "sc";


    @MockBean
    SortableToSqsManager sortableToSqsManager;

    @Autowired
    TestFactory testFactory;

    @Autowired
    SqsQueueProperties sqsQueueProperties;


    @Autowired
    CargoUnitEventService cargoUnitEventService;

    SortingCenter sortingCenter;
    Courier courier;
    DeliveryService deliveryService;
    User user;

    @BeforeEach
    void init() {
        Mockito.when(sqsQueueProperties.getOutQueue()).thenReturn(QUEUE_NAME);
        Mockito.when(sqsQueueProperties.getSource()).thenReturn(SOURCE);

        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        user = testFactory.storedUser(sortingCenter, 1L);
        courier = testFactory.storedCourier(12L, sortingCenter.getId());
        deliveryService = testFactory.storedDeliveryService(sortingCenter.getId().toString());
    }


    @Test
    void createCargoUnitStatusForPlaceEventFromDo() {
        SortingCenter dropoff = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(dropoff.getId(), SortingCenterPropertiesKey.IS_DROPOFF, true);

        var place = testFactory.createForToday(order(dropoff, "o3").build())
                .accept().sort().getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        route.allowNextRead();
        var cell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var sortingCenterTo = testFactory.storedSortingCenter(place.getCourier().getDeliveryServiceId());
        var locationTo = Long.parseLong(Objects.requireNonNull(sortingCenterTo.getYandexId()));

        SortableLot lot = testFactory.storedLot(dropoff, cell, LotStatus.CREATED);

        var place1 = testFactory.createForToday(order(dropoff, "o1").build())
                .accept().sort(cell.getId()).sortToLot(lot.getLotId()).getPlace();
        testFactory.addStampToSortableLotAndPrepare(lot.getBarcode(), "simpleStamp1", user);

        var event = cargoUnitEventService.createDoCargoEvent(place, null, cell, null);
        assertEqualsEvents(event, new SortableSortedOnDropoffEvent(
                Long.parseLong(dropoff.getYandexId()),
                locationTo,
                List.of(),
                SortableType.BOX,
                DoCargoUnitStatusEventType.READY));

        var event2 = cargoUnitEventService.createDoCargoEvent(place1, cell, null, lot);
        assertEqualsEvents(event2, new SortableSortedOnDropoffEvent(
                Long.parseLong(dropoff.getYandexId()),
                locationTo,
                List.of(),
                SortableType.BOX,
                DoCargoUnitStatusEventType.DELETED_FROM_READY));
    }

    @Test
    void createCargoUnitStatusForLotEventFromDo() {
        SortingCenter dropoff = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(dropoff.getId(), SortingCenterPropertiesKey.IS_DROPOFF, true);

        var place = testFactory.createForToday(order(dropoff, "o3").build())
                .accept().sort().getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        route.allowNextRead();
        var cell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var sortingCenterTo = testFactory.storedSortingCenter(place.getCourier().getDeliveryServiceId());
        var locationTo = Long.parseLong(Objects.requireNonNull(sortingCenterTo.getYandexId()));

        SortableLot lot = testFactory.storedLot(dropoff, cell, LotStatus.CREATED);

        var place1 = testFactory.createForToday(order(dropoff, "o1").build())
                .accept().sort(cell.getId()).sortToLot(lot.getLotId()).getPlace();
        testFactory.addStampToSortableLotAndPrepare(lot.getBarcode(), "simpleStamp1", user);

        var event = cargoUnitEventService.createDoCargoEvent(lot.getSortable(), cell, SortableAPIAction.ADD_STAMP);
        assertEqualsEvents(event, new SortableSortedOnDropoffEvent(
                Long.parseLong(dropoff.getYandexId()),
                locationTo,
                List.of(),
                lot.getType(),
                DoCargoUnitStatusEventType.READY));
    }


    @Test
    void checkScEventPublishedForPlaces() {
        testFactory.setConfiguration(ConfigurationProperties.DO_TPL_LES_ENABLED, true);
        SortingCenter dropoff = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(dropoff.getId(), SortingCenterPropertiesKey.IS_DROPOFF, true);

        var place = testFactory.createForToday(order(dropoff, "o3")
                        .deliveryService(deliveryService).build())
                .accept().sort().getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        route.allowNextRead();
        var cell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var sortingCenterTo = testFactory.storedSortingCenter(place.getCourier().getDeliveryServiceId());
        var locationTo = Long.parseLong(Objects.requireNonNull(sortingCenterTo.getYandexId()));
        SortableLot lot = testFactory.storedLot(dropoff, cell, LotStatus.CREATED);

        var place1 = testFactory.createForToday(order(dropoff, "o1")
                        .deliveryService(deliveryService).build())
                .accept().sort(cell.getId()).getPlace();
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
        Mockito.reset(sortableToSqsManager);
        testFactory.sortToLot(place1, lot, user);
        verify(sortableToSqsManager, times(1)).doCargoUnitStatus(argThat(arg -> {
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
    void checkScEventPublishedForLot() {
        testFactory.setConfiguration(ConfigurationProperties.DO_TPL_LES_ENABLED, true);
        SortingCenter dropoff = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(dropoff.getId(), SortingCenterPropertiesKey.IS_DROPOFF, true);

        var place = testFactory.createForToday(order(dropoff, "o3")
                        .deliveryService(deliveryService).build())
                .accept().sort().getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        route.allowNextRead();
        var cell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var sortingCenterTo = testFactory.storedSortingCenter(place.getCourier().getDeliveryServiceId());
        var locationTo = Long.parseLong(Objects.requireNonNull(sortingCenterTo.getYandexId()));
        SortableLot lot = testFactory.storedLot(dropoff, cell, LotStatus.CREATED);

        var place1 = testFactory.createForToday(order(dropoff, "o1")
                        .deliveryService(deliveryService).build())
                .accept().sort(cell.getId()).getPlace();
        testFactory.sortToLot(place1, lot, user);
        Mockito.reset(sortableToSqsManager);
        testFactory.addStampToSortableLotAndPrepare(lot.getBarcode(), "simpleStamp1", user);

        verify(sortableToSqsManager, times(1)).doCargoUnitStatus(argThat(arg -> {
            assertThat(arg)
                    .usingRecursiveComparison()
                    .ignoringFields("barcodes", "timestamp")
                    .isEqualTo(new SortableSortedOnDropoffEvent(
                            Long.parseLong(dropoff.getYandexId()),
                            locationTo,
                            List.of(),
                            SortableType.PALLET,
                            DoCargoUnitStatusEventType.READY));
            return true;
        }));
    }

    private void assertEqualsEvents(Optional<SortableSortedOnDropoffEvent> created,
                                    SortableSortedOnDropoffEvent expected) {
        assertThat(created.isPresent()).isTrue();
        assertThat(created.get()).isEqualToComparingOnlyGivenFields(expected,
                "logisticPointFrom", "logisticPointTo", "type", "eventType");
    }
}
