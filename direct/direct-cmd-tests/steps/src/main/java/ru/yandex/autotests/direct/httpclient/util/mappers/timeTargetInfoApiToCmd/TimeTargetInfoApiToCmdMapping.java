package ru.yandex.autotests.direct.httpclient.util.mappers.timeTargetInfoApiToCmd;

import ru.yandex.autotests.direct.httpclient.data.timetargeting.TimeTargetInfoCmd;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.HierarchicBeanMappingBuilder;
import ru.yandex.autotests.direct.httpclient.util.mappers.basicConverters.YesNoToOneZeroConverter;
import ru.yandex.autotests.directapi.common.api45.TimeTargetInfo;

import static org.dozer.loader.api.FieldsMappingOptions.customConverter;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 06.04.15
 */
public class TimeTargetInfoApiToCmdMapping extends HierarchicBeanMappingBuilder {

    @Override
    protected void configure() {
        mapping(TimeTargetInfoCmd.class, TimeTargetInfo.class)
                .fields("showOnHolidays", "showOnHolidays", customConverter(YesNoToOneZeroConverter.class))
                .fields("workingHolidays", "workingHolidays", customConverter(YesNoToOneZeroConverter.class))
                .fields("timeZone", "timeZone", customConverter(TimeZoneConverter.class))
                .fields("timeZoneText", "timeZone", customConverter(TimeZoneTextConverter.class))
                .fields("timeTarget", "daysHours", customConverter(TimeTargetConverter.class));
    }
}
