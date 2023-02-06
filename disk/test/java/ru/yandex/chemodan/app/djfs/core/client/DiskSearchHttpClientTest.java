package ru.yandex.chemodan.app.djfs.core.client;

import org.junit.Test;

import ru.yandex.misc.io.ClassPathResourceInputStreamSource;
import ru.yandex.misc.test.Assert;

public class DiskSearchHttpClientTest {
    @Test
    public void parseResponse() {
        ClassPathResourceInputStreamSource
                iss = new ClassPathResourceInputStreamSource(getClass(), "disk_search_test_response.json");
        DiskSearchResponse diskSearchResponse = DiskSearchResponse.jsonParser.parseJson(iss);

        Assert.equals(2, diskSearchResponse.getHitsCount());
    }
}
