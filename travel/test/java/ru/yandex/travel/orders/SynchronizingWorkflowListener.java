package ru.yandex.travel.orders;

import java.util.UUID;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;

import ru.yandex.travel.workflow.EventProcessingResultType;
import ru.yandex.travel.workflow.WorkflowProcessingListener;

@Slf4j
class SynchronizingWorkflowListener implements WorkflowProcessingListener {
    private SynchronousQueue<Message> queue = new SynchronousQueue<>();
    private SynchronousQueue<Boolean> proceedQueue = new SynchronousQueue<>();
    private Boolean debugMode;
    private AtomicBoolean enabled = new AtomicBoolean(true);

    public SynchronizingWorkflowListener(Boolean debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public void onEventStartProcessing(UUID workflowId, Long eventId, Message eventData) {
        try {
            if (enabled.get()) {
                proceedQueue.take();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEventProcessed(UUID workflowId, Long eventId, Message eventData, EventProcessingResultType outcome) {
        try {
            if (enabled.get()) {
                queue.put(eventData);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException();
        }
    }

    public Message getNextMessage() {
        if (debugMode) {
            try {
                proceedQueue.put(true);
                return queue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                // 10 seconds timeout in case of hanging threads
                proceedQueue.offer(true, 10, TimeUnit.SECONDS);
                return queue.poll(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void disable() {
        enabled.set(false);
        proceedQueue.offer(true); //if we're waiting to proceed
    }

    public void drain() {
        while (true) {
            try {
                Message msg = queue.poll(10, TimeUnit.MILLISECONDS);
                if (msg == null) {
                    log.debug("Queue is empty");
                    break;
                }
                log.debug("Not processed message {}", msg);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
