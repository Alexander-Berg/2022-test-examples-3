package ru.yandex.market.clickphite.solomon;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * @author Aleksei Malygin <a href="mailto:Malygin-Me@yandex-team.ru"></a>
 * Date: 2018-12-14
 */
public class SolomonClientTest {
    @Test
    public void solomonIdLimitExceeded() {
        SolomonClient client = new SolomonClient("", "", 0);
        String toExceedLimitService = "market-mbi--channel-advisor-integration";
        String okService = "good-id";

        int shardIdLengthLimit = SolomonClient.SOLOMON_SHARD_ID_LENGTH_LIMIT;
        Assert.assertTrue(client.getShardId("market-tst", "stable", toExceedLimitService).length() < shardIdLengthLimit);
        Assert.assertTrue(client.getShardId("market-tst", "stable", okService).length() < shardIdLengthLimit);
    }

    @Test
    public void pushRequestBuilderTest() throws Exception {
        String testData = "Èg,-/5;";
        StringEntity entity = SolomonClient.buildPushRequestEntity(testData);
        StringWriter writer = new StringWriter();
        IOUtils.copy(entity.getContent(), writer, StandardCharsets.UTF_8);
        String outData = writer.toString();
        Assert.assertEquals(testData, outData);
    }
}
