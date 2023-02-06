package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;

public class DeliveryDatesBuilder  extends RandomBuilder<DeliveryDates> {
    private DeliveryDates dates = new DeliveryDates();

    @Override
    public DeliveryDatesBuilder random() {
        dates.setToDate(random.getDate());
        dates.setFromDate(random.getDate());
        return this;
    }

    @Override
    public DeliveryDates build() {
        return dates;
    }
}
