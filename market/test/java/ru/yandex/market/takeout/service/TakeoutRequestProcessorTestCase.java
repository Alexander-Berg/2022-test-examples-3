package ru.yandex.market.takeout.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.takeout.common.TakeoutAsyncHttpClient;
import ru.yandex.market.takeout.config.TakeoutDescription;

public abstract class TakeoutRequestProcessorTestCase extends RequestProcessorTestCase {



    protected CompletableFuture<List<String>> getTakeoutStatus(TakeoutDescription description,
                                                               TakeoutAsyncHttpClient exceptionalHttpClient) {
        TakeoutRequestProcessor takeoutRequestProcessor = new TakeoutRequestProcessor(exceptionalHttpClient,
                description, MODULE_MAP);
        return takeoutRequestProcessor.getStatus(0L, Collections.emptyMap(),
                new RequestContext(""));
    }

    @NotNull
    protected TakeoutDescription getTakeoutDescription(Map<String, String> typeRemap) {
        TakeoutDescription description = new TakeoutDescription();
        description.setTypeRemap(typeRemap);
        return description;
    }

    static class HttpException extends Exception {
    }
}
