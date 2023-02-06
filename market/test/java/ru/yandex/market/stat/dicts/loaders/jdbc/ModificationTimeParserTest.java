package ru.yandex.market.stat.dicts.loaders.jdbc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ModificationTimeParserTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void parsePp() {
        JdbcTaskDefinition taskDefinition = JdbcLoadConfigFromFile.builder()
                .source("market_billing.v_pp_mstat_export_info")
                .destination("pp")
                .modificationTime("mbiFlag()")
                .build()
                .getSingleTaskDefinition();
        assertThat(taskDefinition.getModificationTimeSql(), is("SELECT last_update_date_time FROM market_billing.v_mstat_dictionary_flags WHERE lower(name) = 'v_pp_mstat_export_info'"));
    }

    @Test
    public void parseDefaultMbiFlag() {
        JdbcTaskDefinition taskDefinition = JdbcLoadConfigFromFile.builder()
                .source("mbi_test.test_view")
                .modificationTime("mbiFlag()")
                .build()
                .getSingleTaskDefinition();
        assertThat(taskDefinition.getModificationTimeSql(), is("SELECT last_update_date_time FROM market_billing.v_mstat_dictionary_flags WHERE lower(name) = 'test_view'"));
    }

    @Test
    public void parseNonDefaultMbiFlag() {
        JdbcTaskDefinition taskDefinition = JdbcLoadConfigFromFile.builder()
                .source("mbi_test.test_view")
                .modificationTime("mbiFlag(another_View)")
                .build()
                .getSingleTaskDefinition();
        assertThat(taskDefinition.getModificationTimeSql(), is("SELECT last_update_date_time FROM market_billing.v_mstat_dictionary_flags WHERE lower(name) = 'another_view'"));
    }

    @Test
    public void parseCustomSql() {
        JdbcTaskDefinition taskDefinition = JdbcLoadConfigFromFile.builder()
                .source("mbi_test.test_view")
                .modificationTime("sql(select time from some_table)")
                .build()
                .getSingleTaskDefinition();
        assertThat(taskDefinition.getModificationTimeSql(), is("select time from some_table"));
    }

    @Test
    public void parseBadCustomSql() {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Cannot parse modification time");

        JdbcTaskDefinition taskDefinition = JdbcLoadConfigFromFile.builder()
                .source("mbi_test.test_view")
                .modificationTime("sql()")
                .build()
                .getSingleTaskDefinition();
        String d = taskDefinition.getModificationTimeSql();
    }

    @Test
    public void parseUnknown() {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Cannot parse modification time");

        JdbcTaskDefinition taskDefinition = JdbcLoadConfigFromFile.builder()
                .source("mbi_test.test_view")
                .modificationTime("something strange")
                .build()
                .getSingleTaskDefinition();
        String d = taskDefinition.getModificationTimeSql();
    }
}
