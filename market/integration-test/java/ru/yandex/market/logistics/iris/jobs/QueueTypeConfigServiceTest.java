package ru.yandex.market.logistics.iris.jobs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.jobs.model.QueueType;

class QueueTypeConfigServiceTest extends AbstractContextualTest {
    @Autowired
    private QueueTypeConfigService queueTypeConfigService;

    @Test
    void getBatchSize() {
        for (QueueType queueType : QueueType.values()) {
            int batchSize = queueTypeConfigService.getBatchSize(queueType);

            assertions().assertThat(batchSize).isPositive();
        }
    }
}
