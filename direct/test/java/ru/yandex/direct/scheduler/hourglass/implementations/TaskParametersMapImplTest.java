package ru.yandex.direct.scheduler.hourglass.implementations;

import org.junit.Test;

import ru.yandex.direct.scheduler.hourglass.TaskParametersMap;

import static org.junit.Assert.assertEquals;

public class TaskParametersMapImplTest {

    @Test
    public void internalToString() {

        var taskParametersMap = TaskParametersMap.of("a", "a",
                "z", "z",
                "b", "b",
                "-1", "mississippi",
                "shard_param", "21");

        var paramAsString = taskParametersMap.getAsString();
        assertEquals(paramAsString, "{\"-1\":\"mississippi\",\"a\":\"a\",\"b\":\"b\",\"shard_param\":\"21\"," +
                "\"z\":\"z\"}");
        var mapFromString = TaskParametersMapImpl.fromString(paramAsString);
        assertEquals(taskParametersMap, mapFromString);

    }

    @Test
    public void emptyMap() {
        TaskParametersMap taskParametersMap = TaskParametersMap.of();
        var paramAsString = taskParametersMap.getAsString();
        assertEquals(paramAsString, "{}");
        var mapFromString = TaskParametersMapImpl.fromString(paramAsString);
        assertEquals(taskParametersMap, mapFromString);

        assertEquals(taskParametersMap, mapFromString);
    }
}
