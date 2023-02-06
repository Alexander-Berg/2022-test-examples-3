package ru.yandex.market.mbo.synchronizer.export;

import org.junit.Test;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.util.ExportMapReduceUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ExportMapReduceUtilTest {

    @Test
    public void testDeserializeLongSet() {
        Set<Long> desSet = ExportMapReduceUtil.deserializeSet("1,1", Long::parseLong);

        Set<Long> testSet = new HashSet<>(Arrays.asList(1L, 1L));

        assertEquals(testSet, desSet);
    }

    @Test
    public void testDeserializeStringSet() {
        Set<String> desSet = ExportMapReduceUtil.deserializeSet("Write,more,tests", String::toString);

        Set<String> testSet = new HashSet<>(Arrays.asList("Write", "more", "tests"));

        assertEquals(testSet, desSet);
    }

    @Test
    public void testSerializeMapWithSetValue() {
        Map<Long, Set<Long>> testMap = new HashMap<>();
        testMap.put(1L, new HashSet<>(Arrays.asList(1L, 2L)));
        testMap.put(2L, new HashSet<>(Arrays.asList(2L, 1L)));

        String serializedMap = ExportMapReduceUtil.serializeMapWithSetValue(testMap);

        assertEquals(serializedMap, "1:1,2;2:1,2");
    }

    @Test
    public void testDeserializeMapWithSetValue() {
        Map<Long, Set<Long>> desdMap = ExportMapReduceUtil.deserializeMapWithSetValue("1:1,2;2:2,1",
                                                                                            Long::parseLong,
                                                                                            Long::parseLong);

        Map<Long, Set<Long>> testMap = new HashMap<>();
        testMap.put(1L, new HashSet<>(Arrays.asList(1L, 2L)));
        testMap.put(2L, new HashSet<>(Arrays.asList(2L, 1L)));

        assertEquals(desdMap, testMap);
    }

    @Test
    public void testSerializeAndDeserializeSet() {
        Set<Long> testSet = new HashSet<>();
        testSet.addAll(Arrays.asList(1L, 2L));

        String serialized = ExportMapReduceUtil.serializeCollection(testSet);
        assertEquals(serialized, "1,2");

        Set<Long> desSet = ExportMapReduceUtil.deserializeSet(serialized, Long::parseLong);
        assertEquals(testSet, desSet);
    }

    @Test
    public void testSerializeAndDeserializeMap() {
        Map<String, Set<String>> testMap = new HashMap<>();
        testMap.put("Good", new HashSet<>(Arrays.asList("writing test", "review")));
        testMap.put("Bad", new HashSet<>(Arrays.asList("Spaghetti code", "rush job")));

        String serializedMap = ExportMapReduceUtil.serializeMapWithSetValue(testMap);

        Map<String, Set<String>> desdMap = ExportMapReduceUtil.deserializeMapWithSetValue(serializedMap,
                                                                                          String::toString,
                                                                                          String::toString);
        assertEquals(desdMap, testMap);
    }

}
