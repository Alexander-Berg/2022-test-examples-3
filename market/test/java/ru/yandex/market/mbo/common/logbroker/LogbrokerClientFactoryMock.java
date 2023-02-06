package ru.yandex.market.mbo.common.logbroker;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.kikimr.persqueue.proxy.ProxyBalancer;

public class LogbrokerClientFactoryMock extends LogbrokerClientFactory {

    private AsyncProducerMock asyncProducer;

    public LogbrokerClientFactoryMock(AsyncProducerMock asyncProducer) {
        super((ProxyBalancer) null);
        this.asyncProducer = asyncProducer;
    }

    @Override
    public AsyncProducer asyncProducer(AsyncProducerConfig config) {
        return asyncProducer;
    }
}
