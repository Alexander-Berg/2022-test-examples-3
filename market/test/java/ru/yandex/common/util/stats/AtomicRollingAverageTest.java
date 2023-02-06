package ru.yandex.common.util.stats;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AtomicRollingAverageTest {
    @Test
    public void lifecycle() {
        AtomicRollingAverage avg = new AtomicRollingAverage();
        assertThat(avg.average()).isEqualTo(0.0);
        assertThat(avg.count()).isEqualTo(0L);
        assertThat(avg.sum()).isEqualTo(0L);

        avg.register(1L);
        assertThat(avg.average()).isEqualTo(1.0);
        assertThat(avg.count()).isEqualTo(1L);
        assertThat(avg.sum()).isEqualTo(1L);

        avg.register(2L);
        assertThat(avg.average()).isEqualTo(1.5);
        assertThat(avg.count()).isEqualTo(2L);
        assertThat(avg.sum()).isEqualTo(3L);
    }
}
