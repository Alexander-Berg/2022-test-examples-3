package ru.yandex.market.checkout.checkouter.actualization.flow;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.SerializationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.typeCompatibleWith;

public class DataReceivingAsyncFlowTest extends BaseReceivingFlowTest {

    @Test
    void shouldReceiveValueOnAsynchronousFetch() throws Throwable {
        var stage = fetchAsync(1).apply(context());

        assertThat(stage, notNullValue());
        assertThat(stage.await().isPresent(), is(true));
        assertThat(stage.await().get(), comparesEqualTo(1));
    }

    @Test
    void shouldFetchAsynchronousPipelineWithDependency() throws Throwable {
        var f1 = fetchAsync(27);
        var f2 = fetchAsync(ctx -> "some number is "
                + ctx.getSession().awaitSilently(f1.sessionKey()).orElseThrow());
        var stage = f1.whenSuccess(f2).apply(context());

        stage.awaitChildren();

        assertThat(stage, notNullValue());

        assertThat(stage.session().await(f1.sessionKey()).orElseThrow(), comparesEqualTo(27));
        assertThat(stage.session().await(f2.sessionKey()).orElseThrow(), comparesEqualTo("some number is 27"));
    }

    @Test
    void shouldJoinAsynchronousPipelines() throws Throwable {
        var f1 = fetchAsync(1);
        var f2 = fetchAsync("some string");
        var stage = allCompleteOf(f1, f2).apply(context());

        assertThat(stage, notNullValue());

        assertThat(stage.session().await(f1.sessionKey()).orElseThrow(), comparesEqualTo(1));
        assertThat(stage.session().await(f2.sessionKey()).orElseThrow(), comparesEqualTo("some string"));
    }

    @Test
    void shouldJoinAsynchronousPipelinesAndDoRelated() throws Throwable {
        var f1 = fetchAsync(1);
        var f2 = fetchAsync(ctx -> "some string " +
                ctx.getSession().awaitSilently(f1.sessionKey()).orElseThrow());
        var stage = allCompleteOf(f1, DataReceivingFlow.doNotReceive())
                .whenSuccess(f2)
                .apply(context());

        stage.awaitChildren();

        assertThat(stage, notNullValue());

        assertThat(stage.session().await(f1.sessionKey()).orElseThrow(), comparesEqualTo(1));
        assertThat(stage.session().await(f2.sessionKey()).orElseThrow(), comparesEqualTo("some string 1"));
    }

    @Test
    void shouldFailOnDirectAwaitFailedSubprocesses() throws Throwable {
        var f1 = fetchFailed(SerializationException::new);
        var f2 = fetchFailedAsync(SerializationException::new);
        var stage = allCompleteOf(f1, f2)
                .apply(context());

        Assertions.assertThrows(SerializationException.class, stage::awaitChildren);
    }

    @Test
    void shouldFailOnDirectAwaitFlowWithFailedSubscriber() throws Throwable {
        var f1 = fetchAsync(1);
        var f2 = fetchAsync(1)
                .whenSuccess((c, v) -> {
                    throw new SerializationException();
                });
        var stage = allCompleteOf(f1, f2)
                .apply(context());

        Assertions.assertThrows(SerializationException.class, stage::awaitChildren);
    }

    @Test
    void shouldFailOnDirectFlowWithFailedMutation() throws Throwable {
        var f1 = fetchAsync(1);
        var f2 = fetchAsync(1)
                .mutate(c -> {
                    throw new SerializationException();
                });
        var stage = allCompleteOf(f1, f2)
                .apply(context());

        Assertions.assertThrows(SerializationException.class, stage::awaitChildren);
    }

    @Test
    void shouldFailOnDirectFlowWithFailedMutation2() throws Throwable {
        var f1 = fetchAsync(1);
        var f2 = fetchAsync(1)
                .mutate((c, v) -> {
                    throw new SerializationException();
                });
        var stage = allCompleteOf(f1, f2)
                .apply(context());

        Assertions.assertThrows(SerializationException.class, stage::awaitChildren);
    }

    @Test
    void shouldDoRelatedEvenIfRootProcessesFailed() throws Throwable {
        var f1 = fetchFailed(SerializationException::new);
        var f2 = fetchFailedAsync(SerializationException::new);
        var f3 = fetchAsync(1);
        var stage = allCompleteOf(f1, f2)
                .whenSuccess(f3)
                .apply(context());

        stage.awaitChildrenSilently();

        assertThat(stage, notNullValue());
        assertThat(stage.session().await(f3.sessionKey()).orElseThrow(), comparesEqualTo(1));
    }

    @Test
    void shouldDoRelatedEvenIfRootProcessesFailedOnSubscriber() throws Throwable {
        var f1 = fetch(1);
        var f2 = fetchAsync(1);
        var f3 = fetchAsync(1);
        var stage = allCompleteOf(f1, f2
                .whenSuccess(c -> {
                    throw new SerializationException();
                }))
                .whenException(f3)
                .apply(context());

        stage.awaitChildrenSilently();

        assertThat(stage, notNullValue());
        assertThat(stage.session().await(f3.sessionKey()).orElseThrow(), comparesEqualTo(1));
    }

    @Test
    void shouldDoRelatedEvenIfRootProcessesFailedOnMutation() throws Throwable {
        var f1 = fetch(1);
        var f2 = fetchAsync(1);
        var f3 = fetchAsync(1);
        var stage = allCompleteOf(f1, f2
                .mutate(c -> {
                    throw new SerializationException();
                }))
                .whenException(f3)
                .apply(context());

        stage.awaitChildrenSilently();

        assertThat(stage, notNullValue());
        assertThat(stage.session().await(f3.sessionKey()).orElseThrow(), comparesEqualTo(1));
    }

    @Test
    void shouldDoRelatedEvenIfRootProcessesFailedOnMutation2() throws Throwable {
        var f1 = fetch(1);
        var f2 = fetchAsync(1);
        var f3 = fetchAsync(1);
        var stage = allCompleteOf(f1, f2
                .mutate((c, v) -> {
                    throw new SerializationException();
                }))
                .whenException(f3)
                .apply(context());

        stage.awaitChildrenSilently();

        assertThat(stage, notNullValue());
        assertThat(stage.session().await(f3.sessionKey()).orElseThrow(), comparesEqualTo(1));
    }

    @Test
    void shouldDoRelatedEvenIfRootProcessesFailed3() throws Throwable {
        var f1 = fetchFailed(SerializationException::new);
        var f2 = fetchFailedAsync(SerializationException::new);
        var f3 = fetchAsync(1);
        var stage = allCompleteOf(f1, f2)
                .whenComplete(f3)
                .apply(context());

        stage.awaitChildrenSilently();

        assertThat(stage, notNullValue());
        assertThat(stage.session().await(f3.sessionKey()).orElseThrow(), comparesEqualTo(1));
    }

    @Test
    void shouldFailRelatedIfRootProcessesFailed() throws Throwable {
        var f1 = fetchFailed(SerializationException::new);
        var f2 = fetchFailedAsync(SerializationException::new);
        var f3 = fetchAsync(1);
        var f4 = fetchAsync(2);
        var f5 = fetchAsync(3);
        var stage = allSuccessOf(f1, f2)
                .whenSuccess(f3)
                .whenException(f4)
                .whenComplete(f5)
                .apply(context());

        stage.awaitChildrenSilently();

        assertThat(stage, notNullValue());
        Assertions.assertFalse(stage.session().hasCompletedStage(f3.sessionKey()));
        Assertions.assertTrue(stage.session().hasCompletedStage(f4.sessionKey()));
        Assertions.assertTrue(stage.session().hasCompletedStage(f5.sessionKey()));
    }

    @Test
    void shouldFailOnExceptionOnValueRetrieve() {
        var stage = fetchAsync(() -> {
            throw new SerializationException();
        }).apply(context());

        Assertions.assertThrows(SerializationException.class, stage::awaitChildren);
    }

    @Test
    void shouldNotFailOnExceptionOnSilentValueRetrieve() {
        var stage = fetchAsync(() -> {
            throw new SerializationException();
        }).apply(context());

        var value = Assertions.assertDoesNotThrow(stage::awaitSilently);
        assertThat(value.isPresent(), is(false));
    }

    @Test
    void shouldCallErrorHandlerOnExceptionBeforeApply() {
        var errorRef = new AtomicReference<Throwable>();
        var stage = fetchAsync(() -> {
            throw new SerializationException();
        })
                .whenException((ctx, ex) -> errorRef.set(ex))
                .apply(context());

        stage.awaitChildrenSilently();

        assertThat(errorRef.get(), notNullValue());
        assertThat(errorRef.get().getClass(), typeCompatibleWith(SerializationException.class));
    }

    @Test
    void shouldNotCallErrorHandlerOnExceptionAfterApply() {
        var errorRef = new AtomicReference<Throwable>();
        var stage = fetchAsync(() -> {
            throw new SerializationException();
        }).apply(context());

        stage.whenException((ctx, ex) -> errorRef.set(ex));

        stage.awaitChildrenSilently();

        assertThat(errorRef.get(), notNullValue());
        assertThat(errorRef.get().getClass(), typeCompatibleWith(SerializationException.class));
    }

    @Test
    void shouldCallErrorHandlerOnExceptionInSuccessCallback() throws Throwable {
        var errorRef = new AtomicReference<Throwable>();
        var stage = fetchAsync("do smth")
                .whenSuccess(msg -> {
                    throw new UnsupportedOperationException(msg);
                })
                .whenException((ctx, ex) -> errorRef.set(ex)).apply(context());

        stage.awaitChildrenSilently();

        assertThat(errorRef.get(), notNullValue());
        assertThat(errorRef.get().getClass(), typeCompatibleWith(UnsupportedOperationException.class));
        assertThat(errorRef.get().getMessage(), is("do smth"));
    }

    @Test
    void shouldNotCallChildStageIfExceptionInParent() {
        var flag = new AtomicBoolean(false);
        var stage = fetchAsync(() -> {
            throw new SerializationException();
        }).whenSuccess(fetchAsync(() -> "test")
                .mutate(c -> flag.set(true))).apply(context());

        stage.awaitChildrenSilently();

        assertThat(flag.get(), notNullValue());
        assertThat(flag.get(), is(false));
    }

    @Test
    void shouldAwaitAllStages() throws Throwable {
        var f1 = fetchAsync(1);
        var f2 = fetchAsync("test1");
        var f3 = fetchAsync("test1");
        var f4 = fetchAsync(5);
        var f5 = fetchAsync("test2");

        var stage = allCompleteOf(
                f1.whenSuccess(f2).whenSuccess(f3),
                f4.whenSuccess(f5)
        ).apply(context());

        stage.awaitChildren();

        assertThat(stage.stage(), is(StageAware.ReceivingStage.COMPLETED));
        assertThat(stage.session().hasCompletedStage(f1.sessionKey()), is(true));
        assertThat(stage.session().hasCompletedStage(f2.sessionKey()), is(true));
        assertThat(stage.session().hasCompletedStage(f3.sessionKey()), is(true));
        assertThat(stage.session().hasCompletedStage(f4.sessionKey()), is(true));
        assertThat(stage.session().hasCompletedStage(f5.sessionKey()), is(true));
    }

    @Test
    void shouldAwaitAllStagesThroughTheMainStage() throws Throwable {
        var f1 = fetchAsync(1);
        var f2 = fetchAsync("test1");
        var f3 = fetchAsync(4);
        var f4 = fetchAsync("test1");
        var f5 = fetchAsync(5);
        var f6 = fetchAsync("test2");

        var stage = allCompleteOf(
                f1.whenSuccess(f2).whenSuccess(f3).whenSuccess(f4),
                f5.whenSuccess(f6)
        ).apply(context());

        stage.awaitChildren();

        assertThat(stage.stage(), is(StageAware.ReceivingStage.COMPLETED));
        assertThat(stage.session().hasCompletedStage(f1.sessionKey()), is(true));
        assertThat(stage.session().hasCompletedStage(f2.sessionKey()), is(true));
        assertThat(stage.session().hasCompletedStage(f3.sessionKey()), is(true));
        assertThat(stage.session().hasCompletedStage(f4.sessionKey()), is(true));
        assertThat(stage.session().hasCompletedStage(f5.sessionKey()), is(true));
        assertThat(stage.session().hasCompletedStage(f6.sessionKey()), is(true));
    }

    @Test
    void shouldAwaitStageInsideAllStatement() throws Throwable {
        var f1 = fetchAsync(1);
        var f2 = fetchAsync("a");
        var f3 = fetch(ctx -> 4);
        var f4 = fetchAsync(ctx -> "test-"
                + ctx.getSession().awaitSilently(f3.sessionKey()).orElseThrow());

        var stage = allCompleteOf(
                f1.whenSuccess(f2),
                f3
        ).whenSuccess(f4).apply(context());

        stage.awaitChildren();

        assertThat(stage.stage(), is(StageAware.ReceivingStage.COMPLETED));
        assertThat(stage.session().hasCompletedStage(f1.sessionKey()), is(true));
        assertThat(stage.session().hasCompletedStage(f2.sessionKey()), is(true));
        assertThat(stage.session().hasCompletedStage(f3.sessionKey()), is(true));
        assertThat(stage.session().hasCompletedStage(f4.sessionKey()), is(true));
    }

    @Test
    void shouldNotFailWithExceptionInAsyncFetcher() {
        var flow = fetchAsync(() -> {
            throw new SerializationException();
        });

        Assertions.assertDoesNotThrow(() -> flow.apply(context()));
    }

    @Test
    void shouldNotFailWithExceptionInAsyncFetcher2() {
        var flow = fetchAsync(1)
                .whenSuccess(fetchAsync(() -> {
                    throw new SerializationException();
                }));

        Assertions.assertDoesNotThrow(() -> flow.apply(context()));
    }

    @Test
    void shouldFailWithExceptionInAsyncFetcherAwait() {
        var flow = fetchAsync(() -> {
            throw new SerializationException();
        });

        var stage = Assertions.assertDoesNotThrow(() -> flow.apply(context()));
        Assertions.assertThrows(SerializationException.class, stage::await);
    }

    @Test
    void shouldFailWithExceptionInAsyncFetcherAwait2() {
        var flow = fetchAsync(1)
                .whenSuccess(fetchAsync(ctx -> {
                    throw new SerializationException();
                }));

        var stage = Assertions.assertDoesNotThrow(() -> flow.apply(context()));
        Assertions.assertThrows(SerializationException.class, stage::awaitChildren);
    }

    @Test
    void shouldAcceptNullValueAsync() throws Throwable {
        var value = new AtomicReference<Boolean>();
        var flow = fetchAsync(() -> null)
                .whenComplete(DataSubscriber.onSuccess(v -> value.set(v == null)));

        var stage = flow.apply(context());

        stage.awaitChildren();

        Assertions.assertTrue(value.get());
    }

    @Test
    void shouldCancelStage() throws Throwable {
        var flow = fetchAsync(1);
        var stage = flow.apply(context());
        stage.cancel();

        assertThat(stage.stage(), is(StageAware.ReceivingStage.CANCELED));

        Assertions.assertThrows(CancellationException.class, stage::await);
        Assertions.assertThrows(CancellationException.class, stage::awaitChildren);
        Assertions.assertThrows(CancellationException.class, () -> stage.await(1, TimeUnit.MILLISECONDS));
    }

    @Test
    void shouldGetWithTimeout() {
        var flow = fetchAsync(1);
        var stage = flow.apply(context());

        Assertions.assertThrows(TimeoutException.class, () -> stage.await(1, TimeUnit.MILLISECONDS));
    }
}
