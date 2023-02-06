package ru.yandex.autotests.direct.httpclient.data.timetargeting;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 02.04.15
 */
public class TimeTargetInfoCmd {

    @JsonPath(responsePath = "time_target_holiday")
    private String showOnHolidays;
    @JsonPath(responsePath = "time_target_holiday_from")
    private Integer holidayShowFrom;
    @JsonPath(responsePath = "time_target_holiday_to")
    private Integer holidayShowTo;
    @JsonPath(responsePath = "timeTarget")
    private String timeTarget;
    @JsonPath(responsePath = "timezone_id")
    private String timeZone;
    @JsonPath(responsePath = "timezone_text")
    private String timeZoneText;
    @JsonPath(responsePath = "time_target_working_holiday")
    private String workingHolidays;


    public String getShowOnHolidays() {
        return showOnHolidays;
    }

    public void setShowOnHolidays(String showOnHolidays) {
        this.showOnHolidays = showOnHolidays;
    }

    public Integer getHolidayShowFrom() {
        return holidayShowFrom;
    }

    public void setHolidayShowFrom(Integer holidayShowFrom) {
        this.holidayShowFrom = holidayShowFrom;
    }

    public Integer getHolidayShowTo() {
        return holidayShowTo;
    }

    public void setHolidayShowTo(Integer holidayShowTo) {
        this.holidayShowTo = holidayShowTo;
    }

    public String getTimeTarget() {
        return timeTarget;
    }

    public void setTimeTarget(String timeTarget) {
        this.timeTarget = timeTarget;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getWorkingHolidays() {
        return workingHolidays;
    }

    public void setWorkingHolidays(String workingHolidays) {
        this.workingHolidays = workingHolidays;
    }

    public String getTimeZoneText() {
        return timeZoneText;
    }

    public void setTimeZoneText(String timeZoneText) {
        this.timeZoneText = timeZoneText;
    }
}
