package ru.yandex.market.takeout.service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Assert;

import ru.yandex.market.takeout.config.TakeoutDescription;

public class TakeoutRequestProcessorGetStatusTest extends TakeoutRequestProcessorTestCase {

    public void testGetEmptyStatusWithoutRemap() throws Exception {
        TakeoutDescription takeoutDescription = getTakeoutDescription(Collections.emptyMap());
        String responseBody = "{\"types\":[]}";
        CompletableFuture<List<String>> takeoutStatus = getTakeoutStatus(takeoutDescription,
                getSuccessfulHttpClient(responseBody));
        List<String> strings = takeoutStatus.get();
        Assert.assertEquals(Collections.emptyList(), strings);
    }

    public void testGetEmptyStatus() throws Exception {
        TakeoutDescription takeoutDescription = getTakeoutDescription(Collections.singletonMap("type", "type"));
        String responseBody = "{\"types\":[]}";
        CompletableFuture<List<String>> takeoutStatus = getTakeoutStatus(takeoutDescription,
                getSuccessfulHttpClient(responseBody));
        List<String> strings = takeoutStatus.get();
        Assert.assertEquals(Collections.emptyList(), strings);
    }

    public void testGetNotEmptyStatus() throws Exception {
        TakeoutDescription takeoutDescription = getTakeoutDescription(Collections.singletonMap("type", "type"));
        String responseBody = "{\"types\":[\"type\"]}";
        CompletableFuture<List<String>> takeoutStatus = getTakeoutStatus(takeoutDescription,
                getSuccessfulHttpClient(responseBody));
        List<String> strings = takeoutStatus.get();
        Assert.assertEquals(Collections.singletonList("type"), strings);

    }

    public void testGetNotEmptyStatusWithUnknownRemap() throws Exception {
        TakeoutDescription takeoutDescription = getTakeoutDescription(Collections.singletonMap("type", "type"));
        String responseBody = "{\"types\":[\"type2\"]}";
        CompletableFuture<List<String>> takeoutStatus = getTakeoutStatus(takeoutDescription,
                getSuccessfulHttpClient(responseBody));
        List<String> strings = takeoutStatus.get();
        Assert.assertEquals(Collections.emptyList(), strings);

    }

    public void testGetNotEmptyStatusWithRemap() throws Exception {
        TakeoutDescription takeoutDescription = getTakeoutDescription(Collections.singletonMap("type2", "type"));
        String responseBody = "{\"types\":[\"type2\"]}";
        CompletableFuture<List<String>> takeoutStatus = getTakeoutStatus(takeoutDescription,
                getSuccessfulHttpClient(responseBody));
        List<String> strings = takeoutStatus.get();
        Assert.assertEquals(Collections.singletonList("type"), strings);
    }

    public void testExceptionalGetStatus() {

        TakeoutDescription takeoutDescription = getTakeoutDescription(Collections.emptyMap());
        CompletableFuture<List<String>> status = getTakeoutStatus(takeoutDescription,
                EXCEPTIONAL_HTTP_CLIENT);

        Assert.assertThrows(HttpException.class, () -> {
            unwrapExecutionException(status);
        });
    }

}
