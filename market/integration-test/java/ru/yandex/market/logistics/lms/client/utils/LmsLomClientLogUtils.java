package ru.yandex.market.logistics.lms.client.utils;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.lom.lms.model.logging.enums.LmsLomLoggingCode;

@UtilityClass
@ParametersAreNonnullByDefault
@SuppressWarnings("HideUtilityClassConstructor")
public class LmsLomClientLogUtils {

    @Nonnull
    public String getEntityNotFoundLog(LmsLomLoggingCode code, String method) {
        return String.format(
            "level=ERROR\t"
                + "format=plain\t"
                + "code=%s\t"
                + "payload=%s\t"
                + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t"
                + "tags=ENTITY_NOT_FOUND",
            code,
            method
        );
    }

    @Nonnull
    public String lmsClientCalling(String methodName, String params) {
        return String.format(
            "level=WARN\t"
                + "format=plain\t"
                + "code=%s\t"
                + "payload=Lms usage\t"
                + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t"
                + "tags=LMS_CLIENT_USAGE\t"
                + "extra_keys=params\t"
                + "extra_values=%s",
            methodName,
            params
        );
    }

    @Nonnull
    public String ytClientCalling(String methodName, String params) {
        return String.format(
            "level=WARN\t"
                + "format=plain\t"
                + "code=%s\t"
                + "payload=YT client for LMS usage\t"
                + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t"
                + "tags=YT_CLIENT_USAGE\t"
                + "extra_keys=params\t"
                + "extra_values=%s",
            methodName,
            params
        );
    }
}
