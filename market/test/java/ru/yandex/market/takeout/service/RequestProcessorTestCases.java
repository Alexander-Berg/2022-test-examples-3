package ru.yandex.market.takeout.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestContext;

public abstract class RequestProcessorTestCases extends TestCase {
    private static final ScheduledExecutorService scheduledExecutorService;

    static {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    protected RequestProcessor getSuccessfulProcessor(String type) {
        return getProcessor(type, listCompletableFuture -> listCompletableFuture.complete(Collections.singletonList(type)));
    }

    protected RequestProcessor getEmptyProcessor(String type) {
        return getProcessor(type, listCompletableFuture -> listCompletableFuture.complete(Collections.emptyList()));
    }

    protected RequestProcessor getExceptionalProcessor(String type) {
        return getProcessor(type, listCompletableFuture -> listCompletableFuture.completeExceptionally(new RequestException()));
    }

    protected RequestProcessor getSlowSuccessfulProcessor(String type, long millis) {
        return getSlowProcessor(type, millis,
                listCompletableFuture -> listCompletableFuture.complete(Collections.singletonList(type)));
    }

    protected RequestProcessor getSlowEmptyProcessor(String type, long millis) {
        return getSlowProcessor(type, millis,
                listCompletableFuture -> listCompletableFuture.complete(Collections.emptyList()));
    }

    protected RequestProcessor getSlowExceptionalProcessor(String type, long millis) {
        return getSlowProcessor(type, millis,
                listCompletableFuture -> listCompletableFuture.completeExceptionally(new RequestException()));
    }

    protected static class RequestException extends Exception {

    }

    @NotNull
    private RequestProcessor getProcessor(String type,
                                          Consumer<CompletableFuture<List<String>>> consumer) {
        return new RequestProcessor() {
            @Override
            public Set<String> getTypes() {
                return Collections.singleton(type);
            }

            @Override
            public Module getModule() {
                return null;
            }

            @Override
            public CompletableFuture<List<String>> getStatus(long uid, Map<String, String> tickets,
                                                             RequestContext requestContext) {
                CompletableFuture<List<String>> listCompletableFuture = new CompletableFuture<>();
                consumer.accept(listCompletableFuture);
                return listCompletableFuture;
            }

            @Override
            public CompletableFuture delete(long uid, long muid, Map<String, String> tickets, Set<String> types) {
                return null;
            }

            @Override
            public CompletableFuture<?> deleteHard(DeleteTask task, Map<String, String> tickets) {
                return CompletableFuture.completedFuture(null);
            }
        };
    }

    @NotNull
    private RequestProcessor getSlowProcessor(String type, long millis,
                                              Consumer<CompletableFuture<List<String>>> consumer) {
        return new RequestProcessor() {
            @Override
            public Set<String> getTypes() {
                return Collections.singleton(type);
            }

            @Override
            public Module getModule() {
                return null;
            }

            @Override
            public CompletableFuture<List<String>> getStatus(long uid, Map<String, String> tickets,
                                                             RequestContext requestContext) {
                CompletableFuture<List<String>> listCompletableFuture = new CompletableFuture<>();
                scheduledExecutorService.schedule(() -> consumer.accept(listCompletableFuture), millis,
                        TimeUnit.MILLISECONDS);
                return listCompletableFuture;
            }

            @Override
            public CompletableFuture delete(long uid, long muid, Map<String, String> tickets, Set<String> types) {
                return null;
            }

            @Override
            public CompletableFuture<?> deleteHard(DeleteTask task, Map<String, String> tickets) {
                return CompletableFuture.completedFuture(null);
            }
        };
    }
}
