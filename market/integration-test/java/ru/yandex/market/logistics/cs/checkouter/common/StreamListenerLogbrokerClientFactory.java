package ru.yandex.market.logistics.cs.checkouter.common;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.consumer.BatchingStreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.SyncConsumer;
import ru.yandex.kikimr.persqueue.consumer.stream.LogbrokerStreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.stream.StreamConsumerConfig;
import ru.yandex.kikimr.persqueue.consumer.sync.SyncConsumerConfig;
import ru.yandex.kikimr.persqueue.consumer.transport.ConsumerTransport;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.kikimr.persqueue.proxy.ProxyBalancer;

public class StreamListenerLogbrokerClientFactory extends LogbrokerClientFactory {
    private final ConsumerTransport consumerTransport;

    public StreamListenerLogbrokerClientFactory(ConsumerTransport consumerTransport) {
        super((ProxyBalancer) null);
        this.consumerTransport = consumerTransport;
    }

    @Override
    public AsyncProducer asyncProducer(AsyncProducerConfig config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncProducer asyncProducer(String topic, byte[] sourceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SyncConsumer syncConsumer(SyncConsumerConfig config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SyncConsumer syncConsumer(SyncConsumerConfig config, long timeout, TimeUnit timeoutUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SyncConsumer syncConsumer(Collection<String> topics, String consumerPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StreamConsumer streamConsumer(StreamConsumerConfig config) {
        return new LogbrokerStreamConsumer(config, consumerTransport);
    }

    @Override
    public StreamConsumer streamConsumer(Collection<String> topics, String consumerPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BatchingStreamConsumer batchingStreamConsumer(StreamConsumerConfig config) {
        throw new UnsupportedOperationException();
    }
}
