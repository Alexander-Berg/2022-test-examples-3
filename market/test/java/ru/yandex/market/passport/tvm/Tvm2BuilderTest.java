package ru.yandex.market.passport.tvm;

import org.apache.http.client.HttpClient;
import org.joda.time.Duration;
import org.junit.Test;

import ru.yandex.inside.passport.tvm2.TvmClientCredentials;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;

/**
 * @author anmalysh
 * @since 10/26/2018
 */
public class Tvm2BuilderTest {

    private Timeout timeout = Timeout.seconds(1);
    private HttpClient client = ApacheHttpClientUtils.singleConnectionClient(timeout);
    private TvmClientCredentials credentials = new TvmClientCredentials(0, "a");

    @Test(expected = IllegalStateException.class)
    public void testClientWithTimeoutFails() {
        new Tvm2Builder()
            .setHttpClient(client)
            .setTimeout(timeout)
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testClientWithUserAgentFails() {
        new Tvm2Builder()
            .setHttpClient(client)
            .setUserAgent("b")
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testClientWithMaxConnectionsFails() {
        new Tvm2Builder()
            .setHttpClient(client)
            .setMaxConnections(1)
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testNoCredentialsFails() {
        new Tvm2Builder()
            .build();
    }

    @Test
    public void testMinimalPasses() {
        new Tvm2Builder()
            .setCredentials(credentials)
            .build();
    }

    @Test
    public void testHttpClientPasses() {
        new Tvm2Builder()
            .setCredentials(credentials)
            .setHttpClient(client)
            .setRefreshDelay(Duration.ZERO)
            .setRefreshErrorDelay(Duration.ZERO)
            .build();
    }

    @Test
    public void testNoHttpClientPasses() {
        new Tvm2Builder()
            .setCredentials(credentials)
            .setMaxConnections(1)
            .setUserAgent("c")
            .setTimeout(timeout)
            .setRefreshDelay(Duration.ZERO)
            .setRefreshErrorDelay(Duration.ZERO)
            .build();
    }
}
