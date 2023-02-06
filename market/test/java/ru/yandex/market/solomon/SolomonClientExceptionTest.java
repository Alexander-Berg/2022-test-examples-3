package ru.yandex.market.solomon;

import org.junit.Assert;
import org.junit.Test;

public class SolomonClientExceptionTest {

    @Test
    public void testResponseParse() {
        String responseBody = "{" +
            "\"status\":\"QUOTA_ERROR\"," +
            "\"errorMessage\":\"more than 1000000 metrics in shard\"," +
            "\"sensorsProcessed\":626" +
            "}";
        SolomonClientException exception = new SolomonClientException("msg", 413, responseBody);

        Assert.assertEquals(413, exception.getResponseCode());
        Assert.assertEquals("QUOTA_ERROR", exception.getStatus());
        Assert.assertEquals("more than 1000000 metrics in shard", exception.getErrorMessage());
        Assert.assertEquals(Long.valueOf(626L), exception.getSensorsProcessed());
    }
}
