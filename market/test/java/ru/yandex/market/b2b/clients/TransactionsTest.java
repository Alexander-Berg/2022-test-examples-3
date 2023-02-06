package ru.yandex.market.b2b.clients;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

public class TransactionsTest extends AbstractFunctionalTest {

    @Autowired
    TransactionTemplate tt;

    @Test
    public void bindResource_commit() {
        String value = Randoms.string();
        AtomicReference reference = new AtomicReference();

        tt.execute((status) -> {
            bindResource(reference, value);
            return null;
        });

        // по окончании успешной транзакции должно выполнится действие afterCommit
        Assertions.assertEquals(value, reference.get());
    }

    @Test
    public void bindResource_rollback() {
        String value = Randoms.string();
        AtomicReference reference = new AtomicReference();

        try {
            tt.execute((status) -> {
                bindResource(reference, value);
                throw new TestException();
            });
        } catch (TestException e) {
        }

        // по окончании НЕуспешной транзакции НЕ должно выполнится действие afterCommit
        Assertions.assertNull(reference.get());
    }

    private static void bindResource(AtomicReference reference, String value) {
        Transactions.getOrBindResource("abc", () -> value, new Transactions.ResourceSynchronization<>() {
            @Override
            public void afterCommit(String value) {
                reference.set(value);
            }
        });
    }

    private class TestException extends RuntimeException {
    }
}
