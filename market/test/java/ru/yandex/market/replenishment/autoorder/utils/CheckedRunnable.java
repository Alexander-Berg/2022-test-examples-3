package ru.yandex.market.replenishment.autoorder.utils;

@FunctionalInterface
public interface CheckedRunnable<E extends Exception> extends Runnable {

    @Override
    default void run() throws RuntimeException {
        try {
            runThrows();
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    void runThrows() throws E;

}
