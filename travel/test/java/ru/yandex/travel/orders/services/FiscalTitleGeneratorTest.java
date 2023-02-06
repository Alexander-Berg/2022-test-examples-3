package ru.yandex.travel.orders.services;

import java.time.Instant;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.entities.TrainOrder;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.train.model.ReservationPlaceType;
import ru.yandex.travel.train.model.TrainPlace;

import static org.assertj.core.api.Assertions.assertThat;

public class FiscalTitleGeneratorTest {
    private TrainOrderItemFactory orderItemFactory;

    @Before
    public void setUp() {
        orderItemFactory = new TrainOrderItemFactory();
        orderItemFactory.setOrderItemState(EOrderItemState.IS_CONFIRMED);
        orderItemFactory.setTrainTicketNumber("011Я");
        orderItemFactory.setCarNumber("04");
        orderItemFactory.setStationFromTitleGenitive("Екатеринбурга");
        orderItemFactory.setStationFromTimezone("Asia/Yekaterinburg");
        orderItemFactory.setStationToPreposition("во");
        orderItemFactory.setStationToTitleAccusative("Владимир");
        orderItemFactory.setDepartureTime(Instant.parse("2019-08-21T12:00:00Z"));
    }

    @Test
    public void testTrainFiscalTitleOneTicket() {
        TrainOrder order = new TrainOrder();
        order.addOrderItem(orderItemFactory.createTrainOrderItem());
        assertThat(FiscalTitleGenerator.getFiscalTitle(order)).isEqualTo(
                "Заказ билета на поезд 011Я из Екатеринбурга во Владимир 21 августа 2019 года в 17:00, вагон 04");
    }

    @Test
    public void testTrainFiscalTitleSeveralTickets() {
        TrainOrder order = new TrainOrder();
        TrainOrderItem orderItem = orderItemFactory.createTrainOrderItem();
        orderItem.getPayload().getPassengers().add(orderItem.getPayload().getPassengers().get(0));
        order.addOrderItem(orderItem);
        assertThat(FiscalTitleGenerator.getFiscalTitle(order)).isEqualTo(
                "Заказ билетов на поезд 011Я из Екатеринбурга во Владимир 21 августа 2019 года в 17:00, вагон 04");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoOrderItem() {
        TrainOrder order = new TrainOrder();
        FiscalTitleGenerator.getFiscalTitle(order);
    }

    @Test
    public void testMakeTrainFiscalItemTitle() {
        assertThat(FiscalTitleGenerator.makeTrainFiscalItemTitle("Start", null, false, "007")).isEqualTo("Start");

        var places = new ArrayList<TrainPlace>();
        assertThat(FiscalTitleGenerator.makeTrainFiscalItemTitle("Start", places, false, "007")).isEqualTo("Start");

        places.add(new TrainPlace("01", ReservationPlaceType.NO_VALUE));
        assertThat(FiscalTitleGenerator.makeTrainFiscalItemTitle("Start", places, false, "007"))
                .isEqualTo("Start, место 01");

        places.add(new TrainPlace("2", ReservationPlaceType.NO_VALUE));
        assertThat(FiscalTitleGenerator.makeTrainFiscalItemTitle("Start", places, false, "007"))
                .isEqualTo("Start, места 01, 2");

        assertThat(FiscalTitleGenerator.makeTrainFiscalItemTitle("Start", places, true, "007"))
                .isEqualTo("Start, поезд 007, места 01, 2");
    }
}
