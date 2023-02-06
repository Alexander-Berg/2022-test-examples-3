package ru.yandex.market.tpl.carrier.core.db;

import java.util.concurrent.Callable;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.assertj.core.api.Assertions;

@UtilityClass
public class QueryCountAssertions {

    @SneakyThrows
    public static void assertQueryCountTotalEqual(int queryCount, Runnable runnable) {
        QueryCountHolder.clear();
        try {
            runnable.run();
        } finally {
            Assertions.assertThat(QueryCountHolder.get("default").getTotal()).isEqualTo(queryCount);
        }
    }

    @SneakyThrows
    public static <T> T assertQueryCountTotalEqual(int queryCount, Callable<T> callable) {
        QueryCountHolder.clear();
        try {
            return callable.call();
        } finally {
            Assertions.assertThat(QueryCountHolder.get("default").getTotal()).isEqualTo(queryCount);
        }
    }
}
