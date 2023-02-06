package ru.yandex.travel.commons.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FutureUtilsTest {
    @Test
    public void joinCompleted() {
        CompletableFuture<String> completed = CompletableFuture.completedFuture("completed");
        CompletableFuture<String> failed = CompletableFuture.failedFuture(new RuntimeException("failed msg"));
        CompletableFuture<String> cancelled = new CompletableFuture<>();
        cancelled.cancel(false);
        CompletableFuture<String> inProgress = new CompletableFuture<>();

        assertThat(FutureUtils.joinCompleted(completed)).isEqualTo("completed");
        assertThatThrownBy(() -> FutureUtils.joinCompleted(failed))
                .isInstanceOf(CompletionException.class)
                .hasMessageContaining("failed msg");
        assertThatThrownBy(() -> FutureUtils.joinCompleted(cancelled))
                .isInstanceOf(CancellationException.class);
        assertThatThrownBy(() -> FutureUtils.joinCompleted(inProgress))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The future has to be completed");
    }
}
