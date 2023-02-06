package ru.yandex.direct.ess.router.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.ess.router.configuration.commandline.LogbrokerParams;
import ru.yandex.direct.ess.router.models.EssWorkMode;

import static org.assertj.core.api.Assertions.assertThat;

class ShardUtilsTest {

    @Test
    void testGetShards_SpecifiedShards() {

        List<Integer> totalShards = IntStream.rangeClosed(1, 17).boxed().collect(Collectors.toList());

        LogbrokerParams logbrokerParams = getLogbrokerParams(Arrays.asList(16, 17, 18));
        Collection<Integer> shards = getShards(logbrokerParams, null, totalShards);
        assertThat(shards).isEqualTo(Stream.of(16, 17).collect(Collectors.toList()));

        logbrokerParams = getLogbrokerParams(Arrays.asList(234, 2346, 2342348));
        shards = getShards(logbrokerParams, null, totalShards);
        assertThat(shards).isEmpty();
    }

    @Test
    void testGetShards_SpecifiedChunks() {
        List<Integer> totalShards = IntStream.rangeClosed(1, 17).boxed().collect(Collectors.toList());
        LogbrokerParams logbrokerParams = getLogbrokerParams(null);

        // it's hard test. when EssWorkMode.CHUNKS_COUNT will change pls update this code
        List<Integer> shards = getShards(logbrokerParams, EssWorkMode.ESS_0,
                totalShards);
        assertThat(shards).isEqualTo(List.of(4, 8, 12, 16));

        logbrokerParams = getLogbrokerParams(null);
        shards = getShards(logbrokerParams, EssWorkMode.ESS_1, totalShards);
        assertThat(shards).isEqualTo(List.of(1, 5, 9, 13, 17));

        logbrokerParams = getLogbrokerParams(null);
        shards = getShards(logbrokerParams, EssWorkMode.ESS_2, totalShards);
        assertThat(shards).isEqualTo(List.of(2, 6, 10, 14));

        logbrokerParams = getLogbrokerParams(null);
        shards = getShards(logbrokerParams, EssWorkMode.ESS_3, totalShards);
        assertThat(shards).isEqualTo(List.of(3, 7, 11, 15));
    }

    private LogbrokerParams getLogbrokerParams(List<Integer> shards) {
        LogbrokerParams logbrokerParams = new LogbrokerParams();
        logbrokerParams.partitions = shards;

        return logbrokerParams;
    }

    private List<Integer> getShards(LogbrokerParams logbrokerParams, EssWorkMode workMode, List<Integer> totalShards) {
        if (logbrokerParams.partitions != null && !logbrokerParams.partitions.isEmpty()) {
            return logbrokerParams.partitions.stream()
                    .filter(totalShards::contains)
                    .collect(Collectors.toList());
        } else {
            return workMode.getShards(totalShards);
        }
    }
}
