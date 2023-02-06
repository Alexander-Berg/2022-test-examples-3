package ru.yandex.market.core.database;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

/**
 * на самом деле ничего не преобразует, только падает при нахождении нежелательных паттернов
 */
public final class ForbiddenSql implements SqlTransformers.Transformer {
    public static final SqlTransformers.Transformer INSTANCE = new ForbiddenSql();
    private static final List<ForbiddenSqlPatternDetector> FORBIDDEN_SQL_TO_REASON = ImmutableMap.<String, String>builder()
            .put("\\(\\+\\)", "use standard joins instead of Oracle-specific")
            .put("\\bnvl\\b\\s*\\(", "use more strict and standard coalesce(arg1,arg2[,...,argN]) instead")
            .put("stragg\\b", "use listagg instead")
            .put("listagg_clob\\b", "use listagg instead")
            .build()
            .entrySet().stream()
            .map(e -> new ForbiddenSqlPatternDetector(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

    private ForbiddenSql() {
    }

    @Override
    public String transform(String sql) {
        if (sql == null) {
            throw new IllegalArgumentException("Input sql is null");
        }
        for (var forbidden : FORBIDDEN_SQL_TO_REASON) {
            forbidden.accept(sql);
        }
        return sql;
    }

    private static class ForbiddenSqlPatternDetector implements Consumer<String> {
        private final Pattern pattern;
        private final String error;

        ForbiddenSqlPatternDetector(String pattern, String error) {
            // same pattern params as in PatternStringTransformer
            this.pattern = Pattern.compile(pattern,
                    Pattern.MULTILINE |
                            Pattern.CASE_INSENSITIVE |
                            Pattern.UNICODE_CASE |
                            Pattern.DOTALL);
            this.error = error;
        }

        @Override
        public void accept(String sql) {
            if (pattern.matcher(sql).find()) {
                throw new IllegalArgumentException(String.format(
                        "Found forbidden SQL pattern [%s] in query [%s], %s",
                        pattern, sql, error
                ));
            }
        }
    }
}
