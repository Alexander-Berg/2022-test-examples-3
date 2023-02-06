package steps.shopOutletSteps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.checkout.checkouter.json.Names;

class ScheduleSteps {
    private static final int DAY_FROM = 1;
    private static final String TIME_FROM = "10.00";
    private static final int DAY_TO = 5;
    private static final String TIME_TO = "22.00";

    private ScheduleSteps() {
    }

    static JSONArray getScheduleJsonArray() throws JSONException {
        JSONArray scheduleJsonArray = new JSONArray();
        JSONObject scheduleJson = new JSONObject();

        scheduleJson.put(Names.ShopOutlet.DayTimeRange.DAY_FROM, DAY_FROM);
        scheduleJson.put(Names.ShopOutlet.DayTimeRange.TIME_FROM, TIME_FROM);
        scheduleJson.put(Names.ShopOutlet.DayTimeRange.DAY_TO, DAY_TO);
        scheduleJson.put(Names.ShopOutlet.DayTimeRange.TIME_TO, TIME_TO);

        scheduleJsonArray.put(scheduleJson);
        return scheduleJsonArray;
    }
}
