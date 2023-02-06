package ru.yandex.market.antifraud.db;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by oroboros on 31.05.17.
 */
public class FixInClauseTest {

    @Test
    public void mustReplace() {
        mustFix(Arrays.asList("e1", "e2"));
    }

    @Test
    public void mustReaplceWithSetOrArrayParams() {
        mustFix(new String[] {"e1", "e2"});
        mustFix(ImmutableSet.of("e1", "e2"));
    }

    @Test
    public void mustNotReplaceSingleElementList() {
        Map<String, Object> params = new HashMap<>();
        params.put("db", "dbname");
        params.put("entities", Arrays.asList("e1"));
        LoggingJdbcTemplate.SqlAndParams fixed = LoggingJdbcTemplate.fixArrayParamsForInClause(SQL, params);
        assertThat(fixed.getSql(), is(SQL));
        assertThat(fixed.getParams().get("entities"), is(Arrays.asList("e1")));
    }

    @Test
    public void mustNotReplaceNotAList() {
        Map<String, Object> params = new HashMap<>();
        params.put("db", "dbname");
        params.put("entities", "e1, e2");
        LoggingJdbcTemplate.SqlAndParams fixed = LoggingJdbcTemplate.fixArrayParamsForInClause(SQL, params);
        assertThat(fixed.getSql(), is(SQL));
        assertThat(fixed.getParams().get("entities"), is("e1, e2"));
    }

    @Test
    public void mustNotModifySqlWithoutIn() {
        String sql = "select * from sometbl";
        LoggingJdbcTemplate.SqlAndParams fixed = LoggingJdbcTemplate.fixArrayParamsForInClause(sql, new HashMap<>());
        assertThat(fixed.getSql(), is(sql));
    }

    private void mustFix(Object entitiesValue) {
        Map<String, Object> params = new HashMap<>();
        params.put("db", "dbname");
        params.put("entities", entitiesValue);
        LoggingJdbcTemplate.SqlAndParams fixed = LoggingJdbcTemplate.fixArrayParamsForInClause(SQL, params);
        assertTrue(fixed.getSql().contains("(:entities_incfix_0,:entities_incfix_1)"));
        assertTrue(!fixed.getSql().contains("(:entities)"));
        assertTrue(!fixed.getParams().containsKey("entities"));
        assertThat(fixed.getParams().get("entities_incfix_0"), is("e1"));
        assertThat(fixed.getParams().get("entities_incfix_1"), is("e2"));
        assertThat(fixed.getParams().get("db"), is("dbname"));
    }

    private static final String SQL = "\n" +
            "select table_name, column_name, udt_name, is_nullable, character_maximum_length \n" +
            "from INFORMATION_SCHEMA.COLUMNS \n" +
            "where table_name in (\n" +
            "  select table_name \n" +
            "  from INFORMATION_SCHEMA.TABLES \n" +
            "  where \n" +
            "    table_type = 'BASE TABLE' and \n" +
            "    table_catalog = :db and \n" +
            "    table_name in (:entities) \n" +
            ") and \n" +
            "table_catalog = :db and \n" +
            "table_schema = 'public' and \n" +
            "is_updatable = 'YES';";
}
