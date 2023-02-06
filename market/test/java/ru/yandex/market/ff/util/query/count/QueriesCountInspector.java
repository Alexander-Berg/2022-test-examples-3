package ru.yandex.market.ff.util.query.count;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.Value;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * @author kotovdv 11/08/2017.
 */
public class QueriesCountInspector implements StatementInspector {

    private static ThreadLocal<List<QueryInfo>> holder = ThreadLocal.withInitial(ArrayList::new);

    @Override
    public String inspect(String sql) {
        holder.get().add(new QueryInfo(sql, getSource()));
        return sql;
    }

    public static void reset() {
        holder.get().clear();
    }

    public static int getCount() {
        return holder.get().size();
    }

    public static List<QueryInfo> getQueries() {
        return List.copyOf(holder.get());
    }

    private static String getSource() {
        String thisClassName = QueriesCountInspector.class.getSimpleName();
        return Stream.of(Thread.currentThread().getStackTrace())
                .filter(element ->
                        !element.getClassName().contains(thisClassName) && element.getClassName().contains("yandex"))
                .findFirst()
                .map(element -> getFileName(element) + ':' + element.getLineNumber())
                .orElse("Unkown source");
    }

    private static String getFileName(StackTraceElement ste) {
        return Optional.ofNullable(ste.getFileName())
                .map(name -> name.split("\\.")[0])
                .orElse("Unknown file");
    }

    @Value
    public static class QueryInfo {
        private String sql;
        private String source;
    }
}
