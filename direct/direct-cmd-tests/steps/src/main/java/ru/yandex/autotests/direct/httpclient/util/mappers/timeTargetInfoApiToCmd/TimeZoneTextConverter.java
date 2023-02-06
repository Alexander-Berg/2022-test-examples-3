package ru.yandex.autotests.direct.httpclient.util.mappers.timeTargetInfoApiToCmd;

import org.dozer.DozerConverter;
import ru.yandex.autotests.direct.httpclient.data.timetargeting.TimeZones;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 06.04.15
 */
public class TimeZoneTextConverter extends DozerConverter<String, String> {

    public TimeZoneTextConverter() {
        super(String.class, String.class);
    }

    @Override
    public String convertTo(String source, String destination) {
        return TimeZones.getTimeZoneByText(source).getApiText();

    }

    @Override
    public String convertFrom(String source, String destination) {
        return TimeZones.getTimeZoneByApiText(source).getText();
    }

}
