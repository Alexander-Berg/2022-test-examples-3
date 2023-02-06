package ru.yandex.chemodan.app.djfs.api;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.test.Assert;


public class DjfsApiIndexerTest extends DjfsApiActionTest {

    @Test
    @Ignore
    public void testGetResource() {
        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.get("/api/v1/indexer/resources?"
                + "service=disk-search&uid=4007092733"
                + "&resource_id=4007092733:88af1aa7bb778072dc98f10e9eac98faae66614c2cd9ebc98052d46685dbc00f");
        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        String result = helper.getResult(response);
        Assert.notNull(result);
    }

}
