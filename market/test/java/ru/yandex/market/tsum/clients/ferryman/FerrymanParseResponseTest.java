package ru.yandex.market.tsum.clients.ferryman;

import java.io.IOException;

import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.clients.ferryman.model.BatchIdResponse;
import ru.yandex.market.tsum.clients.ferryman.model.BatchStatus;
import ru.yandex.market.tsum.clients.ferryman.model.BatchStatusResponse;

public class FerrymanParseResponseTest {
    private static final Gson GSON = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();

    @Test
    public void testBatchIdResponseDeserializeCorrectly() throws IOException {
        BatchIdResponse response = GSON.fromJson(
            getTestResourceAsString("clients/ferryman/batch_id_response.json"), BatchIdResponse.class);
        Assert.assertEquals("12345", response.getBatchId());
    }

    @Test
    public void testBatchStatusResponseDeserializeCorrectly() throws IOException {
        BatchStatusResponse response;

        response = GSON.fromJson(
            getTestResourceAsString("clients/ferryman/batch_status_final_response.json"),
            BatchStatusResponse.class);
        Assert.assertEquals(BatchStatus.FINAL, response.getBatchStatus());

        response = GSON.fromJson(
            getTestResourceAsString("clients/ferryman/batch_status_unknown_response.json"),
            BatchStatusResponse.class);
        Assert.assertNull(response.getBatchStatus());
    }

    private String getTestResourceAsString(String resourceName) throws IOException {
        return Resources.toString(Resources.getResource(resourceName), Charsets.UTF_8);
    }
}
