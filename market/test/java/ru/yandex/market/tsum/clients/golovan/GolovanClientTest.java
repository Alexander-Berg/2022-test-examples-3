package ru.yandex.market.tsum.clients.golovan;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;

public class GolovanClientTest {

    @Test
    public void testSerializingRequest() {
        String expexted = "[{\"name\":\"hist\",\"host\":\"CON\",\"id\":\"CON:1532597700_1532598000_300\"," +
            "\"period\":300,\"st\":1532597700,\"et\":1532598000,\"signals\":[\"itype=s3mdsstat;prj=188;" +
            "tier=mbo-miscellaneous:s3mds_bucket_stat-bucket_used_space_max\"]}]";
        Gson gson = GolovanClient.getGson();
        String hostName = "CON";
        int period = 300;
        Instant startTimeSeconds = Instant.ofEpochSecond(1532597700);
        Instant endTimeSeconds = Instant.ofEpochSecond(1532598000);
        List<String> signalsName = Collections.singletonList(
            "itype=s3mdsstat;prj=188;tier=mbo-miscellaneous:s3mds_bucket_stat-bucket_used_space_max");
        List<CtxFields> ctxList = Collections.singletonList(new CtxFields("hist", hostName, period,
            startTimeSeconds, endTimeSeconds, signalsName));
        String reqBody = gson.toJson(ctxList);
        Assert.assertEquals(reqBody, expexted);
    }
}
