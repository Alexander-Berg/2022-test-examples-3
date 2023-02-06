package ru.yandex.chemodan.app.djfs.core.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ThreadLocalRandom;

import lombok.Data;

import ru.yandex.chemodan.app.djfs.core.ProxyInvocationHandlerBase;
import ru.yandex.chemodan.app.djfs.core.db.DaoProxyFactory;

/**
 * Иногда кидает исключения на вызов методов. Для тестирования случаев, когда что-то пошло не так.
 *
 * @author eoshch
 */
public class RandomFailingInvocationHandler extends ProxyInvocationHandlerBase {
    private final Object instance;
    private final ProbabilitySource probabilitySource;

    public RandomFailingInvocationHandler(Object instance, ProbabilitySource probabilitySource) {
        this.instance = instance;
        this.probabilitySource = probabilitySource;
    }

    protected Object invokeImpl(Object proxy, Method method, Object[] args) throws Throwable {
        if (probabilitySource.shouldFail()) {
            throw new RandomFailureException();
        }
        try {
            return method.invoke(instance, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T proxy(Class<T> type, T instance, ProbabilitySource probabilitySource) {
        return (T) Proxy.newProxyInstance(DaoProxyFactory.class.getClassLoader(), new Class[]{type},
                new RandomFailingInvocationHandler(instance, probabilitySource));
    }

    @Data
    public static class ProbabilitySource {
        private double failureProbability;

        private boolean shouldFail() {
            return ThreadLocalRandom.current().nextDouble() < failureProbability;
        }
    }

    public static class RandomFailureException extends RuntimeException {
        public RandomFailureException() {
            super("random failure");
        }
    }
}
