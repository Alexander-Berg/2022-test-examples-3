package ru.yandex.market.tsum.clients.kombat;

import java.util.List;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.junit.Assert;
import org.junit.Test;

public class KombatClientMockedTest {
    private static final Gson GSON = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();

    @Test
    public void testReportBatchResponseDeserializesCorrectly() {
        var collectionType = new TypeToken<List<Battle>>() {
        }.getType();
        List<Battle> battles = GSON.fromJson(
            "[{\"id\":\"1\", \"maxrps\":false, \"name\":\"MAIN\"},{\"id\":\"2\",\"maxrps\":true," +
                "\"name\":\"MAIN@prime\"}]",
            collectionType
        );

        var response = new TestReportBatchResponse(battles);
        List<Battle> responseBattles = response.getBattles();

        Assert.assertEquals(responseBattles.size(), 2);

        var b1 = responseBattles.get(0);
        Assert.assertNotNull(b1);
        Assert.assertEquals("1", b1.getId());
        Assert.assertEquals("MAIN", b1.getName());
        Assert.assertEquals(false, b1.getMaxRps());

        var b2 = responseBattles.get(1);
        Assert.assertNotNull(b2);
        Assert.assertEquals("2", b2.getId());
        Assert.assertEquals("MAIN@prime", b2.getName());
        Assert.assertEquals(true, b2.getMaxRps());
    }

    @Test
    public void testReportResultResponseDeserializesCorrectly() {
        TestReportResultResponse response = GSON.fromJson(
            "{\"anon_mem\":-132132,\"status\":\"complete\",\"time\":77}",
            TestReportResultResponse.class
        );

        Assert.assertTrue(response.isReady());
        Assert.assertNull(response.getError());
        Assert.assertEquals(0, response.getProgress());
    }
}
