package ru.yandex.travel.commons.retry;

import io.opentracing.mock.MockTracer;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class RetryingTests {
    private final MockTracer tracer = new MockTracer();
    private final Retry retryHelper = new Retry(tracer);
    private int attempt;

    private String action(String parameter) {
        attempt += 1;
        if (attempt < 5) {
            throw new RuntimeException("Oops");
        }
        return String.format("%s - %s", attempt, parameter);
    }

    private CompletableFuture<String> runAsync(String parameter) {
        return CompletableFuture.supplyAsync(() -> this.action(parameter));
    }


    @Test(expected = RuntimeException.class)
    public void testExceptionOnJoin() {
        runAsync("foo").join();
    }

    @Test
    public void testDefaultRetryStrategy() {
        String res = retryHelper.withRetry("test", this::runAsync, "foo").join();
        Assert.assertEquals("5 - foo", res);
    }

    @Test(expected = RuntimeException.class)
    public void testEmptyStrategyDoesNotRetryAnything() {
        RetryStrategy<String> strategy = new RetryStrategyBuilder<String>().build();
        retryHelper.withRetry("test", this::runAsync, "foo", strategy).join();
    }

    @Test
    public void testStrategyCatchingAllRuntimeExceptions() {
        RetryStrategy<String> strategy = new RetryStrategyBuilder<String>()
                .retryOnExceptionClass(RuntimeException.class)
                .setNumRetries(10)
                .build();
        String res = retryHelper.withRetry("test", this::runAsync, "foo", strategy).join();
        Assert.assertEquals("5 - foo", res);
    }

    @Test
    public void testStrategyCatchingRuntimeExceptionsWithSpecificMessages() {
        RetryStrategy<String> strategy = new RetryStrategyBuilder<String>()
                .retryOnException(ex -> "Oops".equals(ex.getMessage()))
                .setNumRetries(10)
                .build();
        String res = retryHelper.withRetry("test", this::runAsync, "foo", strategy).join();
        Assert.assertEquals("5 - foo", res);
    }

    @Test(expected = RuntimeException.class)
    public void testStrategyNotCatchingRuntimeExceptionsWithWrongMessages() {
        RetryStrategy<String> strategy = new RetryStrategyBuilder<String>()
                .retryOnException(ex -> "Ouch".equals(ex.getMessage()))
                .setNumRetries(10)
                .build();
        String res = retryHelper.withRetry("test", this::runAsync, "foo", strategy).join();
        Assert.assertEquals("5 - foo", res);
    }

    @Test
    public void testStrategyRetryingOnSpecificResults() {
        RetryStrategy<String> strategy = new RetryStrategyBuilder<String>()
                .retryOnExceptionClass(RuntimeException.class)
                .validateResult(res -> {
                    if (Integer.valueOf(res.substring(0, res.indexOf(' '))) < 7) {
                        throw new RuntimeException("Error");
                    }
                })
                .setNumRetries(10)
                .build();
        String res = retryHelper.withRetry("test", this::runAsync, "foo", strategy).join();
        Assert.assertEquals("7 - foo", res);
    }


    @Test
    // need to assert on exception details, so can't use `expected = RetryException.class` here
    public void testStrategyNotEnoughAttemptsCombined() {
        boolean thrown = false;
        RetryStrategy<String> strategy = new RetryStrategyBuilder<String>()
                .retryOnExceptionClass(RuntimeException.class)
                .validateResult(res -> {
                    if (Integer.valueOf(res.substring(0, res.indexOf(' '))) < 7) {
                        throw new RuntimeException("Error");
                    }
                })
                .setNumRetries(5)
                .build();
        try {
            retryHelper.withRetry("test", this::runAsync, "foo", strategy).join();
        } catch (CompletionException ex) {
            if (ex.getCause() instanceof RetryException) {
                thrown = true;
                RetryException retryException = (RetryException) ex.getCause();
                Assert.assertEquals(6, retryException.getAttemptExceptions().size()); // initial + 5 allowed retries
                for (int i = 0; i < 4; i++) {
                    Assert.assertNotNull(retryException.getAttemptExceptions().get(i));
                    Assert.assertTrue(retryException.getAttemptExceptions().get(i) instanceof RuntimeException);
                    Assert.assertEquals("Oops", retryException.getAttemptExceptions().get(i).getMessage());
                }
                for (int i = 4; i < 6; i++) {
                    Assert.assertNotNull(retryException.getAttemptExceptions().get(i));
                    Assert.assertTrue(retryException.getAttemptExceptions().get(i) instanceof UnacceptedResultException);
                    Assert.assertEquals(String.format("%s - foo", i + 1),
                            ((UnacceptedResultException) retryException.getAttemptExceptions().get(i)).getResult());
                }
            }
        }
        Assert.assertTrue(thrown);
    }
}
