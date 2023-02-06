package ru.yandex.market.crm.lb.test;

import com.google.common.collect.Multimap;

import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogBrokerMessageConsumer;
import ru.yandex.market.crm.lb.SessionFactory;
import ru.yandex.market.crm.lb.dao.PartitionDao;
import ru.yandex.market.crm.lb.impl.MessageProcessor;
import ru.yandex.market.crm.lb.impl.ReadingSession;
import ru.yandex.market.crm.lb.lock.MultiHostPartitionLocksManager;
import ru.yandex.market.crm.lb.lock.PartitionLocksManager;
import ru.yandex.market.mcrm.handshake.HandshakeService;
import ru.yandex.market.mcrm.lock.LockService;

/**
 * @author apershukov
 */
public class ReadingSessionStubFactory implements SessionFactory {

    private final HandshakeService handshakeService;
    private final LockService lockService;
    private final PartitionDao partitionDao;
    private final StreamConsumerStub streamConsumer;

    public ReadingSessionStubFactory(HandshakeService handshakeService,
                                     LockService lockService,
                                     PartitionDao partitionDao,
                                     StreamConsumerStub streamConsumer) {
        this.handshakeService = handshakeService;
        this.lockService = lockService;
        this.partitionDao = partitionDao;
        this.streamConsumer = streamConsumer;
    }

    @Override
    public ReadingSession create(String topic,
                                 String clientId,
                                 Multimap<String, LogBrokerMessageConsumer> consumers,
                                 MessageProcessor messageProcessor) {
        PartitionLocksManager partitionLocksManager = new MultiHostPartitionLocksManager(lockService, clientId);

        return new ReadingSession(
                messageProcessor,
                handshakeService,
                partitionLocksManager,
                partitionDao,
                consumers,
                streamConsumer,
                clientId,
                topic,
                LBInstallation.LOGBROKER
        );
    }
}
