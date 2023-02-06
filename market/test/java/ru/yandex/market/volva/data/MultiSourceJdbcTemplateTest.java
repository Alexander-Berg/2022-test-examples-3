package ru.yandex.market.volva.data;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import lombok.Getter;
import org.junit.Test;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author dzvyagin
 */
public class MultiSourceJdbcTemplateTest {


    @Test
    public void testQuery() {
        NamedParameterJdbcTemplate masterTemplate = mock(NamedParameterJdbcTemplate.class);
        NamedParameterJdbcTemplate template1 = mock(NamedParameterJdbcTemplate.class);
        NamedParameterJdbcTemplate template2 = mock(NamedParameterJdbcTemplate.class);
        DatasourcePack datasourcePack = new DatasourcePack(getCredentials(2));
        TemplateMockProvider provider = new TemplateMockProvider(masterTemplate, template1, template2);

        doAnswer(i -> null).when(template1).query(eq("select 1"), any(RowCallbackHandler.class));
        doAnswer(i -> {
            try {
                Thread.sleep(20L);
            } catch (Exception ignored) {
            }
            return null;
        }).when(template2).query(eq("select 1"), any(RowCallbackHandler.class));

        MultiSourceJdbcTemplate template = new MultiSourceJdbcTemplate(datasourcePack, provider::getTemplate);
        template.updateReadTemplatesSort();

        String selectQuery = "select * from table";
        template.query(selectQuery, r -> null);
        verify(template1).query(eq(selectQuery), any(ResultSetExtractor.class));

        String insertQuery = "insert into table (row1, row2) values (val1, val2)";
        template.execute(insertQuery, r -> null);
        verify(template1, never()).execute(eq(insertQuery), any(PreparedStatementCallback.class));
        verify(masterTemplate).execute(eq(insertQuery), any(PreparedStatementCallback.class));
    }

    private DbCredentials getCredentials(int hostCount) {
        List<String> hosts = new ArrayList<>();
        IntStream.range(0, hostCount).forEach(i -> hosts.add("host" + i));
        String hostStr = String.join(",", hosts);
        String url = "jdbc:postgresql://" + hostStr + "/db_name?&targetServerType=master&ssl=true";
        return DbCredentials.builder()
                .jdbcUrl(url)
                .jdbcDriver("org.postgresql.Driver")
                .jdbcUsername("username")
                .jdbcPassword("password")
                .jdbcSchema("schema")
                .build();
    }


    @Getter
    private class TemplateMockProvider {

        private int count;
        private NamedParameterJdbcTemplate[] templates;

        public TemplateMockProvider(NamedParameterJdbcTemplate... templates) {
            this.templates = templates;
        }

        public NamedParameterJdbcTemplate getTemplate(DataSource dataSource) {
            NamedParameterJdbcTemplate template = templates[count];
            count++;
            return template;
        }
    }
}
