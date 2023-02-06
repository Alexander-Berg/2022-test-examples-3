package ru.yandex.market.mbo.cardrender.app.service;

import java.util.concurrent.atomic.AtomicReference;

import org.mockito.Mockito;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.market.mbo.cardrender.app.model.datacamp.DatacampLogbrokerEvent;
import ru.yandex.market.mbo.common.logbroker.LogbrokerProducerService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

/**
 * @author apluhin
 * @created 1/18/22
 */
public class DatacampServiceMockLb extends DatacampService {

    public LogbrokerProducerService currentProducer;

    public DatacampServiceMockLb(LogbrokerClientFactory logbrokerClientFactory,
                                 AsyncProducerConfig renderModelProducerConfig,
                                 StorageKeyValueService storageKeyValueService) {
        super(logbrokerClientFactory, renderModelProducerConfig, storageKeyValueService);
    }

    @Override
    protected void setNewProducer(AtomicReference<LogbrokerProducerService<DatacampLogbrokerEvent>> producer) {
        currentProducer = Mockito.mock(LogbrokerProducerService.class);
        producer.set(currentProducer);
    }
}
