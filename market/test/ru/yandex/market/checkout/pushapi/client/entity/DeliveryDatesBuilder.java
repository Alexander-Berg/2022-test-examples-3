package ru.yandex.market.checkout.pushapi.client.entity;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;

import java.time.LocalTime;
import java.util.Date;

import static ru.yandex.market.checkout.pushapi.client.entity.BuilderUtil.createDate;

/**
 * Created by msavelyev on 25.09.13.
 */
public class DeliveryDatesBuilder implements Builder<DeliveryDates> {

    private DeliveryDates deliveryDates;

    public DeliveryDatesBuilder() {
        deliveryDates = new DeliveryDates();
        deliveryDates.setFromDate(createDate("2013-06-01"));
        deliveryDates.setToDate(createDate("2013-06-02"));
    }

    public DeliveryDatesBuilder setFromDate(Date fromDate) {
        deliveryDates.setFromDate(fromDate);
        return this;
    }

    public DeliveryDatesBuilder setToDate(Date toDate) {
        deliveryDates.setToDate(toDate);
        return this;
    }

    public DeliveryDatesBuilder setFromTime(LocalTime fromTime) {
        deliveryDates.setFromTime(fromTime);
        return this;
    }

    public DeliveryDatesBuilder setToTime(LocalTime toTime) {
        deliveryDates.setToTime(toTime);
        return this;
    }

    @Override
    public DeliveryDates build() {
        return deliveryDates;
    }
}
