package ru.yandex.market.tsum.tms.tasks.aas;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.tsum.clients.dbaas.DbaasApiClient;
import ru.yandex.market.tsum.clients.dbaas.DbaasQuota;

import java.util.regex.Pattern;

public class DbaasQuotaCountingCronTaskTest {

    @Test
    public void createTskvRecord() {
        String quoteAnswer = "{\n" +
            "  \"clustersQuota\": 10,\n" +
            "  \"clustersUsed\": 9,\n" +
            "  \"cpuQuota\": 20,\n" +
            "  \"cpuUsed\": 19,\n" +
            "  \"createdAt\": \"2018-07-17T13:50:29.380Z\",\n" +
            "  \"description\": \"some description\",\n" +
            "  \"id\": \"9960d9f4-22ab-4b77-8014-c40785fbbff7\",\n" +
            "  \"ioQuota\": 30,\n" +
            "  \"ioUsed\": 29,\n" +
            "  \"memoryQuota\": 40,\n" +
            "  \"memoryUsed\": 39,\n" +
            "  \"name\": \"some name\",\n" +
            "  \"networkQuota\": 50,\n" +
            "  \"networkUsed\": 49,\n" +
            "  \"spaceQuota\": 60,\n" +
            "  \"spaceUsed\": 59\n" +
            "}";
        DbaasQuota quota = DbaasApiClient.getGson().fromJson(quoteAnswer, DbaasQuota.class);
        Pattern example = Pattern.compile("tskv\tdate=.*\tprojectId=9960d9f4-22ab-4b77-8014-c40785fbbff7\t" +
            "clustersQuota=10\tclustersAllocated=9\tclustersUsed=9\t" +
            "cpuQuota=20.0\tcpuAllocated=19.0\tcpuUsed=19.0\t" +
            "ioQuota=30\tioAllocated=29\tioUsed=29\t" +
            "memoryQuota=40\tmemoryAllocated=39\tmemoryUsed=39\t" +
            "networkQuota=50\tnetworkAllocated=49\tnetworkUsed=49\t" +
            "spaceQuota=60\tspaceAllocated=59\tspaceUsed=59");
        String tskvRecord = DbaasQuotaCountingCronTask.createTskvRecord("9960d9f4-22ab-4b77-8014-c40785fbbff7", quota);
        Assert.assertTrue(example.matcher(tskvRecord).find());
    }
}