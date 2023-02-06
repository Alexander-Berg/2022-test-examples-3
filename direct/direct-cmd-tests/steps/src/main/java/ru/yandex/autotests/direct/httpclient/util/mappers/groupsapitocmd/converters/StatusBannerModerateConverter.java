package ru.yandex.autotests.direct.httpclient.util.mappers.groupsapitocmd.converters;

import org.dozer.CustomConverter;

/**
 * Created by shmykov on 29.04.15.
 */
public class StatusBannerModerateConverter implements CustomConverter {

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_SENDING = "Sending";

    @Override
    public Object convert(Object existingDestinationFieldValue, Object sourceFieldValue, Class<?> destinationClass, Class<?> sourceClass) {
        if (sourceFieldValue.equals(STATUS_PENDING)) {
            return STATUS_SENDING;
        }
        return sourceFieldValue;
    }
}
