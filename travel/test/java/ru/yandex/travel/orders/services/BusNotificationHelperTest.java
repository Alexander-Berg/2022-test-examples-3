package ru.yandex.travel.orders.services;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.bus.model.BusPointInfo;
import ru.yandex.travel.bus.model.BusReservation;
import ru.yandex.travel.bus.model.BusRide;
import ru.yandex.travel.bus.model.BusesOrder;
import ru.yandex.travel.bus.model.BusesTicket;
import ru.yandex.travel.orders.entities.BusOrderItem;
import ru.yandex.travel.orders.repository.NotificationRepository;
import ru.yandex.travel.orders.services.buses.BusNotificationHelper;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflows.orderitem.bus.BusProperties;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class BusNotificationHelperTest {
    private BusNotificationHelper busNotificationHelper;
    private BusProperties busProperties;
    private WorkflowRepository workflowRepository;
    private NotificationRepository notificationRepository;
    private UrlShortenerService urlShortenerService;

    @Before
    public void setUp() {
        busProperties = new BusProperties();
        workflowRepository = mock(WorkflowRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        urlShortenerService = mock(UrlShortenerService.class);
        busNotificationHelper = new BusNotificationHelper(busProperties, workflowRepository, notificationRepository,
                urlShortenerService);
    }

    @Test
    public void testBuildGenericRouteTitle() {
        BusOrderItem orderItem1 = createItem();
        BusRide ride1 = orderItem1.getPayload().getRide();
        ride1.getTitlePointFrom().setTitle("Station1");
        ride1.getTitlePointFrom().setPointKey("s1");
        ride1.getTitlePointTo().setSupplierDescription("Station2");
        ride1.getTitlePointTo().setPointKey("s2");
        var s = busNotificationHelper.buildRouteTitle(List.of(orderItem1));
        assertThat(s).isEqualTo("Station1 — Station2");

        // пересадка
        BusOrderItem orderItem2 = createItem();
        BusRide ride2 = orderItem2.getPayload().getRide();
        ride2.getTitlePointFrom().setTitle("Station2");
        ride2.getTitlePointFrom().setPointKey("s2");
        ride2.getTitlePointTo().setTitle("Station3");
        ride2.getTitlePointTo().setPointKey("s3");
        s = busNotificationHelper.buildRouteTitle(List.of(orderItem1, orderItem2));
        assertThat(s).isEqualTo("Station1 — Station2 — Station3");

        // туда-обратно
        ride2.getTitlePointTo().setTitle("Station1");
        ride2.getTitlePointTo().setPointKey("s1");

        s = busNotificationHelper.buildRouteTitle(List.of(orderItem1, orderItem2));
        assertThat(s).isEqualTo("Station1 — Station2 — Station1");

        // прерванный маршрут
        ride2.getTitlePointFrom().setTitle("Station3");
        ride2.getTitlePointFrom().setPointKey("s3");
        ride2.getTitlePointTo().setTitle("Station4");
        ride2.getTitlePointTo().setPointKey("s4");
        s = busNotificationHelper.buildRouteTitle(List.of(orderItem1, orderItem2));
        assertThat(s).isEqualTo("Station1 — Station2, Station3 — Station4");
    }

    @SuppressWarnings("SameParameterValue")
    private BusOrderItem createItem() {
        var item = new BusOrderItem();
        item.setId(UUID.randomUUID());
        item.setState(EOrderItemState.IS_CONFIRMED);
        BusReservation reservation = new BusReservation();
        var ride = new BusRide();
        ride.setPointFrom(new BusPointInfo());
        ride.setPointTo(new BusPointInfo());
        ride.setTitlePointFrom(new BusPointInfo());
        ride.setTitlePointTo(new BusPointInfo());
        reservation.setRide(ride);
        reservation.getRide().setSupplierId(0);
        BusesOrder order = new BusesOrder();
        order.setId("orderId");
        BusesTicket ticket = new BusesTicket();
        ticket.setId("ticketId1");
        order.setTickets(List.of(ticket));
        reservation.setOrder(order);
        item.setReservation(reservation);
        return item;
    }
}
