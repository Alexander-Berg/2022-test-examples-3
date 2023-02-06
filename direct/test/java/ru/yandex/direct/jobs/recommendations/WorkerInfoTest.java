package ru.yandex.direct.jobs.recommendations;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkerInfoTest {

    static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {1, 1},
                {1, 99},
                {99, 1},
                {99, 99},
        });
    }

    @ParameterizedTest(name = "shard = {0}, worker = {1}")
    @MethodSource("params")
    @DisplayName("После сохранения и восстановления воркер равен исходному")
    void test(Integer shard, Integer workerNumber) {
        WorkerInfo workerInfo = new WorkerInfo(shard, workerNumber);
        WorkerInfo workerInfoRestored = WorkerInfo.valueOf(workerInfo.toString());
        assertEquals(workerInfo, workerInfoRestored);
    }
}
