package ru.yandex.direct.core.entity.bs.export.model;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerSpecTest {
    private static final int MIN_TABLE_VALUE = 0;
    private static final int MAX_TABLE_VALUE = 255;

    private static final Map<Long, WorkerSpec> specById = new HashMap<>();

    @BeforeAll
    static void fillPriorityMap() {
        for (WorkerSpec spec : WorkerSpec.values()) {
            specById.putIfAbsent(spec.getWorkerId(), spec);
        }
    }

    /**
     * В таблице {@link ru.yandex.direct.dbschema.ppc.tables.BsExportQueue} для par_id используется TINYINT unsigned
     */
    @ParameterizedTest
    @EnumSource(value = WorkerSpec.class)
    void checkWorkerIdForDbRestrictions(WorkerSpec spec) {
        assertThat(spec.getWorkerId())
                .isGreaterThanOrEqualTo(MIN_TABLE_VALUE)
                .isLessThanOrEqualTo(MAX_TABLE_VALUE);
    }

    @ParameterizedTest
    @EnumSource(value = WorkerSpec.class)
    void checkWorkerIdUniqueness(WorkerSpec spec) {
        WorkerSpec first = specById.get(spec.getWorkerId());
        assertThat(specById)
                .withFailMessage("workerId должен быть уникален (совпадает с %s)", first)
                .containsValue(spec);
    }
}
