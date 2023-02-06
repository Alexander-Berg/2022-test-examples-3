package ru.yandex.market.crm.lb.test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.google.common.collect.Sets;

import ru.yandex.market.crm.lb.LogBrokerMessageConsumer;
import ru.yandex.market.crm.lb.LogIdentifier;

/**
 * @author apershukov
 */
public class LBConsumerStub implements LogBrokerMessageConsumer<byte[]> {

    private final String id;
    private final Set<LogIdentifier> logIdentifiers;
    private final BlockingQueue<byte[]> consumedBatches;

    public LBConsumerStub(String id, LogIdentifier... logIdentifiers) {
        this.id = id;
        this.logIdentifiers = Sets.newHashSet(logIdentifiers);
        this.consumedBatches = new LinkedBlockingQueue<>();
    }

    public LBConsumerStub(LogIdentifier... logIdentifiers) {
        this("consumer-stub", logIdentifiers);
    }

    @Override
    public void accept(List<byte[]> messages) {
        for (byte[] message : messages) {
            consumedBatches.offer(message);
        }
    }

    @Override
    public List<byte[]> transform(byte[] message) {
        return Collections.singletonList(message);
    }

    @Nonnull
    @Override
    public Set<LogIdentifier> getLogIdentifiers() {
        return logIdentifiers;
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    public BlockingQueue<byte[]> getConsumedBatches() {
        return consumedBatches;
    }

    public byte[] pollConsumedBatch() throws InterruptedException {
        return consumedBatches.poll(10, TimeUnit.SECONDS);
    }

    public byte[] pollConsumedBatch(long timeout, TimeUnit unit) throws InterruptedException {
        return consumedBatches.poll(timeout, unit);
    }
}
