package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.checkout.checkouter.delivery.outlet.BreakTime;
import ru.yandex.market.checkout.checkouter.delivery.outlet.DayTimeRange;

import java.util.List;

public class DayTimeRangeBuilder {
    private DayTimeRange range = new DayTimeRange();


    public DayTimeRangeBuilder setDayFrom(int dayFrom) {
        range.setDayFrom(dayFrom);
        return this;
    }

    public DayTimeRangeBuilder setDayTo(int dayTo) {
        range.setDayTo(dayTo);
        return this;
    }


    public DayTimeRangeBuilder setTimeFrom(String timeFrom) {
        range.setTimeFrom(timeFrom);
        return this;
    }

    public DayTimeRangeBuilder setTimeTo(String timeTo) {
        range.setTimeTo(timeTo);
        return this;
    }

    public DayTimeRangeBuilder setBreaks(List<BreakTime> breaks) {
        range.setBreaks(breaks);
        return this;
    }

    public DayTimeRange build() {
        return range;
    }
}
