package ru.yandex.market.notification.safe.util;

import java.util.Optional;
import java.util.concurrent.Callable;

import org.junit.Test;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.notification.exception.NotificationException;
import ru.yandex.market.notification.test.util.ClassUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link TransactionUtils}.
 *
 * @author Vladislav Bauer
 */
public class TransactionUtilsTest {

    @Test
    public void testConstructor() {
        ClassUtils.checkConstructor(TransactionUtils.class);
    }

    @Test
    public void testCreateTransactionCallbackPositive() {
        final Object expected = new Object();
        final Callable<Object> callable = () -> expected;
        final Object actual = doInTransaction(callable);

        assertThat(actual, equalTo(expected));
    }

    @Test(expected = NotificationException.class)
    public void testCreateTransactionCallbackNegative() {
        final Callable<Object> callable = () -> { throw new Exception(); };
        final Object actual = doInTransaction(callable);

        fail(String.valueOf(actual));
    }

    @Test
    public void testExecuteInTransactionPositive() {
        final Object expected = new Object();

        checkExecuteInTransaction(() -> expected, expected);
        checkExecuteInTransaction(() -> null, null);
    }

    @Test
    public void testExecuteInTransactionNegative() {
        checkExecuteInTransaction(() -> { throw new RuntimeException(); }, null);
    }


    private <T> void checkExecuteInTransaction(final Callable<T> callable, final T result) {
        final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any())).then(invocation -> callable.call());

        final Optional<T> optional = TransactionUtils.executeInTransaction(transactionTemplate, callable);
        assertThat(optional.orElse(null), equalTo(result));

        verify(transactionTemplate, times(1)).execute(any());
        verifyNoMoreInteractions(transactionTemplate);
    }

    private Object doInTransaction(final Callable<Object> callable) {
        final TransactionCallback<Object> callback = TransactionUtils.createTransactionCallback(callable);
        final SimpleTransactionStatus status = new SimpleTransactionStatus();

        return callback.doInTransaction(status);
    }

}
