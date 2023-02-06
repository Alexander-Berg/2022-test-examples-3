package ru.yandex.autotests.reporting.api.beans;

import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.date.DatePatterns;

/**
 * Created by kateleb on 15.11.16.
 */
public class ReportingApiComponent {
    public static final String CPC_SLIDE1 = "cpcSlide1";
    public static final String CPC_SLIDE2 = "cpcSlide2";
    public static final String CPA_SLIDE1 = "cpaSlide1";
    public static final String ASSORTMENT = "assortment";
    public static final String PRICES = "prices";
    public static final String FORECASTER = "forecaster";
    public static final String GROUP_BY_MONTH = "groupByMonth";
    public static final String CLICKS_SHARE_DIAGRAM_PERIOD = "clicksShareDiagramPeriod";
    public static final String CLICKS_SHARE_DYNAMIC_DIAGRAM_PERIOD = "clicksShareDynamicDiagramPeriod";
    public static final String MOBILE_DESKTOP_DIAGRAM_PERIOD = "mobileDesktopDiagramPeriod";
    public static final String CATEGORY_DYNAMIC_DIAGRAM_PERIOD = "categoryDynamicDiagramPeriod";
    public static final String COMPETITION_MAP_DIAGRAM_PERIOD = "competitionMapDiagramPeriod";
    public static final String ORDERS_SHARE_DIAGRAM_PERIOD = "ordersShareDiagramPeriod";
    public static final String ORDERS_SHARE_DYNAMIC_DIAGRAM_PERIOD = "ordersShareDynamicDiagramPeriod";
    public static final String COMPLETED_CANCELLED_DIAGRAM_PERIOD = "completedCancelledDiagramPeriod";
    public static final String PERIOD = "period";
    public static final String NUM_MODELS = "numModels";
    public static final String MIN_CLICKS_THRESHOLD = "minClicksThreshold";
    public static final String PERIODLENGTH = "periodLength";
    public static final String COUNTFORVENDORS = "countForVendors";

    private String name;
    private JsonObject value;

    private ReportingApiComponent(String name, JsonObject json) {
        this.name = name;
        this.value = json;
    }

    public static ReportingApiComponent cpcSlide1(LocalDateTime... period) {
        JsonObject componentJson = new JsonObject();
        JsonObject periodjs = getPeriodJson(period);
        componentJson.add(CLICKS_SHARE_DIAGRAM_PERIOD, periodjs);
        componentJson.add(CLICKS_SHARE_DYNAMIC_DIAGRAM_PERIOD, periodjs);
        componentJson.add(MOBILE_DESKTOP_DIAGRAM_PERIOD, periodjs);
        componentJson.add(CATEGORY_DYNAMIC_DIAGRAM_PERIOD, periodjs);
        return new ReportingApiComponent(CPC_SLIDE1, componentJson);
    }

    public static ReportingApiComponent cpcSlide2(LocalDateTime... period) {
        JsonObject periodjs = getPeriodJson(period);
        JsonObject componentJson = new JsonObject();
        componentJson.add(COMPETITION_MAP_DIAGRAM_PERIOD, periodjs);
        return new ReportingApiComponent(CPC_SLIDE2, componentJson);
    }

    public static ReportingApiComponent cpaSlide1(LocalDateTime... period) {
        JsonObject componentJson = new JsonObject();
        JsonObject periodjs = getPeriodJson(period);
        componentJson.add(ORDERS_SHARE_DIAGRAM_PERIOD, periodjs);
        componentJson.add(ORDERS_SHARE_DYNAMIC_DIAGRAM_PERIOD, periodjs);
        componentJson.add(COMPLETED_CANCELLED_DIAGRAM_PERIOD, periodjs);
        componentJson.add(CATEGORY_DYNAMIC_DIAGRAM_PERIOD, periodjs);
        return new ReportingApiComponent(CPA_SLIDE1, componentJson);
    }

    public static ReportingApiComponent assortment(int numModels, boolean groupByMonth, LocalDateTime... period) {
        JsonObject componentJson = new JsonObject();
        JsonObject periodjs = getPeriodJson(period);
        componentJson.add(PERIOD, periodjs);
        if (numModels != -1) {
            componentJson.addProperty(NUM_MODELS, numModels); //0
        }
        componentJson.addProperty(GROUP_BY_MONTH, groupByMonth); //false
        return new ReportingApiComponent(ASSORTMENT, componentJson);
    }

    public static ReportingApiComponent prices(int minClicksThreshold, LocalDateTime... period) {
        JsonObject componentJson = new JsonObject();
        JsonObject periodjs = getPeriodJson(period);
        componentJson.add(PERIOD, periodjs);
        componentJson.addProperty(MIN_CLICKS_THRESHOLD, minClicksThreshold);//0
        return new ReportingApiComponent(PRICES, componentJson);
    }

    public static ReportingApiComponent forecaster(int periodLength) {
        JsonObject componentJson = new JsonObject();
        componentJson.addProperty(PERIODLENGTH, periodLength);
        componentJson.addProperty(COUNTFORVENDORS, false);
        return new ReportingApiComponent(FORECASTER, componentJson);

    }

    private static JsonObject getPeriodJson(LocalDateTime[] period) {
        return period == null || period.length == 0 ? new JsonObject() :
                period.length == 1? makePeriod(period[0]) : makePeriod(period[0], period[1]);
    }

    private static JsonObject makePeriod(LocalDateTime start, LocalDateTime end) {
        JsonObject period = new JsonObject();
        period.addProperty("from", DatePatterns.MONTH_PARTITION.format(start));
        period.addProperty("to", DatePatterns.MONTH_PARTITION.format(end));
        return period;
    }

    private static JsonObject makePeriod(LocalDateTime date) {
        return makePeriod(date, date);
    }

    public void addProperty(String name, String value) {
        this.value.addProperty(name, value);
    }

    public void addProperty(String name, boolean value) {
        this.value.addProperty(name, value);
    }

    public void addProperty(String name, int value) {
        this.value.addProperty(name, value);
    }

    public void addProperty(String name, JsonObject value) {
        this.value.add(name, value);
    }

    public void addProperty(String name) {
        addProperty(name, new JsonObject());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JsonObject getValue() {
        return value;
    }

    public void setValue(JsonObject value) {
        this.value = value;
    }
}
