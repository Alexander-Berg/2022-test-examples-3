package ru.yandex.market.cleanweb.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class BaseJsonRpcSerializationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class BaseJsonRpcTest extends BaseJsonRpc {
    }

    @Test
    public void testDeserialize() throws IOException {
        BaseJsonRpc value = MAPPER.readValue(
            "  {\n" +
                "    \"jsonrpc\": \"2.0\",\n" +
                "    \"id\": 11,\n" +
                "    \"error\": {\n" +
                "      \"message\": \"FailedRequestError()\",\n" +
                "      \"code\": -32000\n" +
                "    }\n" +
                "  }\n",
            BaseJsonRpcTest.class
        );
        Assert.assertNotNull(value);
    }

    @Test
    public void testSerialize() throws IOException {
        BaseJsonRpcTest value = new BaseJsonRpcTest();
        value.id = 1;
        String valueAsString = MAPPER.writeValueAsString(value);
        Assert.assertEquals("{\"jsonrpc\":\"2.0\",\"id\":1}", valueAsString);
    }
}
