package ru.yandex.market.logistics.cs.checkouter.common;

import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Preconditions;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import ru.yandex.kikimr.persqueue.consumer.transport.ConsumerMessageListener;
import ru.yandex.kikimr.persqueue.consumer.transport.ConsumerTransport;
import ru.yandex.kikimr.persqueue.consumer.transport.message.CommitMessage;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerInitResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.outbound.ConsumerInitRequest;
import ru.yandex.kikimr.persqueue.consumer.transport.message.outbound.ConsumerLockedMessage;
import ru.yandex.kikimr.persqueue.consumer.transport.message.outbound.ConsumerReadRequest;

import static org.awaitility.Awaitility.await;

@Slf4j
public class ConsumerTransportMock implements ConsumerTransport {
    private final AtomicReference<ConsumerMessageListener> listenerRef = new AtomicReference<>();
    private final Deque<ConsumerReadResponse> responseQueue = new LinkedBlockingDeque<>();
    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> scheduledFuture;

    public synchronized void start() {
        Preconditions.checkArgument(scheduledFuture == null, "transport already started");
        scheduledFuture = service.scheduleWithFixedDelay(() -> {
            ConsumerMessageListener listener = listenerRef.get();
            if (listener != null) {
                ConsumerReadResponse response = responseQueue.poll();
                if (response != null) {
                    listener.onData(response);
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    @SneakyThrows
    public synchronized void stop() {
        Preconditions.checkArgument(scheduledFuture != null, "transport already stopped");
        scheduledFuture.cancel(true);
        await().until(scheduledFuture::isDone);
        scheduledFuture = null;
    }

    public void addEvent(ConsumerReadResponse response) {
        responseQueue.push(response);
    }

    @Override
    public void openConnection(ConsumerMessageListener listener) {
        listenerRef.set(listener);
        log.info("openConnection {}", listener);
    }

    @Override
    public void sendInit(ConsumerInitRequest init) {
        listenerRef.get().onInit(new ConsumerInitResponse(""));
    }

    @Override
    public void sendRead(ConsumerReadRequest read) {
        log.info("sendRead {}", read);
    }

    @Override
    public void sendCommit(CommitMessage commit) {
        log.info("sendCommit {}", commit);
    }

    @Override
    public void sendLocked(ConsumerLockedMessage locked) {
        log.info("sendLocked {}", locked);
    }

    @Override
    public void closeConnection() {
        log.info("closeConnection");
    }
}
