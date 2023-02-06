package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.outlet.DayTimeRange;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;

public class DayTimeRangeJsonHandlerTest extends AbstractJsonHandlerTestBase {

    public static final String JSON = "{ \"dayFrom\": 0, \"timeFrom\": \"08:00\", \"dayTo\": 5, \"timeTo\": \"16:00\"}";

    @Test
    public void serialize() throws Exception {
        DayTimeRange dayTimeRange = EntityHelper.getDayTimeRange();

        String json = write(dayTimeRange);

        checkJson(json, "$." + Names.ShopOutlet.DayTimeRange.DAY_FROM, 0);
        checkJson(json, "$." + Names.ShopOutlet.DayTimeRange.TIME_FROM, "08:00");
        checkJson(json, "$." + Names.ShopOutlet.DayTimeRange.DAY_TO, 5);
        checkJson(json, "$." + Names.ShopOutlet.DayTimeRange.TIME_TO, "16:00");
    }

    @Test
    public void deserialize() throws Exception {
        DayTimeRange dayTimeRange = read(DayTimeRange.class, JSON);

        Assertions.assertEquals(0, dayTimeRange.getDayFrom());
        Assertions.assertEquals("08:00", dayTimeRange.getTimeFrom());
        Assertions.assertEquals(5, dayTimeRange.getDayTo());
        Assertions.assertEquals("16:00", dayTimeRange.getTimeTo());
    }
}
