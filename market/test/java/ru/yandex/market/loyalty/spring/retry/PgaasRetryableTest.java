package ru.yandex.market.loyalty.spring.retry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.postgresql.util.PSQLState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionSystemException;

import ru.yandex.market.loyalty.spring.retry.spring.PgaasRetryable;
import ru.yandex.market.loyalty.spring.utils.InspectExceptions;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
@ContextConfiguration(classes = PgaasRetryableTest.Config.class)
@RunWith(SpringRunner.class)
public class PgaasRetryableTest {
    private static final SQLException RECOVERABLE_EXCEPTION = new SQLException("reason",
            PSQLState.COMMUNICATION_ERROR.getState());

    @Autowired
    private SomeExternalService someExternalService;

    @Autowired
    private SomeService someService;

    @Before
    public void resetMocks() {
        reset(someExternalService);
    }

    @Test
    public void okFromFirstTry() throws Exception {
        when(someExternalService.execute())
                .thenReturn(null);
        someService.useExternalService();
    }

    @Test
    public void okFrom3Try() throws Exception {
        SQLException[] sqlExceptions = new SQLException[2];
        Arrays.fill(sqlExceptions, RECOVERABLE_EXCEPTION);
        when(someExternalService.execute())
                .thenThrow(sqlExceptions)
                .thenReturn(null);
        someService.useExternalService();
    }

    @Test(expected = SQLException.class)
    public void okFrom4TryButItsTooLate() throws Exception {
        SQLException[] sqlExceptions = new SQLException[3];
        Arrays.fill(sqlExceptions, RECOVERABLE_EXCEPTION);
        when(someExternalService.execute())
                .thenThrow(sqlExceptions)
                .thenReturn(null);
        someService.useExternalService();
    }

    @Test(expected = SQLException.class)
    public void notRecoverableException() throws Exception {
        when(someExternalService.execute())
                .thenThrow(new SQLException("reason", PSQLState.SYNTAX_ERROR.getState()))
                .thenReturn(null);
        someService.useExternalService();
    }

    @Test
    public void recoverableCauseException() throws Exception {
        when(someExternalService.execute())
                .thenThrow(new Exception(RECOVERABLE_EXCEPTION))
                .thenReturn(null);
        someService.useExternalService();
    }

    @Test
    public void connectionTimeoutException() throws Exception {
        //copy from org.apache.commons.dbcp2.PoolingDataSource.getConnection()
        NoSuchElementException poolException = new NoSuchElementException();
        SQLException timeoutException =
                new SQLException("Cannot get a connection, pool error " + poolException.getMessage(), poolException);
        when(someExternalService.execute())
                .thenThrow(new Exception(timeoutException))
                .thenReturn(null);
        someService.useExternalService();
    }

    @Test
    public void recoverableApplicationException() throws Exception {
        TransactionSystemException exception = new TransactionSystemException("some exception");
        exception.initApplicationException(RECOVERABLE_EXCEPTION);
        when(someExternalService.execute())
                .thenThrow(exception)
                .thenReturn(null);
        someService.useExternalService();
    }

    @Test
    public void recoverableSuppressedException() throws Exception {
        when(someExternalService.execute())
                .then(invocation -> {
                    try (Closeable ignored = () -> {
                        throw new IOException(RECOVERABLE_EXCEPTION);
                    }) {
                        throw new Exception();
                    }
                }).thenReturn(null);
        someService.useExternalService();
    }

    @Test(expected = Exception.class)
    public void testRecursionOverLimit() throws Exception {
        Exception exception = RECOVERABLE_EXCEPTION;
        for (int i = 0; i < InspectExceptions.RECURSION_LIMIT + 1; ++i) {
            exception = new Exception(exception);
        }
        when(someExternalService.execute())
                .thenThrow(exception)
                .thenReturn(null);
        someService.useExternalService();
    }

    @Test
    public void testRecursionInLimit() throws Exception {
        Exception exception = RECOVERABLE_EXCEPTION;
        for (int i = 0; i < InspectExceptions.RECURSION_LIMIT; ++i) {
            exception = new Exception(exception);
        }
        when(someExternalService.execute())
                .thenThrow(exception)
                .thenReturn(null);
        someService.useExternalService();
    }

    @EnableRetry
    @Configuration
    public static class Config {
        @Bean
        public SomeExternalService someExternalService() {
            return mock(SomeExternalService.class);
        }

        @Bean
        public SomeService someService(SomeExternalService someExternalService) {
            return new SomeService(someExternalService);
        }
    }

    public interface SomeExternalService {
        Object execute() throws Exception;
    }

    public static class SomeService {
        private final SomeExternalService someExternalService;

        public SomeService(SomeExternalService someExternalService) {
            this.someExternalService = someExternalService;
        }

        @PgaasRetryable
        public void useExternalService() throws Exception {
            someExternalService.execute();
        }
    }
}
