package ru.yandex.market.api.user.order.builders;

import com.google.common.collect.Lists;
import ru.yandex.market.api.user.order.Outlet;
import ru.yandex.market.api.user.order.checkout.WeekSchedule;

import java.time.DayOfWeek;
import java.util.Objects;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class OutletBuilder extends RandomBuilder<Outlet> {

    private static final int TIME_8_00 = 8 * 60;
    private static final int TIME_20_00 = 20 * 60;
    private static final int INTERVAL_4_HOURS = 4 * 60;


    Outlet outlet;

    public OutletBuilder() {
        this.outlet = new Outlet();
    }

    @Override
    public Outlet build() {
        return outlet;
    }

    @Override
    public OutletBuilder random() {
        outlet.setId(random.getInt(100000));
        outlet.setName(random.getString());
        outlet.setNotes(random.getString());
        for (int i = random.getInt(4); i != 0; --i) {
            if (Objects.isNull(outlet.getPhones())) {
                outlet.setPhones(Lists.newArrayList());
            }
            outlet.getPhones().add(random.getNumber());
        }
        outlet.setWorkSchedules(Lists.newArrayList(
            new WeekSchedule(
                DayOfWeek.MONDAY,
                DayOfWeek.FRIDAY,
                TIME_8_00 + random.getInt(INTERVAL_4_HOURS),
                TIME_20_00 + random.getInt(INTERVAL_4_HOURS)
            ),
            new WeekSchedule(
                DayOfWeek.SATURDAY,
                DayOfWeek.SATURDAY,
                TIME_8_00 + random.getInt(INTERVAL_4_HOURS),
                TIME_20_00 + random.getInt(INTERVAL_4_HOURS)
            )
        ));
        outlet.setAddress(random.getRoomAddress());
        return this;
    }

    public OutletBuilder withId(long id) {
        outlet.setId(id);
        return this;
    }
}
