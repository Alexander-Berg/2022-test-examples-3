package ru.yandex.market.mbo.common.logbroker;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class AsyncProducerMock implements AsyncProducer {

    private ProducerInitResponse initResponse;
    private Map<Long, ProducerWriteResponse> mockResponses = new HashMap<>();
    private Set<Long> throwOnSeqNo = new HashSet<>();

    public void mockInitResponse(long startSeqNo, String topic) {
        this.initResponse = new ProducerInitResponse(startSeqNo, topic, 0, null);
    }

    public void mockWriteResponse(ProducerWriteResponse response) {
        mockResponses.put(response.getSeqNo(), response);
    }

    public void mockFailWithExceptionOnSeqNo(long seqNo) {
        throwOnSeqNo.add(seqNo);
    }

    @Override
    public CompletableFuture<ProducerInitResponse> init() {
        return CompletableFuture.completedFuture(initResponse);
    }

    @Override
    public CompletableFuture<ProducerWriteResponse> write(byte[] data, long seqNo, long timestamp) {
        if (throwOnSeqNo.contains(seqNo)) {
            throw new RuntimeException("Expected exception.");
        }
        return CompletableFuture.completedFuture(
            mockResponses.getOrDefault(seqNo, new ProducerWriteResponse(seqNo, 0, false))
        );
    }

    @Override
    public CompletableFuture<Void> closeFuture() {
        return null;
    }

    @Override
    public void close() {

    }
}
