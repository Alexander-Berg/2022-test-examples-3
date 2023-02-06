package ru.yandex.market.grade.statica.client;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.grade.statica.client.PersStaticUtil.OPINIONS_GET_BATCH;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 25.03.2021
 */
public class PersStaticUtilTest {

    @Test
    public void testStaticLoadBatched() {
        checkStaticLoadBatched(OPINIONS_GET_BATCH);
        checkStaticLoadBatched(OPINIONS_GET_BATCH + 1);
        checkStaticLoadBatched(OPINIONS_GET_BATCH - 1);
        checkStaticLoadBatched(OPINIONS_GET_BATCH + 3);
    }

    private void checkStaticLoadBatched(long batchSize) {
        PersStaticClient persStaticClient = mock(PersStaticClient.class);

        List<Long> allItems = LongStream.range(0, batchSize).boxed()
            .collect(Collectors.toList());

        when(persStaticClient.getModelOpinionsCountBulk(anyList())).then(invocation -> {
            List<Long> items = invocation.getArgument(0);
            return items.stream().collect(Collectors.toMap(x -> x, x -> x * 2));
        });

        Map<Long, Long> result = PersStaticUtil.getModelOpinionsCountBulkBatched(persStaticClient, allItems);
        assertEquals(allItems.size(), result.size());
        result.forEach((modelId, count) -> {
            assertEquals(modelId * 2, count);
        });

        int callCount = (int) batchSize / OPINIONS_GET_BATCH + (batchSize % OPINIONS_GET_BATCH > 0 ? 1 : 0);
        verify(persStaticClient, times(callCount))
            .getModelOpinionsCountBulk(anyList());
    }

}
