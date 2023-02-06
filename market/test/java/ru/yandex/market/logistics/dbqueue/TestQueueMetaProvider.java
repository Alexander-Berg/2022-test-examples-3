package ru.yandex.market.logistics.dbqueue;

import java.util.Arrays;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.logistics.dbqueue.domain.SimpleQueueMetaProvider;
import ru.yandex.market.logistics.dbqueue.dto.QueueMeta;

@ParametersAreNonnullByDefault
public class TestQueueMetaProvider extends SimpleQueueMetaProvider {
    public TestQueueMetaProvider() {
        super(Arrays.asList(
            new QueueMeta("test.queue.1", "Queue 1", 2),
            new QueueMeta("test.queue.2", "Queue 2", 0)
        ));
    }
}
