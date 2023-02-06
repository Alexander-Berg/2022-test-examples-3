package ru.yandex.market.logistics.cte.base;

import java.util.Optional;

import org.hibernate.resource.jdbc.spi.StatementInspector;

public class QueriesCountInspector implements StatementInspector {

    private static ThreadLocal<Integer> holder = new ThreadLocal<>();

    public static void reset() {
        holder.set(0);
    }

    public static Optional<Integer> getCount() {
        return Optional.ofNullable(holder.get());
    }

    @Override
    public String inspect(String sql) {
        int queryCount = getCount().orElse(0);
        holder.set(queryCount + 1);

        return sql;
    }
}
