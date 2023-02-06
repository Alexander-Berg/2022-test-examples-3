package ru.yandex.market.checkout.checkouter.actualization.flow;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.SerializationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class DataReceivingSyncFlowTest extends BaseReceivingFlowTest {

    @Test
    void shouldNotFailWithoutApplyOnSimpleValue() throws Throwable {
        var flow = DataReceivingFlow.valueOf(context(), 1);

        assertThat(flow, notNullValue());
        assertThat(flow.await().isPresent(), is(true));
        assertThat(flow.await().get(), comparesEqualTo(1));
    }

    @Test
    void shouldReceiveValueOnSynchronousFetch() throws Throwable {
        var stage = fetch(1).apply(context());

        assertThat(stage, notNullValue());
        assertThat(stage.await().isPresent(), is(true));
        assertThat(stage.await().get(), comparesEqualTo(1));
    }

    @Test
    void shouldFetchValuesPipelineWithDependency() throws Throwable {
        var f1 = fetch(1);
        var f2 = fetch((c) -> "some string " + c.getSession().awaitSilently(f1.sessionKey()).orElseThrow());
        var stage = f1.whenSuccess(f2).apply(context());

        assertThat(stage.session().hasPassedStage(f1.sessionKey()), is(true));
        assertThat(stage.session().hasPassedStage(f2.sessionKey()), is(true));

        assertThat(stage.session().await(f1.sessionKey()).orElseThrow(), comparesEqualTo(1));

        assertThat(stage.session().await(f2.sessionKey()).orElseThrow(), is("some string 1"));
    }

    @Test
    void shouldNotFailWithExceptionInSyncFetcher() {
        var flow = fetch(ctx -> {
            throw new SerializationException();
        });

        Assertions.assertDoesNotThrow(() -> flow.apply(context()));
    }

    @Test
    void shouldNotFailWithExceptionInSyncFetcher2() {
        var flow = fetch(1)
                .whenSuccess(fetch(ctx -> {
                    throw new SerializationException();
                }));

        Assertions.assertDoesNotThrow(() -> flow.apply(context()));
    }

    @Test
    void shouldFailWithExceptionInSyncFetcherAwait() {
        var flow = fetch(ctx -> {
            throw new SerializationException();
        });

        var stage = Assertions.assertDoesNotThrow(() -> flow.apply(context()));
        Assertions.assertThrows(SerializationException.class, stage::await);
    }

    @Test
    void shouldFailWithExceptionInSyncFetcherAwait2() {
        var flow = fetch(1)
                .whenSuccess(fetch(ctx -> {
                    throw new SerializationException();
                }));

        var stage = Assertions.assertDoesNotThrow(() -> flow.apply(context()));
        Assertions.assertThrows(SerializationException.class, stage::awaitChildren);
    }

    @Test
    void shouldAcceptNullValue() throws Throwable {
        var value = new AtomicReference<Boolean>();
        var flow = fetch(ctx -> null)
                .whenComplete(DataSubscriber.onSuccess(v -> value.set(v == null)));
        var stage = flow.apply(context());

        stage.awaitChildren();

        Assertions.assertTrue(value.get());
    }

    @Test
    void shouldFailOnDirectAwaitFailedSubprocesses() throws Throwable {
        var f1 = fetchFailed(SerializationException::new);
        var f2 = fetchFailed(SerializationException::new);
        var stage = allCompleteOf(f1, f2)
                .apply(context());

        Assertions.assertThrows(SerializationException.class, stage::awaitChildren);
    }

    @Test
    void shouldFailOnDirectAwaitFlowWithFailedSubscriber() throws Throwable {
        var f1 = fetch(1);
        var f2 = fetch(1)
                .whenSuccess((c, v) -> {
                    throw new SerializationException();
                });
        var stage = allCompleteOf(f1, f2)
                .apply(context());

        Assertions.assertThrows(SerializationException.class, stage::awaitChildren);
    }

    @Test
    void shouldFailOnDirectFlowWithFailedMutation() throws Throwable {
        var f1 = fetch(1);
        var f2 = fetch(1)
                .mutate(c -> {
                    throw new SerializationException();
                });
        var stage = allCompleteOf(f1, f2)
                .apply(context());

        Assertions.assertThrows(SerializationException.class, stage::awaitChildren);
    }

    @Test
    void shouldFailOnDirectFlowWithFailedMutation2() throws Throwable {
        var f1 = fetch(1);
        var f2 = fetch(1)
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
        var f2 = fetchFailed(SerializationException::new);
        var f3 = fetch(1);
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
        var f2 = fetch(1);
        var f3 = fetch(1);
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
        var f2 = fetch(1);
        var f3 = fetch(1);
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
        var f2 = fetch(1);
        var f3 = fetch(1);
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
        var f2 = fetchFailed(SerializationException::new);
        var f3 = fetch(1);
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
        var f2 = fetchFailed(SerializationException::new);
        var f3 = fetch(1);
        var f4 = fetch(2);
        var f5 = fetch(3);
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
}
