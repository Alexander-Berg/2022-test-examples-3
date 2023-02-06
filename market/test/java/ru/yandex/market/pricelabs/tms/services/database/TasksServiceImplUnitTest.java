package ru.yandex.market.pricelabs.tms.services.database;

import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.pricelabs.services.database.model.JobType;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class TasksServiceImplUnitTest {

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("tests")
    void convertLimitMapToArray(Map<JobType, Integer> limits, int[] expect) {
        assertArrayEquals(expect, TasksServiceImpl.convertLimitMapToArray(asObjectMap(limits)));
    }

    static Object[][] tests() {
        return new Object[][]{
                {Map.of(), new int[0]},
                {Map.of(JobType.SHOP_LOOP_FULL, 0), new int[]{-1, 0}},
                {Map.of(JobType.SHOP_LOOP_FULL, -1), new int[]{-1, 0}},
                {Map.of(JobType.SHOP_LOOP_FULL, 1), new int[]{-1, 1}},
                {Map.of(JobType.SHOP_LOOP_FULL, 99), new int[]{-1, 99}}
        };
    }

    public static Object2IntMap<JobType> asObjectMap(Map<JobType, Integer> map) {
        Object2IntMap<JobType> ret = new Object2IntOpenHashMap<>(map.size());
        for (var e : map.entrySet()) {
            ret.put(e.getKey(), e.getValue().intValue());
        }
        return ret;
    }
}
