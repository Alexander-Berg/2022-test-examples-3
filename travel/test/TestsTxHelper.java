package ru.yandex.travel.orders.test;

import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

public class TestsTxHelper {
    @Autowired
    private TransactionTemplate transactionTemplate;

    public void runInTx(Runnable r) {
        callInTx(() -> {
            r.run();
            return null;
        });
    }

    public <T> T callInTx(Callable<T> r) {
        return transactionTemplate.execute(tx -> {
            try {
                return r.call();
            } catch (Exception e) {
                throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
            }
        });
    }
}
