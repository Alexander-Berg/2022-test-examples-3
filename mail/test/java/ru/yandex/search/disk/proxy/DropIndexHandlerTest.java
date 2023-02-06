package ru.yandex.search.disk.proxy;

import org.apache.http.HttpStatus;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.test.util.TestBase;

public class DropIndexHandlerTest extends TestBase {
    @Test
    public void test() throws Exception {
        try (ProxyCluster cluster = new ProxyCluster(this)) {
            cluster.start();
            cluster.backend().add("\"version\":0,\"id\":1,\"type\":\"file\"");
            String uri = "/search?prefix=0&text=id:*&length=0";
            cluster.backend().checkSearch(
                uri,
                "{\"hitsCount\":1,\"hitsArray\":[]}");
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.proxy().port(),
                "/drop-index?uid=0");
            cluster.backend().checkSearch(
                uri,
                "{\"hitsCount\":0,\"hitsArray\":[]}");
        }
    }
}

