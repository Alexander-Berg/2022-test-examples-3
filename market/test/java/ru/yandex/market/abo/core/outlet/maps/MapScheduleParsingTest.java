package ru.yandex.market.abo.core.outlet.maps;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.abo.core.outlet.maps.model.MapsScheduleDay;
import ru.yandex.market.abo.core.outlet.maps.model.MapsScheduleLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author artemmz
 * @date 25.04.18.
 */
public class MapScheduleParsingTest {
    private static final String SCHEDULE_FROM_YT = "[" +
            "    {" +
            "        \"day\": \"saturday\"," +
            "        \"time_minutes_begin\": 600," +
            "        \"time_minutes_end\": 1200" +
            "    }," +
            "    {" +
            "        \"day\": \"sunday\"," +
            "        \"time_minutes_begin\": 660," +
            "        \"time_minutes_end\": 1080" +
            "    }," +
            "    {" +
            "        \"day\": \"weekdays\"," +
            "        \"time_minutes_begin\": 600," +
            "        \"time_minutes_end\": 1200" +
            "    }" +
            "]";

    @Test
    public void parseSchedule() {
        List<MapsScheduleLine> scheduleLines = OutletYtService.extractSchedule(SCHEDULE_FROM_YT);
        assertEquals(3, scheduleLines.size());
        MapsScheduleLine saturday = scheduleLines.iterator().next();
        assertEquals(MapsScheduleDay.SATURDAY, saturday.getDay());
        assertEquals(600, saturday.getTimeMinutesBegin());
        assertEquals(1200, saturday.getTimeMinutesEnd());
    }

    @Test
    public void parseNullOrEmpty() {
        assertTrue(OutletYtService.extractSchedule(null).isEmpty());
        assertTrue(OutletYtService.extractSchedule("").isEmpty());
    }
}
