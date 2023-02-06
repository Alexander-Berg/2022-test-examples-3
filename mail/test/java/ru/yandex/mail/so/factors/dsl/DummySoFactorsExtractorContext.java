package ru.yandex.mail.so.factors.dsl;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import org.apache.http.concurrent.Cancellable;
import org.apache.http.protocol.HttpContext;

import ru.yandex.function.NullConsumer;
import ru.yandex.http.util.SynchronizedHttpContext;
import ru.yandex.http.util.nio.client.EmptyRequestsListener;
import ru.yandex.http.util.nio.client.RequestsListener;
import ru.yandex.logger.PrefixedLogger;
import ru.yandex.mail.so.factors.FactorsAccessViolationHandler;
import ru.yandex.mail.so.factors.LoggingFactorsAccessViolationHandler;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractorContext;
import ru.yandex.parser.mail.errors.ErrorInfo;

public class DummySoFactorsExtractorContext
    implements Executor, SoFactorsExtractorContext
{
    private static final long EXECUTOR_DELAY = 100L;
    private final HttpContext httpContext = new SynchronizedHttpContext();
    private final PrefixedLogger logger;
    private final FactorsAccessViolationHandler accessViolationHandler;

    public DummySoFactorsExtractorContext(final PrefixedLogger logger) {
        this.logger = logger;
        accessViolationHandler =
            new LoggingFactorsAccessViolationHandler(new LongAdder(), logger);
    }

    @Override
    public FactorsAccessViolationHandler accessViolationHandler() {
        return accessViolationHandler;
    }

    @Override
    public PrefixedLogger logger() {
        return logger;
    }

    @Override
    public HttpContext httpContext() {
        return httpContext;
    }

    @Override
    public RequestsListener requestsListener() {
        return EmptyRequestsListener.INSTANCE;
    }

    @Override
    public Consumer<ErrorInfo> errorsConsumer() {
        return NullConsumer.instance();
    }

    @Override
    public Executor executor() {
        return this;
    }

    @Override
    public boolean debugExtractors() {
        return false;
    }

    @Override
    public Set<String> debugFlags() {
        return Collections.emptySet();
    }

    @Override
    public void execute(final Runnable runnable) {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(EXECUTOR_DELAY);
                } catch (InterruptedException e) {
                }
                runnable.run();
            }
        }.start();
    }

    // CancellationSubscriber implementation
    @Override
    public boolean cancelled() {
        return false;
    }

    @Override
    public void subscribeForCancellation(final Cancellable callback) {
    }

    @Override
    public void unsubscribeFromCancellation(final Cancellable callback) {
    }
}

