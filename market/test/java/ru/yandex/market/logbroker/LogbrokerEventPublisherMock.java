package ru.yandex.market.logbroker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import ru.yandex.market.logbroker.event.LogbrokerEvent;

public class LogbrokerEventPublisherMock<TEvent extends LogbrokerEvent> implements LogbrokerEventPublisher<TEvent> {
    private final List<TEvent> sendEvents = new ArrayList<>();
    private Predicate<TEvent> logbrokerEventFilter = __ -> true;

    @Override
    public CompletableFuture<TEvent> publishEventAsync(@Nonnull TEvent event) {
        if (logbrokerEventFilter.test(event)) {
            sendEvents.add(event);
            return CompletableFuture.completedFuture(event);
        } else {
            return CompletableFuture.failedFuture(new LogbrokerInteractionException(
                "Error encountered while sending event to Logbroker"
            ));
        }
    }

    public List<TEvent> getSendEvents() {
        return Collections.unmodifiableList(sendEvents);
    }

    public void clear() {
        sendEvents.clear();
    }

    public void setLogbrokerEventFilter(Predicate<TEvent> logbrokerEventFilter) {
        this.logbrokerEventFilter = logbrokerEventFilter;
    }

    @Override
    public void close() {
    }
}
