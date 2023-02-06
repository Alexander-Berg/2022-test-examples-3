package ru.yandex.market.antifraud.yql.validate;

import org.junit.Test;
import org.springframework.jdbc.UncategorizedSQLException;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class YqlRetrierTest {
    @Test
    public void mustRetry() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        try {
            YqlRetrier.retry(() -> {
                counter.incrementAndGet();
                throw new TestUncategorizedSQLException("task", "sql", new SQLException());
            }, 5, 0L);
        } catch (TestUncategorizedSQLException e) {
            assertThat(counter.get(), is(5));
        }
    }

    @Test
    public void mustNotRetry() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        try {
            YqlRetrier.retry(() -> {
                counter.incrementAndGet();
                throw new RuntimeException();
            }, 5, 0L);
        } catch (RuntimeException e) {
            assertThat(counter.get(), is(1));
        }
    }

    class TestUncategorizedSQLException extends UncategorizedSQLException {
        public TestUncategorizedSQLException(String task, String sql, SQLException ex) {
            super(task, sql, ex);
        }
    }
}
