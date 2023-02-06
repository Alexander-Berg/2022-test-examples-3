package ru.yandex.market.loyalty.test;

public class Junit5 {
    public static <E extends Throwable> E assertThrows(Class<E> expectedException, RunnableWithException mustFail) {
        try {
            mustFail.run();
        } catch (Throwable e) {
            if (expectedException.isAssignableFrom(e.getClass())) {
                return expectedException.cast(e);
            } else {
                throw new AssertionError("expected: " + expectedException + " but was: " + e.getClass(), e);
            }
        }
        throw new AssertionError("expected exception: " + expectedException);
    }

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Throwable;
    }
}
