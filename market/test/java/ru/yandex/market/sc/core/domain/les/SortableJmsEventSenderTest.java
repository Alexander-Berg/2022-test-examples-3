package ru.yandex.market.sc.core.domain.les;

import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.sc.CargoUnit;
import ru.yandex.market.logistics.les.sc.CargoUnitType;
import ru.yandex.market.logistics.les.sc.DoCargoUnitStatusEvent;
import ru.yandex.market.logistics.les.sc.DoUnitStatusEventType;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.external.sqs.SqsQueueProperties;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest

public class SortableJmsEventSenderTest {

    private static final String QUEUE_NAME = "sc_out";
    private static final String SOURCE = "sc";


    @MockBean
    JmsTemplate jmsTemplate;

    @Autowired
    TestFactory testFactory;

    @Autowired
    SqsQueueProperties sqsQueueProperties;


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
    void checkLesEventPublishedForPlaces() {
        testFactory.setConfiguration(ConfigurationProperties.DO_TPL_LES_ENABLED, true);
        SortingCenter dropoff = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(dropoff.getId(), SortingCenterPropertiesKey.IS_DROPOFF, true);

        var place = testFactory.createForToday(order(dropoff, "o3")
                        .deliveryService(deliveryService).build())
                .accept().sort().getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow().allowReading();
        var cell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var sortingCenterTo = testFactory.storedSortingCenter(place.getCourier().getDeliveryServiceId());
        var locationTo = Long.parseLong(Objects.requireNonNull(sortingCenterTo.getYandexId()));

        SortableLot lot = testFactory.storedLot(dropoff, cell, LotStatus.CREATED);

        var place1 = testFactory.createForToday(order(dropoff, "o1")
                        .deliveryService(deliveryService).build())
                .accept().sort(cell.getId()).sortToLot(lot.getLotId()).getPlace();
        ArgumentCaptor<Event> argumentForDo = ArgumentCaptor.forClass(Event.class);
        verify(jmsTemplate, Mockito.times(3))
                .convertAndSend(Mockito.eq(QUEUE_NAME), argumentForDo.capture());
        argumentForDo.getAllValues().forEach(event -> {
                    assertThat(event.getSource()).isEqualTo(SOURCE);
                    assertThat(event.getEventType()).isEqualTo("CARGO_UNIT_STATUS_FROM_DO");
                    assertThat(event.getPayload()).isInstanceOf(DoCargoUnitStatusEvent.class);
                }
        );
        assertThat(argumentForDo.getAllValues().size()).isEqualTo(3);
        assertThat(argumentForDo.getAllValues().get(0).getPayload())
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(new DoCargoUnitStatusEvent(
                        new CargoUnit("o3", "o3", CargoUnitType.BOX, Long.parseLong(dropoff.getYandexId()), locationTo),
                        0L,
                        DoUnitStatusEventType.READY));
        assertThat(argumentForDo.getAllValues().get(1).getPayload())
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(new DoCargoUnitStatusEvent(
                        new CargoUnit("o1", "o1", CargoUnitType.BOX, Long.parseLong(dropoff.getYandexId()), locationTo),
                        0L,
                        DoUnitStatusEventType.READY));
        assertThat(argumentForDo.getAllValues().get(2).getPayload())
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(new DoCargoUnitStatusEvent(
                        new CargoUnit("o1", "o1", CargoUnitType.BOX, Long.parseLong(dropoff.getYandexId()), locationTo),
                        0L,
                        DoUnitStatusEventType.DELETED_FROM_READY));

    }

    @Test
    void checkLesEventPublishedForLot() {
        testFactory.setConfiguration(ConfigurationProperties.DO_TPL_LES_ENABLED, true);
        SortingCenter dropoff = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(dropoff.getId(), SortingCenterPropertiesKey.IS_DROPOFF, true);

        var place = testFactory.createForToday(order(dropoff, "o3")
                        .deliveryService(deliveryService).build())
                .accept().sort().getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow().allowReading();
        var cell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var sortingCenterTo = testFactory.storedSortingCenter(place.getCourier().getDeliveryServiceId());
        var locationTo = Long.parseLong(Objects.requireNonNull(sortingCenterTo.getYandexId()));

        SortableLot lot = testFactory.storedLot(dropoff, cell, LotStatus.CREATED);

        var place1 = testFactory.createForToday(order(dropoff, "o1")
                        .deliveryService(deliveryService).build())
                .accept().sort(cell.getId()).sortToLot(lot.getLotId()).getPlace();
        Mockito.reset(jmsTemplate);
        testFactory.addStampToSortableLotAndPrepare(lot.getBarcode(), "simpleStamp", user);
        testFactory.deleteStamp(lot.getBarcode(), "simpleStamp", user);

        ArgumentCaptor<Event> argumentForDo = ArgumentCaptor.forClass(Event.class);
        verify(jmsTemplate, Mockito.times(2))
                .convertAndSend(Mockito.eq(QUEUE_NAME), argumentForDo.capture());
        argumentForDo.getAllValues().forEach(event -> {
                    assertThat(event.getSource()).isEqualTo(SOURCE);
                    assertThat(event.getEventType()).isEqualTo("CARGO_UNIT_STATUS_FROM_DO");
                    assertThat(event.getPayload()).isInstanceOf(DoCargoUnitStatusEvent.class);
                }
        );
        assertThat(argumentForDo.getAllValues().size()).isEqualTo(2);
        assertThat(argumentForDo.getAllValues().get(0).getPayload())
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(new DoCargoUnitStatusEvent(
                        new CargoUnit("simpleStamp", lot.getBarcode(), CargoUnitType.LOT,
                                Long.parseLong(dropoff.getYandexId()), locationTo),
                        0L,
                        DoUnitStatusEventType.READY));
        assertThat(argumentForDo.getAllValues().get(1).getPayload())
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(new DoCargoUnitStatusEvent(
                        new CargoUnit("simpleStamp", lot.getBarcode(), CargoUnitType.LOT,
                                Long.parseLong(dropoff.getYandexId()), locationTo),
                        0L,
                        DoUnitStatusEventType.DELETED_FROM_READY));
    }
}
