package ru.yandex.market.passport.blackbox;

import org.apache.http.client.HttpClient;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;

/**
 * @author anmalysh
 * @since 10/26/2018
 */
public class Blacbox2BuilderTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private Timeout timeout = Timeout.seconds(1);
    private HttpClient client = ApacheHttpClientUtils.singleConnectionClient(timeout);

    @Mock
    private Tvm2 tvm2;

    @Test(expected = IllegalStateException.class)
    public void testNoUrlFails() {
        new Blackbox2Builder()
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testClientWithTimeoutFails() {
        new Blackbox2Builder()
            .setUrl("a")
            .setHttpClient(client)
            .setTimeout(timeout)
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testClientWithUserAgentFails() {
        new Blackbox2Builder()
            .setUrl("a")
            .setHttpClient(client)
            .setUserAgent("b")
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testClientWithMaxConnectionsFails() {
        new Blackbox2Builder()
            .setUrl("a")
            .setHttpClient(client)
            .setMaxConnections(1)
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testClientWithTvm2Fails() {
        new Blackbox2Builder()
            .setUrl("a")
            .setHttpClient(client)
            .setTvm(tvm2)
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testTvmWithoutServiceIdFails() {
        new Blackbox2Builder()
            .setUrl("a")
            .setTvm(tvm2)
            .build();
    }

    @Test
    public void testMinimalPasses() {
        new Blackbox2Builder()
            .setUrl("a")
            .setBlackboxServiceId(1)
            .build();
    }

    @Test
    public void testHttpClientPasses() {
        new Blackbox2Builder()
            .setUrl("a")
            .setHttpClient(client)
            .setBlackboxServiceId(1)
            .setRetries(2)
            .build();
    }

    @Test
    public void testNoHttpClientPasses() {
        new Blackbox2Builder()
            .setUrl("a")
            .setMaxConnections(1)
            .setUserAgent("c")
            .setTimeout(timeout)
            .setTvm(tvm2)
            .setBlackboxServiceId(2)
            .setRetries(1)
            .build();
    }
}
