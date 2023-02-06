package ru.yandex.chemodan.app.djfs.core.client;

import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.misc.io.ClassPathResourceInputStreamSource;
import ru.yandex.misc.test.Assert;

/**
 * @author tolmalev
 */
public class DataApiHttpClientTest {
    @Test
    public void parseResponseTest() {
        ClassPathResourceInputStreamSource iss = new ClassPathResourceInputStreamSource(getClass(), "dataapi_lenta_test_response.json");
        DataapiMordaBlockList blocks = DataApiHttpClient.dataapiMordaBlockListParser.parseJson(iss);

        DataapiMordaBlock block = blocks.getItems().first();

        Assert.equals(block.getUserTimezoneId(), "Europe/Moscow");
        Assert.equals(block.getMinDate(), Instant.parse("2008-12-05T10:55:08.000Z"));
        Assert.equals(block.getMaxDate(), Instant.parse("2008-12-30T20:45:43.000Z"));

        Assert.notNull(block.getUserMinDate());
        Assert.notNull(block.getUserMaxDate());

        Assert.notEmpty(block.getPhotosliceDate());

    }
}
