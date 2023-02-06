package ru.yandex.market.global.index.iterator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import util.RandomDataGenerator;

public class BatchIteratorTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(BatchIteratorTest.class).build();
    private static final List<String> STRINGS = RANDOM.objects(String.class, 100).collect(Collectors.toList());

    private BatchedIterator.Batch<String> getBatch(String position, int size) {
        int pos = position != null ? Integer.parseInt(position) : 0;
        int sizeAvailable = Math.min(size, STRINGS.size() - pos);

        if (pos >= STRINGS.size()) {
            throw new RuntimeException("Incorrect position");
        }

        int nextPos = pos + sizeAvailable;
        String nextPosition = nextPos < STRINGS.size()
                ? String.valueOf(nextPos)
                : BatchedIterator.END_OF_INPUT;

        return new BatchedIterator.Batch<String>()
                .setItems(STRINGS.subList(pos, nextPos))
                .setPosition(nextPosition);
    }

    @Test
    public void testIterateAll() {
        BatchedIterator<String> batchedIterator = new BatchedIterator<>(51) {
            @Override
            protected Batch<String> loadNextBatch(String position, int batchSize) {
                return BatchIteratorTest.this.getBatch(position, batchSize);
            }
        };

        List<String> iterated = new ArrayList<>();
        while (batchedIterator.hasNext()) {
            iterated.add(batchedIterator.next());
        }

        Assertions.assertThat(iterated).containsExactlyElementsOf(STRINGS);
    }
}
