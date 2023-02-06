package ru.yandex.market.mboc.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * @author moskovkin@yandex-team.ru
 * @since 19.07.19
 */
public class FlushingBufferTest {

    public static final int TEST_DATA_COUNT = 10;
    public static final int FLUSH_SIZE = 3;
    private static final EnhancedRandom RANDOM = EnhancedRandomBuilder.aNewEnhancedRandom();

    @Test
    public void testProcessAllDataInCorrectOrder() {
        List<String> inData = RANDOM.objects(String.class, TEST_DATA_COUNT).collect(Collectors.toList());
        List<String> outData = new ArrayList<>();

        FlushingBuffer<String> flushingBuffer = new FlushingBuffer<>(FLUSH_SIZE, outData::addAll);
        inData.forEach(flushingBuffer);
        flushingBuffer.flush();

        Assertions.assertThat(outData).containsExactlyElementsOf(inData);
    }

    @Test
    public void testCorrectBatchSizes() {
        List<String> inData = RANDOM.objects(String.class, TEST_DATA_COUNT).collect(Collectors.toList());
        List<Integer> flushSizes = new ArrayList<>();

        FlushingBuffer<String> flushingBuffer = new FlushingBuffer<>(FLUSH_SIZE, l -> flushSizes.add(l.size()));
        inData.forEach(flushingBuffer);
        flushingBuffer.flush();

        Assertions.assertThat(flushSizes).allMatch(e -> e <= FLUSH_SIZE);
        Assertions.assertThat(flushSizes.stream()
            .filter(e -> e < FLUSH_SIZE)
            .collect(Collectors.toList()).size()
        ).isLessThanOrEqualTo(1);
    }
}
