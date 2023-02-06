package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.checkout.checkouter.delivery.outlet.BreakTime;

public class BreakTimeBuilder {
    private BreakTime time = new BreakTime();

    public BreakTimeBuilder setFrom(String from) {
        time.setTimeFrom(from);
        return this;
    }

    public BreakTimeBuilder setTo(String to) {
        time.setTimeTo(to);
        return this;
    }

    public BreakTime build() {
        return time;
    }
}
