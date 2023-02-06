package ru.yandex.market.loyalty.admin.tms;

import org.mockito.stubbing.Stubber;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import ru.yandex.market.loyalty.spring.utils.AbstractBatchedConsumer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

public class YtTestUtils {


    public static <T> void mockAnyYtRequestWithArgsResult(List<T> records, JdbcTemplate jdbcTemplate) {
        stubResultWithGivenParameters(records, 2)
                .when(jdbcTemplate)
                .query(
                        any(String.class),
                        any(Object[].class),
                        any(RowCallbackHandler.class)
                );
    }

    public static void mockAnyYtRequestWithError(JdbcTemplate jdbcTemplate) {
        doThrow(CannotGetJdbcConnectionException.class)
                .when(jdbcTemplate)
                .query(
                        any(String.class),
                        any(Object[].class),
                        any(RowCallbackHandler.class)
                );
    }

    static <T> void mockAnyYtRequestResult(List<T> records, JdbcTemplate jdbcTemplate) {
        stubResultWithGivenParameters(records, 1)
                .when(jdbcTemplate)
                .query(
                        any(String.class),
                        any(RowCallbackHandler.class)
                );
    }

    private static <T> Stubber stubResultWithGivenParameters(List<T> records, int consumerIdx) {
        return doAnswer(invocation -> {
            final AbstractBatchedConsumer<T, ?> argument = invocation.getArgument(consumerIdx);

            for (T record : records) {
                argument.processRow(record);
            }

            return null;
        });
    }
}
