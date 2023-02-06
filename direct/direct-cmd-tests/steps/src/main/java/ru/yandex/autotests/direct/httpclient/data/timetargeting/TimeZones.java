package ru.yandex.autotests.direct.httpclient.data.timetargeting;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 06.04.15
 */
public enum TimeZones implements ITextResource {
    EUROPE_MOSCOW("130", "Europe/Moscow");

    TimeZones(String timeZoneId, String apiText) {
        this.timeZoneId = timeZoneId;
        this.apiText = apiText;
    }

    private String timeZoneId;
    private String apiText;

    @Override
    public String getBundle() {
        return "backend.timetargeting.TimeZones";
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public String getApiText() {
        return apiText;
    }

    public String getText() {
        return TextResourceFormatter.resource(this).
                locale(DirectTestRunProperties.getInstance().getDirectCmdLocale()).toString();
    }

    @Override
    public String toString() {
        return getText();
    }

    public static TimeZones getTimeZoneById(String timeZoneId) {
        for (TimeZones timeZone : TimeZones.values())  {
            if (timeZone.getTimeZoneId().equals(timeZoneId)) {
                return timeZone;
            }
        }
        throw new BackEndClientException("Временная зона с id="+timeZoneId+" не найдена.");
    }

    public static TimeZones getTimeZoneByApiText(String apiText) {
        for (TimeZones timeZone : TimeZones.values())  {
            if (timeZone.getApiText().equals(apiText)) {
                return timeZone;
            }
        }
        throw new BackEndClientException("Временная зона с текстом в api '"+apiText+"' не найдена.");
    }

    public static TimeZones getTimeZoneByText(String text) {
        for (TimeZones timeZone : TimeZones.values())  {
            if (timeZone.getText().equals(text)) {
                return timeZone;
            }
        }
        throw new BackEndClientException("Временная зона с текстом '"+text+"' не найдена.");
    }
}
