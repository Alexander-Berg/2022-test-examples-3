package ru.yandex.direct.ytcomponents.repository;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class YtClusterFreshnessRepositoryTest {

    @Test
    public void testStringToShard_Correct() {
        assertThat(YtClusterFreshnessRepository.dbNameToShard("ppc:123"))
                .isEqualTo(123);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testStringToShard_BadValue() {
        YtClusterFreshnessRepository.dbNameToShard("ppc");
    }
}
