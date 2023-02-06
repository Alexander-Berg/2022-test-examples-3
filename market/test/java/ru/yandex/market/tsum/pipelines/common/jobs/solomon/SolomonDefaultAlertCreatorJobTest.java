package ru.yandex.market.tsum.pipelines.common.jobs.solomon;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.clients.solomon.models.SolomonAlert;
import ru.yandex.market.tsum.clients.solomon.models.SolomonAssociatedChannel;

public class SolomonDefaultAlertCreatorJobTest {
    private static final Gson GSON = new GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX")
        .create();

    @Test
    public void createBadRpsAlertTest() throws IOException {
        SolomonDefaultAlertCreatorJob solomonDefaultAlertCreatorJob = new SolomonDefaultAlertCreatorJob();

        SolomonAlert solomonAlert = solomonDefaultAlertCreatorJob.createBadRpsAlert(
            "testing",
            "market-Dyno",
            "dyno_auto-graph",
            10,
            300,
            150,
            "dyno",
            "vladimirlevin",
            List.of(new SolomonAssociatedChannel("juggler_spok")),
            "5xx"
        );

        JsonElement expected = GSON.fromJson(
            IOUtils.toString(
                this.getClass().getResourceAsStream(
                    "/alertCreatorTests/SolomonDefaultAlertCreatorJob-bad_rps-tets.json"
                ),
                StandardCharsets.UTF_8
            ),
            JsonElement.class
        );

        Assert.assertEquals(expected, GSON.toJsonTree(solomonAlert));
    }

    @Test
    public void createSolomonQuotaAlertTest() throws IOException {
        SolomonDefaultAlertCreatorJob solomonDefaultAlertCreatorJob = new SolomonDefaultAlertCreatorJob();

        SolomonAlert solomonAlert = solomonDefaultAlertCreatorJob.createSolomonQuotaAlert(
            "testing",
            "market-Dyno",
            "dyno_auto-graph",
            95,
            85,
            "dyno",
            "vladimirlevin",
            List.of(new SolomonAssociatedChannel("juggler_spok"))
        );

        JsonElement expected = GSON.fromJson(
            IOUtils.toString(
                this.getClass().getResourceAsStream(
                    "/alertCreatorTests/SolomonDefaultAlertCreatorJob-solomon_quota-tets.json"
                ),
                StandardCharsets.UTF_8
            ),
            JsonElement.class
        );

        Assert.assertEquals(expected, GSON.toJsonTree(solomonAlert));
    }

}
