package ru.yandex.market.clickhouse.dealer.util;

import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.apache.http.NoHttpResponseException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.UncategorizedSQLException;

import ru.yandex.clickhouse.except.ClickHouseException;
import ru.yandex.clickhouse.except.ClickHouseUnknownException;

/**
 * @author Aleksei Malygin <a href="mailto:Malygin-Me@yandex-team.ru"></a>
 * Date: 2019-05-29
 */
public class RetrierTest {

    private Retrier retrier = Retrier.INSTANCE.withTimeout(0);

    @Test
    public void retryOnNoHttpResponseException() {
        UncategorizedSQLException e = new UncategorizedSQLException(null, null,
                new ClickHouseUnknownException(
                        1002,
                        new NoHttpResponseException("health-house.market.yandex.net:8123 failed to respond"),
                        "health-house.market.yandex.net",
                        8123
                )
        );
        retryTest(e);
    }

    @Test
    public void retryOnTimeoutExceptionTest() {
        UncategorizedSQLException e = new UncategorizedSQLException(null, null,
                new ClickHouseException(
                        209,
                        new Throwable(
                                "Code: 209, e.displayText() = DB::NetException: " +
                                        "Timeout exceeded while reading from socket " +
                                        "([2a02:6b8:c02:592:0:633:60e5:5d3f]:9000): " +
                                        "while receiving packet from welder01ht.market.yandex.net:9000," +
                                        " 2a02:6b8:c02:592:0:633:60e5:5d3f, e.what() = DB::NetException"
                        ),
                        "health-house-testing.market.yandex.net",
                        8123
                )
        );
        retryTest(e);
    }

    @Test
    public void retryOnConnectionResetExceptionTest() {
        UncategorizedSQLException e = new UncategorizedSQLException(null, null,
                new ClickHouseUnknownException(
                        1002,
                        new SocketException("Connection reset"),
                        "health-house-testing.market.yandex.net",
                        8123
                )
        );
        retryTest(e);
    }

    private void retryTest(UncategorizedSQLException e) {
        Supplier<Integer> s = Mockito.mock(Supplier.class);
        List ignoreExceptions = Arrays.asList("Timeout exceeded while reading from socket", "Connection reset",
                "failed to respond");

        Mockito.when(s.get()).thenThrow(e).thenThrow(e).thenReturn(100);
        Assertions.assertThatCode(() -> retrier.retryOnException(s, 3, ignoreExceptions)).doesNotThrowAnyException();

        Mockito.when(s.get()).thenThrow(e).thenThrow(e).thenReturn(100);
        Assertions.assertThatCode(() -> retrier.retryOnException(s, 2, ignoreExceptions)).doesNotThrowAnyException();

        Mockito.when(s.get()).thenThrow(e).thenThrow(e).thenReturn(100);
        Assertions.assertThatThrownBy(() -> retrier.retryOnException(s, 1, ignoreExceptions))
                .isInstanceOf(UncategorizedSQLException.class);
    }

    public void retryOnException() {
        RuntimeException runtimeException = new RuntimeException();
        Runnable r = () -> {
            throw runtimeException;
        };

        Assertions.assertThatThrownBy(() -> retrier.retryOnException(r, 3, Collections.emptyList()))
                .isInstanceOf(RuntimeException.class);
    }
}
