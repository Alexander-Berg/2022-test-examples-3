package ru.yandex.market.crm.lb.writer.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.ProducerStreamClosedException;
import ru.yandex.market.crm.lb.writer.LbWriter;
import ru.yandex.market.crm.lb.writer.LbWriterConfig;

class LbWriterImplTest {

    public static final int ATTEMPTS = 3;
    private static final byte[] DATA = new byte[0];
    private AsyncProducer producer;

    private LbWriter writer;

    /**
     * Сессия LogBroker живёт ограниченное время:
     *
     * Если во время записи обнаружили, что сессия неактивна - закрываем текущую и открываем новую
     * @see SelfRevivingAsyncProducer
     * Под капотом вызываются методы {@link AsyncProducer#close()} и {@link AsyncProducer#init()}
     *
     * При этом, чтобы не кидать в пользователя ошибкой о неуспешной записи, пытаемся сделать несколько попыток
     * @see RetryableAsyncProducer
     */
    @Test
    void shouldWriteSuccessfullyWithRetryAndRevivingProducer() throws InterruptedException {
        producer = mockAsyncProducer(new ProducerStreamClosedException());
        writer = createWriter(ATTEMPTS, e -> e.getCause() instanceof ProducerStreamClosedException);

        //После создания producer(а) инициализировали сессию
        Mockito.verify(producer, Mockito.times(1)).init();
        Mockito.verify(producer, Mockito.times(0)).close();

        writer.write(DATA);
        //Было несколько попыток записи (последняя удачная)
        Mockito.verify(producer, Mockito.times(3)).write(DATA);

        //После каждой НЕудачной попытки пересоздавали сессию
        Mockito.verify(producer, Mockito.times(2)).close();
        Mockito.verify(producer, Mockito.times(3)).init();
    }

    @Test
    void shouldFailWhenTooFewRetryAttempts() throws InterruptedException {
        producer = mockAsyncProducer(new ProducerStreamClosedException());
        writer = createWriter(ATTEMPTS - 1, e -> e.getCause() instanceof ProducerStreamClosedException);
        Assertions.assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> writer.write(DATA))
                .withCause(new ExecutionException(new ProducerStreamClosedException()));
    }

    @Test
    void shouldNotRetryOnFailedPredicate() throws InterruptedException {
        producer = mockAsyncProducer(new ProducerStreamClosedException());
        writer = createWriter(ATTEMPTS, e -> e instanceof NullPointerException);
        Assertions.assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> writer.write(DATA))
                .withCause(new ExecutionException(new ProducerStreamClosedException()));
    }

    private LbWriter createWriter(int maxAttempts, Predicate<Throwable> retryPredicate) throws InterruptedException {
        LogbrokerClientFactory clientFactory = Mockito.mock(LogbrokerClientFactory.class);
        Mockito.when(clientFactory.asyncProducer(Mockito.any())).thenReturn(producer);
        LbWriterConfig config = LbWriterConfig.builder()
                .setMaxWriteAttempts(maxAttempts)
                .setRetryPredicate(retryPredicate)
                .build();
        LbWriterImpl writer = new LbWriterImpl(clientFactory, config, () -> null);
        writer.init();
        return writer;
    }

    private AsyncProducer mockAsyncProducer(Throwable e) {
        AsyncProducer asyncProducer = Mockito.mock(AsyncProducer.class);
        Mockito.when(asyncProducer.init())
                .thenReturn(CompletableFuture.completedFuture(null));
        Mockito.when(asyncProducer.write(DATA))
                .thenReturn(CompletableFuture.failedFuture(e))
                .thenReturn(CompletableFuture.failedFuture(e))
                .thenReturn(CompletableFuture.completedFuture(null));
        return asyncProducer;
    }
}
