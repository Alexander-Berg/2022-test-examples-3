package ru.yandex.market.delivery.mdbapp.components.queue.producer;

import org.junit.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = DbQueueFailedSyncTest.Config.class)
public class DbQueueFailedSyncTest extends DbQueueSyncAbstractTest {

    @Test
    public void test() {
        // Проверяем, что тест верно сконфигурирован (синхронизатор НЕ работает)
        softly.assertThat(synchronizer.isShouldWait()).isFalse();

        //  Проверяем, что произошел ранний вызов enqueue()
        softly.assertThat(earlyEnqueueHappened).isTrue();

        // Проверяем, что при раннем вызове enqueue() получили именно то исключение, которое нашли в логах
        // https://paste.yandex-team.ru/3515265
        checkException(thrownByEarlyEnqueue);

        // Проверяем, что постановка в очередь продолжает вызывать исключение даже после поднятия контекста приложения
        Throwable ex = getThrowable(
            () -> testSampleProducer.enqueue(enqueueParams())
        );
        // Проверяем, что это то же самое исключение
        checkException(ex);
    }

    // Переопределяем свойство, отключая тем самым синхронизацию
    @TestConfiguration
    @PropertySource("classpath:dbqueue-sync.properties")
    public static class Config {
    }
}
