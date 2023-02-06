package ru.yandex.market.logistics.test.integration.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.hibernate.resource.jdbc.spi.StatementInspector;

public class QueriesContentInspector implements StatementInspector {

    private static final ThreadLocal<List<String>> QUERIES = ThreadLocal.withInitial(ArrayList::new);

    @Override
    public String inspect(String sql) {
        QUERIES.get().add(sql);
        return sql;
    }

    public static void reset() {
        QUERIES.set(new ArrayList<>());
    }

    /**
     * Возвращает количество выполненных запросов с последнего вызова {@link #reset()}.
     * По сути идентично выполнению {@code getQueries().size()}.
     *
     * @return количество выполненных запросов
     */
    public static int getCount() {
        return QUERIES.get().size();
    }

    /**
     * Возвращает содержимое всех выполненных запросов в порядке вызова с последнего вызова {@link #reset()}.
     *
     * @return список выполненных sql запросов
     */
    @Nonnull
    public static List<String> getQueries() {
        return Collections.unmodifiableList(QUERIES.get());
    }
}
