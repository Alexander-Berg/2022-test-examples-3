package ru.yandex.market.razladki;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 24/08/15
 */
@Ignore
public class RazladkiClientTest {

    private static final Logger log = LogManager.getLogger();
    private HttpClient httpClient = HttpClientBuilder.create().build();

    @Test
    public void testSendData() throws Exception {
//        String metric = "one_min.money.mstat.clicks.count.TOTAL";
        String metric = "one_min.money.mstat.clicks.chips.TOTAL";
//        String metric = "five_min.money.mstat.clicks.chips.TOTAL";
//        String metric = "five_min.money.mstat.clicks.count.TOTAL";
        List<RazladkiParam> params = getParams(metric);

        RazladkiClient client = new RazladkiClient();
        client.setProject("market");
//        client.setProject("xxx4");
        client.setLauncherUrl("http://launcher.razladki.yandex-team.ru/");
//        client.setLauncherUrl("http://launcher.razladki-dev.yandex-team.ru/");
        client.afterPropertiesSet();
        int batchSize = 10000;
        for (int i = 0; i < params.size(); i += batchSize) {
            List<RazladkiParam> batch = params.subList(i, Math.min(i + batchSize, params.size()));
            try {
                client.sendData(batch);
            } catch (IOException e) {
                log.error(e);
            }
        }


        System.gc();

    }

    private List<RazladkiParam> getParams(String metric) throws IOException {


        /*


        https://market-graphite.yandex-team.ru/render/?width=1293&height=949&_salt=1447349679.39&target=five_min
        .money.mstat.clicks.chips.TOTAL&from=00%3A00_20140521&until=23%3A59_20151111

         */
        List<RazladkiParam> result = new ArrayList<>();
        String url = "https://market-graphite.yandex-team.ru/render/?width=1287&height=949&from=00%3A00_20150825" +
            "&target=" + metric + "&until=23%3A59_20151224&format=json";

        HttpGet request = new HttpGet(url);
        HttpResponse response = httpClient.execute(request);
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(new InputStreamReader(response.getEntity().getContent()));
        JsonArray dataPoints = jsonElement.getAsJsonArray().get(0).getAsJsonObject().getAsJsonArray("datapoints");
        for (JsonElement dataPoint : dataPoints) {
            JsonArray valueTs = dataPoint.getAsJsonArray();
            if (valueTs.get(0).isJsonNull()) {
                continue;
            }
            result.add(new RazladkiParam(
                metric,
                valueTs.get(1).getAsInt(),
                valueTs.get(0).getAsFloat()
            ));
        }
        return result;

    }


}
