package ru.yandex.market.tsum.clients.balancer;

import com.google.gson.JsonObject;
import org.eclipse.egit.github.core.client.GsonUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BalancerApiRequestTest {
    @Test
    public void testAddSimple() throws Exception {
        BalancerApiRequest req = BalancerApiRequest.builder().setAction(BalancerApiAction.ADD).setVhost("market.new" +
            ".hostname").setHost("pepelac01ht.market.yandex.net").setPort(443).build();
        String expectedJsonString = "{" +
            "\"vhost\":\"market.new.hostname\"," +
            "\"host\": \"pepelac01ht.market.yandex.net\", " +
            "\"port\": 443" +
            "}";
        assertEquals(GsonUtils.fromJson(expectedJsonString, JsonObject.class), req.toJson());
    }

    @Test
    public void testAddExtended() throws Exception {
        BalancerApiRequest req = BalancerApiRequest.builder()
            .setAction(BalancerApiAction.ADD)
            .setVhost("market.new.hostname")
            .setHost("pepelac01ht.market.yandex.net")
            .setPort(443)
            .setServiceName(BalancerApiService.PARTNER)
            .setBackendProto(BalancerBackendProto.HTTP)
            .build();
        String expectedJsonString = "{" +
            "\"vhost\":\"market.new.hostname\"," +
            "\"host\": \"pepelac01ht.market.yandex.net\", " +
            "\"backend_proto\": \"http\"," +
            "\"service_name\": \"partner\"," +
            "\"port\": 443" +
            "}";
        assertEquals(GsonUtils.fromJson(expectedJsonString, JsonObject.class), req.toJson());
    }

    @Test
    public void testDelete() throws Exception {
        BalancerApiRequest req = BalancerApiRequest.builder().setAction(BalancerApiAction.DELETE).setVhost("market" +
            ".new.hostname").build();
        String expectedJsonString = "{" +
            "\"vhost\":\"market.new.hostname\"" +
            "}";
        assertEquals(GsonUtils.fromJson(expectedJsonString, JsonObject.class), req.toJson());
    }

}
