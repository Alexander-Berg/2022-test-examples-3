package ru.yandex.market.olap2.util;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.olap2.config.IntegrationTestConfig;

import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class RegrantReaderITest {
    private static final String BASE_PREFIX = "RegrantReaderITest__";

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private RegrantReader regrantReader;

    @Value("${olap2.metadata.jdbc.username}")
    private String jdbcUsername;

    private String prefix;
    private String tblName;
    private String viewName;

    @Before
    public void init() {
        for(String t: jdbcTemplate.queryForList("select table_name from tables " +
            "where table_name like '" + BASE_PREFIX + "%'", Collections.emptyMap(), String.class)) {
            jdbcTemplate.getJdbcOperations().execute("drop table " + t + " cascade");
        }

        prefix = BASE_PREFIX + ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE) + "__";
        tblName = prefix + "testtbl";
        viewName = prefix + "testview";
        jdbcTemplate.getJdbcOperations().execute("create table " + tblName + " (a int)");
        jdbcTemplate.getJdbcOperations().execute("create or replace view " + viewName + " as select * from " + tblName);
    }

    @Test
    public void testGetVerticaTablesWithPrefix() {
        assertThat(regrantReader.getVerticaTablesWithPrefix(prefix).get(0), is(tblName));
    }

    @Test
    public void testGetVerticaViewsWithPrefix() {
        assertThat(regrantReader.getVerticaViewsWithTablesPrefix(prefix).get(0), is(viewName));
    }


    @Test
    public void testGrantSelect() {
        assertGrantsCount(0);
        regrantReader.grantSelect(tblName, jdbcUsername);
        assertGrantsCount(1);
    }

    private void assertGrantsCount(int count) {
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from grants " +
            "where grantee = :current_user and object_name like :table",
            ImmutableMap.of(
                "current_user", jdbcUsername,
                "table", '%' + tblName + '%'
            ),
            Integer.class), is(count));

    }
}
