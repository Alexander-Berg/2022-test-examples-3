package ru.yandex.market.loyalty.core.utils;

import org.jetbrains.annotations.NotNull;
import org.mockito.invocation.Invocation;
import org.mockito.listeners.InvocationListener;
import org.mockito.listeners.MethodInvocationReport;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class NoExternalCallsInTransactionChecker implements TransactionalAdvice.Listener, InvocationListener {
    private Predicate<InvocationInTransaction> exclusion;

    private final List<InvocationInTransaction> invocationInTransactions = new ArrayList<>();

    private final ThreadLocal<Method> transactionMethod = new ThreadLocal<>();

    @SafeVarargs
    public NoExternalCallsInTransactionChecker(
            Predicate<InvocationInTransaction>... exclusions
    ) {
        this.exclusion = Arrays.stream(exclusions).reduce(t -> false, (a, b) -> (r) -> a.test(r) || b.test(r));
    }

    @Override
    public void reportInvocation(MethodInvocationReport methodInvocationReport) {
        if (transactionMethod.get() != null) {
            InvocationInTransaction invocation = new InvocationInTransaction(
                    transactionMethod.get(),
                    ((Invocation) methodInvocationReport.getInvocation()).getMethod()
            );
            if (!exclusion.test(invocation)) {
                invocationInTransactions.add(invocation);
            }
        }
    }

    @Override
    public Object callWithinTransaction(Method method, Callable<?> task) throws Exception {
        try {
            transactionMethod.set(method);
            return task.call();
        } finally {
            transactionMethod.set(null);
        }
    }

    public void withExclusion(Predicate<InvocationInTransaction> exclusion, Runnable task) {
        Predicate<InvocationInTransaction> before = this.exclusion;
        this.exclusion = exclusion;
        try {
            task.run();
        } finally {
            this.exclusion = before;
        }
    }

    public <V> V withExclusion(Predicate<InvocationInTransaction> exclusion, Callable<V> task) throws Exception {
        Predicate<InvocationInTransaction> before = this.exclusion;
        this.exclusion = exclusion;
        try {
            return task.call();
        } finally {
            this.exclusion = before;
        }
    }

    @NotNull
    public static Predicate<InvocationInTransaction> excludeByMockClassAndName(
            Class<?> aClass, String methodName
    ) {
        return (i) -> aClass.isAssignableFrom(i.getMockedMethod().getDeclaringClass())
                && i.getMockedMethod().getName().equals(methodName);
    }

    public void clean() {
        invocationInTransactions.clear();
    }

    public void check() {
        assertThat(invocationInTransactions, is(empty()));
    }

    public static class InvocationInTransaction {
        private final Method transactionMethod;
        private final Method mockedMethod;

        public InvocationInTransaction(Method transactionMethod, Method mockedMethod) {
            this.transactionMethod = transactionMethod;
            this.mockedMethod = mockedMethod;
        }

        public Method getTransactionMethod() {
            return transactionMethod;
        }

        public Method getMockedMethod() {
            return mockedMethod;
        }

        @Override
        public String toString() {
            return "InvocationInTransaction, " +
                    "transactionMethod: " + transactionMethod +
                    ", mockedMethod: " + mockedMethod;
        }
    }
}
