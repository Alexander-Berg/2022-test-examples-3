package ru.yandex.market.checkout.checkouter.actualization.flow;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import ru.yandex.market.reservation.feature.api.config.flow.FlowNamedStage;


public abstract class BaseReceivingFlowTest {

    protected static <T> T awaitAndReturn(long seconds, T value) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
            return value;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected static SimpleContext context() {
        return new SimpleContext();
    }

    protected <T> DataReceivingFlow<SimpleContext, SimpleContext, T> fetch(T value) {
        return DataReceivingFlow.<SimpleContext, SimpleContext, T>fetch(ctx -> value, Function.identity());
    }

    protected <T> DataReceivingFlow<SimpleContext, SimpleContext, T> fetch(
            FetchingFunction<SimpleContext, T> valueSupplier) {
        return DataReceivingFlow.fetch(valueSupplier, Function.identity());
    }

    protected <T> DataReceivingFlow<SimpleContext, SimpleContext, T> fetchFailed(
            Supplier<RuntimeException> exceptionSupplier) {
        return DataReceivingFlow.<SimpleContext, SimpleContext, T>fetch(c -> {
            throw exceptionSupplier.get();
        }, Function.identity());
    }

    protected <T> DataReceivingFlow<SimpleContext, SimpleContext, T> fetchNamed(
            FlowNamedStage<T, ?> stage,
            FetchingFunction<SimpleContext, T> valueSupplier) {
        return DataReceivingFlow.fetch(FetchingFunction.named(stage, valueSupplier), Function.identity());
    }

    protected <T> DataReceivingFlow<SimpleContext, SimpleContext, T> fetchAsync(T value) {
        return DataReceivingFlow.<SimpleContext, SimpleContext, T>fetchAsync(ctx -> awaitAndReturn(1, value),
                Function.identity());
    }

    protected <T> DataReceivingFlow<SimpleContext, SimpleContext, T> fetchAsync(Supplier<T> valueSupplier) {
        return DataReceivingFlow.<SimpleContext, SimpleContext, T>fetchAsync(ctx -> awaitAndReturn(1,
                valueSupplier.get()),
                Function.identity());
    }

    protected <T> DataReceivingFlow<SimpleContext, SimpleContext, T> fetchAsync(
            FetchingFunction<SimpleContext, T> function) {
        return DataReceivingFlow.<SimpleContext, SimpleContext, T>fetchAsync(ctx -> awaitAndReturn(1,
                function.fetch(ctx)),
                Function.identity());
    }

    protected <T> DataReceivingFlow<SimpleContext, SimpleContext, T> fetchFailedAsync(
            Supplier<RuntimeException> exceptionSupplier) {
        return DataReceivingFlow.<SimpleContext, SimpleContext, T>fetchAsync(c -> {
            throw exceptionSupplier.get();
        }, Function.identity());
    }

    protected <T> DataReceivingFlow<SimpleContext, SimpleContext, T> fetchNamedAsync(
            FlowNamedStage<T, ?> stage,
            FetchingFunction<SimpleContext, T> function) {
        return DataReceivingFlow.<SimpleContext, SimpleContext, T>fetchAsync(
                FetchingFunction.named(stage, ctx -> awaitAndReturn(1, function.fetch(ctx))),
                Function.identity());
    }

    @SafeVarargs
    protected final DataReceivingFlow<SimpleContext, SimpleContext, Void> allCompleteOf(
            DataReceivingFlow<SimpleContext, SimpleContext, ?> flow,
            DataReceivingFlow<SimpleContext, SimpleContext, ?>... flows) {
        return DataReceivingFlow.allCompleteOf(Function.identity(), flow, flows);
    }

    @SafeVarargs
    protected final DataReceivingFlow<SimpleContext, SimpleContext, Void> allSuccessOf(
            DataReceivingFlow<SimpleContext, SimpleContext, ?> flow,
            DataReceivingFlow<SimpleContext, SimpleContext, ?>... flows) {
        return DataReceivingFlow.allSuccessOf(Function.identity(), flow, flows);
    }

    protected static class SimpleContext implements FlowSessionAware<SimpleContext, SimpleContext> {

        private FlowRuntimeSession<SimpleContext, SimpleContext> session;

        @Nonnull
        public FlowRuntimeSession<SimpleContext, SimpleContext> getSession() {
            return Objects.requireNonNull(session);
        }

        @Override
        public void setSession(@Nonnull FlowRuntimeSession<SimpleContext, SimpleContext> session) {
            this.session = session;
        }

        @Override
        public boolean hasSession() {
            return session != null;
        }
    }
}
