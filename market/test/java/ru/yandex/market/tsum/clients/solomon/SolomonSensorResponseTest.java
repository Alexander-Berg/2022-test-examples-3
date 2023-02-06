package ru.yandex.market.tsum.clients.solomon;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.clients.solomon.models.SolomonSensorResponse;

public class SolomonSensorResponseTest {

    @Test
    public void testDeserialize() {
        String responseSample = "{\n" +
            "  \"sensors\": [\n" +
            "    {\n" +
            "      \"labels\": {\n" +
            "        \"host\": \"none\",\n" +
            "        \"account\": \"market-indexer-production\",\n" +
            "        \"sensor\": \"node_count_limit\"\n" +
            "      },\n" +
            "      \"created\": \"2017-02-14T13:32:04Z\",\n" +
            "      \"deriv\": false,\n" +
            "      \"values\": [\n" +
            "        {\n" +
            "          \"ts\": \"2018-07-19T15:12:59Z\",\n" +
            "          \"value\": 60000\n" +
            "        },\n" +
            "        {\n" +
            "          \"ts\": \"2018-07-19T15:13:00Z\",\n" +
            "          \"value\": 60000\n" +
            "        },\n" +
            "        {\n" +
            "          \"ts\": \"2018-07-19T15:13:19Z\",\n" +
            "          \"value\": 60000\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        SolomonSensorResponse response = SolomonApiClient.getGson().fromJson(responseSample,
            SolomonSensorResponse.class);
        System.out.println(response);
        Assert.assertNotNull(response.getSensors());
        Assert.assertNotNull(response.getSensors().get(0).getCreated());
        Assert.assertNotNull(response.getSensors().get(0).getLabels());
        Assert.assertNotNull(response.getSensors().get(0).getValues());
        Assert.assertEquals(response.getSensors().get(0).getValues().size(), 3);
    }

}
