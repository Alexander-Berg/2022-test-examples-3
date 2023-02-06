package ru.yandex.market.mbo.mdm.common.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ru.yandex.market.logbroker.LogbrokerInteractionException;
import ru.yandex.market.logbroker.event.LogbrokerEvent;

public class MdmLogbrokerServiceMock implements MdmLogbrokerService {

    private AtomicInteger successCount = new AtomicInteger(0);
    private boolean shouldFail = false;
    private Predicate<LogbrokerEvent> successFilter = null;

    @Override
    public <P, T extends LogbrokerEvent<P>> void publishEvents(Collection<T> events,
                                                               int batchSize,
                                                               Consumer<List<P>> failureHandler,
                                                               @Nullable Consumer<List<P>> successHandler) {
        if (shouldFail) {
            failureHandler.accept(events.stream().map(LogbrokerEvent::getPayload).collect(Collectors.toList()));
        } else {
            if (successFilter != null) {
                List<T> successful = events.stream().filter(e -> successFilter.test(e)).collect(Collectors.toList());
                List<T> failed = events.stream().filter(e -> !successFilter.test(e)).collect(Collectors.toList());
                failureHandler.accept(failed.stream().map(LogbrokerEvent::getPayload).collect(Collectors.toList()));
                if (successHandler != null) {
                    successHandler.accept(
                        successful.stream().map(LogbrokerEvent::getPayload).collect(Collectors.toList()));
                }
            } else if (successHandler != null) {
                successHandler.accept(events.stream().map(LogbrokerEvent::getPayload).collect(Collectors.toList()));
            }
            successCount.addAndGet(events.size());
        }
    }

    public void setSuccessFilter(Predicate<LogbrokerEvent> filter) {
        this.successFilter = filter;
    }

    @Override
    public CompletableFuture<LogbrokerEvent> publishEventAsync(@Nonnull LogbrokerEvent event) {
        if (shouldFail) {
            return CompletableFuture.failedFuture(new LogbrokerInteractionException("Expected failure"));
        } else if (successFilter != null) {
            if (!successFilter.test(event)) {
                return CompletableFuture.failedFuture(new LogbrokerInteractionException("Expected failure"));
            }
        }
        return CompletableFuture.completedFuture(event);
    }

    @Override
    public void close() {

    }

    public MdmLogbrokerServiceMock setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
        return this;
    }

    public int getSuccessCount() {
        return successCount.get();
    }
}
