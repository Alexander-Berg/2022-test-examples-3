package ru.yandex.market.checkout.checkouter.json;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.shop.Schedule;
import ru.yandex.market.checkout.checkouter.shop.ScheduleLine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.isA;

public class ScheduleJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        Schedule schedule = new Schedule(Arrays.asList(EntityHelper.getScheduleLine()));

        String json = write(schedule);

        checkJson(json, "$", JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$", hasSize(1));
    }

    @Test
    public void deserialize() throws Exception {
        String json = "[ " + ScheduleLineJsonHandlerTest.SCHEDULE_LINE + " ]";

        Schedule schedule = read(Schedule.class, json);

        assertThat(schedule.getLines(), hasSize(1));
        assertThat(schedule.getLines().get(0), isA(ScheduleLine.class));
    }
}
