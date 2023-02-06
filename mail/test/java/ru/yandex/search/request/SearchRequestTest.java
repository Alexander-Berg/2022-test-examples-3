package ru.yandex.search.request;

import java.io.StringReader;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.request.RequestInfo;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.search.request.config.ImmutableSearchBackendRequestConfig;
import ru.yandex.search.request.config.ImmutableSearchBackendRequestsConfig;
import ru.yandex.search.request.config.SearchBackendRequestsConfigBuilder;
import ru.yandex.test.util.TestBase;

public class SearchRequestTest extends TestBase {
    @Test
    public void test() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig("Proxy"))) {
            IniConfig config = new IniConfig(
                new StringReader(
                    this.loadResourceAsString("config.ini")));
            SearchBackendRequestsConfigBuilder builder =
                new SearchBackendRequestsConfigBuilder(config);

            final ImmutableSearchBackendRequestsConfig immutable
                = new ImmutableSearchBackendRequestsConfig(builder);

            System.out.println(immutable.configs());
            System.out.println(config);

            server.add("/api/async/mail/search?request=pelmeni", (request, response, context) -> {
                ImmutableSearchBackendRequestConfig reqConfig =
                    immutable.configs().get(new RequestInfo(request)).asterisk();
                Assert.assertEquals(1, reqConfig.minPos().intValue());
                Assert.assertEquals(1000, reqConfig.failoverDelay());
                Assert.assertEquals(false, reqConfig.localityShuffle());
                Assert.assertEquals(SearchBackendRequestType.SEQUENTIAL, reqConfig.type());
                response.setStatusCode(HttpStatus.SC_OK);
            });

            server.add("/api/async/mail/search?request=vareniki", (request, response, context) -> {
                ImmutableSearchBackendRequestConfig reqConfig =
                    immutable.configs().get(new RequestInfo(request)).get("translit");
                Assert.assertEquals(-1, reqConfig.minPos().intValue());
                Assert.assertEquals(10000, reqConfig.failoverDelay());
                Assert.assertEquals(true, reqConfig.allowLaggingHosts());
                Assert.assertEquals(SearchBackendRequestType.PARALLEL, reqConfig.type());

                // asterisk for this request
                reqConfig = immutable.configs().get(new RequestInfo(request)).get("notexisting");
                Assert.assertEquals(SearchBackendRequestType.SEQUENTIAL, reqConfig.type());

                response.setStatusCode(HttpStatus.SC_OK);
            });

            server.add("/default", (request, response, context) -> {
                // expecting asterisk
                ImmutableSearchBackendRequestConfig reqConfig =
                    immutable.configs().get(new RequestInfo(request)).get("vasya");
                Assert.assertEquals(30000, reqConfig.failoverDelay());
                Assert.assertEquals(SearchBackendRequestType.SEQUENTIAL, reqConfig.type());
                response.setStatusCode(HttpStatus.SC_OK);
            });

            server.start();

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK, server.port(),
                "/api/async/mail/search?request=pelmeni");
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK, server.port(),
                "/api/async/mail/search?request=vareniki");
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK, server.port(),
                "/default");

        }
    }
}
