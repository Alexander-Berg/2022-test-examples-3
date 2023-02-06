package ru.yandex.autotests.direct.cmd.util.mappers.perfGroupsRequestToResponse;

import org.apache.commons.lang3.StringUtils;
import org.dozer.CustomConverter;

import java.util.List;

/**
 * Created by aleran on 23.11.2015.
 */
public class MinusKeywordsConverter implements CustomConverter {

    @Override
    public Object convert(Object existingDestinationFieldValue, Object sourceFieldValue, Class<?> destinationClass, Class<?> sourceClass) {
        return StringUtils.join((List<String>) sourceFieldValue, ",");
    }
}
