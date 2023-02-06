package ru.yandex.travel.orders.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.entities.GenericOrder;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflows.orderitem.train.TrainWorkflowProperties;
import ru.yandex.travel.train.model.ReservationPlaceType;
import ru.yandex.travel.train.model.TrainPassenger;
import ru.yandex.travel.train.model.TrainPlace;
import ru.yandex.travel.train.model.TrainReservationUiData;
import ru.yandex.travel.train.model.TrainTicket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TrainNotificationHelperTest {
    private UrlShortenerService urlShortenerService;
    private TrainNotificationHelper notificationHelper;

    @Before
    public void setUp() {
        TrainWorkflowProperties trainWorkflowProperties = new TrainWorkflowProperties();
        trainWorkflowProperties.setMail(new TrainWorkflowProperties.MailProperties());
        trainWorkflowProperties.getMail().setFrontUrl("example.yandex.ru");
        trainWorkflowProperties.setSms(new TrainWorkflowProperties.SmsProperties());
        trainWorkflowProperties.getSms().setOrderTicketText("Ваш билет на поезд%s: %s");
        trainWorkflowProperties.getSms().setOrderTicketsText("Ваши билеты на поезд%s: %s");
        urlShortenerService = mock(UrlShortenerService.class);
        notificationHelper = new TrainNotificationHelper(trainWorkflowProperties, urlShortenerService);
    }

    @Test
    public void testConfirmationSmsText() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_CONFIRMED);
        var order = new GenericOrder();
        order.setId(UUID.fromString("00000000-1111-2222-3333-444444444444"));
        var orderItem = factory.createTrainOrderItem();
        order.addOrderItem(orderItem);
        orderItem.getPayload().getReservationRequestData().setTrainTicketNumber("060A");
        orderItem.getPayload().setDepartureTime(Instant.parse("2018-03-20T10:20:30Z"));
        orderItem.getPayload().setArrivalTime(Instant.parse("2018-03-21T10:20:30Z"));
        when(urlShortenerService.shorten(any(), anyBoolean())).thenReturn("http://ya.ru/veryshorturl");

        var resultText = notificationHelper.getConfirmationSmsText(order);
        assertThat(resultText).isEqualTo("Ваш билет на поезд 060A 20 марта 2018: http://ya.ru/veryshorturl");
        verify(urlShortenerService).shorten(eq("https://example.yandex.ru/my/order/00000000-1111-2222-3333" +
                "-444444444444?utm_source=sms&utm_medium=transaction&utm_campaign=buy"), eq(true));

        var orderItem2 = factory.createTrainOrderItem();
        order.addOrderItem(orderItem2);
        resultText = notificationHelper.getConfirmationSmsText(order);
        assertThat(resultText).isEqualTo("Ваши билеты на поезд: http://ya.ru/veryshorturl");
    }

    @Test
    public void testBuildGenericRouteTitle() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_CONFIRMED);
        TrainOrderItem orderItem1 = factory.createTrainOrderItem();
        TrainReservationUiData uiData1 = orderItem1.getPayload().getUiData();
        uiData1.setStationFromNotGeneralize(false);
        uiData1.setStationFromTitle("Station1");
        uiData1.setStationFromSettlementTitle("City1");
        uiData1.setStationToNotGeneralize(false);
        uiData1.setStationToTitle("Station2");
        uiData1.setStationToSettlementTitle("City2");
        var s = notificationHelper.buildGenericRouteTitle(List.of(orderItem1));
        assertThat(s).isEqualTo("City1 — City2");

        // пересадка
        TrainOrderItem orderItem2 = factory.createTrainOrderItem();
        TrainReservationUiData uiData2 = orderItem2.getPayload().getUiData();
        orderItem2.getPayload().setDepartureTime(orderItem1.getPayload().getDepartureTime().plusSeconds(999));
        uiData2.setStationFromNotGeneralize(false);
        uiData2.setStationFromTitle("Station2");
        uiData2.setStationFromSettlementTitle("City2");
        uiData2.setStationToNotGeneralize(false);
        uiData2.setStationToTitle("Station3");
        uiData2.setStationToSettlementTitle("City3");
        s = notificationHelper.buildGenericRouteTitle(List.of(orderItem1, orderItem2));
        assertThat(s).isEqualTo("City1 — City2 — City3");

        // пересадка на разных станциях в одном городе
        uiData2.setStationFromTitle("Station2.2");
        s = notificationHelper.buildGenericRouteTitle(List.of(orderItem1, orderItem2));
        assertThat(s).isEqualTo("City1 — City2 — City3");

        // туда-обратно
        uiData2.setStationToTitle("Station1");
        uiData2.setStationToSettlementTitle("City1");
        s = notificationHelper.buildGenericRouteTitle(List.of(orderItem1, orderItem2));
        assertThat(s).isEqualTo("City1 — City2 — City1");

        // пересадка, последний сегмент в одном городе
        uiData2.setStationToTitle("Station3");
        uiData2.setStationToSettlementTitle("City2");
        s = notificationHelper.buildGenericRouteTitle(List.of(orderItem1, orderItem2));
        assertThat(s).isEqualTo("City1 — Station2 — Station3");

        // прерванный маршрут
        uiData2.setStationFromTitle("Station3");
        uiData2.setStationFromSettlementTitle("City3");
        uiData2.setStationToTitle("Station4");
        uiData2.setStationToSettlementTitle("City4");
        s = notificationHelper.buildGenericRouteTitle(List.of(orderItem1, orderItem2));
        assertThat(s).isEqualTo("City1 — City2, City3 — City4");

        // NotGeneralize
        uiData1.setStationFromNotGeneralize(true);
        s = notificationHelper.buildGenericRouteTitle(List.of(orderItem1));
        assertThat(s).isEqualTo("Station1 — City2");
    }

    @Test
    public void testBuildRouteTrainTitle() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_CONFIRMED);
        TrainOrderItem orderItem1 = factory.createTrainOrderItem();
        TrainReservationUiData uiData1 = orderItem1.getPayload().getUiData();
        uiData1.setStationFromNotGeneralize(false);
        uiData1.setStationFromTitle("Station1");
        uiData1.setStationFromSettlementTitle("City1");
        uiData1.setStationToNotGeneralize(false);
        uiData1.setStationToTitle("Station2");
        uiData1.setStationToSettlementTitle("City2");
        var s = notificationHelper.buildRouteTrainTitle(orderItem1.getPayload());
        assertThat(s).isEqualTo("City1 — City2");

        // NotGeneralize
        uiData1.setStationFromNotGeneralize(true);
        s = notificationHelper.buildRouteTrainTitle(orderItem1.getPayload());
        assertThat(s).isEqualTo("Station1 — City2");
    }

    @Test
    public void testGetPlacesString() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_CONFIRMED);
        var passengers = new ArrayList<TrainPassenger>();
        TrainPassenger passenger1 = factory.createTrainPassenger();
        passengers.add(passenger1);
        TrainTicket ticket1 = passenger1.getTicket();
        setPlaces(ticket1, List.of("*"));
        var s = notificationHelper.getPlacesString(passengers);
        assertThat(s).isEqualTo("место *");

        setPlaces(ticket1, List.of());
        s = notificationHelper.getPlacesString(passengers);
        assertThat(s).isEqualTo("без мест");

        setPlaces(ticket1, List.of("01"));
        s = notificationHelper.getPlacesString(passengers);
        assertThat(s).isEqualTo("место 1");

        setPlaces(ticket1, List.of("01Б"));
        s = notificationHelper.getPlacesString(passengers);
        assertThat(s).isEqualTo("место 1Б");

        setPlaces(ticket1, List.of("010", "01Б", "5C"));
        s = notificationHelper.getPlacesString(passengers);
        assertThat(s).isEqualTo("места 1Б, 5C, 10");

        TrainPassenger passenger2 = factory.createTrainPassenger();
        passengers.add(passenger2);
        TrainTicket ticket2 = passenger2.getTicket();
        setPlaces(ticket2, List.of("01Б", "06"));
        s = notificationHelper.getPlacesString(passengers);
        assertThat(s).isEqualTo("места 1Б, 5C, 6, 10");
    }

    private void setPlaces(TrainTicket ticket1, List<String> numbers) {
        ticket1.setPlaces(numbers.stream()
                .map(x -> new TrainPlace(x, ReservationPlaceType.LOWER))
                .collect(Collectors.toList()));
    }
}
