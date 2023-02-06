package ru.yandex.chemodan.balanceclient.model;

import java.util.Map;

import org.junit.Test;

import ru.yandex.chemodan.balanceclient.model.request.PayRequestRequest;
import ru.yandex.misc.test.Assert;

public class PayRequestRequestTest {
    @Test
    public void asArray__emptyPayload() {
        PayRequestRequest request = new PayRequestRequest()
                .withOperatorUid(123L);

        Map<String, String> map = (Map<String, String>) request.asArray()[1];
        Assert.isFalse(map.containsKey("Payload"));
    }

    @Test
    public void asArray__onlyAddPayloadKeys() {
        PayRequestRequest request = new PayRequestRequest()
                .withOperatorUid(123L)
                .addToPayload("payloadKey1", "payloadValue1");

        Map<String, String> map = (Map<String, String>) request.asArray()[1];
        Assert.equals(map.get("Payload"), "{\"payloadKey1\":\"payloadValue1\"}");
    }

    @Test
    public void asArray__onlyWithPayload() {
        PayRequestRequest request = new PayRequestRequest()
                .withOperatorUid(123L)
                .withPayload("{\"payloadKey0\":\"payloadValue0\"}");

        Map<String, String> map = (Map<String, String>) request.asArray()[1];
        Assert.equals(map.get("Payload"), "{\"payloadKey0\":\"payloadValue0\"}");
    }

    @Test
    public void asArray__withPayloadDistinctKeys() {
        PayRequestRequest request = new PayRequestRequest()
                .withOperatorUid(123L)
                .withPayload("{\"payloadKey0\":\"payloadValue0\"}")
                .addToPayload("payloadKey1", "payloadValue1");

        Map<String, String> map = (Map<String, String>) request.asArray()[1];
        Assert.equals(map.get("Payload"), "{\"payloadKey0\":\"payloadValue0\",\"payloadKey1\":\"payloadValue1\"}");
    }

    @Test
    public void asArray__withPayloadIntersectKeys() {
        PayRequestRequest request = new PayRequestRequest()
                .withOperatorUid(123L)
                .withPayload("{\"payloadKey0\":\"payloadValue0\",\"payloadKey2\":\"payloadValue2\"}")
                .addToPayload("payloadKey0", "redefinePayloadValue0")
                .addToPayload("payloadKey1", "payloadValue1");

        Map<String, String> map = (Map<String, String>) request.asArray()[1];
        Assert.equals(map.get("Payload"),
                "{\"payloadKey0\":\"payloadValue0\",\"payloadKey2\":\"payloadValue2\"," +
                        "\"payloadKey1\":\"payloadValue1\"}");
    }
}
