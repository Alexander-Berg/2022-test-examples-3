package ru.yandex.market.clickphite.onetime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"/>
 * @date 02/03/15
 */
public class ErrorsPP {

    private static final JsonParser JSON_PARSER = new JsonParser();
    /*
    4Q14
    wget "https://bsgraphite.yandex-team.ru/render?target=sum(one_min.yabs???_rt.ext_requests.market.requests)&format=json&from=1412107200&until=1420059600" -O rps.json
    wget "https://bsgraphite.yandex-team.ru/render?target=sum(one_min.yabs???_rt.errors.market_errors.errors)&format=json&from=1412107200&until=1420059600" -O errors.json

    1Q15
    wget "https://bsgraphite.yandex-team.ru/render?target=sum(one_min.yabs???_rt.ext_requests.market.requests)&format=json&from=1420059600&until=1427835600" -O rps.json
    wget "https://bsgraphite.yandex-team.ru/render?target=sum(one_min.yabs???_rt.errors.market_errors.errors)&format=json&from=1420059600&until=1427835600" -O errors.json
     */

    public static void main(String[] args) throws Exception {
        String rpsFile = "/Users/andreevdm/tmp/kpi/rps.json";
        String errorsFile = "/Users/andreevdm/tmp/kpi/errors.json";
        Map<Integer, Double> rps = readMetrics(rpsFile);
        Map<Integer, Double> errors = readMetrics(errorsFile);

        long totalRequestCount = 0;
        long totalErrorCount = 0;


        for (Map.Entry<Integer, Double> rpsEntry : rps.entrySet()) {
            Double errorsCount = errors.get(rpsEntry.getKey());
            if (errorsCount == null) {
                continue;
            }

            double requestCount = rpsEntry.getValue() * 60;
            double errorCount = errorsCount * 60;


            totalRequestCount += requestCount;
            totalErrorCount += errorCount;
        }
        totalRequestCount /= 2;
        double errorsPercent = totalErrorCount * 100.0 / totalRequestCount;
        System.out.println("Requests: " + totalRequestCount);
        System.out.println("Errors: " + totalErrorCount + " (" + errorsPercent + "%)");

    }

    private static Map<Integer, Double> readMetrics(String file) throws Exception {
        Map<Integer, Double> metrics = new HashMap<>();
        JsonObject jsonObject = JSON_PARSER.parse(new FileReader(file)).getAsJsonArray().get(0).getAsJsonObject();
        JsonArray dataPoints = jsonObject.getAsJsonArray("datapoints");
        for (JsonElement dataPointElement : dataPoints) {
            JsonArray dataPoint = dataPointElement.getAsJsonArray();
            JsonElement valueElement = dataPoint.get(0);
            if (valueElement.isJsonNull()) {
                continue;
            }
            Double value = valueElement.getAsDouble();
            Integer timestamp = dataPoint.get(1).getAsInt();
            metrics.put(timestamp, value);
        }

        return metrics;
    }
}
