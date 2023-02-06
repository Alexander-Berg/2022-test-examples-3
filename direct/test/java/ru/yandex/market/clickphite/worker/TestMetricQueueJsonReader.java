package ru.yandex.market.clickphite.worker;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ru.yandex.market.clickphite.metric.MetricQueue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Десериализует из json-файла объект очереди.
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 21.10.17
 */
class TestMetricQueueJsonReader {
    static MetricQueue readMetricQueue(String queueFilePath) throws IOException {
        File queueFile = new File(queueFilePath);
        String queueFileContent = new String(Files.readAllBytes(queueFile.toPath()));
        JsonObject json = new Gson().fromJson(queueFileContent, JsonObject.class).get("queue").getAsJsonObject();

        SortedMap<Long, RangeSet<Integer>> diff = new TreeMap<>();
        JsonElement diffDocument = json.get("diff");
        for (Map.Entry<String, JsonElement> entry : diffDocument.getAsJsonObject().entrySet()) {
            Long timestampMillis = Long.parseLong(entry.getKey());
            diff.put(timestampMillis, toRangeSet(entry.getValue()));
        }

        return new MetricQueue(
            toRangeSet(json.get("mainRangeSet")),
            diff,
            json.get("maxProcessedTimeSeconds").getAsInt()
        );
    }

    private static RangeSet<Integer> toRangeSet(JsonElement object) {
        JsonArray ranges = object.getAsJsonArray();
        RangeSet<Integer> rangeSet = TreeRangeSet.create();
        for (JsonElement range : ranges) {
            JsonObject rangeObj = range.getAsJsonObject();
            rangeSet.add(Range.closedOpen(rangeObj.get("start").getAsInt(), rangeObj.get("end").getAsInt()));

        }
        return rangeSet;
    }
}
