package ru.yandex.market.logistics.iris.repository;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.jobs.model.QueueType;
import ru.yandex.market.logistics.iris.model.MessagesAmountInformation;
import ru.yandex.market.logistics.iris.model.MessagesAttemptNumberInformation;
import ru.yandex.misc.test.Assert;

class ReplicaMonitoringRepositoryTest {
    @Autowired
    ReplicaMonitoringRepository replicaMonitoringRepository;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/queue/4.xml")
    public void shouldFindMessageAmountOfDifferentTypesAndEnrichWithAbsentTypes() {
        List<MessagesAmountInformation> actualMessagesAmountFullInformation = replicaMonitoringRepository.findMessagesAmountFullInformation();

        Assert.assertEquals(9, actualMessagesAmountFullInformation.size());

        Assert.assertContains(actualMessagesAmountFullInformation, new MessagesAmountInformation(QueueType.REFERENCE_SYNC.name(), 2));
        Assert.assertContains(actualMessagesAmountFullInformation, new MessagesAmountInformation(QueueType.SYNC_STOCKS_BY_LIFETIME.name(), 2));
        Assert.assertContains(actualMessagesAmountFullInformation, new MessagesAmountInformation(QueueType.REFERENCE_INDEX_MERGE.name()));
        Assert.assertContains(actualMessagesAmountFullInformation, new MessagesAmountInformation(QueueType.ITEM_PUBLISH.name()));
        Assert.assertContains(actualMessagesAmountFullInformation, new MessagesAmountInformation("UNKNOWN", 1));
        Assert.assertContains(actualMessagesAmountFullInformation, new MessagesAmountInformation(QueueType.COMPLETE_ITEM_PUBLISH.name()));
        Assert.assertContains(actualMessagesAmountFullInformation, new MessagesAmountInformation(QueueType.CONTENT_SYNC_OF_SSKUS_WITHOUT_MSKU.name()));
        Assert.assertContains(actualMessagesAmountFullInformation, new MessagesAmountInformation(QueueType.CONTENT_FULL_SYNC.name()));
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/queue/5.xml")
    public void shouldFindMessagesAttemptNumberFullInformation() {
        List<MessagesAttemptNumberInformation> actualMessagesAttemptNumberFullInformation =
                replicaMonitoringRepository.findMessagesAttemptNumberFullInformation();


        Assert.assertEquals(3, actualMessagesAttemptNumberFullInformation.size());

        Assert.assertContains(actualMessagesAttemptNumberFullInformation, new MessagesAttemptNumberInformation(QueueType.REFERENCE_SYNC.name(), 1, 0));
        Assert.assertContains(actualMessagesAttemptNumberFullInformation, new MessagesAttemptNumberInformation(QueueType.SYNC_STOCKS_BY_LIFETIME.name(), 0, 0));
        Assert.assertContains(actualMessagesAttemptNumberFullInformation, new MessagesAttemptNumberInformation(QueueType.ITEM_PUBLISH.name(), 1, 1));
    }
}
