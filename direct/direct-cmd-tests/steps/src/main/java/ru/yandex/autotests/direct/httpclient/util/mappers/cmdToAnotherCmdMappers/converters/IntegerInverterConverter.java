package ru.yandex.autotests.direct.httpclient.util.mappers.cmdToAnotherCmdMappers.converters;

import org.apache.commons.lang.StringUtils;
import org.dozer.CustomConverter;
import ru.yandex.autotests.direct.httpclient.data.campaigns.campaignInfo.DeviceTargeting;

public class IntegerInverterConverter implements CustomConverter {
    @Override
    public Object convert(Object existingDestinationFieldValue, Object sourceFieldValue, Class<?> destinationClass, Class<?> sourceClass) {
        if (sourceFieldValue != null) {
            return (Integer) sourceFieldValue == 0 ? 1 : 0;
        }
        return sourceFieldValue;
    }
}
