package ru.yandex.autotests.direct.httpclient.util.mappers.basicConverters;

import org.dozer.CustomConverter;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;

/**
 * Created by shmykov on 13.04.15.
 */
public class ApiToCmdHrefUrlProtocolConverter implements CustomConverter {

    @Override
    public Object convert(Object existingDestinationFieldValue, Object sourceFieldValue, Class<?> destinationClass, Class<?> sourceClass) {
        if (sourceFieldValue == null || sourceFieldValue.equals("")) {
            return null;
        }
        String[] protocolAndHref = ((String) sourceFieldValue).split("//");
        if (protocolAndHref.length == 2) {
            return protocolAndHref[0] + "//";

        } else {
            throw new BackEndClientException("Wrong href format: " + sourceFieldValue);
        }
    }
}