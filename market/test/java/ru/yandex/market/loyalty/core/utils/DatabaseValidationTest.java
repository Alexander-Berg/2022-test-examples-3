package ru.yandex.market.loyalty.core.utils;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

public class DatabaseValidationTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final Pattern COLUMNS_SEPARATOR = Pattern.compile(",[\\s]*");
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void everyForeignKeyShouldHaveIndex() {
        List<String> indexes = jdbcTemplate.queryForList(
                "/*validator=false*/select indexdef from pg_indexes where tablename not like 'pg%'",
                String.class
        );

        Pattern indexDDL = Pattern.compile("CREATE[\\s]+(UNIQUE[\\s]+)?INDEX[\\s]+[\\S]+[\\s]+ON[\\s]+(public\\.)?" +
                "([\\S]+)[\\s]+USING[\\s]+[\\S]+[\\s]*\\(([\\w\\s,]+)\\).*");

        List<IndexDefinition> indexDefinitions = indexes.stream()
                // quick fix
                .filter(index -> !index.contains("translate(substr(status, 1, 1), 'AIUERT'::text, 'TTTFFF'::text)"))
                .filter(index -> !index.contains("translate(substr(status, 1, 1), 'AIURTE'::text, 'XXXVVX'::text)"))
                .map(index -> {
                    Matcher matcher = indexDDL.matcher(index);
                    assertTrue(index, matcher.find());
                    String table = matcher.group(3);
                    String columns = matcher.group(4);
                    return new IndexDefinition(
                            table, ImmutableList.copyOf(COLUMNS_SEPARATOR.split(columns))
                    );
                })
                .collect(ImmutableList.toImmutableList());

        List<Map<String, Object>> foreignKeys = jdbcTemplate.queryForList("/*validator=false*/SELECT " +
                "       tc.constraint_name, " +
                "       tc.table_name, " +
                "       kcu.column_name " +
                "FROM " +
                "     information_schema.table_constraints AS tc " +
                "       JOIN information_schema.key_column_usage AS kcu " +
                "         ON tc.constraint_name = kcu.constraint_name " +
                "              AND tc.table_schema = kcu.table_schema " +
                "WHERE constraint_type = 'FOREIGN KEY'");

        List<String> missedIndexesForForeignKey = foreignKeys.stream()
                .filter(fk -> indexDefinitions.stream().noneMatch(index ->
                        {
                            // https://st.yandex-team.ru/MARKETINCIDENTS-3484 поучительная история об этом тесте
                            // именно в этой строке была ошибка
                            String firstIndexedColumn = index.getColumns().get(0);
                            return index.getTable().equals(fk.get("table_name")) && firstIndexedColumn.equals(fk.get(
                                    "column_name"));
                        }
                ))
                .map(fk -> fk.get("table_name") + ":" + fk.get("constraint_name"))
                .collect(ImmutableList.toImmutableList());

        assertThat(missedIndexesForForeignKey, is(empty()));
    }

    public static class IndexDefinition {
        private final String table;
        private final List<String> columns;

        public IndexDefinition(String table, List<String> columns) {
            this.table = table;
            this.columns = columns;
        }

        public String getTable() {
            return table;
        }

        public List<String> getColumns() {
            return columns;
        }

        @Override
        public String toString() {
            return "IndexDefinition{" +
                    "table='" + table + '\'' +
                    ", columns=" + columns +
                    '}';
        }
    }
}
