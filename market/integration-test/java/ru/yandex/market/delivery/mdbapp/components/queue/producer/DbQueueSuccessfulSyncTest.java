package ru.yandex.market.delivery.mdbapp.components.queue.producer;

import org.junit.Test;

public class DbQueueSuccessfulSyncTest extends DbQueueSyncAbstractTest {

    @Test
    public void test() {
        // Проверяем, что тест верно сконфигурирован (синхронизатор работает)
        softly.assertThat(synchronizer.isShouldWait()).isTrue();

        //  Проверяем, что произошел ранний вызов enqueue()
        softly.assertThat(earlyEnqueueHappened).isTrue();

        // Проверяем, что при раннем вызове enqueue() выброшено исключение, генерируемое сихронизатором
        softly.assertThat(thrownByEarlyEnqueue).isNotNull();
        softly.assertThat(thrownByEarlyEnqueue.getClass())
            .isEqualTo(IllegalStateException.class);
        softly.assertThat(thrownByEarlyEnqueue.getMessage())
            .isEqualTo("Can't enqueue task because the queue hasn't been initialized yet");

        // Проверяем, что постановка в очередь после инициализации контекста и db-queue проходит успешно,
        // несмотря на то, что был ранний вызов enqueue()
        testSampleProducer.enqueue(enqueueParams());
    }
}
