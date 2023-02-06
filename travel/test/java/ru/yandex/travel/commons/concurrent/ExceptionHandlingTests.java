package ru.yandex.travel.commons.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.travel.commons.concurrent.FutureUtils.handleExceptionOfType;

public class ExceptionHandlingTests {
    private CompletableFuture<String> call(int outcome) {
        switch (outcome) {
            case 1:
                return CompletableFuture.failedFuture(new DerivedException1());
            case 2:
                return CompletableFuture.failedFuture(new DerivedException2());
            case 3:
                return CompletableFuture.failedFuture(new AnotherException());
            default:
                return CompletableFuture.completedFuture("normal execution");
        }
    }

    @Test
    public void tesNormal() throws ExecutionException, InterruptedException {
        CompletableFuture<String> result = handleExceptionOfType(call(0), BaseException.class, d -> "handled");
        assertThat(result.get()).isEqualTo("normal execution");
    }

    @Test
    public void testSimpleExceptionHandling() throws ExecutionException, InterruptedException {
        CompletableFuture<String> result = handleExceptionOfType(call(1), DerivedException1.class, d -> "handled 1");
        assertThat(result.get()).isEqualTo("handled 1");
    }

    @Test
    public void testExceptionNotHandled() throws ExecutionException, InterruptedException {
        CompletableFuture<String> result = handleExceptionOfType(call(2), DerivedException1.class, d -> "handled 1");
        assertThatThrownBy(result::get).hasCauseInstanceOf(DerivedException2.class);
    }

    @Test
    public void testExceptionHandlingChain() throws ExecutionException, InterruptedException {
        CompletableFuture<String> derived1Handled = handleExceptionOfType(call(2), DerivedException1.class, d ->
                "handled 1");
        CompletableFuture<String> derived2Handled = handleExceptionOfType(derived1Handled, DerivedException2.class,
                d -> "handled 2");
        assertThatThrownBy(derived1Handled::get).hasCauseInstanceOf(DerivedException2.class);
        assertThat(derived2Handled.get()).isEqualTo("handled 2");
    }

    @Test
    public void testBaseExceptionHandling() throws ExecutionException, InterruptedException {
        CompletableFuture<String> handled = handleExceptionOfType(call(1), BaseException.class, d -> "handled base");
        CompletableFuture<String> unhandled = handleExceptionOfType(call(3), BaseException.class, d -> "handled base");
        assertThat(handled.get()).isEqualTo("handled base");
        assertThatThrownBy(unhandled::get).hasCauseInstanceOf(AnotherException.class);
    }

    public static class BaseException extends RuntimeException {
    }

    public static class DerivedException1 extends BaseException {
    }

    public static class DerivedException2 extends BaseException {
    }

    public static class AnotherException extends RuntimeException {
    }


}
