package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.shop.ScheduleLine;

public class ScheduleLineJsonHandlerTest extends AbstractJsonHandlerTestBase {

    public static final String SCHEDULE_LINE = "{\"day\":0,\"start\":600,\"end\":1200}";

    @Test
    public void serialize() throws Exception {
        ScheduleLine scheduleLine = EntityHelper.getScheduleLine();

        String json = write(scheduleLine);
        System.out.println(json);

        checkJson(json, "$." + Names.ScheduleLine.DAY, 0);
        checkJson(json, "$." + Names.ScheduleLine.START, 600);
        checkJson(json, "$." + Names.ScheduleLine.END, 1200);
    }

    @Test
    public void deserialize() throws Exception {
        ScheduleLine scheduleLine = read(ScheduleLine.class, SCHEDULE_LINE);

        Assertions.assertEquals(0, scheduleLine.getDay());
        Assertions.assertEquals(600, scheduleLine.getStartMinute());
        Assertions.assertEquals(1200, scheduleLine.getEndMinute());
    }
}
