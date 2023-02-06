package ru.yandex.market.sc.core.util;

import java.util.concurrent.Callable;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.assertj.core.api.Assertions;

@UtilityClass
public class QueryCountAssertion {

    @SneakyThrows
    public static <T> T assertQueryCountEqual(
            int expectedQueryCount,
            Callable<T> codeThatNeedsToBeCheckedHowManyTimesItQueriesDataBase
    ) {
        QueryCountHolder.clear();
        try {
            return codeThatNeedsToBeCheckedHowManyTimesItQueriesDataBase.call();
        } finally {
            Assertions.assertThat(QueryCountHolder.get("default").getTotal()).isEqualTo(expectedQueryCount);
        }
    }
}
