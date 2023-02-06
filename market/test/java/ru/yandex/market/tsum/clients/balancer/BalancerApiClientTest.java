package ru.yandex.market.tsum.clients.balancer;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.request.netty.HttpClientConfig;

import static org.junit.Assert.assertEquals;

public class BalancerApiClientTest {
    @Ignore
    @Test
    public void testWithAliveBackend() throws Exception {
        BalancerApiRequest req = BalancerApiRequest.builder().setAction(BalancerApiAction.ADD).setVhost("market.new" +
            ".hostname").setHost("pepelac01ht.market.yandex.net").setPort(443).build();
        HttpClientConfig config = new HttpClientConfig();
        BalancerApiClient cli = new BalancerApiClient("http://balancer-api.tst.vs.market.yandex.net:4242/");
        String res = cli.runRequest(req).get("status").toString();
        assertEquals("\"OK\"", res);
    }

}
