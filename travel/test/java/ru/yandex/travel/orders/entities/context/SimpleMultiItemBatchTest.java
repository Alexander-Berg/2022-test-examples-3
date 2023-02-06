package ru.yandex.travel.orders.entities.context;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.orders.entities.context.SimpleMultiItemBatchTaskState.COMPLETED;
import static ru.yandex.travel.orders.entities.context.SimpleMultiItemBatchTaskState.IN_PROGRESS;

public class SimpleMultiItemBatchTest {
    @Test
    public void testSerialization() throws Exception {
        SimpleMultiItemBatch batch = new SimpleMultiItemBatch("testBatch");
        batch.addTask(UUID.fromString("0-0-0-0-1"), IN_PROGRESS);
        batch.addTask(UUID.fromString("0-0-0-0-2"), COMPLETED);

        ObjectMapper mapper = new ObjectMapper();
        String serialized = mapper.writeValueAsString(batch);

        // no redundant fields like "empty"
        assertThat(Sets.newHashSet(mapper.readTree(serialized).fieldNames()))
                .isEqualTo(Set.of("name", "states"));

        SimpleMultiItemBatch restoredBatch = mapper.readValue(serialized, SimpleMultiItemBatch.class);

        assertThat(restoredBatch.isEmpty()).isFalse();
        assertThat(restoredBatch).isEqualTo(batch);

        restoredBatch.changeTaskState(UUID.fromString("0-0-0-0-1"), IN_PROGRESS, COMPLETED);
        assertThat(restoredBatch.allTasksInState(COMPLETED)).isTrue();
    }
}
