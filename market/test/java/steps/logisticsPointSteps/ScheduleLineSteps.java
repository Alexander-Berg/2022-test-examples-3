package steps.logisticsPointSteps;

import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.core.schedule.ScheduleLine.DayOfWeek;
import ru.yandex.market.delivery.mdbapp.integration.payload.ScheduleLine;

class ScheduleLineSteps {
    private static final DayOfWeek START_DAY = DayOfWeek.MONDAY;
    private static final int DAYS = 7;
    private static final int START_MINUTE = 8;
    private static final int MINUTES = 120;

    private ScheduleLineSteps() {
    }

    static List<ScheduleLine> getScheduleLines() {
        return Arrays.asList(
            new ScheduleLine(
                START_DAY,
                DAYS,
                START_MINUTE,
                MINUTES
            ),
            new ScheduleLine(
                START_DAY.nextDay(),
                DAYS - 1,
                START_MINUTE + 60,
                MINUTES
            )
        );
    }

    static List<ScheduleLine> getInletScheduleLines() {
        return Arrays.asList(
            new ScheduleLine(
                START_DAY,
                0,
                START_MINUTE,
                MINUTES
            ),
            new ScheduleLine(
                START_DAY.nextDay(),
                0,
                START_MINUTE + 60,
                MINUTES
            )
        );
    }

    static JSONArray getScheduleLinesJson() throws JSONException {
        JSONArray scheduleLinesJsonArray = new JSONArray();
        JSONObject scheduleLineJson = new JSONObject();

        scheduleLineJson.put("startDay", START_DAY);
        scheduleLineJson.put("minutes", MINUTES);
        scheduleLineJson.put("days", DAYS);
        scheduleLineJson.put("startMinute", START_MINUTE);

        scheduleLinesJsonArray.put(scheduleLineJson);

        JSONObject scheduleLineJsonNext = new JSONObject();

        scheduleLineJsonNext.put("startDay", START_DAY.nextDay());
        scheduleLineJsonNext.put("minutes", MINUTES);
        scheduleLineJsonNext.put("days", DAYS - 1);
        scheduleLineJsonNext.put("startMinute", START_MINUTE + 60);
        scheduleLinesJsonArray.put(scheduleLineJsonNext);

        return scheduleLinesJsonArray;
    }

    static JSONArray getInletScheduleLinesJson() throws JSONException {
        JSONArray scheduleLinesJsonArray = new JSONArray();
        JSONObject scheduleLineJson = new JSONObject();

        scheduleLineJson.put("startDay", START_DAY);
        scheduleLineJson.put("minutes", MINUTES);
        scheduleLineJson.put("startMinute", START_MINUTE);

        scheduleLinesJsonArray.put(scheduleLineJson);

        JSONObject scheduleLineJsonNext = new JSONObject();

        scheduleLineJsonNext.put("startDay", START_DAY.nextDay());
        scheduleLineJsonNext.put("minutes", MINUTES);
        scheduleLineJsonNext.put("startMinute", START_MINUTE + 60);
        scheduleLinesJsonArray.put(scheduleLineJsonNext);

        return scheduleLinesJsonArray;
    }
}
