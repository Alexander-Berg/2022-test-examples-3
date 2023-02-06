package ru.yandex.market.antifraud.db;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by oroboros on 18.05.17.
 */
public class LoggingJdbcTemplateTest {
    private static final int MAX_PARAMS = 56;

    private int queryParamsSet = 0;
    private int totalParamsSet = 0;
    private int totalQueriesRun = 0;

    @Test
    public void testBatchInsert() throws Exception {
        setFinalStatic(
                LoggingJdbcTemplate.class.getDeclaredField("MAX_PARAMS_PER_QUERY"),
                MAX_PARAMS);
        JdbcOperations jdbcOperations = mock(JdbcOperations.class);
        NamedParameterJdbcTemplate mockTemplate = mock(NamedParameterJdbcTemplate.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(mockTemplate.getJdbcOperations()).thenReturn(jdbcOperations);
        doAnswer(invocation -> {
            queryParamsSet++;
            totalParamsSet++;
            return null;
        }).when(ps).setObject(anyInt(), any(Object.class));
        when(jdbcOperations.update(anyString(), any(PreparedStatementSetter.class)))
            .then((Answer<Integer>) invocation -> {
                String query = (String) invocation.getArguments()[0];
                PreparedStatementSetter pss = (PreparedStatementSetter) invocation.getArguments()[1];
                queryParamsSet = 0;
                pss.setValues(ps);
                assertThat(StringUtils.countMatches(query, "?"), is(queryParamsSet));
                assertThat(queryParamsSet, lessThan(MAX_PARAMS));
                totalQueriesRun++;
                return 1;
            });
        LoggingJdbcTemplate template = new LoggingJdbcTemplate(mockTemplate);
        template.batchInsert(
                50,
                ImmutableSortedSet.of("c1", "c2", "c3"),
                "insert into tbl %columns% values %value_placeholders%;",
                (row, column) -> row + "/" + column
        );

        assertThat(totalQueriesRun, is(3));
        assertThat(totalParamsSet, is(50 * 3));
    }

    void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }

    @Test
    public void testIncFix() {
        LoggingJdbcTemplate.SqlAndParams sqlAndParams = LoggingJdbcTemplate.fixArrayParamsForInClause(
                "select * from tbl where id in (:ids)",
                ImmutableMap.of("ids", Arrays.asList(1, 2, 3)));
        assertThat(sqlAndParams.getSql(),
                is("select * from tbl where id in (:ids_incfix_0,:ids_incfix_1,:ids_incfix_2)"));
        assertThat(sqlAndParams.getParams(), is(ImmutableMap.of(
                "ids_incfix_0", 1,
                "ids_incfix_1", 2,
                "ids_incfix_2", 3
        )));
    }

    private enum TestEnum {
        V_1(1), V_2(2);

        private int i;
        TestEnum(int i) {
            this.i = i;
        }

        @Override
        public String toString() {
            return "S_" + i;
        }
    }

    @Test
    public void testEnumFix() {
        LoggingJdbcTemplate.SqlAndParams sqlAndParams = LoggingJdbcTemplate.fixArrayParamsForEnum(
                "select * from tbl where status = :status",
                ImmutableMap.of("status", TestEnum.V_1));
        assertThat(sqlAndParams.getSql(),
                is("select * from tbl where status = :status"));
        assertThat(sqlAndParams.getParams().get("status"), is("S_1"));
    }

    @Test
    public void testBothFix() {
        LoggingJdbcTemplate.SqlAndParams sqlAndParams = LoggingJdbcTemplate.fix(
                "select * from tbl where status in (:statuses)",
                ImmutableMap.of("statuses", Arrays.asList(TestEnum.V_1, TestEnum.V_2)));
        assertThat(sqlAndParams.getSql(),
                is("select * from tbl where status in (:statuses_incfix_0,:statuses_incfix_1)"));
        assertThat(sqlAndParams.getParams(), is(ImmutableMap.of(
                "statuses_incfix_0", "S_1",
                "statuses_incfix_1", "S_2"
        )));
    }

    @Test
    public void testQueryForNullObject() {
        NamedParameterJdbcTemplate mockTemplate = mock(NamedParameterJdbcTemplate.class);
        when(mockTemplate.queryForObject(anyString(), anyMap(), any(RowMapper.class)))
            .thenThrow(new EmptyResultDataAccessException(1));
        LoggingJdbcTemplate loggingJdbcTemplate = new LoggingJdbcTemplate(mockTemplate);
        assertNull(loggingJdbcTemplate.queryForObject(
            "some sql",
            Collections.emptyMap(),
            (rs, i) -> "not a null"));
    }
}
