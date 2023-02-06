package ru.yandex.market.logistics.test.integration.jpa;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import org.hibernate.resource.jdbc.spi.StatementInspector;

public class MultiplyThreadsQueriesInspector implements StatementInspector {

    private static final AtomicInteger QUERIES_AMOUNT = new AtomicInteger();
    //for debug purpose
    private static final InheritableThreadLocal<List<String>> QUERIES = new InheritableThreadLocal<>();

    static {
        QUERIES.set(new CopyOnWriteArrayList<>());
    }

    @Override
    public String inspect(String sql) {
        QUERIES_AMOUNT.incrementAndGet();
        QUERIES.get().add(sql);

        return sql;
    }

    public static void reset() {
        QUERIES_AMOUNT.set(0);
        QUERIES.set(new CopyOnWriteArrayList<>());
    }

    /**
     * Возвращает количество выполненных запросов с последнего вызова {@link #reset()}.
     * По сути идентично выполнению {@code getQueries().size()}.
     *
     * @return количество выполненных запросов
     */
    public static int getCount() {
        return QUERIES_AMOUNT.get();
    }

    /**
     * Возвращает содержимое всех выполненных запросов в порядке вызова с последнего вызова {@link #reset()}.
     * Может возвращать не все запросы, так как многопоточная реализация. Использовать только для дебага.
     *
     * @return список выполненных sql запросов
     */
    @Nonnull
    public static List<String> getQueries() {
        return Collections.unmodifiableList(QUERIES.get());
    }
}
