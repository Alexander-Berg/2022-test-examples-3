package ru.yandex.market.logistics.lms.client.models;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FallbackClientLogTestInfo {
    Runnable lmsDataClientCallingAndVerifying;

    boolean loggingEnabled;

    String methodName;

    String params;
}
